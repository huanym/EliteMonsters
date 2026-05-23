package com.elitemonsters.plugin.api;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import java.util.Set;
import java.util.UUID;

public class HordeCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Location center;
    private final int totalWaves;
    private final Set<UUID> participants;
    public HordeCompleteEvent(Location center, int totalWaves, Set<UUID> participants) { this.center = center; this.totalWaves = totalWaves; this.participants = participants; }
    public Location getCenter() { return center; }
    public int getTotalWaves() { return totalWaves; }
    public Set<UUID> getParticipants() { return participants; }
    @NotNull @Override public HandlerList getHandlers() { return HANDLERS; }
    @NotNull public static HandlerList getHandlerList() { return HANDLERS; }
}