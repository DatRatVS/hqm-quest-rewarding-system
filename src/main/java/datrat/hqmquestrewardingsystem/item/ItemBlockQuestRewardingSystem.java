package datrat.hqmquestrewardingsystem.item;

import datrat.hqmquestrewardingsystem.QuestRewardingSystemMod;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public final class ItemBlockQuestRewardingSystem extends ItemBlock {
    public ItemBlockQuestRewardingSystem(Block block) {
        super(block);
        this.setHasSubtypes(false);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "tile." + QuestRewardingSystemMod.MODID + ".questRewardingSystem";
    }
}
