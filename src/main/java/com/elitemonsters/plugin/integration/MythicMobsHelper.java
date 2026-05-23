package com.elitemonsters.plugin.integration;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.List;

public class MythicMobsHelper {

    private final EliteMonstersPlugin plugin;
    private boolean enabled = false;
    private double chance = 0.0;
    private List<String> blacklist = List.of();
    private Method adaptMethod;
    private Method getTypeMethod;

    public MythicMobsHelper(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") == null) {
            plugin.getLogger().info("MythicMobs not found, mythic mob elite conversion disabled");
            enabled = false;
            return;
        }
        try {
            Class<?> bukkitAdapterClass = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
            adaptMethod = bukkitAdapterClass.getMethod("adapt", LivingEntity.class);
            Class<?> activeMobClass = Class.forName("io.lumine.mythic.core.mobs.ActiveMob");
            getTypeMethod = activeMobClass.getMethod("getType");

            enabled = plugin.getConfig().getBoolean("mythicmobs.enabled", false);
            chance = plugin.getConfig().getDouble("mythicmobs.elite-chance", 0.05);
            blacklist = plugin.getConfig().getStringList("mythicmobs.blacklist").stream().map(String::toLowerCase).toList();
            plugin.getLogger().info("MythicMobs integration enabled (chance=" + chance + ", blacklist=" + blacklist.size() + ")");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize MythicMobs integration: " + e.getMessage());
            enabled = false;
        }
    }

    public boolean isMythicMob(LivingEntity entity) {
        if (!enabled) return false;
        try {
            Object activeMob = adaptMethod.invoke(null, entity);
            return activeMob != null;
        } catch (Exception e) {
            return false;
        }
    }

    public String getMythicMobType(LivingEntity entity) {
        if (!enabled) return null;
        try {
            Object activeMob = adaptMethod.invoke(null, entity);
            if (activeMob == null) return null;
            return (String) getTypeMethod.invoke(activeMob);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean canBecomeElite(LivingEntity entity) {
        if (!enabled) return false;
        try {
            Object activeMob = adaptMethod.invoke(null, entity);
            if (activeMob == null) return false;
            String mobType = ((String) getTypeMethod.invoke(activeMob)).toLowerCase();
            if (blacklist.contains(mobType)) return false;
            return Math.random() < chance;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isEnabled() { return enabled; }
    public double getChance() { return chance; }
}