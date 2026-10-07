import dev.guogaology.world.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Compiles unchanged against pre-update and current pure world-generation sources. */
public final class Underworld046Fingerprint {
    public static void main(String[] args)throws Exception{
        var inner=MessageDigest.getInstance("SHA-256");var lakes=MessageDigest.getInstance("SHA-256");
        int sites=0,samples=0;
        for(long seed:new long[]{123456789L,20261007L,-719311L}){
            for(int x=-1107;x<=1200;x+=173)for(int z=-1171;z<=1200;z+=197){
                var column=TerrainField.column(seed,x,z,false);
                for(int y=-56;y<=312;y+=8){inner.update(Double.toHexString(column.density(y)).getBytes(StandardCharsets.UTF_8));samples++;}
            }
            for(int gx=-12;gx<=12;gx++)for(int gz=-12;gz<=12;gz++){
                var lake=UnderworldLakes.lake(seed,gx,gz);lakes.update((gx+","+gz+":"+lake).getBytes(StandardCharsets.UTF_8));
                if(lake==null)continue;sites++;
                for(int dx:new int[]{-140,-70,0,70,140})for(int dz:new int[]{-140,-70,0,70,140}){
                    var c=UnderworldLakes.column(seed,lake.x()+dx,lake.z()+dz);
                    lakes.update(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        System.out.println("INNER "+samples+" "+HexFormat.of().formatHex(inner.digest()));
        System.out.println("LAKES "+sites+" "+HexFormat.of().formatHex(lakes.digest()));
    }
}
