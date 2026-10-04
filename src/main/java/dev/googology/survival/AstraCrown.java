package dev.googology.survival;

import java.io.*;
import java.util.zip.GZIPInputStream;

/** The selected sixfold model, rotated upright once; front and back share one continuous tube. */
public final class AstraCrown {
    public static final int BASE=95;
    public static final int WIDTH,HEIGHT,DEPTH;
    private static final byte[] VOXELS;
    static {
        try(var raw=AstraCrown.class.getResourceAsStream("/data/googology/architecture/astra_crown.bin.gz")){
            if(raw==null)throw new IOException("Missing sixfold crown");
            try(var in=new DataInputStream(new GZIPInputStream(raw))){
                WIDTH=in.readInt();HEIGHT=in.readInt();DEPTH=in.readInt();
                if(WIDTH!=89||HEIGHT!=87||DEPTH!=49)throw new IOException("Wrong crown dimensions");
                VOXELS=in.readNBytes(WIDTH*HEIGHT*DEPTH);
                if(VOXELS.length!=WIDTH*HEIGHT*DEPTH)throw new EOFException("Truncated crown");
            }
        }catch(IOException e){throw new ExceptionInInitializerError(e);}
    }
    public static int colorAt(int x,int y,int z){
        int xx=x+WIDTH/2,yy=y-BASE,zz=z+DEPTH/2;
        if(xx<0||xx>=WIDTH||yy<0||yy>=HEIGHT||zz<0||zz>=DEPTH)return -1;
        return Byte.toUnsignedInt(VOXELS[(yy*DEPTH+zz)*WIDTH+xx])-2;
    }
    static void place(SanctuaryLayout p){
        for(int y=BASE;y<BASE+HEIGHT;y++)for(int z=-DEPTH/2;z<=DEPTH/2;z++)for(int x=-WIDTH/2;x<=WIDTH/2;x++)
            if(colorAt(x,y,z)>=0)p.set(x,y,z,SanctuaryLayout.CROWN);
    }
    private AstraCrown(){}
}
