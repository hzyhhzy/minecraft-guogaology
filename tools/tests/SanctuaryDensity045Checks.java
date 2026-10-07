import dev.googology.survival.*;
import dev.googology.world.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Samples the exact production candidate selector, never chunks or saved worlds. */
public final class SanctuaryDensity045Checks {
    private static final double[] PREVIOUS={.598,.650,.595,.860,.560,.590,.405,.590};
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static String option(String[] args,String key,String fallback){for(String arg:args)if(arg.startsWith(key+"="))return arg.substring(key.length()+1);return fallback;}
    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    public static void main(String[] args)throws Exception{
        var flags=Arrays.asList(args);boolean verify=flags.contains("--verify"),holdout=flags.contains("--holdout"),biomes=flags.contains("--biomes");
        int radius=Integer.parseInt(option(args,"--radius","20")),spacing=SanctuaryPlacement.SPACING;
        Path output=Path.of(option(args,"--output","build/density-0405"));Files.createDirectories(output);
        String scale=option(args,"--inner-scale",""),explicitRates=option(args,"--rates","");double[] rates=null;
        if(!explicitRates.isEmpty())rates=Arrays.stream(explicitRates.split(",")).mapToDouble(Double::parseDouble).toArray();
        else if(!scale.isEmpty()){
            rates=PREVIOUS.clone();double inner=Double.parseDouble(scale),under=Double.parseDouble(option(args,"--under-scale",".45"));
            for(int i=0;i<rates.length;i++)rates[i]*=i==6?under:inner;
        }
        long[] seeds=holdout?new long[]{41871,-97231,642903}:new long[]{9317,20260930,-384121,42,9918273};
        double area=Math.pow(radius*2.0*spacing/1000,2);long[] allSites=new long[8];double[] areas32=new double[8],areas64=new double[8];
        long[] dimensionTotals=new long[2],minimumDistance2=new long[8];Arrays.fill(minimumDistance2,Long.MAX_VALUE);
        int differentClose=0,naturalLakes=0,emptyLakes=0;String split=holdout?"holdout":"calibration";
        var rows=new StringBuilder("split,seed,dimension,sites,area_km2,density_per_km2\n");
        var positions=new StringBuilder("split,seed,dimension,gx,gz,x,y,z,theme\n");
        check(SanctuaryPlacement.MIN_SAME_KIND_DISTANCE==500,"preserve same-theme 500-block minimum");
        check(spacing==384&&SanctuaryPlacement.MARGIN==80&&SanctuaryPlacement.ATTEMPTS==32,"preserve candidate geometry and bounded search");
        check(UnderworldLakes.CELL==spacing&&UnderworldLakes.DEEP_RADIUS>=Math.hypot(64,64)+50,"independent natural lakes cover the entire tree envelope plus fifty blocks");
        for(long seed:seeds)for(boolean under:new boolean[]{false,true}){
            var sampler=rates==null?new SanctuaryPlacement.Sampler(seed,under):new SanctuaryPlacement.Sampler(seed,under,rates);
            int found=0;int[] themes=new int[8];var expected=new LinkedHashMap<Long,SanctuaryPlacement.Position>();var cells=new ArrayList<int[]>();
            for(int gx=-radius;gx<radius;gx++)for(int gz=-radius;gz<radius;gz++){
                var p=sampler.find(gx,gz);if(verify){expected.put(key(gx,gz),p);cells.add(new int[]{gx,gz});}
                if(under){var lake=UnderworldLakes.lake(seed,gx,gz);if(lake!=null){naturalLakes++;if(p==null)emptyLakes++;}}
                if(p==null)continue;
                found++;themes[p.theme().ordinal()]++;
                positions.append(split).append(',').append(seed).append(',').append(under?"underworld":"inner").append(',').append(gx).append(',').append(gz).append(',').append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(',').append(p.theme()).append('\n');
                int ox=p.x()-gx*spacing,oz=p.z()-gz*spacing;
                check(ox>=80&&ox<spacing-80&&oz>=80&&oz<spacing-80,"accepted site stays in original nonoverlap margins");
                check(p.y()>=-58&&p.y()+SanctuaryLayout.of(p.theme()).height<=315,"accepted building respects actual final blueprint height");
                if(under){var lake=UnderworldLakes.lake(seed,gx,gz);check(lake!=null&&p.x()==lake.x()&&p.z()==lake.z()&&p.y()==0,"tree uses a preexisting natural sea-level lake");}
                if(verify)for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                    if(dx==0&&dz==0)continue;var q=sampler.find(gx+dx,gz+dz);if(q==null)continue;
                    long xx=p.x()-(long)q.x(),zz=p.z()-(long)q.z(),d2=xx*xx+zz*zz;
                    if(p.theme()==q.theme()){minimumDistance2[p.theme().ordinal()]=Math.min(minimumDistance2[p.theme().ordinal()],d2);check(d2>=250000,"same-theme spacing includes neighbors outside survey bounds");}
                    else if(d2<250000)differentClose++;
                }
            }
            if(verify){
                Collections.reverse(cells);var reverse=rates==null?new SanctuaryPlacement.Sampler(seed,under):new SanctuaryPlacement.Sampler(seed,under,rates);
                for(var c:cells)check(Objects.equals(expected.get(key(c[0],c[1])),reverse.find(c[0],c[1])),"reverse generation order cannot change accepted sites");
                Collections.shuffle(cells,new Random(seed));var shuffled=rates==null?new SanctuaryPlacement.Sampler(seed,under):new SanctuaryPlacement.Sampler(seed,under,rates);
                for(var c:cells)check(Objects.equals(expected.get(key(c[0],c[1])),shuffled.find(c[0],c[1])),"shuffled generation order cannot change accepted sites");
            }
            if(biomes){
                if(under){areas32[6]+=area;areas64[6]+=area;}
                else for(int step:new int[]{32,64})for(int x=-radius*spacing+step/2;x<radius*spacing;x+=step)for(int z=-radius*spacing+step/2;z<radius*spacing;z+=step)
                    (step==32?areas32:areas64)[BiomeRegions.kind(seed,x,z)]+=step*(double)step/1_000_000;
            }
            dimensionTotals[under?1:0]+=found;for(int i=0;i<8;i++)allSites[i]+=themes[i];
            rows.append(String.format(Locale.ROOT,"%s,%d,%s,%d,%.6f,%.6f%n",split,seed,under?"underworld":"inner",found,area,found/area));
            System.out.printf(Locale.ROOT,"DENSITY_045 %s seed=%d dimension=%s sites=%d area=%.6f density=%.6f themes=%s%n",split,seed,under?"underworld":"inner",found,area,found/area,Arrays.toString(themes));
        }
        var summary=new StringBuilder("split,dimension,sites,area_km2,density_per_km2,target\n");
        for(int dim=0;dim<2;dim++){
            double density=dimensionTotals[dim]/(area*seeds.length),target=dim==0?1:.5;
            System.out.printf(Locale.ROOT,"TOTAL_045 %s %s sites=%d area=%.6f density=%.6f target=%.1f%n",split,dim==0?"inner":"underworld",dimensionTotals[dim],area*seeds.length,density,target);
            summary.append(String.format(Locale.ROOT,"%s,%s,%d,%.6f,%.6f,%.1f%n",split,dim==0?"inner":"underworld",dimensionTotals[dim],area*seeds.length,density,target));
            if(verify)check(Math.abs(density-target)<=target*.1,"multi-seed accepted density stays within ten percent of target "+(dim==0?"inner":"underworld")+" actual="+density);
        }
        var byTheme=new StringBuilder("split,theme,sites,area32_km2,area64_km2,density_per_km2,minimum_same_theme_distance\n");
        if(biomes)for(var theme:SurvivalTheme.values()){
            int i=theme.ordinal();double density=allSites[i]/areas32[i],distance=minimumDistance2[i]==Long.MAX_VALUE?0:Math.sqrt(minimumDistance2[i]);
            System.out.printf(Locale.ROOT,"BIOME_045 %s sites=%d area32=%.6f area64=%.6f density=%.6f minSpacing=%.3f%n",theme,allSites[i],areas32[i],areas64[i],density,distance);
            byTheme.append(String.format(Locale.ROOT,"%s,%s,%d,%.6f,%.6f,%.6f,%.3f%n",split,theme,allSites[i],areas32[i],areas64[i],density,distance));
            check(Math.abs(areas32[i]/areas64[i]-1)<.015,"biome area denominator stable at 32/64-block survey resolutions "+theme);
            if(verify){double target=theme.underworld()?.5:1;check(Math.abs(density-target)<=target*.15,"accepted density in each biome remains close to its target "+theme+" actual="+density);}
        }
        if(verify)check(differentClose>0,"different themes retain the original freedom to occur less than five hundred blocks apart");
        check(emptyLakes>naturalLakes/2,"natural lakes remain plentiful independently of selected buildings");
        Files.writeString(output.resolve("dimensions.csv"),rows);Files.writeString(output.resolve("summary.csv"),summary);Files.writeString(output.resolve("themes.csv"),byTheme);Files.writeString(output.resolve("sites.csv"),positions);
        System.out.println("DENSITY_045_OK checks="+checks+" naturalLakes="+naturalLakes+" emptyLakes="+emptyLakes+" rates="+(rates==null?"production":Arrays.toString(rates))+" output="+output);
    }
}
