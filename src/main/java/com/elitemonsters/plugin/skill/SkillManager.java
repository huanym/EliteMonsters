package com.elitemonsters.plugin.skill;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import com.elitemonsters.plugin.affix.AffixData;
import com.elitemonsters.plugin.generation.EliteMobData;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillManager {

    private final EliteMonstersPlugin plugin;
    private final Map<String, SkillExecutor> skillExecutors = new HashMap<>();
    private final Random random = new Random();
    private final Map<UUID, Map<String, Long>> skillCooldowns = new HashMap<>();
    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    public SkillManager(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadSkills() {
        registerDefaultSkills();
    }

    private void registerDefaultSkills() {
        skillExecutors.put("FRENZY_ENRAGE", (data, entity) -> {
            double hpPct = entity.getHealth() / entity.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (hpPct < 0.5) {
                if (!entity.hasPotionEffect(PotionEffectType.STRENGTH)) {
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, true));
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, true));
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.0f, 1.0f);
                    entity.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, entity.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0);
                }
            }
        });

        skillExecutors.put("SWIFT_DASH", (data, entity) -> {
            if (isOnCooldown(data, "SWIFT_DASH", 8000)) return;
            if (((Mob) entity).getTarget() instanceof LivingEntity target && target instanceof Player) {
                Location behind = target.getLocation().add(target.getLocation().getDirection().multiply(-2));
                behind.setY(target.getLocation().getY());
                entity.getWorld().spawnParticle(Particle.CLOUD, entity.getLocation(), 20, 0.3, 0.3, 0.3, 0.1);
                entity.teleport(behind);
                entity.getWorld().spawnParticle(Particle.CLOUD, behind, 20, 0.3, 0.3, 0.3, 0.1);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        });

        skillExecutors.put("VAMPIRIC_LIFESTEAL", (data, entity) -> {});

        skillExecutors.put("FLAMING_AURA", (data, entity) -> {
            entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1, 0), 10, 0.8, 0.8, 0.8, 0.02);
            entity.getWorld().spawnParticle(Particle.LAVA, entity.getLocation().add(0, 0.5, 0), 2, 0.5, 0.3, 0.5, 0);
            for (Entity nearby : entity.getNearbyEntities(3, 2, 3)) {
                if (nearby instanceof LivingEntity le && le instanceof Player && !le.hasMetadata("elitemonsters-elite")) {
                    le.setFireTicks(40);
                }
            }
        });

        skillExecutors.put("WITHERING_AURA", (data, entity) -> {
            entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation().add(0, 1, 0), 8, 0.8, 0.8, 0.8, 0.02);
            entity.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, entity.getLocation().add(0, 1, 0), 3, 0.6, 0.4, 0.6, 0.01);
            for (Entity nearby : entity.getNearbyEntities(4, 2, 4)) {
                if (nearby instanceof LivingEntity le && le instanceof Player && !le.hasMetadata("elitemonsters-elite")) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1, false, true));
                }
            }
        });

        skillExecutors.put("BOMBARDING_THROW", (data, entity) -> {
            if (isOnCooldown(data, "BOMBARDING_THROW", 6000)) return;
            if (((Mob) entity).getTarget() instanceof LivingEntity target && target instanceof Player) {
                Location eye = entity.getEyeLocation();
                Location tloc = target.getLocation().add(0, 1, 0);
                Vector dir = tloc.toVector().subtract(eye.toVector()).normalize();

                TNTPrimed tnt = entity.getWorld().spawn(eye.add(dir.multiply(1.5)), TNTPrimed.class);
                tnt.setFuseTicks(30);
                tnt.setVelocity(dir.multiply(1.2));
                tnt.setYield(2.0f);
                tnt.setIsIncendiary(false);
                tnt.setMetadata("elitemonsters-tnt", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);
                entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
            }
        });

        skillExecutors.put("SHIELDED_BARRIER", (data, entity) -> {
            if (isOnCooldown(data, "SHIELDED_BARRIER", 15000)) return;
            entity.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 2, false, true));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 1, false, true));
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.2f);
            entity.getWorld().spawnParticle(Particle.ENCHANT, entity.getLocation().add(0, 1, 0), 50, 1.0, 1.0, 1.0, 0.5);
        });

        skillExecutors.put("INVISIBLE_CLOAK", (data, entity) -> {
            if (isOnCooldown(data, "INVISIBLE_CLOAK", 12000)) return;
            entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 1, false, true));
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
            entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        });

        skillExecutors.put("REGENERATING_HEAL", (data, entity) -> {
            if (isOnCooldown(data, "REGENERATING_HEAL", 10000)) return;
            entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 2, false, true));
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
            entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 1, 0), 15, 0.8, 0.8, 0.8, 0.1);
        });

        skillExecutors.put("SUMMONING_MINIONS", (data, entity) -> {
            if (isOnCooldown(data, "SUMMONING_MINIONS", 20000)) return;
            Location loc = entity.getLocation();
            World world = entity.getWorld();
            int count = 2 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                Location sl = loc.clone().add(random.nextDouble() * 4 - 2, 0, random.nextDouble() * 4 - 2);
                sl.setY(world.getHighestBlockYAt(sl));
                EntityType[] types = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER};
                Entity ent = world.spawnEntity(sl, types[random.nextInt(types.length)]);
                if (ent instanceof Mob mob && ((Mob) entity).getTarget() != null) {
                    mob.setTarget(((Mob) entity).getTarget());
                }
            }
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.0f);
            entity.getWorld().spawnParticle(Particle.WITCH, entity.getLocation().add(0, 1, 0), 50, 1.5, 0.5, 1.5, 0);
        });

        skillExecutors.put("CHAINING_PULL", (data, entity) -> {
            if (isOnCooldown(data, "CHAINING_PULL", 10000)) return;
            if (((Mob) entity).getTarget() instanceof LivingEntity target && target instanceof Player) {
                Vector pullDir = entity.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
                target.setVelocity(pullDir.multiply(1.5).setY(1.0));
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.5f);
                entity.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
            }
        });

        skillExecutors.put("CHAINING_HATRED", (data, entity) -> {
            for (Entity nearby : entity.getNearbyEntities(10, 5, 10)) {
                if (nearby instanceof Monster mon && !mon.hasMetadata("elitemonsters-elite")) {
                    if (((Mob) entity).getTarget() != null) {
                        mon.setTarget(((Mob) entity).getTarget());
                    }
                }
            }
        });
    }

    public void assignSkills(EliteMobData data) {
        AffixData affix = data.getAffix();
        List<String> availableSkills = new ArrayList<>(affix.getSkillKeys());
        int skillCount = Math.min(data.getStarLevel(), availableSkills.size());
        Collections.shuffle(availableSkills);

        for (int i = 0; i < skillCount; i++) {
            data.getActiveSkills().add(availableSkills.get(i));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity entity = data.getEntity();
                if (entity == null || entity.isDead() || data.isDead()) {
                    cancel();
                    return;
                }
                for (String skillKey : data.getActiveSkills()) {
                    SkillExecutor executor = skillExecutors.get(skillKey);
                    if (executor != null) {
                        executor.execute(data, entity);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void onEliteAttack(EliteMobData data, EntityDamageByEntityEvent event) {
        LivingEntity entity = data.getEntity();
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        Location hitLoc = target.getLocation().add(0, 1, 0);
        World world = entity.getWorld();
        AffixData affix = data.getAffix();

        // === ON-HIT EFFECTS PER AFFIX ===

        if (affix.getKey().equalsIgnoreCase("FRENZY")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true));
            target.setVelocity(entity.getLocation().getDirection().setY(0.5).multiply(1.8));
            world.spawnParticle(Particle.SWEEP_ATTACK, hitLoc, 5, 0.5, 0.5, 0.5, 0);
            world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.2f);
        }

        if (affix.getKey().equalsIgnoreCase("SWIFT")) {
            if (random.nextDouble() < 0.3) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, true));
            }
            world.spawnParticle(Particle.CLOUD, hitLoc, 8, 0.3, 0.3, 0.3, 0.05);
            world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.5f);
        }

        if (affix.getKey().equalsIgnoreCase("VAMPIRIC")) {
            double heal = event.getDamage() * 0.3;
            double newHp = Math.min(entity.getHealth() + heal, entity.getAttribute(Attribute.MAX_HEALTH).getValue());
            entity.setHealth(newHp);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, false, true));
            world.spawnParticle(Particle.DAMAGE_INDICATOR, hitLoc, 10, 0.4, 0.4, 0.4, 0.05);
            world.spawnParticle(Particle.HEART, entity.getLocation().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3, 0);
            world.playSound(hitLoc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 0.5f, 1.0f);
        }

        if (affix.getKey().equalsIgnoreCase("FLAMING")) {
            target.setFireTicks(60);
            world.spawnParticle(Particle.FLAME, hitLoc, 15, 0.4, 0.4, 0.4, 0.05);
            world.spawnParticle(Particle.LAVA, hitLoc, 3, 0.3, 0.2, 0.3, 0);
            world.playSound(hitLoc, Sound.ENTITY_BLAZE_HURT, 0.7f, 1.0f);
        }

        if (affix.getKey().equalsIgnoreCase("WITHERING")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, true));
            world.spawnParticle(Particle.SMOKE, hitLoc, 12, 0.5, 0.5, 0.5, 0.03);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, hitLoc, 5, 0.4, 0.4, 0.4, 0.02);
            world.playSound(hitLoc, Sound.ENTITY_WITHER_HURT, 0.6f, 1.0f);
        }

        if (affix.getKey().equalsIgnoreCase("BOMBARDING")) {
            world.spawnParticle(Particle.EXPLOSION, hitLoc, 15, 1.0, 1.0, 1.0, 0.1);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, hitLoc, 8, 0.8, 0.8, 0.8, 0.02);
            target.setVelocity(entity.getLocation().getDirection().setY(0.8).multiply(2.5));
            world.playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
            for (Entity nearby : target.getNearbyEntities(3, 2, 3)) {
                if (nearby instanceof LivingEntity le && le instanceof Player && !le.hasMetadata("elitemonsters-elite")) {
                    Vector away = le.getLocation().toVector().subtract(hitLoc.toVector()).normalize().multiply(1.5).setY(0.5);
                    le.setVelocity(away);
                    le.damage(2.0, entity);
                }
            }
        }

        if (affix.getKey().equalsIgnoreCase("SHIELDED")) {
            target.setVelocity(entity.getLocation().getDirection().setY(0.3).multiply(0.8));
            world.spawnParticle(Particle.BLOCK, hitLoc, 10, 0.4, 0.4, 0.4, 0, Material.IRON_BLOCK.createBlockData());
            world.playSound(hitLoc, Sound.BLOCK_ANVIL_HIT, 0.5f, 1.5f);
        }

        if (affix.getKey().equalsIgnoreCase("INVISIBLE")) {
            if (random.nextDouble() < 0.4) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0, false, true));
            }
            if (random.nextDouble() < 0.2) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, false, true));
            }
            world.spawnParticle(Particle.PORTAL, hitLoc, 8, 0.3, 0.3, 0.3, 0.05);
            world.playSound(hitLoc, Sound.ENTITY_ILLUSIONER_HURT, 0.4f, 1.3f);
        }

        if (affix.getKey().equalsIgnoreCase("REGENERATING")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, true));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, true));
            world.spawnParticle(Particle.HEART, entity.getLocation().add(0, 1.5, 0), 5, 0.4, 0.4, 0.4, 0);
            world.spawnParticle(Particle.ITEM_SLIME, hitLoc, 5, 0.3, 0.3, 0.3, 0.02);
            world.playSound(hitLoc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 0.5f, 0.8f);
        }

        if (affix.getKey().equalsIgnoreCase("SUMMONING")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true));
            world.spawnParticle(Particle.WITCH, hitLoc, 12, 0.5, 0.5, 0.5, 0.03);
            world.playSound(hitLoc, Sound.ENTITY_EVOKER_HURT, 0.6f, 1.0f);
        }

        if (affix.getKey().equalsIgnoreCase("CHAINING")) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 2, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, true));
            world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 15, 0.6, 0.6, 0.6, 0.05);
            world.strikeLightningEffect(hitLoc);
            world.playSound(hitLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.0f);
        }

        hitCounters.merge(entity.getUniqueId(), 1, Integer::sum);
    }

    public void onEliteDamaged(EliteMobData data, EntityDamageEvent event) {
        LivingEntity entity = data.getEntity();
        if (data.getActiveSkills().contains("FRENZY_ENRAGE")) {
            double hpPct = entity.getHealth() / entity.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (hpPct < 0.5 && !entity.hasPotionEffect(PotionEffectType.STRENGTH)) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, true));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, true));
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.0f, 1.0f);
                entity.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, entity.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0);
            }
        }
    }

    public void onEliteDeath(EliteMobData data) {
        UUID uuid = data.getEntityId();
        skillCooldowns.remove(uuid);
        hitCounters.remove(uuid);
    }

    private boolean isOnCooldown(EliteMobData data, String skillKey, long cooldownMs) {
        UUID uuid = data.getEntityId();
        Map<String, Long> cooldowns = skillCooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(skillKey) && now - cooldowns.get(skillKey) < cooldownMs) {
            return true;
        }
        cooldowns.put(skillKey, now);
        return false;
    }

    @FunctionalInterface
    public interface SkillExecutor {
        void execute(EliteMobData data, LivingEntity entity);
    }
}