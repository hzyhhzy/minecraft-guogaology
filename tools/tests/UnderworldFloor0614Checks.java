import dev.guogaology.world.TerrainField;
import dev.guogaology.world.UnderworldFloor;
import dev.guogaology.world.WorldNoise;
import java.util.Arrays;

/** Pure floor contracts; the separate native QA verifies Minecraft's actual random factory and chunks. */
public final class UnderworldFloor0614Checks {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static final int MIN=-64,AIR=0,ROCK=1,BEDROCK=2,OTHER=3;
    private static final class Board {
        final int[][][] cells=new int[32][20][16];
        int writes;
        Board(){for(var column:cells)for(int d=8;d<20;d++)Arrays.fill(column[d],ROCK);}
        boolean air(int x,int y,int z){return cells[x+16][y-MIN][z+16]==AIR;}
        void set(int x,int y,int z,boolean bedrock){
            check(x>=-16&&x<16&&z>=-16&&z<0,"current chunks only");
            check(y>=MIN&&y<MIN+16,"no write reaches the guaranteed rock plane");
            cells[x+16][y-MIN][z+16]=bedrock?BEDROCK:ROCK;writes++;
        }
        UnderworldFloor.Result apply(int x,UnderworldFloor.RandomAt random){
            return UnderworldFloor.generate(x,-16,MIN,random,this::air,this::set);
        }
        int hash(){return Arrays.deepHashCode(cells);}
    }
    public static void main(String[] args){
        check(UnderworldFloor.RANDOM_NAME.equals("minecraft:bedrock_floor"),"native named factory");
        check(UnderworldFloor.BEDROCK_LAYERS==5&&UnderworldFloor.FOUNDATION_LIMIT==16,"bounded native footprint");
        for(int d=1;d<=4;d++){
            float boundary=(5-d)/5.0F;
            check(UnderworldFloor.bedrockAtDepth(d,Math.nextDown(boundary)),"just below native probability");
            check(!UnderworldFloor.bedrockAtDepth(d,boundary),"not below native probability");
        }
        check(UnderworldFloor.bedrockAtDepth(0,.999F),"entire bottom guaranteed");
        check(!UnderworldFloor.bedrockAtDepth(5,0),"no sixth bedrock layer");
        Board full=new Board();var fullResult=full.apply(-16,(x,y,z)->0);
        check(fullResult.bedrockPlacements()==5*256,"all five selected with zero draw");
        check(fullResult.rockPlacements()==3*256,"old detached air gap bridged");
        check(fullResult.unjoinedColumns()==0,"reaches the old real rock");
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int d=0;d<20;d++)
            check(full.cells[x][d][z]==(d<5?BEDROCK:ROCK),"closed rough foundation through old terrain");
        Board minimum=new Board();minimum.cells[0][1][0]=OTHER;minimum.cells[1][9][1]=OTHER;
        var minResult=minimum.apply(-16,(x,y,z)->.999F);
        check(minResult.bedrockPlacements()==256,"only bottom selected near one");
        check(minimum.cells[0][1][0]==OTHER,"unselected base non-air preserved");
        check(minimum.cells[1][9][1]==OTHER,"existing non-air above base preserved");
        for(int x=0;x<16;x++)for(int z=0;z<16;z++){
            check(minimum.cells[x][0][z]==BEDROCK,"complete bottom shield");
            for(int d=1;d<8;d++)check(minimum.cells[x][d][z]!=AIR,"no gap above bottom");
            for(int d=5;d<20;d++)check(minimum.cells[x][d][z]!=BEDROCK,"no high bedrock or ceiling");
        }
        Board empty=new Board();for(var column:empty.cells)for(var row:column)Arrays.fill(row,AIR);
        var emptyResult=empty.apply(-16,(x,y,z)->.999F);
        check(emptyResult.unjoinedColumns()==256,"custom empty generator reported rather than unbounded fill");
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)
            check(empty.cells[x][16][z]==AIR,"hard upper fill cap");
        UnderworldFloor.RandomAt positional=(x,y,z)->(float)WorldNoise.unit(WorldNoise.hash(12345,x,y,z));
        Board forward=new Board(),reverse=new Board();
        forward.apply(-16,positional);forward.apply(0,positional);
        reverse.apply(0,positional);reverse.apply(-16,positional);
        check(forward.hash()==reverse.hash(),"generation order cannot shift the positional mask");
        int before=forward.hash();forward.apply(-16,positional);forward.apply(0,positional);
        check(forward.hash()==before,"reapplying has no state drift");
        for(long seed:new long[]{0,123456789L,-89716931L})for(int i=0;i<128;i++){
            int x=Math.floorDiv((i%16)*137-960,4)*4,z=Math.floorDiv((i/16)*193-700,4)*4;
            var c=TerrainField.column(seed,x,z,true);
            check(c.density(-48)>=1-1e-10,"proved lower rock anchor survives terrain/lakes/caves");
            check(c.density(-48)==c.uncarvedDensity(-48),"caves cannot pierce anchor");
        }
        System.out.println("UNDERWORLD_FLOOR0614 checks="+checks+" maskOrderStable=true anchorColumns=384");
    }
}
