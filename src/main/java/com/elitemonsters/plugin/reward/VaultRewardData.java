package com.elitemonsters.plugin.reward;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;

public class VaultRewardData extends RewardData {
    private final double minAmount;
    private final double maxAmount;

    public VaultRewardData(Map<?, ?> map) {
        super(map);
        Object amtObj = map.get("amount");
        if (amtObj instanceof Number n) {
            this.minAmount = n.doubleValue();
            this.maxAmount = n.doubleValue();
        } else {
            Object minObj = map.get("min-amount");
            Object maxObj = map.get("max-amount");
            this.minAmount = minObj instanceof Number n ? n.doubleValue() : 0.0;
            this.maxAmount = maxObj instanceof Number n ? n.doubleValue() : minAmount;
        }
    }

    @Override
    public void give(Player player, EliteMonstersPlugin plugin) {
        if (plugin.getEconomy() == null) return;
        double amount;
        if (minAmount == maxAmount) {
            amount = minAmount;
        } else {
            amount = minAmount + plugin.getRewardManager().getRandom().nextDouble() * (maxAmount - minAmount);
        }
        if (amount <= 0) return;
        plugin.getEconomy().depositPlayer(player, amount);
    }
}