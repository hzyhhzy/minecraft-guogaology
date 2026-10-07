import dev.googology.mining.EquipmentRules;

/** Pure shared-rule regression for 0.4.4 socket eligibility; no Minecraft runtime. */
public final class EquipmentRules044Checks {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        check(EquipmentRules.grade(0)==1,"numeric tools retain Lv1");
        check(EquipmentRules.slots(0,0)==1&&EquipmentRules.slots(0,1)==1,"numeric tools retain one socket");
        for(int tier=1;tier<=4;tier++){
            check(EquipmentRules.grade(tier)==3,"all material tiers accept displayed Lv3: "+tier);
            for(int kind=0;kind<=EquipmentRules.BOW;kind++)
                check(EquipmentRules.slots(tier,kind)==(kind==EquipmentRules.MANUSCRIPT?new int[]{2,3,4,6}[tier-1]:new int[]{2,4,6,8}[tier-1]),"socket capacity remains tier/kind dependent");
            check(EquipmentRules.stationGrade(8,4)<=EquipmentRules.grade(tier),"all manuscripts accept Ordinal raw stage4 / displayed Lv3");
        }
        for(int level=1;level<=3;level++){
            check(EquipmentRules.stationGrade(0,level)==level,"regional station ranks unchanged");
            check(EquipmentRules.stationGrade(8,level+1)==level,"Ordinal station ranks retain displayed grades");
        }
        check(EquipmentRules.stationGrade(8,1)==1,"ungraded Ordinal Crystal still fits basic rank");
        for(int kind=0;kind<=EquipmentRules.BOW;kind++)
            check(EquipmentRules.compatible(kind,8)==(kind==EquipmentRules.MANUSCRIPT),"Ordinal remains manuscript only");
        check(!EquipmentRules.compatible(EquipmentRules.MANUSCRIPT,5),"Criticality remains incompatible with manuscripts");
        System.out.println("EQUIPMENT_RULES_044_OK checks="+checks);
    }
}
