package com.coolnydude.exposureletters.client;

import com.coolnydude.exposureletters.ExposureLetters;
import com.coolnydude.exposureletters.client.render.EnvelopeTooltipRenderer;
import com.coolnydude.exposureletters.client.render.ExposureLecternRenderer;
import com.coolnydude.exposureletters.client.render.PaperHatModel;
import com.coolnydude.exposureletters.client.render.PaperPlaneRenderer;
import com.coolnydude.exposureletters.item.EnvelopeItem;
import com.coolnydude.exposureletters.item.NoteItem;
import com.coolnydude.exposureletters.item.StampItem;
import com.coolnydude.exposureletters.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = ExposureLetters.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    public static final int PAPER_WHITE = 16777215;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModRegistry.NOTE.get(), ExposureLetters.id("written"),
                    (stack, level, entity, seed) -> NoteItem.getContent(stack).isEmpty() ? 0.0F : 1.0F);
            ItemProperties.register(ModRegistry.STAMP_ITEM.get(), ExposureLetters.id("design"),
                    (stack, level, entity, seed) -> StampItem.DESIGNS.indexOf(StampItem.getDesign(stack)) / 10.0F);
            ItemProperties.register(ModRegistry.ENVELOPE.get(), ExposureLetters.id("state"),
                    (stack, level, entity, seed) -> EnvelopeItem.isSealed(stack) ? 1.0F : (EnvelopeItem.countContents(stack) > 0 ? 0.5F : 0.0F));
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.PAPER_PLANE_ENTITY.get(), PaperPlaneRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityType.LECTERN, ExposureLecternRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.PAPER_BOAT_BE.get(), com.coolnydude.exposureletters.client.render.PaperBoatRenderer::new);
    }

    @SubscribeEvent
    public static void onTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(EnvelopeItem.EnvelopeTooltip.class, EnvelopeTooltipRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PaperHatModel.LAYER, PaperHatModel::createLayer);
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0
                ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, PAPER_WHITE))
                : -1, ModRegistry.PAPER_HAT.get());
    }

    @SubscribeEvent
    public static void onClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private PaperHatModel model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (this.model == null) {
                    this.model = new PaperHatModel(Minecraft.getInstance().getEntityModels().bakeLayer(PaperHatModel.LAYER));
                }
                return this.model;
            }

            @Override
            public int getDefaultDyeColor(ItemStack stack) {
                return FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, PAPER_WHITE));
            }
        }, ModRegistry.PAPER_HAT.get());
    }

    private ClientSetup() {}
}
