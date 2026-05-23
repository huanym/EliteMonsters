package com.elitemonsters.plugin.integration;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.List;

public class WorldGuardHelper {

    private final EliteMonstersPlugin plugin;
    private Object regionContainer;
    private Method createQueryMethod;
    private Method getApplicableRegionsMethod;
    private Method getRegionsMethod;
    private Method getIdMethod;
    private Method adaptMethod;
    private boolean enabled = false;
    private boolean whitelistMode = false;
    private List<String> regionList = List.of();
    private boolean affectHorde = true;

    public WorldGuardHelper(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            plugin.getLogger().info("WorldGuard not found, region control disabled");
            enabled = false;
            return;
        }
        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wgInstance = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuardClass.getMethod("getPlatform").invoke(wgInstance);
            regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            createQueryMethod = regionContainer.getClass().getMethod("createQuery");

            Class<?> regionQueryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
            Class<?> applicableRegionSetClass = Class.forName("com.sk89q.worldguard.protection.ApplicableRegionSet");
            Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");

            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            adaptMethod = bukkitAdapterClass.getMethod("adapt", World.class);

            getApplicableRegionsMethod = regionQueryClass.getMethod("getApplicableRegions", Class.forName("com.sk89q.worldedit.util.Location"));
            getRegionsMethod = applicableRegionSetClass.getMethod("getRegions");
            getIdMethod = protectedRegionClass.getMethod("getId");

            enabled = plugin.getConfig().getBoolean("generation.worldguard.enabled", false);
            whitelistMode = plugin.getConfig().getString("generation.worldguard.mode", "blacklist").equalsIgnoreCase("whitelist");
            regionList = plugin.getConfig().getStringList("generation.worldguard.regions").stream().map(String::toLowerCase).toList();
            affectHorde = plugin.getConfig().getBoolean("generation.worldguard.affect-horde", true);
            plugin.getLogger().info("WorldGuard integration enabled (" + (whitelistMode ? "whitelist" : "blacklist") + " mode, " + regionList.size() + " regions)");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize WorldGuard integration: " + e.getMessage());
            enabled = false;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean isRegionBlocked(Location location) {
        if (!enabled || regionContainer == null || regionList.isEmpty()) return false;
        try {
            Object query = createQueryMethod.invoke(regionContainer);
            Object weWorld = adaptMethod.invoke(null, location.getWorld());
            Class<?> weLocClass = Class.forName("com.sk89q.worldedit.util.Location");
            Object weLoc = weLocClass.getConstructor(weWorld.getClass(), double.class, double.class, double.class)
                .newInstance(weWorld, location.getX(), location.getY(), location.getZ());
            Object regionSet = getApplicableRegionsMethod.invoke(query, weLoc);
            Iterable<Object> regions = (Iterable<Object>) getRegionsMethod.invoke(regionSet);
            for (Object region : regions) {
                String regionId = ((String) getIdMethod.invoke(region)).toLowerCase();
                boolean inList = regionList.contains(regionId);
                if (whitelistMode ? !inList : inList) return true;
            }
            return whitelistMode;
        } catch (Exception e) {
            plugin.getErrorLogger().log("WorldGuardHelper", "Failed to check region", e);
            return false;
        }
    }

    public boolean isEnabled() { return enabled; }
    public boolean isAffectHorde() { return affectHorde; }
}