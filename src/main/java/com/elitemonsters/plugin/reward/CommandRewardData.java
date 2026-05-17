package com.elitemonsters.plugin.reward;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class CommandRewardData extends RewardData {
    private final List<String> commands;

    @SuppressWarnings("unchecked")
    public CommandRewardData(Map<?, ?> map) {
        super(map);
        Object cmdObj = map.get("commands");
        if (cmdObj instanceof List<?> l) {
            this.commands = l.stream().map(Object::toString).toList();
        } else if (cmdObj instanceof String s) {
            this.commands = List.of(s);
        } else {
            this.commands = List.of();
        }
    }

    @Override
    public void give(Player player, EliteMonstersPlugin plugin) {
        var loc = player.getLocation();
        for (String cmd : commands) {
            String processed = cmd
                .replace("{player}", player.getName())
                .replace("{x}", String.valueOf((int) loc.getX()))
                .replace("{y}", String.valueOf((int) loc.getY()))
                .replace("{z}", String.valueOf((int) loc.getZ()));
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processed);
        }
    }
}