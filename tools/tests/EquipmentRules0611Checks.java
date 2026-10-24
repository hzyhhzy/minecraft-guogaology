import dev.guogaology.mining.EquipmentRules;
import dev.guogaology.mining.EquipmentRules.Core;
import dev.guogaology.mining.EquipmentRules.Gear;
import dev.guogaology.mining.EquipmentRules.Snapshot;
import java.util.*;

/** Confirmed 0.6.11 examples and carrier boundaries, without loading Minecraft. */
public final class EquipmentRules0611Checks {
    private static int checks;
    private static final List<Core> NONE=List.of();
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    private static void near(double actual,double expected,String label){check(Math.abs(actual-expected)<1e-10*Math.max(1,Math.abs(expected)),label+": "+actual+" != "+expected);}
    private static List<Core> power(int... levels){var cores=new ArrayList<Core>();for(int level:levels)cores.add(new Core(1,level));return List.copyOf(cores);}
    private static List<Core> copies(int type,int level,int count){return Collections.nCopies(count,new Core(type,level));}
    private static Snapshot state(int kind,List<Core> own,List<Core> book,boolean hasBook,boolean deep){
        return new Snapshot(new Gear(4,kind,0,own),hasBook?new Gear(4,6,0,book):Gear.empty(6),Gear.empty(2),Gear.empty(3),Gear.empty(4),Gear.empty(5),deep);
    }
    public static void main(String[] args){
        check(EquipmentRules.REVISION==51,"refresh existing equipment onto the current rules");
        for(int tier=1;tier<=4;tier++){
            near(EquipmentRules.arrowSpeedMultiplier(tier),new double[]{1,1.3,1.6,2}[tier-1],"tier arrow speed "+tier);
            near(EquipmentRules.bowBaseDamageCoefficient(tier),2,"all arrows keep native coefficient2 "+tier);
            var bare=EquipmentRules.itemPreview(new Gear(tier,7,0,NONE),false);
            near(bare.effects().attack(),new double[]{6,8,10,12}[tier-1],"bare full-draw noncritical arrow "+tier);
            near(EquipmentRules.bookAttackHp(tier),tier,"attack-only innate HP "+tier);
            near(EquipmentRules.bookAttackBase(tier),tier*.1,"attack-only innate percentage "+tier);
            near(EquipmentRules.bookBase(tier),tier*.05,"protection innate unchanged "+tier);
            near(EquipmentRules.bookHealingBase(tier),tier*.1,"healing innate unchanged "+tier);
            near(EquipmentRules.bookMiningBase(tier),tier*.1,"mining innate unchanged "+tier);
            near(EquipmentRules.bookDurationBase(tier),tier*.25,"duration innate unchanged "+tier);
        }
        for(int grade=1;grade<=3;grade++){
            var core=power(grade);
            near(EquipmentRules.attack(16,core,NONE,false),new double[]{19,22,25}[grade-1],"single sword grade outside "+grade);
            near(EquipmentRules.attack(16,core,NONE,true),new double[]{32,48,80}[grade-1],"single sword grade inside "+grade);
            near(EquipmentRules.attack(0,NONE,core,false),new double[]{2,4,6}[grade-1],"book Power HP independent of sword "+grade);
            near(EquipmentRules.attack(1,NONE,core,true),new double[]{1.5,2,3}[grade-1],"book Power factor independent of sword "+grade);
            near(EquipmentRules.arrowDamageCoefficient(4,core,false),new double[]{2.5,3,3.5}[grade-1],"single bow grade coefficient outside "+grade);
            near(EquipmentRules.arrowDamageCoefficient(4,core,true),new double[]{3,4,6}[grade-1],"single bow grade coefficient inside "+grade);
        }
        near(EquipmentRules.itemPreview(new Gear(3,7,0,power(1)),false).effects().attack(),13,"Gamma grade1 bow uses native float speed/coefficient before double ceil");
        near(EquipmentRules.attack(16,power(1,3),NONE,false),25.486832980505138,"mixed sword grades use RSS HP");
        near(EquipmentRules.attack(16,power(1,3),NONE,true),81.96969000988257,"mixed sword grades use RSS increments, not factors");
        near(EquipmentRules.attack(0,NONE,power(1,3),false),6.324555320336759,"mixed book grades use their own HP scale");
        near(EquipmentRules.attack(1,NONE,power(1,3),true),3.0615528128088303,"mixed book grades use percentage increments");
        near(EquipmentRules.arrowDamageCoefficient(4,power(1,3),false),3.58113883008419,"mixed bow normal coefficient RSS");
        near(EquipmentRules.arrowDamageCoefficient(4,power(1,3),true),6.123105625617661,"mixed bow deep increment RSS");

        var eight=copies(1,3,8);var six=copies(1,3,6);
        near(state(1,NONE,NONE,false,false).effects().attack(),16,"bare sword outside");
        near(state(1,NONE,NONE,false,true).effects().attack(),16,"bare sword inside");
        near(state(7,NONE,NONE,false,false).effects().attack(),12,"bare Omega bow outside");
        near(state(7,NONE,NONE,false,true).effects().attack(),12,"bare Omega bow inside");
        near(state(1,power(3),NONE,false,false).effects().attack(),25,"one grade3 sword outside");
        near(state(1,power(3),NONE,false,true).effects().attack(),80,"one grade3 sword inside");
        near(state(7,power(3),NONE,false,false).effects().attack(),21,"one grade3 bow outside");
        near(state(7,power(3),NONE,false,true).effects().attack(),36,"one grade3 bow inside");
        near(state(1,eight,NONE,false,false).effects().attack(),41.45584412271572,"eight grade3 sword outside");
        near(state(1,eight,NONE,false,true).effects().attack(),197.01933598375618,"eight grade3 sword inside");
        near(state(7,eight,NONE,false,false).effects().attack(),38,"eight grade3 bow outside rounds once");
        near(state(7,eight,NONE,false,true).effects().attack(),80,"eight grade3 bow inside rounds once");
        near(state(1,eight,six,true,false).effects().attack(),60.15278257941478,"full sword/book outside");
        near(state(1,eight,six,true,true).effects().attack(),1627.0982297193164,"full sword/book inside");
        near(state(7,eight,six,true,false).effects().attack(),56.696938456699066,"full bow/book outside keeps fractional manuscript HP after ceil");
        near(state(7,eight,six,true,true).effects().attack(),660.6857023834318,"full bow/book inside multiplies manuscript after ceil");
        double powerVNative=Math.ceil(6*(EquipmentRules.bowBaseDamageCoefficient(4)+3));
        near(powerVNative,30,"native PowerV unmodified coefficient route");
        near(EquipmentRules.attack(powerVNative,NONE,six,false,.4,4),48.696938456699066,"PowerV bow with book outside");
        near(EquipmentRules.attack(powerVNative,NONE,six,true,.4,4),247.75713839378693,"PowerV bow with book inside");

        for(boolean deep:new boolean[]{false,true}){
            var book=new Gear(4,6,0,power(3,3));var sword=new Gear(4,1,0,power(3,3));var bow=new Gear(4,7,0,power(3,3));
            var bookTerms=EquipmentRules.attackPreviewTerms(book,deep);
            near(bookTerms.getFirst(),deep?1.4:4,"preview innate attack follows carrier "+deep);
            near(bookTerms.get(1),deep?1+2*Math.sqrt(2):6*Math.sqrt(2),"book preview uses book Power scale "+deep);
            near(EquipmentRules.attackPreviewTerms(sword,deep).getFirst(),deep?1+4*Math.sqrt(2):9*Math.sqrt(2),"sword preview uses sword Power scale "+deep);
            near(EquipmentRules.attackPreviewTerms(bow,deep).getFirst(),deep?1+2*Math.sqrt(2):1.5*Math.sqrt(2),"bow preview is coefficient, not final HP "+deep);
            near(EquipmentRules.attack(16,List.of(new Core(8,4)),NONE,deep),16,"old equipment Ordinal still inactive "+deep);
            near(EquipmentRules.protectionFactor(List.of(),NONE,deep,.2),1.2,"defense retains old book innate "+deep);
            near(EquipmentRules.protectionFactor(List.of(),List.of(new Core(6,3)),deep,.2),deep?2.4:2.2,"defense core values unchanged "+deep);
            near(EquipmentRules.universal(copies(8,4,2),deep),.4*Math.sqrt(2),"Ordinal attack multiplier unchanged "+deep);
            near(EquipmentRules.criticalCoefficient(List.of(new Core(5,3)),six,deep),.3,"nuclear burst core unchanged "+deep);
            near(EquipmentRules.bonusHealth(List.of(),List.of(new Core(7,3)),deep),12,"heart HP unchanged "+deep);
            check(EquipmentRules.spreadBudget(copies(0,3,8),six,deep)==11,"mining spread uniform pattern budget "+deep);
        }
        near(EquipmentRules.regen(List.of(new Core(4,3),new Core(8,4)),.4),5.7,"healing unchanged");
        near(EquipmentRules.wearFactor(copies(3,3,2),six),128,"wear channel unchanged");
        near(EquipmentRules.reach(NONE,copies(2,3,2)),Math.sqrt(18),"reach channel unchanged");
        near(EquipmentRules.attack(8,power(3),NONE,false),17,"numeric sword shares ordinary sword Power scale");
        near(EquipmentRules.attack(8,power(3),NONE,true),40,"numeric sword shares deep sword Power scale");
        near(EquipmentRules.manuscriptAttackOverflow(2048,2048,2159.292,197,197),2159.292,"restore newly reachable manuscript overflow");
        near(EquipmentRules.manuscriptAttackOverflow(2048,2048,5020,5016,2048),2052,"external additive overflow itself stays capped");
        near(EquipmentRules.manuscriptAttackOverflow(2048,2048,5022.4,5016,2048),2054.4,"only own deep innate increment is restored");
        near(EquipmentRules.manuscriptAttackOverflow(2048,2048,10040,10032,2048),2056,"external multiplier scales own increment without uncapping its baseline");
        near(EquipmentRules.manuscriptAttackOverflow(2048,2048,1900,1800,1800),2048,"do not compensate absent native overflow");
        near(EquipmentRules.manuscriptAttackOverflow(100,2048,3000,2000,2000),100,"do not replace a read below the native maximum");
        near(EquipmentRules.manuscriptAttackOverflow(2048,2048,5000,5000,2048),2048,"zero manuscript contribution keeps external clamp");
        near(EquipmentRules.manuscriptAttackOverflow(2048,2048,5000,5010,2048),2048,"nonpositive effective manuscript contribution is ignored");
        near(EquipmentRules.manuscriptAttackOverflow(4096,2048,5020,5016,2048),4096,"never reduce another read modifier's larger result");
        for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}){
            near(EquipmentRules.manuscriptAttackOverflow(2048,2048,invalid,197,197),2048,"invalid with-raw falls back");
            near(EquipmentRules.manuscriptAttackOverflow(2048,2048,2200,invalid,197),2048,"invalid counterfactual falls back");
            near(EquipmentRules.manuscriptAttackOverflow(2048,2048,2200,197,invalid),2048,"invalid sanitized value falls back");
            near(EquipmentRules.manuscriptAttackOverflow(2048,invalid,2200,197,197),2048,"invalid native maximum falls back");
        }
        check(Double.isNaN(EquipmentRules.manuscriptAttackOverflow(Double.NaN,2048,2200,197,197)),"preserve a nonfinite original read");
        check(EquipmentRules.manuscriptAttackOverflow(Double.POSITIVE_INFINITY,2048,2200,197,197)==Double.POSITIVE_INFINITY,"preserve an original infinity read");
        near(EquipmentRules.manuscriptAttackOverflow(2048,2048,Double.MAX_VALUE,-Double.MAX_VALUE,0),2048,"overflow in the restored increment falls back");
        var rules=EquipmentRules.rulesJson();
        for(String field:List.of("swordPowerAttackGrades","swordPowerPercentGrades","bookPowerAttackGrades","bookPowerPercentGrades","bowPowerCoefficientGrades","bowPowerPercentGrades","bowBaseDamageCoefficients","bookInnateAttackPercent"))check(rules.contains("\""+field+"\":"),"export carrier-specific rules "+field);
        check(rules.contains("\"powerPercentGrades\":[0.25,0.5,1]"),"defense export retains its original scale");
        System.out.println("EQUIPMENT_RULES0611_OK checks="+checks);
    }
}
