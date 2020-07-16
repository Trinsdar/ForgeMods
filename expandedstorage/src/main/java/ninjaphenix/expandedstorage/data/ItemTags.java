package ninjaphenix.expandedstorage.data;

import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraftforge.common.Tags;
import ninjaphenix.expandedstorage.ModContent;
import org.jetbrains.annotations.NotNull;

public class ItemTags extends ItemTagsProvider
{
    public ItemTags(@NotNull final DataGenerator generator, @NotNull final BlockTagsProvider blockTagsProvider) { super(generator, blockTagsProvider); }

    @Override
    protected void registerTags() { getOrCreateBuilder(Tags.Items.CHESTS_WOODEN).add(ModContent.WOOD_CHEST.getSecond()); }

    @NotNull @Override
    public String getName() { return "Expanded Storage - Item Tags"; }
}