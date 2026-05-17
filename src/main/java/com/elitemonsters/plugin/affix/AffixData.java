package com.elitemonsters.plugin.affix;

import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

public class AffixData {
    private String key;
    private String name;
    private String color;
    private String prefix;
    private double healthMultiplier = 1.0;
    private double damageMultiplier = 1.0;
    private double speedMultiplier = 1.0;
    private double armor = 0.0;
    private double knockbackResistance = 0.0;
    private List<String> skillKeys = new ArrayList<>();
    private List<String> immunities = new ArrayList<>();
    private List<ParticleConfig> particles = new ArrayList<>();
    private Sound spawnSound;
    private Sound hitSound;
    private Sound deathSound;

    public record ParticleConfig(Particle particle, int count, double offset) {}

    // Getters and setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public double getHealthMultiplier() { return healthMultiplier; }
    public void setHealthMultiplier(double healthMultiplier) { this.healthMultiplier = healthMultiplier; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(double damageMultiplier) { this.damageMultiplier = damageMultiplier; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(double speedMultiplier) { this.speedMultiplier = speedMultiplier; }
    public double getArmor() { return armor; }
    public void setArmor(double armor) { this.armor = armor; }
    public double getKnockbackResistance() { return knockbackResistance; }
    public void setKnockbackResistance(double knockbackResistance) { this.knockbackResistance = knockbackResistance; }
    public List<String> getSkillKeys() { return skillKeys; }
    public void setSkillKeys(List<String> skillKeys) { this.skillKeys = skillKeys; }
    public List<String> getImmunities() { return immunities; }
    public void setImmunities(List<String> immunities) { this.immunities = immunities; }
    public List<ParticleConfig> getParticles() { return particles; }
    public void setParticles(List<ParticleConfig> particles) { this.particles = particles; }
    public Sound getSpawnSound() { return spawnSound; }
    public void setSpawnSound(Sound spawnSound) { this.spawnSound = spawnSound; }
    public Sound getHitSound() { return hitSound; }
    public void setHitSound(Sound hitSound) { this.hitSound = hitSound; }
    public Sound getDeathSound() { return deathSound; }
    public void setDeathSound(Sound deathSound) { this.deathSound = deathSound; }
}
