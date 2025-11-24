package me.lukiiy.wildTag;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class WildTag extends JavaPlugin {
    private final HashMap<World, Match> activeMatches = new HashMap<>();
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
        return activeMatches;
    }

    public Match start(Collection<Player> players, World world, Location center, double size, long seconds, Collection<Player> hunters) {
        if (players == null || players.isEmpty() || world == null || (center != null && center.getWorld() != world) || getMatch(world) != null) return null;

        Match match = new Match(players.stream().toList(), world, center, size, seconds, hunters.stream().toList());

        activeMatches.put(world, match);
        getComponentLogger().info(Component.text("Starting tag match on world " + world.getName() + "!").color(NamedTextColor.GOLD));
        return match;
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
        for (Match match : new ArrayList<>(activeMatches.values())) match.end();
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
