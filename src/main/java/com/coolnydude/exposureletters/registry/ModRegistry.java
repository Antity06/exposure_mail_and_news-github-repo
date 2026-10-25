package com.coolnydude.exposureletters.registry;

import com.coolnydude.exposureletters.ExposureLetters;
import com.coolnydude.exposureletters.component.EnvelopeAddress;
import com.coolnydude.exposureletters.component.NewspaperContent;
import com.coolnydude.exposureletters.component.NewspaperMeta;
import com.coolnydude.exposureletters.component.NoteContent;
import com.coolnydude.exposureletters.component.SealInfo;
import com.coolnydude.exposureletters.block.PaperBoatBlock;
import com.coolnydude.exposureletters.block.PaperBoatBlockEntity;
import com.coolnydude.exposureletters.entity.PaperPlane;
import com.coolnydude.exposureletters.item.EnvelopeItem;
import com.coolnydude.exposureletters.item.NewspaperItem;
import com.coolnydude.exposureletters.item.NoteItem;
import com.coolnydude.exposureletters.item.PaperBoatItem;
import com.coolnydude.exposureletters.item.PaperHatItem;
import com.coolnydude.exposureletters.item.PaperPlaneItem;
import com.coolnydude.exposureletters.item.PrintedNewspaperItem;
import com.coolnydude.exposureletters.item.StampItem;
import com.coolnydude.exposureletters.recipe.NewspaperCloningRecipe;
import com.coolnydude.exposureletters.recipe.NoteFromPaperPlaneRecipe;
import com.coolnydude.exposureletters.recipe.PaperBoatCraftingRecipe;
import com.coolnydude.exposureletters.recipe.PaperPlaneCraftingRecipe;
import com.coolnydude.exposureletters.recipe.StampRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public final class ModRegistry {
    private static final String ID = ExposureLetters.MOD_ID;

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<NoteContent>> NOTE_CONTENT =
            COMPONENTS.registerComponentType("note_content",
                    b -> b.persistent(NoteContent.CODEC).networkSynchronized(NoteContent.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> ENVELOPE_CONTENTS =
            COMPONENTS.registerComponentType("envelope_contents",
                    b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EnvelopeAddress>> ENVELOPE_ADDRESS =
            COMPONENTS.registerComponentType("envelope_address",
                    b -> b.persistent(EnvelopeAddress.CODEC).networkSynchronized(EnvelopeAddress.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SealInfo>> SEALED =
            COMPONENTS.registerComponentType("sealed",
                    b -> b.persistent(SealInfo.CODEC).networkSynchronized(SealInfo.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> STAMP =
            COMPONENTS.registerComponentType("stamp",
                    b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.stringUtf8(64)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<NewspaperContent>> NEWSPAPER_CONTENT =
            COMPONENTS.registerComponentType("newspaper_content",
                    b -> b.persistent(NewspaperContent.CODEC).networkSynchronized(NewspaperContent.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<NewspaperMeta>> NEWSPAPER_META =
            COMPONENTS.registerComponentType("newspaper_meta",
                    b -> b.persistent(NewspaperMeta.CODEC).networkSynchronized(NewspaperMeta.STREAM_CODEC));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PAPER_ARMOR =
            ARMOR_MATERIALS.register("paper", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                        for (ArmorItem.Type t : ArmorItem.Type.values()) m.put(t, 0);
                    }),
                    5, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.PAPER),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ID, "paper"), "", true)),
                    0.0F, 0.0F));

    public static final DeferredItem<NoteItem> NOTE =
            ITEMS.register("note", () -> new NoteItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<EnvelopeItem> ENVELOPE =
            ITEMS.register("envelope", () -> new EnvelopeItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<StampItem> STAMP_ITEM =
            ITEMS.register("stamp", () -> new StampItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<NewspaperItem> NEWSPAPER =
            ITEMS.register("newspaper", () -> new NewspaperItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<NewspaperItem> NEWSPAPER_DRAFT =
            ITEMS.register("newspaper_draft", () -> new NewspaperItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<PrintedNewspaperItem> PRINTED_NEWSPAPER =
            ITEMS.register("printed_newspaper", () -> new PrintedNewspaperItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<PaperHatItem> PAPER_HAT =
            ITEMS.register("paper_hat", () -> new PaperHatItem((Holder<ArmorMaterial>) PAPER_ARMOR,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(5))));
    public static final DeferredItem<PaperPlaneItem> PAPER_PLANE =
            ITEMS.register("paper_plane", () -> new PaperPlaneItem(new Item.Properties().stacksTo(16)));
    public static final DeferredBlock<PaperBoatBlock> PAPER_BOAT_BLOCK =
            BLOCKS.register("paper_boat", () -> new PaperBoatBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD).noOcclusion().instabreak()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaperBoatBlockEntity>> PAPER_BOAT_BE =
            BLOCK_ENTITIES.register("paper_boat", () -> BlockEntityType.Builder.of(PaperBoatBlockEntity::new, PAPER_BOAT_BLOCK.get()).build(null));
    public static final DeferredItem<PaperBoatItem> PAPER_BOAT =
            ITEMS.register("paper_boat", () -> new PaperBoatItem(PAPER_BOAT_BLOCK.get(), new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<EntityType<?>, EntityType<PaperPlane>> PAPER_PLANE_ENTITY =
            ENTITIES.register("paper_plane", () -> EntityType.Builder.<PaperPlane>of(PaperPlane::new, MobCategory.MISC)
                    .sized(0.5F, 0.25F).clientTrackingRange(4).updateInterval(20)
                    .build(ID + ":paper_plane"));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<NewspaperCloningRecipe>> NEWSPAPER_CLONING =
            RECIPES.register("newspaper_cloning", () -> new SimpleCraftingRecipeSerializer<>(NewspaperCloningRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StampRecipe>> STAMP_RECIPE =
            RECIPES.register("stamp", () -> new SimpleCraftingRecipeSerializer<>(StampRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PaperPlaneCraftingRecipe>> PAPER_PLANE_CRAFTING =
            RECIPES.register("paper_plane", () -> new SimpleCraftingRecipeSerializer<>(PaperPlaneCraftingRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PaperBoatCraftingRecipe>> PAPER_BOAT_CRAFTING =
            RECIPES.register("paper_boat", () -> new SimpleCraftingRecipeSerializer<>(PaperBoatCraftingRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<NoteFromPaperPlaneRecipe>> NOTE_FROM_PAPER_PLANE =
            RECIPES.register("note_from_paper_plane", () -> new SimpleCraftingRecipeSerializer<>(NoteFromPaperPlaneRecipe::new));

    public static final TagKey<Item> ENVELOPE_INSERTABLE =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ID, "envelope_insertable"));
    public static final TagKey<Item> NEWSPAPERS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ID, "newspapers"));

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != null && "exposure".equals(event.getTabKey().location().getNamespace())) {
            CreativeModeTab.Output out = event;
            out.accept(NOTE.get());
            out.accept(ENVELOPE.get());
            for (String design : StampItem.DESIGNS) {
                ItemStack stamp = new ItemStack(STAMP_ITEM.get());
                stamp.set(STAMP.get(), design);
                out.accept(stamp);
            }
            out.accept(NEWSPAPER.get());
            out.accept(PAPER_HAT.get());
            out.accept(PAPER_PLANE.get());
            out.accept(PAPER_BOAT.get());
        }
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ARMOR_MATERIALS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        RECIPES.register(bus);
        bus.addListener(ModRegistry::addCreative);
    }

    private ModRegistry() {}
}
