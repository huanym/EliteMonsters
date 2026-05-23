package com.elitemonsters.plugin.visual;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import com.elitemonsters.plugin.affix.AffixData;
import com.elitemonsters.plugin.generation.EliteMobData;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class VisualManager {

    private final EliteMonstersPlugin plugin;
    
    private double particleDensity = 1.0;
    private boolean lightningEnabled = true;

    public VisualManager(EliteMonstersPlugin plugin) { this.plugin = plugin; this.particleDensity = plugin.getConfig().getString("visual.particle-density", "high").equalsIgnoreCase("low") ? 0.3 : plugin.getConfig().getString("visual.particle-density", "high").equalsIgnoreCase("medium") ? 0.6 : 1.0; }

    public void setLightningEnabled(boolean enabled) { this.lightningEnabled = enabled; }

    public BukkitTask playEliteSpawnEffects(LivingEntity entity, AffixData affix) {
        Location loc = entity.getLocation().add(0, 1, 0);
        ParticleManager.drawHelix(loc, Particle.FLAME, 1.5, 3.0, 60, 3);
        for (AffixData.ParticleConfig pc : affix.getParticles()) {
            entity.getWorld().spawnParticle(pc.particle(), loc, (int)(pc.count() * 20 * particleDensity), pc.offset() * 3, 2.0, pc.offset() * 3, 0.05);
        }
        ParticleManager.drawBurst(loc, Particle.ENCHANT, 2.0, 32, 0.5);
        if (lightningEnabled && plugin.getConfig().getBoolean("spawn-effects.lightning", true)) {
            entity.getWorld().strikeLightningEffect(entity.getLocation());
        }
        if (affix.getSpawnSound() != null) {
            for (Player player : entity.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(entity.getLocation()) <= 2500)
                    player.playSound(player.getLocation(), affix.getSpawnSound(), 1.0f, 1.0f);
            }
        }
        if (plugin.getConfig().getBoolean("spawn-effects.global-alert", true)) {
            double range = plugin.getConfig().getDouble("spawn-effects.alert-range", 50);
            Component msg = plugin.getLangManager().getComponent("elite-alert");
            for (Player player : entity.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(entity.getLocation()) <= range * range)
                    player.sendMessage(msg);
            }
        }
        return startParticleTask(entity, affix);
    }

    public void playEliteHitSound(LivingEntity entity, AffixData affix) {
        if (affix.getHitSound() != null) entity.getWorld().playSound(entity.getLocation(), affix.getHitSound(), 0.5f, 1.0f);
    }

    public void playEliteDeathEffects(LivingEntity entity, EliteMobData data) {
        AffixData affix = data.getAffix();
        Location loc = entity.getLocation().add(0, 1, 0);
        Particle first = affix.getParticles().isEmpty() ? Particle.CLOUD : affix.getParticles().get(0).particle();
        ParticleManager.drawSphere(loc, first, 2.0, 8, 24);
        for (AffixData.ParticleConfig pc : affix.getParticles()) {
            entity.getWorld().spawnParticle(pc.particle(), loc, (int)(pc.count() * 30 * particleDensity), pc.offset() * 4, 2.0, pc.offset() * 4, 0.15);
        }
        ParticleManager.drawCircle(loc, Particle.ENCHANT, 2.5, 36, 1.0);
        if (affix.getDeathSound() != null) entity.getWorld().playSound(entity.getLocation(), affix.getDeathSound(), 1.0f, 0.5f);
    }

    public void updateHealthBar(LivingEntity entity, EliteMobData data) {
        if (!plugin.getConfig().getBoolean("visual.health-bar-above", true)) return;
        double current = entity.getHealth();
        double max = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        String baseName = data.getBaseDisplayName();
        if (baseName == null) return;
        Component nameComp = GradientUtil.parse(baseName);
        Component hbComp = plugin.getLangManager().getComponent("health-bar", String.format("%.0f", Math.ceil(current)), String.format("%.0f", max));
        entity.customName(nameComp.append(Component.space()).append(hbComp));
    }

    public BukkitTask startParticleTask(LivingEntity entity, AffixData affix) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead() || !entity.isValid()) { cancel(); return; }
                Location loc = entity.getLocation().add(0, 1, 0);
                ParticleManager.drawCircle(loc, Particle.ENCHANT, 0.8, 8, 0.3);
                for (AffixData.ParticleConfig pc : affix.getParticles()) {
                    entity.getWorld().spawnParticle(pc.particle(), loc, Math.max(1, (int)(pc.count() * particleDensity)), pc.offset(), 0.3, pc.offset(), 0.01);
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }
}