package com.massivecraft.factions.missions;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.XMaterial;
import com.massivecraft.factions.zcore.frame.FactionGUI;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MissionGUI implements FactionGUI {

    /**
     * @author Driftay
     */

    private FactionsPlugin plugin;
    private FPlayer fPlayer;
    private Inventory inventory;
    private Map<Integer, String> slots;

    public MissionGUI(FactionsPlugin plugin, FPlayer fPlayer) {
        this.slots = new HashMap<>();
        this.plugin = plugin;
        this.fPlayer = fPlayer;
        this.inventory = plugin.getServer().createInventory(this, plugin.getConfig().getInt("MissionGUISize") * 9, plugin.color(plugin.getConfig().getString("Missions-GUI-Title")));
    }

    @Override
    public void onClick(int slot, ClickType action) {
        String missionName = slots.get(slot);
        if (missionName == null) return;
        ConfigurationSection configurationSection = plugin.getConfig().getConfigurationSection("Missions");
        if (missionName.equals(plugin.color(FactionsPlugin.getInstance().getConfig().getString("Randomization.Start-Item.Allowed.Name")))) {
            Mission pickedMission = null;
            Set<String> keys = plugin.getConfig().getConfigurationSection("Missions").getKeys(false);
            while (pickedMission == null) {
                Random r = new Random();
                int pick = r.nextInt(keys.size() - 1);
                if (!keys.toArray()[pick].toString().equals("FillItem")) {
                    missionName = keys.toArray()[pick].toString();
                    if (!fPlayer.getFaction().getMissions().containsKey(missionName)) {
                        pickedMission = new Mission(missionName, plugin.getConfig().getString("Missions." + missionName + ".Mission.Type"));
                        fPlayer.getFaction().getMissions().put(missionName, pickedMission);
                        fPlayer.msg(TL.MISSION_MISSION_STARTED, fPlayer.describeTo(fPlayer.getFaction()), plugin.color(plugin.getConfig().getString("Missions." + missionName + ".Name")));
                        build();
                        fPlayer.getPlayer().openInventory(inventory);
                        return;
                    }
                }
            }
        } else if (plugin.getConfig().getBoolean("Randomization.Enabled")) {
            return;
        }
        if (configurationSection == null) return;

        if (fPlayer.getFaction().getMissions().containsKey(missionName)) {
            if (action == ClickType.RIGHT || action == ClickType.SHIFT_LEFT) {
                // deactivate mission confirm gui
                new DeactivateMissionGUI(plugin, missionName).buildGUI(fPlayer);
            } else {
                // already active
                fPlayer.msg(TL.MISSION_MISSION_ACTIVE);
                fPlayer.getPlayer().closeInventory();
            }
            return;
        }

        int max = plugin.getConfig().getInt("MaximumMissionsAllowedAtOnce");
        if (fPlayer.getFaction().getMissions().size() >= max) {
            fPlayer.msg(TL.MISSION_MISSION_MAX_ALLOWED, max);
            fPlayer.getPlayer().closeInventory();
            return;
        }
        if (missionName.equals(plugin.color(FactionsPlugin.getInstance().getConfig().getString("Randomization.Start-Item.Disallowed.Name"))))
            return;

        ConfigurationSection section = configurationSection.getConfigurationSection(missionName);
        if (section == null) return;

        if (FactionsPlugin.getInstance().getConfig().getBoolean("DenyMissionsMoreThenOnce")) {
            if (fPlayer.getFaction().getCompletedMissions().contains(missionName)) {
                fPlayer.msg(TL.MISSION_ALREAD_COMPLETED);
                fPlayer.getPlayer().closeInventory();
                return;
            }
        }

        ConfigurationSection missionSection = section.getConfigurationSection("Mission");
        if (missionSection == null) return;

        Mission mission = new Mission(missionName, missionSection.getString("Type"));
        fPlayer.getFaction().getMissions().put(missionName, mission);
        fPlayer.msg(TL.MISSION_MISSION_STARTED, fPlayer.describeTo(fPlayer.getFaction()), plugin.color(section.getString("Name")));
        build();
        fPlayer.getPlayer().openInventory(inventory);
    }

    @Override
    public void build() {
        ConfigurationSection configurationSection = plugin.getConfig().getConfigurationSection("Missions");
        if (configurationSection == null) {
            return;
        }
        ItemStack fillItem = XMaterial.matchXMaterial(configurationSection.getString("FillItem.Material")).get().parseItem();
        ItemMeta fillmeta = fillItem.getItemMeta();
        fillmeta.setDisplayName("");
        fillItem.setItemMeta(fillmeta);
        for (int fill = 0; fill < configurationSection.getInt("FillItem.Rows") * 9; ++fill) {
            //Why were we generating a new itemstack per slot?????
            inventory.setItem(fill, fillItem);
        }
        for (String key : configurationSection.getKeys(false)) {
            if (!key.equals("FillItem")) {
                ConfigurationSection section = configurationSection.getConfigurationSection(key);
                int slot = section.getInt("Slot");
                int amount = section.getInt("Amount");

                ItemStack itemStack = XMaterial.matchXMaterial(section.getString("Material")).get().parseItem();
                if (amount > 1) {
                    itemStack.setAmount(amount);
                }

                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("Name")));
                List<String> loreLines = new ArrayList<>();
                for (String line : section.getStringList("Lore")) {
                    loreLines.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                if (fPlayer.getFaction().getMissions().containsKey(key)) {
                    Mission mission = fPlayer.getFaction().getMissions().get(key);
                    itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
                    if (!FactionsPlugin.getInstance().mc17) {
                        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                    for (String progressLine : plugin.getConfig().getStringList("Mission-Progress-Format")) {
                        loreLines.add(plugin.color(progressLine)
                                .replace("{progress}", String.valueOf(mission.getProgress()))
                                .replace("{total}", String.valueOf(section.getConfigurationSection("Mission").get("Amount"))));
                    }
                }
                itemMeta.setLore(loreLines);
                itemStack.setItemMeta(itemMeta);
                inventory.setItem(slot, itemStack);
                slots.put(slot, key);
            }
        }
        if (plugin.getConfig().getBoolean("Randomization.Enabled")) {
            ItemStack start;
            ItemMeta meta;
            start = XMaterial.matchXMaterial(plugin.getConfig().getString("Randomization.Start-Item.Allowed.Material")).get().parseItem();
            meta = start.getItemMeta();
            meta.setDisplayName(plugin.color(plugin.getConfig().getString("Randomization.Start-Item.Allowed.Name")));
            List<String> loree = new ArrayList<>();
            for (String string : plugin.getConfig().getStringList("Randomization.Start-Item.Allowed.Lore")) {
                loree.add(plugin.color(string));
            }
            meta.setLore(loree);
            start.setItemMeta(meta);
            if (fPlayer.getFaction().getCompletedMissions().size() >= configurationSection.getKeys(false).size() - 1 && plugin.getConfig().getBoolean("DenyMissionsMoreThenOnce")) {
                start = XMaterial.matchXMaterial(plugin.getConfig().getString("Randomization.Start-Item.Disallowed.Material")).get().parseItem();
                meta = start.getItemMeta();
                meta.setDisplayName(plugin.color(plugin.getConfig().getString("Randomization.Start-Item.Disallowed.Name")));
                List<String> lore = new ArrayList<>();
                for (String string : plugin.getConfig().getStringList("Randomization.Start-Item.Disallowed.Lore")) {
                    lore.add(plugin.color(string).replace("%reason%", TL.MISSION_MISSION_ALL_COMPLETED.toString()));
                }
                meta.setLore(lore);
                start.setItemMeta(meta);
            }
            if (fPlayer.getFaction().getMissions().size() >= plugin.getConfig().getInt("MaximumMissionsAllowedAtOnce")) {
                start = XMaterial.matchXMaterial(plugin.getConfig().getString("Randomization.Start-Item.Disallowed.Material")).get().parseItem();
                meta = start.getItemMeta();
                meta.setDisplayName(plugin.color(plugin.getConfig().getString("Randomization.Start-Item.Disallowed.Name")));
                List<String> lore = new ArrayList<>();
                for (String string : plugin.getConfig().getStringList("Randomization.Start-Item.Disallowed.Lore")) {
                    lore.add(plugin.color(string).replace("%reason%", FactionsPlugin.getInstance().txt.parse(TL.MISSION_MISSION_MAX_ALLOWED.toString(), plugin.getConfig().getInt("MaximumMissionsAllowedAtOnce"))));
                }
                meta.setLore(lore);
                start.setItemMeta(meta);
            }
            inventory.setItem(plugin.getConfig().getInt("Randomization.Start-Item.Slot"), start);
            slots.put(plugin.getConfig().getInt("Randomization.Start-Item.Slot"), start.getItemMeta().getDisplayName());
        }
    }

    public Inventory getInventory() {
        return inventory;
    }
}
