import dev.googology.mining.EquipmentRules;
import dev.googology.mining.EquipmentRules.Core;
import dev.googology.mining.EquipmentRules.Gear;
import dev.googology.mining.EquipmentRules.Snapshot;
import java.util.*;

/** Independent regression checks for the confirmed 0.3.12 channel boundaries. */
public final class EquipmentRules047Checks {
    private static int checks;
    private static final List<Core> NONE=List.of();
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    private static void near(double actual,double expected,String label){check(Math.abs(actual-expected)<1e-9*Math.max(1,Math.abs(expected)),label+": "+actual+" != "+expected);}
    private static List<Core> core(int type,int... levels){var out=new ArrayList<Core>();for(int level:levels)out.add(new Core(type,level));return out;}
    private static List<Gear> armor(int tier,List<Core> cores){return List.of(new Gear(tier,2,0,cores),new Gear(tier,3,0,cores),new Gear(tier,4,0,cores),new Gear(tier,5,0,cores));}
    private static Snapshot state(Gear main,Gear off,boolean deep){return new Snapshot(main,off,Gear.empty(2),Gear.empty(3),Gear.empty(4),Gear.empty(5),deep);}
    public static void main(String[] args){
        check(EquipmentRules.REVISION==48,"production revision");
        for(int kind=0;kind<=7;kind++)check(EquipmentRules.compatible(kind,8)==(kind==6),"Ordinal manuscript only "+kind);
        for(int type=0;type<9;type++)check(EquipmentRules.compatible(6,type)==(type!=5),"Criticality forbidden in manuscripts "+type);
        for(int type=0;type<9;type++)check(EquipmentRules.compatible(7,type)==EquipmentRules.compatible(1,type),"bow compatibility follows sword "+type);
        check(!EquipmentRules.compatible(1,0)&&EquipmentRules.compatible(0,0),"Sequence gear mining only");
        check(EquipmentRules.compatible(2,4)&&EquipmentRules.compatible(5,4)&&!EquipmentRules.compatible(3,4),"Laver aquatic gear scope");
        for(int tier=1;tier<=4;tier++){
            check(EquipmentRules.slots(tier,7)==new int[]{2,4,6,8}[tier-1],"bow sockets "+tier);
            check(EquipmentRules.durability(tier,7)==new int[]{768,1152,1536,2304}[tier-1],"bow durability "+tier);
            near(EquipmentRules.arrowSpeedMultiplier(tier),new double[]{1.1,1.2,1.3,1.5}[tier-1],"bow launch speed "+tier);
            double a=0,t=0;for(int kind=2;kind<=5;kind++){a+=EquipmentRules.nativeArmor(tier,kind);t+=EquipmentRules.nativeToughness(tier,kind);}
            near(a,new double[]{16,18,20,22}[tier-1],"full native armor "+tier);
            near(t,new double[]{4,8,12,16}[tier-1],"full native toughness "+tier);
            near(EquipmentRules.protectionFactor(armor(tier,NONE),NONE,false),1,"bare armor has no custom multiplier "+tier);
        }
        for(boolean deep:new boolean[]{false,true}){
            near(EquipmentRules.attack(16,core(1,3,3),core(1,3),deep,0,0),deep?16*(1+Math.sqrt(2))*2:16+3*Math.sqrt(2)+3,"separate weapon/book Power RSS "+deep);
            near(EquipmentRules.attack(16,core(8,4),NONE,deep),16,"equipment Ordinal never attacks "+deep);
            near(EquipmentRules.universal(core(8,4,4),deep),.4*Math.sqrt(2),"Ordinal same-item RSS in both realms "+deep);
            near(EquipmentRules.bonusHealth(armor(4,core(8,4,4)),core(7,1,3),deep),4*Math.sqrt(10),"health is only manuscript Guogao "+deep);
            near(EquipmentRules.bonusHealth(armor(4,core(7,3)),core(8,4),deep),0,"no armor/Ordinal health "+deep);
            near(EquipmentRules.miningMultiplier(core(0,3),core(8,4),deep),1,"no Ordinal mining multiplier "+deep);
            check(EquipmentRules.spreadBudget(Collections.nCopies(8,new Core(0,3)),core(8,4),deep)==(deep?25:8),"extra-mining max budget "+deep);
            near(EquipmentRules.projectileCoefficient(core(5,3)),0,"obsolete manuscript projectile channel removed");
        }
        near(EquipmentRules.attack(16,core(1,3),EquipmentRules.join(core(1,2),core(8,4)),false,.2,2),25.5,"normal attack independent HP source sum");
        near(EquipmentRules.attack(16,core(1,3),EquipmentRules.join(core(1,2),core(8,4)),true,.2,2),16*1.2*1.4*1.5*2,"deep attack independent source product");
        near(EquipmentRules.regen(core(8,4),.4),0,"Ordinal and bare book cannot create healing");
        near(EquipmentRules.regen(EquipmentRules.join(core(4,1,3),core(8,4)),.4),Math.sqrt(10)*1.9,"healing channel own coefficients");
        near(EquipmentRules.controlDuration(NONE,core(8,4),1),0,"Ordinal cannot create debuffs");
        near(EquipmentRules.controlDuration(core(7,3,3),core(8,4),1),6.5,"Guogao duplicates cannot amplify duration");
        near(EquipmentRules.controlDuration(core(7,1),core(8,1,4),.25),2*(1.25+Math.sqrt(.25*.25+1.25*1.25)),"mixed Ordinal duration RSS");
        near(EquipmentRules.wearFactor(core(3,1,2,3),core(8,4)),84,"Absence item wear budget sum");
        near(EquipmentRules.wearFactor(core(4,3),core(3,3)),1,"Laver gear and book Absence do not protect wear");
        check(EquipmentRules.yieldLevel(core(2,1,3,3),core(2,3))==6,"Branch yield max ignores manuscripts");
        near(EquipmentRules.reach(core(3,3),core(2,1,3)),Math.sqrt(10),"reach only manuscript Branch RSS");
        near(EquipmentRules.wearFactor(6,core(3,1,2,3)),1,"manuscript preview has no Empty Set durability channel");
        near(EquipmentRules.wearFactor(0,core(3,1,2,3)),84,"pick keeps its own Empty Set durability channel");
        check(EquipmentRules.coreEffectKey(0,2,3).equals("enchantment.minecraft.fortune"),"pick names Fortune");
        for(boolean deep:new boolean[]{false,true}){
            var previewBook=new Gear(4,6,0,EquipmentRules.join(core(1,3,3),core(8,4),core(4,2),core(7,3)));
            var terms=EquipmentRules.attackPreviewTerms(previewBook,deep);
            check(terms.size()==3,"book shows separate innate/Power/Ordinal terms");
            double bonus=deep?1:0;for(double term:terms)bonus=deep?bonus*term:bonus+term;
            near(bonus,EquipmentRules.attack(deep?1:0,NONE,previewBook.cores(),deep,.2,2),"preview operators match actual attack "+deep);
            var sword=new Gear(4,1,0,core(1,3,3));var swordTerms=EquipmentRules.attackPreviewTerms(sword,deep);
            near(swordTerms.getFirst(),deep?1+Math.sqrt(2):3*Math.sqrt(2),"weapon excludes both base damage and other sources");
            var preview=EquipmentRules.itemPreview(sword,deep);
            check(preview.book().isEmpty()&&preview.armor().stream().allMatch(g->g.tier()<0),"single-item preview cannot leak worn gear or held manuscript");
            var helmet=EquipmentRules.itemPreview(new Gear(4,2,0,core(6,3)),deep);
            near(helmet.effects().protectionFactor()-1,.4,"single armor contribution includes its real share");
            near(EquipmentRules.itemPreview(previewBook,deep).effects().bonusHealth(),12,"preview manuscript HP remains an addition");
        }
        check(EquipmentRules.coreEffectKey(1,2,3).equals("enchantment.minecraft.looting"),"sword names Looting");
        check(EquipmentRules.coreEffectKey(7,2,3).equals("enchantment.minecraft.piercing"),"bow names Piercing rather than yield");
        near(EquipmentRules.manuscriptMiningFlat(core(0,1,2,3)),Math.sqrt(336),"mining flat uses RSS");
        near(EquipmentRules.manuscriptMiningRate(core(0,1,2,3)),Math.sqrt(1.3125),"mining percent uses separate RSS");
        near(EquipmentRules.manuscriptMiningSpeed(18,4,core(0,3),0),81.6,"Omega with single grade3");
        near(EquipmentRules.manuscriptMiningSpeed(35,4,core(0,3),0),122.4,"native Efficiency V adds before book multiplier");
        near(EquipmentRules.manuscriptMiningSpeed(1,4,core(0,3),0),40.8,"empty hand and wrong tool get both bonuses");
        near(EquipmentRules.manuscriptMiningSpeed(35,4,core(0,3),.4),142.8,"Haste II is additive in same multiplier bucket");
        near(EquipmentRules.manuscriptMiningSpeed(18,4,Collections.nCopies(6,new Core(0,3)),0),220.15938560837372,"six grade3 max Omega");
        near(EquipmentRules.manuscriptMiningSpeed(35,4,Collections.nCopies(6,new Core(0,3)),0),285.60071123568774,"six grade3 max netherite EfficiencyV");
        near(EquipmentRules.manuscriptMiningSpeed(9,4,NONE,0),12.6,"empty book innate outside RSS");
        near(EquipmentRules.manuscriptMiningSpeed(1,0,NONE,0),1,"no book preserves base");
        near(EquipmentRules.criticalCoefficient(core(5,1,2,3),core(8,4),true),Math.sqrt(.15*.15+.225*.225+.3*.3),"burst coefficient own RSS");
        near(EquipmentRules.oxygenConsumption(core(4,3)),0,"Laver III infinite breath");
        near(EquipmentRules.oxygenConsumption(core(2,3)),1,"Branch no longer affects breathing");
        var arm=armor(4,core(6,3,3));var book=EquipmentRules.join(core(6,2),core(8,4));
        near(EquipmentRules.protectionFactor(arm,book,false,.2),(1+2*Math.sqrt(2))*2.1,"normal armor/book product");
        near(EquipmentRules.protectionFactor(arm,book,true,.2),(1+2*Math.sqrt(2))*1.2*1.5*1.4,"deep armor/book independent product");
        var mixed=new ArrayList<>(armor(4,NONE));mixed.set(0,new Gear(4,2,0,core(6,3)));mixed.set(1,new Gear(4,3,0,core(6,1,1)));
        near(EquipmentRules.protectionFactor(mixed,NONE,false),1+2*(.2+.4*.25*Math.sqrt(2)),"per-item RSS then weighted cross-armor sum");
        var mainBook=new Gear(4,6,0,core(7,3));var offBook=new Gear(1,6,0,core(7,1));
        check(state(mainBook,Gear.empty(-1),false).activeBook()==mainBook,"main book activates without offhand");
        check(state(mainBook,offBook,false).activeBook()==offBook,"offhand wins without book stacking");
        near(state(mainBook,offBook,false).effects().actualMaxHealth(),24,"only active book grants health");
        check(new Gear(4,1,0,core(8,4)).cores().isEmpty(),"invalid old equipment Ordinal ignored");
        check(new Gear(4,6,0,core(5,3)).cores().isEmpty(),"invalid old manuscript Criticality ignored");
        var random=new Random(312);
        for(int i=0;i<1000;i++){
            var cores=new ArrayList<Core>();for(int j=0;j<random.nextInt(7);j++)cores.add(new Core(random.nextBoolean()?6:8,1+random.nextInt(3)));
            for(boolean deep:new boolean[]{false,true}){
                double before=EquipmentRules.protectionFactor(mixed,cores,deep,.2);cores.add(new Core(6,1));
                check(EquipmentRules.protectionFactor(mixed,cores,deep,.2)>=before,"adding a defense core is monotonic");cores.removeLast();
            }
        }
        random=new Random(312);int worn=EquipmentRules.wearCostForFactor(1000000,128,random::nextDouble);check(worn>7450&&worn<8200,"fractional armor/wear budget probability "+worn);
        check(EquipmentRules.wearCostForFactor(100,1,()->{throw new AssertionError("bare RNG");})==100,"bare wear no RNG");
        check(EquipmentRules.rescaledDamage(57,1800,1800)==57,"refresh retains existing wear");
        check(EquipmentRules.rescaledDamage(Integer.MAX_VALUE-1,Integer.MAX_VALUE,3600)==3599,"durability rescale cannot overflow");
        System.out.println("EQUIPMENT_RULES047_OK checks="+checks);
    }
}
