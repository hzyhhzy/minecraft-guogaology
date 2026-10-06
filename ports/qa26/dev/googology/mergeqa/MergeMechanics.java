package dev.googology.mergeqa;

import dev.googology.*;
import dev.googology.mining.*;
import dev.googology.portal.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual loaded-server checks, not mocks or replacement implementations. */
final class MergeMechanics {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("googology:"+id)));}
    private static void move(ServerPlayer p,ServerLevel world,BlockPos pos){p.teleport(new TeleportTransition(world,Vec3.atCenterOf(pos),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));}
    private static void clear(ServerLevel world,BlockPos center){
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.getChunk((center.getX()>>4)+x,(center.getZ()>>4)+z);
        for(var q:BlockPos.betweenClosed(center.offset(-4,-1,-4),center.offset(4,5,4)))world.setBlock(q,q.getY()==center.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
    }
    private static void ritual(ServerLevel world,BlockPos center,Block frame,ItemStack offering,PortalKind kind){
        ritual(world,center,new Block[]{frame},offering,kind);
    }
    private static void ritual(ServerLevel world,BlockPos center,Block[] frames,ItemStack offering,PortalKind kind){
        clear(world,center);
        int frameIndex=0;
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(PortalRitual.isFrameOffset(x,z))world.setBlock(center.offset(x,0,z),frames[frameIndex++%frames.length].defaultBlockState(),2);
        check(PortalRitual.isValidRing(world,center,kind),"valid local ring: "+world.dimension());
        var entity=new ItemEntity(world,center.getX()+.5,center.getY()+.6,center.getZ()+.5,offering);
        check(PortalRitual.tryActivate(world,entity),"ritual activation: "+world.dimension()+" "+offering);
        check(entity.isRemoved(),"ritual consumes exactly one singleton offering");
        var gate=PortalRitual.gateAt(world,center);
        check(gate!=null&&gate.kind()==kind&&PortalRitual.complete(world,gate),"21 blocks form expected complete gate");
        int style=kind.appearance(world.dimension().identifier().toString());
        check(world.getBlockState(center).getValue(dev.googology.block.GoogologyPortalBlock.STYLE)==style,"portal field destination style");
        check(world.getBlockState(center.offset(2,0,0)).getValue(dev.googology.block.GoogologyPortalFrameBlock.STYLE)==style,"portal frame destination style");
    }
    static void testTotem(ServerPlayer p){
        var ow=p.level();var book=item("true_omega_manuscript");
        GearData.install(book,item("guogao_heart_lv2"),3);p.setItemSlot(EquipmentSlot.OFFHAND,book);p.setGameMode(GameType.SURVIVAL);ManuscriptEffects.tick(p);
        p.getInventory().setItem(9,new ItemStack(Items.TOTEM_OF_UNDYING,2));p.setHealth(10);p.invulnerableTime=0;
        boolean hit=p.hurtServer(ow,p.damageSources().generic(),1000);
        System.out.println("MERGE_TOTEM hit="+hit+" alive="+p.isAlive()+" health="+p.getHealth()+" remaining="+p.getInventory().getItem(9).getCount());
        check(p.isAlive()&&p.getInventory().getItem(9).getCount()==1,"lethal hit consumes one inventory totem");
        check(GearData.cores(book).size()==1,"rescue does not consume book/core");
        p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(p);p.setHealth(p.getMaxHealth());p.setGameMode(GameType.CREATIVE);p.removeAllEffects();

        System.out.println("MERGE_TOTEM_OK");
    }
    static void run(ServerPlayer p){
        var server=p.level().getServer();var ow=server.overworld();var outer=server.getLevel(GoogologyMod.OUTER);var inner=server.getLevel(GoogologyMod.DIMENSION);var hell=server.getLevel(GoogologyMod.GUOGAO);
        ritual(ow,new BlockPos(8,240,8),Blocks.CAKE,new ItemStack(Items.APPLE),PortalKind.GGG);
        var rejectPos=new BlockPos(24,240,8);clear(outer,rejectPos);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(PortalRitual.isFrameOffset(x,z))outer.setBlock(rejectPos.offset(x,0,z),Blocks.DIRT.defaultBlockState(),2);
        var denied=new ItemEntity(outer,24.5,240.6,8.5,new ItemStack(MiningContent.MATERIALS[3],2));
        check(!PortalRitual.tryActivate(outer,denied)&&denied.getItem().getCount()==2,"cheap frame cannot enter inner; rejected offering is preserved");
        for(int tier=0;tier<4;tier++){
            check(PortalRitual.isFrameMaterial(outer,MiningContent.STORAGE[tier].defaultBlockState(),PortalKind.INNER),"each mineral storage tier accepted");
            if(tier<3)check(PortalRitual.ritualKind(outer,new ItemStack(MiningContent.MATERIALS[tier]))==null,"only Omega offering enters inner");
        }
        check(!PortalRitual.isFrameMaterial(outer,Blocks.CAKE.defaultBlockState(),PortalKind.INNER),"cake cannot bypass mineral gate");
        check(!PortalRitual.isFrameMaterial(outer,BuiltInRegistries.BLOCK.getValue(Identifier.parse("googology:omega_ore")).defaultBlockState(),PortalKind.INNER),"ore is not a storage block");
        ritual(outer,rejectPos,MiningContent.STORAGE,new ItemStack(MiningContent.MATERIALS[3]),PortalKind.INNER);
        ritual(inner,new BlockPos(116,240,32),GoogologyBlocks.AMBER_GUOGAO,new ItemStack(GoogologyBlocks.AMBER_GUOGAO),PortalKind.GUOGAO);
        check(!PortalRitual.isOffering(new ItemStack(Items.STICK)),"irrelevant items ignored");
        for(var test:List.of(Blocks.DIRT,GoogologyBlocks.ABSENCE_GLASS,GoogologyBlocks.GUOGAO_LOAM)){
            check(!test.defaultBlockState().requiresCorrectToolForDrops(),"local raw material needs no special tool");
            check(Block.getDrops(test.defaultBlockState(),ow,new BlockPos(0,240,0),null,p,ItemStack.EMPTY).stream().anyMatch(s->s.is(test.asItem())),"local raw material drops itself barehanded");
        }
        // Existing-gate command travel is immediate; cold exits are covered by the tick-driven portal QA.
        var outerExit=new BlockPos(8,240,8);clear(outer,outerExit);PortalRitual.fillPortal(outer,outerExit,PortalKind.GGG);
        var innerExit=new BlockPos(100,240,36);clear(inner,innerExit);PortalRitual.fillPortal(inner,innerExit,PortalKind.INNER);
        var hellExit=new BlockPos(468,240,132);clear(hell,hellExit);PortalRitual.fillPortal(hell,hellExit,PortalKind.GUOGAO);
        move(p,ow,new BlockPos(8,240,8));PortalTravel.travel(p);check(p.level()==outer,"travel Overworld -> outer");
        ReturnPortalChecks.checkAutomaticArrival(p,PortalKind.GGG);
        move(p,outer,new BlockPos(24,240,8));PortalTravel.toInner(p);check(p.level()==inner,"travel outer -> inner");
        ReturnPortalChecks.checkAutomaticArrival(p,PortalKind.INNER);
        move(p,inner,new BlockPos(116,240,32));PortalTravel.toGuogao(p);check(p.level()==hell,"travel inner -> underworld");
        ReturnPortalChecks.checkAutomaticArrival(p,PortalKind.GUOGAO);
        PortalTravel.returnHome(p);check(p.level()==inner,"return underworld -> inner");
        PortalTravel.returnHome(p);check(p.level()==outer,"return inner -> outer");
        PortalTravel.returnHome(p);check(p.level()==ow,"return outer -> Overworld");
        var broken=new BlockPos(8,240,8);ow.destroyBlock(broken.offset(-2,0,0),false,p);
        check(PortalRitual.gateAt(ow,broken)==null,"broken frame unregisters gate");
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)check(!PortalKind.isPortal(ow.getBlockState(broken.offset(x,0,z))),"broken frame clears whole portal");

        int templates=0;
        check(dev.googology.outer.world.feature.LaverTreeGuardFeature.laverLog()==GoogologyBlocks.LAVER_VEIN,"tree guard uses canonical Laver wood");
        check(dev.googology.outer.world.feature.LaverTreeGuardFeature.laverPlanks()==GoogologyBlocks.LAVER_PLANKS,"table guard uses canonical Laver planks");
        try(var entries=server.getResourceManager().listResources("structure",id->id.getNamespace().equals("googology")&&id.getPath().endsWith(".nbt")).keySet().stream()){
            for(var id:entries.toList()){
                var path=id.getPath();var key=Identifier.fromNamespaceAndPath(id.getNamespace(),path.substring(10,path.length()-4));
                var template=dev.googology.outer.world.feature.TemplatePlaceFeature.loadTemplate(outer,key);
                check(template!=null&&template.getSize().getX()>0,"template decoded "+key);templates++;
                if(key.getPath().startsWith("bms_"))check(!template.filterBlocks(BlockPos.ZERO,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),GoogologyBlocks.ORDINAL_BRICKS).isEmpty(),"BMS palette resolves real number blocks "+key);
            }
        }
        check(templates==61,"all 61 donor templates load (actual "+templates+")");
        System.out.println("MERGE_MECHANICS_OK checks="+checks+" templates="+templates);
    }
}
