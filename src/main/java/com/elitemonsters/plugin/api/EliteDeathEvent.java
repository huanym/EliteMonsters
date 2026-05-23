package com.elitemonsters.plugin.api;

import com.elitemonsters.plugin.generation.EliteMobData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EliteDeathEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final EliteMobData eliteData;
    private final LivingEntity entity;

    public EliteDeathEvent(EliteMobData eliteData, LivingEntity entity) {
        this.eliteData = eliteData;
        this.entity = entity;
    }

    public EliteMobData getEliteData() { return eliteData; }
    public LivingEntity getEntity() { return entity; }

    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    @NotNull public static HandlerList getHandlerList() { return HANDLERS; }
}