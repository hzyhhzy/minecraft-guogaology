package dev.guogaology.mergeqa;

import com.google.gson.*;
import dev.guogaology.outer.world.LhoIslandField;
import java.nio.file.*;

/** Paired old/new rarity and exact native geometry rules; no normal world reads. */
public final class Stellar065Checks {
    private Stellar065Checks(){}
    public static void run()throws Exception {
        if(LhoIslandField.STELLAR_DENSITY_MULTIPLIER!=16||LhoIslandField.STELLAR_RARITY_ONE_IN!=1100)throw new AssertionError("Stellar rarity multiplier/divisor drift");
        long oldThreshold=Long.MAX_VALUE/1100,newThreshold=oldThreshold*16;
        long oldGate=0,newGate=0,oldStone=0,newStone=0,assertions=0;
        int perSeed=2_000_000;
        for(long seed:new long[]{65,516,20261020,9173,137,47013,9018721,-96543})for(int i=0;i<perSeed;i++){
            int x=(i%1024)-512,z=((i/1024)%1024)-512,y=-64+(int)Math.floorMod(i*17L+(i/1024)*31L,384L);
            long score=LhoIslandField.stellarRarityScore(seed,x,y,z);
            boolean was=score<oldThreshold,now=score<newThreshold;
            if(LhoIslandField.passesStellarRarity(seed,x,y,z)!=now||was&&!now)throw new AssertionError("Exact16-fold independent gate/old subset changed");
            assertions++;
            if(was)oldGate++;if(now)newGate++;
            if(now&&LhoIslandField.isStellarLocalMin(seed,x,y,z)){newStone++;if(was)oldStone++;}
        }
        double gateRatio=(double)newGate/oldGate,stoneRatio=(double)newStone/oldStone;
        if(Math.abs(gateRatio-16)>1||Math.abs(stoneRatio-16)>3.2)throw new AssertionError("Paired native Stellar density unreasonable: "+gateRatio+" / "+stoneRatio);
        var result=new JsonObject();result.addProperty("samplePositions",perSeed*8);result.addProperty("seeds",8);result.addProperty("exactGateAssertions",assertions);
        result.addProperty("oldGate",oldGate);result.addProperty("newGate",newGate);result.addProperty("gateRatio",gateRatio);
        result.addProperty("oldLocalMinStone",oldStone);result.addProperty("newLocalMinStone",newStone);result.addProperty("localMinStoneRatio",stoneRatio);
        result.addProperty("scope","Paired native rarity and unchanged local-minimum functions, before terrain/biome filtering; not a claim of surface blocks per km2.");
        Files.writeString(Path.of("stellar065-native.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
        System.out.println("STELLAR065_OK positions="+(perSeed*8)+" gateRatio="+gateRatio+" localMinRatio="+stoneRatio);
    }
}
