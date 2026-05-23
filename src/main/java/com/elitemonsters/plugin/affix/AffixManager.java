package com.elitemonsters.plugin.affix;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import com.elitemonsters.plugin.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class AffixManager {

    private final EliteMonstersPlugin plugin;
    private final Map<String, AffixData> affixes = new LinkedHashMap<>();
    private final List<String> affixKeys = new ArrayList<>();
    private final Random random = new Random();

    public AffixManager(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAffixes() {
        affixes.clear();
        affixKeys.clear();

        ConfigManager cm = plugin.getConfigManager();
        var config = plugin.getConfig();
        ConfigurationSection affixSection = config.getConfigurationSection("affixes");
        if (affixSection == null) return;

        for (String key : affixSection.getKeys(false)) {
            ConfigurationSection sec = affixSection.getConfigurationSection(key);
            if (sec == null) continue;

            AffixData data = new AffixData();
            data.setKey(key);
            data.setName(sec.getString("name", key));
            data.setColor(sec.getString("color", "&f"));
            data.setPrefix(sec.getString("prefix", "&f[%stars%%name%]"));

            ConfigurationSection attr = sec.getConfigurationSection("attributes");
            if (attr != null) {
                data.setHealthMultiplier(attr.getDouble("health-multiplier", 1.0));
                data.setDamageMultiplier(attr.getDouble("damage-multiplier", 1.0));
                data.setSpeedMultiplier(attr.getDouble("speed-multiplier", 1.0));
                data.setArmor(attr.getDouble("armor", 0.0));
                data.setKnockbackResistance(attr.getDouble("knockback-resistance", 0.0));
            }

            data.setSkillKeys(sec.getStringList("skills"));
            data.setImmunities(sec.getStringList("immunities"));

            List<AffixData.ParticleConfig> particles = new ArrayList<>();
            List<Map<?, ?>> particleList = sec.getMapList("particles");
            for (Map<?, ?> pMap : particleList) {
                try {
                    Particle particle = Particle.valueOf(String.valueOf(pMap.get("type")).toUpperCase());
                    int count = pMap.containsKey("count") ? ((Number) pMap.get("count")).intValue() : 5;
                    double offset = pMap.containsKey("offset") ? ((Number) pMap.get("offset")).doubleValue() : 0.5;
                    particles.add(new AffixData.ParticleConfig(particle, count, offset));
                } catch (IllegalArgumentException e) { plugin.getErrorLogger().log("AffixManager", "Invalid particle type: "+pMap.get("type"), e); }
            }
            data.setParticles(particles);

            ConfigurationSection sounds = sec.getConfigurationSection("sounds");
            if (sounds != null) {
                data.setSpawnSound(getSoundSafe(sounds, "spawn"));
                data.setHitSound(getSoundSafe(sounds, "hit"));
                data.setDeathSound(getSoundSafe(sounds, "death"));
            }

            affixes.put(key, data);
            affixKeys.add(key);
        }

        plugin.getLogger().info("Loaded " + affixes.size() + " affixes");
    }

    private Sound getSoundSafe(ConfigurationSection sounds, String path) {
        String name = sounds.getString(path, "");
        if (name.isEmpty()) return null;
        NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
        return Registry.SOUNDS.get(key);
    }

    public AffixData getRandomAffix() {
        if (affixKeys.isEmpty()) return null;
        return affixes.get(affixKeys.get(random.nextInt(affixKeys.size())));
    }

    public AffixData getAffix(String key) {
        return affixes.get(key.toUpperCase());
    }

    public Collection<AffixData> getAllAffixes() { return affixes.values(); }
}