import dev.guogaology.mining.MiningPattern;
import dev.guogaology.mining.EquipmentRules;
import dev.guogaology.mining.EquipmentRules.Core;
import java.util.*;

/** Native-free acceptance for saved stencils and the confirmed realm-independent capacity. */
public final class MiningPattern0612HelperChecks {
 private static int checks;
 private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  check(MiningPattern.DEFAULT_REFERENCE==12,"default origin is always center");
  check(new MiningPattern(-1,-1).reference()==12,"corrupt origin defaults safely");
  check(new MiningPattern(25,-1).mask()==(0x1ffffff&~(1<<12)),"corrupt mask clips to 24 real cells");
  int[][] defaults={{},{17},{7,17},{11,13,17},{7,11,13,17},{11,13,16,17,18},{7,11,13,16,17,18},{6,8,11,13,16,17,18},{6,7,8,11,13,16,17,18},{2,6,7,8,11,13,16,17,18},{1,3,6,7,8,11,13,16,17,18},{1,2,3,6,7,8,11,13,16,17,18}};
  for(int budget=0;budget<=11;budget++){
   int expectedMask=0;for(int cell:defaults[budget])expectedMask|=1<<cell;
   var pattern=MiningPattern.defaultForBudget(budget);
   check(pattern.reference()==12&&pattern.mask()==expectedMask,"exact approved template for "+(budget+1)+" total blocks");
   check(pattern.count()==budget&&!pattern.contains(12),"default count excludes the unique reference "+budget);
  }
  var empty=new MiningPattern(12,0);check(empty.active(24).isEmpty(),"explicit empty layout disables extras");
  check(empty.toggle(12).equals(empty)&&empty.toggle(-1).equals(empty)&&empty.toggle(25).equals(empty),"origin and invalid cells cannot be selected");
  var all=new MiningPattern(12,-1);
  check(all.active(11).equals(List.of(7,11,13,17,2,6,8,10,14,16,18)),"fixed Manhattan and cell-index order limits the mask before validation");
  check(all.active(0).isEmpty()&&all.active(-3).isEmpty()&&all.active(100).size()==24,"capacity is bounded");
  for(int reference=0;reference<25;reference++){
   var full=new MiningPattern(reference,-1);check(full.count()==24&&!full.contains(reference),"unique reference "+reference);
   for(int cell=0;cell<25;cell++){
    check(full.horizontal(cell)==cell%5-reference%5,"horizontal grid coordinate "+reference+"/"+cell);
    check(full.vertical(cell)==reference/5-cell/5,"up is always positive world Y "+reference+"/"+cell);
    check(full.toggle(cell).toggle(cell).equals(full),"toggle is reversible "+reference+"/"+cell);
    var moved=full.withReference(cell);check(moved.reference()==cell&&!moved.contains(cell)&&!moved.contains(reference),"moving origin removes no unrelated selections "+reference+"/"+cell);
   }
   for(int budget=0;budget<30;budget++){
    var active=full.active(budget);check(active.size()==Math.min(budget,24),"bounded ordered selection");
    check(Integer.bitCount(full.activeMask(budget))==active.size(),"mask/list agree");
    check((full.activeMask(budget)&~full.mask())==0,"selection never invents a cell");
    if(budget>0)check(active.containsAll(full.active(budget-1)),"immutable mask active prefixes are nested");
   }
  }
  for(boolean deep:new boolean[]{false,true}){
   for(int lv=1;lv<=3;lv++)for(int count=1;count<=8;count++){
    int value=new int[]{1,2,4}[lv-1];int wanted=(int)Math.floor(Math.sqrt(count*value*value));
    check(EquipmentRules.spreadBudget(Collections.nCopies(count,new Core(0,lv)),List.of(),deep)==wanted,"realm-independent RSS extra budget lv"+lv+" count"+count+" deep"+deep);
   }
   for(int slots:new int[]{2,4,6,8})check(EquipmentRules.spreadBudget(Collections.nCopies(slots,new Core(0,3)),List.of(),deep)==(int)Math.floor(4*Math.sqrt(slots)),"full pick capacity "+slots);
   check(EquipmentRules.spreadBudget(List.of(new Core(0,1),new Core(0,2),new Core(0,3)),List.of(),deep)==4,"mixed levels have 21 points and four extras");
   check(EquipmentRules.spreadBudget(List.of(),List.of(new Core(0,3)),deep)==0,"manuscript Matrix does not add mining cells");
  }
  var random=new Random(612);for(int i=0;i<1000;i++){
   var saved=new MiningPattern(random.nextInt(25),random.nextInt());int mask=saved.mask();saved.active(random.nextInt(25));
   check(saved.mask()==mask,"reading active cells never mutates saved layout");
   check((saved.activeMask(1)&saved.activeMask(12))==saved.activeMask(1),"reading two active prefixes never invents cells");
  }
  System.out.println("MINING_PATTERN_0612_HELPER_OK checks="+checks);
 }
}
