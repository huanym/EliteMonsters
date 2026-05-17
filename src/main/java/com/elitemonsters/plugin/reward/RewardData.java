package com.elitemonsters.plugin.reward;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;

public abstract class RewardData {
    protected double chance;

    protected RewardData(Map<?, ?> map) {
        Object c = map.get("chance");
        this.chance = c instanceof Number n ? n.doubleValue() : 1.0;
    }

    public static RewardData parse(Map<?, ?> map) {
        String type = map.get("type") instanceof String s ? s.toUpperCase() : "ITEM";
        return switch (type) {
            case "ITEM" -> new ItemRewardData(map);
            case "EXP" -> new ExpRewardData(map);
            case "VAULT" -> new VaultRewardData(map);
            case "COMMAND" -> new CommandRewardData(map);
            case "PERMISSION" -> new PermissionRewardData(map);
            case "GROUP" -> new GroupRewardData(map);
            default -> new ItemRewardData(map);
        };
    }

    public boolean rollChance(Random random) {
        return chance >= 1.0 || random.nextDouble() < chance;
    }

    public double getChance() { return chance; }

    public abstract void give(Player player, EliteMonstersPlugin plugin);
}