package datrat.hqmquestrewardingsystem.registry;

import datrat.hqmquestrewardingsystem.tile.TileEntityQuestRewardingSystem;
import hardcorequesting.quests.Quest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class LoadedRewardingSystemRegistry {
    private static final Set<TileEntityQuestRewardingSystem> LOADED_TILES = Collections.newSetFromMap(new IdentityHashMap<TileEntityQuestRewardingSystem, Boolean>());

    private LoadedRewardingSystemRegistry() {
    }

    public static void register(TileEntityQuestRewardingSystem tile) {
        if (tile == null) {
            return;
        }
        synchronized (LOADED_TILES) {
            LOADED_TILES.add(tile);
        }
    }

    public static void unregister(TileEntityQuestRewardingSystem tile) {
        if (tile == null) {
            return;
        }
        synchronized (LOADED_TILES) {
            LOADED_TILES.remove(tile);
        }
    }

    public static void notifyQuestCompleted(Quest quest, String playerName) {
        if (quest == null || playerName == null || playerName.isEmpty()) {
            return;
        }

        List<TileEntityQuestRewardingSystem> snapshot;
        synchronized (LOADED_TILES) {
            snapshot = new ArrayList<TileEntityQuestRewardingSystem>(LOADED_TILES);
        }

        int questId = quest.getId();
        for (TileEntityQuestRewardingSystem tile : snapshot) {
            if (tile != null && tile.isBoundTo(questId, playerName)) {
                tile.onBoundQuestCompleted();
            }
        }
    }
}
