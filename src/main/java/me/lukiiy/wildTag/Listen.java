package me.lukiiy.wildTag;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.List;

public class Listen implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void hit(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player entity && WildTag.getInstance().getMatch(entity) != null && e.getDamager() instanceof Player damager) {
            Match match = WildTag.getInstance().getMatch(entity);
            if (match == null) return;

            List<Player> runners = match.getRunners();
            List<Player> hunters = match.getHunters();

            if (hunters.contains(damager)) {
                if (runners.size() == 1) {
                    match.eliminate(entity);
                    return;
                }

                e.setDamage(999);
                hunters.forEach(h -> h.getScheduler().run(WildTag.getInstance(), (task) -> match.getNearestRunner(h), null));
            } else {
                if (hunters.contains(entity)) e.setDamage(0);
                if (Kit.isKitItem(damager.getInventory().getItemInMainHand())) e.setDamage(0);
            }
        }
    }

    @EventHandler
    public void death(PlayerDeathEvent e) {
        Player p = e.getEntity();
        e.getDrops().removeIf(Kit::isKitItem);

        Match match = WildTag.getInstance().getMatch(p);
        if (match == null) return;

        match.eliminate(p);

        p.getScheduler().execute(WildTag.getInstance(), () -> {
            if (match.players.size() <= 1) return;
            p.spigot().respawn();
        }, null, 40L);
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Match match = WildTag.getInstance().getMatch(p.getWorld());
        if (match == null) return;
        
        p.setGameMode(GameMode.SPECTATOR);
        p.teleportAsync(match.center);
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Match match = WildTag.getInstance().getMatch(p);
        if (match == null) return;

        match.eliminate(p);
    }

    @EventHandler
    public void food(FoodLevelChangeEvent e) {
        if (WildTag.getInstance().getMatch((Player) e.getEntity()) == null) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void itemUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        Match match = WildTag.getInstance().getMatch(p);
        if (match == null) return;

        if ((p.getInventory().getItemInMainHand().getType() == Material.COMPASS || p.getInventory().getItemInOffHand().getType() == Material.COMPASS) && match.getHunters().contains(p) && p.getCooldown(Material.COMPASS) <= 0) {
            Player nearest = match.getNearestRunner(p);
            if (nearest == null) return;

            final int fY = nearest.getLocation().getBlockY() - p.getLocation().getBlockY();
            String yDisplay = fY == 0 ? "" : "(y " + (fY > 0 ? "+" + fY : String.valueOf(fY)) + ")";

            p.sendMessage(Component.text("Pointing at ").append(nearest.name().color(TextColor.color(0xFFF53))).appendSpace().append(Component.text(yDisplay).color(TextColor.color(0xFBFF63))));
            p.setCooldown(Material.COMPASS, 5);
        }
    }

    @EventHandler
    public void drop(PlayerDropItemEvent e) {
        Match match = WildTag.getInstance().getMatch(e.getPlayer());

        if (Kit.isKitItem(e.getItemDrop().getItemStack())) {
            if (match != null) e.setCancelled(true);
            else e.getItemDrop().remove();
        }
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        Match matchFrom = WildTag.getInstance().getMatch(e.getFrom());
        Match matchTo = WildTag.getInstance().getMatch(p.getWorld());

        if (matchFrom != null && matchFrom.getRunners().contains(p)) {
            matchFrom.world.getPlayers().forEach(mP -> mP.sendMessage(Component.translatable("death.attack.outsideBorder", p.name())));
            p.sendMessage(Component.text("You got eliminated because you changed worlds!").color(NamedTextColor.RED));
            matchFrom.eliminate(p);
        }

        if (matchTo != null) {
            p.sendMessage(Component.text("This world has an on-going tag match!").color(NamedTextColor.YELLOW));
            p.setGameMode(GameMode.SPECTATOR);
            p.teleportAsync(matchTo.center);
            matchTo.castInfo(p);
        }
    }

    @EventHandler
    public void respawn(PlayerPostRespawnEvent e) {
        Player p = e.getPlayer();
        Location loc = p.getLastDeathLocation();

        if (loc == null || !loc.isChunkLoaded()) return;

        Match match = WildTag.getInstance().getMatch(loc.getWorld());
        if (match == null) return;

        p.teleportAsync(match.center).thenAccept(s -> {
            if (s) p.setGameMode(GameMode.SPECTATOR);
        });
    }
}
