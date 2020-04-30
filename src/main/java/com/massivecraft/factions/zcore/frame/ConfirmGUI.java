package com.massivecraft.factions.zcore.frame;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.XMaterial;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public abstract class ConfirmGUI {

    private final FactionsPlugin plugin;
    private final ConfigurationSection config;

    private final Gui gui;

    public ConfirmGUI(FactionsPlugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        this.config = config;

        this.gui = new Gui(FactionsPlugin.getInstance(), 1, plugin.color(config.getString("title")));
    }

    protected abstract Consumer<InventoryClickEvent> getConfirmAction(FPlayer fPlayer);

    protected abstract UnaryOperator<String> applyLorePlaceholdersOperator(FPlayer fPlayer);

    public FactionsPlugin getPlugin() {
        return plugin;
    }

    public void buildGUI(FPlayer fPlayer) {
        PaginatedPane pane = new PaginatedPane(0, 0, 9, this.gui.getRows());
        List<GuiItem> GUIItems = new ArrayList<>();
        int i;

        // Confirm
        ItemStack confirmItem = buildItem("confirm-item", applyLorePlaceholdersOperator(fPlayer));
        for (i = 0; i < 5; ++i) {
            GUIItems.add(new GuiItem(confirmItem, getConfirmAction(fPlayer)));
        }

        //Separator
        ItemStack separatorItem = buildItem("separation-item", UnaryOperator.identity());
        GUIItems.set(4, new GuiItem(separatorItem, (e) -> e.setCancelled(true)));

        // Deny
        ItemStack denyItem = buildItem("deny-item", UnaryOperator.identity());
        for (i = 5; i < 10; ++i) {
            GUIItems.add(new GuiItem(denyItem, (e) -> {
                e.setCancelled(true);
                fPlayer.getPlayer().closeInventory();
            }));
        }

        pane.populateWithGuiItems(GUIItems);
        gui.addPane(pane);
        gui.update();
        gui.show(fPlayer.getPlayer());
    }

    @SuppressWarnings({"ConstantConditions", "OptionalGetWithoutIsPresent"})
    private ItemStack buildItem(String configPath, UnaryOperator<String> lorePlaceholders) {
        ConfigurationSection configurationSection = config.getConfigurationSection(configPath);
        assert configurationSection != null;

        ItemStack item = XMaterial.matchXMaterial(configurationSection.getString("Type")).get().parseItem();
        assert item != null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            for (String line : configurationSection.getStringList("Lore")) {
                lore.add(plugin.color(lorePlaceholders.apply(line)));
            }
            meta.setLore(lore);
            meta.setDisplayName(plugin.color(Objects.requireNonNull(configurationSection.getString("Name"))));
            item.setItemMeta(meta);
        }
        return item;
    }

}
