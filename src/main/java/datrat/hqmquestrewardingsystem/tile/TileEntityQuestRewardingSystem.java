package datrat.hqmquestrewardingsystem.tile;

import datrat.hqmquestrewardingsystem.hooks.QuestRewardingHooks;
import datrat.hqmquestrewardingsystem.registry.LoadedRewardingSystemRegistry;
import hardcorequesting.QuestingData;
import hardcorequesting.quests.Quest;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

public final class TileEntityQuestRewardingSystem extends TileEntity implements ISidedInventory {
    private static final int SLOT_COUNT = 9;
    private static final int[] ACCESSIBLE_SLOTS = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int RETRY_COOLDOWN_TICKS = 100;
    private static final int FALLBACK_INTERVAL_TICKS = 20;
    private static final int RECENT_CLAIM_TICKS = 200;

    private final ItemStack[] inventory = new ItemStack[SLOT_COUNT];
    private String boundPlayerName;
    private int boundQuestId = -1;
    private int retryCooldown;
    private long lastClaimTick = Long.MIN_VALUE;
    private QuestRewardingHooks.ClaimResult lastClaimResult = QuestRewardingHooks.ClaimResult.UNBOUND;

    public QuestRewardingHooks.ValidationResult bindToSelectedQuest(EntityPlayer player) {
        QuestingData data = QuestingData.getQuestingData(player);
        if (data == null) {
            return QuestRewardingHooks.ValidationResult.fail("Quest Rewarding System: no HQM data exists for this player.");
        }

        int selectedQuest = data.selectedQuest;
        QuestRewardingHooks.ValidationResult validation = QuestRewardingHooks.validateSelectedQuest(selectedQuest);
        if (!validation.success) {
            return validation;
        }

        Quest quest = QuestRewardingHooks.resolveQuest(selectedQuest);
        this.boundPlayerName = QuestingData.getUserName(player);
        this.boundQuestId = quest.getId();
        this.retryCooldown = 0;
        this.lastClaimResult = QuestRewardingHooks.ClaimResult.WAITING_FOR_COMPLETION;
        this.markDirty();
        this.updateBlockState();
        this.attemptClaim(true);
        return QuestRewardingHooks.ValidationResult.ok("Quest Rewarding System: bound to " + quest.getName() + " for " + this.boundPlayerName + ".");
    }

    public boolean isBound() {
        return this.boundPlayerName != null && !this.boundPlayerName.isEmpty() && this.boundQuestId >= 0;
    }

    public boolean isBoundTo(int questId, String playerName) {
        return this.isBound() && this.boundQuestId == questId && this.boundPlayerName.equals(playerName);
    }

    public String getBoundPlayerName() {
        return this.boundPlayerName;
    }

    public int getBoundQuestId() {
        return this.boundQuestId;
    }

    public QuestRewardingHooks.ClaimResult getLastClaimResult() {
        return this.lastClaimResult;
    }

    public String getStatusMessage() {
        this.attemptClaim(true);
        return QuestRewardingHooks.describeStatus(this);
    }

    public boolean wasRecentlyClaimed() {
        return this.worldObj != null && this.lastClaimTick != Long.MIN_VALUE && this.worldObj.getTotalWorldTime() - this.lastClaimTick <= RECENT_CLAIM_TICKS;
    }

    public void clearBinding() {
        this.boundPlayerName = null;
        this.boundQuestId = -1;
        this.retryCooldown = 0;
        this.lastClaimResult = QuestRewardingHooks.ClaimResult.UNBOUND;
        this.markDirty();
        this.updateBlockState();
    }

    public void onBoundQuestCompleted() {
        this.retryCooldown = 0;
        this.attemptClaim(true);
    }

    public void unregisterLoadedTile() {
        LoadedRewardingSystemRegistry.unregister(this);
    }

    @Override
    public void validate() {
        super.validate();
        this.registerLoadedTile();
    }

    @Override
    public void invalidate() {
        this.unregisterLoadedTile();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        this.unregisterLoadedTile();
        super.onChunkUnload();
    }

