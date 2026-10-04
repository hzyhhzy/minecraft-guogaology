package dev.googology.mining;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

/** Ordinary attributes are additive; only ordinal ore uses exponential Denxi speed. */
public final class EquipmentRules {
    private EquipmentRules() {}
    public static final int MAX_SOCKETS=10, REVISION=23;
    public static final String[] MINERALS={"omega","epsilon","gamma","psi","strata","proof"};
    public static final String[] CORES={"sequence_core","power_tower_core","hydra_bud","lho_trace","laver_core","astra_critical_core","boundary_core","guogao_heart","ordinal_crystal"};
    public static final String[] KINDS={"pickaxe","sword","helmet","chestplate","leggings","boots"};
    private static final int[] SLOTS={1,2,4,4,6,6,10}, CALIBRATION={0,0,2,4,6,12,15};
    private static final double[] MINING={0,14,20,30,40,60,80}, PICK={0,9,13,18,25,35,50}, SWORD={0,12,18,25,35,50,70};
    private static final double[] DEFENSE={0,48,70,100,140,200,280}, SCALE={0,1,1.5,2,3,4,6};
    public static int slots(int tier){return SLOTS[tier];}
    public static int grade(int tier){return tier==0?1:(tier+1)/2;}
    public static int digit(double value){return (int)Math.floor(Math.max(0,Math.min(9,value)));}
    public static int coreType(String id){for(int i=0;i<CORES.length;i++)if(id.equals(CORES[i])||id.startsWith(CORES[i]+"_lv"))return i;return -1;}
    public static int coreLevel(String id){int p=id.lastIndexOf("_lv");return p<0?1:Integer.parseInt(id.substring(p+3));}
    public static int stationGrade(int type,int level){return type==8?Math.max(1,level-1):level;}
    public static int denxiLevel(int type,int level){return Math.max(0,type==8?level-1:level);}
    public static double weight(int type,int level){return type==8?level*.5:1+(level-1)*.5;}
    public static boolean compatible(int kind,int type){return switch(type){case 0->kind==0;case 4->kind<2;case 1,5->kind==1;case 3->kind<2;case 6,7->kind>=2;default->true;};}
    public static double basePower(int tier){return DEFENSE[tier];}
    public static double baseAttack(int tier,int kind,double value){return tier==0?(kind==1?5:3)+digit(value)/3d:(kind==1?SWORD[tier]:PICK[tier]);}
    public static double baseMining(int tier,double value){return tier==0?4+5*digit(value)/9d:MINING[tier];}
    public static double baseDenxi(int tier,double value){return tier==0?8*Math.pow(64,digit(value)/9d):512*Math.pow(16,tier);}
    public static double denxi(int tier,double value,int levels){return baseDenxi(tier,value)*Math.pow(4,levels);}
    public static double oreWork(int tier){return tier==1?2560:5*baseDenxi(tier-1,0)*Math.pow(4,CALIBRATION[tier]);}
    public static int durability(int tier,int kind){return durability(tier,kind,0);}
    public static int durability(int tier,int kind,double value){return tier==0?(int)Math.round(131*Math.pow(2031d/131,digit(value)/9d)):(int)Math.round((kind<2?3000:new int[]{600,900,850,700}[kind-2])*SCALE[tier]);}
    public static double armorShare(int kind){return switch(kind){case 2->.2;case 3->.4;case 4->.25;case 5->.15;default->0;};}
    public static int visualArmor(int tier,int kind){return kind<2?0:new int[]{3,8,6,3}[kind-2];}
    public static int spreadBudget(double weight){return (int)Math.floor(weight*4);}
    public static String format(double value){
        if(value==0)return "0.00";
        double rounded=BigDecimal.valueOf(value).round(new MathContext(3,RoundingMode.HALF_UP)).doubleValue();
        if(Math.abs(rounded)>=1e12)return String.format(Locale.ROOT,"%.2e",rounded);
        double unit=Math.abs(rounded)>=1e6?1e6:Math.abs(rounded)>=1e3?1e3:1;
        double scaled=rounded/unit;
        int decimals=Math.max(0,2-(int)Math.floor(Math.log10(Math.abs(scaled))));
        return String.format(Locale.ROOT,"%."+decimals+"f",scaled)+(unit==1e6?"M":unit==1e3?"k":"");
    }
}
