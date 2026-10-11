package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.block.ConwayLinkBlock;
import dev.guogaology.outer.world.LhoChunkGenerator;
import dev.guogaology.outer.world.SurfaceShaftField;
import dev.guogaology.outer.world.feature.LaverTableFeature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Development-only native loading, loot, generation and photographs; never shipped. */
public final class JarImport0541Checks {
    private boolean opening, queued, done;
    private volatile boolean ready, captured;
    private volatile Throwable failure;
    private int ticks, frame, checks;
    private final StringBuilder report = new StringBuilder();
    private final long deadline = System.nanoTime() + 600_000_000_000L;
    private BlockPos shaft;
    public static void initialize() {
        var test = new JarImport0541Checks();
        ClientTickEvents.END_CLIENT_TICK.register(test::tick);
    }
    private void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    private void note(String value) { report.append(value).append('\n'); System.out.println("IMPORT0541 " + value); }
    private static Identifier id(String path) { return GuogaologyMod.id(path); }
    private static ItemStack item(String name) { return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(name.contains(":") ? name : "guogaology:" + name))); }

    private void tick(Minecraft client) {
        if (done) return;
        try {
            if (failure != null) throw new RuntimeException(failure);
            if (System.nanoTime() > deadline) throw new AssertionError("import QA timed out");
            client.options.pauseOnLostFocus=false;client.options.renderDistance().set(4);client.options.framerateLimit().set(60);
            if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
            if (client.level == null) {
                if (!opening && client.isGameLoadFinished() && client.gui.overlay() == null) {
                    opening=true;client.createWorldOpenFlows().openWorld("port-qa",client::stop);
                }
                return;
            }
            if (client.player == null || client.getSingleplayerServer() == null) return;
            if (!queued) {
                queued=true;var uuid=client.player.getUUID();
                client.getSingleplayerServer().execute(()->{try {
                    var player=client.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                    if (frame==0) run(player);
                    display(player,frame);ready=true;
                } catch(Throwable e){failure=e;}});
                return;
            }
            if (!ready || client.gui.overlay()!=null) return;
            if (++ticks==120) {
                var folder=client.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);
                int picture=frame;
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image->{try(image){
                    image.writeToFile(folder.resolve("import0541-"+picture+".png"));captured=true;
                }catch(Exception e){failure=e;}});
            }
            if (!captured) return;
            if (++frame==2) {
                note("checks="+checks+" native26.2; jar, leaf loot, recipes, facing, table sampling, fresh shafts");
                Files.writeString(Path.of("import0541-report.txt"),report.toString());
                Files.writeString(Path.of("port-client-ok.txt"),"IMPORT0541_RUNTIME_OK\n"+report);
                done=true;client.stop();
            } else {ticks=0;queued=false;ready=false;captured=false;}
        } catch(Throwable e) {
            done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}client.stop();
        }
    }

    private void run(ServerPlayer player) throws Exception {
        var world=player.level().getServer().overworld();
        check(GuogaologyBlocks.TRANSLUCENT.contains(GuogaologyBlocks.GUMMY_JAR),"jar translucent registration");
        check(GuogaologyBlocks.GUMMY_JAR.defaultBlockState().getLightEmission()==3,"jar luminance");
        check(BuiltInRegistries.BLOCK.getValue(id("graham_vine")).defaultBlockState().is(BlockTags.CLIMBABLE),"Graham Vine climbable");
        check(GuogaologyBlocks.CONWAY_LINK.defaultBlockState().getValue(ConwayLinkBlock.FACING)==Direction.EAST,"old links default east");
        for(int i=0;i<4;i++) check(SurfaceShaftField.conwayStep(i).getValue(ConwayLinkBlock.FACING)==new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}[i],"step orientation "+i);
        var pos=new BlockPos(0,220,0);int min=4,max=0;
        for(int i=0;i<512;i++) {
            var drops=Block.getDrops(GuogaologyBlocks.GUMMY_JAR.defaultBlockState(),world,pos,null,player,ItemStack.EMPTY);
            int count=0;
            for(var drop:drops){check(drop.is(item("gummy").getItem()),"jar ordinary gummy only");count+=drop.getCount();}
            check(count>=1&&count<=3,"jar yield");min=Math.min(min,count);max=Math.max(max,count);
        }
        check(min==1&&max==3,"jar both yield endpoints");
        var enchantments=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for(int fortune=0;fortune<=4;fortune++) {
            var tool=new ItemStack(Items.DIAMOND_PICKAXE);
            if(fortune>0)tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE),fortune);
            int ribbons=0;
            for(int i=0;i<4096;i++) for(var drop:Block.getDrops(GuogaologyBlocks.GIANT_LAVER.defaultBlockState(),world,pos,null,player,tool)) {
                check(drop.is(item("laver_ribbon").getItem())&&drop.getCount()==1,"leaf non-silk loot");ribbons++;
            }
            double expected=new double[]{.05,.0625,.083333336,.1,.1}[fortune];
            check(Math.abs(ribbons/4096.0-expected)<.025,"Fortune leaf rate "+fortune+": "+ribbons);
            note("leaf fortune="+fortune+" ribbons="+ribbons+"/4096");
        }
        var silk=new ItemStack(Items.DIAMOND_PICKAXE);silk.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH),1);
        for(int i=0;i<16;i++) {
            var drops=Block.getDrops(GuogaologyBlocks.GIANT_LAVER.defaultBlockState(),world,pos,null,player,silk);
            check(drops.size()==1&&drops.getFirst().is(GuogaologyBlocks.GIANT_LAVER.asItem()),"Silk leaf intact");
        }
        String[] products={"amber_guogao","berry_guogao","lime_guogao","azure_guogao"};
        String[] fruits={"apple","sweet_berries","melon_slice","glow_berries"};
        for(int i=0;i<4;i++) for(String binder:new String[]{"guogaology:fruit_cake_gel","minecraft:slime_ball"}) {
            var input=CraftingInput.of(3,1,List.of(item("minecraft:"+fruits[i]),item(binder),item("minecraft:sugar")));
            var recipe=world.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,world);
            boolean matches=recipe.isPresent()&&recipe.get().value().assemble(input).is(item(products[i]).getItem());
            check(matches==binder.startsWith("guogaology:"),"gel recipe "+products[i]+"/"+binder);
        }
        sampleTables(world);
        sampleShafts(player.level().getServer().getLevel(GuogaologyMod.OUTER));
    }

    private void sampleTables(ServerLevel world) {
        var feature=(LaverTableFeature)world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getValue(id("laver_table")).config();
        var template=world.getStructureManager().get(id("laver_table_7")).orElseThrow();
        var origin=new BlockPos(0,230,180);var random=RandomSource.create(541);int slots=0,jars=0;
        for(var rotation:Rotation.values()) {
            var settings=new StructurePlaceSettings().setRotation(rotation);
            var surface=LaverTableFeature.tableSurface(template,origin,settings);
            check(!surface.isEmpty(),"table template surface");
            for(int iteration=0;iteration<100;iteration++) {
                for(var p:surface)world.setBlock(p.above(),Blocks.AIR.defaultBlockState(),2);
                int count=feature.placeGummies(world,random,template,origin,settings,GuogaologyBlocks.AMBER_GUOGAO),actual=0;
                for(var p:surface){var state=world.getBlockState(p.above());check(state.isAir()||state.is(GuogaologyBlocks.GUMMY_JAR),"table cell type");if(!state.isAir())actual++;}
                check(actual==count,"table count matches physical cells");slots+=surface.size();jars+=count;
            }
        }
        check(Math.abs(jars/(double)slots-.01)<.003,"table independent 1% chance");note("table jars="+jars+" eligible="+slots);
    }

    private void sampleShafts(ServerLevel outer) {
        check(outer!=null,"outer dimension loaded");
        var candidates=new ArrayList<BlockPos>();
        var probe=new SurfaceShaftField.ShaftProbe(){
            public boolean hasData(int x,int z){return true;}
            public int top(int x,int z){candidates.add(new BlockPos(x,0,z));return Integer.MIN_VALUE;}
            public boolean isVoidOrEdge(int x,int y,int z){return false;}
            public boolean isFluid(int x,int y,int z){return false;}
        };
        for(int x=-64;x<=64;x++)for(int z=-64;z<=64;z++)SurfaceShaftField.apply(outer.getSeed(),new ChunkPos(x,z),-64,319,probe,(a,b,c,d)->{},i->i,i->i+10);
        candidates.sort(Comparator.comparingInt(p->Math.abs(p.getX())+Math.abs(p.getZ())));
        int checked=0;
        for(var center:candidates) {
            var biome=outer.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(center.getX()>>2,20,center.getZ()>>2,outer.getChunkSource().randomState().sampler());
            if(biome.is(LhoChunkGenerator.LHO_VOID_BIOME)||biome.is(LhoChunkGenerator.LHO_EDGE_BIOME))continue;
            var chunk=outer.getChunk(center.getX()>>4,center.getZ()>>4);
            int top=-65;
            for(int y=319;y>-64;y--)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                if(chunk.getBlockState(center.offset(dx,y,dz)).is(GuogaologyBlocks.CONWAY_LINK))top=Math.max(top,y);
            if(top<0)continue;
            for(int y=-64;y<Math.min(20,top);y++)check(chunk.getBlockState(center.atY(y)).isAir(),"shaft center open to void "+center+" y="+y);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)check(chunk.getBlockState(center.offset(dx,-64,dz)).isAir(),"no restored bedrock floor");
            int steps=0;for(int y=-63;y<Math.min(20,top);y++) {
                int row=0;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(chunk.getBlockState(center.offset(dx,y,dz)).is(GuogaologyBlocks.CONWAY_LINK))row++;
                check(row==1,"one spiral step per Y");steps+=row;
            }
            note("fresh shaft="+center.getX()+","+top+","+center.getZ()+" checkedLowerSteps="+steps);
            if(shaft==null)shaft=center.atY(top);
            if(++checked==2)break;
        }
        check(checked==2,"two fresh naturally generated shafts");
    }

    private void display(ServerPlayer player,int frame) {
        player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
        if(frame==0) {
            var world=player.level().getServer().getLevel(GuogaologyMod.OUTER);
            player.teleport(new TeleportTransition(world,new Vec3(shaft.getX()+.5,shaft.getY()+8,shaft.getZ()+3.5),Vec3.ZERO,180,70,TeleportTransition.DO_NOTHING));
        } else {
            var world=player.level().getServer().overworld();
            for(var p:BlockPos.betweenClosed(new BlockPos(-8,219,-5),new BlockPos(8,224,5)))world.setBlock(p,p.getY()==219?Blocks.QUARTZ_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            for(int i=0;i<4;i++) {
                world.setBlock(new BlockPos(i*2-3,220,0),SurfaceShaftField.conwayStep(i),2);
                world.setBlock(new BlockPos(i*2-3,221,0),GuogaologyBlocks.GUMMY_JAR.defaultBlockState(),2);
            }
            player.teleport(new TeleportTransition(world,new Vec3(4.5,224,8.5),Vec3.ZERO,155,16,TeleportTransition.DO_NOTHING));
        }
    }
}
