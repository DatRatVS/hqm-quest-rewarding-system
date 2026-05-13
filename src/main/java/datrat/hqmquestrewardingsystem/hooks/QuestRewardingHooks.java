package datrat.hqmquestrewardingsystem.hooks;

import datrat.hqmquestrewardingsystem.QuestRewardingSystemMod;
import datrat.hqmquestrewardingsystem.registry.LoadedRewardingSystemRegistry;
import datrat.hqmquestrewardingsystem.tile.TileEntityQuestRewardingSystem;
import hardcorequesting.EventHandler;
import hardcorequesting.QuestingData;
import hardcorequesting.Team;
import hardcorequesting.client.sounds.SoundHandler;
import hardcorequesting.client.sounds.Sounds;
import hardcorequesting.quests.Quest;
import hardcorequesting.quests.QuestData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class QuestRewardingHooks {
    private QuestRewardingHooks() {
    }

    public static void onQuestCompleted(Quest quest, String playerName) {
        LoadedRewardingSystemRegistry.notifyQuestCompleted(quest, playerName);
    }

    public static ValidationResult validateSelectedQuest(int selectedQuest) {
        if (selectedQuest < 0) {
            return ValidationResult.fail("Quest Rewarding System: no quest is selected in the quest book.");
        }

        Quest quest = resolveQuest(selectedQuest);
        if (quest == null) {
            return ValidationResult.fail("Quest Rewarding System: selected quest no longer exists.");
        }

        ValidationResult rewardResult = validateQuestRewards(quest);
        if (!rewardResult.success) {
            return rewardResult;
        }

        return ValidationResult.ok("Quest Rewarding System: selected quest is supported.");
    }

    public static ValidationResult validateQuestRewards(Quest quest) {
        if (quest == null) {
            return ValidationResult.fail("Quest Rewarding System: selected quest no longer exists.");
        }
        if (hasPickOneRewards(quest)) {
            return ValidationResult.fail("Quest Rewarding System: pick-one reward quests are unsupported.");
        }
        if (!hasFixedItemRewards(quest) && !hasReputationRewards(quest)) {
            return ValidationResult.fail("Quest Rewarding System: selected quest has no fixed item or reputation rewards.");
        }
        return ValidationResult.ok("Quest Rewarding System: selected quest is supported.");
    }

    public static Quest resolveQuest(int questId) {
        if (questId < 0 || questId >= Quest.size()) {
            return null;
        }

        try {
            return Quest.getQuest(questId);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static ClaimResult tryClaim(TileEntityQuestRewardingSystem tile) {
        if (tile == null || !tile.hasWorldObj() || tile.getWorld().isRemote) {
            return ClaimResult.NO_ACTION;
        }
        if (!tile.isBound()) {
            return ClaimResult.UNBOUND;
        }

        Quest quest = resolveQuest(tile.getBoundQuestId());
        if (quest == null) {
            tile.clearBinding();
            return ClaimResult.INVALID_QUEST;
        }
        if (hasPickOneRewards(quest)) {
            return ClaimResult.UNSUPPORTED_PICK_ONE;
        }
        if (!hasFixedItemRewards(quest) && !hasReputationRewards(quest)) {
            return ClaimResult.UNSUPPORTED_NO_REWARDS;
        }

        String playerName = tile.getBoundPlayerName();
        EntityPlayer player = QuestingData.getPlayer(playerName);
        if (player == null) {
            return ClaimResult.PLAYER_OFFLINE;
        }
        if (!quest.isEnabled(playerName)) {
            return ClaimResult.WAITING_FOR_COMPLETION;
        }

        QuestData data = quest.getQuestData(player);
        if (data == null || !quest.hasReward(player)) {
            return ClaimResult.WAITING_FOR_COMPLETION;
        }

        boolean fixedItemsAvailable = hasFixedItemRewards(quest) && data.getReward(player);
        boolean reputationAvailable = hasReputationRewards(quest) && data.canClaim();
        if (!fixedItemsAvailable && !reputationAvailable) {
            return ClaimResult.WAITING_FOR_COMPLETION;
        }

        boolean claimed = false;
        if (fixedItemsAvailable) {
            List<ItemStack> rewards = copyAndMergeRewards(quest.getReward());
            if (!tile.canFitRewards(rewards)) {
                return ClaimResult.INVENTORY_FULL;
            }

            tile.insertRewards(rewards);
            markItemRewardClaimed(quest, data, player, playerName);
            claimed = true;
        }

        if (reputationAvailable) {
            applyReputationReward(quest, data, player, playerName);
            claimed = true;
        }

        if (claimed) {
            playCompleteSound(player);
            return ClaimResult.CLAIMED;
        }

        return ClaimResult.WAITING_FOR_COMPLETION;
    }

    public static String describeStatus(TileEntityQuestRewardingSystem tile) {
        if (tile == null || !tile.isBound()) {
            if (tile != null && tile.getLastClaimResult() == ClaimResult.INVALID_QUEST) {
                return "Quest Rewarding System: bound quest no longer exists; block has been unbound.";
            }
            return "Quest Rewarding System: unbound.";
        }

        Quest quest = resolveQuest(tile.getBoundQuestId());
        if (quest == null) {
            tile.clearBinding();
            return "Quest Rewarding System: bound quest no longer exists; block has been unbound.";
        }
        if (hasPickOneRewards(quest)) {
            return "Quest Rewarding System: unsupported pick-one reward quest.";
        }
        if (!hasFixedItemRewards(quest) && !hasReputationRewards(quest)) {
            return "Quest Rewarding System: bound quest no longer has fixed item or reputation rewards.";
        }

        String playerName = tile.getBoundPlayerName();
        EntityPlayer player = QuestingData.getPlayer(playerName);
        if (player == null) {
            return "Quest Rewarding System: bound to " + quest.getName() + "; player " + playerName + " is offline.";
        }

        if (tile.wasRecentlyClaimed()) {
            return "Quest Rewarding System: claimed reward for " + quest.getName() + ".";
        }

        ClaimResult result = tile.getLastClaimResult();
        if (result == ClaimResult.INVENTORY_FULL) {
            return "Quest Rewarding System: bound to " + quest.getName() + "; waiting for inventory space.";
        }
        if (quest.hasReward(player)) {
            return "Quest Rewarding System: bound to " + quest.getName() + "; reward is claimable.";
        }

        return "Quest Rewarding System: bound to " + quest.getName() + "; waiting for quest completion.";
    }

    public static boolean hasPickOneRewards(Quest quest) {
        return hasAnyStack(quest == null ? null : quest.getRewardChoice());
    }

    public static boolean hasFixedItemRewards(Quest quest) {
        return hasAnyStack(quest == null ? null : quest.getReward());
    }

    public static boolean hasReputationRewards(Quest quest) {
        return quest != null && quest.getReputationRewards() != null && !quest.getReputationRewards().isEmpty();
    }

    private static boolean hasAnyStack(ItemStack[] stacks) {
        if (stacks == null) {
            return false;
        }
        for (ItemStack stack : stacks) {
            if (stack != null && stack.stackSize > 0) {
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack> copyAndMergeRewards(ItemStack[] rewardStacks) {
        List<ItemStack> merged = new ArrayList<ItemStack>();
        if (rewardStacks == null) {
            return merged;
        }

        for (ItemStack stack : rewardStacks) {
            if (stack == null || stack.stackSize <= 0) {
                continue;
            }

            ItemStack copy = stack.copy();
            boolean combined = false;
            for (ItemStack existing : merged) {
                if (canStacksMerge(existing, copy)) {
                    existing.stackSize += copy.stackSize;
                    combined = true;
                    break;
                }
            }
            if (!combined) {
                merged.add(copy);
            }
        }

        return merged;
    }

    public static boolean canStacksMerge(ItemStack left, ItemStack right) {
        return left != null
            && right != null
            && left.isItemEqual(right)
            && ItemStack.areItemStackTagsEqual(left, right);
    }

    private static void markItemRewardClaimed(Quest quest, QuestData data, EntityPlayer player, String playerName) {
        Team team = QuestingData.getQuestingData(player).getTeam();
        if (team != null && !team.isSingle() && team.getRewardSetting() == Team.RewardSetting.ANY) {
            if (data.reward != null) {
                for (int i = 0; i < data.reward.length; i++) {
                    data.reward[i] = false;
                }
            }
            quest.sendUpdatedDataToTeam(player);
        } else {
            data.claimReward(player);
            quest.sendUpdatedDataToTeam(playerName);
        }
    }

    private static void applyReputationReward(Quest quest, QuestData data, EntityPlayer player, String playerName) {
        data.claimed = true;
        Team team = QuestingData.getQuestingData(player).getTeam();
        if (team != null) {
            team.receiveAndSyncReputation(quest, quest.getReputationRewards());
        }

        EventHandler handler = EventHandler.instance();
        if (handler != null) {
            handler.onEvent(new EventHandler.ReputationEvent(player));
        }
        quest.sendUpdatedDataToTeam(playerName);
    }

    private static void playCompleteSound(EntityPlayer player) {
        try {
            SoundHandler.play(Sounds.COMPLETE, player);
        } catch (Throwable t) {
            if (QuestRewardingSystemMod.logger != null) {
                QuestRewardingSystemMod.logger.debug("Unable to play HQM completion sound", t);
            }
        }
    }

    public static final class ValidationResult {
        public final boolean success;
        public final String message;

        private ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ValidationResult ok(String message) {
            return new ValidationResult(true, message);
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }

    public enum ClaimResult {
        NO_ACTION(false),
        UNBOUND(false),
        INVALID_QUEST(false),
        UNSUPPORTED_PICK_ONE(true),
        UNSUPPORTED_NO_REWARDS(true),
        PLAYER_OFFLINE(true),
        WAITING_FOR_COMPLETION(false),
        INVENTORY_FULL(true),
        CLAIMED(false);

        private final boolean cooldown;

        ClaimResult(boolean cooldown) {
            this.cooldown = cooldown;
        }

        public boolean shouldCooldown() {
            return this.cooldown;
        }
    }
}
