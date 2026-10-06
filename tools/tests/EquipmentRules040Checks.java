import dev.googology.mining.EquipmentRules;
import java.util.Random;

/** Standalone rules regression; compile alongside the shared EquipmentRules source. */
public final class EquipmentRules040Checks {
    private static void check(boolean b,String label){if(!b)throw new AssertionError(label);}
    public static void main(String[] args){
        for(int digit=0;digit<10;digit++)check(EquipmentRules.harvestLevel(0,digit)==(digit<5?0:digit<8?1:digit==8?2:3),"numeric harvest level "+digit);
        for(int kind=0;kind<=6;kind++)check(!EquipmentRules.compatible(kind,4),"Laver inactive for kind "+kind);
        check(EquipmentRules.rescaledDamage(57,1800,1800)==57,"no same-capacity floating wear");
        check(EquipmentRules.rescaledDamage(57,3600,3600)==57,"no realm-change floating wear");
        check(EquipmentRules.rescaledDamage(300001,1040400,3600)==1039,"exact old-capacity ceiling migration");
        check(EquipmentRules.rescaledDamage(Integer.MAX_VALUE-1,Integer.MAX_VALUE,3600)==3599,"migration cannot overflow or break gear");
        check(EquipmentRules.durabilityFactor(0)==1&&EquipmentRules.durabilityFactor(1)==9&&EquipmentRules.durabilityFactor(6)==169&&EquipmentRules.durabilityFactor(28)==3249,"same-family points sum once");
        check(EquipmentRules.wearCost(100,0,()->{throw new AssertionError("zero-point RNG");})==100,"unprotected wear unchanged");
        check(EquipmentRules.wearCost(20,6,()->0)==20&&EquipmentRules.wearCost(20,6,()->1)==0,"per-point probability extremes");
        var random=new Random(173);int cost=EquipmentRules.wearCost(200000,1,random::nextDouble);
        check(cost>21500&&cost<23000,"one ninth protection distribution: "+cost);
        random=new Random(179);cost=EquipmentRules.wearCost(200000,6,random::nextDouble);
        check(cost>1050&&cost<1350,"combined one 169th protection distribution: "+cost);
        System.out.println("EQUIPMENT_RULES040_OK");
    }
}
