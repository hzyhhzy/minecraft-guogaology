package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.world.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Native 0.5.14 crafting, real plant growth and solid luminous Guogao art, in a disposable world. */
public final class Core514Checks {
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
    private static final BlockPos ROOT=new BlockPos(8,286,8),CUBE=new BlockPos(8,220,8);
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError("Core514: "+why);}
    private static void set(ServerLevel world,BlockPos at,BlockState state){world.setBlock(at,state,FLAGS);}
    private static Map<BlockPos,BlockState> painter(ServerLevel world,BlockPos at){
        var map=new LinkedHashMap<BlockPos,BlockState>();long salt=WorldNoise.mix(world.getSeed()^at.asLong());
        CombinatorialForms.grahamTree(new VoxelBrush(Integer.MIN_VALUE,Integer.MAX_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE,
                (x,y,z,m)->map.put(new BlockPos(x,y,z),NaturalScenery.formMaterial(m))),at.getX(),at.getY()+1,at.getZ(),CombinatorialForms.grahamHeight(salt),salt);
        return map;
    }
    private static void prepare(ServerLevel world,BlockPos at,BlockState soil){
        for(int cx=(at.getX()-7)>>4;cx<=(at.getX()+7)>>4;cx++)for(int cz=(at.getZ()-7)>>4;cz<=(at.getZ()+7)>>4;cz++)world.getChunk(cx,cz);
        var from=at.offset(-7,-1,-7);var to=at.offset(7,16,7);
        for(var q:BlockPos.betweenClosed(from,to))check(world.getBlockEntity(q)==null,"fixture must not replace a preexisting block entity "+q);
        for(var q:BlockPos.betweenClosed(from,to))set(world,q,Blocks.AIR.defaultBlockState());
        set(world,at.below(),soil);set(world,at,GrahamGrowth.BLOCK.defaultBlockState());
    }
    private static Map<BlockPos,BlockState> snapshot(ServerLevel world,BlockPos at){
        var map=new LinkedHashMap<BlockPos,BlockState>();
        for(var q:BlockPos.betweenClosed(at.offset(-7,-1,-7),at.offset(7,16,7)))map.put(q.immutable(),world.getBlockState(q));
        return map;
    }
    private static RandomSource triggeringRandom(){
        for(long seed=0;seed<1024;seed++)if(RandomSource.create(seed).nextInt(7)==0)return RandomSource.create(seed);
        throw new AssertionError("No triggering native random seed");
    }
    private static void exactTree(ServerLevel world,BlockPos at,BlockState soil){
        var expected=painter(world,at);
        check(expected.values().stream().anyMatch(s->s.is(GuogaologyBlocks.POWER_BRICKS)),"Graham trunk and branches use existing Power Wood");
        check(expected.values().stream().allMatch(s->s.is(GuogaologyBlocks.POWER_BRICKS)||s.equals(GuogaologyBlocks.ordinalBrick(3))),"complete Graham painter adds only Power Wood and original number3 tips");
        check(expected.values().stream().filter(s->s.equals(GuogaologyBlocks.ordinalBrick(3))).count()>=4,"many existing number3 tips");
        check(world.getBlockState(at.below()).equals(soil),"growth preserves its exact support state");
        for(var q:BlockPos.betweenClosed(at.offset(-7,0,-7),at.offset(7,16,7)))
            check(world.getBlockState(q).equals(expected.getOrDefault(q,Blocks.AIR.defaultBlockState())),"exact tree painter and all branch gaps "+q);
    }
    private static void rejection(ServerLevel world,BlockPos at,String why){
        var before=snapshot(world,at);var bone=new ItemStack(Items.BONE_MEAL,2);
        check(!GrahamGrowth.canGrow(world,at),why+" fails eligibility");
        check(!BoneMealItem.growCrop(bone,world,at)&&bone.getCount()==2,why+" rejects real bonemeal without consumption");
        world.getBlockState(at).randomTick(world,at,triggeringRandom());
        check(!GrahamGrowth.tryGrow(world,at)&&snapshot(world,at).equals(before),why+" leaves the complete fixture unchanged");
    }
    private static void deep(ServerLevel world,BlockState soil){
        check(world!=null&&GrahamGrowth.isDeep(world),"deep dimension exists");
        prepare(world,ROOT,soil);var planted=world.getBlockState(ROOT);
        check(planted.canSurvive(world,ROOT)&&planted.isRandomlyTicking(),"native planted state survives local soil and receives random ticks");
        check(GrahamGrowth.canGrow(world,ROOT),"open supported plant can grow");
        planted.randomTick(world,ROOT,triggeringRandom());exactTree(world,ROOT,soil);
        prepare(world,ROOT,soil);var bone=new ItemStack(Items.BONE_MEAL,2);
        check(BoneMealItem.growCrop(bone,world,ROOT)&&bone.getCount()==1,"real native bonemeal grows and consumes exactly one");exactTree(world,ROOT,soil);
        for(var support:List.of(GuogaologyBlocks.EPSILON_TURF,GuogaologyBlocks.GUOGAO_LOAM,GuogaologyBlocks.POWER_SAND,
                GuogaologyBlocks.ROOTBOUND_STONE,GuogaologyBlocks.NUMBER_STONES[0],GuogaologyBlocks.NUMBER_STONES[9],Blocks.DIRT,Blocks.FARMLAND)){
            prepare(world,ROOT,support.defaultBlockState());check(world.getBlockState(ROOT).canSurvive(world,ROOT)&&GrahamGrowth.canGrow(world,ROOT),"all intended local planting supports "+support);
        }
        prepare(world,ROOT,soil);var occupied=painter(world,ROOT).keySet().stream().filter(q->q.getY()>ROOT.getY()+1).findFirst().orElseThrow();
        set(world,occupied,Blocks.STONE.defaultBlockState());rejection(world,ROOT,"solid at a planned voxel");
        prepare(world,ROOT,soil);var expected=painter(world,ROOT);int x0=ROOT.getX(),x1=x0,y1=ROOT.getY(),z0=ROOT.getZ(),z1=z0;
        for(var q:expected.keySet()){x0=Math.min(x0,q.getX());x1=Math.max(x1,q.getX());y1=Math.max(y1,q.getY());z0=Math.min(z0,q.getZ());z1=Math.max(z1,q.getZ());}
        BlockPos hole=null;for(var q:BlockPos.betweenClosed(new BlockPos(x0,ROOT.getY(),z0),new BlockPos(x1,y1,z1)))if(!expected.containsKey(q)&&q.getY()>ROOT.getY()){hole=q.immutable();break;}
        check(hole!=null,"production tree contains branch gaps inside its full volume");
        set(world,hole,Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)world.getBlockEntity(hole);
        check(chest!=null,"real container in a branch gap");chest.setItem(0,new ItemStack(Items.DIAMOND,7));
        rejection(world,ROOT,"container in a branch gap");check(world.getBlockEntity(hole)==chest&&chest.getItem(0).getCount()==7,"failed growth preserves container identity and inventory");set(world,hole,Blocks.AIR.defaultBlockState());
        prepare(world,ROOT,soil);set(world,occupied,Blocks.WATER.defaultBlockState());rejection(world,ROOT,"water inside planned volume");
        prepare(world,ROOT,soil);set(world,ROOT.below(),Blocks.AIR.defaultBlockState());rejection(world,ROOT,"missing support");
        prepare(world,ROOT,soil);set(world,ROOT.below(),Blocks.OAK_PLANKS.defaultBlockState());rejection(world,ROOT,"building floor is not growing soil");
        prepare(world,ROOT,soil);var border=world.getWorldBorder();double oldX=border.getCenterX(),oldZ=border.getCenterZ(),oldSize=border.getSize();
        try{border.setCenter(ROOT.getX()+.5,ROOT.getZ()+.5);border.setSize(2);rejection(world,ROOT,"whole tree crosses world border");}
        finally{border.setCenter(oldX,oldZ);border.setSize(oldSize);}
        var high=new BlockPos(8,world.getMaxY()-1,8);var highBelow=world.getBlockState(high.below());var highState=world.getBlockState(high);
        check(world.getBlockEntity(high)==null&&world.getBlockEntity(high.below())==null,"height fixture avoids containers");
        try{set(world,high.below(),soil);set(world,high,GrahamGrowth.BLOCK.defaultBlockState());check(!GrahamGrowth.canGrow(world,high)&&!GrahamGrowth.tryGrow(world,high)&&world.getBlockState(high).is(GrahamGrowth.BLOCK),"tree beyond build height fails before writes");}
        finally{set(world,high,highState);set(world,high.below(),highBelow);}
        unloaded(world,soil);
        System.out.println("CORE514_GRAHAM_DEEP_OK dimension="+world.dimension().identifier()+" nativeRandomTick/nativeBonemeal/exactPainter/soil/atomicObstructions/height/border/unloaded");
    }
    private static void unloaded(ServerLevel world,BlockState soil){
        var at=new BlockPos(5135,286,5128);var chunk=world.getChunkAt(at);var expected=painter(world,at);
        check(expected.keySet().stream().anyMatch(q->(q.getX()>>4)==321),"actual tree extends into the unloaded east chunk");
        for(var q:BlockPos.betweenClosed(new BlockPos(5121,285,5121),new BlockPos(5135,302,5135)))
            check(chunk.getBlockEntity(q)==null,"unloaded edge fixture avoids containers");
        for(var q:BlockPos.betweenClosed(new BlockPos(5121,285,5121),new BlockPos(5135,302,5135)))chunk.setBlockState(q,Blocks.AIR.defaultBlockState(),0);
        chunk.setBlockState(at.below(),soil,0);chunk.setBlockState(at,GrahamGrowth.BLOCK.defaultBlockState(),0);
        check(world.getChunkSource().getChunkNow(321,320)==null,"neighbor genuinely has no completed FULL chunk before growth");
        int count=world.getChunkSource().getLoadedChunksCount();var before=new LinkedHashMap<BlockPos,BlockState>();
        for(var q:expected.keySet())if((q.getX()>>4)==320&&(q.getZ()>>4)==320)before.put(q,chunk.getBlockState(q));
        var bone=new ItemStack(Items.BONE_MEAL,2);check(!BoneMealItem.growCrop(bone,world,at)&&bone.getCount()==2,"native bonemeal refuses an unloaded tree arm");
        chunk.getBlockState(at).randomTick(world,at,triggeringRandom());check(!GrahamGrowth.tryGrow(world,at),"unloaded arm rejects direct and tick growth");
        for(var e:before.entrySet())check(chunk.getBlockState(e.getKey()).equals(e.getValue()),"no partial tree appears in the loaded half");
        check(world.getChunkSource().getChunkNow(321,320)==null&&world.getChunkSource().getLoadedChunksCount()==count,"all failed growth paths request no completed chunk");
    }
    private static void outside(ServerLevel world){
        check(!GrahamGrowth.isDeep(world),"outside dimension guard");prepare(world,ROOT,Blocks.DIRT.defaultBlockState());
        // A broad dirt patch retains the modern native BushBlock's existing bonemeal spread.
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)set(world,ROOT.offset(x,-1,z),Blocks.DIRT.defaultBlockState());
        var before=snapshot(world,ROOT);for(long seed=0;seed<32;seed++)world.getBlockState(ROOT).randomTick(world,ROOT,RandomSource.create(seed));
        check(snapshot(world,ROOT).equals(before)&&!GrahamGrowth.tryGrow(world,ROOT),"outside random ticks and deep helper leave plant unchanged");
        var bone=new ItemStack(Items.BONE_MEAL,2);check(BoneMealItem.growCrop(bone,world,ROOT)&&bone.getCount()==1,"outside retains original native bush bonemeal behavior");
        int plants=0;for(var q:BlockPos.betweenClosed(ROOT.offset(-7,0,-7),ROOT.offset(7,16,7))){var state=world.getBlockState(q);check(state.isAir()||state.is(GrahamGrowth.BLOCK),"outside bonemeal never paints a deep tree");if(state.is(GrahamGrowth.BLOCK))plants++;}
        check(plants>=2&&world.getBlockState(ROOT).is(GrahamGrowth.BLOCK),"outside keeps original plant and its native bush spread");
        System.out.println("CORE514_GRAHAM_OUTSIDE_OK dimension="+world.dimension().identifier()+" unchangedRandomTick/nativeBushSpread");
    }
    private static void cube(ServerPlayer player){
        var world=player.level();var state=CoreResources.GUOGAO_DROP.defaultBlockState();
        check(state.getLightEmission()==15,"Guogao cube emits light15");
        for(var shape:List.of(state.getShape(world,CUBE),state.getCollisionShape(world,CUBE))){var boxes=shape.toAabbs();check(boxes.size()==1,"one full solid outline/collision box");var box=boxes.getFirst();check(box.minX==0&&box.minY==0&&box.minZ==0&&box.maxX==1&&box.maxY==1&&box.maxZ==1,"Guogao outline/collision fills the full block");}
        check(state.isCollisionShapeFullBlock(world,CUBE)&&!state.hasBlockEntity(),"solid cube uses no block entity");
        var enchantments=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);var silk=new ItemStack(Items.WOODEN_PICKAXE);silk.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH),1);
        var fortune=new ItemStack(Items.WOODEN_PICKAXE);fortune.enchant(enchantments.getOrThrow(Enchantments.FORTUNE),3);
        for(var tool:List.of(ItemStack.EMPTY,new ItemStack(Items.WOODEN_PICKAXE),new ItemStack(Items.IRON_SWORD),silk,fortune)){
            var drops=Block.getDrops(state,world,CUBE,null,player,tool);check(drops.size()==1&&drops.getFirst().is(CoreResources.GUOGAO_DROP.asItem())&&drops.getFirst().getCount()==1,"native loot returns one intact Guogao cube "+tool);
        }
    }
    public static void run(ServerPlayer player){
        checks=0;CoreResources055Checks.runRecipes(player.level());
        check(dev.guogaology.outer.registry.ModBlocks.GRAHAM==GrahamGrowth.BLOCK,"original imported registry identity resolves to host growth block");
        check(BuiltInRegistries.BLOCK.getValue(GuogaologyMod.id("graham"))==GrahamGrowth.BLOCK,"saved Graham block ID is unchanged");
        check(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id("graham"))==GrahamGrowth.BLOCK.asItem(),"saved Graham item ID is unchanged");
        var server=player.level().getServer();cube(player);
        deep(server.getLevel(GuogaologyMod.DIMENSION),GuogaologyBlocks.EPSILON_TURF.defaultBlockState());
        deep(server.getLevel(GuogaologyMod.GUOGAO),GuogaologyBlocks.GUOGAO_LOAM.defaultBlockState());
        outside(server.getLevel(GuogaologyMod.OUTER));outside(server.overworld());
        System.out.println("CORE514_SERVER_OK checks="+checks+" allNativeRecipes/deepGrahamGrowth/noChunkLoad/noPartialWrites/unchangedOutside/fullLuminousCube/intactDrops");
    }
    public static void display(ServerPlayer player,int scene){
        var server=player.level().getServer();var world=scene==0?server.getLevel(GuogaologyMod.DIMENSION):scene==1||scene==3?server.getLevel(GuogaologyMod.GUOGAO):server.overworld();
        var at=scene<2?ROOT:CUBE;player.closeContainer();player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);player.getInventory().clearContent();player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        for(int cx=(at.getX()-15)>>4;cx<=(at.getX()+15)>>4;cx++)for(int cz=(at.getZ()-15)>>4;cz<=(at.getZ()+15)>>4;cz++)world.getChunk(cx,cz);
        for(var q:BlockPos.betweenClosed(at.offset(-15,-1,-15),at.offset(15,17,15)))check(world.getBlockEntity(q)==null,"photograph stage avoids existing containers");
        var floor=scene==0?GuogaologyBlocks.EPSILON_TURF.defaultBlockState():scene==1?GuogaologyBlocks.GUOGAO_LOAM.defaultBlockState():Blocks.SMOOTH_QUARTZ.defaultBlockState();
        for(var q:BlockPos.betweenClosed(at.offset(-15,-1,-15),at.offset(15,17,15)))set(world,q,q.getY()==at.getY()-1?floor:Blocks.AIR.defaultBlockState());
        Vec3 eye,target;
        if(scene<2){set(world,at,GrahamGrowth.BLOCK.defaultBlockState());check(BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL),world,at),"photographed tree is actually grown with native bonemeal");exactTree(world,at,floor);eye=Vec3.atLowerCornerOf(at).add(10,7.4,14);target=Vec3.atLowerCornerOf(at).add(.5,3.6,.5);}
        else{
            if(scene==3)for(var q:BlockPos.betweenClosed(at.offset(-6,-1,-6),at.offset(6,9,6)))if(Math.abs(q.getX()-at.getX())==6||Math.abs(q.getZ()-at.getZ())==6||q.getY()==at.getY()+9)set(world,q,Blocks.SMOOTH_QUARTZ.defaultBlockState());
            set(world,at,CoreResources.GUOGAO_DROP.defaultBlockState());eye=Vec3.atLowerCornerOf(at).add(3.1,2.3,4.1);target=Vec3.atCenterOf(at);
        }
        var delta=target.subtract(eye);float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
        player.teleport(new TeleportTransition(world,eye,Vec3.ZERO,yaw,pitch,TeleportTransition.DO_NOTHING));player.hasChangedDimension();
        if(!player.connection.hasClientLoaded())player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        player.getAbilities().flying=true;player.onUpdateAbilities();
    }
    public static void settled(ServerPlayer player,int scene){
        if(scene==3){var world=player.level();check(world.getBlockState(CUBE).is(CoreResources.GUOGAO_DROP),"lit scene contains actual Guogao cube");
            check(world.getBrightness(LightLayer.BLOCK,CUBE.east())==14,"native settled light propagation is14one block from light15cube");
            System.out.println("CORE514_NATIVE_LIGHT_OK source=15 neighbor="+world.getBrightness(LightLayer.BLOCK,CUBE.east()));}
    }
}
