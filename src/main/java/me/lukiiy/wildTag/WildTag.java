package me.lukiiy.wildTag;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class WildTag extends JavaPlugin {
    public final Random rng = ThreadLocalRandom.current();

    @Override
    public void onEnable() {
        setupConfig();
        getServer().getPluginManager().registerEvents(new Listen(), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, it -> it.registrar().register(Cmd.INSTANCE.register(), "WildTag main command"));
    }

    @Override
    public void onDisable() {
        endAll();
    }

    public static WildTag getInstance() {
        return JavaPlugin.getPlugin(WildTag.class);
    }

    // Config
    public void setupConfig() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    // API
    public Map<World, Match> getMatches() {
        return Match.getMatches();
    }

    public Match getMatch(World world) {
        return getMatches().get(world);
    }

    public Match getMatch(Player player) {
        Match match = getMatch(player.getWorld());

        if (match != null && match.players.get(player) != null) return match;
        return null;
    }

    public Location randomLocation(World world) {
        int range = getConfig().getInt("mapSearchRange");

        int cX = rng.nextInt(range * 2) - range;
        int cZ = rng.nextInt(range * 2) - range;
        int cY = world.getHighestBlockYAt(cX, cZ);

        return new Location(world, cX, cY, cZ);
    }

    public void endAll() {
        Match.getMatches().values().forEach(Match::end);
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
