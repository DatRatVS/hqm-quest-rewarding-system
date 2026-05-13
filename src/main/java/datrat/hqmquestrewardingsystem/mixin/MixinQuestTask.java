package datrat.hqmquestrewardingsystem.mixin;

import datrat.hqmquestrewardingsystem.hooks.QuestRewardingHooks;
import hardcorequesting.quests.Quest;
import hardcorequesting.quests.QuestTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QuestTask.class, remap = false)
public abstract class MixinQuestTask {
    @Inject(method = "completeQuest(Lhardcorequesting/quests/Quest;Ljava/lang/String;)V", at = @At("RETURN"), remap = false)
    private static void hqmquestrewardingsystem$onCompleteQuest(Quest quest, String playerName, CallbackInfo ci) {
        QuestRewardingHooks.onQuestCompleted(quest, playerName);
    }
}
