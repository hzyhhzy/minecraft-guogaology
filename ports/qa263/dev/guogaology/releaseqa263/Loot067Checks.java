package dev.guogaology.releaseqa263;

import com.google.gson.*;
import dev.guogaology.mining.GearData;
import dev.guogaology.mining.MiningContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.sdl.SDLVideo;

import java.nio.file.*;
import java.util.*;

/** Exact old-release writer and fixed-release verifier. Never packaged in the production Mod. */
public final class Loot067Checks implements ClientModInitializer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String WORLD = "loot067-fixture";
    private static final String FIXTURE = "loot067-fixture.json";
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final BlockPos STORED = new BlockPos(-5, 220, 0);
    private static final BlockPos UNOPENED = new BlockPos(5, 220, 0);
    private static final BlockPos DISPLAY = new BlockPos(0, 220, 4);
    private static final String[] LEGACY = {"player_placed", "stable", "anchored"};
    private static final List<ResourceKey<Level>> REALMS = List.of(Level.OVERWORLD, realm("outer"), realm("guogaology"), realm("guogao"));
    private static final Map<String, Set<String>> TAGS = Map.of(
        "#ordinal_branch_stones", Set.of("epsilon_symbol", "omega_symbol", "great_omega_symbol", "zeta_symbol", "psi_symbol", "phi_symbol"),
        "#ordinal_flowers", Set.of("epsilon_bloom", "omega_bloom", "great_omega_bloom", "zeta_vine", "psi_fern", "phi_bloom"),
        "#fruit_guogao", Set.of("amber_guogao", "berry_guogao", "lime_guogao", "azure_guogao", "plain_amber_guogao", "plain_berry_guogao", "plain_lime_guogao", "plain_azure_guogao"));
    private record Spec(String theme, String core, String decoration, String[] ring) {}
    private record Group(String token, int minimum, int maximum, Set<String> alternatives) {}
    private static Spec spec(String theme, String core, String decoration, String... ring) { return new Spec(theme, core, decoration, ring); }
    private static final List<Spec> SPECS = List.of(
        spec("matrix", "sequence_core", "matrix_archive_ceramic", "ordinal_bricks", "matrix_sheet", "ordinal_bricks", "ridge_lamina", "ridge_lamina", "ordinal_bricks", "matrix_sheet", "ordinal_bricks"),
        spec("power", "power_tower_core", "recursive_bronze", "power_bricks", "ordinal_bricks", "power_bricks", "ordinal_bricks", "ordinal_bricks", "power_bricks", "ordinal_bricks", "power_bricks"),
        spec("hydra", "hydra_bud", "hydra_jade", "#ordinal_branch_stones", "hydra_sprig", "#ordinal_branch_stones", "#ordinal_flowers", "#ordinal_flowers", "#ordinal_branch_stones", "hydra_sprig", "#ordinal_branch_stones"),
        spec("absence", "lho_trace", "lho_letter_l", "absence_glass", "stellar_stone", "absence_glass", "fffz_trace", "fos_trace", "absence_glass", "stellar_stone", "absence_glass"),
        spec("weaver", "laver_core", "resonant_silk", "laver_ribbon", "iblp_marked", "laver_ribbon", "iblp_marked", "iblp_marked", "laver_ribbon", "iblp_marked", "laver_ribbon"),
        spec("astra", "astra_critical_core", "astra_marble", "compute_chip", "cloud_pearl", "compute_chip", "nuke_mushroom", "nuke_mushroom", "compute_chip", "nuke_mushroom", "compute_chip"),
        spec("frontier", "boundary_core", "axiom_porcelain", "turing_tape", "turing_endcap", "turing_tape", "set_membrane", "set_membrane", "turing_tape", "turing_endcap", "turing_tape"),
        spec("guogao", "guogao_heart", "guogao_heart_resin", "fruit_cake_gel", "guogao_drop", "fruit_cake_gel", "#fruit_guogao", "#fruit_guogao", "fruit_cake_gel", "#fruit_guogao", "fruit_cake_gel"));

    private Path game;
    private String mode, version;
    private JsonObject manifest;
    private final JsonObject report = new JsonObject();
    private final JsonArray pictures = new JsonArray();
    private final long deadline = System.nanoTime() + 1_050_000_000_000L;
    private boolean opening, queued, capturing, done, backupAccepted;
    private volatile boolean ready, captured, armedSuccess;
    private volatile Throwable failure;
    private volatile MinecraftServer server;
    private int checks, ticks, scene, chestFills;
    private volatile Vec3 camera = new Vec3(.5, 224, 21.5);

    private static Identifier id(String name) { return Identifier.parse(name.contains(":") ? name : "guogaology:" + name); }
    private static ResourceKey<Level> realm(String name) { return ResourceKey.create(Registries.DIMENSION, id(name)); }
    private static ResourceKey<LootTable> loot(String name) { return ResourceKey.create(Registries.LOOT_TABLE, id(name)); }
    private static Block block(String name) { return BuiltInRegistries.BLOCK.getValue(id(name)); }
    private static ItemStack item(String name, int count) { return new ItemStack(BuiltInRegistries.ITEM.getValue(id(name)), count); }
    private static BlockPos legacyPosition(int mask) { return new BlockPos(-12 + mask * 3, 220, 8); }
    private void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError("Loot067: " + message); }
    private boolean baseline() { return mode.equals("baseline"); }
    private boolean broken() { return mode.equals("broken"); }

    @Override public void onInitializeClient() {
        try {
            game = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
            check(Boolean.getBoolean("guogaology.qa263") && Boolean.getBoolean("guogaology.loot067"), "explicit isolated flags");
            check(Files.isRegularFile(game.resolve("qa263-isolated.json")), "fresh staging marker");
            JsonObject input = JsonParser.parseString(Files.readString(game.resolve("loot067-inputs.json"))).getAsJsonObject();
            check(game.startsWith(Path.of(input.get("buildRoot").getAsString()).toAbsolutePath().normalize()), "game directory stays inside build");
            mode = input.get("mode").getAsString();
            version = FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString();
            check(version.equals(input.get("productionVersion").getAsString()), "exact requested production version");
            check(version.equals(baseline() ? "0.6.0" : broken() ? "0.6.6" : "0.6.7"), "mode uses the intended old/broken/fixed release");
            check(FabricLoader.getInstance().getModContainer("fabricloader").orElseThrow().getMetadata().getVersion().getFriendlyString().equals(input.get("loaderVersion").getAsString()), "actual Loader matches staging");
            check(FabricLoader.getInstance().getModContainer("fabric-api").orElseThrow().getMetadata().getVersion().getFriendlyString().equals(input.get("apiVersion").getAsString()), "actual Fabric API matches staging");
            report.add("inputs", input);
            ServerLifecycleEvents.SERVER_STARTED.register(value -> {
                server = value;
                try { Files.writeString(game.resolve("loot067-world-started.json"), "{\"started\":true,\"version\":\"" + version + "\"}", StandardOpenOption.CREATE_NEW); }
                catch (Throwable error) { failure = error; }
            });
            ServerLifecycleEvents.SERVER_STOPPED.register(value -> {
                if (!armedSuccess || failure != null || broken() || value != server) return;
                try {
                    report.addProperty("serverStopped", true);
                    report.addProperty("success", true);
                    report.addProperty("checks", checks);
                    Files.writeString(game.resolve("loot067-ok.json"), JSON.toJson(report), StandardOpenOption.CREATE_NEW);
                    System.out.println("LOOT067_CLEAN_STOP_OK mode=" + mode + " checks=" + checks);
                } catch (Throwable error) { error.printStackTrace(); }
            });
            ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    private void hidden(Minecraft client) {
        check(!client.mouseHandler.isMouseGrabbed(), "QA mouse stays unlocked");
        long flags = SDLVideo.SDL_GetWindowFlags(client.getWindow().handle());
        check((flags & SDLVideo.SDL_WINDOW_HIDDEN) != 0, "SDL window remains hidden");
        check((flags & SDLVideo.SDL_WINDOW_NOT_FOCUSABLE) != 0, "SDL window cannot take focus");
    }

    private void tick(Minecraft client) {
        if (done) return;
        try {
            if (failure != null) throw new RuntimeException(failure);
            if (System.nanoTime() > deadline) throw new AssertionError("deadline at " + mode + " scene " + scene);
            client.options.pauseOnLostFocus = false;
            client.options.renderDistance().set(3);
            client.options.framerateLimit().set(60);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            client.gui.toastManager().clear();
            if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
            if (client.level == null) {
                if (!opening && client.isGameLoadFinished() && client.gui.overlay() == null) {
                    opening = true;
                    if (baseline() || mode.equals("fresh")) {
                        check(!Files.exists(game.resolve("saves").resolve(WORLD)), "new world cannot reuse a prior save");
                        var settings = new LevelSettings("Guogaology isolated loot compatibility", GameType.CREATIVE,
                            LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
                        client.createWorldOpenFlows().createFreshLevel(WORLD, settings,
                            new WorldOptions(Long.getLong("guogaology.qa263.seed", 67060L), true, false),
                            WorldPresets::createNormalWorldDimensions, new TitleScreen());
                    } else {
                        check(Files.isRegularFile(game.resolve("saves").resolve(WORLD).resolve(FIXTURE)), "open an actual saved fixture copy");
                        client.createWorldOpenFlows().openWorld(WORLD, () -> fail(client, new AssertionError("world open cancelled")));
                    }
                }
                // Only the ordinary experimental-world backup prompt is bypassed, for this disposable copy.
                if (opening && !backupAccepted && client.gui.screen() instanceof BackupConfirmScreen screen) {
                    backupAccepted = true;
                    var field = BackupConfirmScreen.class.getDeclaredField("onProceed");
                    field.setAccessible(true);
                    ((BackupConfirmScreen.Listener)field.get(screen)).proceed(false, false);
                }
                if (opening && client.gui.screen() instanceof DatapackLoadFailureScreen) {
                    if (!broken()) throw new AssertionError("fixed/old production reached native DatapackLoadFailureScreen");
                    hidden(client);
                    if (++ticks < 90) return;
                    if (!capturing) capture(client, "loot067-broken066-datapack-failure.png");
                    if (!captured) return;
                    check(server == null && client.getSingleplayerServer() == null, "broken release never starts a server");
                    var failureReport = new JsonObject();
                    failureReport.addProperty("mode", mode);
                    failureReport.addProperty("productionVersion", version);
                    failureReport.addProperty("screen", "DatapackLoadFailureScreen");
                    failureReport.addProperty("hidden", true);
                    failureReport.addProperty("mouseUnlocked", true);
                    failureReport.addProperty("serverStarted", false);
                    failureReport.addProperty("screenshot", "loot067-broken066-datapack-failure.png");
                    Files.writeString(game.resolve("loot067-rejected.json"), JSON.toJson(failureReport), StandardOpenOption.CREATE_NEW);
                    // This marker is NOT success. The Python runner must independently match exact errors/table IDs.
                    done = true; client.stop();
                }
                return;
            }
            if (broken()) throw new AssertionError("0.6.6 unexpectedly reached a loaded world");
            if (client.player == null || client.getSingleplayerServer() == null || client.gui.overlay() != null) return;
            if (!queued) {
                queued = true;
                var uuid = client.player.getUUID();
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = server.getPlayerList().getPlayer(uuid);
                        check(player != null, "real integrated player");
                        player.setGameMode(GameType.CREATIVE);
                        if (baseline() || mode.equals("fresh")) writeFixture(player);
                        verifyFixture(player);
                        if (!baseline()) { verifyLoot(player); displayLoot(player); }
                        move(player, new Vec3(.5, 224, 21.5));
                        ready = true;
                    } catch (Throwable error) { failure = error; }
                });
                return;
            }
            if (!ready || !client.level.dimension().equals(Level.OVERWORLD) || client.player.position().distanceToSqr(camera) > 1) return;
            if (scene == 0 ? client.gui.screen() != null : !(client.gui.screen() instanceof AbstractContainerScreen<?>)) return;
            client.player.setYRot(180); client.player.yRotO = 180; client.player.setYHeadRot(180);
            client.player.setXRot(scene == 0 ? 14 : 20); client.player.xRotO = scene == 0 ? 14 : 20;
            if (++ticks < 180) return;
            if (!capturing) {
                hidden(client);
                check(client.level.getBlockState(legacyPosition(0)).is(block("lho_trace")), "real legacy core reaches native client");
                check(GearData.digit(client.player.getInventory().getItem(0)) == 7, "native client receives persisted digit component");
                capture(client, "loot067-" + mode + (scene == 0 ? "-world.png" : "-chest.png"));
            }
            if (!captured) return;
            if (!baseline() && scene == 0) {
                scene = 1; ticks = 0; capturing = false; captured = false; ready = false;
                var uuid = client.player.getUUID();
                server.execute(() -> {
                    try {
                        var player = server.getPlayerList().getPlayer(uuid);
                        move(player, new Vec3(.5, 221, 8.5));
                        player.openMenu((ChestBlockEntity)server.overworld().getBlockEntity(DISPLAY));
                        player.containerMenu.broadcastChanges();
                        ready = true;
                    } catch (Throwable error) { failure = error; }
                });
                return;
            }
            done = true;
            var uuid = client.player.getUUID();
            server.execute(() -> {
                try {
                    var player = server.getPlayerList().getPlayer(uuid);
                    player.closeContainer();
                    verifyFixture(player);
                    check(server.saveEverything(false, true, true), "native saveEverything flushes actual chunks/player");
                    report.addProperty("mode", mode);
                    report.addProperty("version", version);
                    report.addProperty("minecraft", "26.3");
                    report.addProperty("hidden", true);
                    report.addProperty("mouseUnlocked", true);
                    report.addProperty("nativeSaveCompleted", true);
                    report.addProperty("fixtureProducer", manifest.get("producerVersion").getAsString());
                    report.addProperty("fixtureDimensions", REALMS.size());
                    report.add("screenshots", pictures);
                    report.addProperty("chestFills", chestFills);
                    armedSuccess = true;
                    client.execute(client::stop);
                } catch (Throwable error) { failure = error; client.execute(() -> fail(client, error)); }
            });
        } catch (Throwable error) { fail(client, error); }
    }

    private void fail(Minecraft client, Throwable error) {
        done = true; armedSuccess = false; failure = error; error.printStackTrace();
        try { Files.writeString(game.resolve("loot067-failed.txt"), error.toString(), StandardOpenOption.CREATE_NEW); }
        catch (Exception ignored) {}
        client.stop();
    }
    private void capture(Minecraft client, String name) throws Exception {
        capturing = true;
        Path folder = game.resolve("screenshots"); Files.createDirectories(folder);
        Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
            try (image) { image.writeToFile(folder.resolve(name)); captured = true; }
            catch (Throwable error) { failure = error; }
        });
        pictures.add(name);
    }
    private void move(ServerPlayer player, Vec3 target) {
        camera = target;
        player.teleport(new TeleportTransition(server.overworld(), target, Vec3.ZERO, 180, 14, TeleportTransition.DO_NOTHING));
        player.getAbilities().flying = true; player.onUpdateAbilities();
    }

    private BlockState legacy(Block type, int mask) {
        BlockState state = type.defaultBlockState();
        for (int i = 0; i < LEGACY.length; i++) {
            var property = type.getStateDefinition().getProperty(LEGACY[i]);
            check(property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty, "old Boolean property remains " + LEGACY[i]);
            state = state.setValue((net.minecraft.world.level.block.state.properties.BooleanProperty)property, (mask & (1 << i)) != 0);
        }
        return state;
    }
    private ItemStack named(String name, int count, String title) {
        ItemStack stack = item(name, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(title));
        var custom = new CompoundTag(); custom.putString("loot067_token", "old-release-kept");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        return stack;
    }
    private ItemStack oldCore(int count, int mask) {
        ItemStack stack = named("lho_trace", count, "Legacy Absence fixture");
        var values = new LinkedHashMap<String, String>();
        for (int i = 0; i < LEGACY.length; i++) values.put(LEGACY[i], Boolean.toString((mask & (1 << i)) != 0));
        values.put("loot067_unknown", "keep-this-key");
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(values));
        // The native constructor/copy hook belongs to the actually loaded release:
        // 0.6.0 retains all flags, while fresh0.6.7 copies normalize them naturally.
        return stack.copy();
    }
    /** Compare persistent ownership fields, omitting derived attributes and only the documented retired Lv1 flags. */
    private JsonObject stack(ItemStack stack) {
        JsonObject value = new JsonObject();
        if (stack.isEmpty()) { value.addProperty("empty", true); return value; }
        value.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        value.addProperty("count", stack.getCount()); value.addProperty("damage", stack.getDamageValue());
        if (stack.has(MiningContent.DIGIT)) value.addProperty("digit", stack.get(MiningContent.DIGIT));
        if (stack.has(DataComponents.CUSTOM_NAME)) value.addProperty("name", stack.get(DataComponents.CUSTOM_NAME).getString());
        if (stack.has(DataComponents.CUSTOM_DATA)) value.addProperty("customData", stack.get(DataComponents.CUSTOM_DATA).copyTag().toString());
        if (stack.has(DataComponents.BLOCK_STATE)) {
            var properties = new TreeMap<>(stack.get(DataComponents.BLOCK_STATE).properties());
            if (stack.is(item("lho_trace", 1).getItem())) for (String key : LEGACY) properties.remove(key);
            if (!properties.isEmpty()) value.add("itemState", JSON.toJsonTree(properties));
        }
        if (stack.has(MiningContent.CORES)) {
            var cores = new JsonArray(); for (var core : stack.get(MiningContent.CORES)) cores.add(stack(core)); value.add("cores", cores);
        }
        return value;
    }
    private JsonArray inventory(net.minecraft.world.Container inventory, int count) {
        JsonArray values = new JsonArray(); for (int i = 0; i < count; i++) values.add(stack(inventory.getItem(i))); return values;
    }
    private void legacyItemFlags(ItemStack stack, String where) {
        if (stack.isEmpty()) return;
        if (stack.is(item("lho_trace", 1).getItem())) {
            var state = stack.get(DataComponents.BLOCK_STATE);
            for (String key : LEGACY) {
                boolean present = state != null && state.properties().containsKey(key);
                check(present == baseline(), where + " exact finished-Lv1 core " + key + (baseline() ? " stays in actual060" : " is retired by actual067"));
            }
            check(state != null && "keep-this-key".equals(state.properties().get("loot067_unknown")), where + " unrelated saved item-state key survives");
        }
        if (stack.has(MiningContent.CORES)) {
            var slots = stack.get(MiningContent.CORES);
            for (int slot = 0; slot < slots.size(); slot++) legacyItemFlags(slots.get(slot), where + "/socket" + slot);
        }
    }
    private void rememberBlock(JsonArray rows, ServerLevel level, BlockPos position) {
        var row = new JsonObject(); row.addProperty("x", position.getX()); row.addProperty("y", position.getY()); row.addProperty("z", position.getZ());
        row.addProperty("state", level.getBlockState(position).toString()); rows.add(row);
    }
    private void writeFixture(ServerPlayer player) throws Exception {
        Path folder = server.getWorldPath(LevelResource.ROOT);
        check(!Files.exists(folder.resolve(FIXTURE)), "fixture manifest is new");
        var dimensions = new JsonArray(); int index = 0;
        for (var realm : REALMS) {
            ServerLevel level = server.getLevel(realm); check(level != null, "all four actual dimensions exist " + realm.identifier());
            for (int cx = -1; cx <= 1; cx++) for (int cz = -1; cz <= 1; cz++) level.getChunk(cx, cz);
            for (var pos : BlockPos.betweenClosed(-16, 219, -4, 16, 226, 18))
                level.setBlock(pos, (pos.getY() == 219 ? Blocks.SMOOTH_QUARTZ : Blocks.AIR).defaultBlockState(), FLAGS);
            var sentinels = new JsonArray();
            Block[] types = {Blocks.DIAMOND_BLOCK, block("epsilon_block"), block("ordinal_bricks")};
            for (int i = 0; i < types.length; i++) { var pos = new BlockPos(-2 + i * 2, 220, 0); level.setBlock(pos, types[i].defaultBlockState(), FLAGS); rememberBlock(sentinels, level, pos); }
            level.setBlock(STORED, Blocks.CHEST.defaultBlockState(), FLAGS);
            var stored = (ChestBlockEntity)level.getBlockEntity(STORED);
            stored.setItem(0, named("true_omega_material", 3 + index, "Saved material " + index));
            stored.setItem(1, item("minecraft:stick", 17 + index));
            stored.setItem(2, oldCore(17, 0)); stored.setItem(3, oldCore(19, 7)); stored.setChanged();
            level.setBlock(UNOPENED, Blocks.CHEST.defaultBlockState(), FLAGS);
            var unopened = (ChestBlockEntity)level.getBlockEntity(UNOPENED);
            unopened.setLootTable(loot("chests/guogao_sanctum"), 67060L + index); unopened.setChanged();
            var dimension = new JsonObject(); dimension.addProperty("dimension", realm.identifier().toString()); dimension.add("blocks", sentinels);
            dimension.add("stored", inventory(stored, 4)); dimension.addProperty("unopenedKey", unopened.getLootTable().identifier().toString()); dimension.addProperty("unopenedSeed", unopened.getLootTableSeed());
            dimensions.add(dimension); index++;
        }
        var world = server.overworld(); var legacy = new JsonArray();
        for (int mask = 4; mask < 8; mask++) world.setBlock(legacyPosition(mask).offset(0, -2, 2), block("ordinal_crystal").defaultBlockState(), FLAGS);
        for (int mask = 0; mask < 8; mask++) { world.setBlock(legacyPosition(mask), legacy(block("lho_trace"), mask), FLAGS); rememberBlock(legacy, world, legacyPosition(mask)); }
        for (int i = 0; i < 4; i++) {
            var pos = new BlockPos(-12 + i * 4, 220, 15);
            world.setBlock(pos, legacy(block(new String[]{"fffz_trace", "fos_trace", "lho_hydra_psi", "lho_hydra_z"}[i]), 3), FLAGS); rememberBlock(legacy, world, pos);
        }
        player.getInventory().clearContent();
        var pick = named("number_pickaxe", 1, "Saved numeric pickaxe"); pick.set(MiningContent.DIGIT, 7d); GearData.refresh(pick); pick.setDamageValue(11);
        var sword = named("number_sword", 1, "Saved numeric sword"); sword.set(MiningContent.DIGIT, 4d); GearData.refresh(sword); sword.setDamageValue(9);
        var socketed = named("true_omega_pickaxe", 1, "Saved socket order");
        GearData.setCores(socketed, List.of(item("hydra_bud_lv2", 1), ItemStack.EMPTY, item("ordinal_crystal_lv3", 1), ItemStack.EMPTY, oldCore(1, 7))); socketed.setDamageValue(23);
        for (var gear : List.of(pick, sword, socketed)) { gear.set(MiningContent.DEEP, false); GearData.refresh(gear); }
        player.getInventory().setItem(0, pick); player.getInventory().setItem(1, sword); player.getInventory().setItem(2, socketed); player.getInventory().setItem(3, oldCore(5, 7)); player.getInventory().setChanged();
        manifest = new JsonObject(); manifest.addProperty("schema", 1); manifest.addProperty("producerVersion", version);
        manifest.addProperty("fixtureOnly", true); manifest.add("dimensions", dimensions); manifest.add("legacyBlocks", legacy); manifest.add("player", inventory(player.getInventory(), 4));
        Files.writeString(folder.resolve(FIXTURE), JSON.toJson(manifest), StandardOpenOption.CREATE_NEW);
        check(server.saveEverything(false, true, true), "old/fresh native fixture flush succeeds");
    }
    private void verifyBlocks(ServerLevel level, JsonArray rows) {
        for (var value : rows) {
            var row = value.getAsJsonObject(); var pos = new BlockPos(row.get("x").getAsInt(), row.get("y").getAsInt(), row.get("z").getAsInt());
            level.getChunk(pos); check(level.getBlockState(pos).toString().equals(row.get("state").getAsString()), "saved native block state " + level.dimension().identifier() + " " + pos);
        }
    }
    private void verifyFixture(ServerPlayer player) throws Exception {
        manifest = JsonParser.parseString(Files.readString(server.getWorldPath(LevelResource.ROOT).resolve(FIXTURE))).getAsJsonObject();
        check(manifest.get("fixtureOnly").getAsBoolean() && manifest.get("schema").getAsInt() == 1, "known dedicated fixture schema");
        String producer = manifest.get("producerVersion").getAsString();
        check(mode.equals("fresh") ? producer.equals("0.6.7") : baseline() || mode.equals("upgrade") ? producer.equals("0.6.0") : Set.of("0.6.0", "0.6.7").contains(producer), "fixture producer matches old upgrade/fresh/reopen mode");
        check(manifest.getAsJsonArray("dimensions").size() == 4, "four persisted dimensions");
        for (var value : manifest.getAsJsonArray("dimensions")) {
            var row = value.getAsJsonObject(); var key = ResourceKey.create(Registries.DIMENSION, id(row.get("dimension").getAsString()));
            var level = server.getLevel(key); check(level != null, "saved dimension remains " + key.identifier()); verifyBlocks(level, row.getAsJsonArray("blocks"));
            level.getChunk(STORED); level.getChunk(UNOPENED);
            check(level.getBlockEntity(STORED) instanceof ChestBlockEntity, "saved actual item chest");
            check(inventory((ChestBlockEntity)level.getBlockEntity(STORED), 4).equals(row.get("stored")), "all saved chest ownership/count/custom fields " + key.identifier());
            for (int slot = 0; slot < 4; slot++) legacyItemFlags(((ChestBlockEntity)level.getBlockEntity(STORED)).getItem(slot), key.identifier() + "/chest" + slot);
            check(level.getBlockEntity(UNOPENED) instanceof ChestBlockEntity, "saved unopened chest exists");
            var unopened = (ChestBlockEntity)level.getBlockEntity(UNOPENED);
            check(unopened.getLootTable() != null && unopened.getLootTable().identifier().toString().equals(row.get("unopenedKey").getAsString()), "unopened real chest preserves loot table key");
            check(unopened.getLootTableSeed() == row.get("unopenedSeed").getAsLong(), "unopened real chest preserves random seed");
        }
        verifyBlocks(server.overworld(), manifest.getAsJsonArray("legacyBlocks"));
        check(inventory(player.getInventory(), 4).equals(manifest.get("player")), "saved player numeric digits/damage/names/custom data/core slot order and empty slots");
        for (int slot = 0; slot < 4; slot++) legacyItemFlags(player.getInventory().getItem(slot), "player" + slot);
        report.addProperty("persistedLegacyBlockStates", manifest.getAsJsonArray("legacyBlocks").size());
        report.addProperty("persistedPlayerStacks", 4); report.addProperty("unopenedChests", 4);
    }

    private static long mixed(long value) { value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L; value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL; return value ^ (value >>> 31); }
    private static List<Group> groups(Spec spec, boolean main) {
        Map<String, Integer> counts = new LinkedHashMap<>(); counts.put("ordinal_crystal", 1); for (String token : spec.ring()) counts.merge(token, 1, Integer::sum);
        var result = new ArrayList<Group>(); String core = spec.core() + (main ? "_lv2" : ""); result.add(new Group(core, 1, 1, Set.of(core)));
        for (var entry : counts.entrySet()) { int n = entry.getValue(); result.add(new Group(entry.getKey(), main ? 2*n : Math.max(1,n-1), main ? 4*n : n == 1 ? 1 : n+1, entry.getKey().startsWith("#") ? TAGS.get(entry.getKey()) : Set.of(entry.getKey()))); }
        if (main) result.add(new Group("true_omega_material", 2, 4, Set.of("true_omega_material")));
        result.add(new Group(spec.decoration(), 16, 24, Set.of(spec.decoration()))); return result;
    }
    private Map<String, Integer> chestContents(net.minecraft.world.Container chest, List<Group> groups, String key, Map<String, Set<String>> seen) {
        var allowed = new HashMap<String, Group>(); for (var group : groups) for (String member : group.alternatives()) check(allowed.put(member, group) == null, "unambiguous loot category");
        var totals = new HashMap<String, Integer>(); var selected = new HashMap<String, Set<String>>();
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            var stack = chest.getItem(slot); if (stack.isEmpty()) continue; var name = BuiltInRegistries.ITEM.getKey(stack.getItem());
            check(name.getNamespace().equals("guogaology") && allowed.containsKey(name.getPath()), "real chest contains only expected items " + key + " " + name);
            check(stack.getCount() <= stack.getMaxStackSize(), "native maximum stack count"); var group = allowed.get(name.getPath());
            totals.merge(group.token(), stack.getCount(), Integer::sum); selected.computeIfAbsent(group.token(), ignored -> new HashSet<>()).add(name.getPath());
        }
        check(totals.size() == groups.size(), "no guaranteed group missing from native fill " + key + " " + totals);
        for (var group : groups) { int n = totals.getOrDefault(group.token(), 0); check(n >= group.minimum() && n <= group.maximum(), "native count bounds " + key + " " + group.token() + "=" + n); check(selected.get(group.token()).size() == 1, "one alternative selected per pool " + key); seen.computeIfAbsent(group.token(), ignored -> new HashSet<>()).addAll(selected.get(group.token())); }
        return totals;
    }
    private void verifyLoot(ServerPlayer player) {
        var registry = server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE);
        var tables = registry.listElementIds().filter(key -> key.identifier().getNamespace().equals("guogaology")).sorted(Comparator.comparing(key -> key.identifier().toString())).toList();
        check(tables.size() == 248, "all 248 actual Guogaology loot tables survived full normal resource load: " + tables.size());
        for (var key : tables) check(registry.get(key).isPresent() && server.reloadableRegistries().getLootTable(key) != LootTable.EMPTY, "actual registered nonfallback loot table " + key.identifier());
        report.addProperty("loadedLootTables", tables.size());
        for (var entry : TAGS.entrySet()) {
            var members = BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id(entry.getKey().substring(1)))).orElseThrow();
            var actual = new HashSet<String>(); members.stream().forEach(holder -> { var name = holder.unwrapKey().orElseThrow().identifier(); check(name.getNamespace().equals("guogaology"), "actual tag member namespace"); actual.add(name.getPath()); });
            check(actual.equals(entry.getValue()), "actual synchronized tag membership " + entry.getKey() + actual);
        }
        var params = new LootParams.Builder(player.level()).withParameter(LootContextParams.ORIGIN, player.position()).withOptionalParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
        var chestReports = new JsonArray(); int tableIndex = 0;
        for (var spec : SPECS) for (boolean main : new boolean[]{true, false}) {
            String name = "chests/" + spec.theme() + (main ? "_sanctum" : "_ruin"); var table = server.reloadableRegistries().getLootTable(loot(name)); var groups = groups(spec, main);
            var sums = new HashMap<String, Long>(); var minima = new HashMap<String, Integer>(); var maxima = new HashMap<String, Integer>(); var seen = new HashMap<String, Set<String>>();
            for (int sample = 0; sample < 512; sample++) {
                var chest = new SimpleContainer(27); table.fill(chest, params, mixed(6700000L + tableIndex * 10000L + sample)); chestFills++;
                var totals = chestContents(chest, groups, name, seen);
                for (var entry : totals.entrySet()) { sums.merge(entry.getKey(), (long)entry.getValue(), Long::sum); minima.merge(entry.getKey(), entry.getValue(), Math::min); maxima.merge(entry.getKey(), entry.getValue(), Math::max); }
            }
            var row = new JsonObject(); row.addProperty("table", name); row.addProperty("fills", 512); var measurements = new JsonArray();
            for (var group : groups) {
                double mean = sums.get(group.token()) / 512.0; check(minima.get(group.token()) == group.minimum() && maxima.get(group.token()) == group.maximum(), "both count endpoints reached " + name + group.token());
                check(Math.abs(mean - (group.minimum()+group.maximum())/2.0) < Math.max(.20, (group.maximum()-group.minimum())*.08), "normal uniform mean " + name + group.token() + "=" + mean);
                check(seen.get(group.token()).equals(group.alternatives()), "every legal tag alternative actually drawn " + name + group.token());
                var measurement = new JsonObject(); measurement.addProperty("group", group.token()); measurement.addProperty("minimum", minima.get(group.token())); measurement.addProperty("maximum", maxima.get(group.token())); measurement.addProperty("mean", mean); measurement.add("seen", JSON.toJsonTree(new TreeSet<>(seen.get(group.token())))); measurements.add(measurement);
            }
            row.add("groups", measurements); chestReports.add(row); tableIndex++;
        }
        report.add("chests", chestReports);
        var entities = new JsonArray();
        for (String name : List.of("fruit_cake_slime", "fruit_slime")) {
            var type = BuiltInRegistries.ENTITY_TYPE.getValue(id(name)); var entity = type.create(player.level(), EntitySpawnReason.TRIGGERED); check(entity != null, "actual registered gel entity");
            var parameters = new LootParams.Builder(player.level()).withParameter(LootContextParams.ORIGIN, player.position()).withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.DAMAGE_SOURCE, player.level().damageSources().generic()).create(LootContextParamSets.ENTITY);
            int total = 0; for (var drop : server.reloadableRegistries().getLootTable(loot("entities/" + name)).getRandomItems(parameters, mixed(67100L + entities.size()))) { check(drop.is(item("fruit_cake_gel", 1).getItem()), "actual gel loot table runs"); total += drop.getCount(); }
            check(total == (name.equals("fruit_cake_slime") ? 2 : 1), "native no-player gel base quantity " + name);
            var row = new JsonObject(); row.addProperty("entity", name); row.addProperty("baseGel", total); entities.add(row);
        }
        report.add("entityLoot", entities);
    }
    private void displayLoot(ServerPlayer player) {
        var world = server.overworld(); var params = new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(DISPLAY)).withOptionalParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
        for (int i = 0; i < 4; i++) {
            var pos = DISPLAY.offset((i-1)*3, 0, 0); world.setBlock(pos, Blocks.CHEST.defaultBlockState(), FLAGS);
            var chest = (ChestBlockEntity)world.getBlockEntity(pos); chest.clearContent();
            String name = new String[]{"hydra_sanctum", "guogao_sanctum", "hydra_ruin", "guogao_ruin"}[i];
            server.reloadableRegistries().getLootTable(loot("chests/" + name)).fill(chest, params, mixed(67067L + i)); chestFills++; chest.setChanged();
            var spec = SPECS.get(i % 2 == 0 ? 2 : 7); chestContents(chest, groups(spec, i < 2), name, new HashMap<>());
        }
        report.addProperty("actualBlockEntityChestFills", 4);
    }
}
