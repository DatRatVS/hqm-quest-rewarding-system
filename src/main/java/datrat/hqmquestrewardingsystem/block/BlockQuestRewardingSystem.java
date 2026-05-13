package datrat.hqmquestrewardingsystem.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import datrat.hqmquestrewardingsystem.QuestRewardingSystemMod;
import datrat.hqmquestrewardingsystem.hooks.QuestRewardingHooks;
import datrat.hqmquestrewardingsystem.tile.TileEntityQuestRewardingSystem;
import hardcorequesting.HardcoreQuesting;
import hardcorequesting.items.ModItems;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class BlockQuestRewardingSystem extends BlockContainer {
    public static final String REGISTRY_NAME = "questRewardingSystem";
    private static final Random DROP_RANDOM = new Random();

    @SideOnly(Side.CLIENT)
    private IIcon unboundIcon;

    @SideOnly(Side.CLIENT)
    private IIcon boundIcon;

    public BlockQuestRewardingSystem() {
        super(Material.rock);
        this.setUnlocalizedName(QuestRewardingSystemMod.MODID + ".questRewardingSystem");
        this.setHardness(1.0F);
        this.setResistance(5.0F);
        this.setCreativeTab(HardcoreQuesting.HQMTab);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        this.unboundIcon = register.registerIcon("hqm:hqmItemBarrelEmpty");
        this.boundIcon = register.registerIcon("hqm:hqmItemBarrel");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return meta == 1 ? this.boundIcon : this.unboundIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess access, int x, int y, int z, int side) {
        return this.getIcon(side, access.getBlockMetadata(x, y, z));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityQuestRewardingSystem();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (player == null) {
            return false;
        }

        ItemStack held = player.getCurrentEquippedItem();
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityQuestRewardingSystem)) {
            return false;
        }

        TileEntityQuestRewardingSystem rewardingSystem = (TileEntityQuestRewardingSystem) tile;
        if (held != null && held.getItem() == ModItems.book) {
            if (!world.isRemote) {
                QuestRewardingHooks.ValidationResult result = rewardingSystem.bindToSelectedQuest(player);
                player.addChatMessage(new ChatComponentText(result.message));
            }
            return true;
        }

        if (!world.isRemote) {
            player.addChatMessage(new ChatComponentText(rewardingSystem.getStatusMessage()));
        }
        return true;
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IInventory) {
            this.dropInventoryItems(world, x, y, z, (IInventory) tile);
        }
        if (tile instanceof TileEntityQuestRewardingSystem) {
            ((TileEntityQuestRewardingSystem) tile).unregisterLoadedTile();
        }
        super.breakBlock(world, x, y, z, block, meta);
    }

    private void dropInventoryItems(World world, int x, int y, int z, IInventory inventory) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null) {
                continue;
            }

            float offsetX = DROP_RANDOM.nextFloat() * 0.8F + 0.1F;
            float offsetY = DROP_RANDOM.nextFloat() * 0.8F + 0.1F;
            float offsetZ = DROP_RANDOM.nextFloat() * 0.8F + 0.1F;
            while (stack.stackSize > 0) {
                int amount = DROP_RANDOM.nextInt(21) + 10;
                if (amount > stack.stackSize) {
                    amount = stack.stackSize;
                }

                stack.stackSize -= amount;
                ItemStack droppedStack = new ItemStack(stack.getItem(), amount, stack.getMetadata());
                if (stack.hasTagCompound()) {
                    droppedStack.setTagCompound((NBTTagCompound) stack.getTagCompound().copy());
                }

                EntityItem entityItem = new EntityItem(world, (double) x + offsetX, (double) y + offsetY, (double) z + offsetZ, droppedStack);
                entityItem.motionX = DROP_RANDOM.nextGaussian() * 0.05D;
                entityItem.motionY = DROP_RANDOM.nextGaussian() * 0.05D + 0.2D;
                entityItem.motionZ = DROP_RANDOM.nextGaussian() * 0.05D;
                world.spawnEntityInWorld(entityItem);
            }
        }
    }
}
