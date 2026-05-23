package com.elitemonsters.plugin.generation;

import com.elitemonsters.plugin.affix.AffixData;
import org.bukkit.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EliteMobData {
    private final UUID entityId;
    private final LivingEntity entity;
    private final AffixData affix;
    private final int starLevel;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double speedMultiplier;
    private final double armor;
    private final double knockbackResistance;
    private final Set<String> activeSkills;
    private String baseDisplayName;
    private boolean isDead = false;

    private final double originalMaxHealth;
    private final double originalAttackDamage;
    private final double originalMovementSpeed;
    private final double originalKnockbackResistance;

    public EliteMobData(LivingEntity entity, AffixData affix, int starLevel,
                        double healthMultiplier, double damageMultiplier, double speedMultiplier,
                        double armor, double knockbackResistance,
                        double originalMaxHealth, double originalAttackDamage,
                        double originalMovementSpeed, double originalKnockbackResistance) {
        this.entityId = entity.getUniqueId();
        this.entity = entity;
        this.affix = affix;
        this.starLevel = starLevel;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.armor = armor;
        this.knockbackResistance = knockbackResistance;
        this.originalMaxHealth = originalMaxHealth;
        this.originalAttackDamage = originalAttackDamage;
        this.originalMovementSpeed = originalMovementSpeed;
        this.originalKnockbackResistance = originalKnockbackResistance;
        this.activeSkills = new HashSet<>();
    }

    public UUID getEntityId() { return entityId; }
    public LivingEntity getEntity() { return (entity != null && entity.isValid() && !entity.isDead()) ? entity : null; }
    public AffixData getAffix() { return affix; }
    public int getStarLevel() { return starLevel; }
    public double getHealthMultiplier() { return healthMultiplier; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public double getArmor() { return armor; }
    public double getKnockbackResistance() { return knockbackResistance; }
    public Set<String> getActiveSkills() { return activeSkills; }
    public String getBaseDisplayName() { return baseDisplayName; }
    public void setBaseDisplayName(String name) { this.baseDisplayName = name; }
    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { isDead = dead; }
    public double getOriginalMaxHealth() { return originalMaxHealth; }
    public double getOriginalAttackDamage() { return originalAttackDamage; }
    public double getOriginalMovementSpeed() { return originalMovementSpeed; }
    public double getOriginalKnockbackResistance() { return originalKnockbackResistance; }
}