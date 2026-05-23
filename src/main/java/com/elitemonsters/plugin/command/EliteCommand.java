package com.elitemonsters.plugin.command;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import com.elitemonsters.plugin.affix.AffixData;
import com.elitemonsters.plugin.visual.GradientUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.*;

import java.util.*;
import java.util.stream.Collectors;

public class EliteCommand implements TabExecutor {

    private final EliteMonstersPlugin plugin;
    private boolean lightningEnabled = true;

    public EliteCommand(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
        lightningEnabled = plugin.getConfig().getBoolean("spawn-effects.lightning", true);
    }

    private Component lang(String key, Object... args) { return plugin.getLangManager().getComponent(key, args); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "spawn": return handleSpawn(sender, args);
            case "reload": return handleReload(sender);
            case "info": return handleInfo(sender);
            case "list": return handleList(sender);
            case "horde": return handleHorde(sender, args);
            case "toggle": return handleToggle(sender, args);
            case "test": return handleTest(sender, args);
            case "clear": return handleClear(sender, args);
            default: sendHelp(sender); return true;
        }
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elitemonsters.command.spawn")) { sender.sendMessage(lang("no-permission")); return true; }
        if (!(sender instanceof Player player)) { sender.sendMessage(lang("player-only")); return true; }
        if (args.length < 2) { sender.sendMessage(lang("spawn-usage")); return true; }
        EntityType entityType;
        try { entityType = EntityType.valueOf(args[1].toUpperCase()); }
        catch (IllegalArgumentException e) { sender.sendMessage(lang("mob-invalid", args[1])); return true; }
        String affixKey = args.length >= 3 ? args[2].toUpperCase() : null;
        int starLevel = 0;
        if (args.length >= 4) {
            try { starLevel = Integer.parseInt(args[3]); }
            catch (NumberFormatException e) { sender.sendMessage(lang("level-invalid")); return true; }
        }
        Location spawnLoc = player.getLocation();
        if (args.length >= 6) {
            try { spawnLoc = new Location(player.getWorld(), Double.parseDouble(args[args.length-3]), Double.parseDouble(args[args.length-2]), Double.parseDouble(args[args.length-1])); }
            catch (NumberFormatException e) { plugin.getErrorLogger().log("EliteCommand", "Invalid coordinate format in spawn args", e); }
        }
        Entity entity = player.getWorld().spawnEntity(spawnLoc, entityType);
        if (!(entity instanceof LivingEntity livingEntity)) { entity.remove(); sender.sendMessage(lang("mob-cannot-spawn")); return true; }
        plugin.getGenerationListener().convertToElite(livingEntity, affixKey, starLevel);
        sender.sendMessage(lang("mob-spawned", entityType.name()));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("elitemonsters.command.reload")) { sender.sendMessage(lang("no-permission")); return true; }
        plugin.reload();
        lightningEnabled = plugin.getConfig().getBoolean("spawn-effects.lightning", true);
        sender.sendMessage(lang("config-reloaded"));
        return true;
    }

    private boolean handleToggle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elitemonsters.command.reload")) { sender.sendMessage(lang("no-permission")); return true; }
        if (args.length < 2) { sender.sendMessage(lang("toggle-usage")); return true; }
        switch (args[1].toLowerCase()) {
            case "lightning":
                lightningEnabled = !lightningEnabled;
                plugin.getConfig().set("spawn-effects.lightning", lightningEnabled);
                plugin.getVisualManager().setLightningEnabled(lightningEnabled);
                plugin.saveConfig();
                sender.sendMessage(lang(lightningEnabled ? "lightning-on" : "lightning-off"));
                break;
            case "alert":
                boolean alert = !plugin.getConfig().getBoolean("spawn-effects.global-alert", true);
                plugin.getConfig().set("spawn-effects.global-alert", alert);
                plugin.saveConfig();
                sender.sendMessage(lang(alert ? "alert-on" : "alert-off"));
                break;
            default:
                sender.sendMessage(lang("toggle-usage"));
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(lang("info-header"));
        sender.sendMessage(lang("info-affixes", plugin.getAffixManager().getAllAffixes().size()));
        sender.sendMessage(lang("info-lightning", lightningEnabled ? "ON" : "OFF"));
        for (AffixData affix : plugin.getAffixManager().getAllAffixes()) {
            sender.sendMessage(GradientUtil.parse(affix.getColor() + affix.getName() + " (" + affix.getKey() + ")"));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage(lang("player-only")); return true; }
        sender.sendMessage(lang("list-header"));
        int count = 0;
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity.hasMetadata("elitemonsters-elite") && entity instanceof LivingEntity le) {
                double dist = player.getLocation().distance(le.getLocation());
                if (dist <= 50) {
                    String customName = le.getCustomName();
                    String displayName = customName != null ? customName : le.getType().name();
                    sender.sendMessage(lang("list-entry", displayName, String.format("%.1f", dist)));
                    count++;
                }
            }
        }
        if (count == 0) sender.sendMessage(lang("no-elites-nearby"));
        return true;
    }

    private boolean handleHorde(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(lang("horde-usage")); return true; }
        switch (args[1].toLowerCase()) {
            case "start":
                if (!sender.hasPermission("elitemonsters.command.horde")) { sender.sendMessage(lang("no-permission")); return true; }
                if (!(sender instanceof Player player)) { sender.sendMessage(lang("player-only")); return true; }
                if (plugin.getHordeManager().isHordeActive()) { sender.sendMessage(lang("horde-already-active")); return true; }
                plugin.getHordeManager().startHorde(player.getLocation(), args.length >= 3 ? args[2].toUpperCase() : null);
                break;
            case "stop":
                if (!sender.hasPermission("elitemonsters.command.horde")) { sender.sendMessage(lang("no-permission")); return true; }
                if (!plugin.getHordeManager().isHordeActive()) { sender.sendMessage(lang("horde-inactive")); return true; }
                plugin.getHordeManager().stopHorde();
                break;
            case "info":
                if (plugin.getHordeManager().isHordeActive()) {
                    var horde = plugin.getHordeManager().getActiveHorde();
                    sender.sendMessage(lang("horde-active", horde.getCurrentWave(), horde.getTotalWaves()));
                } else { sender.sendMessage(lang("horde-inactive")); }
                break;
            default: sender.sendMessage(lang("horde-usage"));
        }
        return true;
    }

    
    private boolean handleTest(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elitemonsters.command.reload")) { sender.sendMessage(lang("no-permission")); return true; }
        if (args.length < 2) { sender.sendMessage(lang("test-usage")); return true; }
        switch (args[1].toLowerCase()) {
            case "info": return handleTestInfo(sender);
            case "spawn": return handleTestSpawn(sender, args);
            case "horde": return handleTestHorde(sender);
            case "loot": return handleTestLoot(sender, args);
            case "reward": return handleTestReward(sender, args);
            case "stress": return handleTestStress(sender, args);
            case "errors": return handleTestErrors(sender);
                    case "cleanup": return handleTestCleanup(sender);
            default: sender.sendMessage(lang("test-usage")); return true;
        }
    }

    
    private boolean handleTestInfo(CommandSender sender) {
        int elites = plugin.getGenerationListener().getEliteMobs().size();
        String horde = plugin.getHordeManager().isHordeActive() ? "ACTIVE wave "+plugin.getHordeManager().getActiveHorde().getCurrentWave()+"/"+plugin.getHordeManager().getActiveHorde().getTotalWaves() : "INACTIVE";
        int affixes = plugin.getAffixManager().getAllAffixes().size();
        int rewards = plugin.getRewardManager().getRewardIds().size();
        sender.sendMessage(GradientUtil.parse("<g:#FF6B35:#F7C948>=== EliteMonsters Debug ===</g>"));
        sender.sendMessage(GradientUtil.parse("&#FFAA00Elites: &#FFFFFF"+elites+"  &#FFAA00Horde: &#FFFFFF"+horde));
        sender.sendMessage(GradientUtil.parse("&#FFAA00Affixes: &#FFFFFF"+affixes+"  &#FFAA00Rewards: &#FFFFFF"+rewards));
        sender.sendMessage(GradientUtil.parse("&#FFAA00DynamicDiff: &#FFFFFF"+plugin.getConfigManager().isDynamicDifficulty()+"  &#FFAA00Debug: &#FFFFFF"+plugin.getConfigManager().isDebug()));
        long mem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        sender.sendMessage(GradientUtil.parse("&#FFAA00Memory: &#FFFFFF"+(mem/1024/1024)+"MB  &#FFAA00Threads: &#FFFFFF"+Thread.activeCount()));
        return true;
    }

    
    private boolean handleTestSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(lang("player-only")); return true; }
        String type = args.length >= 3 ? args[2] : "ZOMBIE";
        String affixKey = args.length >= 4 ? args[3] : null;
        int star = args.length >= 5 ? Integer.parseInt(args[4]) : 3;
        try {
            EntityType entityType = EntityType.valueOf(type.toUpperCase());
            Entity e = player.getWorld().spawnEntity(player.getLocation(), entityType);
            if (e instanceof LivingEntity le) {
                plugin.getGenerationListener().convertToElite(le, affixKey, star);
                sender.sendMessage(lang("mob-spawned", type+" star="+star+" affix="+(affixKey!=null?affixKey:"random")));
            } else { e.remove(); sender.sendMessage(lang("mob-cannot-spawn")); }
        } catch (Exception ex) { sender.sendMessage(GradientUtil.parse("&#FF5555Error: "+ex.getMessage())); }
        return true;
    }

    private boolean handleTestHorde(CommandSender sender) {
        if (plugin.getHordeManager().isHordeActive()) { sender.sendMessage(lang("horde-already-active")); return true; }
        if (!(sender instanceof Player player)) { sender.sendMessage(lang("player-only")); return true; }
        boolean ok = plugin.getHordeManager().startHorde(player.getLocation(), null);
        sender.sendMessage(GradientUtil.parse(ok ? "&#55FF55Horde test started!" : "&#FF5555Failed to start horde"));
        return true;
    }

    private boolean handleTestLoot(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(lang("player-only")); return true; }
        String entityType = args.length >= 3 ? args[2] : "ZOMBIE";
        String affix = args.length >= 4 ? args[3] : "FRENZY";
        int star = args.length >= 5 ? Integer.parseInt(args[4]) : 3;
        var dummyData = plugin.getGenerationListener().getEliteData(player);
        if (dummyData == null) {
            sender.sendMessage(GradientUtil.parse("&#FFAA00Spawning test mob to get EliteMobData..."));
            try {
                Entity e = player.getWorld().spawnEntity(player.getLocation(), EntityType.valueOf(entityType.toUpperCase()));
                if (e instanceof LivingEntity le) plugin.getGenerationListener().convertToElite(le, affix, star);
                sender.sendMessage(GradientUtil.parse("&#55FF55Check the spawned elite for loot!"));
            } catch (Exception ex) { sender.sendMessage(GradientUtil.parse("&#FF5555Error: "+ex.getMessage())); }
        }
        return true;
    }

    private boolean handleTestReward(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(lang("player-only")); return true; }
        if (args.length < 3) { sender.sendMessage(GradientUtil.parse("&#FF5555Usage: /elite test reward <rewardId>")); return true; }
        String rewardId = args[2];
        plugin.getRewardManager().giveReward(rewardId, player);
        sender.sendMessage(GradientUtil.parse("&#55FF55Reward "+rewardId+" given!"));
        return true;
    }

    private boolean handleTestStress(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(lang("player-only")); return true; }
        int count = args.length >= 3 ? Integer.parseInt(args[2]) : 10;
        if (count > 50) count = 50;
        long start = System.currentTimeMillis();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            try {
                Entity e = player.getWorld().spawnEntity(player.getLocation(), EntityType.ZOMBIE);
                if (e instanceof LivingEntity le) { plugin.getGenerationListener().convertToElite(le, null, 0); spawned++; }
            } catch (Exception ignored) {}
        }
        long elapsed = System.currentTimeMillis() - start;
        sender.sendMessage(GradientUtil.parse("&#55FF55Stress test: "+spawned+"/"+count+" elites in "+elapsed+"ms"));
        return true;
    }

    

    private boolean handleTestErrors(CommandSender sender) {
        var errors = plugin.getErrorLogger().getRecentErrors();
        int total = plugin.getErrorLogger().getTotalErrors();
        sender.sendMessage(GradientUtil.parse("<g:#FF6B35:#F7C948>=== Error Log (total: "+total+") ===</g>"));
        if (errors.isEmpty()) { sender.sendMessage(GradientUtil.parse("&#55FF55No errors recorded")); return true; }
        for (var e : errors) {
            sender.sendMessage(GradientUtil.parse("&#FFAA00["+e.time()+"] &#FF5555"+e.context()+"&#AAAAAA: "+e.message()));
        }
        sender.sendMessage(GradientUtil.parse("&#AAAAAAFile: plugins/EliteMonsters/errors.log"));
        return true;
    }

    private boolean handleTestCleanup(CommandSender sender) {
        int before = plugin.getGenerationListener().getEliteMobs().size();
        plugin.getGenerationListener().revertAllElites();
        int after = plugin.getGenerationListener().getEliteMobs().size();
        sender.sendMessage(GradientUtil.parse("&#55FF55Cleanup: "+before+" -> "+after+" elites"));
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elitemonsters.command.clear")) { sender.sendMessage(lang("no-permission")); return true; }

        // Parse args: /elite clear [range|chunk|world <name>|type <mob>] ...
        Double radius = null;
        boolean chunkOnly = false;
        World targetWorld = null;
        String mobType = null;

        int i = 1;
        while (i < args.length) {
            String arg = args[i].toLowerCase();
            switch (arg) {
                case "chunk":
                    chunkOnly = true;
                    i++;
                    break;
                case "world":
                    if (i + 1 < args.length) {
                        targetWorld = plugin.getServer().getWorld(args[i + 1]);
                        if (targetWorld == null) {
                            sender.sendMessage(lang("clear-world-not-found", args[i + 1]));
                            return true;
                        }
                        i += 2;
                    } else {
                        sender.sendMessage(lang("clear-usage"));
                        return true;
                    }
                    break;
                case "type":
                    if (i + 1 < args.length) {
                        mobType = args[i + 1].toUpperCase();
                        try { EntityType.valueOf(mobType); }
                        catch (IllegalArgumentException e) {
                            sender.sendMessage(lang("mob-invalid", args[i + 1]));
                            return true;
                        }
                        i += 2;
                    } else {
                        sender.sendMessage(lang("clear-usage"));
                        return true;
                    }
                    break;
                default:
                    try {
                        radius = Double.parseDouble(arg);
                        i++;
                    } catch (NumberFormatException e) {
                        sender.sendMessage(lang("clear-usage"));
                        return true;
                    }
                    break;
            }
        }

        // Determine search scope
        Location center = null;
        List<World> worlds = new ArrayList<>();

        if (targetWorld != null) {
            worlds.add(targetWorld);
        } else if (sender instanceof Player player) {
            center = player.getLocation();
            worlds.add(player.getWorld());
        } else {
            worlds.addAll(plugin.getServer().getWorlds());
        }

        // Count and remove
        int removed = 0;
        for (World world : worlds) {
            for (Entity entity : world.getEntities()) {
                if (!entity.hasMetadata("elitemonsters-elite")) continue;
                if (!(entity instanceof LivingEntity)) continue;

                // Filter by type
                if (mobType != null && entity.getType().name().equalsIgnoreCase(mobType)) continue;

                // Filter by range
                if (radius != null && center != null) {
                    if (!entity.getWorld().equals(center.getWorld())) continue;
                    if (entity.getLocation().distance(center) > radius) continue;
                }

                // Filter by chunk
                if (chunkOnly && center != null) {
                    Chunk centerChunk = center.getChunk();
                    Chunk entityChunk = entity.getLocation().getChunk();
                    if (!centerChunk.equals(entityChunk)) continue;
                }

                entity.remove();
                removed++;
            }
        }

        sender.sendMessage(lang("clear-success", removed));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(lang("help-header"));
        sender.sendMessage(lang("help-spawn"));
        sender.sendMessage(lang("help-reload"));
        sender.sendMessage(lang("help-info"));
        sender.sendMessage(lang("help-list"));
        sender.sendMessage(lang("help-horde"));
        sender.sendMessage(lang("help-toggle"));
        sender.sendMessage(lang("help-test"));
        sender.sendMessage(lang("help-clear"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("spawn","reload","info","list","horde","toggle","test","clear"), args[0]);
        if (args.length >= 2 && args[0].equalsIgnoreCase("horde")) {
            if (args.length == 2) return filter(List.of("start","stop","info"), args[1]);
            return Collections.emptyList();
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("toggle")) {
            if (args.length == 2) return filter(List.of("lightning","alert"), args[1]);
            return Collections.emptyList();
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("spawn")) {
            if (args.length == 2) return filter(Arrays.stream(EntityType.values()).map(Enum::name).collect(Collectors.toList()), args[1]);
            if (args.length == 3) return filter(plugin.getAffixManager().getAllAffixes().stream().map(AffixData::getKey).collect(Collectors.toList()), args[2]);
            if (args.length == 4) return filter(List.of("1","2","3","4","5"), args[3]);
            return Collections.emptyList();
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("test")) {
            if (args.length == 2) return filter(List.of("info","spawn","horde","loot","reward","stress","errors","cleanup"), args[1]);
            if (args.length >= 3 && args[1].equalsIgnoreCase("spawn")) {
                if (args.length == 3) return filter(Arrays.stream(EntityType.values()).map(Enum::name).collect(Collectors.toList()), args[2]);
                if (args.length == 4) return filter(plugin.getAffixManager().getAllAffixes().stream().map(AffixData::getKey).collect(Collectors.toList()), args[3]);
                return filter(List.of("1","2","3","4","5"), args[args.length-1]);
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("loot")) {
                if (args.length == 3) return filter(Arrays.stream(EntityType.values()).map(Enum::name).collect(Collectors.toList()), args[2]);
                if (args.length == 4) return filter(plugin.getAffixManager().getAllAffixes().stream().map(AffixData::getKey).collect(Collectors.toList()), args[3]);
                return filter(List.of("1","2","3","4","5"), args[args.length-1]);
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("reward")) {
                return filter(new ArrayList<>(plugin.getRewardManager().getRewardIds()), args[2]);
            }
            return Collections.emptyList();
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("clear")) {
            List<String> options = new ArrayList<>();
            options.add("chunk");
            options.add("world");
            options.add("type");
            // Check previous arg
            String prev = args.length >= 2 ? args[args.length - 2].toLowerCase() : "";
            if (prev.equals("world")) {
                return filter(plugin.getServer().getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[args.length - 1]);
            }
            if (prev.equals("type")) {
                return filter(Arrays.stream(EntityType.values()).map(Enum::name).collect(Collectors.toList()), args[args.length - 1]);
            }
            return filter(options, args[args.length - 1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
}