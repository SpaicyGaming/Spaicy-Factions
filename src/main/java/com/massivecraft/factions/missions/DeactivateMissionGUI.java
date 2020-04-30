package com.massivecraft.factions.missions;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.zcore.frame.ConfirmGUI;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class DeactivateMissionGUI extends ConfirmGUI {

    private final String missionName;
    private final String missionDisplayName;

    public DeactivateMissionGUI(FactionsPlugin plugin, String missionName) {
        super(plugin, Objects.requireNonNull(plugin.getConfig().getConfigurationSection("Mission-deactivate-gui")));
        this.missionName = missionName;

        ConfigurationSection missionConfig = plugin.getConfig().getConfigurationSection("Missions." + missionName);
        assert missionConfig != null;
        this.missionDisplayName = getPlugin().color(missionConfig.getString("Name"));
    }

    @Override
    protected Consumer<InventoryClickEvent> getConfirmAction(FPlayer fPlayer) {
        return (e) -> {
            e.setCancelled(true);
            fPlayer.getFaction().getMissions().remove(missionName);
            fPlayer.getFaction().msg(TL.MISSION_MISSION_DEACTIVATED, fPlayer.describeTo(fPlayer.getFaction()), missionDisplayName);
            fPlayer.getPlayer().closeInventory();
        };
    }

    @Override
    protected UnaryOperator<String> applyLorePlaceholdersOperator(FPlayer fPlayer) {
        return s -> s.replace("{mission}", fPlayer.getFaction().getTag());
    }
}
