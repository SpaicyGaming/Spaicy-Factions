package com.massivecraft.factions.zcore.frame.fdisband;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.zcore.frame.ConfirmGUI;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Factions - Developed by Driftay.
 * All rights reserved 2020.
 * Creation Date: 1/18/2020
 */
public class FDisbandFrame extends ConfirmGUI {

    public FDisbandFrame(FactionsPlugin plugin) {
        super(plugin, Objects.requireNonNull(plugin.getConfig().getConfigurationSection("f-disband-gui")));
    }

    @Override
    protected Consumer<InventoryClickEvent> getConfirmAction(FPlayer fPlayer) {
        return (e) -> {
            e.setCancelled(true);
            fPlayer.getPlayer().setMetadata("disband_confirm", new FixedMetadataValue(FactionsPlugin.getInstance(), System.currentTimeMillis()));
            fPlayer.getPlayer().closeInventory();
            fPlayer.getPlayer().performCommand("f disband");
        };
    }

    @Override
    protected UnaryOperator<String> applyLorePlaceholdersOperator(FPlayer fPlayer) {
        return s -> s.replace("{faction}", fPlayer.getFaction().getTag());
    }
}
