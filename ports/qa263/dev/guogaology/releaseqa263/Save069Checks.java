package dev.guogaology.releaseqa263;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.sdl.SDLVideo;

import java.nio.file.*;
import java.util.*;

/** Real old-JAR writer -> new-JAR disk upgrade -> second clean reopen. QA only. */
public final class Save069Checks implements ClientModInitializer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String WORLD = "save069-fixture", FIXTURE = "save069-fixture.json";
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final BlockPos CHEST = new BlockPos(0, 220, -2), CONTROL = new BlockPos(72, 230, 72);
    private static final String[] LEGACY = {"player_placed", "stable", "anchored"};
    private static final List<ResourceKey<Level>> REALMS = List.of(Level.OVERWORLD, realm("outer"), realm("guogaology"), realm("guogao"));
    private static final Vec3 CAMERA = new Vec3(.5, 224, 35.5);
    private Path game;
    private String mode, version;
    private JsonObject manifest;
    private final JsonObject report = new JsonObject();
    private final long deadline = System.nanoTime() + 1_050_000_000_000L;
    private boolean opening, prepareQueued, verifyQueued, capturing, done, backupAccepted;
    private volatile boolean prepared, verified, captured, armedSuccess;
    private volatile Throwable failure;
    private volatile MinecraftServer server;
    private int checks, ticks;

    private static Identifier id(String name) { return Identifier.parse(name.contains(":") ? name : "guogaology:" + name); }
    private static ResourceKey<Level> realm(String name) { return ResourceKey.create(Registries.DIMENSION, id(name)); }
    private static Block block(String name) { return BuiltInRegistries.BLOCK.getValue(id(name)); }
    private static ItemStack item(String name, int count) { return new ItemStack(BuiltInRegistries.ITEM.getValue(id(name)), count); }
    private void check(boolean value, String why) { checks++; if (!value) throw new AssertionError("Save069: " + why); }
    private boolean baseline() { return mode.startsWith("baseline"); }

    @Override public void onInitializeClient() {
        try {
            game = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
            check(Boolean.getBoolean("guogaology.qa263") && Boolean.getBoolean("guogaology.save069"), "isolated QA flags");
            check(Files.isRegularFile(game.resolve("qa263-isolated.json")), "fresh runner marker");
            var input = JsonParser.parseString(Files.readString(game.resolve("save069-inputs.json"))).getAsJsonObject();
            check(game.startsWith(Path.of(input.get("buildRoot").getAsString()).toAbsolutePath().normalize()), "build-only game directory");
            mode = input.get("mode").getAsString();
            version = FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString();
            check(version.equals(input.get("productionVersion").getAsString()), "requested exact production version");
            check(version.equals(mode.equals("baseline060") ? "0.6.0" : mode.equals("baseline068") ? "0.6.8" : "0.6.9"), "old/fixed production boundary");
            check(FabricLoader.getInstance().getModContainer("fabricloader").orElseThrow().getMetadata().getVersion().getFriendlyString().equals(input.get("loaderVersion").getAsString()), "actual Loader version");
            check(FabricLoader.getInstance().getModContainer("fabric-api").orElseThrow().getMetadata().getVersion().getFriendlyString().equals(input.get("apiVersion").getAsString()), "actual API version");
            report.add("inputs", input);
            ServerLifecycleEvents.SERVER_STARTED.register(value -> {
                server = value;
                try { Files.writeString(game.resolve("save069-world-started.json"), "{\"started\":true}", StandardOpenOption.CREATE_NEW); }
                catch (Throwable error) { failure = error; }
            });
            ServerLifecycleEvents.SERVER_STOPPED.register(value -> {
                if (!armedSuccess || failure != null || value != server) return;
                try {
                    report.addProperty("serverStopped", true); report.addProperty("success", true); report.addProperty("checks", checks);
                    Files.writeString(game.resolve("save069-ok.json"), JSON.toJson(report), StandardOpenOption.CREATE_NEW);
                    System.out.println("SAVE069_CLEAN_STOP_OK mode=" + mode + " checks=" + checks);
                } catch (Throwable error) { error.printStackTrace(); }
            });
            ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    private void tick(Minecraft client) {
        if (done) return;
        try {
            if (failure != null) throw new RuntimeException(failure);
            if (System.nanoTime() > deadline) throw new AssertionError("Save069 deadline: " + mode);
            client.options.pauseOnLostFocus = false; client.options.renderDistance().set(3); client.options.framerateLimit().set(60);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE); client.gui.toastManager().clear();
            if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
            if (client.level == null) {
                if (!opening && client.isGameLoadFinished() && client.gui.overlay() == null) {
                    opening = true;
                    if (baseline()) {
                        check(!Files.exists(game.resolve("saves").resolve(WORLD)), "old writer starts a genuine fresh save");
                        var settings = new LevelSettings("Guogaology save compatibility fixture", GameType.CREATIVE,
                            LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
                        client.createWorldOpenFlows().createFreshLevel(WORLD, settings,
                            new WorldOptions(Long.getLong("guogaology.qa263.seed", 69068L), true, false), WorldPresets::createNormalWorldDimensions, new TitleScreen());
                    } else {
                        check(Files.isRegularFile(game.resolve("saves").resolve(WORLD).resolve(FIXTURE)), "real old disk fixture was copied");
                        client.createWorldOpenFlows().openWorld(WORLD, () -> fail(client, new AssertionError("fixture opening cancelled")));
                    }
                }
                if (opening && !backupAccepted && client.gui.screen() instanceof BackupConfirmScreen screen) {
                    backupAccepted = true; var field = BackupConfirmScreen.class.getDeclaredField("onProceed"); field.setAccessible(true);
                    ((BackupConfirmScreen.Listener)field.get(screen)).proceed(false, false);
                }
                if (opening && client.gui.screen() instanceof DatapackLoadFailureScreen) throw new AssertionError("native save/datapack load failed");
                return;
            }
            if (client.player == null || client.getSingleplayerServer() == null || client.gui.overlay() != null) return;
            if (!prepareQueued) {
                prepareQueued = true; var uuid = client.player.getUUID();
                server.execute(() -> { try {
                    var player = server.getPlayerList().getPlayer(uuid); check(player != null, "actual integrated player");
                    player.setGameMode(GameType.CREATIVE); warmChunks();
                    if (baseline()) writeFixture(player);
                    prepared = true;
                } catch (Throwable error) { failure = error; } }); return;
            }
            if (!prepared || ++ticks < 120) return; // Real entity-section visibility after disk chunk loads.
            if (!verifyQueued) {
                verifyQueued = true; ticks = 0; var uuid = client.player.getUUID();
                server.execute(() -> { try {
                    var player = server.getPlayerList().getPlayer(uuid); verifyFixture(player);
                    player.teleport(new TeleportTransition(server.overworld(), CAMERA, Vec3.ZERO, 180, 14, TeleportTransition.DO_NOTHING));
                    player.getAbilities().flying = true; player.onUpdateAbilities(); verified = true;
                } catch (Throwable error) { failure = error; } }); return;
            }
            if (!verified || !client.level.dimension().equals(Level.OVERWORLD) || client.player.position().distanceToSqr(CAMERA) > 1 || client.gui.screen() != null) return;
            client.player.setYRot(180); client.player.yRotO = 180; client.player.setYHeadRot(180); client.player.setXRot(14); client.player.xRotO = 14;
            if (++ticks < 180) return;
            if (!capturing) {
                check(!client.mouseHandler.isMouseGrabbed(), "mouse remains unlocked");
                long flags = SDLVideo.SDL_GetWindowFlags(client.getWindow().handle());
                check((flags & SDLVideo.SDL_WINDOW_HIDDEN) != 0 && (flags & SDLVideo.SDL_WINDOW_NOT_FOCUSABLE) != 0, "window remains hidden and unfocusable");
                check(GearData.digit(client.player.getInventory().getItem(0)) == 7, "native client receives persisted numeric component");
                capturing = true; var folder = game.resolve("screenshots"); Files.createDirectories(folder);
                String name = "save069-" + mode + "-world.png"; report.addProperty("screenshot", name);
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
                    try (image) { image.writeToFile(folder.resolve(name)); captured = true; } catch (Throwable error) { failure = error; }
                });
            }
            if (!captured) return;
            done = true; var uuid = client.player.getUUID();
            server.execute(() -> { try {
                verifyFixture(server.getPlayerList().getPlayer(uuid));
                check(server.saveEverything(false, true, true), "native chunks/entities/player saved to disk");
                report.addProperty("mode", mode); report.addProperty("version", version); report.addProperty("minecraft", "26.3");
                report.addProperty("hidden", true); report.addProperty("mouseUnlocked", true); report.addProperty("nativeSaveCompleted", true);
                report.addProperty("fixtureProducer", manifest.get("producerVersion").getAsString()); report.addProperty("fixtureDimensions", REALMS.size());
                armedSuccess = true; client.execute(client::stop);
            } catch (Throwable error) { failure = error; client.execute(() -> fail(client, error)); } });
        } catch (Throwable error) { fail(client, error); }
    }

    private void fail(Minecraft client, Throwable error) {
        done = true; armedSuccess = false; failure = error; error.printStackTrace();
        try { Files.writeString(game.resolve("save069-failed.txt"), error.toString(), StandardOpenOption.CREATE_NEW); } catch (Exception ignored) {}
        client.stop();
    }
    private void warmChunks() {
        for (var key : REALMS) { var level = server.getLevel(key); check(level != null, "saved dimension remains registered " + key.identifier());
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) { level.setChunkForced(x, z, true); level.getChunk(x, z); }
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private BlockState with(BlockState state, String key, String value) {
        var property = state.getBlock().getStateDefinition().getProperty(key); check(property != null, "saved property still exists " + key);
        return state.setValue((Property)property, (Comparable)property.getValue(value).orElseThrow());
    }
    private BlockState legacy(String name, int mask) {
        var state = block(name).defaultBlockState();
        for (int i = 0; i < LEGACY.length; i++) state = with(state, LEGACY[i], Boolean.toString((mask & (1 << i)) != 0));
        return state;
    }
    private JsonObject state(BlockState value) {
        var result = new JsonObject(); result.addProperty("id", BuiltInRegistries.BLOCK.getKey(value.getBlock()).toString()); var properties = new JsonObject();
        value.getProperties().forEach(property -> properties.addProperty(property.getName(), serialized(value, property))); result.add("properties", properties); return result;
    }
    private static <T extends Comparable<T>> String serialized(BlockState state, Property<T> property) { return property.getName(state.getValue(property)); }
    private void remember(JsonArray rows, ServerLevel world, BlockPos pos) {
        var row = new JsonObject(); row.addProperty("x", pos.getX()); row.addProperty("y", pos.getY()); row.addProperty("z", pos.getZ()); row.add("state", state(world.getBlockState(pos))); rows.add(row);
    }
    private ItemStack named(String name, int count, String title) {
        var stack = item(name, count); stack.set(DataComponents.CUSTOM_NAME, Component.literal(title));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Save069 retained lore"))));
        stack.set(DataComponents.REPAIR_COST, 7); var data = new CompoundTag(); data.putString("save069_token", "keep-opaque-data");
        var nested = new CompoundTag(); nested.putInt("number", 11242); data.put("unrecognized", nested); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); return stack;
    }
    private ItemStack oldCore(int count, int mask) {
        var stack = named("lho_trace", count, "Saved Absence " + mask); var properties = new LinkedHashMap<String, String>();
        for (int i = 0; i < LEGACY.length; i++) properties.put(LEGACY[i], Boolean.toString((mask & (1 << i)) != 0));
        properties.put("save069_unknown", "keep-this-key"); stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(properties)); return stack.copy();
    }
    /** All persistent components except explicitly derived caches and prior064 flag retirement. */
    private JsonElement stack(ItemStack value) {
        if (value.isEmpty()) return JsonNull.INSTANCE;
        var json = ItemStack.CODEC.encodeStart(server.registryAccess().createSerializationContext(JsonOps.INSTANCE), value).getOrThrow();
        return canonicalItem(json.deepCopy());
    }
    private JsonElement canonicalItem(JsonElement value) {
        if (value.isJsonArray()) { for (int i = 0; i < value.getAsJsonArray().size(); i++) value.getAsJsonArray().set(i, canonicalItem(value.getAsJsonArray().get(i))); }
        else if (value.isJsonObject()) {
            var object = value.getAsJsonObject(); var components = object.getAsJsonObject("components");
            if (components != null) {
                components.remove("minecraft:attribute_modifiers"); components.remove("guogaology:equipment_rules"); components.remove("guogaology:deep_profile");
                if (object.has("id") && object.get("id").getAsString().equals("guogaology:lho_trace")) {
                    var props = components.getAsJsonObject("minecraft:block_state");
                    if (props != null) { for (String key : LEGACY) props.remove(key); if (props.size() == 0) components.remove("minecraft:block_state"); }
                }
            }
            for (var entry : new ArrayList<>(object.entrySet())) object.add(entry.getKey(), canonicalItem(entry.getValue()));
        }
        return value;
    }
    private JsonArray inventory(net.minecraft.world.Container owner, int size) { var rows = new JsonArray(); for (int i = 0; i < size; i++) rows.add(stack(owner.getItem(i))); return rows; }
    private void writeFixture(ServerPlayer player) throws Exception {
        var folder = server.getWorldPath(LevelResource.ROOT); check(!Files.exists(folder.resolve(FIXTURE)), "new old-release fixture manifest");
        var dimensions = new JsonArray(); int dimensionIndex = 0;
        for (var key : REALMS) {
            var world = server.getLevel(key); var rows = new JsonArray();
            for (var p : BlockPos.betweenClosed(-16, 219, -4, 16, 226, 30)) world.setBlock(p, (p.getY() == 219 ? Blocks.SMOOTH_QUARTZ : Blocks.AIR).defaultBlockState(), FLAGS);
            for (int i = 0; i < 2; i++) { var pos = new BlockPos(-8 + i * 4, 220, 0); var type = block(i == 0 ? "server_rack" : "office_monitor");
                check(type.getStateDefinition().getProperty("facing") == null, "actual old device has no facing field"); world.setBlock(pos, type.defaultBlockState(), FLAGS); remember(rows, world, pos);
            }
            int row = 0; for (String name : List.of("chair", "nuke_chair", "snake_head")) { int column = 0;
                for (String facing : List.of("north", "east", "south", "west")) { var pos = new BlockPos(-8 + column++ * 4, 220, 4 + row * 3); world.setBlock(pos, with(block(name).defaultBlockState(), "facing", facing), FLAGS); remember(rows, world, pos); } row++;
            }
            int material = 0; for (String name : List.of("fffz_trace", "fos_trace", "lho_hydra_psi", "lho_hydra_z")) {
                var permanent = new BlockPos(-12 + material * 7, 220, 15); world.setBlock(permanent, legacy(name, 3), FLAGS); remember(rows, world, permanent);
                var anchored = new BlockPos(-12 + material * 7, 220, 21); world.setBlock(anchored.offset(0, -2, 2), block("ordinal_crystal").defaultBlockState(), FLAGS); world.setBlock(anchored, legacy(name, 4), FLAGS); remember(rows, world, anchored); material++;
            }
            if (dimensionIndex == 0) {
                // The finished core retains all eight legacy world palettes.
                // In060 its anchored=true variants need a real nearby anchor;
                // from064 onward these properties are inert compatibility data.
                for (int mask = 4; mask < 8; mask++) {
                    var pos = new BlockPos(-14 + mask * 4, 230, 0);
                    world.setBlock(pos.offset(0, -2, 2), block("ordinal_crystal").defaultBlockState(), FLAGS);
                }
                for (int mask = 0; mask < 8; mask++) {
                    var pos = new BlockPos(-14 + mask * 4, 230, 0); world.setBlock(pos, legacy("lho_trace", mask), FLAGS); remember(rows, world, pos);
                }
            }
            world.setBlock(CHEST, Blocks.CHEST.defaultBlockState(), FLAGS); var chest = (ChestBlockEntity)world.getBlockEntity(CHEST); check(chest != null, "native component chest");
            var gear = named("true_omega_pickaxe", 1, "Saved socket order " + dimensionIndex);
            var raw = named("fffz_trace", 1, "Saved raw FFFZ"); raw.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("stable", "true", "anchored", "true", "player_placed", "true", "save069_raw", "untouched")));
            GearData.setCores(gear, List.of(item("hydra_bud_lv2", 1), ItemStack.EMPTY, oldCore(1, 7), ItemStack.EMPTY, raw)); gear.setDamageValue(23);
            chest.setItem(0, gear); chest.setItem(1, oldCore(17, 0)); chest.setItem(2, oldCore(19, 7)); chest.setItem(3, raw.copy());
            chest.setItem(4, named("laver_boat", 1, "Saved unused Laver boat")); chest.setItem(5, named("dread_boat", 1, "Saved unused Dread boat")); chest.setChanged();
            var boats = new JsonArray(); int boatIndex = 0; for (String wood : List.of("laver", "dread")) {
                var type = BuiltInRegistries.ENTITY_TYPE.getValue(id(wood + "_boat")); var boat = type.create(world, EntitySpawnReason.TRIGGERED); check(boat != null, "old registered boat entity");
                boat.setNoGravity(true); boat.setPos(-8 + boatIndex++ * 16, 220.2, 27); boat.setCustomName(Component.literal("Saved " + wood + " hull " + dimensionIndex));
                check(world.addFreshEntity(boat), "native old boat added for disk persistence"); var saved = new JsonObject(); saved.addProperty("uuid", boat.getUUID().toString()); saved.addProperty("id", id(wood + "_boat").toString()); saved.addProperty("name", boat.getCustomName().getString()); boats.add(saved);
            }
            var entry = new JsonObject(); entry.addProperty("dimension", key.identifier().toString()); entry.add("blocks", rows); entry.add("stored", inventory(chest, 6)); entry.add("boats", boats); dimensions.add(entry); dimensionIndex++;
        }
        player.getInventory().clearContent(); var pick = named("number_pickaxe", 1, "Saved numeric pickaxe"); pick.set(MiningContent.DIGIT, 7d); GearData.refresh(pick); pick.setDamageValue(11);
        var sword = named("number_sword", 1, "Saved numeric sword"); sword.set(MiningContent.DIGIT, 4d); GearData.refresh(sword); sword.setDamageValue(9);
        player.getInventory().setItem(0, pick); player.getInventory().setItem(1, sword); player.getInventory().setItem(2, oldCore(5, 7)); player.getInventory().setChanged();
        manifest = new JsonObject(); manifest.addProperty("schema", 1); manifest.addProperty("fixtureOnly", true); manifest.addProperty("producerVersion", version); manifest.add("dimensions", dimensions); manifest.add("player", inventory(player.getInventory(), 3));
        manifest.add("rawLhoControls", rawControls(player)); Files.writeString(folder.resolve(FIXTURE), JSON.toJson(manifest), StandardOpenOption.CREATE_NEW);
        check(server.saveEverything(false, true, true), "exact old release flushes fixture to actual disk");
    }
    private void verifyFixture(ServerPlayer player) throws Exception {
        manifest = JsonParser.parseString(Files.readString(server.getWorldPath(LevelResource.ROOT).resolve(FIXTURE))).getAsJsonObject();
        check(manifest.get("schema").getAsInt() == 1 && manifest.get("fixtureOnly").getAsBoolean(), "known fixture identity");
        check(Set.of("0.6.0", "0.6.8").contains(manifest.get("producerVersion").getAsString()), "fixture is genuinely old production");
        check(manifest.getAsJsonArray("dimensions").size() == 4, "all custom dimensions and Overworld persisted"); int blockCount = 0, boatCount = 0;
        for (var value : manifest.getAsJsonArray("dimensions")) {
            var entry = value.getAsJsonObject(); var key = realm(entry.get("dimension").getAsString()); var level = server.getLevel(key); check(level != null, "old dimension survives");
            for (var v : entry.getAsJsonArray("blocks")) {
                var row = v.getAsJsonObject(); var pos = new BlockPos(row.get("x").getAsInt(), row.get("y").getAsInt(), row.get("z").getAsInt()); level.getChunk(pos);
                var expected = row.getAsJsonObject("state"); var actual = state(level.getBlockState(pos));
                String name = expected.get("id").getAsString(); if (!baseline() && Set.of("guogaology:server_rack", "guogaology:office_monitor").contains(name)) {
                    check(!expected.getAsJsonObject("properties").has("facing"), "legacy disk omitted new facing property");
                    check("north".equals(actual.getAsJsonObject("properties").get("facing").getAsString()), "old missing-facing device defaults NORTH on native decode"); actual.getAsJsonObject("properties").remove("facing");
                }
                check(actual.equals(expected), "old native palette properties retained " + key.identifier() + " " + pos); blockCount++;
            }
            level.getChunk(CHEST); check(level.getBlockEntity(CHEST) instanceof ChestBlockEntity, "real saved component chest reloaded");
            var chest = (ChestBlockEntity)level.getBlockEntity(CHEST); check(inventory(chest, 6).equals(entry.get("stored")), "complete persistent chest components/counts/empty socket order retained " + key.identifier());
            for (var v : entry.getAsJsonArray("boats")) {
                var row = v.getAsJsonObject(); var entity = level.getEntity(UUID.fromString(row.get("uuid").getAsString())); check(entity != null, "boat entity loaded from saved entity section");
                check(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(row.get("id").getAsString()), "boat wood/entity registry ID retained");
                check(entity.getCustomName() != null && entity.getCustomName().getString().equals(row.get("name").getAsString()), "saved boat custom name retained"); boatCount++;
            }
        }
        check(inventory(player.getInventory(), 3).equals(manifest.get("player")), "saved player digits/damage/lore/name/custom data retained");
        check(rawControls(player).equals(manifest.get("rawLhoControls")), "native raw FFFZ/FOS/psi/Z stability/provenance rules equal exact old release");
        report.addProperty("persistedBlockStates", blockCount); report.addProperty("persistedBoats", boatCount); report.addProperty("persistedChestStacks", 24); report.addProperty("persistedPlayerStacks", 3);
        report.addProperty("rawLhoNativeControls", 16); report.addProperty("diskUpgradeVerified", !baseline());
    }
    private JsonArray rawControls(ServerPlayer player) {
        var world = server.overworld(); world.setChunkForced(CONTROL.getX() >> 4, CONTROL.getZ() >> 4, true); world.getChunk(CONTROL); var result = new JsonArray();
        var oldMain = player.getMainHandItem(); var oldOff = player.getOffhandItem(); var oldPos = player.position();
        try {
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY); player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY); player.setPos(90, 230, 90);
            for (var p : BlockPos.betweenClosed(CONTROL.offset(-4, -4, -4), CONTROL.offset(4, 4, 4))) world.setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
            for (String name : List.of("fffz_trace", "fos_trace", "lho_hydra_psi", "lho_hydra_z")) for (int test = 0; test < 4; test++) {
                var anchor = test == 1 ? CONTROL.offset(-2, -2, -2) : CONTROL.offset(-3, 0, 0); world.setBlock(anchor, Blocks.AIR.defaultBlockState(), FLAGS);
                var initial = test == 3 ? with(block(name).defaultBlockState(), "player_placed", "true") : block(name).defaultBlockState(); world.setBlock(CONTROL, initial, FLAGS);
                if (test == 1 || test == 2) world.setBlock(anchor, block("ordinal_crystal").defaultBlockState(), FLAGS);
                var drops = Block.getDrops(world.getBlockState(CONTROL), world, CONTROL, null, player, ItemStack.EMPTY); int count = drops.stream().mapToInt(ItemStack::getCount).sum();
                check(count == (test == 1 || test == 3 ? 1 : 0), "raw native two-ring/provenance drop rule " + name + "/" + test);
                var row = new JsonObject(); row.addProperty("id", name); row.addProperty("case", test); row.addProperty("count", count); result.add(row);
                world.setBlock(CONTROL, Blocks.AIR.defaultBlockState(), FLAGS); world.setBlock(anchor, Blocks.AIR.defaultBlockState(), FLAGS);
            }
        } finally { player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, oldMain); player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, oldOff); player.setPos(oldPos); }
        return result;
    }
}
