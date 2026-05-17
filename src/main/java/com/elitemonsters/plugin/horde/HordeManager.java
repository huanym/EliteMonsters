package com.elitemonsters.plugin.horde;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class HordeManager {

    private final EliteMonstersPlugin plugin;
    private final Random random = new Random();
    private HordeSession activeHorde;
    private BukkitTask autoTask;
    public static final String HORDE_META_KEY = "elitemonsters-horde";

    public HordeManager(EliteMonstersPlugin plugin) { this.plugin = plugin; }

    public void startAutoTask() {
        if (!plugin.getConfig().getBoolean("horde.enabled", true)) return;
        long interval = plugin.getConfig().getLong("horde.auto-interval", 7200) * 20L;
        if (interval <= 0) return;
        autoTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeHorde != null) return;
                if (plugin.getServer().getOnlinePlayers().size() < plugin.getConfig().getInt("horde.min-players", 1)) return;
                Location center = getHordeCenter();
                if (center != null) startHorde(center, null);
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    public boolean startHorde(Location location, String specifiedType) {
        plugin.getConfigManager().debugLog("Horde start: world=" + location.getWorld().getName() + " type=" + (specifiedType != null ? specifiedType : "auto"));
        if (activeHorde != null) return false;
        int totalWaves = getTotalWaves();
        if (totalWaves == 0) return false;
        activeHorde = new HordeSession(location, totalWaves, specifiedType);
        activeHorde.start();
        Component msg = plugin.getLangManager().getComponent("horde-start", totalWaves);
        plugin.broadcastComponent(msg);
        return true;
    }

    public void stopHorde() {
        if (activeHorde != null) { activeHorde.destroy(); activeHorde = null; broadcast(langComponent("horde-stopped")); }
    }

    public boolean isHordeActive() { return activeHorde != null && activeHorde.isRunning(); }
    public HordeSession getActiveHorde() { return activeHorde; }

    public void checkRandomHorde(World world) {
        if (activeHorde != null || !plugin.getConfig().getBoolean("horde.enabled", true)) return;
        if (random.nextDouble() >= plugin.getConfig().getDouble("horde.random-chance", 0.15)) return;
        if (world.getTime() < 13000) return;
        if (plugin.getServer().getOnlinePlayers().size() < plugin.getConfig().getInt("horde.min-players", 1)) return;
        Location center = getHordeCenter();
        if (center != null) startHorde(center, null);
    }

    public void cleanup() {
        if (autoTask != null) autoTask.cancel();
        if (activeHorde != null) { activeHorde.destroy(); activeHorde = null; }
    }

    private Location getHordeCenter() {
        String mode = plugin.getConfig().getString("horde.center-mode", "random_player");
        if (mode.equalsIgnoreCase("world_spawn")) {
            var players = new ArrayList<>(plugin.getServer().getOnlinePlayers());
            if (players.isEmpty()) return null;
            return players.get(0).getWorld().getSpawnLocation();
        }
        Player target = getRandomPlayer();
        return target != null ? target.getLocation() : null;
    }

    private Player getRandomPlayer() { var ps = new ArrayList<>(plugin.getServer().getOnlinePlayers()); return ps.isEmpty() ? null : ps.get(random.nextInt(ps.size())); }
    private int getTotalWaves() { var c = plugin.getConfig().getConfigurationSection("horde.waves"); return c == null ? 0 : c.getKeys(false).size(); }
    private Component langComponent(String key, Object... args) { return plugin.getLangManager().getComponent(key, args); }
    private void broadcast(Component msg) { plugin.broadcastComponent(msg); }

    public class HordeSession {
        private final Location center;
        private final int totalWaves;
        private final String specifiedType;
        private int currentWave = 0;
        private boolean running = false;
        private BukkitTask waveTask;
        private BukkitTask countdownTask;
        private BukkitTask timeoutTask;
        private final List<LivingEntity> spawnedMobs = new ArrayList<>();
        private final Set<UUID> participants = new HashSet<>();
        private BossBar bossBar;

        public HordeSession(Location center, int totalWaves, String specifiedType) { this.center = center; this.totalWaves = totalWaves; this.specifiedType = specifiedType; }

        public void start() {
            running = true;
            startNextWave();
        }

        public void destroy() {
            running = false;
            cancelTasks();
            removeBossBar();
            for (LivingEntity m : spawnedMobs) {
                m.setGlowing(false);
                if (m.isValid()) m.remove();
            }
            spawnedMobs.clear();
        }

        private void cancelTasks() { if (waveTask != null) { waveTask.cancel(); waveTask = null; } if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; } if (timeoutTask != null) { timeoutTask.cancel(); timeoutTask = null; } }

        private void startNextWave() {
            currentWave++;
            if (currentWave > totalWaves) { completeHorde(); return; }
            plugin.getConfigManager().debugLog("Horde wave " + currentWave + "/" + totalWaves + " start");

            String waveKey = "horde.waves." + currentWave;
            int mobCount = plugin.getConfig().getInt(waveKey + ".mob-count", 10);
            List<String> mobTypes = plugin.getConfig().getStringList(waveKey + ".mob-types");
            double eliteChance = plugin.getConfig().getDouble(waveKey + ".elite-chance", 0.0);
            boolean boss = plugin.getConfig().getBoolean(waveKey + ".boss", false);
            long timeoutSeconds = plugin.getConfig().getLong(waveKey + ".wave-timeout", 0);
            boolean glowMobs = plugin.getConfig().getBoolean("horde.glow-mobs", true);

            Component titleComp = langComponent("horde-title-wave", currentWave, totalWaves);
            showTitleToAll(titleComp, Component.empty());

            Component waveMsg = langComponent("horde-wave-start", currentWave, totalWaves);
            broadcast(waveMsg);

            double spawnRadius = plugin.getConfig().getDouble("horde.spawn-radius", 15.0);
            double minRadius = plugin.getConfig().getDouble("horde.spawn-min-radius", 8.0);

            World world = center.getWorld();
            for (int i = 0; i < mobCount; i++) {
                EntityType type = EntityType.valueOf(mobTypes.get(random.nextInt(mobTypes.size())).toUpperCase());
                Location spawnLoc = getSafeSpawnLocation(world, spawnRadius, minRadius);
                Entity entity = world.spawnEntity(spawnLoc, type);
                if (entity instanceof LivingEntity living) {
                    living.setMetadata(HORDE_META_KEY, new FixedMetadataValue(plugin, true));
                    if (glowMobs) living.setGlowing(true);
                    spawnedMobs.add(living);
                    if (random.nextDouble() < eliteChance) {
                        plugin.getGenerationListener().convertToElite(living, null, 0);
                    }
                    Player nearest = getNearestPlayer(world, spawnLoc);
                    if (nearest != null && living instanceof Mob mob) {
                        mob.setTarget(nearest);
                    }
                }
            }

            if (boss) {
                if (mobTypes.size() >= 5) {
                    EntityType bossType = EntityType.valueOf(mobTypes.get(mobTypes.size() - 1).toUpperCase());
                    Location bossLoc = getSafeSpawnLocation(world, spawnRadius, minRadius);
                    Entity bossEntity = world.spawnEntity(bossLoc, bossType);
                    if (bossEntity instanceof LivingEntity bossLiving) {
                        bossLiving.setMetadata(HORDE_META_KEY, new FixedMetadataValue(plugin, true));
                        if (glowMobs) bossLiving.setGlowing(true);
                        spawnedMobs.add(bossLiving);
                        plugin.getGenerationListener().convertToElite(bossLiving, null, 5);
                        Player nearest = getNearestPlayer(world, bossLoc);
                        if (nearest != null && bossLiving instanceof Mob mob) {
                            mob.setTarget(nearest);
                        }
                    }
                }
            }

            removeBossBar();
            bossBar = BossBar.bossBar(langComponent("horde-bossbar-wave", currentWave, totalWaves), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
            addBossBarToPlayers();

            if (timeoutSeconds > 0) {
                final int waveNum = currentWave;
                timeoutTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getConfigManager().debugLog("Horde wave " + waveNum + " timeout");
                        broadcast(langComponent("horde-timeout", waveNum));
                        failHorde();
                    }
                }.runTaskLater(plugin, timeoutSeconds * 20L);
            }

            waveTask = new BukkitRunnable() {
                @Override
                public void run() {
                    spawnedMobs.removeIf(m -> !m.isValid());
                    if (bossBar != null) {
                        int alive = (int) spawnedMobs.stream().filter(LivingEntity::isValid).count();
                        float progress = Math.max(0.0f, Math.min(1.0f, (float) alive / mobCount));
                        bossBar.progress(progress);
                    }
                    if (spawnedMobs.isEmpty()) {
                        if (timeoutTask != null) { timeoutTask.cancel(); timeoutTask = null; }
                        this.cancel();
                        waveTask = null;
                        giveWaveRewards(waveKey);
                        int countdown = plugin.getConfig().getInt("horde.wave-interval", 15);
                        if (currentWave < totalWaves) {
                            broadcast(langComponent("horde-wave-clear", currentWave, countdown));
                        }
                        removeBossBar();
                        if (currentWave < totalWaves && countdown > 0) {
                            bossBar = BossBar.bossBar(langComponent("horde-bossbar-countdown", countdown), 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
                            addBossBarToPlayers();
                            countdownTask = new BukkitRunnable() {
                                int remaining = countdown;
                                @Override
                                public void run() {
                                    if (bossBar != null) {
                                        bossBar.progress((float) remaining / countdown);
                                        bossBar.name(langComponent("horde-bossbar-countdown", remaining));
                                    }
                                    if (remaining <= 0) {
                                        startNextWave();
                                        this.cancel();
                                        countdownTask = null;
                                    }
                                    remaining--;
                                }
                            }.runTaskTimer(plugin, 0L, 20L);
                        } else {
                            startNextWave();
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }

        private void completeHorde() {
            running = false;
            plugin.getConfigManager().debugLog("Horde completed: " + totalWaves + " waves, " + participants.size() + " participants");
            giveCompletionRewards();
            HordeManager.this.activeHorde = null;
        }

        private void failHorde() {
            running = false;
            cancelTasks();
            removeBossBar();
            Component failMsg = langComponent("horde-failed");
            broadcast(failMsg);
            Component titleComp = langComponent("horde-title-failed");
            showTitleToAll(titleComp, Component.empty());
            for (LivingEntity m : spawnedMobs) {
                m.setGlowing(false);
                if (m.isValid()) m.remove();
            }
            spawnedMobs.clear();
            HordeManager.this.activeHorde = null;
        }

        private void removeBossBar() {
            if (bossBar != null) {
                for (Player p : plugin.getServer().getOnlinePlayers()) bossBar.removeViewer(p);
                bossBar = null;
            }
        }

        private void addBossBarToPlayers() {
            if (bossBar == null) return;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getWorld().equals(center.getWorld())) {
                    bossBar.addViewer(player);
                }
            }
        }

        private void showTitleToAll(Component title, Component subtitle) {
            Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500));
            Title tit = Title.title(title, subtitle != null ? subtitle : Component.empty(), times);
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getWorld().equals(center.getWorld())) {
                    player.showTitle(tit);
                }
            }
        }

        private void giveWaveRewards(String waveKey) {
            List<String> rewardIds = plugin.getConfig().getStringList(waveKey + ".rewards");
            if (!rewardIds.isEmpty()) {
                plugin.getConfigManager().debugLog("Wave " + currentWave + " rewards: " + rewardIds);
                for (UUID pid : participants) {
                    Player player = plugin.getServer().getPlayer(pid);
                    if (player == null || !player.isOnline()) continue;
                    plugin.getRewardManager().giveRewards(rewardIds, player);
                }
                return;
            }

            String path = waveKey + ".rewards";
            double money = plugin.getConfig().getDouble(path + ".money", 0);
            int xp = plugin.getConfig().getInt(path + ".xp", 0);
            List<String> items = plugin.getConfig().getStringList(path + ".items");
            List<String> commands = plugin.getConfig().getStringList(path + ".commands");
            for (UUID pid : participants) {
                Player player = plugin.getServer().getPlayer(pid);
                if (player == null || !player.isOnline()) continue;
                if (money > 0 && plugin.getEconomy() != null) { plugin.getEconomy().depositPlayer(player, money); player.sendMessage(langComponent("horde-reward-money", String.format("%.0f", money))); }
                if (xp > 0) player.giveExp(xp);
                for (String itemStr : items) {
                    String[] parts = itemStr.split(":");
                    if (parts.length >= 2) { Material mat = Material.getMaterial(parts[0]); if (mat != null) { var left = player.getInventory().addItem(new ItemStack(mat, Integer.parseInt(parts[1]))); left.forEach((k,v) -> player.getWorld().dropItem(player.getLocation(), v)); } }
                }
                for (String cmd : commands) plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.replace("%player%", player.getName()));
            }
        }

        private void giveCompletionRewards() {
            Component completeMsg = langComponent("horde-complete");
            broadcast(completeMsg);

            Component titleComp = plugin.getLangManager().getComponent("horde-title-complete");
            showTitleToAll(titleComp, Component.empty());

            removeBossBar();

            if (!plugin.getConfig().getBoolean("horde.global-rewards.enabled", true)) return;
            String playerList = participants.stream()
                    .map(plugin.getServer()::getPlayer)
                    .filter(p -> p != null && p.isOnline())
                    .map(Player::getName)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            for (String cmd : plugin.getConfig().getStringList("horde.global-rewards.completion-commands"))
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                    cmd.replace("%waves%", String.valueOf(totalWaves)).replace("%players%", playerList));

            List<String> completionRewardIds = plugin.getConfig().getStringList("horde.global-rewards.rewards");
            if (!completionRewardIds.isEmpty()) {
                for (UUID pid : participants) {
                    Player player = plugin.getServer().getPlayer(pid);
                    if (player != null && player.isOnline()) {
                        plugin.getRewardManager().giveRewards(completionRewardIds, player);
                        player.sendMessage(langComponent("horde-player-survived", totalWaves));
                    }
                }
            } else {
                for (UUID pid : participants) { Player player = plugin.getServer().getPlayer(pid); if (player != null && player.isOnline()) player.sendMessage(langComponent("horde-player-survived", totalWaves)); }
            }
        }

        private Location getSafeSpawnLocation(World world, double radius, double minRadius) {
            for (int attempt = 0; attempt < 10; attempt++) {
                double a = random.nextDouble() * Math.PI * 2;
                double d = minRadius + random.nextDouble() * (radius - minRadius);
                double x = center.getX() + Math.cos(a) * d;
                double z = center.getZ() + Math.sin(a) * d;
                int y = world.getHighestBlockYAt((int) x, (int) z);
                Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);
                Material ground = world.getBlockAt((int) x, y, (int) z).getType();
                if (ground == Material.LAVA || ground == Material.WATER) continue;
                Material at = loc.getBlock().getType();
                Material above = world.getBlockAt((int) x, y + 2, (int) z).getType();
                if (at.isAir() && above.isAir()) return loc;
            }
            double x = center.getX();
            double z = center.getZ();
            return new Location(world, x, world.getHighestBlockYAt((int) x, (int) z) + 1, z);
        }

        private Player getNearestPlayer(World world, Location loc) { Player n=null; double md=Double.MAX_VALUE; for(Player p:world.getPlayers()){ double dd=p.getLocation().distanceSquared(loc); if(dd<md){md=dd;n=p;} } return n; }
        public void registerParticipant(UUID playerId) { participants.add(playerId); }
        public boolean isRunning() { return running; }
        public int getCurrentWave() { return currentWave; }
        public int getTotalWaves() { return totalWaves; }
    }
}