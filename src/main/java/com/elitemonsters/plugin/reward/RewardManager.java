package com.elitemonsters.plugin.reward;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class RewardManager {

    private final EliteMonstersPlugin plugin;
    private final Map<String, RewardData> rewards = new LinkedHashMap<>();
    private final Random random = new Random();
    private File rewardsFile;
    private FileConfiguration rewardsConfig;

    public RewardManager(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void load() {
        rewards.clear();
        rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);

        ConfigurationSection section = rewardsConfig.getConfigurationSection("rewards");
        if (section == null) {
            plugin.getLogger().warning("rewards.yml does not contain 'rewards' section");
            return;
        }

        int loaded = 0;
        for (String key : section.getKeys(false)) {
            try {
                Object value = section.get(key);
                if (value instanceof Map<?, ?> map) {
                    rewards.put(key, RewardData.parse(map));
                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to parse reward '" + key + "': " + e.getMessage());
            }
        }
        plugin.getLogger().info("RewardManager loaded " + loaded + " rewards");
    }

    public RewardData getReward(String id) {
        return rewards.get(id);
    }

    public Set<String> getRewardIds() {
        return Collections.unmodifiableSet(rewards.keySet());
    }

    public void giveReward(String id, Player player) {
        RewardData reward = rewards.get(id);
        if (reward == null) {
            plugin.getLogger().warning("Unknown reward: " + id);
            return;
        }
        if (reward.rollChance(random)) {
            reward.give(player, plugin);
        }
    }

    public void giveRewards(List<String> ids, Player player) {
        for (String id : ids) {
            giveReward(id, player);
        }
    }

    public void save() {
        if (rewardsConfig != null && rewardsFile != null) {
            try {
                rewardsConfig.save(rewardsFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save rewards.yml: " + e.getMessage());
            }
        }
    }

    public void reload() { load(); }
}