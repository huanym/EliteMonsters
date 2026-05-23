package com.elitemonsters.plugin.gui;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import com.elitemonsters.plugin.affix.AffixData;
import com.elitemonsters.plugin.generation.EliteMobData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class EliteGUI implements Listener {

    private final EliteMonstersPlugin plugin;
    private final NamespacedKey guiKey;
    private final NamespacedKey actionKey;
    private final Map<UUID, Map<String, Object>> menuData = new HashMap<>();
    private static final Component TITLE = Component.text("EliteMonsters 管理", TextColor.color(0xFFD700));

    public EliteGUI(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
        this.guiKey = new NamespacedKey(plugin, "elite_gui");
        this.actionKey = new NamespacedKey(plugin, "gui_action");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        inv.setItem(11, guiItem(Material.ZOMBIE_HEAD, "生成精英怪", "spawn_mob"));
        inv.setItem(12, guiItem(Material.SKELETON_SKULL, "尸潮控制", "horde"));
        inv.setItem(13, guiItem(Material.COMMAND_BLOCK, "设置", "settings"));
        inv.setItem(14, guiItem(Material.CRAFTING_TABLE, "快捷生成", "quick"));
        inv.setItem(15, guiItem(Material.ENCHANTED_BOOK, "精英怪列表", "list"));
        inv.setItem(26, guiItem(Material.BARRIER, "关闭", "close"));
        player.openInventory(inv);
    }

    private void openSpawnMobType(Player player, int page) {
        List<EntityType> mobTypes = Arrays.stream(EntityType.values())
            .filter(t -> t.isAlive() && t.isSpawnable())
            .filter(t -> t != EntityType.PLAYER && t != EntityType.ARMOR_STAND && t != EntityType.GIANT && t != EntityType.ILLUSIONER)
            .toList();

        int perPage = 36;
        int totalPages = (mobTypes.size() + perPage - 1) / perPage;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("生物类型 " + (page+1) + "/" + totalPages));

        int start = page * perPage;
        int end = Math.min(start + perPage, mobTypes.size());
        for (int i = start; i < end; i++) {
            EntityType type = mobTypes.get(i);
            Material icon = safeMaterial(type.name() + "_SPAWN_EGG", Material.EGG);
            inv.addItem(guiItem(icon, type.name(), "mob:" + type.name()));
        }

        if (page > 0) inv.setItem(45, guiItem(Material.ARROW, "上一页", "page_spawn:" + (page - 1)));
        if (page < totalPages - 1) inv.setItem(53, guiItem(Material.ARROW, "下一页", "page_spawn:" + (page + 1)));
        inv.setItem(49, guiItem(Material.BARRIER, "返回", "back"));

        menuData.put(player.getUniqueId(), Map.of("page", page));
        player.openInventory(inv);
    }

    private void openSpawnAffix(Player player, EntityType mobType) {
        List<AffixData> affixes = new ArrayList<>(plugin.getAffixManager().getAllAffixes());
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("词缀 - " + mobType.name()));
        for (int i = 0; i < Math.min(affixes.size(), 18); i++) {
            AffixData a = affixes.get(i);
            inv.setItem(i, guiItem(Material.ENCHANTED_BOOK, a.getName(), "affix:" + a.getKey()));
        }
        inv.setItem(22, guiItem(Material.BOOK, "随机词缀", "affix:random"));
        inv.setItem(26, guiItem(Material.BARRIER, "返回", "back"));
        menuData.put(player.getUniqueId(), Map.of("mobType", mobType.name()));
        player.openInventory(inv);
    }

    private void openSpawnStar(Player player, EntityType mobType, String affixKey) {
        int maxStar = plugin.getConfigManager().getMaxStarLevel();
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("星级 - " + mobType.name()));
        Material[] mats = {Material.LEATHER, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE};
        for (int i = 0; i < maxStar; i++) {
            inv.setItem(i, guiItem(mats[i], (i+1) + "星", "spawn:" + mobType.name() + ":" + affixKey + ":" + (i+1)));
        }
        inv.setItem(8, guiItem(Material.BARRIER, "返回", "back"));
        menuData.put(player.getUniqueId(), Map.of("mobType", mobType.name(), "affixKey", affixKey));
        player.openInventory(inv);
    }

    private void openEliteList(Player player, int page) {
        List<EliteMobData> elites = plugin.getGenerationListener().getEliteMobs().values().stream()
            .filter(d -> d.getEntity() != null && d.getEntity().getWorld().equals(player.getWorld()))
            .sorted(Comparator.comparingDouble(d -> d.getEntity().getLocation().distanceSquared(player.getLocation())))
            .toList();

        int perPage = 45;
        int totalPages = Math.max(1, (elites.size() + perPage - 1) / perPage);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("精英怪 " + (page+1) + "/" + totalPages));

        int start = page * perPage;
        int end = Math.min(start + perPage, elites.size());
        for (int i = start; i < end; i++) {
            EliteMobData d = elites.get(i);
            LivingEntity e = d.getEntity();
            double dist = Math.sqrt(e.getLocation().distanceSquared(player.getLocation()));
            inv.addItem(guiItem(Material.SKELETON_SKULL, d.getBaseDisplayName(),
                "info: " + String.format("%.0fm %s %d星", dist, d.getAffix().getName(), d.getStarLevel())));
        }

        if (page > 0) inv.setItem(45, guiItem(Material.ARROW, "上一页", "page_list:" + (page-1)));
        if (page < totalPages - 1) inv.setItem(53, guiItem(Material.ARROW, "下一页", "page_list:" + (page+1)));
        inv.setItem(49, guiItem(Material.BARRIER, "返回", "back"));
        menuData.put(player.getUniqueId(), Map.of("page", page));
        player.openInventory(inv);
    }

    private void openHordeControl(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("尸潮控制"));
        boolean active = plugin.getHordeManager().isHordeActive();
        inv.setItem(2, guiItem(Material.GREEN_WOOL, "启动尸潮", "horde:start"));
        inv.setItem(4, guiItem(Material.RED_WOOL, "停止尸潮", "horde:stop"));
        inv.setItem(6, guiItem(Material.CLOCK, active ? "进行中" : "未激活", "horde:status"));
        inv.setItem(8, guiItem(Material.BARRIER, "返回", "back"));
        player.openInventory(inv);
    }

    private void openSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("设置"));
        boolean debug = plugin.getConfig().getBoolean("debug", false);
        boolean dynDiff = plugin.getConfig().getBoolean("generation.dynamic-difficulty", false);
        inv.setItem(0, guiItem(debug ? Material.LIME_DYE : Material.GRAY_DYE, "Debug: " + (debug?"开":"关"), "toggle:debug"));
        inv.setItem(1, guiItem(dynDiff ? Material.LIME_DYE : Material.GRAY_DYE, "动态难度: " + (dynDiff?"开":"关"), "toggle:dyn"));
        inv.setItem(8, guiItem(Material.BARRIER, "返回", "back"));
        player.openInventory(inv);
    }

    private void openQuickSpawn(Player player) {
        Inventory inv = Bukkit.createInventory(null, 18, Component.text("快捷生成"));
        inv.setItem(0, guiItem(Material.ZOMBIE_HEAD, "1星僵尸", "spawn:ZOMBIE:random:1"));
        inv.setItem(1, guiItem(Material.SKELETON_SKULL, "3星骷髅", "spawn:SKELETON:random:3"));
        inv.setItem(2, guiItem(Material.CREEPER_HEAD, "5星苦力怕", "spawn:CREEPER:random:5"));
        inv.setItem(3, guiItem(Material.WITHER_SKELETON_SKULL, "5星凋零骷髅", "spawn:WITHER_SKELETON:random:5"));
        inv.setItem(9, guiItem(Material.REDSTONE, "吸血伯爵", "spawn:ZOMBIE:VAMPIRIC:3"));
        inv.setItem(10, guiItem(Material.BLAZE_POWDER, "炎魔", "spawn:SKELETON:FLAMING:3"));
        inv.setItem(11, guiItem(Material.TNT, "爆破鬼才", "spawn:CREEPER:BOMBARDING:3"));
        inv.setItem(17, guiItem(Material.BARRIER, "返回", "back"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() == null)) return; // only our GUIs
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        String action = getAction(item);
        if (action == null) return;

        if (action.equals("close")) { player.closeInventory(); return; }
        if (action.equals("back")) { openMainMenu(player); return; }

        if (action.startsWith("mob:")) {
            String typeName = action.substring(4);
            try { openSpawnAffix(player, EntityType.valueOf(typeName)); }
            catch (IllegalArgumentException ignored) {}
        } else if (action.startsWith("affix:")) {
            String affix = action.substring(6);
            Map<String,Object> d = menuData.get(player.getUniqueId());
            if (d != null) {
                EntityType mt = EntityType.valueOf((String)d.get("mobType"));
                openSpawnStar(player, mt, affix.equals("random") ? null : affix);
            }
        } else if (action.startsWith("spawn:")) {
            String[] parts = action.substring(6).split(":");
            if (parts.length >= 3) spawnElite(player, parts[0], parts[1], parts[2]);
        } else if (action.startsWith("page_spawn:")) {
            openSpawnMobType(player, Integer.parseInt(action.substring(11)));
        } else if (action.startsWith("page_list:")) {
            openEliteList(player, Integer.parseInt(action.substring(10)));
        } else if (action.startsWith("horde:")) {
            player.closeInventory();
            String cmd = action.substring(6);
            if (cmd.equals("start")) plugin.getHordeManager().startHorde(player.getLocation(), null);
            else if (cmd.equals("stop")) plugin.getHordeManager().stopHorde();
            else player.sendMessage(plugin.getHordeManager().isHordeActive() ? "尸潮进行中" : "无尸潮");
        } else if (action.startsWith("toggle:")) {
            String key = action.substring(7);
            if (key.equals("debug")) {
                boolean v = !plugin.getConfig().getBoolean("debug");
                plugin.getConfig().set("debug", v);
                plugin.getConfigManager().save();
                player.sendMessage("Debug: " + (v ? "开" : "关"));
                openSettings(player);
            } else if (key.equals("dyn")) {
                boolean v = !plugin.getConfig().getBoolean("generation.dynamic-difficulty");
                plugin.getConfig().set("generation.dynamic-difficulty", v);
                plugin.getConfigManager().save();
                player.sendMessage("动态难度: " + (v ? "开" : "关"));
                openSettings(player);
            }
        } else if (action.equals("spawn_mob")) {
            openSpawnMobType(player, 0);
        } else if (action.equals("horde")) {
            openHordeControl(player);
        } else if (action.equals("settings")) {
            openSettings(player);
        } else if (action.equals("quick")) {
            openQuickSpawn(player);
        } else if (action.equals("list")) {
            openEliteList(player, 0);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() == null) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        menuData.remove(event.getPlayer().getUniqueId());
    }

    private void spawnElite(Player player, String typeName, String affixKey, String starStr) {
        player.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                EntityType type = EntityType.valueOf(typeName.toUpperCase());
                String affix = affixKey.equals("random") ? null : affixKey.toUpperCase();
                int star = Integer.parseInt(starStr);
                org.bukkit.entity.Entity e = player.getWorld().spawnEntity(player.getLocation(), type);
                if (e instanceof LivingEntity le) {
                    plugin.getGenerationListener().convertToElite(le, affix, star);
                    player.sendMessage(Component.text("已生成 " + star + "星 " + typeName));
                }
            } catch (Exception ex) {
                player.sendMessage("生成失败: " + ex.getMessage());
            }
        });
    }

    private ItemStack guiItem(Material material, String name, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, TextColor.color(0xFFD700)));
            meta.getPersistentDataContainer().set(guiKey, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getAction(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    private Material safeMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }
}