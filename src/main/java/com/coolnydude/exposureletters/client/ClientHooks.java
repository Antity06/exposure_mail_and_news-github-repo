package com.coolnydude.exposureletters.client;

import com.coolnydude.exposureletters.client.render.LecternItemRenderer;
import com.coolnydude.exposureletters.client.screen.EnvelopeScreen;
import com.coolnydude.exposureletters.client.screen.NewspaperEditScreen;
import com.coolnydude.exposureletters.client.screen.NewspaperReadScreen;
import com.coolnydude.exposureletters.client.screen.NoteScreen;
import com.coolnydude.exposureletters.component.NewspaperContent;
import com.coolnydude.exposureletters.component.NoteContent;
import com.coolnydude.exposureletters.item.NewspaperItem;
import com.coolnydude.exposureletters.util.LecternBookAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class ClientHooks {
    public static void openNoteInHand(InteractionHand hand, NoteContent content) {
        Minecraft.getInstance().setScreen(new NoteScreen(content, hand, null));
    }

    public static void openNoteOnLectern(BlockPos pos, NoteContent content) {
        Minecraft.getInstance().setScreen(new NoteScreen(content, InteractionHand.MAIN_HAND, pos));
    }

    public static void openEnvelope(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new EnvelopeScreen(hand));
    }

    public static void openNewspaperEditor(InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack stack = mc.player.getItemInHand(hand);
        mc.setScreen(new NewspaperEditScreen(hand, NewspaperItem.getContent(stack)));
    }

    public static void openNewspaperReader(NewspaperContent content, BlockPos pos) {
        Minecraft.getInstance().setScreen(new NewspaperReadScreen(content, pos));
    }

    public static void syncLectern(BlockPos pos, ItemStack book) {
        LecternItemRenderer.setClientBook(pos, book);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof LecternBookAccess access) {
            access.exposureLetters$setClientBook(book);
        }
    }

    private ClientHooks() {}
}
