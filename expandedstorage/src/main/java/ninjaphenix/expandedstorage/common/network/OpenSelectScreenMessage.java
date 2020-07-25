package ninjaphenix.expandedstorage.common.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import ninjaphenix.expandedstorage.client.screen.SelectContainerScreen;
import ninjaphenix.expandedstorage.common.inventory.AbstractContainer;
import ninjaphenix.expandedstorage.common.inventory.IDataNamedContainerProvider;
import org.jetbrains.annotations.Nullable;

public final class OpenSelectScreenMessage
{
    @SuppressWarnings({"unused", "EmptyMethod"})
    static void encode(final OpenSelectScreenMessage message, final PacketBuffer buffer)
    {
    }

    @SuppressWarnings({"InstantiationOfUtilityClass", "unused"})
    static OpenSelectScreenMessage decode(final PacketBuffer buffer)
    {
        return new OpenSelectScreenMessage();
    }

    @SuppressWarnings({"ConstantConditions", "unused"})
    static void handle(final OpenSelectScreenMessage message, final Supplier<NetworkEvent.Context> contextSupplier)
    {
        final NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getOriginationSide() == LogicalSide.SERVER)
        {
            handleClient();
            context.setPacketHandled(true);
            return;
        }
        final ServerPlayerEntity sender = context.getSender();
        final AbstractContainer<?> container = (AbstractContainer<?>) sender.openContainer;
        Networker.INSTANCE.openSelectScreen(sender, (type) -> Networker.INSTANCE.openContainer(sender, new IDataNamedContainerProvider()
        {
            @Override
            public void writeExtraData(final PacketBuffer buffer)
            {
                buffer.writeBlockPos(container.ORIGIN).writeInt(container.getInv().getSizeInventory());
            }

            @Override
            public @Nullable Container createMenu(final int windowId, final PlayerInventory playerInventory, final PlayerEntity player)
            {
                return Networker.INSTANCE.getContainer(windowId, container.ORIGIN, container.getInv(), player, container.DISPLAY_NAME);
            }

            @Override
            public ITextComponent getDisplayName()
            {
                return container.DISPLAY_NAME;
            }
        }));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient()
    {
        Minecraft.getInstance().displayGuiScreen(new SelectContainerScreen());
    }
}