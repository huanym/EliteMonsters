package com.elitemonsters.plugin.horde;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;

public class HordeNightListener implements Listener {

    private final EliteMonstersPlugin plugin;
    private long lastCheckDay = -1;

    public HordeNightListener(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTimeSkip(TimeSkipEvent event) {
        long currentDay = event.getWorld().getFullTime() / 24000L;
        if (currentDay == lastCheckDay) return;
        lastCheckDay = currentDay;

        plugin.getHordeManager().checkRandomHorde(event.getWorld());
    }
}