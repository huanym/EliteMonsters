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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;

public class EliteGenerationListener implements Listener {

    private final EliteMonstersPlugin plugin;
    private final Map<UUID, EliteMobData> eliteMobs = new HashMap<>();
    private final Random random = new Random();
    public static final String ELITE_META_KEY = "elitemonsters-elite";
    public static final String ELITE_UUID_KEY = "elitemonsters-uuid";
    private NamespacedKey eliteKey;
    private NamespacedKey eliteUuidKey;

    public EliteGenerationListener(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
        this.eliteKey = new NamespacedKey(plugin, ELITE_META_KEY);
        this.eliteUuidKey = new NamespacedKey(plugin, ELITE_UUID_KEY);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;
        if (entity.getType() == EntityType.SLIME || entity.getType() == EntityType.MAGMA_CUBE) return;
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM ||
            reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG ||
            reason == CreatureSpawnEvent.SpawnReason.COMMAND) return;
        String mobType = entity.getType().getKey().getKey().toUpperCase();
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
        int starLevel = specifiedLevel > 0 ? Math.min(specifiedLevel, maxStar) : random.nextInt(maxStar) + 1;
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

        eliteMobs.put(entity.getUniqueId(), data);

        if (plugin.getConfig().getBoolean("spawn-effects.lightning", true)) {
            entity.getWorld().strikeLightningEffect(entity.getLocation());
        }
        if (plugin.getConfig().getBoolean("spawn-effects.global-alert", true)) {
            double range = plugin.getConfig().getDouble("spawn-effects.alert-range", 50);
            for (Player p : entity.getWorld().getPlayers()) {
                if (p.getLocation().distance(entity.getLocation()) <= range) {
                    p.sendMessage(plugin.getLangManager().getComponent("elite-alert"));
                }
            }
        }
        plugin.getVisualManager().playEliteSpawnEffects(entity, affix);
    }

    private String getStars(int level) {
        String full = plugin.getConfig().getString("star-system.star-char", "\u2605");
        String empty = plugin.getConfig().getString("star-system.empty-star-char", "\u2606");
        int max = plugin.getConfigManager().getMaxStarLevel();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) sb.append(i < level ? full : empty);
        return sb.toString();
    }

    private void applyArmor(LivingEntity entity, int starLevel) {
        EntityEquipment equip = entity.getEquipment();
        if (equip == null) return;
        String tierName = plugin.getConfig().getString("star-system.levels." + starLevel + ".armor-tier", "LEATHER");
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
        event.getDrops().removeIf(item -> item != null && (item.getType().getKey().getKey().toUpperCase().contains("HELMET") || item.getType().getKey().getKey().toUpperCase().contains("CHESTPLATE") || item.getType().getKey().getKey().toUpperCase().contains("LEGGINGS") || item.getType().getKey().getKey().toUpperCase().contains("BOOTS")));
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
    }

    public void revertAllElites() {
        List<EliteMobData> snapshot = new ArrayList<>(eliteMobs.values());
        for (EliteMobData data : snapshot) {
            try {
                revertElite(data);
            } catch (Exception ignored) {}
        }
        eliteMobs.clear();
    }

    public EliteMobData getEliteData(LivingEntity entity) { return eliteMobs.get(entity.getUniqueId()); }
    public boolean isElite(LivingEntity entity) { return entity.hasMetadata(ELITE_META_KEY); }
    public Map<UUID, EliteMobData> getEliteMobs() { return eliteMobs; }
}