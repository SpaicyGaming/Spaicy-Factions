package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class BannersListener implements Listener {

    public static HashMap<String, Location> bannerLocations = new HashMap<>();
    private HashMap<String, Boolean> bannerCooldownMap = new HashMap<>();

    @EventHandler
    public void onBannerPlace(BlockPlaceEvent e) {
        if (FactionsPlugin.getInstance().mc17) return;

        if (e.getItemInHand().getType().name().contains("BANNER")) {
            ItemStack bannerInHand = e.getItemInHand();
            FPlayer fme = FPlayers.getInstance().getByPlayer(e.getPlayer());
            ItemStack warBanner = fme.getFaction().getBanner();
            if (warBanner == null) return;
            ItemMeta warmeta = warBanner.getItemMeta();
            warmeta.setDisplayName(FactionsPlugin.getInstance().color(FactionsPlugin.getInstance().getConfig().getString("fbanners.Item.Name")));
            warmeta.setLore(FactionsPlugin.getInstance().colorList(FactionsPlugin.getInstance().getConfig().getStringList("fbanners.Item.Lore")));
            warBanner.setItemMeta(warmeta);
            if (warBanner.isSimilar(bannerInHand)) {
                if (fme.getFaction().isWilderness()) {
                    fme.msg(TL.WARBANNER_NOFACTION);
                    e.setCancelled(true);
                    return;
                }
                int bannerTime = FactionsPlugin.getInstance().getConfig().getInt("fbanners.Banner-Time") * 20;
                Location placedLoc = e.getBlockPlaced().getLocation();
                FLocation fplacedLoc = new FLocation(placedLoc);
                if ((Board.getInstance().getFactionAt(fplacedLoc).isWarZone() && FactionsPlugin.getInstance().getConfig().getBoolean("fbanners.Placeable.Warzone")) || (fme.getFaction().getRelationTo(Board.getInstance().getFactionAt(fplacedLoc)) == Relation.ENEMY && FactionsPlugin.getInstance().getConfig().getBoolean("fbanners.Placeable.Enemy"))) {
                    if (bannerCooldownMap.containsKey(fme.getTag())) {
                        fme.msg(TL.WARBANNER_COOLDOWN);
                        e.setCancelled(true);
                        return;
                    }
                    for (FPlayer fplayer : fme.getFaction().getFPlayers()) {
                        fplayer.getPlayer().sendTitle(FactionsPlugin.getInstance().color(fme.getTag() + " Placed A WarBanner!"), FactionsPlugin.getInstance().color("&7use &c/f tpbanner&7 to tp to the banner!"));
                    }
                    bannerCooldownMap.put(fme.getTag(), true);
                    bannerLocations.put(fme.getTag(), e.getBlockPlaced().getLocation());
                    int bannerCooldown = FactionsPlugin.getInstance().getConfig().getInt("fbanners.Banner-Place-Cooldown");
                    org.bukkit.entity.ArmorStand as = (org.bukkit.entity.ArmorStand) e.getBlockPlaced().getLocation().add(0.5, 1.0, 0.5).getWorld().spawnEntity(e.getBlockPlaced().getLocation().add(0.5, 1.0, 0.5), EntityType.ARMOR_STAND);
                    as.setVisible(false);
                    as.setGravity(false);
                    as.setCanPickupItems(false);
                    as.setCustomName(FactionsPlugin.getInstance().color(FactionsPlugin.getInstance().getConfig().getString("fbanners.BannerHolo").replace("{Faction}", fme.getTag())));
                    as.setCustomNameVisible(true);
                    String tag = fme.getTag();
                    Bukkit.getScheduler().scheduleSyncDelayedTask(FactionsPlugin.getInstance(), () -> bannerCooldownMap.remove(tag), Long.parseLong(bannerCooldown + ""));
                    Block banner = e.getBlockPlaced();
                    Material bannerType = banner.getType();
                    Faction bannerFaction = fme.getFaction();
                    banner.getWorld().strikeLightningEffect(banner.getLocation());
                    int radius = FactionsPlugin.getInstance().getConfig().getInt("fbanners.Banner-Effect-Radius");
                    List<String> effects = FactionsPlugin.getInstance().getConfig().getStringList("fbanners.Effects");
                    int affectorTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(FactionsPlugin.getInstance(), () -> {
                        for (Entity e1 : Objects.requireNonNull(banner.getLocation().getWorld()).getNearbyEntities(banner.getLocation(), radius, 255.0, radius)) {
                            if (e1 instanceof Player) {
                                Player player = (Player) e1;
                                FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
                                if (fplayer.getFaction() != bannerFaction) {
                                    continue;
                                }
                                for (String effect : effects) {
                                    String[] components = effect.split(":");
                                    player.addPotionEffect(new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName(components[0])), 100, Integer.parseInt(components[1])));
                                }
                                if (banner.getType() == bannerType) {
                                    continue;
                                }
                                banner.setType(bannerType);
                            }
                        }
                    }, 0L, 20L);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(FactionsPlugin.getInstance(), () -> {
                        banner.setType(Material.AIR);
                        as.remove();
                        banner.getWorld().strikeLightningEffect(banner.getLocation());
                        Bukkit.getScheduler().cancelTask(affectorTask);
                        bannerLocations.remove(bannerFaction.getTag());
                    }, Long.parseLong(bannerTime + ""));
                } else {
                    fme.msg(TL.WARBANNER_INVALIDLOC);
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBannerBreak(BlockBreakEvent e) {
        FPlayer fme = FPlayers.getInstance().getByPlayer(e.getPlayer());
        if (FactionsPlugin.getInstance().mc17) {
            return;
        }

        if (bannerLocations.containsValue(e.getBlock().getLocation())) {
            if (e.getBlock().getType().name().contains("BANNER")) {
                e.setCancelled(true);
                fme.msg(TL.BANNER_CANNOT_BREAK);
            }
        }
    }

}
