package com.zmer.testmod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import com.zmer.testmod.block.SignalBlock;
import com.zmer.testmod.block.ChargingStationBlock;
import com.zmer.testmod.block.ChargingStationBlockEntity;
import com.zmer.testmod.item.WireframeGoggles;
import com.zmer.testmod.item.DecorativeGoggles;
import com.zmer.testmod.item.TechCollar;
import com.zmer.testmod.item.ExoskeletonItem;
import com.zmer.testmod.item.KeycardItem;
import com.zmer.testmod.item.MechanicalGlovesItem;
import com.zmer.testmod.item.AiBeltItem;
import com.zmer.testmod.item.ElectronicShacklesItem;
import com.zmer.testmod.item.AnkleShacklesItem;
import com.zmer.testmod.item.ControlPanelItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ExampleMod.MODID)
public class ExampleMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "zmer_test_mod";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold BlockEntityTypes
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    // Signal Block — toggles between red cross and green arrow
    public static final RegistryObject<Block> SIGNAL_BLOCK = BLOCKS.register("signal_block",
            () -> new SignalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .strength(1.0f).noOcclusion().lightLevel(state -> 15)));
    public static final RegistryObject<Item> SIGNAL_BLOCK_ITEM = ITEMS.register("signal_block",
            () -> new BlockItem(SIGNAL_BLOCK.get(), new Item.Properties()));

    // Path Block — walking constraint in wireframe mode (cyan, green wireframe)
    public static final RegistryObject<Block> PATH_BLOCK = BLOCKS.register("path_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(1.0f).noOcclusion().lightLevel(state -> 15)));
    public static final RegistryObject<Item> PATH_BLOCK_ITEM = ITEMS.register("path_block",
            () -> new BlockItem(PATH_BLOCK.get(), new Item.Properties()));

    // Wireframe Goggles — wearing activates WIREFRAME mode
    public static final RegistryObject<Item> WIREFRAME_GOGGLES = ITEMS.register("wireframe_goggles",
            () -> new WireframeGoggles(new Item.Properties().stacksTo(1)));

    // Decorative Goggles — cosmetic only, positioned 2 pixels higher
    public static final RegistryObject<Item> DECORATIVE_GOGGLES = ITEMS.register("decorative_goggles",
            () -> new DecorativeGoggles(new Item.Properties().stacksTo(1)));

    // High-Tech Collar — Curios necklace with lock mechanism
    public static final RegistryObject<Item> TECH_COLLAR = ITEMS.register("tech_collar",
            () -> new TechCollar(new Item.Properties().stacksTo(1)));

    // Exoskeleton — Curios body accessory with lock & blindness injection
    public static final RegistryObject<Item> EXOSKELETON = ITEMS.register("exoskeleton",
            () -> new ExoskeletonItem(new Item.Properties().stacksTo(1)));

    // Keycard - for collar authentication
    public static final RegistryObject<Item> KEYCARD = ITEMS.register("keycard",
            () -> new KeycardItem(new Item.Properties().stacksTo(1)));

    // Mechanical Gloves — Curios hands accessory (gray full-coverage gauntlets)
    public static final RegistryObject<Item> MECHANICAL_GLOVES = ITEMS.register("mechanical_gloves",
            () -> new MechanicalGlovesItem(new Item.Properties().stacksTo(1)));

    // Tab Icon Item — creative tab icon (not obtainable in game)
    public static final RegistryObject<Item> TAB_ICON = ITEMS.register("tab_icon",
            () -> new Item(new Item.Properties()));

    // Charging Station — docking platform to recharge Exoskeleton energy
    public static final RegistryObject<Block> CHARGING_STATION = BLOCKS.register("charging_station",
            () -> new ChargingStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0f).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(state -> state.getValue(ChargingStationBlock.CHARGING) ? 12 : 4)));
    public static final RegistryObject<Item> CHARGING_STATION_ITEM = ITEMS.register("charging_station",
            () -> new BlockItem(CHARGING_STATION.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<ChargingStationBlockEntity>> CHARGING_STATION_BE =
            BLOCK_ENTITIES.register("charging_station",
                    () -> BlockEntityType.Builder.of(ChargingStationBlockEntity::new, CHARGING_STATION.get()).build(null));

    // AI Management Belt — Curios belt accessory with AI behavior management
    public static final RegistryObject<Item> AI_BELT = ITEMS.register("ai_belt",
            () -> new AiBeltItem(new Item.Properties().stacksTo(1)));

    // Electronic Shackles — Curios legs accessory with QTE lock
    public static final RegistryObject<Item> ELECTRONIC_SHACKLES = ITEMS.register("electronic_shackles",
            () -> new ElectronicShacklesItem(new Item.Properties().stacksTo(1)));
            
    public static final RegistryObject<Item> ANKLE_SHACKLES = ITEMS.register("ankle_shackles",
            () -> new AnkleShacklesItem(new Item.Properties().stacksTo(1)));

    // Control Panel — master's remote control interface for owned targets
    public static final RegistryObject<Item> CONTROL_PANEL = ITEMS.register("control_panel",
            () -> new ControlPanelItem(new Item.Properties().stacksTo(1)));

    // Tech Barrier Block — blocks players wearing tech gear, lets others pass through
    public static final RegistryObject<Block> TECH_BARRIER = BLOCKS.register("tech_barrier",
            () -> new com.zmer.testmod.block.TechBarrierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(3.0f, 6.0f).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Item> TECH_BARRIER_ITEM = ITEMS.register("tech_barrier",
            () -> new BlockItem(TECH_BARRIER.get(), new Item.Properties()));

    // Exo Assimilator Block
    public static final RegistryObject<Block> EXO_ASSIMILATOR = BLOCKS.register("exo_assimilator",
            () -> new com.zmer.testmod.block.ExoAssimilatorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Item> EXO_ASSIMILATOR_ITEM = ITEMS.register("exo_assimilator",
            () -> new BlockItem(EXO_ASSIMILATOR.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<com.zmer.testmod.block.ExoAssimilatorBlockEntity>> EXO_ASSIMILATOR_BE =
            BLOCK_ENTITIES.register("exo_assimilator",
                    () -> BlockEntityType.Builder.of(com.zmer.testmod.block.ExoAssimilatorBlockEntity::new, EXO_ASSIMILATOR.get()).build(null));

    // Creates a creative tab with the id "zmer_test_mod:main_tab" placed after the combat tab
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> TAB_ICON.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(SIGNAL_BLOCK_ITEM.get());
                output.accept(PATH_BLOCK_ITEM.get());
                        output.accept(WIREFRAME_GOGGLES.get());
                        output.accept(DECORATIVE_GOGGLES.get());
                output.accept(TECH_COLLAR.get());
                output.accept(EXOSKELETON.get());
                output.accept(CHARGING_STATION_ITEM.get());
                output.accept(KEYCARD.get());
                output.accept(MECHANICAL_GLOVES.get());
                output.accept(TECH_BARRIER_ITEM.get());
                output.accept(AI_BELT.get());
                output.accept(ELECTRONIC_SHACKLES.get());
                output.accept(ANKLE_SHACKLES.get());
                output.accept(CONTROL_PANEL.get());
                output.accept(EXO_ASSIMILATOR_ITEM.get());
            }).build());

    public ExampleMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        GeckoLib.initialize();

        // Register network
        NetworkHandler.register();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entities get registered
        BLOCK_ENTITIES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            event.enqueueWork(() -> {
                com.zmer.testmod.client.CuriosRenderers.registerRenderers();
                com.zmer.testmod.client.GogglesOffsetHandler.init();
            });
        }

        @SubscribeEvent
        public static void onRegisterRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event)
        {
            event.registerBlockEntityRenderer(EXO_ASSIMILATOR_BE.get(), com.zmer.testmod.client.ExoAssimilatorRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event)
        {
            event.registerLayerDefinition(
                    com.zmer.testmod.client.WireframeGogglesModel.LAYER_LOCATION,
                    com.zmer.testmod.client.WireframeGogglesModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.zmer.testmod.client.DecorativeGogglesModel.LAYER_LOCATION,
                    com.zmer.testmod.client.DecorativeGogglesModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.zmer.testmod.client.TechCollarModel.LAYER_LOCATION,
                    com.zmer.testmod.client.TechCollarModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.zmer.testmod.client.ExoskeletonModel.LAYER_LOCATION,
                    com.zmer.testmod.client.ExoskeletonModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.zmer.testmod.client.MechanicalGlovesModel.LAYER_LOCATION,
                    com.zmer.testmod.client.MechanicalGlovesModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.zmer.testmod.client.AiBeltModel.LAYER_LOCATION,
                    com.zmer.testmod.client.AiBeltModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.zmer.testmod.client.ElectronicShacklesModel.LAYER_LOCATION,
                    com.zmer.testmod.client.ElectronicShacklesModel::createBodyLayer);
            event.registerLayerDefinition(
                    com.zmer.testmod.client.AnkleShacklesModel.LAYER_LOCATION,
                    com.zmer.testmod.client.AnkleShacklesModel::createBodyLayer);
            LOGGER.info("[WireframeGoggles] Registered custom model layers");
        }
    }
}
