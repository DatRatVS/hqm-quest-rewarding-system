package datrat.hqmquestrewardingsystem;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import datrat.hqmquestrewardingsystem.block.BlockQuestRewardingSystem;
import datrat.hqmquestrewardingsystem.item.ItemBlockQuestRewardingSystem;
import datrat.hqmquestrewardingsystem.tile.TileEntityQuestRewardingSystem;
import net.minecraft.block.Block;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = QuestRewardingSystemMod.MODID,
    name = QuestRewardingSystemMod.NAME,
    version = QuestRewardingSystemMod.VERSION,
    dependencies = "required-after:HardcoreQuesting"
)
public final class QuestRewardingSystemMod {
    public static final String MODID = "hqm-questrewardingsystem";
    public static final String NAME = "Hardcore Questing Mode: Quest Rewarding System";
    public static final String VERSION = "1.0.1";

    public static Logger logger;
    public static Block questRewardingSystemBlock;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        questRewardingSystemBlock = new BlockQuestRewardingSystem();
        GameRegistry.registerBlock(questRewardingSystemBlock, ItemBlockQuestRewardingSystem.class, BlockQuestRewardingSystem.REGISTRY_NAME);
        GameRegistry.registerTileEntity(TileEntityQuestRewardingSystem.class, MODID + ".questRewardingSystem");
        logger.info("Loaded {}", NAME);
    }
}
