package ninjaphenix.refinement.api.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;
import ninjaphenix.refinement.api.common.ITierRecipe;
import ninjaphenix.refinement.api.common.ITierRecipeSerializer;
import ninjaphenix.refinement.impl.RefinementContent;
import ninjaphenix.refinement.impl.common.container.UpgradeContainer;
import org.jetbrains.annotations.Nullable;

public class ShapelessDowngradeRecipe implements ITierRecipe
{
    @Override
    public boolean matches(final UpgradeContainer container)
    {
        return false;
    }

    @Override
    public boolean canFit(final int width, final int height)
    {
        return false;
    }

    @Override
    public ResourceLocation getResult(final UpgradeContainer container)
    {
        return null;
    }

    @Override
    public ResourceLocation getId()
    {
        return null;
    }

    @Override
    public ITierRecipeSerializer<?> getSerializer()
    {
        return RefinementContent.SHAPELESS_DOWNGRADE_SERIALIZER.get();
    }

    public static class Serializer extends ForgeRegistryEntry<ITierRecipeSerializer<?>> implements ITierRecipeSerializer<ShapelessDowngradeRecipe>
    {
        @Override
        public ShapelessDowngradeRecipe read(final ResourceLocation recipeId, final JsonObject json)
        {
            return null;
        }

        @Nullable
        @Override
        public ShapelessDowngradeRecipe read(final ResourceLocation recipeId, final PacketBuffer buffer)
        {
            return null;
        }

        @Override
        public void write(final PacketBuffer buffer, final ShapelessDowngradeRecipe recipe)
        {

        }
    }
}
