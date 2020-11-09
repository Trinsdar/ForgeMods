package ninjaphenix.expandedstorage.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ISidedInventoryProvider;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMerger;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import ninjaphenix.expandedstorage.Registries;
import ninjaphenix.expandedstorage.common.block.entity.AbstractChestTileEntity;
import ninjaphenix.expandedstorage.common.block.enums.CursedChestType;
import ninjaphenix.expandedstorage.common.inventory.DoubleSidedInventory;
import ninjaphenix.expandedstorage.common.inventory.IDataNamedContainerProvider;
import ninjaphenix.expandedstorage.common.network.Networker;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import static net.minecraft.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public abstract class BaseChestBlock<T extends AbstractChestTileEntity> extends ContainerBlock implements ISidedInventoryProvider
{
    public static final EnumProperty<CursedChestType> TYPE = EnumProperty.create("type", CursedChestType.class);
    private final Supplier<TileEntityType<? extends T>> tileEntityType;
    private final TileEntityMerger.ICallback<T, Optional<ISidedInventory>> INVENTORY_GETTER = new TileEntityMerger.ICallback<T, Optional<ISidedInventory>>()
    {

        @Override
        public Optional<ISidedInventory> acceptDouble(final T first, final T second)
        {
            return Optional.of(new DoubleSidedInventory(first, second));
        }

        @Override
        public Optional<ISidedInventory> acceptSingle(final T single)
        {
            return Optional.of(single);
        }

        @Override
        public Optional<ISidedInventory> acceptNone()
        {
            return Optional.empty();
        }
    };
    private final TileEntityMerger.ICallback<T, Optional<IDataNamedContainerProvider>> CONTAINER_GETTER = new TileEntityMerger.ICallback<T, Optional<IDataNamedContainerProvider>>()
    {
        @Override
        public Optional<IDataNamedContainerProvider> acceptDouble(final T first, final T second)
        {
            return Optional.of(new IDataNamedContainerProvider()
            {
                private final DoubleSidedInventory inventory = new DoubleSidedInventory(first, second);

                @Override
                public void writeExtraData(final PacketBuffer buffer)
                {
                    buffer.writeBlockPos(first.getBlockPos()).writeInt(inventory.getContainerSize());
                }

                @Override
                public ITextComponent getDisplayName()
                {
                    if (first.hasCustomName()) { return first.getDisplayName(); }
                    else if (second.hasCustomName()) { return second.getDisplayName(); }
                    return new TranslationTextComponent("container.expandedstorage.generic_double", first.getDisplayName());
                }

                @Nullable
                @Override
                public Container createMenu(final int windowId, final PlayerInventory playerInventory, final PlayerEntity player)
                {
                    if (first.canOpen(player) && second.canOpen(player))
                    {
                        first.unpackLootTable(player);
                        second.unpackLootTable(player);
                        return Networker.INSTANCE.getContainer(windowId, first.getBlockPos(), inventory, player, getDisplayName());
                    }
                    return null;
                }
            });
        }

        @Override
        public Optional<IDataNamedContainerProvider> acceptSingle(final T single)
        {
            return Optional.of(new IDataNamedContainerProvider()
            {
                @Override
                public void writeExtraData(final PacketBuffer buffer)
                {
                    buffer.writeBlockPos(single.getBlockPos()).writeInt(single.getContainerSize());
                }

                @Override
                public ITextComponent getDisplayName() { return single.getDisplayName(); }

                @Nullable
                @Override
                public Container createMenu(final int windowId, final PlayerInventory playerInventory, final PlayerEntity player)
                {
                    if (single.canOpen(player))
                    {
                        single.unpackLootTable(player);
                        return Networker.INSTANCE.getContainer(windowId, single.getBlockPos(), single, player, getDisplayName());
                    }
                    return null;
                }
            });
        }

        @Override
        public Optional<IDataNamedContainerProvider> acceptNone()
        {
            return Optional.empty();
        }
    };

    protected BaseChestBlock(final Properties builder, final Supplier<TileEntityType<? extends T>> tileEntityType)
    {
        super(builder);
        this.tileEntityType = tileEntityType;
        registerDefaultState(defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(TYPE, CursedChestType.SINGLE));
    }

    public static Direction getDirectionToAttached(final BlockState state)
    {
        switch (state.getValue(TYPE))
        {
            case TOP: return Direction.DOWN;
            case BACK: return state.getValue(HORIZONTAL_FACING);
            case RIGHT: return state.getValue(HORIZONTAL_FACING).getClockWise();
            case BOTTOM: return Direction.UP;
            case FRONT: return state.getValue(HORIZONTAL_FACING).getOpposite();
            case LEFT: return state.getValue(HORIZONTAL_FACING).getCounterClockWise();
            default: throw new IllegalArgumentException("BaseChestBlock#getDirectionToAttached received an unexpected state.");
        }
    }

    public static TileEntityMerger.Type getMergeType(final BlockState state)
    {
        switch (state.getValue(TYPE))
        {
            case TOP:
            case LEFT:
            case FRONT: return TileEntityMerger.Type.FIRST;
            case BACK:
            case RIGHT:
            case BOTTOM: return TileEntityMerger.Type.SECOND;
            default: return TileEntityMerger.Type.SINGLE;
        }
    }

    public static CursedChestType getChestType(final Direction facing, final Direction offset)
    {
        if (facing.getClockWise() == offset) { return CursedChestType.RIGHT; }
        else if (facing.getCounterClockWise() == offset) { return CursedChestType.LEFT; }
        else if (facing == offset) { return CursedChestType.BACK; }
        else if (facing == offset.getOpposite()) { return CursedChestType.FRONT; }
        else if (offset == Direction.DOWN) { return CursedChestType.TOP; }
        else if (offset == Direction.UP) { return CursedChestType.BOTTOM; }
        return CursedChestType.SINGLE;
    }

    @Override
    protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING, TYPE);
    }

    public final TileEntityMerger.ICallbackWrapper<? extends T> combine(final BlockState state, final IWorld world, final BlockPos pos,
                                                                        final boolean alwaysOpen)
    {
        final BiPredicate<IWorld, BlockPos> isChestBlocked = alwaysOpen ? (_world, _pos) -> false : this::isBlocked;
        return TileEntityMerger.combineWithNeigbour(tileEntityType.get(), BaseChestBlock::getMergeType, BaseChestBlock::getDirectionToAttached,
                                                    HORIZONTAL_FACING, state, world, pos, isChestBlocked);
    }

    protected boolean isBlocked(final IWorld world, final BlockPos pos) { return ChestBlock.isChestBlockedAt(world, pos); }

    @Nullable
    @Override
    public final INamedContainerProvider getMenuProvider(final BlockState state, final World world, final BlockPos pos) { return null; }

    @Override
    @SuppressWarnings("deprecation")
    public final ActionResultType use(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player,
                                      final Hand handIn, final BlockRayTraceResult hit)
    {
        if (!world.isClientSide)
        {
            final Optional<IDataNamedContainerProvider> containerProvider = combine(state, world, pos, false).apply(CONTAINER_GETTER);
            containerProvider.ifPresent(provider ->
                                        {
                                            Networker.INSTANCE.openContainer((ServerPlayerEntity) player, provider);
                                            player.awardStat(getOpenStat());
                                        });
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public final void setPlacedBy(final World world, final BlockPos pos, final BlockState state, @Nullable final LivingEntity placer,
                                  final ItemStack stack)
    {
        if (stack.hasCustomHoverName())
        {
            final TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof AbstractChestTileEntity)
            {
                ((AbstractChestTileEntity) tileEntity).setCustomName(stack.getDisplayName());
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(final BlockState state, final World world, final BlockPos pos, final BlockState newState, final boolean moved)
    {
        if (state.getBlock() != newState.getBlock())
        {
            final TileEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof IInventory)
            {
                InventoryHelper.dropContents(world, pos, (IInventory) tileEntity);
                world.updateNeighborsAt(pos, this);
            }
            super.onRemove(state, world, pos, newState, moved);
        }
    }

    // todo: look at and see if it can be updated, specifically want to remove "BlockState state;", "Direction direction_3;" if possible
    // todo: add config to prevent automatic merging of chests.
    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context)
    {
        final World world = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        CursedChestType chestType = CursedChestType.SINGLE;
        final Direction direction_1 = context.getHorizontalDirection().getOpposite();
        final Direction direction_2 = context.getClickedFace();
        if (context.isSecondaryUseActive())
        {
            final BlockState state;
            final Direction direction_3;
            if (direction_2.getAxis().isVertical())
            {
                state = world.getBlockState(pos.relative(direction_2.getOpposite()));
                direction_3 = state.getBlock() == this && state.getValue(TYPE) == CursedChestType.SINGLE ? state.getValue(HORIZONTAL_FACING) : null;
                if (direction_3 != null && direction_3.getAxis() != direction_2.getAxis() && direction_3 == direction_1)
                {
                    chestType = direction_2 == Direction.UP ? CursedChestType.TOP : CursedChestType.BOTTOM;
                }
            }
            else
            {
                Direction offsetDir = direction_2.getOpposite();
                final BlockState clickedBlock = world.getBlockState(pos.relative(offsetDir));
                if (clickedBlock.getBlock() == this && clickedBlock.getValue(TYPE) == CursedChestType.SINGLE)
                {
                    if (clickedBlock.getValue(HORIZONTAL_FACING) == direction_2 && clickedBlock.getValue(HORIZONTAL_FACING) == direction_1)
                    {
                        chestType = CursedChestType.FRONT;
                    }
                    else
                    {
                        state = world.getBlockState(pos.relative(direction_2.getOpposite()));
                        if (state.getValue(HORIZONTAL_FACING).get2DDataValue() < 2) { offsetDir = offsetDir.getOpposite(); }
                        if (direction_1 == state.getValue(HORIZONTAL_FACING))
                        {
                            chestType = (offsetDir == Direction.WEST || offsetDir == Direction.NORTH) ? CursedChestType.LEFT : CursedChestType.RIGHT;
                        }
                    }
                }
            }
        }
        else
        {
            for (final Direction dir : Direction.values())
            {
                final BlockState state = world.getBlockState(pos.relative(dir));
                if (state.getBlock() != this || state.getValue(TYPE) != CursedChestType.SINGLE || state.getValue(HORIZONTAL_FACING) != direction_1)
                {
                    continue;
                }
                final CursedChestType type = getChestType(direction_1, dir);
                if (type != CursedChestType.SINGLE)
                {
                    chestType = type;
                    break;
                }
            }
        }
        return defaultBlockState().setValue(HORIZONTAL_FACING, direction_1).setValue(TYPE, chestType);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(final BlockState state, final Direction offset, final BlockState offsetState, final IWorld world,
                                  final BlockPos pos, final BlockPos offsetPos)
    {
        final TileEntityMerger.Type mergeType = getMergeType(state);
        if (mergeType == TileEntityMerger.Type.SINGLE)
        {
            final Direction facing = state.getValue(HORIZONTAL_FACING);
            if (!offsetState.hasProperty(TYPE)) { return state.setValue(TYPE, CursedChestType.SINGLE); }
            final CursedChestType newType = getChestType(facing, offset);
            if (offsetState.getValue(TYPE) == newType.getOpposite() && facing == offsetState.getValue(HORIZONTAL_FACING))
            {
                return state.setValue(TYPE, newType);
            }
        }
        else if (world.getBlockState(pos.relative(getDirectionToAttached(state))).getBlock() != this)
        {
            return state.setValue(TYPE, CursedChestType.SINGLE);
        }
        return super.updateShape(state, offset, offsetState, world, pos, offsetPos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(final BlockState state, final World world, final BlockPos pos)
    {
        return combine(state, world, pos, true).apply(INVENTORY_GETTER).map(Container::getRedstoneSignalFromContainer).orElse(0);
    }

    private Stat<ResourceLocation> getOpenStat() { return Stats.CUSTOM.get(Stats.OPEN_CHEST); }

    @Nullable
    @Override
    public final TileEntity newBlockEntity(@Nullable final IBlockReader world) { return null; }

    @Override
    public final boolean hasTileEntity(@Nullable final BlockState state) { return true; }

    @Override
    public abstract TileEntity createTileEntity(@Nullable final BlockState state, @Nullable final IBlockReader world);

    @Override
    @SuppressWarnings("deprecation")
    public final BlockState mirror(final BlockState state, final Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public final BlockState rotate(final BlockState state, final Rotation rotation)
    {
        return state.setValue(HORIZONTAL_FACING, rotation.rotate(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public final boolean hasAnalogOutputSignal(final BlockState state) { return true; }

    public abstract <R extends Registries.TierData> SimpleRegistry<R> getDataRegistry();

    @Override // keep for hoppers.
    public ISidedInventory getContainer(final BlockState state, final IWorld world, final BlockPos pos)
    {
        return combine(state, world, pos, true).apply(INVENTORY_GETTER).orElse(null);
    }
}