    @Override
    public void updateEntity() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }

        this.registerLoadedTile();
        this.updateBlockState();
        if (this.retryCooldown > 0) {
            this.retryCooldown--;
            return;
        }

        long offset = Math.abs(this.xCoord * 31L + this.yCoord * 17L + this.zCoord * 13L);
        if ((this.worldObj.getTotalWorldTime() + offset) % FALLBACK_INTERVAL_TICKS == 0L) {
            this.attemptClaim(false);
        }
    }

    private void registerLoadedTile() {
        if (this.worldObj != null && !this.worldObj.isRemote) {
            LoadedRewardingSystemRegistry.register(this);
        }
    }

    private QuestRewardingHooks.ClaimResult attemptClaim(boolean ignoreCooldown) {
        if (!ignoreCooldown && this.retryCooldown > 0) {
            return this.lastClaimResult;
        }

        QuestRewardingHooks.ClaimResult result = QuestRewardingHooks.tryClaim(this);
        this.lastClaimResult = result;
        if (result == QuestRewardingHooks.ClaimResult.CLAIMED && this.worldObj != null) {
            this.lastClaimTick = this.worldObj.getTotalWorldTime();
        }
        this.retryCooldown = result.shouldCooldown() ? RETRY_COOLDOWN_TICKS : 0;
        return result;
    }

    public boolean canFitRewards(List<ItemStack> rewards) {
        ItemStack[] simulated = this.copyInventory();
        for (ItemStack reward : rewards) {
            if (reward == null || reward.stackSize <= 0) {
                continue;
            }
            ItemStack remaining = reward.copy();
            this.insertIntoArray(simulated, remaining);
            if (remaining.stackSize > 0) {
                return false;
            }
        }
        return true;
    }

    public void insertRewards(List<ItemStack> rewards) {
        for (ItemStack reward : rewards) {
            if (reward == null || reward.stackSize <= 0) {
                continue;
            }
            ItemStack remaining = reward.copy();
            this.insertIntoArray(this.inventory, remaining);
        }
        this.markDirty();
    }

    private ItemStack[] copyInventory() {
        ItemStack[] copy = new ItemStack[this.inventory.length];
        for (int i = 0; i < this.inventory.length; i++) {
            copy[i] = this.inventory[i] == null ? null : this.inventory[i].copy();
        }
        return copy;
    }

    private void insertIntoArray(ItemStack[] targetInventory, ItemStack remaining) {
        for (int i = 0; i < targetInventory.length && remaining.stackSize > 0; i++) {
            ItemStack existing = targetInventory[i];
            if (existing == null || !QuestRewardingHooks.canStacksMerge(existing, remaining)) {
                continue;
            }

            int limit = Math.min(this.getInventoryStackLimit(), existing.getMaxStackSize());
            int move = Math.min(remaining.stackSize, limit - existing.stackSize);
            if (move > 0) {
                existing.stackSize += move;
                remaining.stackSize -= move;
            }
        }

        for (int i = 0; i < targetInventory.length && remaining.stackSize > 0; i++) {
            if (targetInventory[i] != null) {
                continue;
            }

            int limit = Math.min(this.getInventoryStackLimit(), remaining.getMaxStackSize());
            int move = Math.min(remaining.stackSize, limit);
            ItemStack inserted = remaining.copy();
            inserted.stackSize = move;
            targetInventory[i] = inserted;
            remaining.stackSize -= move;
        }
    }

    private void updateBlockState() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }

        Quest quest = QuestRewardingHooks.resolveQuest(this.boundQuestId);
        int targetMeta = this.isBound() && quest != null && QuestRewardingHooks.validateQuestRewards(quest).success ? 1 : 0;
        if (this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord) != targetMeta) {
            this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, targetMeta, 3);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("BoundPlayer") && tag.hasKey("BoundQuest")) {
            this.boundPlayerName = tag.getString("BoundPlayer");
            this.boundQuestId = tag.getInteger("BoundQuest");
        } else {
            this.boundPlayerName = null;
            this.boundQuestId = -1;
        }

        for (int i = 0; i < this.inventory.length; i++) {
            this.inventory[i] = null;
        }

        NBTTagList items = tag.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound itemTag = items.getCompoundTagAt(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.inventory.length) {
                this.inventory[slot] = ItemStack.loadItemStackFromNBT(itemTag);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (this.isBound()) {
            tag.setString("BoundPlayer", this.boundPlayerName);
            tag.setInteger("BoundQuest", this.boundQuestId);
        }

        NBTTagList items = new NBTTagList();
        for (int i = 0; i < this.inventory.length; i++) {
            ItemStack stack = this.inventory[i];
            if (stack == null) {
                continue;
            }

            NBTTagCompound itemTag = new NBTTagCompound();
            itemTag.setByte("Slot", (byte) i);
            stack.writeToNBT(itemTag);
            items.appendTag(itemTag);
        }
        tag.setTag("Items", items);
    }

    @Override
    public int getSizeInventory() {
        return this.inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.isValidSlot(slot) ? this.inventory[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (!this.isValidSlot(slot) || this.inventory[slot] == null || amount <= 0) {
            return null;
        }

        ItemStack result;
        if (this.inventory[slot].stackSize <= amount) {
            result = this.inventory[slot];
            this.inventory[slot] = null;
        } else {
            result = this.inventory[slot].splitStack(amount);
            if (this.inventory[slot].stackSize <= 0) {
                this.inventory[slot] = null;
            }
        }

        this.retryCooldown = 0;
        this.markDirty();
        return result;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (!this.isValidSlot(slot)) {
            return null;
        }

        ItemStack stack = this.inventory[slot];
        this.inventory[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (!this.isValidSlot(slot)) {
            return;
        }

        this.inventory[slot] = stack;
        if (stack != null && stack.stackSize > this.getInventoryStackLimit()) {
            stack.stackSize = this.getInventoryStackLimit();
        }
        this.markDirty();
    }

    @Override
    public String getInventoryName() {
        return "Quest Rewarding System";
    }

    @Override
    public boolean isCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return this.worldObj != null
            && this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) == this
            && player.getDistanceSq((double) this.xCoord + 0.5D, (double) this.yCoord + 0.5D, (double) this.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openChest() {
    }

    @Override
    public void closeChest() {
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(int side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return this.isValidSlot(slot) && stack != null;
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < this.inventory.length;
    }
}
