package com.elitemonsters.plugin.reward;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;

public class ExpRewardData extends RewardData {
    private final int minAmount;
    private final int maxAmount;
    private final boolean isLevels;

    public ExpRewardData(Map<?, ?> map) {
        super(map);
        Object amtObj = map.get("amount");
        if (amtObj instanceof Number n) {
            this.minAmount = n.intValue();
            this.maxAmount = n.intValue();
        } else {
            Object minObj = map.get("min-amount");
            Object maxObj = map.get("max-amount");
            this.minAmount = minObj instanceof Number n ? n.intValue() : 0;
            this.maxAmount = maxObj instanceof Number n ? n.intValue() : minAmount;
        }
        this.isLevels = map.get("is-levels") instanceof Boolean b ? b : false;
    }

    @Override
    public void give(Player player, EliteMonstersPlugin plugin) {
        int amount;
        if (minAmount == maxAmount) {
            amount = minAmount;
        } else {
            amount = minAmount + new Random().nextInt(maxAmount - minAmount + 1);
        }
        if (amount <= 0) return;

        if (isLevels) {
            player.giveExpLevels(amount);
        } else {
            player.giveExp(amount);
        }
    }
}