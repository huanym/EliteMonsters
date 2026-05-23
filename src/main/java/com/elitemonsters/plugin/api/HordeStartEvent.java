package com.elitemonsters.plugin.api;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import java.util.Set;
import java.util.UUID;

public class HordeStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Location center;
    private final int totalWaves;
    public HordeStartEvent(Location center, int totalWaves) { this.center = center; this.totalWaves = totalWaves; }
    public Location getCenter() { return center; }
    public int getTotalWaves() { return totalWaves; }
    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    @NotNull public static HandlerList getHandlerList() { return HANDLERS; }
}