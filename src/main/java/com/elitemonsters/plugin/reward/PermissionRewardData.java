package com.elitemonsters.plugin.reward;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.entity.Player;

import java.util.Map;

public class PermissionRewardData extends RewardData {
    private final String permission;
    private final String duration; // null or "" = permanent, otherwise e.g. "7d", "30d", "1h"

    public PermissionRewardData(Map<?, ?> map) {
        super(map);
        this.permission = map.get("permission") instanceof String s ? s : "";
        this.duration = map.get("duration") instanceof String s ? s : null;
    }

    @Override
    public void give(Player player, EliteMonstersPlugin plugin) {
        if (permission.isEmpty()) return;
        String cmd;
        if (duration != null && !duration.isEmpty()) {
            cmd = "lp user " + player.getName() + " permission settemp " + permission + " true " + duration;
        } else {
            cmd = "lp user " + player.getName() + " permission set " + permission + " true";
        }
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
    }
}