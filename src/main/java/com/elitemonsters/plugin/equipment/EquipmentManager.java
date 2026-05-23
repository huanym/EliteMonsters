package com.elitemonsters.plugin.equipment;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import com.elitemonsters.plugin.generation.EliteMobData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class EquipmentManager {

    private final EliteMonstersPlugin plugin;
    private final List<EquipmentTemplate> templates = new ArrayList<>();
    private final Random random = new Random();

    public EquipmentManager(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void load() {
        templates.clear();
        File file = new File(plugin.getDataFolder(), "equipment.yml");
        if (!file.exists()) {
            plugin.saveResource("equipment.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("equipment");
        if (section == null) {
            plugin.getLogger().warning("equipment.yml missing equipment section");
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection sec = section.getConfigurationSection(key);
            if (sec == null || !sec.getBoolean("enabled", true)) continue;

            EquipmentTemplate template = new EquipmentTemplate();
            template.key = key;
            template.minStar = sec.getInt("min-star", 1);
            template.maxStar = sec.getInt("max-star", 5);
            template.affixes = sec.getStringList("affixes").stream().map(String::toUpperCase).toList();
            template.nameFormat = sec.getString("name", "");
            template.lore = sec.getStringList("lore");
            template.chance = sec.getDouble("chance", 0.1);

            String matStr = sec.getString("material", "STONE");
            template.material = Material.getMaterial(matStr.toUpperCase());
            if (template.material == null) {
                plugin.getLogger().warning("Invalid material: " + matStr + " in equipment " + key);
                continue;
            }

            ConfigurationSection enchSec = sec.getConfigurationSection("enchantments");
            if (enchSec != null) {
                for (String enchName : enchSec.getKeys(false)) {
                    NamespacedKey nk = NamespacedKey.minecraft(enchName.toLowerCase());
                    Enchantment ench = Registry.ENCHANTMENT.get(nk);
                    if (ench != null) {
                        template.enchantments.put(ench, enchSec.getInt(enchName));
                    }
                }
            }
            templates.add(template);
        }
        plugin.getLogger().info("EquipmentManager loaded " + templates.size() + " templates");
    }

    public void rollEquipment(EliteMobData data, LivingEntity entity, Player killer) {
        if (templates.isEmpty()) return;
        int star = data.getStarLevel();
        String affixKey = data.getAffix().getKey().toUpperCase();

        for (EquipmentTemplate template : templates) {
            if (star < template.minStar || star > template.maxStar) continue;
            if (!template.affixes.isEmpty() && !template.affixes.contains(affixKey)) continue;
            if (random.nextDouble() >= template.chance) continue;

            ItemStack item = createItem(template, data, killer);
            entity.getWorld().dropItemNaturally(entity.getLocation(), item);
        }
    }

    private ItemStack createItem(EquipmentTemplate template, EliteMobData data, Player killer) {
        ItemStack item = new ItemStack(template.material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String affixName = data.getAffix().getName();
        String starStr = plugin.getConfigManager().getStarChar().repeat(data.getStarLevel());

        String displayName = template.nameFormat
            .replace("%affix%", affixName)
            .replace("%star%", starStr)
            .replace("%killer%", killer != null ? killer.getName() : "???");

        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));

        List<String> formattedLore = new ArrayList<>();
        for (String line : template.lore) {
            formattedLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                line.replace("%affix%", affixName).replace("%star%", starStr)));
        }
        meta.setLore(formattedLore);

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        for (var entry : template.enchantments.entrySet()) {
            item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        return item;
    }

    private static class EquipmentTemplate {
        String key;
        int minStar, maxStar;
        List<String> affixes = List.of();
        Material material;
        String nameFormat;
        List<String> lore = List.of();
        double chance;
        final Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
    }
}