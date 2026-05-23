package com.elitemonsters.plugin.gui;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import com.elitemonsters.plugin.affix.AffixData;
import com.elitemonsters.plugin.generation.EliteMobData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EliteGUI implements Listener {

    private final EliteMonstersPlugin plugin;
    private final Map<UUID, String> openMenus = new HashMap<>();
    private final Map<UUID, Map<String, Object>> menuData = new HashMap<>();

    public EliteGUI(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("EliteMonsters - 管理面板", TextColor.color(0xFFD700)));

        inv.setItem(11, createItem(Material.ZOMBIE_HEAD, "生成精英怪", "点击选择生物类型"));
        inv.setItem(12, createItem(Material.COMPASS, "附近精英怪", "查看并管理附近精英怪"));
        inv.setItem(13, createItem(Material.SKELETON_SKULL, "尸潮控制", "启动/停止尸潮事件"));
        inv.setItem(14, createItem(Material.COMMAND_BLOCK, "设置", "开关调试/动态难度等"));
        inv.setItem(15, createItem(Material.CRAFTING_TABLE, "快捷操作", "生成预设精英怪"));
        inv.setItem(26, createItem(Material.BARRIER, "关闭", ""));

        openMenus.put(player.getUniqueId(), "main");
        player.openInventory(inv);
    }

    private void openSpawnMobType(Player player, int page) {
        List<EntityType> mobTypes = Arrays.stream(EntityType.values())
            .filter(t -> t.isAlive() && t.isSpawnable())
            .filter(t -> t != EntityType.PLAYER && t != EntityType.ARMOR_STAND)
            .toList();

        int perPage = 45;
        int totalPages = (mobTypes.size() + perPage - 1) / perPage;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("选择生物类型 (" + (page + 1) + "/" + totalPages + ")"));

        int start = page * perPage;
        int end = Math.min(start + perPage, mobTypes.size());
        for (int i = start; i < end; i++) {
            EntityType type = mobTypes.get(i);
            inv.addItem(createItem(Material.valueOf(getSpawnEgg(type)), type.name(), "点击选择"));
        }

        if (page > 0) inv.setItem(45, createItem(Material.ARROW, "上一页", ""));
        if (page < totalPages - 1) inv.setItem(53, createItem(Material.ARROW, "下一页", ""));
        inv.setItem(49, createItem(Material.BARRIER, "返回", ""));

        openMenus.put(player.getUniqueId(), "spawn_mob");
        menuData.put(player.getUniqueId(), Map.of("page", page));
        player.openInventory(inv);
    }

    private void openSpawnAffix(Player player, EntityType mobType) {
        List<AffixData> affixes = new ArrayList<>(plugin.getAffixManager().getAllAffixes());
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("选择词缀 - " + mobType.name()));

        for (int i = 0; i < Math.min(affixes.size(), 18); i++) {
            AffixData a = affixes.get(i);
            inv.setItem(i, createItem(Material.ENCHANTED_BOOK, a.getName(), "Key: " + a.getKey()));
        }
        inv.setItem(22, createItem(Material.DIRT, "随机词缀", ""));
        inv.setItem(26, createItem(Material.BARRIER, "返回", ""));

        openMenus.put(player.getUniqueId(), "spawn_affix");
        menuData.put(player.getUniqueId(), Map.of("mobType", mobType.name()));
        player.openInventory(inv);
    }

    private void openSpawnStar(Player player, EntityType mobType, String affixKey) {
        int maxStar = plugin.getConfigManager().getMaxStarLevel();
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("选择星级 - " + mobType.name()));

        Material[] starMats = {Material.LEATHER, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE};
        for (int i = 0; i < maxStar; i++) {
            String starStr = plugin.getConfigManager().getStarChar().repeat(i + 1);
            inv.setItem(i, createItem(starMats[i], starStr + " " + (i + 1) + "星", "点击生成"));
        }
        inv.setItem(8, createItem(Material.BARRIER, "返回", ""));

        openMenus.put(player.getUniqueId(), "spawn_star");
        menuData.put(player.getUniqueId(), Map.of("mobType", mobType.name(), "affixKey", affixKey));
        player.openInventory(inv);
    }

    private void openEliteList(Player player, int page) {
        List<EliteMobData> elites = plugin.getGenerationListener().getEliteMobs().values().stream()
            .filter(d -> d.getEntity() != null)
            .filter(d -> d.getEntity().getWorld().equals(player.getWorld()))
            .sorted(Comparator.comparingDouble(d -> d.getEntity().getLocation().distanceSquared(player.getLocation())))
            .toList();

        int perPage = 45;
        int totalPages = Math.max(1, (elites.size() + perPage - 1) / perPage);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("附近精英怪 (" + (page + 1) + "/" + totalPages + ")"));

        int start = page * perPage;
        int end = Math.min(start + perPage, elites.size());
        for (int i = start; i < end; i++) {
            EliteMobData d = elites.get(i);
            LivingEntity e = d.getEntity();
            double dist = Math.sqrt(e.getLocation().distanceSquared(player.getLocation()));
            String lore = String.format("距离: %.1fm | 词缀: %s | 星级: %d", dist, d.getAffix().getName(), d.getStarLevel());
            inv.addItem(createItem(Material.SKELETON_SKULL, d.getBaseDisplayName(), lore));
        }

        if (page > 0) inv.setItem(45, createItem(Material.ARROW, "上一页", ""));
        if (page < totalPages - 1) inv.setItem(53, createItem(Material.ARROW, "下一页", ""));
        inv.setItem(49, createItem(Material.BARRIER, "返回", ""));

        openMenus.put(player.getUniqueId(), "list");
        menuData.put(player.getUniqueId(), Map.of("page", page));
        player.openInventory(inv);
    }

    private void openHordeControl(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("尸潮控制"));

        boolean active = plugin.getHordeManager().isHordeActive();
        inv.setItem(2, createItem(Material.GREEN_WOOL, "启动尸潮", active ? "当前尸潮进行中" : "手动启动尸潮"));
        inv.setItem(4, createItem(Material.RED_WOOL, "停止尸潮", active ? "强制停止当前尸潮" : "当前无尸潮"));
        inv.setItem(6, createItem(Material.CLOCK, "尸潮状态", active ? "进行中" : "未激活"));
        inv.setItem(8, createItem(Material.BARRIER, "返回", ""));

        openMenus.put(player.getUniqueId(), "horde");
        player.openInventory(inv);
    }

    private void openSettings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("设置"));

        boolean debug = plugin.getConfig().getBoolean("debug", false);
        boolean dynDiff = plugin.getConfig().getBoolean("generation.dynamic-difficulty", false);

        inv.setItem(0, createItem(debug ? Material.LIME_DYE : Material.GRAY_DYE, "Debug模式", debug ? "当前: 开启" : "当前: 关闭"));
        inv.setItem(1, createItem(dynDiff ? Material.LIME_DYE : Material.GRAY_DYE, "动态难度", dynDiff ? "当前: 开启" : "当前: 关闭"));
        inv.setItem(8, createItem(Material.BARRIER, "返回", ""));

        openMenus.put(player.getUniqueId(), "settings");
        player.openInventory(inv);
    }

    private void openQuickSpawn(Player player) {
        Inventory inv = Bukkit.createInventory(null, 18, Component.text("快捷生成"));

        inv.setItem(0, createItem(Material.ZOMBIE_HEAD, "1星僵尸", "基础测试"));
        inv.setItem(1, createItem(Material.SKELETON_SKULL, "3星骷髅", "中等难度"));
        inv.setItem(2, createItem(Material.CREEPER_HEAD, "5星苦力怕", "Boss级别"));
        inv.setItem(3, createItem(Material.WITHER_SKELETON_SKULL, "5星凋零骷髅", "顶级挑战"));
        inv.setItem(9, createItem(Material.ZOMBIE_HEAD, "吸血伯爵", "VAMPIRIC词缀"));
        inv.setItem(10, createItem(Material.SKELETON_SKULL, "炎魔", "FLAMING词缀"));
        inv.setItem(11, createItem(Material.CREEPER_HEAD, "爆破鬼才", "BOMBARDING词缀"));
        inv.setItem(17, createItem(Material.BARRIER, "返回", ""));

        openMenus.put(player.getUniqueId(), "quick");
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String name = item.getItemMeta().getDisplayName();

        if (name.contains("关闭") || name.contains("返回")) {
            if (menu.equals("main")) { player.closeInventory(); return; }
            openMainMenu(player);
            return;
        }

        switch (menu) {
            case "main" -> handleMainClick(player, name);
            case "spawn_mob" -> handleSpawnMobClick(player, name, item);
            case "spawn_affix" -> handleSpawnAffixClick(player, name, item);
            case "spawn_star" -> handleSpawnStarClick(player, name, event.getSlot());
            case "list" -> handleListClick(player, name);
            case "horde" -> handleHordeClick(player, name);
            case "settings" -> handleSettingsClick(player, name);
            case "quick" -> handleQuickClick(player, name);
        }
    }

    private void handleMainClick(Player player, String name) {
        if (name.contains("生成精英怪")) openSpawnMobType(player, 0);
        else if (name.contains("附近精英怪")) openEliteList(player, 0);
        else if (name.contains("尸潮控制")) openHordeControl(player);
        else if (name.contains("设置")) openSettings(player);
        else if (name.contains("快捷操作")) openQuickSpawn(player);
    }

    private void handleSpawnMobClick(Player player, String name, ItemStack item) {
        if (name.contains("上一页")) { int p = (int) menuData.get(player.getUniqueId()).get("page"); openSpawnMobType(player, p - 1); }
        else if (name.contains("下一页")) { int p = (int) menuData.get(player.getUniqueId()).get("page"); openSpawnMobType(player, p + 1); }
        else {
            try { openSpawnAffix(player, EntityType.valueOf(name)); }
            catch (IllegalArgumentException ignored) {}
        }
    }

    private void handleSpawnAffixClick(Player player, String name, ItemStack item) {
        Map<String, Object> data = menuData.get(player.getUniqueId());
        EntityType mobType = EntityType.valueOf((String) data.get("mobType"));
        String affixKey = name.contains("随机") ? null : item.getItemMeta().getLore().get(0).replace("Key: ", "");
        openSpawnStar(player, mobType, affixKey);
    }

    private void handleSpawnStarClick(Player player, String name, int slot) {
        if (slot >= 5) return;
        Map<String, Object> data = menuData.get(player.getUniqueId());
        EntityType mobType = EntityType.valueOf((String) data.get("mobType"));
        String affixKey = (String) data.get("affixKey");
        int star = slot + 1;

        player.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Entity entity = player.getWorld().spawnEntity(player.getLocation(), mobType);
            if (entity instanceof LivingEntity le) {
                plugin.getGenerationListener().convertToElite(le, affixKey, star);
                player.sendMessage(Component.text("已生成 " + star + "星 " + mobType.name()));
            }
        });
    }

    private void handleListClick(Player player, String name) {
        if (name.contains("上一页")) { int p = (int) menuData.get(player.getUniqueId()).get("page"); openEliteList(player, p - 1); }
        else if (name.contains("下一页")) { int p = (int) menuData.get(player.getUniqueId()).get("page"); openEliteList(player, p + 1); }
    }

    private void handleHordeClick(Player player, String name) {
        player.closeInventory();
        if (name.contains("启动尸潮")) {
            plugin.getHordeManager().startHorde(player.getLocation(), null);
        } else if (name.contains("停止尸潮")) {
            plugin.getHordeManager().stopHorde();
        } else if (name.contains("尸潮状态")) {
            boolean active = plugin.getHordeManager().isHordeActive();
            player.sendMessage(active ? "尸潮进行中" : "当前无尸潮");
        }
    }

    private void handleSettingsClick(Player player, String name) {
        if (name.contains("Debug")) {
            boolean v = !plugin.getConfig().getBoolean("debug", false);
            plugin.getConfig().set("debug", v);
            plugin.getConfigManager().save();
            player.sendMessage("Debug: " + (v ? "开启" : "关闭"));
            openSettings(player);
        } else if (name.contains("动态难度")) {
            boolean v = !plugin.getConfig().getBoolean("generation.dynamic-difficulty", false);
            plugin.getConfig().set("generation.dynamic-difficulty", v);
            plugin.getConfigManager().save();
            player.sendMessage("动态难度: " + (v ? "开启" : "关闭"));
            openSettings(player);
        }
    }

    private void handleQuickClick(Player player, String name) {
        player.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            EntityType type = EntityType.ZOMBIE;
            String affix = null;
            int star = 1;
            if (name.contains("僵尸")) { type = EntityType.ZOMBIE; if (name.contains("1星")) star = 1; }
            else if (name.contains("骷髅")) { type = EntityType.SKELETON; star = name.contains("3星") ? 3 : 5; }
            else if (name.contains("苦力怕")) { type = EntityType.CREEPER; star = 5; }
            else if (name.contains("凋零骷髅")) { type = EntityType.WITHER_SKELETON; star = 5; }
            else if (name.contains("吸血")) { type = EntityType.ZOMBIE; affix = "VAMPIRIC"; star = 3; }
            else if (name.contains("炎魔")) { type = EntityType.SKELETON; affix = "FLAMING"; star = 3; }
            else if (name.contains("爆破")) { type = EntityType.CREEPER; affix = "BOMBARDING"; star = 3; }
            org.bukkit.entity.Entity entity = player.getWorld().spawnEntity(player.getLocation(), type);
            if (entity instanceof LivingEntity le) {
                plugin.getGenerationListener().convertToElite(le, affix, star);
                player.sendMessage(Component.text("已生成"));
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openMenus.remove(event.getPlayer().getUniqueId());
        menuData.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack createItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, TextColor.color(0xFFD700)));
            if (!lore.isEmpty()) meta.lore(List.of(Component.text(lore, TextColor.color(0xAAAAAA))));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getSpawnEgg(EntityType type) {
        return switch (type) {
            case ZOMBIE -> "ZOMBIE_SPAWN_EGG";
            case SKELETON -> "SKELETON_SPAWN_EGG";
            case CREEPER -> "CREEPER_SPAWN_EGG";
            case SPIDER -> "SPIDER_SPAWN_EGG";
            case ENDERMAN -> "ENDERMAN_SPAWN_EGG";
            case WITCH -> "WITCH_SPAWN_EGG";
            case WITHER_SKELETON -> "WITHER_SKELETON_SPAWN_EGG";
            case BLAZE -> "BLAZE_SPAWN_EGG";
            case GHAST -> "GHAST_SPAWN_EGG";
            case PIGLIN -> "PIGLIN_SPAWN_EGG";
            case ZOMBIFIED_PIGLIN -> "ZOMBIFIED_PIGLIN_SPAWN_EGG";
            case HOGLIN -> "HOGLIN_SPAWN_EGG";
            case ZOGLIN -> "ZOGLIN_SPAWN_EGG";
            case PIGLIN_BRUTE -> "PIGLIN_BRUTE_SPAWN_EGG";
            case DROWNED -> "DROWNED_SPAWN_EGG";
            case HUSK -> "HUSK_SPAWN_EGG";
            case STRAY -> "STRAY_SPAWN_EGG";
            case PHANTOM -> "PHANTOM_SPAWN_EGG";
            case SLIME -> "SLIME_SPAWN_EGG";
            case MAGMA_CUBE -> "MAGMA_CUBE_SPAWN_EGG";
            case SILVERFISH -> "SILVERFISH_SPAWN_EGG";
            case ENDERMITE -> "ENDERMITE_SPAWN_EGG";
            case GUARDIAN -> "GUARDIAN_SPAWN_EGG";
            case ELDER_GUARDIAN -> "ELDER_GUARDIAN_SPAWN_EGG";
            case SHULKER -> "SHULKER_SPAWN_EGG";
            case EVOKER -> "EVOKER_SPAWN_EGG";
            case VINDICATOR -> "VINDICATOR_SPAWN_EGG";
            case PILLAGER -> "PILLAGER_SPAWN_EGG";
            case RAVAGER -> "RAVAGER_SPAWN_EGG";
            case VEX -> "VEX_SPAWN_EGG";
            case BREEZE -> "BREEZE_SPAWN_EGG";
            case BOGGED -> "BOGGED_SPAWN_EGG";
            default -> type.name() + "_SPAWN_EGG";
        };
    }
}