package com.elitemonsters.plugin;

import com.elitemonsters.plugin.generation.EliteMobData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class LootManager {
    private final EliteMonstersPlugin plugin;
    private final List<LootTable> tables = new ArrayList<>();
    private final Random random = new Random();

    public LootManager(EliteMonstersPlugin plugin) { this.plugin = plugin; }

    @SuppressWarnings("unchecked")
    public void load() {
        tables.clear();
        File file = new File(plugin.getDataFolder(), "loot.yml");
        if (!file.exists()) plugin.saveResource("loot.yml", false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("loot-tables");
        if (section == null) { plugin.getLogger().warning("loot.yml missing loot-tables section"); return; }
        for (String key : section.getKeys(false)) {
            ConfigurationSection sec = section.getConfigurationSection(key);
            if (sec == null || !sec.getBoolean("enabled", true)) continue;
            LootTable table = new LootTable(key);
            table.entityTypes = sec.getStringList("entity-types").stream().map(String::toUpperCase).toList();
            table.affixes = sec.getStringList("affixes").stream().map(String::toUpperCase).toList();
            table.minStar = sec.getInt("min-star", 0);
            List<Map<?,?>> dropList = sec.getMapList("drops");
            for (Map<?,?> dropMap : dropList) {
                String matStr = dropMap.get("material") instanceof String s ? s : "STONE";
                Material mat = Material.getMaterial(matStr.toUpperCase());
                if (mat == null) continue;
                double chance = dropMap.get("chance") instanceof Number n ? n.doubleValue() : 1.0;
                Object amt = dropMap.get("amount");
                int minAmt, maxAmt;
                if (amt instanceof Number n) { minAmt = maxAmt = n.intValue(); }
                else { minAmt = dropMap.get("min-amount") instanceof Number n2 ? n2.intValue() : 1; maxAmt = dropMap.get("max-amount") instanceof Number n3 ? n3.intValue() : minAmt; }
                table.drops.add(new LootDrop(mat, chance, minAmt, maxAmt));
            }
            tables.add(table);
        }
        plugin.getLogger().info("LootManager loaded " + tables.size() + " loot tables");
    }

    public void rollLoot(EliteMobData data, LivingEntity entity, Player killer) {
        String mobType = entity.getType().getKey().getKey().toUpperCase();
        int star = data.getStarLevel();
        String affix = data.getAffix().getKey().toUpperCase();
        for (LootTable table : tables) {
            if (!table.matches(mobType, affix, star)) continue;
            for (LootDrop drop : table.drops) {
                if (random.nextDouble() >= drop.chance) continue;
                int amount = drop.minAmount == drop.maxAmount ? drop.minAmount : drop.minAmount + random.nextInt(drop.maxAmount - drop.minAmount + 1);
                if (amount <= 0) continue;
                ItemStack item = new ItemStack(drop.material, Math.min(amount, drop.material.getMaxStackSize()));
                entity.getWorld().dropItemNaturally(entity.getLocation(), item);
            }
            return; // first match only
        }
    }

    private static class LootTable {
        final String name;
        List<String> entityTypes = List.of();
        List<String> affixes = List.of();
        int minStar = 0;
        final List<LootDrop> drops = new ArrayList<>();
        LootTable(String name) { this.name = name; }
        boolean matches(String mobType, String affix, int star) {
            if (minStar > 0 && star < minStar) return false;
            if (!entityTypes.isEmpty() && !entityTypes.contains(mobType)) return false;
            if (!affixes.isEmpty() && !affixes.contains(affix)) return false;
            return true;
        }
    }

    private record LootDrop(Material material, double chance, int minAmount, int maxAmount) {}
}