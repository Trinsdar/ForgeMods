package ninjaphenix.expandedstorage.common.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.LockCode;

public final class DoubleSidedInventory implements ILockableContainer, ISidedInventory
{
    private final ISidedInventory first;
    private final ISidedInventory second;

    public DoubleSidedInventory(final ISidedInventory firstInventory, final ISidedInventory secondInventory)
    {
        first = firstInventory;
        second = secondInventory;
    }

    @Override
    public int[] getSlotsForFace(final EnumFacing direction)
    {
        final int[] firstSlots = first.getSlotsForFace(direction);
        final int[] secondSlots = second.getSlotsForFace(direction);
        final int[] combined = new int[firstSlots.length + secondSlots.length];
        int index = 0;
        for (final int slot : firstSlots) { combined[index++] = slot; }
        for (final int slot : secondSlots) { combined[index++] = slot + first.getSizeInventory(); }
        return combined;
    }

    @Override
    public boolean canInsertItem(final int slot, final ItemStack stack, final EnumFacing direction)
    {
        if (slot >= first.getSizeInventory()) { return second.canInsertItem(slot - first.getSizeInventory(), stack, direction); }
        return first.canInsertItem(slot, stack, direction);
    }

    @Override
    public boolean canExtractItem(final int slot, final ItemStack stack, final EnumFacing direction)
    {
        if (slot >= first.getSizeInventory()) { return second.canExtractItem(slot - first.getSizeInventory(), stack, direction); }
        return first.canExtractItem(slot, stack, direction);
    }

    @Override
    public int getSizeInventory() { return first.getSizeInventory() + second.getSizeInventory(); }

    @Override
    public boolean isEmpty() { return first.isEmpty() && second.isEmpty(); }

    @Override
    public boolean isUsableByPlayer(final EntityPlayer player) { return first.isUsableByPlayer(player) && second.isUsableByPlayer(player); }

    @Override
    public void clear()
    {
        first.clear();
        second.clear();
    }

    @Override
    public void markDirty()
    {
        first.markDirty();
        second.markDirty();
    }

    @Override
    public void openInventory(final EntityPlayer player)
    {
        first.openInventory(player);
        second.openInventory(player);
    }

    @Override
    public void closeInventory(final EntityPlayer player)
    {
        first.closeInventory(player);
        second.closeInventory(player);
    }

    public boolean isPart(final ISidedInventory inventory) { return first == inventory || second == inventory; }

    @Override
    public int getInventoryStackLimit() { return first.getInventoryStackLimit(); }

    @Override
    public ItemStack getStackInSlot(final int slot)
    {
        if (slot >= first.getSizeInventory()) { return second.getStackInSlot(slot - first.getSizeInventory()); }
        return first.getStackInSlot(slot);
    }

    @Override
    public ItemStack decrStackSize(final int slot, final int amount)
    {
        if (slot >= first.getSizeInventory()) { return second.decrStackSize(slot - first.getSizeInventory(), amount); }
        return first.decrStackSize(slot, amount);
    }

    @Override
    public ItemStack removeStackFromSlot(final int slot)
    {
        if (slot >= first.getSizeInventory()) { return second.removeStackFromSlot(slot - first.getSizeInventory()); }
        return first.removeStackFromSlot(slot);
    }

    @Override
    public void setInventorySlotContents(final int slot, final ItemStack stack)
    {
        if (slot >= first.getSizeInventory()) { second.setInventorySlotContents(slot - first.getSizeInventory(), stack); }
        else { first.setInventorySlotContents(slot, stack); }
    }

    @Override
    public boolean isItemValidForSlot(final int slot, final ItemStack stack)
    {
        if (slot >= first.getSizeInventory()) { return second.isItemValidForSlot(slot - first.getSizeInventory(), stack); }
        return first.isItemValidForSlot(slot, stack);
    }

    //
    // TODO: implement
    //

    @Override
    public int getField(int i)
    {
        return 0;
    }

    @Override
    public void setField(int i, int i1)
    {

    }

    @Override
    public int getFieldCount()
    {
        return 0;
    }

    @Override
    public boolean isLocked()
    {
        return false;
    }

    @Override
    public void setLockCode(LockCode code)
    {

    }

    @Override
    public LockCode getLockCode()
    {
        return null;
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn)
    {
        return null;
    }

    @Override
    public String getGuiID()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public boolean hasCustomName()
    {
        return false;
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return null;
    }
}