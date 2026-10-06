import dev.googology.mining.EquipmentRules;
import dev.googology.mining.EquipmentRules.Core;
import dev.googology.mining.EquipmentRules.Gear;
import java.util.*;

/** Independent expected-value checks for the user's 0.3.11 aggregation rules. */
public final class EquipmentRules041Checks {
    private static int checks;
    private static void check(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
    private static void near(double a,double b,String label){check(Math.abs(a-b)<1e-8*Math.max(1,Math.abs(b)),label+": "+a+" != "+b);}
    private static List<Core> core(int type,int... levels){var out=new ArrayList<Core>();for(int level:levels)out.add(new Core(type,level));return out;}
    private static List<Gear> armor(int tier,int type,int count,int level){var out=new ArrayList<Gear>();for(int k=2;k<6;k++)out.add(new Gear(tier,k,0,count==0?List.of():Collections.nCopies(count,new Core(type,level))));return out;}
    public static void main(String[] args){
        var none=List.<Core>of();
        for(int digit=0;digit<10;digit++)check(EquipmentRules.harvestLevel(0,digit)==(digit<5?0:digit<8?1:digit==8?2:3),"numeric harvest "+digit);
        for(int type=0;type<9;type++)check(EquipmentRules.compatible(6,type),"all nine book types "+type);
        check(EquipmentRules.compatible(0,0)&&EquipmentRules.compatible(1,0)&&!EquipmentRules.compatible(2,0),"yield gear scope");
        check(EquipmentRules.compatible(0,2)&&EquipmentRules.compatible(2,2)&&!EquipmentRules.compatible(1,2),"branch gear scope");
        for(int kind=0;kind<6;kind++)check(EquipmentRules.compatible(kind,4),"Laver wear scope "+kind);
        near(EquipmentRules.miningMultiplier(none,none,false),1,"bare mining");
        for(int level=1;level<=3;level++){
            near(EquipmentRules.miningMultiplier(none,core(2,level),false),1+(level-1)*.5,"single branch speed "+level);
            near(EquipmentRules.wearFactor(core(4,level),none),new double[]{16,64,256}[level-1],"single Laver wear "+level);
            near(EquipmentRules.regen(core(4,level)),level,"single Laver healing "+level);
            near(EquipmentRules.reach(core(3,level),none),level,"single reach "+level);
        }
        near(EquipmentRules.miningMultiplier(none,core(2,1,2,3),false),Math.sqrt(7.25),"mixed speed RMS");
        near(EquipmentRules.miningMultiplier(none,core(2,1,2,3),true),7.25,"deep speed squared sum");
        near(EquipmentRules.spreadBudget(core(2,1,2,3),none,false),3,"range integer sqrt");
        near(EquipmentRules.spreadBudget(core(2,1,2,3),none,true),14,"deep range squared sum");
        near(EquipmentRules.spreadBudget(core(4,3),none,true),0,"wear never becomes range");
        near(EquipmentRules.yieldLevel(core(0,1,3),core(0,2)),6,"tool-book yield direct sum");
        near(EquipmentRules.yieldLevel(Collections.nCopies(8,new Core(0,3)),Collections.nCopies(6,new Core(0,3))),42,"no arbitrary yield cap");
        near(EquipmentRules.wearFactor(core(4,1,2,3),none),336,"wear direct sum");
        near(EquipmentRules.regen(core(4,1,2,3)),Math.sqrt(14),"healing RMS");
        near(EquipmentRules.attack(16,core(1,2,3),core(1,1),false),22,"book and sword attack add");
        near(EquipmentRules.reach(core(3,1,2,3),none),Math.sqrt(14),"reach RMS");
        near(EquipmentRules.bonusHealth(armor(4,0,0,1),core(7,1,2,3),false),4*Math.sqrt(14),"normal HP RMS");
        near(EquipmentRules.bonusHealth(armor(4,0,0,1),core(7,1,2,3),true),60,"deep HP sum");
        for(int tier=1;tier<=4;tier++)near(EquipmentRules.protectionFactor(armor(tier,0,0,1),none,false),new double[]{2,2.5,3,4}[tier-1],"naked full armor factor "+tier);
        near(EquipmentRules.protectionFactor(armor(4,6,8,3),core(6,3,3,3,3,3,3),false),4*(1+.25*Math.sqrt(126)),"armor shares combine once");
        near(EquipmentRules.protectionFactor(armor(4,6,8,3),core(6,3,3,3,3,3,3),true),130,"deep protection one budget");
        near(EquipmentRules.universal(core(8,4,4),false),Math.sqrt(.5),"normal ordinal RMS");
        near(EquipmentRules.universal(core(8,4,4),true),1,"deep ordinal sum");
        near(EquipmentRules.attack(16,core(1,3),core(8,4),false),20,"normal ordinal small additive");
        near(EquipmentRules.attack(16,core(1,3),core(8,4),true),24,"deep ordinal boosts increments once");
        near(EquipmentRules.projectileBurst(10,EquipmentRules.join(core(5,3),core(8,4)),true),6,"projectile extra damage gets one deep multiplier");
        near(EquipmentRules.criticalCoefficient(core(5,1,2,3),none,false),Math.sqrt(.15*.15+.225*.225+.3*.3),"blast coefficient RMS");
        near(EquipmentRules.oxygenConsumption(core(2,1)),.25,"oxygen level one");
        near(EquipmentRules.oxygenConsumption(core(2,2)),1d/16,"oxygen level two");
        near(EquipmentRules.oxygenConsumption(core(2,3)),0,"oxygen level three");
        near(EquipmentRules.controlDuration(core(7,1,3)),Math.sqrt(10),"mixed binding duration");
        near(EquipmentRules.controlDuration(Collections.nCopies(8,new Core(7,3))),8,"binding duration ceiling");
        check(EquipmentRules.durability(4,6)==0,"manuscript has no durability");
        check(EquipmentRules.wearCostForFactor(100,1,()->{throw new AssertionError("bare RNG");})==100,"bare wear no RNG");
        check(EquipmentRules.wearCostForFactor(100,256,()->0)==100&&EquipmentRules.wearCostForFactor(100,256,()->1)==0,"probability extremes");
        var random=new Random(311);int worn=EquipmentRules.wearCostForFactor(100000,16,random::nextDouble);check(worn>5950&&worn<6500,"one-sixteenth distribution "+worn);
        check(EquipmentRules.rescaledDamage(57,1800,1800)==57,"refresh preserves damage");
        check(EquipmentRules.rescaledDamage(Integer.MAX_VALUE-1,Integer.MAX_VALUE,3600)==3599,"rescale cannot overflow");
        System.out.println("EQUIPMENT_RULES041_OK checks="+checks);
    }
}
