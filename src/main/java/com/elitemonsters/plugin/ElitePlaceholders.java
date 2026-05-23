package com.elitemonsters.plugin;

import com.elitemonsters.plugin.generation.EliteMobData;
import com.elitemonsters.plugin.horde.HordeManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ElitePlaceholders extends PlaceholderExpansion {
    private final EliteMonstersPlugin plugin;

    public ElitePlaceholders(EliteMonstersPlugin plugin) { this.plugin = plugin; }

    @NotNull @Override public String getIdentifier() { return "elitemonsters"; }
    @NotNull @Override public String getAuthor() { return "huanym"; }
    @NotNull @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        switch (identifier.toLowerCase()) {
            case "total_elites":
                return String.valueOf(plugin.getGenerationListener().getEliteMobs().size());
            case "horde_active":
                HordeManager.HordeSession h = plugin.getHordeManager().getActiveHorde();
                return h != null && h.isRunning() ? "true" : "false";
            case "horde_wave":
                HordeManager.HordeSession hw = plugin.getHordeManager().getActiveHorde();
                return hw != null && hw.isRunning() ? String.valueOf(hw.getCurrentWave()) : "0";
            case "horde_total_waves":
                HordeManager.HordeSession ht = plugin.getHordeManager().getActiveHorde();
                return ht != null && ht.isRunning() ? String.valueOf(ht.getTotalWaves()) : "0";
            case "nearest_elite":
                return getNearestEliteName(player);
            case "nearest_elite_star":
                return getNearestEliteStar(player);
            case "nearest_elite_health":
                return getNearestEliteHealth(player);
            default:
                return null;
        }
    }

    private String getNearestEliteName(Player player) {
        EliteMobData data = findNearestElite(player);
        return data != null ? data.getBaseDisplayName() : "";
    }
    private String getNearestEliteStar(Player player) {
        EliteMobData data = findNearestElite(player);
        return data != null ? String.valueOf(data.getStarLevel()) : "0";
    }
    private String getNearestEliteHealth(Player player) {
        EliteMobData data = findNearestElite(player);
        LivingEntity e = data != null ? data.getEntity() : null;
        if (e == null || !e.isValid()) return "0";
        return String.valueOf((int) e.getHealth());
    }
    private EliteMobData findNearestElite(Player player) {
        EliteMobData nearest = null;
        double minDist = Double.MAX_VALUE;
        for (EliteMobData data : plugin.getGenerationListener().getEliteMobs().values()) {
            LivingEntity e = data.getEntity();
            if (e == null || !e.isValid()) continue;
            if (!e.getWorld().equals(player.getWorld())) continue;
            double d = e.getLocation().distanceSquared(player.getLocation());
            if (d < minDist) { minDist = d; nearest = data; }
        }
        return nearest;
    }
}