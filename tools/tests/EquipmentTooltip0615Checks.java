import dev.guogaology.mining.EquipmentRules;
import dev.guogaology.mining.EquipmentTooltipRules;
import java.util.HashSet;
import java.util.List;

/** Capability boundaries, not tooltip snapshots or gameplay balance tests. */
public final class EquipmentTooltip0615Checks {
    private static int checks;
    private static final String PREFIX="mining.guogaology.tooltip.trait.";
    private static void check(boolean condition,String why){checks++;if(!condition)throw new AssertionError(why);}
    private static EquipmentRules.Core core(int type,int level){return new EquipmentRules.Core(type,level);}
    private static List<String> traits(int kind,EquipmentRules.Core... cores){return EquipmentTooltipRules.traitKeys(kind,List.of(cores),false,false);}
    public static void main(String[] args) {
        for(int kind=0;kind<=EquipmentRules.BOW;kind++) {
            check(traits(kind).isEmpty(),"bare carriers add no descriptive numeric traits");
            for(int type=0;type<9;type++)for(int level=1;level<=(type==8?4:3);level++) {
                var once=traits(kind,core(type,level));
                var repeated=traits(kind,core(type,level),core(type,level));
                check(once.equals(repeated),"repeated cores do not duplicate capabilities");
                check(once.size()==new HashSet<>(once).size(),"capability summary has no duplicate keys");
                if(!EquipmentRules.compatible(kind,type))check(once.isEmpty(),"incompatible cores cannot advertise effects");
                if(type==1||type==8||type==3&&kind!=EquipmentRules.MANUSCRIPT)check(once.isEmpty(),"pure stat bonuses stay in station preview");
            }
        }
        for(int grade=1;grade<=3;grade++) {
            var active=List.of(core(2,grade));
            check(EquipmentTooltipRules.traitKeys(0,active,true,false).equals(List.of("enchantment.minecraft.silk_touch")),"Silk replaces Fortune rather than listing both");
            check(EquipmentTooltipRules.traitKeys(0,active,false,false).equals(List.of("enchantment.minecraft.fortune")),"disabled Silk advertises actual Fortune");
            check(traits(1,core(2,grade)).equals(List.of("enchantment.minecraft.looting")),"sword uses Looting");
            check(traits(EquipmentRules.MANUSCRIPT,core(2,grade)).isEmpty(),"manuscript reach is numeric and not Fortune");
            var bow=traits(EquipmentRules.BOW,core(2,grade));
            check(bow.size()==grade&&bow.getFirst().equals("enchantment.minecraft.infinity"),"bow retains all unlocked abilities, not only highest");
        }
        check(traits(6,core(5,3)).isEmpty(),"legacy manuscript nuclear core has no actual effect");
        check(traits(6,core(3,1)).equals(List.of(PREFIX+"stealth")),"Lv1 stealth does not falsely advertise Night Vision");
        check(traits(6,core(3,2)).contains("effect.minecraft.night_vision"),"Night Vision begins at Lv2");
        check(!traits(6,core(4,1)).contains(PREFIX+"food_floor"),"food floor begins at Lv2");
        check(traits(6,core(4,2)).containsAll(List.of(PREFIX+"healing",PREFIX+"food_floor")),"healing and hunger safeguard are separate mechanisms");
        check(traits(6,core(6,1)).isEmpty(),"numeric Lv1 jump/landing bonuses do not reappear as prose");
        check(traits(6,core(6,2)).contains(PREFIX+"flight")&&!traits(6,core(6,2)).contains(PREFIX+"sprint_flight"),"Lv2 flight has no sprint");
        var mixed=traits(6,core(6,1),core(6,2),core(6,3));
        check(mixed.contains(PREFIX+"sprint_flight")&&!mixed.contains(PREFIX+"flight"),"Lv3 owns flight summary when levels coexist");
        check(mixed.contains(PREFIX+"fall_immunity")&&mixed.contains(PREFIX+"collision_immunity"),"Lv3 actual immunities are listed");
        check(!traits(6,core(7,1)).contains(PREFIX+"inventory_totems")&&traits(6,core(7,2)).contains(PREFIX+"inventory_totems"),"inventory-totem mechanism starts at Lv2");
        check(EquipmentTooltipRules.traitKeys(6,List.of(),false,true).equals(List.of(PREFIX+"socket_totems")),"socketed totem is independent of a Guogao core");
        check(EquipmentTooltipRules.traitKeys(1,List.of(),false,true).isEmpty(),"invalid non-manuscript totem cannot advertise rescue");
        check(traits(2,core(4,1)).containsAll(List.of("enchantment.minecraft.respiration","enchantment.minecraft.aqua_affinity")),"helmet water abilities reflect both native mechanisms");
        check(traits(5,core(4,1)).equals(List.of("enchantment.minecraft.depth_strider")),"boots advertise movement rather than breathing");
        for(int stage=2;stage<=4;stage++)check(EquipmentRules.displayedCoreGrade(8,stage)==stage-1,"Ordinal display grades preserve material-stage semantics");
        System.out.println("EquipmentTooltip0615Checks passed: "+checks);
    }
}
