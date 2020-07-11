package torcherino.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import torcherino.Torcherino;
import torcherino.api.TierSupplier;
import torcherino.blocks.tile.TorcherinoTileEntity;
import torcherino.config.Config;
import torcherino.network.Networker;

import java.util.Random;

import static net.minecraft.state.properties.BlockStateProperties.FACING;
import static net.minecraft.state.properties.BlockStateProperties.POWERED;

public class JackoLanterinoBlock extends HorizontalBlock implements TierSupplier
{
    private final ResourceLocation tierName;

    public JackoLanterinoBlock(@NotNull final ResourceLocation tierName, @NotNull final ResourceLocation registryName)
    {
        super(Block.Properties.from(Blocks.JACK_O_LANTERN));
        setRegistryName(registryName);
        this.tierName = tierName;
    }

    @NotNull @Override
    public ResourceLocation getTierName() { return tierName; }

    @Override
    public boolean hasTileEntity(@NotNull final BlockState state) { return true; }

    @Nullable @Override
    public TileEntity createTileEntity(@NotNull final BlockState state, @NotNull final IBlockReader world) { return new TorcherinoTileEntity(); }

    @Override
    protected void fillStateContainer(@NotNull final StateContainer.Builder<Block, BlockState> builder)
    {
        super.fillStateContainer(builder);
        builder.add(FACING, POWERED);
    }

    @NotNull @Override @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(@NotNull final BlockState state, @NotNull final World world, @NotNull final BlockPos pos,
            @NotNull final PlayerEntity player, @NotNull final Hand hand, @NotNull final BlockRayTraceResult hit)
    {
        if (!world.isRemote) Networker.INSTANCE.openScreenServer(world, (ServerPlayerEntity) player, pos);
        return ActionResultType.SUCCESS;
    }

    @Override
    public void onBlockPlacedBy(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState state, @Nullable LivingEntity placer,
            @NotNull final ItemStack stack)
    {
        if (world.isRemote) return;
        if (stack.hasDisplayName())
        {
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof TorcherinoTileEntity)) return;
            ((TorcherinoTileEntity) tile).setCustomName(stack.getDisplayName());
        }
        if (Config.INSTANCE.log_placement)
        {
            String prefix = "Something";
            if (placer != null) prefix = placer.getDisplayName().getString() + "(" + placer.getCachedUniqueIdString() + ")";
            Torcherino.LOGGER.info("[Torcherino] {} placed a {} at {} {} {}.", prefix,
                    StringUtils.capitalize(getTranslationKey().replace("block.torcherino.", "")
                                                              .replace("_", " ")), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @Override @SuppressWarnings("deprecation")
    public void tick(@NotNull final BlockState state, @NotNull final ServerWorld world, @NotNull final BlockPos pos, @NotNull final Random random)
    {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TorcherinoTileEntity) ((TorcherinoTileEntity) tileEntity).tick();
    }

    @NotNull @Override @SuppressWarnings("deprecation")
    public PushReaction getPushReaction(@NotNull final BlockState state) { return PushReaction.IGNORE; }

    @Override @SuppressWarnings("deprecation")
    public void onBlockAdded(@NotNull final BlockState state, @NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState oldState,
            final boolean b)
    {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TorcherinoTileEntity) ((TorcherinoTileEntity) tileEntity).setPoweredByRedstone(state.get(POWERED));
    }



    //@Override
    //public ResourceLocation getLootTable()
    //{
    //    ResourceLocation registryName = getRegistryName();
    //    return new ResourceLocation(registryName.getNamespace(), "blocks/" + registryName.getPath());
    //}

    @NotNull @Override @SuppressWarnings("ConstantConditions")
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
        boolean powered = context.getWorld().isBlockPowered(context.getPos());
        return super.getStateForPlacement(context).with(FACING, context.getPlacementHorizontalFacing().getOpposite()).with(POWERED, powered);
    }

    @Override @SuppressWarnings("deprecation")
    public void neighborChanged(@NotNull final BlockState state, @NotNull final World world, @NotNull final BlockPos pos, @NotNull final Block block,
            @NotNull final BlockPos fromPos, final boolean b)
    {
        if (world.isRemote) return;
        boolean powered = world.isBlockPowered(pos);
        if (state.get(POWERED) != powered)
        {
            world.setBlockState(pos, state.with(POWERED, powered));
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof TorcherinoTileEntity) ((TorcherinoTileEntity) tileEntity).setPoweredByRedstone(powered);
        }
    }
}