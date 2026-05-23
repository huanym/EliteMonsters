package com.elitemonsters.plugin.generation;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import com.elitemonsters.plugin.affix.AffixData;
import com.elitemonsters.plugin.visual.GradientUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import com.elitemonsters.plugin.horde.HordeManager;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;
import org.bukkit.scheduler.BukkitTask;

public class EliteGenerationListener implements Listener {

    private final EliteMonstersPlugin plugin;
    private final Map<UUID, EliteMobData> eliteMobs = new HashMap<>();
    private int spawnErrors = 0;
    private final Map<UUID, BukkitTask> particleTasks = new HashMap<>();
    private final Random random = new Random();
    public static final String ELITE_META_KEY = "elitemonsters-elite";
    public static final String ELITE_UUID_KEY = "elitemonsters-uuid";
    private NamespacedKey eliteKey;
    private NamespacedKey eliteUuidKey;

    public EliteGenerationListener(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
        this.eliteKey = new NamespacedKey(plugin, ELITE_META_KEY);
        this.eliteUuidKey = new NamespacedKey(plugin, ELITE_UUID_KEY);
        startCleanupTask();
    }
    private void startCleanupTask() {
        new org.bukkit.scheduler.BukkitRunnable() {
            public void run() {
                java.util.List<java.util.UUID> toRemove = new java.util.ArrayList<>();
                for (var entry : eliteMobs.entrySet()) {
                    LivingEntity e = entry.getValue().getEntity();
                    if (e == null || !e.isValid() || e.isDead()) toRemove.add(entry.getKey());
                }
                for (java.util.UUID id : toRemove) {
                    BukkitTask pt = particleTasks.remove(id);
                    if (pt != null) pt.cancel();
                    eliteMobs.remove(id);
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;
        if (entity.getType() == EntityType.SLIME || entity.getType() == EntityType.MAGMA_CUBE) return;
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (plugin.getConfigManager().isWorldBlacklisted(entity.getWorld().getName())) return;
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM ||
            reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG ||
            reason == CreatureSpawnEvent.SpawnReason.COMMAND) return;
        String mobType = entity.getType().name();
        Location loc = entity.getLocation();
        String biome = loc.getBlock().getBiome().getKey().getKey().toUpperCase();
        String time = getTimeCondition(entity.getWorld());
        String difficulty = entity.getWorld().getDifficulty().name();
        double chance = plugin.getConfigManager().getEliteChance(mobType, biome, loc.getY(), time, difficulty);
        if (random.nextDouble() > chance) return;

        int maxPerChunk = plugin.getConfig().getInt("generation.max-per-chunk", 3);
        if (maxPerChunk > 0 && !plugin.getHordeManager().isHordeActive()) {
            int count = 0;
            org.bukkit.Chunk chunk = loc.getChunk();
            for (Entity e : chunk.getEntities()) {
                if (e.hasMetadata(ELITE_META_KEY)) count++;
            }
            if (count >= maxPerChunk) return;
        }
        convertToElite(entity, null, 0);
    }

    public void convertToElite(LivingEntity entity, String specifiedAffix, int specifiedLevel) {
        if (eliteMobs.containsKey(entity.getUniqueId())) return;
        try {
        List<AffixData> affixes = new ArrayList<>(plugin.getAffixManager().getAllAffixes());
        if (affixes.isEmpty()) return;
        AffixData affix;
        if (specifiedAffix != null) {
            affix = plugin.getAffixManager().getAffix(specifiedAffix);
            if (affix == null) affix = affixes.get(random.nextInt(affixes.size()));
        } else {
            affix = affixes.get(random.nextInt(affixes.size()));
        }
        int maxStar = plugin.getConfigManager().getMaxStarLevel();
        int starLevel;
        if (specifiedLevel > 0) { starLevel = Math.min(specifiedLevel, maxStar); }
        else if (plugin.getConfigManager().isDynamicDifficulty()) { starLevel = calculateDynamicStar(entity); }
        else { starLevel = random.nextInt(maxStar) + 1; }
        double attrMult = plugin.getConfigManager().getStarAttributeMultiplier(starLevel);
        double healthMult = affix.getHealthMultiplier() * attrMult;
        double damageMult = affix.getDamageMultiplier() * attrMult;
        double speedMult = affix.getSpeedMultiplier();
        double armor = affix.getArmor();
        double kbResist = affix.getKnockbackResistance();
        double globalScale = plugin.getConfigManager().getGlobalAttributeScale();
        healthMult = 1.0 + (healthMult - 1.0) * globalScale;
        damageMult = 1.0 + (damageMult - 1.0) * globalScale;
        speedMult = 1.0 + (speedMult - 1.0) * globalScale;

        AttributeInstance maxHp = entity.getAttribute(Attribute.MAX_HEALTH);
        double origHp = maxHp != null ? maxHp.getBaseValue() : 20.0;
        AttributeInstance atk = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        double origAtk = atk != null ? atk.getBaseValue() : 2.0;
        AttributeInstance spd = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        double origSpd = spd != null ? spd.getBaseValue() : 0.23;
        AttributeInstance kb = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        double origKb = kb != null ? kb.getBaseValue() : 0.0;

        EliteMobData data = new EliteMobData(entity, affix, starLevel, healthMult, damageMult, speedMult, armor, kbResist,
                origHp, origAtk, origSpd, origKb);

        if (maxHp != null) { maxHp.setBaseValue(Math.min(origHp * healthMult, 2048.0)); entity.setHealth(maxHp.getValue()); }
        if (atk != null) atk.setBaseValue(origAtk * damageMult);
        if (spd != null) spd.setBaseValue(Math.max(0.05, Math.min(origSpd * speedMult, 1.0)));
        if (kb != null) { double k = Math.min(origKb + kbResist, 1.0); kb.setBaseValue(k); }

        entity.setMetadata(ELITE_META_KEY, new FixedMetadataValue(plugin, true));
        entity.getPersistentDataContainer().set(eliteKey, PersistentDataType.BOOLEAN, true);
        entity.getPersistentDataContainer().set(eliteUuidKey, PersistentDataType.STRING, entity.getUniqueId().toString());

        applyArmor(entity, starLevel);

        String baseName = entity.getType().name();
        String mobTranslated = plugin.getLangManager().get("mob-names." + baseName);
        if (mobTranslated == null || mobTranslated.equals("mob-names." + baseName)) mobTranslated = baseName;
        data.setBaseDisplayName(mobTranslated);
        String stars = getStars(starLevel);
        String affixName = affix.getName();
        String nameFormat = plugin.getLangManager().get("name-format");
        String displayName = nameFormat
                .replace("{stars}", stars)
                .replace("{affix_name}", affixName)
                .replace("{mob_name}", mobTranslated);
        entity.customName(GradientUtil.parse(displayName));
        entity.setCustomNameVisible(true);

                plugin.getConfigManager().debugLog("Elite spawned: type=" + entity.getType() + " affix=" + affix.getKey() + " star=" + starLevel + " hp=" + String.format("%.1f", healthMult) + " dmg=" + String.format("%.1f", damageMult));
eliteMobs.put(entity.getUniqueId(), data);

        if (plugin.getConfigManager().isSpawnLightning()) {
            entity.getWorld().strikeLightningEffect(entity.getLocation());
        }
        if (plugin.getConfigManager().isSpawnGlobalAlert()) {
            double range = plugin.getConfigManager().getSpawnAlertRange();
            for (Player p : entity.getWorld().getPlayers()) {
                if (p.getLocation().distance(entity.getLocation()) <= range) {
                    p.sendMessage(plugin.getLangManager().getComponent("elite-alert"));
                }
            }
        }
        BukkitTask task = plugin.getVisualManager().playEliteSpawnEffects(entity, affix);
        if (task != null) particleTasks.put(entity.getUniqueId(), task);
        plugin.getServer().getPluginManager().callEvent(new com.elitemonsters.plugin.api.EliteSpawnEvent(data, entity, affix, starLevel));
        } catch (Exception ex) {
            spawnErrors++;
            plugin.getErrorLogger().log("convertToElite", "Failed to convert "+entity.getType().name(), ex);
            if (eliteMobs.containsKey(entity.getUniqueId())) { BukkitTask t = particleTasks.remove(entity.getUniqueId()); if (t != null) t.cancel(); eliteMobs.remove(entity.getUniqueId()); }
        }
    }

    private String getStars(int level) {
        String full = plugin.getConfigManager().getStarChar();
        String empty = plugin.getConfigManager().getEmptyStarChar();
        int max = plugin.getConfigManager().getMaxStarLevel();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) sb.append(i < level ? full : empty);
        return sb.toString();
    }

    private void applyArmor(LivingEntity entity, int starLevel) {
        EntityEquipment equip = entity.getEquipment();
        if (equip == null) return;
        String tierName = plugin.getConfigManager().getStarArmorTier(starLevel);
        Material helmet = getArmorMaterial(tierName, "HELMET");
        Material chestplate = getArmorMaterial(tierName, "CHESTPLATE");
        Material leggings = getArmorMaterial(tierName, "LEGGINGS");
        Material boots = getArmorMaterial(tierName, "BOOTS");
        if (helmet != null) equip.setHelmet(createArmorPiece(helmet, starLevel), true);
        if (chestplate != null) equip.setChestplate(createArmorPiece(chestplate, starLevel), true);
        if (leggings != null) equip.setLeggings(createArmorPiece(leggings, starLevel), true);
        if (boots != null) equip.setBoots(createArmorPiece(boots, starLevel), true);
        equip.setHelmetDropChance(0f); equip.setChestplateDropChance(0f);
        equip.setLeggingsDropChance(0f); equip.setBootsDropChance(0f);
    }

    private Material getArmorMaterial(String tier, String slot) {
        try { return Material.valueOf(tier.toUpperCase() + "_" + slot); }
        catch (IllegalArgumentException e) { return null; }
    }

    private ItemStack createArmorPiece(Material material, int starLevel) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setUnbreakable(true);
            if (starLevel >= 3) meta.addEnchant(Enchantment.PROTECTION, starLevel - 2, true);
            if (starLevel >= 4) meta.addEnchant(Enchantment.THORNS, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEliteDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity damager)) return;
        if (!damager.hasMetadata(ELITE_META_KEY)) return;
        EliteMobData data = eliteMobs.get(damager.getUniqueId());
        if (data == null) return;
        event.setDamage(event.getDamage() * data.getDamageMultiplier());
        plugin.getVisualManager().playEliteHitSound(damager, data.getAffix());
        plugin.getSkillManager().onEliteAttack(data, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEliteDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!entity.hasMetadata(ELITE_META_KEY)) return;
        EliteMobData data = eliteMobs.get(entity.getUniqueId());
        if (data == null) return;
        if (data.getAffix().getImmunities().contains(event.getCause().name())) { event.setCancelled(true); return; }
        if (data.getArmor() > 0) { double r = data.getArmor() / (data.getArmor() + 20.0); event.setDamage(event.getDamage() * (1.0 - r)); }
        plugin.getSkillManager().onEliteDamaged(data, event);
        plugin.getVisualManager().updateHealthBar(entity, data);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEliteDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasMetadata(ELITE_META_KEY)) return;
        EliteMobData data = eliteMobs.remove(entity.getUniqueId());
        if (data == null) return;
        data.setDead(true);
        BukkitTask pt = particleTasks.remove(entity.getUniqueId());
        if (pt != null) pt.cancel();
        event.getDrops().removeIf(item -> item != null && (item.getType().name().contains("HELMET") || item.getType().name().contains("CHESTPLATE") || item.getType().name().contains("LEGGINGS") || item.getType().name().contains("BOOTS")));
        plugin.getServer().getPluginManager().callEvent(new com.elitemonsters.plugin.api.EliteDeathEvent(data, entity));
        Player killer = entity.getKiller();
        if (killer != null) { plugin.getLootManager().rollLoot(data, entity, killer); plugin.getEquipmentManager().rollEquipment(data, entity, killer); }
        plugin.getVisualManager().playEliteDeathEffects(entity, data);
        plugin.getSkillManager().onEliteDeath(data);
    }

    private String getTimeCondition(World world) {
        long time = world.getTime();
        if (time >= 0 && time < 13000) return "DAY";
        long moonPhase = world.getFullTime() / 24000L % 8;
        if (moonPhase == 0) return "FULL_MOON";
        return "NIGHT";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity().hasMetadata("elitemonsters-tnt")) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHordeMobDeath(EntityDeathEvent event) {
        if (!plugin.getHordeManager().isHordeActive()) return;
        var horde = plugin.getHordeManager().getActiveHorde();
        if (horde == null) return;
        if (event.getEntity().hasMetadata(HordeManager.HORDE_META_KEY) && event.getEntity().getKiller() instanceof Player) {
            horde.onPlayerKilledMob(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHordeCombat(EntityDamageByEntityEvent event) {
        if (!plugin.getHordeManager().isHordeActive()) return;
        var horde = plugin.getHordeManager().getActiveHorde();
        if (horde == null) return;
        if (event.getDamager() instanceof Player player && event.getEntity().hasMetadata(HordeManager.HORDE_META_KEY)) {
            horde.registerParticipant(player.getUniqueId());
        }
        if (event.getDamager() instanceof LivingEntity damager && damager.hasMetadata(HordeManager.HORDE_META_KEY)
            && event.getEntity() instanceof Player player) {
            horde.registerParticipant(player.getUniqueId());
        }
    }

    public void revertElite(EliteMobData data) {
        LivingEntity entity = data.getEntity();
        if (entity == null || !entity.isValid()) return;

        AttributeInstance maxHp = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) { maxHp.setBaseValue(data.getOriginalMaxHealth()); entity.setHealth(Math.min(entity.getHealth(), maxHp.getValue())); }
        AttributeInstance atk = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(data.getOriginalAttackDamage());
        AttributeInstance spd = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(data.getOriginalMovementSpeed());
        AttributeInstance kb = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(data.getOriginalKnockbackResistance());

        EntityEquipment equip = entity.getEquipment();
        if (equip != null) {
            equip.setHelmet(null); equip.setChestplate(null); equip.setLeggings(null); equip.setBoots(null);
            equip.setHelmetDropChance(0.5f); equip.setChestplateDropChance(0.5f);
            equip.setLeggingsDropChance(0.5f); equip.setBootsDropChance(0.5f);
        }

        entity.customName(null);
        entity.setCustomNameVisible(false);
        entity.setGlowing(false);
        entity.removeMetadata(ELITE_META_KEY, plugin);
        BukkitTask pt2 = particleTasks.remove(data.getEntityId());
        if (pt2 != null) pt2.cancel();
    }

    public void revertAllElites() {
        List<EliteMobData> snapshot = new ArrayList<>(eliteMobs.values());
        for (EliteMobData data : snapshot) {
            try {
                revertElite(data);
            } catch (Exception ex) { if (plugin.getConfigManager().isDebug()) plugin.getLogger().log(java.util.logging.Level.WARNING, "Error reverting elite", ex); }
        }
        eliteMobs.clear();
        for (BukkitTask task : particleTasks.values()) { try { task.cancel(); } catch (Exception ex) { if (plugin.getConfigManager().isDebug()) plugin.getLogger().warning("Error cancelling particle task: "+ex.getMessage()); } }
        particleTasks.clear();
    }

    public EliteMobData getEliteData(LivingEntity entity) { return eliteMobs.get(entity.getUniqueId()); }
    public boolean isElite(LivingEntity entity) { return entity.hasMetadata(ELITE_META_KEY); }
    
    private int calculateDynamicStar(LivingEntity entity) {
        int max = plugin.getConfigManager().getMaxStarLevel();
        double totalScore = 0;
        int count = 0;
        for (org.bukkit.entity.Player p : entity.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(entity.getLocation()) > 2500) continue;
            double score = 0;
            for (org.bukkit.inventory.ItemStack item : p.getInventory().getArmorContents()) {
                if (item == null) continue;
                String mat = item.getType().name();
                if (mat.contains("NETHERITE")) score += 4;
                else if (mat.contains("DIAMOND")) score += 3;
                else if (mat.contains("IRON")) score += 2;
                else if (mat.contains("CHAINMAIL")) score += 1;
                else score += 0.5;
                score += item.getEnchantments().size() * 0.5;
            }
            totalScore += score;
            count++;
        }
        if (count == 0) return 1;
        double avg = totalScore / count;
        if (avg >= 15) return max;
        if (avg >= 10) return Math.min(max, 4);
        if (avg >= 5) return Math.min(max, 3);
        if (avg >= 2) return 2;
        return 1;
    }

    public Map<UUID, EliteMobData> getEliteMobs() { return eliteMobs; }
}