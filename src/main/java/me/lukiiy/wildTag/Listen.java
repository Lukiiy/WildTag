package me.lukiiy.wildTag;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
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
        if (e.getEntity() instanceof Player attacked && WildTag.getInstance().getMatch(attacked) != null && e.getDamager() instanceof Player damager) {
            Match match = WildTag.getInstance().getMatch(attacked);
            if (match == null) return;

            List<Player> runners = match.getPlayers(Match.Teams.RUNNER);
            List<Player> hunters = match.getPlayers(Match.Teams.HUNTER);

            if (Kit.isKitItem(damager.getInventory().getItemInMainHand()) || runners.contains(damager)) e.setDamage(0);

            if (hunters.contains(damager) && runners.contains(attacked)) {
                if (runners.size() == 1) {
                    match.eliminate(attacked);
                    return;
                }

                e.setDamage(attacked.getHealth() + 1);
                hunters.forEach(h -> h.getScheduler().run(WildTag.getInstance(), (task) -> match.getNearestRunner(h), null));
            }
        }
    }

    @EventHandler
    public void death(PlayerDeathEvent e) {
        e.getDrops().removeIf(Kit::isKitItem);

        Player p = e.getEntity();
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
        match.castInfo(p);
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

        if ((p.getInventory().getItemInMainHand().getType() == Material.COMPASS || p.getInventory().getItemInOffHand().getType() == Material.COMPASS) && match.getPlayers(Match.Teams.HUNTER).contains(p) && p.getCooldown(Material.COMPASS) <= 0) {
            Player nearest = match.getNearestRunner(p);
            if (nearest == null) return;

            final int fY = nearest.getLocation().getBlockY() - p.getLocation().getBlockY();
            String yDisplay = fY == 0 ? "" : "(y " + (fY > 0 ? "+" + fY : String.valueOf(fY)) + ")";

            p.sendMessage(Component.text("Pointing at ").append(nearest.name().color(TextColor.color(0xFFF53))).appendSpace().append(Component.text(yDisplay).color(TextColor.color(0xFBFF63))));
            p.setCooldown(Material.COMPASS, 5);
            e.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        if (e.hasBlock() && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.ENDER_CHEST) { // TODO: Update in 1.21.11
            p.sendActionBar(Component.translatable("container.isLocked").arguments(Component.translatable("block.minecraft.ender_chest")));
            p.playSound(p, Sound.BLOCK_CHEST_LOCKED, 1, 1);
            e.setUseInteractedBlock(Event.Result.DENY);
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

        if (matchFrom != null && matchFrom.getPlayers(Match.Teams.RUNNER).contains(p)) {
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
