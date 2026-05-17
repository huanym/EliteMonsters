package com.elitemonsters.plugin.reward;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.*;

public class ItemRewardData extends RewardData {
    private final Material material;
    private final int minAmount;
    private final int maxAmount;
    private final String name;
    private final List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private final int customModelData;
    private final String skullTexture;

    @SuppressWarnings("unchecked")
    public ItemRewardData(Map<?, ?> map) {
        super(map);
        String matStr = map.get("material") instanceof String s ? s : "STONE";
        this.material = Material.getMaterial(matStr.toUpperCase());
        if (this.material == null) throw new IllegalArgumentException("Invalid material: " + matStr);

        Object amtObj = map.get("amount");
        if (amtObj instanceof Number n) {
            this.minAmount = n.intValue();
            this.maxAmount = n.intValue();
        } else {
            Object minObj = map.get("min-amount");
            Object maxObj = map.get("max-amount");
            this.minAmount = minObj instanceof Number n ? n.intValue() : 1;
            this.maxAmount = maxObj instanceof Number n ? n.intValue() : minAmount;
        }

        this.name = map.get("name") instanceof String s ? s : null;
        this.lore = map.get("lore") instanceof List<?> l ? l.stream().map(Object::toString).toList() : List.of();
        this.customModelData = map.get("custom_model_data") instanceof Number n ? n.intValue() : 0;
        this.skullTexture = map.get("skull_texture") instanceof String s ? s : null;

        this.enchantments = new HashMap<>();
        Object enchObj = map.get("enchantments");
        if (enchObj instanceof Map<?, ?> enchMap) {
            for (var entry : enchMap.entrySet()) {
                String enchName = entry.getKey().toString().toLowerCase();
                Enchantment ench = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft(enchName));
                if (ench != null && entry.getValue() instanceof Number num) {
                    enchantments.put(ench, num.intValue());
                }
            }
        }
    }

    @Override
    public void give(Player player, EliteMonstersPlugin plugin) {
        Random random = new Random();
        int amount = minAmount == maxAmount ? minAmount : minAmount + random.nextInt(maxAmount - minAmount + 1);
        if (amount <= 0) return;

        ItemStack item = new ItemStack(material, Math.min(amount, material.getMaxStackSize()));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (name != null) {
                meta.displayName(net.kyori.adventure.text.Component.text(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', name)));
            }
            if (!lore.isEmpty()) {
                List<net.kyori.adventure.text.Component> loreComps = new ArrayList<>();
                for (String line : lore) {
                    loreComps.add(net.kyori.adventure.text.Component.text(
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', line)));
                }
                meta.lore(loreComps);
            }
            for (var entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            if (skullTexture != null && !skullTexture.isEmpty() && meta instanceof SkullMeta skullMeta) {
                try {
                    PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                    PlayerTextures textures = profile.getTextures();
                    String url = skullTexture.startsWith("http") ? skullTexture
                        : "http://textures.minecraft.net/texture/" + skullTexture;
                    textures.setSkin(new URL(url));
                    profile.setTextures(textures);
                    skullMeta.setOwnerProfile(profile);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to set skull texture: " + e.getMessage());
                }
            }

            item.setItemMeta(meta);
        }

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack remaining : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), remaining);
        }
    }
}