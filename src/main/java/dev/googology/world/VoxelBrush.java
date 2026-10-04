package dev.googology.world;

/** Integer voxel rasterizer shared with the standalone geometry audit and preview. */
public final class VoxelBrush {
    @FunctionalInterface public interface Sink { void set(int x,int y,int z,int material); }
    private final int minX,maxX,minY,maxY,minZ,maxZ;
    private final Sink sink;
    public VoxelBrush(int minX,int maxX,int minY,int maxY,int minZ,int maxZ,Sink sink) {
        this.minX=minX;this.maxX=maxX;this.minY=minY;this.maxY=maxY;this.minZ=minZ;this.maxZ=maxZ;this.sink=sink;
    }
    public void set(int x,int y,int z,int material) {
        if(x>=minX&&x<=maxX&&y>=minY&&y<=maxY&&z>=minZ&&z<=maxZ) sink.set(x,y,z,material);
    }
    public void box(int x1,int y1,int z1,int x2,int y2,int z2,int material) {
        for(int x=Math.max(minX,x1);x<=Math.min(maxX,x2);x++)
            for(int z=Math.max(minZ,z1);z<=Math.min(maxZ,z2);z++)
                for(int y=Math.max(minY,y1);y<=Math.min(maxY,y2);y++) sink.set(x,y,z,material);
    }
    public void ellipsoid(double cx,double cy,double cz,double rx,double ry,double rz,int material) {
        for(int x=Math.max(minX,(int)Math.floor(cx-rx));x<=Math.min(maxX,(int)Math.ceil(cx+rx));x++)
            for(int z=Math.max(minZ,(int)Math.floor(cz-rz));z<=Math.min(maxZ,(int)Math.ceil(cz+rz));z++) {
                double q=1-Math.pow((x-cx)/rx,2)-Math.pow((z-cz)/rz,2);
                if(q<0) continue;double h=ry*Math.sqrt(q);
                box(x,(int)Math.ceil(cy-h),z,x,(int)Math.floor(cy+h),z,material);
            }
    }
    public void line(double x1,double y1,double z1,double x2,double y2,double z2,int radius,int material) {
        double pad=radius+.5; // Endpoints are rounded to block centers, including across chunk borders.
        if(Math.max(x1,x2)+pad<minX||Math.min(x1,x2)-pad>maxX||Math.max(z1,z2)+pad<minZ||Math.min(z1,z2)-pad>maxZ) return;
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(y2-y1),Math.max(Math.abs(x2-x1),Math.abs(z2-z1)))*1.7));
        for(int i=0;i<=steps;i++) {
            double t=i/(double)steps;
            int x=(int)Math.round(x1+(x2-x1)*t),y=(int)Math.round(y1+(y2-y1)*t),z=(int)Math.round(z1+(z2-z1)*t);
            box(x-radius,y-radius,z-radius,x+radius,y+radius,z+radius,material);
        }
    }
    /** A circular tube with rounded ends. Distances use the unrounded centerline in every axis. */
    public void tube(double x1,double y1,double z1,double x2,double y2,double z2,double radius,int material) {
        int xa=Math.max(minX,(int)Math.ceil(Math.min(x1,x2)-radius)),xb=Math.min(maxX,(int)Math.floor(Math.max(x1,x2)+radius));
        int ya=Math.max(minY,(int)Math.ceil(Math.min(y1,y2)-radius)),yb=Math.min(maxY,(int)Math.floor(Math.max(y1,y2)+radius));
        int za=Math.max(minZ,(int)Math.ceil(Math.min(z1,z2)-radius)),zb=Math.min(maxZ,(int)Math.floor(Math.max(z1,z2)+radius));
        double dx=x2-x1,dy=y2-y1,dz=z2-z1,length2=dx*dx+dy*dy+dz*dz,r2=radius*radius;
        for(int x=xa;x<=xb;x++) for(int z=za;z<=zb;z++) for(int y=ya;y<=yb;y++) {
            double px=x-x1,py=y-y1,pz=z-z1;
            double t=length2<1e-12?0:Math.clamp((px*dx+py*dy+pz*dz)/length2,0,1);
            double ox=px-t*dx,oy=py-t*dy,oz=pz-t*dz;
            if(ox*ox+oy*oy+oz*oz<=r2) sink.set(x,y,z,material);
        }
    }
    public void cone(double cx,double cy,double cz,double ax,double ay,double az,double length,double radius,int material) {
        double norm=Math.sqrt(ax*ax+ay*ay+az*az);ax/=norm;ay/=norm;az/=norm;
        int x1=Math.max(minX,(int)Math.floor(Math.min(cx,cx+ax*length)-radius));
        int x2=Math.min(maxX,(int)Math.ceil(Math.max(cx,cx+ax*length)+radius));
        int z1=Math.max(minZ,(int)Math.floor(Math.min(cz,cz+az*length)-radius));
        int z2=Math.min(maxZ,(int)Math.ceil(Math.max(cz,cz+az*length)+radius));
        int y1=Math.max(minY,(int)Math.floor(Math.min(cy,cy+ay*length)-radius));
        int y2=Math.min(maxY,(int)Math.ceil(Math.max(cy,cy+ay*length)+radius));
        for(int x=x1;x<=x2;x++) for(int z=z1;z<=z2;z++) for(int y=y1;y<=y2;y++) {
            double dx=x-cx,dy=y-cy,dz=z-cz,t=dx*ax+dy*ay+dz*az;
            if(t<-.6||t>length) continue;
            double r=radius*(1-Math.max(0,t)/length);
            if(dx*dx+dy*dy+dz*dz-t*t<=r*r) sink.set(x,y,z,material);
        }
    }
}
