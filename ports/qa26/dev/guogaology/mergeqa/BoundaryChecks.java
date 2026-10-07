package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.outer.world.*;
import dev.guogaology.outer.world.feature.LaverTableFeature;
import dev.guogaology.world.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.*;
import java.nio.file.*;
import java.util.*;

/** Real engine biome selection, terrain and table decoration regressions. QA JAR only. */
public final class BoundaryChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static BlockPos outerSite,innerSite;
    public static void run(ServerPlayer player)throws Exception{
        long start=System.nanoTime();
        var server=player.level().getServer();var outer=server.getLevel(GuogaologyMod.OUTER);
        var source=outer.getChunkSource().getGenerator().getBiomeSource();
        check(source instanceof LhoBorderBiomeSource,"fresh outer dimension must use bounded coast source");
        var field=LhoBorderBiomeSource.class.getDeclaredField("source");field.setAccessible(true);
        var original=(net.minecraft.world.level.biome.Climate.ParameterList<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>)field.get(source);var climate=outer.getChunkSource().randomState().sampler();
        int oldEdge=0,edge=0,restored=0,voidCount=0;
        // Same footprint as the pre-fix diagnostic, sampled at 16 m, not 8 m.
        for(int z=-2048;z<2048;z+=16)for(int x=-2048;x<2048;x+=16){
            var before=original.findValueBruteForce(climate.sample(x>>2,150>>2,z>>2));
            var after=source.getNoiseBiome(x>>2,150>>2,z>>2,climate);
            if(before.is(LhoChunkGenerator.LHO_VOID_BIOME)){voidCount++;check(after.equals(before),"void footprint changed "+x+","+z+" before="+before+" after="+after+" directAgain="+original.findValueBruteForce(climate.sample(x>>2,150>>2,z>>2)));}
            if(before.is(LhoChunkGenerator.LHO_EDGE_BIOME)){
                oldEdge++;
                if(after.is(LhoChunkGenerator.LHO_EDGE_BIOME)){
                    edge++;boolean found=false;
                    for(int dx=-12;dx<=12&&!found;dx++)for(int dz=-12;dz<=12;dz++)if(dx*dx+dz*dz<=144&&original.findValueBruteForce(climate.sample((x>>2)+dx,150>>2,(z>>2)+dz)).is(LhoChunkGenerator.LHO_VOID_BIOME)){found=true;break;}
                    check(found,"edge farther than 48m from actual void at "+x+","+z);
                }else{restored++;check(!after.is(LhoChunkGenerator.UNDERWORLD_BIOME),"surface fallback became underground biome");}
            }else check(before.equals(after),"ordinary original biome changed");
        }
        check(edge>100&&restored>1000&&voidCount>100,"sample includes coast, restored inland and void");
        check(!source.getNoiseBiome(-224>>2,150>>2,1152>>2,climate).is(LhoChunkGenerator.LHO_EDGE_BIOME),"old 480m barren patch persists");
        // The quantized climate boundary that previously tied void against edge.
        var tie=source.getNoiseBiome(-1136>>2,150>>2,-592>>2,climate);
        for(int i=0;i<128;i++){
            source.getNoiseBiome((i*79)-3000,37,(i*97)-6000,climate);
            check(tie.equals(source.getNoiseBiome(-1136>>2,37,-592>>2,climate)),"query order changes biome tie");
        }
        // Locate fresh outer coast away from all earlier fixture screenshots.
        search:for(int z=8192;z<12288;z+=32)for(int x=8192;x<12288;x+=32)
            if(source.getNoiseBiome(x>>2,150>>2,z>>2,climate).is(LhoChunkGenerator.LHO_EDGE_BIOME)){outerSite=new BlockPos(x,140,z);break search;}
        check(outerSite!=null,"outer coast site");
        var inner=server.getLevel(GuogaologyMod.DIMENSION);long seed=ProceduralTerrain.seed(inner.getChunkSource().randomState());int gaps=0;
        // Includes real noise interpolation, the ceiling of Laver, and the bottom of dense mainland.
        for(int z=8192;z<10240;z+=16)for(int x=8192;x<10240;x+=16){
            var weights=BiomeRegions.weights(seed,x,z);
            if(!BiomeRegions.lhoGap(weights))continue;
            gaps++;
            var col=TerrainField.column(seed,x,z,false);
            check(!col.water.fluid(),"separation filled with water");
            var samples=new TerrainSamples(seed,false);
            for(int y=-60;y<316;y+=4)check(samples.density(x,y,z)<0,"solid/transparent bridge remains at "+x+","+y+","+z);
            if(innerSite==null&&Math.abs(BiomeRegions.lhoContrast(weights))<.08&&weights[0]+weights[1]>.3)innerSite=new BlockPos(x,125,z);
        }
        check(gaps>100&&innerSite!=null,"sufficient independent inner boundary samples");
        // Actual generated blocks must also be empty, after decorations and buildings.
        inner.getChunk(innerSite.getX()>>4,innerSite.getZ()>>4);
        int air=0;
        for(int y=-60;y<316;y++){
            var pos=new BlockPos(innerSite.getX(),y,innerSite.getZ());
            check(inner.getBlockState(pos).isAir(),"decoration bridged the coast at "+pos+": "+inner.getBlockState(pos));air++;
        }
        int plain=0;var random=RandomSource.create(36080);
        for(int i=0;i<10000;i++)if(LaverTableFeature.tableGummy(random,GuogaologyBlocks.AMBER_GUOGAO)==GuogaologyBlocks.PLAIN_AMBER_GUOGAO)plain++;
        check(plain>7850&&plain<8150,"plain gummy probability: "+plain);
        var world=server.overworld();
        var feature=(LaverTableFeature)world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getValue(GuogaologyMod.id("laver_table")).config();
        var template=world.getStructureManager().get(GuogaologyMod.id("laver_table_7")).orElseThrow();
        var anchor=new BlockPos(0,240,180);int emittedPlain=0,emittedFace=0,mixed=0;
        for(var rotation:Rotation.values()){
            var settings=new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setRotation(rotation);
            var top=LaverTableFeature.tableSurface(template,anchor,settings);
            check(!top.isEmpty(),"actual Laver table surface");
            for(int i=0;i<400;i++){
                for(var pos:top)world.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2);
                int count=feature.placeGummies(world,random,template,anchor,settings,GuogaologyBlocks.AMBER_GUOGAO),a=0,b=0;
                for(var pos:top){var state=world.getBlockState(pos.above());if(state.is(GuogaologyBlocks.PLAIN_AMBER_GUOGAO))a++;else if(state.is(GuogaologyBlocks.AMBER_GUOGAO))b++;}
                check(a+b==count,"table writes exactly its selected gummies");emittedPlain+=a;emittedFace+=b;if(a>0&&b>0)mixed++;
            }
        }
        check(emittedPlain>100&&emittedFace>15&&mixed>0,"both variants and mixed tables actually generate");
        for(int i=4;i<8;i++){
            var block=GuogaologyBlocks.ALL_JELLIES[i];
            check(GuogaologyBlocks.TRANSLUCENT.contains(block),"plain gummy transparency registered");
            check(CreativeCatalog.entries("lighting").stream().anyMatch(s->s.is(block.asItem())),"plain gummy in creative category");
            check(dev.guogaology.portal.PortalRitual.isFruit(new net.minecraft.world.item.ItemStack(block)),"plain gummy works as offering");
            var drops=Block.getDrops(block.defaultBlockState(),server.overworld(),new BlockPos(0,220,0),null,player,net.minecraft.world.item.ItemStack.EMPTY);
            check(drops.size()==1&&drops.getFirst().is(block.asItem()),"plain gummy harvest preserves style");
        }
        String result="old_edge="+oldEdge+"\nnew_edge="+edge+"\nrestored_inland="+restored+"\nvoid_unchanged="+voidCount+"\ninner_gap_columns="+gaps+"\nactual_air_column="+air+"\nplain_gummies_per_10000="+plain+"\nactual_gummies="+emittedPlain+" plain / "+emittedFace+" embossed; mixed tables="+mixed+"\nouter_site="+outerSite+"\ninner_site="+innerSite+"\nseconds="+(System.nanoTime()-start)/1e9+"\n";
        Files.writeString(Path.of("boundary-checks.txt"),result);System.out.println("BOUNDARY_036_OK "+result.replace('\n',' '));
    }
    public static void display(ServerPlayer p,int scene){
        var server=p.level().getServer();var level=server.overworld();double x=0,y=225,z=615;float pitch=25;
        if(scene==4){
            for(var pos:BlockPos.betweenClosed(new BlockPos(-12,219,597),new BlockPos(12,224,613)))level.setBlock(pos,pos.getY()==219?Blocks.DEEPSLATE_TILES.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
            for(int i=0;i<8;i++)level.setBlock(new BlockPos((i%4)*3-5,220,602+(i/4)*4),GuogaologyBlocks.ALL_JELLIES[i].defaultBlockState(),2);
        }else{
            level=server.getLevel(scene==5?GuogaologyMod.OUTER:GuogaologyMod.DIMENSION);
            var site=scene==5?outerSite:innerSite;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)level.getChunk((site.getX()>>4)+dx,(site.getZ()>>4)+dz);
            x=site.getX()+.5;y=scene==5?Math.min(300,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,site.getX(),site.getZ())+85):245;z=site.getZ()+75.5;pitch=52;
            if(scene==6){y=130;z=site.getZ()+24.5;pitch=22;}
        }
        p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        p.teleport(new net.minecraft.world.level.portal.TeleportTransition(level,new net.minecraft.world.phys.Vec3(x,y,z),net.minecraft.world.phys.Vec3.ZERO,180,pitch,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
    }
}
