package dev.guogaology.world;

import java.util.BitSet;

/** Buried raw pearls, sampled once per voxel from the actual cloud union. */
public final class CloudPearls {
    private CloudPearls() {}

    public static void draw(VoxelBrush target,int minX,int maxX,int minZ,int maxZ,
                            int cx,int base,int cz,long salt) {
        int radius=NaturalForms.cloudWidth(salt)/2+2,height=NaturalForms.cloudHeight(salt);
        int xa=Math.max(minX,cx-radius),xb=Math.min(maxX,cx+radius);
        int za=Math.max(minZ,cz-radius),zb=Math.min(maxZ,cz+radius);
        if(xa>xb||za>zb)return;
        // One-block halo includes diagonals: a pearl cannot occupy a surface,
        // exposed edge or corner. Only this clipped tile is rasterized, never a
        // world/chunk lookup, and the halo is identical across chunk boundaries.
        int sx=xb-xa+3,sz=zb-za+3,sy=height+19,y0=base-9;
        var cloud=new BitSet(sx*sz*sy);
        NaturalForms.cloud(new VoxelBrush(xa-1,xb+1,y0,y0+sy-1,za-1,zb+1,
                (x,y,z,m)->cloud.set(((x-xa+1)*sz+z-za+1)*sy+y-y0)),cx,base,cz,salt);
        for(int x=xa;x<=xb;x++)for(int z=za;z<=zb;z++)for(int y=y0+1;y<y0+sy-1;y++) {
            int index=((x-xa+1)*sz+z-za+1)*sy+y-y0;
            if(!cloud.get(index)||WorldNoise.unit(WorldNoise.mix(salt^((long)x*73428767)^((long)y*912931)^((long)z*43828931)))>=.01)continue;
            boolean buried=true;
            for(int dx=-1;dx<=1&&buried;dx++)for(int dz=-1;dz<=1&&buried;dz++)for(int dy=-1;dy<=1;dy++)
                if(!cloud.get(index+(dx*sz+dz)*sy+dy)){buried=false;break;}
            if(buried)target.set(x,y,z,NaturalForms.CLOUD_PEARL);
        }
    }
}
