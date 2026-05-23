package com.elitemonsters.plugin.api;

import com.elitemonsters.plugin.affix.AffixData;
import com.elitemonsters.plugin.generation.EliteMobData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EliteSpawnEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final EliteMobData eliteData;
    private final LivingEntity entity;
    private final AffixData affix;
    private final int starLevel;

    public EliteSpawnEvent(EliteMobData eliteData, LivingEntity entity, AffixData affix, int starLevel) {
        this.eliteData = eliteData;
        this.entity = entity;
        this.affix = affix;
        this.starLevel = starLevel;
    }

    public EliteMobData getEliteData() { return eliteData; }
    public LivingEntity getEntity() { return entity; }
    public AffixData getAffix() { return affix; }
    public int getStarLevel() { return starLevel; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    @NotNull public static HandlerList getHandlerList() { return HANDLERS; }
}