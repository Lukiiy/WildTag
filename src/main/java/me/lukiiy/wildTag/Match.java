package me.lukiiy.wildTag;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Match {
    public final Map<Player, Teams> players = new HashMap<>();
    private final long timerFull;
    public long timer;
    public final Location center;
    public final World world;
    public final BossBar bar;

    private final Location ogCenter;
    private final double ogSize;
    private final ScheduledTask task;

    private static final Kit defaultKit = new Kit(ItemStack.of(Material.DIAMOND_PICKAXE), ItemStack.of(Material.DIAMOND_AXE), ItemStack.of(Material.DIAMOND_SHOVEL), ItemStack.of(Material.COBBLESTONE, 16));
    private static final Kit hunterKit = new Kit(getTracker());

    Match(@NotNull List<Player> players, @NotNull World world, @Nullable Location center, double size, long seconds, @Nullable List<Player> hunters) {
        WildTag tag = WildTag.getInstance();

        this.world = world;
        for (Player p : world.getPlayers()) this.players.put(p, Teams.RUNNER);

        if (hunters == null || getHunters().isEmpty()) this.players.put(players.get(tag.rng.nextInt(players.size())), Teams.HUNTER);
        else hunters.forEach(h -> this.players.put(h, Teams.HUNTER));

        WorldBorder border = world.getWorldBorder();
        this.ogCenter = border.getCenter();
        this.ogSize = border.getSize();

        long morePlayerTimer = tag.getConfig().getLong("morePlayerTimer", 0);
        if (players.size() > 2 && morePlayerTimer != 0) seconds += morePlayerTimer * (players.size() - 2);
        this.timerFull = this.timer = seconds;

        bar = BossBar.bossBar(Component.text(timer + " seconds"), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        this.center = center == null ? tag.randomLocation(world) : center;

        List<Location> spawns = getSpawns(this.center, size, players.size());
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);

            p.teleportAsync(spawns.get(i));
            preparePlayer(p);
            bar.addViewer(p);
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, .5f, 1);
        }

        border.setCenter(this.center);
        border.setSize(size);

        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(WildTag.getInstance(), (task) -> {
            if (timer == 0) {
                end(getRunners());
                return;
            }

            if (players.isEmpty() || players.size() == 1) {
                end();
                return;
            }

            if (timer < 16) {
                players.forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1));
                bar.color(BossBar.Color.RED);
            }

            if (timer == timerFull / 2) bar.color(BossBar.Color.YELLOW);

            timer--;

            bar.name(Component.text(timer + " seconds"));
            bar.progress((float) timer / timerFull);

            for (Player p : players) {
                if (getHunters().contains(p)) {
                    Component text = Component.text("Catch them all!").color(TextColor.color(0xFFA1B7));
                    if (timer > timerFull - 5) text = Component.text("Remember to use the compass!").color(TextColor.color(0xFF8257));
                    p.sendActionBar(text);
                } else p.sendActionBar(Component.text("Run away from ").append(Component.join(JoinConfiguration.commas(true), getHunters().stream().map(Player::name).toList())));
            }
        }, 1L, 20L);
    }

    public void end(List<Player> winners) {
        WildTag.getInstance().getMatches().remove(world);
        if (task != null && !task.isCancelled()) task.cancel();

        WorldBorder border = world.getWorldBorder();
        border.setCenter(ogCenter);
        border.setSize(ogSize);

        world.getPlayers().forEach(p -> {
            bar.removeViewer(p);
            if (getHunters().contains(p)) p.getInventory().remove(Material.COMPASS);

            if (winners == null || winners.isEmpty()) p.sendMessage(Component.text("Nobody won...").color(TextColor.color(0xA81F2F)).decorate(TextDecoration.ITALIC));
            else {
                Component list = Component.join(JoinConfiguration.separator(Component.text(", ")), winners.stream().map(Player::name).toList());
                p.sendMessage(list.append(Component.text(" won!").color(TextColor.color(0xFFF53)).decorate(TextDecoration.BOLD)));
            }

            if (p.getGameMode() == GameMode.SPECTATOR) {
                Location pLoc = p.getLocation();
                p.teleportAsync(new Location(world, pLoc.getX(), world.getHighestBlockYAt(pLoc.getBlockX(), pLoc.getBlockZ()) + 1, pLoc.getZ()));
                p.setGameMode(GameMode.SURVIVAL);
            }

            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, .5f, 1);
            p.sendActionBar(Component.empty());
        });
    }

    public void end() {
        end(Collections.emptyList());
    }

    public void end(Player p) {
        end(Collections.singletonList(p));
    }

    public void eliminate(Player p) {
        if (players.size() < 3 || getHunters().contains(p)) {
            end(getRunners());
            return;
        }

        uncastInfo(p);
        players.put(p, Teams.SPECTATOR);
    }

    public void castInfo(Player p) {
        p.sendMessage(Component.newline());
        p.sendMessage(Component.text("Runners: ").color(NamedTextColor.YELLOW).append(Component.join(JoinConfiguration.commas(true), getRunners().stream().map(Player::displayName).toList())));
        p.sendMessage(Component.text("Hunter: ").color(NamedTextColor.YELLOW).append(Component.join(JoinConfiguration.commas(true), getHunters().stream().map(Player::displayName).toList())));
        p.sendMessage(Component.newline());
        bar.addViewer(p);
    }

    public void uncastInfo(Player p) {
        bar.removeViewer(p);
    }

    public List<Player> getRunners() {
        return players.entrySet().stream().filter(entry -> entry.getValue() == Teams.RUNNER).map(Map.Entry::getKey).filter(Player::isOnline).toList();
    }

    public List<Player> getHunters() {
        return players.entrySet().stream().filter(entry -> entry.getValue() == Teams.HUNTER).map(Map.Entry::getKey).filter(Player::isOnline).toList();
    }

    private static List<Location> getSpawns(Location center, double borderSize, int pSize) { // TODO
        if (center == null || center.getWorld() == null) return List.of();
        List<Location> pos = new ArrayList<>();

        World world = center.getWorld();
        double radius = (borderSize / 2) - 8;
        int minY = world.getMinHeight();

        while (pos.size() < pSize) {
            int x1 = (int) (center.getX() + radius);
            int x2 = (int) (center.getX() - radius);
            int z1 = (int) (center.getZ() + radius);
            int z2 = (int) (center.getZ() - radius);

            int y_ne = getValidY(world, x1, z1);
            int y_nw = getValidY(world, x2, z1);
            int y_se = getValidY(world, x1, z2);
            int y_sw = getValidY(world, x2, z2);

            if (y_ne == minY && y_nw == minY && y_se == minY && y_sw == minY) y_ne = y_nw = y_se = y_sw = WildTag.getInstance().rng.nextInt(32, 96);

            pos.add(new Location(world, x1 + .5, y_ne + 1, z1 + .5)); // NE
            if (pos.size() < pSize) pos.add(new Location(world, x2 + .5, y_sw + 1, z2 + .5)); // SW
            if (pos.size() < pSize) pos.add(new Location(world, x2 + .5, y_nw + 1, z1 + .5)); // NW
            if (pos.size() < pSize) pos.add(new Location(world, x1 + .5, y_se + 1, z2 + .5)); // SE

            radius -= 12; // for the next set of players (if available)
        }

        Collections.shuffle(pos);
        return pos;
    }

    private static int getValidY(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        int maxY = world.getMaxHeight();

        if (world.getEnvironment() != World.Environment.NORMAL) {
            y = world.getMinHeight();
            int fallbackY = maxY / 2;

            while (y < maxY) {
                if (world.getBlockAt(x, y, z).isPassable()) {
                    y = avoidLiquid(world.getBlockAt(x, y, z), maxY);
                    return y;
                }
                y++;
            }

            world.getBlockAt(x, fallbackY, z).setType(Material.AIR);
            return fallbackY;
        }

        y = avoidLiquid(world.getBlockAt(x, y, z), maxY);
        return y;
    }

    private static int avoidLiquid(Block b, int maxY) {
        int y = b.getY();

        if (b.isLiquid()) {
            while (b.isLiquid() && y < maxY) {
                y++;
                b = b.getWorld().getBlockAt(b.getX(), y, b.getZ());
            }
            y--;
        }

        return y;
    }

    private static ItemStack getTracker() {
        ItemStack compass = ItemStack.of(Material.COMPASS);

        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.itemName(Component.text("Tracker"));
            meta.setEnchantmentGlintOverride(false);
            compass.setItemMeta(meta);
        }

        return compass;
    }

    private void preparePlayer(Player p) {
        AttributeInstance h = p.getAttribute(Attribute.MAX_HEALTH);
        if (h != null) p.setHealth(h.getValue());

        Block pBlock = p.getLocation().getBlock();

        pBlock.setType(Material.AIR);
        pBlock.getRelative(BlockFace.UP).setType(Material.AIR);

        Block blockBelow = pBlock.getRelative(BlockFace.DOWN);
        if (blockBelow.isEmpty() || blockBelow.isLiquid()) blockBelow.setType(Material.GLASS);

        p.setVelocity(new Vector(0, 0, 0));
        p.setGameMode(GameMode.SURVIVAL);
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setSaturation(5);
        p.setRemainingAir(p.getMaximumAir());
        p.getActivePotionEffects().forEach(potion -> p.removePotionEffect(potion.getType()));
        p.setFallDistance(0);
        p.resetTitle();

        PlayerInventory pInv = p.getInventory();

        pInv.clear();
        defaultKit.apply(pInv);
        if (getHunters().contains(p)) hunterKit.apply(pInv);

        getHunters().forEach(hunter -> hunter.getScheduler().run(WildTag.getInstance(), (task) -> getNearestRunner(hunter), null));
    }

    public Player getNearestRunner(Player p) {
        Player nearest = getRunners().stream()
                .filter(Entity::isValid)
                .min(Comparator.comparingDouble(targets -> targets.getLocation().distanceSquared(p.getLocation())))
                .orElse(null);

        if (nearest == null) return null;

        Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull).filter(item -> item.getType() == Material.COMPASS).forEach(compass -> {
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            meta.setLodestone(nearest.getLocation());
            meta.setLodestoneTracked(false);

            compass.setItemMeta(meta);
        });

        return nearest;
    }

    public enum Teams {
        RUNNER, HUNTER, SPECTATOR
    }
}
