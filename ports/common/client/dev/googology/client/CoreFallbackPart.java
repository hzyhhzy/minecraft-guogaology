package dev.googology.client;

import com.google.gson.JsonObject;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import java.util.List;

/** Static, reload-time vanilla quads for Voxy and other non-FRAPI consumers.
 * Nearby chunk emission still contains only the fixed shell, so this does not
 * duplicate the animated interior or introduce a Voxy dependency. */
final class CoreFallbackPart implements BlockModelPart {
    private final List<BakedQuad> quads;
    private final TextureAtlasSprite particle;
    CoreFallbackPart(List<BakedQuad> quads,TextureAtlasSprite particle){this.quads=quads;this.particle=particle;}
    @Override public List<BakedQuad> getQuads(Direction face){return face==null?quads:List.of();}
    @Override public boolean useAmbientOcclusion(){return false;}
    @Override public TextureAtlasSprite particleIcon(){return particle;}
    static double radiusSquared(JsonObject q){
        double radius=0;for(int axis=0;axis<3;axis++){
            double center=0;for(var vertex:q.getAsJsonArray("v"))center+=vertex.getAsJsonArray().get(axis).getAsDouble()/4;
            radius+=(center-8)*(center-8);
        }return radius;
    }
    static BakedQuad bake(JsonObject q,TextureAtlasSprite sprite){
        Vector3f[] v=new Vector3f[4];long[] uv=new long[4];float nx=0,ny=0,nz=0;
        var coordinates=q.getAsJsonArray(q.has("lod_uv")?"lod_uv":"uv");
        for(int i=0;i<4;i++){
            var p=q.getAsJsonArray("v").get(i).getAsJsonArray();
            // Outer ornaments are omitted by the caller. Limit any slight
            // interior overhang to Voxy's single-cell representation.
            v[i]=new Vector3f(clamp(p.get(0).getAsFloat()/16),clamp(p.get(1).getAsFloat()/16),clamp(p.get(2).getAsFloat()/16));
            var tex=coordinates.get(i).getAsJsonArray();uv[i]=UVPair.pack(sprite.getU(tex.get(0).getAsFloat()),sprite.getV(tex.get(1).getAsFloat()));
            var n=q.getAsJsonArray("n").get(i).getAsJsonArray();nx+=n.get(0).getAsFloat();ny+=n.get(1).getAsFloat();nz+=n.get(2).getAsFloat();
        }
        var face=Math.abs(nx)>=Math.abs(ny)&&Math.abs(nx)>=Math.abs(nz)?(nx<0?Direction.WEST:Direction.EAST):Math.abs(ny)>=Math.abs(nz)?(ny<0?Direction.DOWN:Direction.UP):(nz<0?Direction.NORTH:Direction.SOUTH);
        return new BakedQuad(v[0],v[1],v[2],v[3],uv[0],uv[1],uv[2],uv[3],-1,face,sprite,false,0);
    }
    private static float clamp(float value){return Math.clamp(value,0f,1f);}
}
