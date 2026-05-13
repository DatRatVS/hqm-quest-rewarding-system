package datrat.hqmquestrewardingsystem.core;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@LateMixin
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1001)
public final class QuestRewardingSystemCorePlugin implements IFMLLoadingPlugin, ILateMixinLoader {
    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return "mixins.hqm-questrewardingsystem.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return Arrays.asList("MixinQuestTask");
    }
}
