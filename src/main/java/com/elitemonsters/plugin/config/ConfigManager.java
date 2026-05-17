package com.elitemonsters.plugin.config;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {

    private final EliteMonstersPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    private double baseEliteChance;
    private Map<String, Double> heightMultipliers;
    private Map<String, Double> biomeMultipliers;
    private Map<String, Double> timeMultipliers;
    private Map<String, Double> difficultyMultipliers;
    private List<String> blacklistMobs;
    private List<String> whitelistMobs;
    private boolean useWhitelist;

    private int maxStarLevel;
    private Map<Integer, Double> starAttributeMultipliers;
    private Map<Integer, Integer> starSkillCount;

    public ConfigManager(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        baseEliteChance = config.getDouble("generation.base-chance", 0.05);

        heightMultipliers = new HashMap<>();
        if (config.getConfigurationSection("generation.conditions.height") != null) {
            for (String key : config.getConfigurationSection("generation.conditions.height").getKeys(false)) {
                heightMultipliers.put(key, config.getDouble("generation.conditions.height." + key, 1.0));
            }
        }

        biomeMultipliers = new HashMap<>();
        if (config.getConfigurationSection("generation.conditions.biome") != null) {
            for (String key : config.getConfigurationSection("generation.conditions.biome").getKeys(false)) {
                biomeMultipliers.put(key.toUpperCase(), config.getDouble("generation.conditions.biome." + key, 1.0));
            }
        }

        timeMultipliers = new HashMap<>();
        if (config.getConfigurationSection("generation.conditions.time") != null) {
            for (String key : config.getConfigurationSection("generation.conditions.time").getKeys(false)) {
                timeMultipliers.put(key, config.getDouble("generation.conditions.time." + key, 1.0));
            }
        }

        difficultyMultipliers = new HashMap<>();
        if (config.getConfigurationSection("generation.conditions.difficulty") != null) {
            for (String key : config.getConfigurationSection("generation.conditions.difficulty").getKeys(false)) {
                difficultyMultipliers.put(key, config.getDouble("generation.conditions.difficulty." + key, 1.0));
            }
        }

        blacklistMobs = config.getStringList("generation.blacklist");
        whitelistMobs = config.getStringList("generation.whitelist");
        useWhitelist = config.getBoolean("generation.use-whitelist", false);

        maxStarLevel = config.getInt("star-system.max-level", 5);
        starAttributeMultipliers = new HashMap<>();
        starSkillCount = new HashMap<>();
        for (int i = 1; i <= maxStarLevel; i++) {
            starAttributeMultipliers.put(i, config.getDouble("star-system.levels." + i + ".attribute-multiplier", 1.0 + (i - 1) * 0.5));
            starSkillCount.put(i, config.getInt("star-system.levels." + i + ".skill-count", i));
        }
    }

    public double getEliteChance(String mobType, String biome, double y, String time, String difficulty) {
        if (useWhitelist && !whitelistMobs.contains(mobType)) return 0.0;
        if (!useWhitelist && blacklistMobs.contains(mobType)) return 0.0;

        double chance = baseEliteChance;

        if (heightMultipliers.containsKey("below_0") && y < 0) chance *= heightMultipliers.get("below_0");
        else if (heightMultipliers.containsKey("below_32") && y < 32) chance *= heightMultipliers.get("below_32");
        else if (heightMultipliers.containsKey("below_64") && y < 64) chance *= heightMultipliers.get("below_64");
        else if (heightMultipliers.containsKey("above_128") && y > 128) chance *= heightMultipliers.get("above_128");
        else if (heightMultipliers.containsKey("default")) chance *= heightMultipliers.get("default");

        if (biomeMultipliers.containsKey(biome.toUpperCase())) {
            chance *= biomeMultipliers.get(biome.toUpperCase());
        }

        if (timeMultipliers.containsKey(time)) {
            chance *= timeMultipliers.get(time);
        }

        if (difficultyMultipliers.containsKey(difficulty)) {
            chance *= difficultyMultipliers.get(difficulty);
        }

        return Math.min(chance, 1.0);
    }

    public int getMaxStarLevel() { return maxStarLevel; }
    public double getStarAttributeMultiplier(int star) { return starAttributeMultipliers.getOrDefault(star, 1.0); }
    public int getStarSkillCount(int star) { return starSkillCount.getOrDefault(star, star); }

    public void save() {
        try { config.save(configFile); } catch (IOException e) { plugin.getLogger().warning("鏃犳硶淇濆瓨閰嶇疆: " + e.getMessage()); }
    }

    public void reload() { load(); }
}
