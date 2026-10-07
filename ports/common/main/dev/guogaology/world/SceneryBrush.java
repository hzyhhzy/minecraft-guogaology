package dev.guogaology.world;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

/** Absolute-coordinate geometry, strictly clipped to the chunk currently being decorated. */
public final class SceneryBrush {
    @FunctionalInterface public interface Sink { void set(int x,int y,int z,BlockState state); }
    public final int minX,maxX,minZ,maxZ;
    private final Sink sink;
    public SceneryBrush(ChunkPos c,Sink sink) { this(c.getMinBlockX(),c.getMaxBlockX(),c.getMinBlockZ(),c.getMaxBlockZ(),sink); }
    public SceneryBrush(int minX,int maxX,int minZ,int maxZ,Sink sink){this.minX=minX;this.maxX=maxX;this.minZ=minZ;this.maxZ=maxZ;this.sink=sink;}
    public boolean intersects(int x,int z,int radius) { return minX<=x+radius && maxX>=x-radius && minZ<=z+radius && maxZ>=z-radius; }
    public void set(int x,int y,int z,BlockState state) { if(x>=minX&&x<=maxX&&z>=minZ&&z<=maxZ&&y>-63&&y<318) sink.set(x,y,z,state.getBlock()==dev.guogaology.GuogaologyBlocks.ORDINAL_STONE?dev.guogaology.block.NumberStoneBlock.natural(x,y,z):state); }
    public void box(int x1,int y1,int z1,int x2,int y2,int z2,BlockState state) {
        for(int x=Math.max(minX,x1);x<=Math.min(maxX,x2);x++) for(int z=Math.max(minZ,z1);z<=Math.min(maxZ,z2);z++)
            for(int y=Math.max(-62,y1);y<=Math.min(317,y2);y++) sink.set(x,y,z,state.getBlock()==dev.guogaology.GuogaologyBlocks.ORDINAL_STONE?dev.guogaology.block.NumberStoneBlock.natural(x,y,z):state);
    }
    public void disc(int cx,int cy,int cz,int radius,BlockState state) {
        for(int x=Math.max(minX,cx-radius);x<=Math.min(maxX,cx+radius);x++) for(int z=Math.max(minZ,cz-radius);z<=Math.min(maxZ,cz+radius);z++)
            if((x-cx)*(x-cx)+(z-cz)*(z-cz)<=radius*radius) set(x,cy,z,state);
    }
    public void ellipsoid(double cx,double cy,double cz,double rx,double ry,double rz,BlockState state) {
        for(int x=Math.max(minX,(int)Math.floor(cx-rx));x<=Math.min(maxX,(int)Math.ceil(cx+rx));x++)
            for(int z=Math.max(minZ,(int)Math.floor(cz-rz));z<=Math.min(maxZ,(int)Math.ceil(cz+rz));z++) {
                double q=1-Math.pow((x-cx)/rx,2)-Math.pow((z-cz)/rz,2);if(q<0) continue;
                double h=ry*Math.sqrt(q);box(x,(int)Math.ceil(cy-h),z,x,(int)Math.floor(cy+h),z,state);
            }
    }
    public void line(double x1,double y1,double z1,double x2,double y2,double z2,int radius,BlockState state) {
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(y2-y1),Math.max(Math.abs(x2-x1),Math.abs(z2-z1)))*1.7));
        for(int i=0;i<=steps;i++) { double t=i/(double)steps;int x=(int)Math.round(x1+(x2-x1)*t),y=(int)Math.round(y1+(y2-y1)*t),z=(int)Math.round(z1+(z2-z1)*t);box(x-radius,y-radius,z-radius,x+radius,y+radius,z+radius,state); }
    }
}
