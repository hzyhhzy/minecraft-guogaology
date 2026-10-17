package dev.guogaology.mergeqa;

import dev.guogaology.*;
import dev.guogaology.ambience.*;
import dev.guogaology.block.EmojiLanternBlock;
import dev.guogaology.survival.*;
import dev.guogaology.world.*;
import java.util.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;

/** Actual production painters/readers/callbacks, confined to a disposable QA save. */
public final class Laver511Checks {
    private static int checks;
    private static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    private static final Block[] CELLS={GuogaologyBlocks.IBLP_BLANK,GuogaologyBlocks.IBLP_NODE,GuogaologyBlocks.IBLP_MARKED};
    private static InteractionResult click(ServerPlayer p,BlockPos at){return UseBlockCallback.EVENT.invoker().interact(p,p.level(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at),Direction.UP,at,false));}
    private static void row(ServerLevel w,BlockPos at,Direction direction,int digit,int... kinds){
        w.setBlock(at,GuogaologyBlocks.LAVER_INLAY.defaultBlockState(),2);
        for(int i=0;i<kinds.length;i++)w.setBlock(at.relative(direction,i+1),CELLS[kinds[i]].defaultBlockState(),2);
        w.setBlock(at.relative(direction,kinds.length+1),GuogaologyBlocks.ordinalBrick(digit),2);
    }
    private static void clear(ServerLevel w,BlockPos at){for(var direction:Direction.values())for(int d=0;d<=12;d++)w.setBlock(at.relative(direction,d),Blocks.AIR.defaultBlockState(),2);}
    public static void run(ServerPlayer p)throws Exception{
        checks=0;var w=p.level();var at=new BlockPos(8,240,8);var old=p.position();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)w.getChunk(x,z);
        p.setPos(Vec3.atCenterOf(at));p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.setShiftKeyDown(false);
        try{
            for(var direction:Direction.values()){
                clear(w,at);row(w,at,direction,7,1,2,0,1);
                var table=LiveLaverTable.read(w,at);check(table!=null&&table.notes().size()==4&&table.end().equals(at.relative(direction,5)),"all six cardinal directions: "+direction);
                check(table.duration()==20&&table.notes().get(1).marked()&&table.notes().get(2).semitone()==-1,"marked notes and real blank rests");
                for(int i=0;i<4;i++)check(table.notes().get(i).tick()==i*5,"quarter-second cell offsets");
                check(table.valid(w),"fresh row is valid");
                w.setBlock(at.relative(direction,2),Blocks.STONE.defaultBlockState(),2);check(!table.valid(w)&&LiveLaverTable.read(w,at)==null,"foreign block breaks continuity");
            }
            clear(w,at);w.setBlock(at,GuogaologyBlocks.LAVER_INLAY.defaultBlockState(),2);check(LiveLaverTable.read(w,at)==null,"isolated inlay does not count");
            row(w,at,Direction.EAST,1,1,1,2,0,1);row(w,at,Direction.WEST,3,1,2,1);
            for(int d=1;d<=8;d++)w.setBlock(at.above(d),CELLS[1].defaultBlockState(),2);
            var longest=LiveLaverTable.read(w,at);check(longest!=null&&longest.end().equals(at.east(6)),"longest valid row wins over shorter row and longer unterminated arm");
            w.setBlock(longest.end(),GuogaologyBlocks.ordinalBrick(4),2);var changed=LiveLaverTable.read(w,at);
            check(!longest.valid(w)&&!longest.notes().stream().map(LiveLaverTable.Note::semitone).toList().equals(changed.notes().stream().map(LiveLaverTable.Note::semitone).toList()),"number edits invalidate old tune and audibly alter reread");
            clear(w,at);row(w,at,Direction.EAST,5,1,0,2);w.setBlock(at.east(2),Blocks.AIR.defaultBlockState(),2);check(LiveLaverTable.read(w,at)==null,"air is not a blank rest");
            for(int digit=0;digit<16;digit++){
                var score=LaverScore.row(new int[]{1,2,0,1,1,2},digit);check(score.duration()==30&&score.notes().size()==6,"all matrix digits accepted");
                check(score.notes().get(2).semitone()==-1&&score.notes().stream().filter(n->n.semitone()>=0).allMatch(n->n.semitone()<=24),"all digits stay audible; rest remains silent");
            }
            generatedTables(w);
            courtTables();
            hollowLamps();
            // A loaded edge must not synchronously load the next chunk while searching.
            var edge=new BlockPos(5134,240,5128);var chunk=w.getChunkAt(edge);
            // Set only this QA chunk: Level.setBlock also runs lighting/neighbor hooks,
            // which can make an edge fixture load its own neighbor before the reader runs.
            chunk.setBlockState(edge,GuogaologyBlocks.LAVER_INLAY.defaultBlockState(),0);chunk.setBlockState(edge.east(),CELLS[1].defaultBlockState(),0);
            check(w.getChunkSource().getChunkNow(320,320)!=null,"the scanned source chunk is actually FULL");
            check(w.getChunkSource().getChunkNow(321,320)==null,"neighbor is actually not FULL after constructing the fixture");
            check(LiveLaverTable.read(w,edge)==null&&w.getChunkSource().getChunkNow(321,320)==null,"unfinished row at chunk edge requests no adjacent FULL chunk");
            chunk.setBlockState(edge.west(),CELLS[1].defaultBlockState(),0);chunk.setBlockState(edge.west(2),GuogaologyBlocks.ordinalBrick(1),0);
            check(LiveLaverTable.read(w,edge)==null&&w.getChunkSource().getChunkNow(321,320)==null,"even a shorter loaded candidate is declined when scanning involves an unloaded arm");
            chunk.setBlockState(edge.west(),Blocks.AIR.defaultBlockState(),0);chunk.setBlockState(edge.west(2),Blocks.AIR.defaultBlockState(),0);
            chunk.setBlockState(edge,Blocks.AIR.defaultBlockState(),0);chunk.setBlockState(edge.east(),Blocks.AIR.defaultBlockState(),0);
            clear(w,at);row(w,at,Direction.EAST,1,1,2,0,1);
            check(click(p,at.east())==InteractionResult.PASS&&LaverMusic.activeCount(w)==0,"grid right-click does not play");
            p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.APPLE));p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE));
            check(click(p,at)==InteractionResult.SUCCESS&&LaverMusic.activeCount(w)==1,"occupied main hand and offhand can play");
            check(click(p,at)==InteractionResult.SUCCESS&&LaverMusic.activeCount(w)==0,"occupied hands can stop playback");
            p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            p.setShiftKeyDown(true);check(click(p,at)==InteractionResult.PASS,"sneaking preserves ordinary interaction");p.setShiftKeyDown(false);
            check(click(p,at)==InteractionResult.SUCCESS&&LaverMusic.activeCount(w)==1,"actual callback starts a core-free row and grants the guide");
            check(click(p,at)==InteractionResult.SUCCESS&&LaverMusic.activeCount(w)==0,"same inlay stops its performance");
            click(p,at);w.setBlock(at.east(2),Blocks.AIR.defaultBlockState(),2);
            var tick=LaverMusic.class.getDeclaredMethod("tick",ServerLevel.class);tick.setAccessible(true);tick.invoke(null,w);check(LaverMusic.activeCount(w)==0,"broken performing row cancels on actual server tick");
        }finally{clear(w,at);p.setPos(old);p.setShiftKeyDown(false);}
        System.out.println("LAVER511_SERVER_OK checks="+checks+" sixDirections/longestRow/liveEdits/noChunkLoad/nativeClick/generator16Orientations/court4Preserved/quarterSecond/hollowLamp12");
    }
    private static Map<BlockPos,BlockState> paint(NaturalScenery.Table table)throws Exception{
        var blocks=new LinkedHashMap<BlockPos,BlockState>();var brush=new SceneryBrush(-128,128,-128,128,(x,y,z,state)->blocks.put(new BlockPos(x,y,z),state));
        var method=NaturalScenery.class.getDeclaredMethod("table",SceneryBrush.class,NaturalScenery.Table.class);method.setAccessible(true);method.invoke(null,brush,table);return blocks;
    }
    private static NaturalScenery.Table example(int turn,boolean mirror,boolean vertical){
        var pattern=NotationCatalog.allIblp().stream().filter(t->t.size()==6).findFirst().orElseThrow();
        return new NaturalScenery.Table(new NaturalScenery.Site(511,1234,0,0,4,210,Integer.MIN_VALUE,false),pattern,222,turn,vertical,mirror);
    }
    private static void generatedTables(ServerLevel w)throws Exception{
        for(int turn=0;turn<4;turn++)for(boolean mirror:new boolean[]{false,true})for(boolean vertical:new boolean[]{false,true}){
            var t=example(turn,mirror,vertical);var blocks=paint(t);int n=t.pattern().size();var saved=new LinkedHashMap<BlockPos,BlockState>();
            check(blocks.values().stream().noneMatch(s->s.is(GuogaologyBlocks.LAVER_CORE)),"new natural tables contain no finished core");
            for(int col=-1;col<=1;col++)check(!blocks.containsKey(NaturalScenery.tableCell(t,0,col)),"old apex row really removed");
            for(int[] leg:LaverTableShape.legs(n))check(leg[0]>=1&&leg[0]<n&&leg[1]>=0&&leg[1]<=leg[0],"three supports are inset within real diagram");
            try{
                for(var e:blocks.entrySet()){var pos=e.getKey();w.getChunk(pos);saved.put(pos,w.getBlockState(pos));w.setBlock(pos,e.getValue(),2);}
                for(int r=1;r<=n;r++){
                    var inlay=NaturalScenery.tableCell(t,r,-1);var read=LiveLaverTable.read(w,inlay);
                    check(read!=null&&read.notes().size()==r+1&&read.end().equals(NaturalScenery.tableCell(t,r,r+1)),"actual generated row is playable, turn="+turn+" mirror="+mirror+" vertical="+vertical+" row="+r);
                    check(read.step()==t.pattern().steps()[r-1],"original notation step retained");
                    for(int c=0;c<=r;c++)check(read.notes().get(c).kind()==t.pattern().cell(r,c),"actual pattern data and marks retained");
                }
            }finally{for(var e:saved.entrySet())w.setBlock(e.getKey(),e.getValue(),2);}
        }
    }
    private static void courtTables(){
        var layout=SanctuaryLayout.of(SurvivalTheme.WEAVER);int tables=0,cores=0;
        for(var f:layout.furnishings())if(f.kind().startsWith("playable_iblp_")){
            tables++;var pattern=Arrays.stream(CourtLaverPatterns.ALL).filter(a->f.kind().equals("playable_iblp_"+a.name())).findFirst().orElseThrow();int n=pattern.rows().length,o=n/2;
            for(int r=0;r<=n;r++)for(int c=-1;c<=r+1;c++){
                int x=f.floor().x()+c-o,y=f.floor().y()+3,z=f.floor().z()+r-o;byte material=layout.at(x,y,z);int expected=SanctuaryLayout.LAVER_INLAY;
                if(c>=0&&c<=r)expected=r==0?SanctuaryLayout.LAVER_CORE:Arrays.binarySearch(pattern.rows()[r-1],c)<0?SanctuaryLayout.COURT_BLANK:Arrays.binarySearch(pattern.marks()[r-1],c)>=0?SanctuaryLayout.LAVER_CORE:SanctuaryLayout.COURT_NODE;
                check(material==expected,"palace diagrams, core apex and core marks stay unchanged");if(material==SanctuaryLayout.LAVER_CORE)cores++;
            }
        }
        check(tables==4&&cores==5,"all four original palace diagrams retain their five cores");check(layout.interiorResources().stream().filter(SanctuaryLayout.InteriorResource::regional).count()==8,"palace retains eight authored regional cores in original positions");
    }
    private static void hollowLamps()throws Exception{
        for(var lamp:GuogaologyBlocks.LANTERNS)for(int emotion=0;emotion<2;emotion++){
            int shell=0;for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++){
                var state=EmojiLanternBlock.tile(lamp,x,y,z,emotion);
                if(x==1&&y==1&&z==1)check(state.isAir(),"lamp center is air");
                else{shell++;check(state.is(lamp)&&state.getValue(EmojiLanternBlock.EMOTION)==emotion&&state.getValue(EmojiLanternBlock.PART)==x+3*y+9*z,"26 outer tiles keep color, expression and exact part");}
            }check(shell==26,"entire lamp shell remains");
        }
        var states=new HashMap<BlockPos,BlockState>();var brush=new SceneryBrush(0,2,0,2,(x,y,z,s)->states.put(new BlockPos(x,y,z),s));
        var draw=NaturalScenery.class.getDeclaredMethod("lantern",SceneryBrush.class,int.class,int.class,int.class,long.class);draw.setAccessible(true);draw.invoke(null,brush,0,220,0,42L);
        check(states.size()==27&&states.get(new BlockPos(1,221,1)).isAir(),"real generator explicitly carves center, rather than skipping it");
    }
    /** Actual production geometry for native screenshots and a real S2C musical performance. */
    public static void display(ServerPlayer p,int frame)throws Exception{
        var w=p.level().getServer().overworld();p.closeContainer();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.getInventory().clearContent();p.setShiftKeyDown(false);
        p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        for(var at:BlockPos.betweenClosed(new BlockPos(-14,219,-10),new BlockPos(14,232,12)))w.setBlock(at,at.getY()==219?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        Vec3 eye,target;
        if(frame<2){
            var table=example(0,false,frame==1);for(var e:paint(table).entrySet())if(e.getKey().getY()>=220)w.setBlock(e.getKey(),e.getValue(),2);
            w.setBlock(new BlockPos(7,220,1),Blocks.CALCITE.defaultBlockState(),2);w.setBlock(new BlockPos(7,221,1),CoreResources.CLOUD_PEARL.defaultBlockState(),2);
            for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++)w.setBlock(new BlockPos(x-10,y+221,z+3),EmojiLanternBlock.tile(GuogaologyBlocks.GUOGAO_LANTERN,x,y,z,0),2);
            eye=frame==0?new Vec3(11,232,16):new Vec3(10,229,17);target=new Vec3(0,frame==0?221:224,0);
            move(p,w,eye,target);
            var origin=NaturalScenery.tableCell(table,table.pattern().size(),-1);check(click(p,origin)==InteractionResult.SUCCESS,"native visible table performance starts");
        }else{
            w.setBlock(new BlockPos(0,220,0),Blocks.CALCITE.defaultBlockState(),2);w.setBlock(new BlockPos(0,221,0),CoreResources.CLOUD_PEARL.defaultBlockState(),2);
            eye=new Vec3(2.6,223,3.5);target=new Vec3(.5,221.5,.5);move(p,w,eye,target);
        }
    }
    private static void move(ServerPlayer p,ServerLevel w,Vec3 eye,Vec3 target){
        var delta=target.subtract(eye);float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
        p.teleport(new TeleportTransition(w,eye,Vec3.ZERO,yaw,pitch,TeleportTransition.DO_NOTHING));p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.getAbilities().flying=true;p.onUpdateAbilities();
    }
}
