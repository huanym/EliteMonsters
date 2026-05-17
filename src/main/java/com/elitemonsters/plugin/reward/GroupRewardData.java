package com.elitemonsters.plugin.reward;

import com.elitemonsters.plugin.EliteMonstersPlugin;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupRewardData extends RewardData {
    private final List<RewardData> children;

    @SuppressWarnings("unchecked")
    public GroupRewardData(Map<?, ?> map) {
        super(map);
        this.children = new ArrayList<>();
        Object childrenObj = map.get("children");
        if (childrenObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> childMap) {
                    children.add(RewardData.parse(childMap));
                }
            }
        }
    }

    @Override
    public void give(Player player, EliteMonstersPlugin plugin) {
        for (RewardData child : children) {
            child.give(player, plugin);
        }
    }

    public List<RewardData> getChildren() { return children; }
}