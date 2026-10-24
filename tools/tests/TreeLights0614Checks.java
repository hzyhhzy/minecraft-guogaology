import dev.guogaology.world.*;
import java.util.*;

/** Pure production-path invariants; natural scene paint is checked separately in native QA. */
public final class TreeLights0614Checks {
    private static int checks,placed,skipped;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static int distance(HauntedVines.LampPosition a,HauntedVines.LampPosition b){return Math.max(Math.abs(a.x()-b.x()),Math.max(Math.abs(a.y()-b.y()),Math.abs(a.z()-b.z())));}
    private static void validate(HauntedVines.Band band,Set<HauntedVines.LampPosition> before,OrdinalLightBand.Pattern pattern,long salt){
        check(band.digits().equals(pattern.digits())&&band.random()==pattern.random()&&band.salt()==salt,"whole expression, type and palette salt preserved");
        var row=band.blocks();if(row.isEmpty()){skipped++;return;}placed++;
        check(row.size()==pattern.digits().size()+4,"two blank endpoints on each side, no dropped/extra digits");
        check(new HashSet<>(row).size()==row.size(),"no revisited lamp voxel");
        for(int i=0;i<row.size();i++){
            var at=row.get(i);check(at.y()>=-62&&at.y()<=317,"complete row fits world paint bounds");
            if(i>0)check(distance(at,row.get(i-1))==1,"consecutive lamps touch face, edge or corner");
            for(int j=0;j<i-2;j++)check(distance(at,row.get(j))>=2,"nonlocal parts cannot cross or create an alternate branch");
            for(var old:before)check(distance(at,old)>=2,"other strings have a complete voxel clearance");
        }
    }
    private static void crown(HauntedVines.Crown crown,long salt,int site,HashSet<HauntedVines.LampPosition> reserved){
        for(var pattern:OrdinalLightBand.quartet(salt,site)){
            var before=Set.copyOf(reserved);long seed=WorldNoise.mix(salt+site*433L+pattern.digits().size());
            var band=HauntedVines.crownBand(crown,pattern,seed,WorldNoise.unit(seed)*Math.PI*2,0,reserved);
            validate(band,before,pattern,seed);
            var expected=new HashSet<>(before);expected.addAll(band.blocks());check(expected.equals(reserved),"reserve only an accepted whole row");
            var replayReserved=new HashSet<>(before);
            check(band.equals(HauntedVines.crownBand(crown,pattern,seed,WorldNoise.unit(seed)*Math.PI*2,0,replayReserved)),"deterministic irrespective of external world/chunk access");
        }
    }
    private static void isolatedShapes(){
        for(int dir:new int[]{-1,1})for(int radius:new int[]{2,4,6,9,14,24})for(long salt=0;salt<12;salt++){
            var crown=new HauntedVines.Crown(new HauntedVines.Point(-13,dir<0?260:-10,17),new HauntedVines.Point(0,dir,0),radius,radius,WorldNoise.mix(salt));
            var used=new HashSet<HauntedVines.LampPosition>();crown(crown,salt,0,used);crown(crown,salt,1,used);
        }
        check(placed>500&&skipped>0,"real layouts fit many full rows while constrained crowns safely reject whole rows");
    }
    private static void branching(){
        for(int dir:new int[]{-1,1})for(long salt=0;salt<12;salt++){
            var plan=HauntedVines.treePlan(0,dir<0?260:0,0,70,dir,salt);
            var used=new HashSet<HauntedVines.LampPosition>();int i=0;
            for(var shape:plan.crowns())crown(shape,salt,i++,used);
            var giant=HauntedVines.plan(0,dir<0?270:0,0,210,dir,salt);used.clear();
            for(var band:giant.bands()){
                validate(band,used,new OrdinalLightBand.Pattern(band.digits(),band.random()?OrdinalLightBand.Kind.RANDOM:OrdinalLightBand.Kind.SHORT),band.salt());
                used.addAll(band.blocks());
            }
        }
    }
    private static void invertedOrder(){
        var pattern=new OrdinalLightBand.Pattern(OrdinalLightBand.MEDIUM,OrdinalLightBand.Kind.MEDIUM);
        var up=new HauntedVines.Crown(new HauntedVines.Point(0,80,0),new HauntedVines.Point(0,1,0),9,9,37);
        var down=new HauntedVines.Crown(new HauntedVines.Point(0,80,0),new HauntedVines.Point(0,-1,0),9,9,37);
        var a=HauntedVines.crownBand(up,pattern,37,.7,0,new HashSet<>());
        var b=HauntedVines.crownBand(down,pattern,37,.7,0,new HashSet<>());
        check(!a.blocks().isEmpty()&&a.blocks().size()==b.blocks().size(),"inverted reference fixture fits");
        for(int i=0;i<a.blocks().size();i++){
            var p=a.blocks().get(a.blocks().size()-1-i);var q=b.blocks().get(i);
            check(p.x()==q.x()&&160-p.y()==q.y()&&-p.z()==q.z(),"inverted tree reverses positions, not expression digits");
        }
    }
    private static void blockedFailure(){
        var blocked=new HashSet<HauntedVines.LampPosition>();
        for(int x=-15;x<=15;x++)for(int y=65;y<=96;y++)for(int z=-15;z<=15;z++)blocked.add(new HauntedVines.LampPosition(x,y,z));
        var before=Set.copyOf(blocked);var crown=new HauntedVines.Crown(new HauntedVines.Point(0,80,0),new HauntedVines.Point(0,1,0),9,9,37);
        var band=HauntedVines.crownBand(crown,OrdinalLightBand.pick(37,2),37,0,0,blocked);
        check(band.blocks().isEmpty()&&blocked.equals(before),"fully blocked crown does not scatter or partially reserve a sequence");
        check(!HauntedVines.lampGap(new HauntedVines.LampPosition(16,97,16),blocked),"diagonal neighbour clearance includes all26 directions");
        check(HauntedVines.lampGap(new HauntedVines.LampPosition(17,98,17),blocked),"one full voxel gap is sufficient");
    }
    public static void main(String[]args){isolatedShapes();branching();invertedOrder();blockedFailure();System.out.println("TREE_LIGHTS_0614_OK checks="+checks+" placed="+placed+" wholeRowsSkipped="+skipped);}
}
