package ninjaphenix.expandedstorage.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import ninjaphenix.expandedstorage.ExpandedStorage;
import ninjaphenix.expandedstorage.common.network.Networker;

public final class ScreenTypeSelectionScreenButton extends Button
{
    private final ResourceLocation TEXTURE;

    @SuppressWarnings("ConstantConditions")
    public ScreenTypeSelectionScreenButton(final int x, final int y, final ITooltip onTooltip)
    {
        super(x, y, 22, 22, new TranslationTextComponent("screen.expandedstorage.change_screen_button"), button ->
        {
            Minecraft.getInstance().player.clientSideCloseContainer();
            Networker.INSTANCE.requestOpenSelectionScreen();
        }, onTooltip);
        TEXTURE = ExpandedStorage.getRl("textures/gui/select_screen_button.png");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void renderButton(final MatrixStack stack, final int mouseX, final int mouseY, final float partialTicks)
    {
        Minecraft.getInstance().getTextureManager().bind(TEXTURE);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        blit(stack, x, y, 0, isHovered() ? height : 0, width, height, 32, 48);
    }

    public void renderTooltip(final MatrixStack stack, final int x, final int y)
    {
        if (isHovered()) { onTooltip.onTooltip(this, stack, x, y); }
    }
}