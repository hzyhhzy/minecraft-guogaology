package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.block.NumberStoneBlock;
import dev.guogaology.world.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Release generation, never a synthetic replacement for natural chunks. QA only. */
public final class CrystalOre056Checks {
    private static final class Stats {
        long rock,ore;int chunks,empty;
        final long[] rockY=new long[12],oreY=new long[12];
        void add(Stats other){rock+=other.rock;ore+=other.ore;chunks+=other.chunks;empty+=other.empty;for(int i=0;i<12;i++){rockY[i]+=other.rockY[i];oreY[i]+=other.oreY[i];}}
        String json(){return "{\"chunks\":"+chunks+",\"rock\":"+rock+",\"ore\":"+ore+",\"orePerRock\":"+(rock==0?0:ore/(double)rock)+",\"rockY32\":"+Arrays.toString(rockY)+",\"oreY32\":"+Arrays.toString(oreY)+"}";}
    }
    private static void check(boolean value,String why){if(!value)throw new AssertionError("Ore056: "+why);}
    private static Stats sample(ServerLevel world,int cx,int cz,int side,int skin,boolean vanilla){
        var stats=new Stats();var classes=new IdentityHashMap<Block,Integer>();
        for(int dx=0;dx<side;dx++)for(int dz=0;dz<side;dz++){
            var chunk=world.getChunk(cx+dx,cz+dz);stats.chunks++;long before=stats.rock;
            for(int sectionIndex=0;sectionIndex<chunk.getSectionsCount();sectionIndex++){
                var section=chunk.getSection(sectionIndex);if(section.hasOnlyAir())continue;
                int baseY=world.getMinY()+sectionIndex*16;
                for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    var state=section.getBlockState(x,y,z);var block=state.getBlock();
                    int type=classes.computeIfAbsent(block,b->{
                        if(vanilla){
                            if(b==Blocks.IRON_ORE||b==Blocks.DEEPSLATE_IRON_ORE)return 2;
                            var key=BuiltInRegistries.BLOCK.getKey(b);
                            return state.is(BlockTags.STONE_ORE_REPLACEABLES)||state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)||key.getNamespace().equals("minecraft")&&key.getPath().endsWith("_ore")?1:0;
                        }
                        if(b==CoreResources.ORES.get(skin))return 2;
                        return (skin==0?b instanceof NumberStoneBlock:skin==1?b==GuogaologyBlocks.ABSENCE_GLASS:b==GuogaologyBlocks.ROOTBOUND_STONE)?1:0;
                    });
                    if(type==0)continue;int band=(baseY+y+64)/32;
                    stats.rock++;if(band>=0&&band<12)stats.rockY[band]++;
                    if(type==2){stats.ore++;if(band>=0&&band<12)stats.oreY[band]++;}
                }
            }
            if(stats.rock==before)stats.empty++;
        }
        return stats;
    }
    public static void run(ServerPlayer player)throws Exception{
        var server=player.level().getServer();var overworld=server.overworld();var inner=server.getLevel(GuogaologyMod.DIMENSION);var under=server.getLevel(GuogaologyMod.GUOGAO);
        var results=new LinkedHashMap<String,Stats>();var iron=new Stats();
        // Fixed patches; do not seek iron-rich mountains or choose samples by ore.
        for(int[] patch:new int[][]{{128,128},{192,128},{128,192},{192,192}}){
            System.out.println("ORE056_SAMPLE vanilla origin="+Arrays.toString(patch));iron.add(sample(overworld,patch[0],patch[1],4,0,true));
        }
        results.put("vanillaIron",iron);System.out.println("ORE056_RESULT vanillaIron "+iron.json());check(iron.ore>0&&iron.rock>100000,"native iron benchmark has actual host rock and iron");
        long seed=ProceduralTerrain.seed(inner.getChunkSource().randomState());var sites=new TreeMap<Integer,ChunkPos>();var lhoSites=new ArrayList<ChunkPos>();
        for(int ring=1;ring<=24&&(sites.size()<7||lhoSites.size()<8);ring++)for(int k=0;k<ring*8;k++){
            double a=k*Math.PI*2/(ring*8);int x=(int)Math.round(Math.cos(a)*ring*192),z=(int)Math.round(Math.sin(a)*ring*192);
            int kind=BiomeRegions.kind(seed,x+24,z+24);if(sites.containsKey(kind)&&(kind!=3||lhoSites.size()>=8))continue;
            boolean inside=true;for(int xx:new int[]{0,48})for(int zz:new int[]{0,48})inside&=BiomeRegions.kind(seed,x+xx,z+zz)==kind;
            if(!inside)continue;var column=TerrainField.column(seed,x+24,z+24,false);int depth=0;
            for(int y=-48;y<=304;y+=8)if(column.density(y)>0)depth++;
            if(depth>=(kind==3?1:3)){
                var cp=new ChunkPos(x>>4,z>>4);sites.putIfAbsent(kind,cp);
                if(kind==3&&lhoSites.stream().noneMatch(p->Math.abs(p.x()-cp.x())<12&&Math.abs(p.z()-cp.z())<12))lhoSites.add(cp);
            }
        }
        check(sites.size()==7&&lhoSites.size()==8,"bounded sampler locates seven themes and eight separated LHO patches: "+sites+" lho="+lhoSites);
        var ordinary=new Stats();var lho=new Stats();
        for(var site:sites.entrySet()){
            int kind=site.getKey();var cp=site.getValue();System.out.println("ORE056_SAMPLE biome="+BiomeRegions.NAMES[kind]+" chunk="+cp);
            if(kind==3)continue;
            var stats=sample(inner,cp.x(),cp.z(),4,kind==3?1:0,false);results.put(BiomeRegions.NAMES[kind],stats);
            check(stats.rock>10000,"actual host volume in "+BiomeRegions.NAMES[kind]);check(stats.ore>0,"actual ore in "+BiomeRegions.NAMES[kind]);
            if(kind!=3)ordinary.add(stats);
        }
        for(var cp:lhoSites){System.out.println("ORE056_SAMPLE LHO chunk="+cp);lho.add(sample(inner,cp.x(),cp.z(),4,1,false));}
        results.put("lho_absence",lho);check(lho.rock>50000&&lho.ore>0,"actual ore throughout separated LHO patches");
        results.put("innerOrdinary",ordinary);var dread=new Stats();
        for(int[] patch:new int[][]{{520,520},{584,520},{520,584},{584,584}}){
            System.out.println("ORE056_SAMPLE Underworld origin="+Arrays.toString(patch));dread.add(sample(under,patch[0],patch[1],2,2,false));
        }
        results.put("underworld",dread);check(dread.ore>0,"actual Underworld veins");
        for(int band=0;band<12;band++)if(ordinary.rockY[band]>20000)check(ordinary.oreY[band]>0,"ore reaches populated height band "+(-64+32*band));
        var report=new StringBuilder("{\"seed\":").append(overworld.getSeed()).append(",\"bandMinimumY\":-64,\"bandHeight\":32,\"samples\":{");
        boolean first=true;for(var entry:results.entrySet()){if(!first)report.append(',');first=false;report.append('"').append(entry.getKey()).append("\":").append(entry.getValue().json());System.out.println("ORE056_RESULT "+entry.getKey()+" "+entry.getValue().json());}
        report.append("}}\n");Files.writeString(Path.of("ore056-density.json"),report.toString());
        double ironRate=iron.ore/(double)iron.rock,ordinaryRate=ordinary.ore/(double)ordinary.rock,lhoRate=lho.ore/(double)lho.rock;
        check(ordinaryRate/ironRate>=.8&&ordinaryRate/ironRate<=1.2,"mean ordinary ore density stays within20% of native iron: "+ordinaryRate/ironRate);
        check(lhoRate/ordinaryRate>=.12&&lhoRate/ordinaryRate<=.28,"LHO actual ore density approximates one fifth across separated patches: "+lhoRate/ordinaryRate);
        System.out.println("ORE056_NATIVE_GENERATION_OK chunks="+(iron.chunks+ordinary.chunks+lho.chunks+dread.chunks)+" allBiomes/realVanillaIron/fullHeight lhoRatio="+lhoRate/ordinaryRate);
    }
    public static void display(ServerPlayer player){
        var world=player.level().getServer().overworld();player.closeContainer();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.setNoGravity(true);
        for(var pos:BlockPos.betweenClosed(new BlockPos(-12,219,-4),new BlockPos(12,228,12)))world.setBlock(pos,pos.getY()==219?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        for(int x=-7;x<=7;x++)for(int y=220;y<=224;y++){
            world.setBlock(new BlockPos(x,y,0),((x+14)%5==2&&(y==221||y==223)?CoreResources.EMPTY_CRYSTAL_ORE:GuogaologyBlocks.ABSENCE_GLASS).defaultBlockState(),2);
            if((x+8)%4==0)world.setBlock(new BlockPos(x,y,-4),Blocks.POLISHED_ANDESITE.defaultBlockState(),2);
        }
        player.teleport(new TeleportTransition(world,new Vec3(7.5,224,12.5),Vec3.ZERO,150,10,TeleportTransition.DO_NOTHING));player.hasChangedDimension();player.getAbilities().flying=true;player.onUpdateAbilities();
    }
}
