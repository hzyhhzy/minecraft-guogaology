package dev.guogaology.portal;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.block.GuogaologyPortalFrameBlock;
import dev.guogaology.mining.GearData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Real release code against disposable terrain, not a parallel placement implementation. */
public final class PortalGroundChecks {
    private static final BlockPos ORIGIN=new BlockPos(3112,230,-3112);
    private final ServerLevel world;
    private final Set<BlockPos> changed=new HashSet<>();
    private final List<BlockPos> gates=new ArrayList<>();
    private int checks;
    private PortalGroundChecks(ServerLevel world){this.world=world;}
    private void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private void put(int x,int y,int z,BlockState state){
        var pos=new BlockPos(x,y,z);changed.add(pos);world.setBlock(pos,state,2);
    }
    private void reset(){
        for(var gate:gates)PortalState.get(world.getServer()).removeGate(new PortalState.Gate(world.dimension().identifier().toString(),gate,PortalKind.INNER));
        gates.clear();for(var pos:changed)world.setBlock(pos,Blocks.AIR.defaultBlockState(),2);changed.clear();
    }
    private int minX(){return ((ORIGIN.getX()>>4)-1)<<4;}
    private int minZ(){return ((ORIGIN.getZ()>>4)-1)<<4;}
    private void plane(int y,BlockState state){
        for(int x=minX();x<minX()+48;x++)for(int z=minZ();z<minZ()+48;z++)put(x,y,z,state);
    }
    private BlockPos search(String label){
        var search=new PortalSiteSearch(new PortalTravel.Target(world,ORIGIN,PortalKind.INNER));int steps=0;
        long started=System.nanoTime();
        while(!search.finished()&&steps++<10000)search.step();
        check(search.finished(),label+": bounded search finishes");var exit=search.exit();
        check(exit!=null,label+": exit exists");gates.add(exit);
        for(var pos:BlockPos.betweenClosed(exit.offset(-4,-4,-4),exit.offset(4,4,4)))changed.add(pos.immutable());
        check(PortalTravel.inSearchWindow(ORIGIN,exit),label+": exit stays in loaded search window");
        check(PortalRitual.complete(world,PortalRitual.gateAt(world,exit)),label+": actual portal is complete");
        check(PortalTravel.isSafe(world,exit.offset(0,0,3)),label+": arrival has a safe floor and headroom");
        System.out.println("PORTAL_GROUND_CASE "+label+" y="+exit.getY()+" steps="+steps+" cpu_ms="+(System.nanoTime()-started)/1_000_000.);
        return exit;
    }
    public static void run(ServerPlayer player){var test=new PortalGroundChecks(player.level().getServer().overworld());test.run();test.frames(player);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+id)));}
    private void frames(ServerPlayer player){
        var center=new BlockPos(ORIGIN.getX(),65,ORIGIN.getZ());var oldMain=player.getMainHandItem();var oldOff=player.getOffhandItem();boolean oldGround=player.onGround();
        try {
            plane(64,Blocks.STONE.defaultBlockState());
            var pick=item("true_omega_pickaxe");GearData.setCores(pick,List.of(item("hydra_bud_lv3")));GearData.setSilkTouch(pick,true);GearData.refresh(pick);
            var book=item("true_omega_manuscript");GearData.setCores(book,List.of(item("sequence_core_lv3")));GearData.refresh(book);
            var silk=new ItemStack(Items.NETHERITE_PICKAXE);silk.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
            for(int style=0;style<3;style++){
                var frame=GuogaologyBlocks.PORTAL_FRAME.defaultBlockState().setValue(GuogaologyPortalFrameBlock.STYLE,style);
                put(center.getX(),center.getY(),center.getZ(),frame);
                check(frame.getDestroySpeed(world,center)==4*Blocks.OBSIDIAN.defaultBlockState().getDestroySpeed(world,center),"frame style "+style+" has four times obsidian hardness");
                for(var tool:List.of(ItemStack.EMPTY,new ItemStack(Items.WOODEN_PICKAXE),new ItemStack(Items.DIAMOND_PICKAXE),silk,pick))
                    check(Block.getDrops(frame,world,center,null,player,tool).isEmpty(),"frame style "+style+" never drops with "+tool);
                player.setItemSlot(EquipmentSlot.MAINHAND,pick);player.setItemSlot(EquipmentSlot.OFFHAND,book);player.setOnGround(true);
                float progress=frame.getDestroyProgress(player,world,center),obsidian=Blocks.OBSIDIAN.defaultBlockState().getDestroyProgress(player,world,center);
                check(progress>0&&progress<1&&Math.abs(progress*4-obsidian)<1e-6,"enhanced pick can mine style "+style+" but does not instabreak; relative time is 4x obsidian");
                System.out.println("PORTAL_FRAME_042_MINING style="+style+" hardness="+frame.getDestroySpeed(world,center)+" seconds="+Math.ceil(1.0/progress)/20.0);
                world.setBlock(center,Blocks.AIR.defaultBlockState(),2);
            }
            for(var frame:List.of(GuogaologyBlocks.OUTER_RETURN_FRAME,GuogaologyBlocks.INNER_RETURN_FRAME,GuogaologyBlocks.GUOGAO_RETURN_FRAME)){
                check(frame.defaultBlockState().getDestroySpeed(world,center)<200,"unactivated return material keeps original hardness");
                check(!Block.getDrops(frame.defaultBlockState(),world,center,null,player,silk).isEmpty(),"unactivated return material keeps its drop");
            }
            PortalRitual.fillPortal(world,center,PortalKind.INNER);gates.add(center);
            for(var pos:BlockPos.betweenClosed(center.offset(-2,0,-2),center.offset(2,0,2)))changed.add(pos.immutable());
            var box=new net.minecraft.world.phys.AABB(center).inflate(5);int items=world.getEntitiesOfClass(ItemEntity.class,box).size();
            world.destroyBlock(center.offset(2,0,0),true,player);
            check(PortalRitual.gateAt(world,center)==null,"breaking one activated frame collapses the portal");
            check(world.getEntitiesOfClass(ItemEntity.class,box).size()==items,"actual frame breaking and whole-gate collapse spawn no items");
            System.out.println("PORTAL_FRAME_042_OK total_checks="+checks+" styles=3 tools=5 silk=true collapse_drops=0");
        } finally {player.setItemSlot(EquipmentSlot.MAINHAND,oldMain);player.setItemSlot(EquipmentSlot.OFFHAND,oldOff);player.setOnGround(oldGround);reset();}
    }
    private void run(){
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)world.getChunk((ORIGIN.getX()>>4)+dx,(ORIGIN.getZ()>>4)+dz);
        for(var pos:BlockPos.betweenClosed(new BlockPos(minX(),world.getMinY(),minZ()),new BlockPos(minX()+47,world.getMaxY(),minZ()+47)))world.setBlock(pos,Blocks.AIR.defaultBlockState(),2);

        plane(64,Blocks.STONE.defaultBlockState());
        var exit=search("flat-below-high-source");check(exit.getY()==65,"high mapped Y never overrides a real flat ground candidate");
        var reused=new PortalSiteSearch(new PortalTravel.Target(world,ORIGIN,PortalKind.INNER));reused.step();
        check(reused.finished()&&exit.equals(reused.exit()),"existing safe portal reuses in one step");reset();

        plane(64,Blocks.GRASS_BLOCK.defaultBlockState());
        for(int x=minX();x<minX()+48;x++)for(int z=minZ();z<minZ()+48;z++)put(x,65,z,((x+z)%2==0?Blocks.SHORT_GRASS:Blocks.SNOW).defaultBlockState());
        exit=search("grass-and-thin-snow");check(exit.getY()==65,"replaceable vegetation is cleared at ground level");reset();

        for(int x=minX();x<minX()+48;x++)for(int z=minZ();z<minZ()+48;z++)put(x,63+Math.floorMod(x,4),z,Blocks.STONE.defaultBlockState());
        exit=search("three-block-slope");check(exit.getY()==67,"small uneven ground is leveled without a high pedestal");
        boolean connected=true;
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            int ground=63+Math.floorMod(exit.getX()+x,4);
            for(int y=ground;y<exit.getY();y++)connected&=PortalTravel.ground(world,new BlockPos(exit.getX()+x,y,exit.getZ()+z));
        }
        check(connected,"short foundations actually connect every slope column");reset();

        plane(64,Blocks.STONE.defaultBlockState());plane(190,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true));
        exit=search("under-dense-canopy");check(exit.getY()==65,"leaf canopy cannot become the arrival floor");
        check(world.getBlockState(new BlockPos(exit.getX(),190,exit.getZ())).is(Blocks.OAK_LEAVES),"canopy is preserved");reset();

        for(int x=ORIGIN.getX()-3;x<=ORIGIN.getX()+3;x++)for(int z=minZ();z<minZ()+48;z++)put(x,64,z,Blocks.STONE.defaultBlockState());
        exit=search("narrow-ground-ledge");check(exit.getY()==65,"ledge extension stays level with land rather than high sky");reset();

        plane(0,Blocks.WATER.defaultBlockState());
        exit=search("water-only");check(exit.getY()==2,"water fallback is directly above the water surface");
        check(world.getBlockState(new BlockPos(exit.getX(),0,exit.getZ())).is(Blocks.WATER),"water is not drained to form an exit");reset();

        exit=search("empty-void");check(exit.getY()==ORIGIN.getY(),"last-resort void platform does not add a forced height offset");reset();

        plane(64,Blocks.STONE.defaultBlockState());var center=new BlockPos(ORIGIN.getX(),65,ORIGIN.getZ());
        for(var obstacle:List.of(Blocks.CHEST,GuogaologyBlocks.SEQUENCE_CORE,Blocks.LAVA,Blocks.POWDER_SNOW,Blocks.SWEET_BERRY_BUSH)){
            put(center.getX(),center.getY(),center.getZ(),obstacle.defaultBlockState());
            check(PortalTravel.siteQuality(world,center)==0,"unsafe/nonreplaceable content rejects site: "+obstacle);
            check(PortalTravel.buildGate(world,center,PortalKind.INNER)==null&&world.getBlockState(center).is(obstacle),"construction revalidation preserves obstacle: "+obstacle);
            world.setBlock(center,Blocks.AIR.defaultBlockState(),2);
        }
        put(center.getX(),center.getY(),center.getZ(),Blocks.CHEST.defaultBlockState());
        exit=search("blocked-center");check(!exit.equals(center)&&world.getBlockState(center).is(Blocks.CHEST),"search finds another ground site and preserves the chest");reset();
        System.out.println("PORTAL_GROUND_042_OK checks="+checks+" scenarios=8 existing_reuse=true bounded=true");
    }
}
