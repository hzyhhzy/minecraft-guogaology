package dev.googology.mergeqa;

import dev.googology.*;
import dev.googology.block.*;
import dev.googology.outer.registry.ModBlocks;
import dev.googology.portal.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;
import java.util.*;

/** Real registry / interaction checks for shared 0.3.5 materials. QA JAR only. */
final class MaterialChecks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void move(ServerPlayer p,ServerLevel level,double x,double y,double z,float yaw,float pitch){p.teleport(new TeleportTransition(level,new Vec3(x,y,z),Vec3.ZERO,yaw,pitch,TeleportTransition.DO_NOTHING));}
    static void run(ServerPlayer player){
        var level=player.level().getServer().overworld();var pos=new BlockPos(14,240,22);
        check(BuiltInRegistries.BLOCK.keySet().stream().noneMatch(id->id.getNamespace().equals("googology_outer")),"no separate imported block registry");
        check(BuiltInRegistries.ITEM.keySet().stream().noneMatch(id->id.getNamespace().equals("googology_outer")),"no separate imported item attribution");
        for(var registry:BuiltInRegistries.REGISTRY)check(registry.keySet().stream().noneMatch(id->id.getNamespace().equals("googology_outer")),"no retired builtin registry IDs: "+registry.key());
        level.registryAccess().registries().forEach(entry->check(entry.value().keySet().stream().noneMatch(id->id.getNamespace().equals("googology_outer")),"no retired dynamic registry IDs: "+entry.key()));
        check(!level.getServer().getResourceManager().getNamespaces().contains("googology_outer"),"no retired data namespace");
        check(ModBlocks.ORDINAL_STONE==Blocks.STONE&&ModBlocks.HELL_ORDINAL_STONE==Blocks.NETHERRACK,"vanilla mother rock");
        check(ModBlocks.ANDESITE_ORDINAL_STONE==Blocks.ANDESITE&&ModBlocks.COBBLED_ORDINAL_STONE==Blocks.COBBLESTONE,"vanilla surface stones");
        check(ModBlocks.wood("christmas","log")==Blocks.SPRUCE_LOG&&ModBlocks.wood("hell_christmas","log")==Blocks.DARK_OAK_LOG,"vanilla Christmas logs");
        check(ModBlocks.CHRISTMAS_LEAVES==ModBlocks.wood("christmas","leaves")&&ModBlocks.CHRISTMAS_LEAVES!=Blocks.SPRUCE_LEAVES,"dedicated original Outer Christmas leaves");
        check(ModBlocks.wood("laver","leaves")==GoogologyBlocks.GIANT_LAVER&&ModBlocks.wood("laver","log")==GoogologyBlocks.LAVER_VEIN,"one Laver family");
        check(GoogologyBlocks.LAVER_VEIN.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS),"Laver log keeps orientation");
        check(ModBlocks.LHO_GLASS==GoogologyBlocks.ABSENCE_GLASS,"one void glass");
        var creative=new HashSet<Item>();for(String tab:CreativeCatalog.TABS)for(ItemStack stack:CreativeCatalog.entries(tab))creative.add(stack.getItem());
        for(var block:ModBlocks.ORDERED_BLOCKS)if(block.asItem()!=Items.AIR&&BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("googology")&&!PortalKind.isPortal(block.defaultBlockState()))check(creative.contains(block.asItem()),"imported block in host creative tabs: "+block);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        for(int n=0;n<16;n++){
            var state=GoogologyBlocks.ordinalBrick(n);level.setBlock(pos,state,2);state.useWithoutItem(level,player,hit);
            check(level.getBlockState(pos).getValue(OrdinalBrickBlock.NUMBER)==(n+1)%16,"number brick cycles once: "+n);
            var stack=StatefulDecorBlock.copyAppearance(new ItemStack(GoogologyBlocks.ORDINAL_BRICKS),level.getBlockState(pos));
            check(stack.get(DataComponents.BLOCK_STATE).properties().get("number").equals(""+(n+1)%16),"brick number survives item component");
        }
        for(var lamp:GoogologyBlocks.SEQUENCE_LIGHTS)for(int n:new int[]{0,9,31,32,33}){
            var state=lamp.defaultBlockState().setValue(ChristmasDigitBlock.DIGIT,n);level.setBlock(pos,state,2);state.useWithoutItem(level,player,hit);var next=level.getBlockState(pos);
            check(next.is(lamp)&&next.getValue(ChristmasDigitBlock.DIGIT)==(n>=32?0:n+1),"lamp cycles value and keeps color");
            var drops=Block.getDrops(next,level,pos,null,player,ItemStack.EMPTY);
            check(drops.size()==1&&drops.getFirst().get(DataComponents.BLOCK_STATE).properties().get("digit").equals(""+(n>=32?0:n+1)),"lamp drops edited number");
        }
        for(var stone:GoogologyBlocks.NUMBER_STONES){var state=stone.defaultBlockState();level.setBlock(pos,state,2);state.useWithoutItem(level,player,hit);check(level.getBlockState(pos)==state,"number stones do not cycle");}
        var worlds=List.of(level,player.level().getServer().getLevel(GoogologyMod.OUTER),player.level().getServer().getLevel(GoogologyMod.DIMENSION),player.level().getServer().getLevel(GoogologyMod.GUOGAO));
        for(var world:worlds)for(var kind:PortalKind.values()){
            var target=PortalTravel.destination(world.dimension(),kind);int style=target.equals(GoogologyMod.GUOGAO)?2:target.equals(GoogologyMod.DIMENSION)?1:0;
            check(kind.appearance(world.dimension().identifier().toString())==style,"portal appearance follows destination");
            var bounds=kind.block().defaultBlockState().getShape(world,pos).bounds();
            check(Math.abs(bounds.minY-4.5/16)<1e-6&&Math.abs(bounds.maxY-5.5/16)<1e-6,"portal outline lowered one quarter block");
        }
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        System.out.println("MATERIALS_035_OK checks="+checks);
    }
    static void display(ServerPlayer player,int scene){
        if(scene==9){ReturnPortalChecks.display(player);return;}
        if(scene>=6){BoundaryChecks.display(player,scene-2);return;}
        var level=player.level().getServer().overworld();int z=350+scene*50;
        for(int cx=-2;cx<=2;cx++)for(int cz=(z-20)>>4;cz<=(z+30)>>4;cz++)level.getChunk(cx,cz);
        for(var p:BlockPos.betweenClosed(new BlockPos(-20,219,z-15),new BlockPos(20,227,z+20)))level.setBlock(p,p.getY()==219?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        if(scene==0){
            dev.googology.client.CoreAnimationRenderer.previewSeconds=0;
            for(var q:BlockPos.betweenClosed(new BlockPos(-9,220,z-2),new BlockPos(9,225,z-2)))level.setBlock(q,Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
            String[] names={"fffz_trace","lho_trace","lho_trace_lv2","lho_trace_lv3","fos_trace"};
            for(int i=0;i<names.length;i++){var p=new BlockPos((i-2)*3,221,z);level.setBlock(p.below(),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);var b=BuiltInRegistries.BLOCK.getValue(Identifier.parse("googology:"+names[i]));level.setBlock(p,b.defaultBlockState(),2);}
            move(player,level,.5,223,z+11,180,9);
        }else if(scene==1){
            for(int i=0;i<3;i++)PortalRitual.fillPortal(level,new BlockPos((i-1)*7,220,z),PortalKind.values()[new int[]{0,2,1}[i]]);
            move(player,level,.5,230,z+19,180,28);
        }else if(scene==2){
            Block[] row={Blocks.OAK_LOG,Blocks.DARK_OAK_LOG,GoogologyBlocks.DREAD_LOG,BuiltInRegistries.BLOCK.getValue(Identifier.parse("googology:dread_planks")),GoogologyBlocks.LAVER_VEIN,GoogologyBlocks.LAVER_PLANKS,GoogologyBlocks.GIANT_LAVER};
            for(int i=0;i<row.length;i++)for(int y=220;y<=222;y++)level.setBlock(new BlockPos((i-3)*2,y,z),row[i].defaultBlockState(),2);
            for(int n=0;n<16;n++)level.setBlock(new BlockPos(n-8,220,z+3),GoogologyBlocks.ordinalBrick(n),2);
            for(int n=0;n<6;n++){level.setBlock(new BlockPos((n-3)*2,220,z+6),GoogologyBlocks.SEQUENCE_LIGHTS[n].defaultBlockState(),2);level.setBlock(new BlockPos((n-3)*2,221,z+6),GoogologyBlocks.SEQUENCE_LIGHTS[n].defaultBlockState().setValue(ChristmasDigitBlock.DIGIT,n*6),2);}
            for(int n=0;n<3;n++)level.setBlock(new BlockPos((n-1)*4,220,z+10),dev.googology.mining.MiningContent.TABLES[n].defaultBlockState(),2);
            move(player,level,.5,228,z+21,180,20);
        }else if(scene==3){
            dev.googology.client.CoreAnimationRenderer.previewSeconds=10;
            for(var q:BlockPos.betweenClosed(new BlockPos(-9,220,z-2),new BlockPos(9,225,z-2)))level.setBlock(q,Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
            String[] names={"fffz_trace","lho_trace","lho_trace_lv2","lho_trace_lv3","fos_trace"};
            for(int i=0;i<names.length;i++){var q=new BlockPos((i-2)*3,221,z);level.setBlock(q.below(),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);level.setBlock(q,BuiltInRegistries.BLOCK.getValue(Identifier.parse("googology:"+names[i])).defaultBlockState(),2);}
            move(player,level,3.5,223,z+10,163,9);
        }else if(scene==4){
            // A close, strongly oblique view exposes the real C1 relief sides.
            dev.googology.client.CoreAnimationRenderer.previewSeconds=0;
            for(var q:BlockPos.betweenClosed(new BlockPos(-2,220,z-2),new BlockPos(-2,225,z+4)))level.setBlock(q,Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
            level.setBlock(new BlockPos(0,220,z),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
            level.setBlock(new BlockPos(0,221,z),GoogologyBlocks.LHO_TRACE.defaultBlockState(),2);
            move(player,level,5.8,222.8,z-2.4,61,10);
        }else{
            dev.googology.client.CoreAnimationRenderer.previewSeconds=21;
            String[] names={"lho_trace","lho_trace_lv2","lho_trace_lv3"};
            for(int i=0;i<names.length;i++){var q=new BlockPos((i-1)*3,221,z);level.setBlock(q.below(),Blocks.DEEPSLATE_TILES.defaultBlockState(),2);level.setBlock(q,BuiltInRegistries.BLOCK.getValue(Identifier.parse("googology:"+names[i])).defaultBlockState(),2);}
            move(player,level,5.5,223,z+11,155,9);
        }
    }
}
