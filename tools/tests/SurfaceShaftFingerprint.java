import dev.guogaology.outer.world.SurfaceShaftField;
import net.minecraft.world.level.ChunkPos;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Run against either reviewed 0.5.4.1 bytecode or the reconstructed class.
 * Does not initialize Minecraft, a server, a client or any world.
 */
public final class SurfaceShaftFingerprint {
    public static void main(String[] args) throws Exception {
        var digest=MessageDigest.getInstance("SHA-256");
        var bytes=ByteBuffer.allocate(16);long[] writes={0};int shafts=0;
        for(long seed:new long[]{0,541,-541,Long.MAX_VALUE,Long.MIN_VALUE}) {
            for(int mode=0;mode<4;mode++) {
                final int scenario=mode;
                var probe=new SurfaceShaftField.ShaftProbe(){
                    public boolean hasData(int x,int z){return scenario!=1||(x&3)!=0;}
                    public int top(int x,int z){return scenario==1&&((x+z)&7)==0?Integer.MIN_VALUE:scenario==2?-64:63+Math.floorMod(x*17+z*11,140);}
                    public boolean isVoidOrEdge(int x,int y,int z){return scenario==1&&Math.floorMod(x+z,5)==0;}
                    public boolean isFluid(int x,int y,int z){return scenario==3||(x&3)==0;}
                };
                for(int x=-48;x<=48;x++)for(int z=-48;z<=48;z++) {
                    var chunk=new ChunkPos(x,z);int min=-64,max=scenario==2?-65:319;
                    shafts+=SurfaceShaftField.apply(seed,chunk,min,max,probe,(bx,by,bz,type)->{
                        if(bx<chunk.getMinBlockX()||bx>chunk.getMinBlockX()+15||bz<chunk.getMinBlockZ()||bz>chunk.getMinBlockZ()+15||by<min||by>max)
                            throw new AssertionError("write outside available chunk/height");
                        bytes.clear();bytes.putInt(bx).putInt(by).putInt(bz).putInt(type);digest.update(bytes.array());writes[0]++;
                    },i->i,i->10+i);
                }
            }
        }
        if(shafts<100||writes[0]<1_000_000)throw new AssertionError("insufficient sample");
        System.out.println("SHAFT_FINGERPRINT shafts="+shafts+" writes="+writes[0]+" sha256="+HexFormat.of().formatHex(digest.digest()));
    }
}
