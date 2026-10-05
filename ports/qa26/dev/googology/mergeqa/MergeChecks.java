package dev.googology.mergeqa;
import dev.googology.*;
import dev.googology.mining.*;
import dev.googology.portal.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Development-only checks; never included in the distribution. */
public final class MergeChecks implements ClientModInitializer {
    private boolean opening,queued,done;private volatile boolean ready;private volatile Throwable failure;
    private int ticks,scene;private final long deadline=System.nanoTime()+900_000_000_000L;
    private static final String[] BIOMES={"bms_plain","epsilon_forest","power_desert","laver_forest","astra","lho_void","underworld"};
    private static final List<BlockPos> SITES=new ArrayList<>();
    private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
    public void onInitializeClient(){if(Boolean.getBoolean("googology.qa.voxy")){VoxyChecks.initialize();return;}ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void tick(Minecraft c){
      if(done)return;
      try{
        if(System.nanoTime()>deadline)throw new AssertionError("QA timed out scene "+scene);
        if(failure!=null)throw new RuntimeException(failure);
        c.options.pauseOnLostFocus=false;if(!c.gui.hud.isHidden())c.gui.hud.toggle();c.options.renderDistance().set(5);c.options.framerateLimit().set(60);
        if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
        if(c.player==null||c.getSingleplayerServer()==null)return;
        if(!queued){queued=true;ready=false;var id=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{setup(c.getSingleplayerServer().getPlayerList().getPlayer(id),scene);ready=true;}catch(Throwable e){failure=e;}});return;}
        if(!ready||++ticks<140)return;
        var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int frame=scene;
        Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),i->{try(i){i.writeToFile(folder.resolve("merge-"+frame+".png"));}catch(Exception e){failure=e;}});
        if(++scene>SITES.size()+5){done=true;Files.writeString(Path.of("port-client-ok.txt"),"MERGE_RUNTIME_OK: four tiers, shared registries, three worlds, outer generation and feature placement, seven creatures, passive core data and portal routes; 0.3.5 one namespace, mixed mineral gate, old empty-set art, epsilon-zero texture and four manuscripts.\n");c.stop();return;}
        ticks=0;queued=false;
      }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private static void setup(ServerPlayer p,int scene){
      var s=p.level().getServer();var outer=s.getLevel(GoogologyMod.OUTER);require(outer!=null,"outer dimension missing");
      p.setGameMode(GameType.CREATIVE);
      if(scene==0){
        MergeMechanics.run(p);
        MaterialChecks.run(p);
        require(s.getLevel(GoogologyMod.DIMENSION)!=null&&s.getLevel(GoogologyMod.GUOGAO)!=null,"inner and underworld present");
        require(EquipmentRules.MINERALS.length==4&&MiningContent.MATERIALS.length==4,"four mineral tiers");
        require(EquipmentRules.multiplier(16,false)==4&&EquipmentRules.multiplier(16,true)==256,"bounded realm scaling");
        require(PortalTravel.destination(Level.OVERWORLD,PortalKind.GGG).equals(GoogologyMod.OUTER),"apple outward");
        require(PortalTravel.destination(GoogologyMod.OUTER,PortalKind.INNER).equals(GoogologyMod.DIMENSION),"omega inward");
        require(PortalTravel.destination(GoogologyMod.DIMENSION,PortalKind.INNER).equals(GoogologyMod.OUTER),"omega return");
        require(PortalTravel.destination(GoogologyMod.GUOGAO,PortalKind.GUOGAO).equals(GoogologyMod.DIMENSION),"underworld return");
        for(var item:MiningContent.GEAR.keySet()){
          var stack=new ItemStack(item);GearData.refresh(stack);var spec=MiningContent.GEAR.get(item);
          require(GearData.capacity(stack)>0,"capacity "+item);
          if(spec.kind()==6){var core=new ItemStack(GoogologyBlocks.ORDINAL_CRYSTAL);require(GearData.install(stack,core,3)==null,"book installs");require(!GearData.remove(stack,0).isEmpty(),"book removes");}
        }
        var origin=new BlockPos(0,220,0);var level=s.overworld();
        for(var q:BlockPos.betweenClosed(origin.offset(-12,-1,-10),origin.offset(30,6,12)))level.setBlock(q,q.getY()==219?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        int n=0;for(String id:List.of("snake","deepseek_whale","busy_beaver","fly_y","fruit_cake_slime","evil_pig","fruit_slime")){
          var entity=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("googology",id)).create(level,EntitySpawnReason.COMMAND);
          require(entity!=null,"spawn "+id);entity.snapTo(n++*4,220,0,0,0);if(entity instanceof Mob mob)mob.setNoAi(true);level.addFreshEntity(entity);
        }
        // Sample donor biome source without loading unrelated chunks.
        var source=outer.getChunkSource().getGenerator().getBiomeSource();var climate=outer.getChunkSource().randomState().sampler();var seen=new HashSet<String>();
        for(int radius=0;radius<32&&seen.size()<7;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
          if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;
          for(int y:new int[]{100,-40}){var b=source.getNoiseBiome(x*32,y>>2,z*32,climate);String id=b.unwrapKey().orElseThrow().identifier().getPath();if(seen.add(id)){SITES.add(new BlockPos(x*128,y,z*128));System.out.println("MERGE_SITE "+id+" "+SITES.getLast());}}
        }
        require(SITES.size()>=5,"Donor biome variety: "+seen);
        p.teleport(new TeleportTransition(level,new Vec3(12,225,25),Vec3.ZERO,180,10,TeleportTransition.DO_NOTHING));
      }else if(scene>SITES.size()+1){MaterialChecks.display(p,scene-SITES.size()-2);
      }else if(scene==SITES.size()+1){
        var level=s.overworld();var generator=level.getChunkSource().getGenerator();
        var registry=level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        var names=List.of("bms_matrix","laver_table","nuke_mushroom");
        for(int i=0;i<names.size();i++){
          var center=new BlockPos(i*45,225,100);
          for(var q:BlockPos.betweenClosed(center.offset(-30,-1,-30),center.offset(30,-1,30)))level.setBlock(q,Blocks.GRASS_BLOCK.defaultBlockState(),2);
          var feature=registry.getValue(dev.googology.outer.GoogologyMod.id(names.get(i)));
          require(feature!=null&&feature.place(level,generator,net.minecraft.util.RandomSource.create(173+i),center),"actual configured feature "+names.get(i));
          Block expected=i==0?GoogologyBlocks.ORDINAL_BRICKS:i==1?GoogologyBlocks.LAVER_PLANKS:dev.googology.outer.registry.ModBlocks.NUKE_MUSHROOM_CAP;
          int placed=0;for(var q:BlockPos.betweenClosed(center.offset(-30,0,-30),center.offset(30,40,30)))if(level.getBlockState(q).is(expected))placed++;
          require(placed>0,"feature actually contains its expected blocks: "+names.get(i));
          System.out.println("MERGE_FEATURE_BLOCKS "+names.get(i)+" "+placed);
        }
        System.out.println("MERGE_FEATURES_OK BMS / Laver / mushroom cloud placed");
        p.teleport(new TeleportTransition(level,new Vec3(40,250,175),Vec3.ZERO,180,18,TeleportTransition.DO_NOTHING));
      }else{
        if(scene==1)MergeMechanics.testTotem(p);
        var site=SITES.get(scene-1);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)outer.getChunk((site.getX()>>4)+x,(site.getZ()>>4)+z);
        // A climate sample can fall in a void column at a biome edge. Use the
        // nearest visible land in the loaded neighborhood for the photograph.
        if(site.getY()>=0){
          int best=-64;BlockPos land=site;
          for(int dx=-16;dx<=16;dx+=4)for(int dz=-16;dz<=16;dz+=4){int h=outer.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,site.getX()+dx,site.getZ()+dz);if(h>best){best=h;land=site.offset(dx,0,dz);}}
          site=land;
        }
        int y=site.getY()<0?site.getY():Math.min(300,outer.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,site.getX(),site.getZ())+16);
        p.teleport(new TeleportTransition(outer,new Vec3(site.getX()+.5,y,site.getZ()+.5),Vec3.ZERO,20,55,TeleportTransition.DO_NOTHING));
      }
      p.getAbilities().flying=true;p.onUpdateAbilities();
    }
}
