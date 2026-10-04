package dev.googology.survival;

import dev.googology.*;
import dev.googology.block.ChristmasDigitBlock;
import dev.googology.world.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;

import net.minecraft.resources.ResourceKey;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Large, rare exploration sanctuaries; no entity spawning or old boss seals. */
public final class SurvivalStructures {
    public static final int SPACING=SanctuaryPlacement.SPACING,RADIUS=70;
    public record Site(int x,int y,int z,SurvivalTheme theme,long hash) {
        public int rotation(){return (int)Math.floorMod(hash,4);}
        public SanctuaryLayout layout(){return SanctuaryLayout.of(theme);}
        public BlockPos position(SanctuaryLayout.Point p,int lift){
            int dx=p.x(),dz=p.z();
            return switch(rotation()) {
                case 1 -> new BlockPos(x-dz,y+p.y()+lift,z+dx);
                case 2 -> new BlockPos(x-dx,y+p.y()+lift,z-dz);
                case 3 -> new BlockPos(x+dz,y+p.y()+lift,z-dx);
                default -> new BlockPos(x+dx,y+p.y()+lift,z+dz);
            };
        }
        public BlockPos center(){return position(layout().arena,1);}
        public BlockPos entrance(){return position(layout().entrance,1);}
    }
    private record Key(long seed,int x,int z,boolean under){}
    private static final Map<Key,Optional<Site>> CACHE=new ConcurrentHashMap<>();
    private static final Map<SurvivalTheme,BlockState[]> PALETTES=new ConcurrentHashMap<>();
    public static Site candidate(long seed,int gx,int gz,boolean under){
        var key=new Key(seed,gx,gz,under);
        return CACHE.computeIfAbsent(key,k->{
            var p=SanctuaryPlacement.find(seed,gx,gz,under);
            return p==null?Optional.empty():Optional.of(new Site(p.x(),p.y(),p.z(),p.theme(),p.hash()));
        }).orElse(null);
    }
    public static Site locate(long seed,BlockPos origin,SurvivalTheme theme){
        int gx=Math.floorDiv(origin.getX(),SPACING),gz=Math.floorDiv(origin.getZ(),SPACING);
        Site best=null;double distance=Double.MAX_VALUE;
        for(int ring=0;ring<=(9216+SPACING-1)/SPACING;ring++){
            for(int dx=-ring;dx<=ring;dx++)for(int dz=-ring;dz<=ring;dz++){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=ring)continue;
                var s=candidate(seed,gx+dx,gz+dz,theme.underworld());
                if(s!=null&&s.theme==theme){double d=s.entrance().distSqr(origin);if(d<distance){distance=d;best=s;}}
            }
            if(best!=null&&ring*SPACING>Math.sqrt(distance)+SPACING)break;
        }
        if(CACHE.size()>8192)CACHE.clear();return best;
    }
    public static List<Site> sitesForChunk(long seed,boolean under,ChunkPos chunk){
        var sites=new ArrayList<Site>();
        for(int gx=Math.floorDiv(chunk.getMinBlockX()-RADIUS,SPACING);gx<=Math.floorDiv(chunk.getMaxBlockX()+RADIUS,SPACING);gx++)
            for(int gz=Math.floorDiv(chunk.getMinBlockZ()-RADIUS,SPACING);gz<=Math.floorDiv(chunk.getMaxBlockZ()+RADIUS,SPACING);gz++){
                var site=candidate(seed,gx,gz,under);
                if(site!=null&&Math.abs(site.x-(chunk.getMinBlockX()+8))<RADIUS+9&&Math.abs(site.z-(chunk.getMinBlockZ()+8))<RADIUS+9)sites.add(site);
            }
        return sites;
    }
    public static void generate(WorldGenLevel world,long seed,boolean under,ChunkPos chunk){
        for(var site:sitesForChunk(seed,under,chunk))render(world,site,chunk);
    }
    public static void render(WorldGenLevel world,Site s,ChunkPos chunk){
        var plan=s.layout();var palette=palette(s.theme);var pos=new BlockPos.MutableBlockPos();
        for(int x=chunk.getMinBlockX();x<=chunk.getMaxBlockX();x++)for(int z=chunk.getMinBlockZ();z<=chunk.getMaxBlockZ();z++){
            int dx=x-s.x,dz=z-s.z;
            int lx=switch(s.rotation()){case 1->dz;case 2->-dx;case 3->-dz;default->dx;};
            int lz=switch(s.rotation()){case 1->-dx;case 2->-dz;case 3->dx;default->dz;};
            if(Math.abs(lx)>plan.width/2||Math.abs(lz)>plan.depth/2)continue;
            for(int y=0;y<plan.height;y++){
                byte code=plan.at(lx,y,lz);if(code==SanctuaryLayout.KEEP)continue;
                pos.set(x,s.y+y,z);if(world.isOutsideBuildHeight(pos))continue;
                if(code==SanctuaryLayout.AIR&&world.getBlockState(pos).isAir())continue;
                BlockState state=stateFor(s,lx,y,lz,code);
                world.setBlock(pos,state.getBlock()==GoogologyBlocks.ORDINAL_STONE?dev.googology.block.NumberStoneBlock.natural(pos.getX(),pos.getY(),pos.getZ()):state,Block.UPDATE_CLIENTS);
            }
        }
        long seed=ProceduralTerrain.seed(world.getLevel().getChunkSource().randomState());
        var foundations=SanctuaryFoundations.plan(seed,s.x,s.y,s.z,s.theme,s.rotation());
        var brush=new VoxelBrush(chunk.getMinBlockX(),chunk.getMaxBlockX(),world.getMinY(),(world.getMaxY()+1)-1,chunk.getMinBlockZ(),chunk.getMaxBlockZ(),
                (x,y,z,m)->world.setBlock(new BlockPos(x,y,z),palette[SanctuaryLayout.MAIN],Block.UPDATE_CLIENTS));
        for(var pier:foundations)if(pier.bounds().intersects(new SanctuarySpace.Bounds(chunk.getMinBlockX(),world.getMinY(),chunk.getMinBlockZ(),chunk.getMaxBlockX(),(world.getMaxY()+1)-1,chunk.getMaxBlockZ())))pier.draw(brush);
        int index=0;
        for(var cache:plan.chests()){
            BlockPos p=s.position(cache.floor(),1);int chestIndex=index++;
            if(!chunk.equals(new ChunkPos(p)))continue;
            Direction facing=Direction.SOUTH;for(int r=0;r<s.rotation();r++)facing=facing.getClockWise();
            world.setBlock(p,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,facing),Block.UPDATE_CLIENTS);
            if(world.getBlockEntity(p) instanceof ChestBlockEntity chest)
                chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,GoogologyMod.id("chests/"+s.theme.id+(cache.relic()?"_sanctum":"_ruin"))),WorldNoise.mix(s.hash+chestIndex*71));
        }
    }
    public static BlockState stateFor(Site s,int lx,int y,int lz,byte code){
        String finaleCore=s.layout().finaleCore(lx,y,lz);
        if(finaleCore!=null)return CoreGrades.levels(finaleCore).get(1).defaultBlockState();
        if(s.theme==SurvivalTheme.ABSENCE&&(code==SanctuaryLayout.PHANTOM_FFFZ||code==SanctuaryLayout.PHANTOM_FOS||code==SanctuaryLayout.PHANTOM_EMPTY)
                &&s.layout().railingCollectible(lx,y,lz))
            return palette(s.theme)[code].setValue(dev.googology.block.AnchoredBlock.STABLE,true);
        if(code==SanctuaryLayout.CROWN)return GoogologyBlocks.ASTRA_WEAVE.defaultBlockState().setValue(dev.googology.block.AstraWeaveBlock.COLOR,AstraCrown.colorAt(lx,y,lz));
        if(code==SanctuaryLayout.BMS_DIGIT){
            var cell=s.layout().bmsCells().get(new SanctuaryLayout.Point(lx,y,lz));
            if(cell==null)throw new IllegalStateException("Missing BMS panel metadata");
            var matrix=bmsFor(s,cell.panel());
            return GoogologyBlocks.ordinalBrick(matrix.rows()[cell.row()][cell.column()]);
        }
        if(s.theme==SurvivalTheme.ABSENCE&&(code==SanctuaryLayout.TRIM||code==SanctuaryLayout.NUMBER)){
            // Coordinate hashing keeps each choice stable across chunks and generation order.
            int letter=(int)Math.floorMod(WorldNoise.hash(s.hash,lx,y,lz),3L);
            return (switch(letter){case 0->GoogologyBlocks.LHO_LETTER_L;case 1->GoogologyBlocks.LHO_LETTER_H;default->GoogologyBlocks.LHO_LETTER_O;}).defaultBlockState();
        }
        if(code==SanctuaryLayout.EMOJI_CABLE){
            int color=Math.floorMod(lx+y+lz,5);
            Block lamp=switch(color){case 1->GoogologyBlocks.CYAN_LANTERN;case 2->GoogologyBlocks.ROSE_LANTERN;case 3->GoogologyBlocks.VIOLET_LANTERN;case 4->GoogologyBlocks.LIME_LANTERN;default->GoogologyBlocks.GUOGAO_LANTERN;};
            return lamp.defaultBlockState().setValue(dev.googology.block.EmojiLanternBlock.EMOTION,Math.floorMod(lx*31+y*17+lz,8)==0?1:0);
        }
        if(code==SanctuaryLayout.NUMBER){
            return GoogologyBlocks.ordinalBrick(Math.floorMod(lx+y+lz,16));
        }
        if(code==SanctuaryLayout.SYMBOL&&s.theme!=SurvivalTheme.HYDRA)return GoogologyBlocks.SYMBOLS[Math.floorMod(lx/7+y/7+lz/7,6)].defaultBlockState();
        if(code==SanctuaryLayout.DIGIT)return dev.googology.world.ChristmasSequences.light(Math.floorMod(lx+lz,10),Math.floorMod(y+lx,6));
        if(code==SanctuaryLayout.FRUIT)return GoogologyBlocks.JELLIES[Math.floorMod(lx/3+lz/3,GoogologyBlocks.JELLIES.length)].defaultBlockState();
        return palette(s.theme)[code];
    }
    public static NotationCatalog.Bms bmsFor(Site site,SanctuaryLayout.Point panel){
        return NotationCatalog.hallBms(WorldNoise.hash(site.hash^0x424d5348414c4cL,panel.x(),panel.y(),panel.z()));
    }
    private static BlockState[] palette(SurvivalTheme theme){return PALETTES.computeIfAbsent(theme,SurvivalStructures::createPalette);}
    private static BlockState[] createPalette(SurvivalTheme theme){
        Block main=switch(theme){case MATRIX->GoogologyBlocks.ORDINAL_STONE;case POWER->GoogologyBlocks.POWER_BRICKS;case HYDRA->Blocks.CALCITE;case ABSENCE->GoogologyBlocks.ABSENCE_GLASS;case WEAVER->GoogologyBlocks.LAVER_PLANKS;case ASTRA->GoogologyBlocks.ASTRA_MARBLE;case GUOGAO->GoogologyBlocks.ROOTBOUND_STONE;case FRONTIER->GoogologyBlocks.LIMIT_STONE;};
        Block trim=switch(theme){case MATRIX->Blocks.POLISHED_ANDESITE;case POWER->Blocks.CHISELED_RED_SANDSTONE;case HYDRA->Blocks.SMOOTH_QUARTZ;case ABSENCE->GoogologyBlocks.LHO_LETTER_H;case WEAVER->GoogologyBlocks.TIANYI_FIBER;case ASTRA->GoogologyBlocks.ASTRA_MINT;case GUOGAO->GoogologyBlocks.DREAD_LOG;case FRONTIER->GoogologyBlocks.RANK_AMBER;};
        Block floor=switch(theme){case MATRIX->Blocks.SMOOTH_STONE;case POWER->Blocks.SMOOTH_SANDSTONE;case HYDRA->Blocks.CALCITE;case ABSENCE->GoogologyBlocks.ABSENCE_GLASS;case WEAVER->GoogologyBlocks.LAVER_PLANKS;case ASTRA->GoogologyBlocks.ASTRA_MARBLE;case GUOGAO->GoogologyBlocks.ROOTBOUND_STONE;case FRONTIER->GoogologyBlocks.LOGIC_IVORY;};
        BlockState[] p=new BlockState[SanctuaryLayout.MATERIAL_COUNT];Arrays.fill(p,main.defaultBlockState());
        p[SanctuaryLayout.AIR]=Blocks.AIR.defaultBlockState();p[SanctuaryLayout.MAIN]=main.defaultBlockState();p[SanctuaryLayout.TRIM]=trim.defaultBlockState();p[SanctuaryLayout.FLOOR]=floor.defaultBlockState();
        p[SanctuaryLayout.GLASS]=(theme==SurvivalTheme.ABSENCE?GoogologyBlocks.ABSENCE_GLASS:theme==SurvivalTheme.GUOGAO?Blocks.PURPLE_STAINED_GLASS:Blocks.LIGHT_BLUE_STAINED_GLASS).defaultBlockState();
        p[SanctuaryLayout.LIGHT]=(theme==SurvivalTheme.GUOGAO?GoogologyBlocks.CYAN_LANTERN:theme==SurvivalTheme.ASTRA?GoogologyBlocks.ASTRA_LIGHT:GoogologyBlocks.ORDINAL_CRYSTAL).defaultBlockState();
        p[SanctuaryLayout.LEAF]=GoogologyBlocks.DREAD_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
        p[SanctuaryLayout.LOG]=GoogologyBlocks.DREAD_LOG.defaultBlockState();p[SanctuaryLayout.YARN]=GoogologyBlocks.TIANYI_FIBER.defaultBlockState();
        p[SanctuaryLayout.DARK]=Blocks.DEEPSLATE_TILES.defaultBlockState();p[SanctuaryLayout.METAL]=Blocks.IRON_BLOCK.defaultBlockState();
        p[SanctuaryLayout.DESK]=GoogologyBlocks.OFFICE_DESK.defaultBlockState();p[SanctuaryLayout.MONITOR]=GoogologyBlocks.OFFICE_MONITOR.defaultBlockState();p[SanctuaryLayout.SERVER]=GoogologyBlocks.SERVER_RACK.defaultBlockState();
        p[SanctuaryLayout.PATTERN]=GoogologyBlocks.BASIC_LAVER_PATTERN.defaultBlockState();p[SanctuaryLayout.EMOJI]=GoogologyBlocks.GUOGAO_LANTERN.defaultBlockState();
        p[SanctuaryLayout.WHITE]=GoogologyBlocks.ASTRA_LIGHT.defaultBlockState();
        p[SanctuaryLayout.GOLD]=(theme==SurvivalTheme.POWER?GoogologyBlocks.SUN_PATTERN_TILES:theme==SurvivalTheme.GUOGAO?GoogologyBlocks.STAR_GOLD:GoogologyBlocks.AMBER_INLAY).defaultBlockState();
        p[SanctuaryLayout.CYAN]=Blocks.SEA_LANTERN.defaultBlockState();p[SanctuaryLayout.VIOLET]=Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
        p[SanctuaryLayout.TEAL]=GoogologyBlocks.ASTRA_MINT.defaultBlockState();p[SanctuaryLayout.INK]=Blocks.BLACK_CONCRETE.defaultBlockState();
        p[SanctuaryLayout.RED]=GoogologyBlocks.TREE_NODE_RED.defaultBlockState();p[SanctuaryLayout.GREEN]=GoogologyBlocks.TREE_NODE_GREEN.defaultBlockState();p[SanctuaryLayout.BLUE]=GoogologyBlocks.TREE_NODE_BLUE.defaultBlockState();
        p[SanctuaryLayout.EMOJI_CYAN]=GoogologyBlocks.CYAN_LANTERN.defaultBlockState();p[SanctuaryLayout.EMOJI_ROSE]=GoogologyBlocks.ROSE_LANTERN.defaultBlockState();
        p[SanctuaryLayout.EMOJI_VIOLET]=GoogologyBlocks.VIOLET_LANTERN.defaultBlockState();p[SanctuaryLayout.EMOJI_LIME]=GoogologyBlocks.LIME_LANTERN.defaultBlockState();
        p[SanctuaryLayout.PSI]=GoogologyBlocks.PSI_SYMBOL.defaultBlockState();p[SanctuaryLayout.OMEGA]=GoogologyBlocks.GREAT_OMEGA_SYMBOL.defaultBlockState();p[SanctuaryLayout.ZED]=GoogologyBlocks.LHO_HYDRA_Z.defaultBlockState();
        p[SanctuaryLayout.FIBER_WHITE]=GoogologyBlocks.WHITE_FIBER.defaultBlockState();p[SanctuaryLayout.CLOUD]=GoogologyBlocks.ASTRA_CLOUD.defaultBlockState();p[SanctuaryLayout.CLOUD_SHADE]=GoogologyBlocks.ASTRA_CLOUD_SHADE.defaultBlockState();
        p[SanctuaryLayout.LAVER]=GoogologyBlocks.GIANT_LAVER.defaultBlockState();p[SanctuaryLayout.IBLP_NODE]=GoogologyBlocks.IBLP_NODE.defaultBlockState();p[SanctuaryLayout.IBLP_MARKED]=GoogologyBlocks.IBLP_MARKED.defaultBlockState();p[SanctuaryLayout.IBLP_BLANK]=GoogologyBlocks.IBLP_BLANK.defaultBlockState();
        p[SanctuaryLayout.ARROW]=GoogologyBlocks.POWER_BRICKS.defaultBlockState();p[SanctuaryLayout.Y_WOOD]=GoogologyBlocks.Y_LOG.defaultBlockState();p[SanctuaryLayout.Y_LEAF]=GoogologyBlocks.Y_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true);
        for(int n=0;n<16;n++)p[SanctuaryLayout.FIXED_NUMBER+n]=GoogologyBlocks.ordinalBrick(n);
        p[SanctuaryLayout.SHELF]=(theme==SurvivalTheme.ASTRA?GoogologyBlocks.SERVER_RACK:Blocks.BOOKSHELF).defaultBlockState();
        p[SanctuaryLayout.SLAB]=(theme==SurvivalTheme.ASTRA?Blocks.SMOOTH_QUARTZ_SLAB:theme==SurvivalTheme.GUOGAO?Blocks.POLISHED_BLACKSTONE_SLAB:theme==SurvivalTheme.WEAVER?Blocks.DARK_OAK_SLAB:Blocks.SMOOTH_STONE_SLAB).defaultBlockState();
        p[SanctuaryLayout.CHAIN]=Blocks.IRON_CHAIN.defaultBlockState();p[SanctuaryLayout.LANTERN]=(theme==SurvivalTheme.GUOGAO?Blocks.SOUL_LANTERN:Blocks.LANTERN).defaultBlockState();
        p[SanctuaryLayout.CRYSTAL]=GoogologyBlocks.ORDINAL_CRYSTAL.defaultBlockState();p[SanctuaryLayout.BLOOM]=GoogologyBlocks.EPSILON_BLOOM.defaultBlockState();
        p[SanctuaryLayout.WORKSTATION]=(theme==SurvivalTheme.WEAVER?Blocks.LOOM:Blocks.CRAFTING_TABLE).defaultBlockState();
        p[SanctuaryLayout.PANE]=Blocks.GLASS_PANE.defaultBlockState();p[SanctuaryLayout.SMALL_FLOWER]=GoogologyBlocks.EMOJI_FLOWER.defaultBlockState();p[SanctuaryLayout.FRUIT]=GoogologyBlocks.JELLIES[0].defaultBlockState();
        p[SanctuaryLayout.PHANTOM_PSI]=GoogologyBlocks.LHO_HYDRA_PSI.defaultBlockState();
        p[SanctuaryLayout.PHANTOM_Z]=GoogologyBlocks.LHO_HYDRA_Z.defaultBlockState();
        p[SanctuaryLayout.PHANTOM_FOS]=GoogologyBlocks.FOS_TRACE.defaultBlockState();
        p[SanctuaryLayout.PHANTOM_FFFZ]=GoogologyBlocks.FFFZ_TRACE.defaultBlockState();
        p[SanctuaryLayout.PHANTOM_EMPTY]=GoogologyBlocks.LHO_TRACE.defaultBlockState();
        p[SanctuaryLayout.BOUNDARY_CORE]=GoogologyBlocks.BOUNDARY_CORE.defaultBlockState();
        p[SanctuaryLayout.TURING]=GoogologyBlocks.TURING_TAPE.defaultBlockState();
        p[SanctuaryLayout.TAPE_BUTTON]=dev.googology.block.TuringTapeBlock.startButton();
        p[SanctuaryLayout.SET_GLASS]=GoogologyBlocks.SET_GLASS.defaultBlockState();
        p[SanctuaryLayout.FORMULA]=GoogologyBlocks.FORMULA_STONE.defaultBlockState();
        p[SanctuaryLayout.PROOF]=GoogologyBlocks.PROOF_STONE.defaultBlockState();
        p[SanctuaryLayout.SOIL]=(theme==SurvivalTheme.GUOGAO?GoogologyBlocks.GUOGAO_LOAM:GoogologyBlocks.EPSILON_TURF).defaultBlockState();
        for(int color=0;color<16;color++)p[SanctuaryLayout.PIXEL+color]=GoogologyBlocks.MOSAIC_LIGHT.defaultBlockState().setValue(dev.googology.block.MosaicLightBlock.COLOR,color);
        p[SanctuaryLayout.RELIC_INLAY]=GoogologyBlocks.SANCTUARY_RELICS[theme.ordinal()].defaultBlockState();
        p[SanctuaryLayout.SPAR]=net.minecraft.world.level.block.Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        p[SanctuaryLayout.SCG_SHELL]=net.minecraft.world.level.block.Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        p[SanctuaryLayout.SCG_EDGE]=Blocks.POLISHED_ANDESITE.defaultBlockState();
        p[SanctuaryLayout.TREE_SHELL_RED]=Blocks.RED_STAINED_GLASS.defaultBlockState();
        p[SanctuaryLayout.TREE_SHELL_GREEN]=Blocks.GREEN_STAINED_GLASS.defaultBlockState();
        p[SanctuaryLayout.TREE_SHELL_BLUE]=Blocks.BLUE_STAINED_GLASS.defaultBlockState();
        p[SanctuaryLayout.LAVER_CORE]=GoogologyBlocks.LAVER_CORE.defaultBlockState();
        p[SanctuaryLayout.COURT_NODE]=GoogologyBlocks.LAVER_COURT_NODE.defaultBlockState();
        p[SanctuaryLayout.COURT_BLANK]=GoogologyBlocks.LAVER_COURT_BLANK.defaultBlockState();
        p[SanctuaryLayout.LAVER_INLAY]=GoogologyBlocks.LAVER_INLAY.defaultBlockState();
        p[SanctuaryLayout.SEQUENCE_CORE]=GoogologyBlocks.SEQUENCE_CORE.defaultBlockState();
        p[SanctuaryLayout.HYDRA_BUD]=GoogologyBlocks.HYDRA_BUD.defaultBlockState();
        p[SanctuaryLayout.ASTRA_CORE]=GoogologyBlocks.ASTRA_CRITICAL_CORE.defaultBlockState();
        p[SanctuaryLayout.GUOGAO_HEART]=GoogologyBlocks.GUOGAO_HEART.defaultBlockState();
        p[SanctuaryLayout.POWER_CORE]=GoogologyBlocks.POWER_TOWER_CORE.defaultBlockState();
        p[SanctuaryLayout.LHO_L]=GoogologyBlocks.LHO_LETTER_L.defaultBlockState();
        p[SanctuaryLayout.LHO_H]=GoogologyBlocks.LHO_LETTER_H.defaultBlockState();
        p[SanctuaryLayout.LHO_O]=GoogologyBlocks.LHO_LETTER_O.defaultBlockState();
        p[SanctuaryLayout.RELIEF_BODY]=Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
        p[SanctuaryLayout.RELIEF_EDGE]=Blocks.DARK_PRISMARINE.defaultBlockState();
        p[SanctuaryLayout.RELIEF_RECESS]=Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        p[SanctuaryLayout.RELIEF_TEAR]=Blocks.CYAN_TERRACOTTA.defaultBlockState();
        if(theme==SurvivalTheme.HYDRA){
            p[SanctuaryLayout.SYMBOL]=Blocks.DARK_PRISMARINE.defaultBlockState();
            p[SanctuaryLayout.PSI]=Blocks.DARK_PRISMARINE.defaultBlockState();
            p[SanctuaryLayout.OMEGA]=Blocks.SMOOTH_QUARTZ.defaultBlockState();
            p[SanctuaryLayout.SPAR]=net.minecraft.world.level.block.Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        }
        if(theme==SurvivalTheme.POWER)p[SanctuaryLayout.SPAR]=GoogologyBlocks.SANCTUARY_RELICS[theme.ordinal()].defaultBlockState();
        if(theme==SurvivalTheme.POWER){
            p[SanctuaryLayout.TRIM]=Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
            p[SanctuaryLayout.LOG]=Blocks.DARK_OAK_LOG.defaultBlockState();
            p[SanctuaryLayout.FLOOR]=Blocks.DARK_OAK_PLANKS.defaultBlockState();
            p[SanctuaryLayout.GLASS]=Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
            p[SanctuaryLayout.WHITE]=Blocks.SMOOTH_SANDSTONE.defaultBlockState();
            p[SanctuaryLayout.SLAB]=Blocks.DARK_OAK_SLAB.defaultBlockState();
        }
        if(theme==SurvivalTheme.ABSENCE){
            p[SanctuaryLayout.WHITE]=Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            p[SanctuaryLayout.SLAB]=Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            p[SanctuaryLayout.DARK]=Blocks.CYAN_STAINED_GLASS.defaultBlockState();
            p[SanctuaryLayout.METAL]=Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        }
        if(theme==SurvivalTheme.FRONTIER){
            p[SanctuaryLayout.WHITE]=GoogologyBlocks.LOGIC_IVORY.defaultBlockState();
            p[SanctuaryLayout.DARK]=Blocks.BLACK_CONCRETE.defaultBlockState();
            p[SanctuaryLayout.GLASS]=GoogologyBlocks.SET_GLASS.defaultBlockState();
            p[SanctuaryLayout.VIOLET]=GoogologyBlocks.RAYO_ROSE.defaultBlockState();
            p[SanctuaryLayout.TEAL]=GoogologyBlocks.SET_JADE.defaultBlockState();
            p[SanctuaryLayout.SPAR]=GoogologyBlocks.LIMIT_LAMINA.defaultBlockState();
        }
        return p;
    }
}
