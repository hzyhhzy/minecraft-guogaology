package dev.googology.mining;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

/** Shared four-tier balance. Core contributions are summed before one realm multiplier. */
public final class EquipmentRules {
    private EquipmentRules() {}
    public static final int MAX_SOCKETS=10, REVISION=41;
    public static final String[] MINERALS={"omega","epsilon","gamma","true_omega"};
    public static final String[] CORES={"sequence_core","power_tower_core","hydra_bud","lho_trace","laver_core","astra_critical_core","boundary_core","guogao_heart","ordinal_crystal"};
    public static final String[] KINDS={"pickaxe","sword","helmet","chestplate","leggings","boots"};
    private static final int[] SLOTS={1,2,4,6,8};
    private static final double[] MINING={0,9,11,14,18}, PICK={0,6,8,10,12}, SWORD={0,8,10,12,16};
    private static final double[] DEFENSE={0,8,12,16,24}, SCALE={0,1,1.25,1.5,2};
    public static int slots(int tier){return SLOTS[tier];}
    public static int slots(int tier,int kind){return kind==6?new int[]{0,2,3,4,6}[tier]:slots(tier);}
    public static int grade(int tier){return new int[]{1,1,2,2,3}[tier];}
    public static double multiplier(double points,boolean deep){double m=1+3*Math.clamp(points/16,0,1);return deep?m*m*m*m:m;}
    /** 0=stone, 1=iron, 2=diamond, 3=netherite. Mineral picks keep their existing capability. */
    public static int harvestLevel(int tier,double value){int d=digit(value);return tier>0?3:d<5?0:d<8?1:d==8?2:3;}
    /** Branch points reduce wear, never the visible/base durability capacity. */
    public static double durabilityFactor(double points){double n=1+2*Math.max(0,points);return n*n;}
    public static int wearCost(int amount,double points,java.util.function.DoubleSupplier random){
        if(amount<=0||points<=0)return amount;
        double chance=1/durabilityFactor(points);int spent=0;
        for(int i=0;i<amount;i++)if(random.getAsDouble()<chance)spent++;
        return spent;
    }
    /** Integer ceiling preserves the old wear fraction without floating-point extra damage. */
    public static int rescaledDamage(int damage,int oldMax,int newMax){
        if(oldMax<=0||newMax<=0)return 0;
        int worn=Math.clamp(damage,0,oldMax);
        long scaled=oldMax==newMax?worn:((long)worn*newMax+oldMax-1)/oldMax;
        return (int)Math.min(newMax-1,scaled);
    }
    public static int digit(double value){return (int)Math.floor(Math.max(0,Math.min(9,value)));}
    public static int coreType(String id){for(int i=0;i<CORES.length;i++)if(id.equals(CORES[i])||id.startsWith(CORES[i]+"_lv"))return i;return -1;}
    public static int coreLevel(String id){int p=id.lastIndexOf("_lv");return p<0?1:Integer.parseInt(id.substring(p+3));}
    public static int stationGrade(int type,int level){return type==8?Math.max(1,level-1):level;}
    public static int denxiLevel(int type,int level){return Math.max(0,type==8?level-1:level);}
    public static double weight(int type,int level){return type==8?level*.5:1+(level-1)*.5;}
    public static boolean compatible(int kind,int type){if(type<0||type>8)return false;if(kind==6)return true;return switch(type){case 0->kind<2;case 1,5,7->kind==1;case 2->kind==0||kind==2;case 3->kind<2;case 4,8->true;case 6->kind>=2;default->false;};}
    public static double basePower(int tier){return DEFENSE[tier];}
    public static double baseAttack(int tier,int kind,double value){return tier==0?(kind==1?5:3)+digit(value)/3d:(kind==1?SWORD[tier]:PICK[tier]);}
    public static double baseMining(int tier,double value){return tier==0?4+5*digit(value)/9d:MINING[tier];}
    public static double baseDenxi(int tier,double value){return baseMining(tier,value);}
    public static double denxi(int tier,double value,int levels){return baseMining(tier,value);}
    public static double oreWork(int tier){return 3+(tier-1)*.5;}
    public static int durability(int tier,int kind){return durability(tier,kind,0);}
    public static int durability(int tier,int kind,double value){if(kind==6)return 0;return tier==0?(int)Math.round(131*Math.pow(2031d/131,digit(value)/9d)):(int)Math.round((kind<2?1800:new int[]{400,600,550,450}[kind-2])*SCALE[tier]);}
    public static double armorShare(int kind){return switch(kind){case 2->.2;case 3->.4;case 4->.25;case 5->.15;default->0;};}
    public static int visualArmor(int tier,int kind){return kind<2||kind>5?0:new int[]{3,8,6,3}[kind-2];}
    public static int spreadBudget(double weight){return (int)Math.floor(weight*4);}
    /** Minecraft-free input model, also used by the equipment simulator. */
    public record Core(int type,int level){}
    public record Gear(int tier,int kind,int digit,List<Core> cores){
        public Gear{cores=List.copyOf(cores);}
        public static Gear empty(int kind){return new Gear(-1,kind,0,List.of());}
    }
    public record Snapshot(Gear mainhand,Gear offhand,Gear helmet,Gear chestplate,Gear leggings,Gear boots,boolean deep){
        public List<Gear> armor(){return List.of(helmet,chestplate,leggings,boots);}
        public List<Core> book(){return offhand.kind()==6?offhand.cores():List.of();}
        public Effects effects(){
            var tool=mainhand.kind()>=0&&mainhand.kind()<2?mainhand.cores():List.<Core>of();var book=book();double base=mainhand.tier()<0||mainhand.kind()<0||mainhand.kind()>=2?1:baseAttack(mainhand.tier(),mainhand.kind(),mainhand.digit());
            return new Effects(attack(base,mainhand.kind()==1?tool:without(tool,1),book,deep),miningMultiplier(mainhand.kind()==0?tool:List.of(),book,deep),mainhand.kind()==0?spreadBudget(tool,book,deep):0,yieldLevel(mainhand.kind()<2?tool:List.of(),book),reach(mainhand.kind()<2?tool:List.of(),book),wearFactor(tool,book),regen(book),bonusHealth(armor(),book,deep),protectionFactor(armor(),book,deep),mainhand.kind()==1?criticalCoefficient(tool,book,deep):0,projectileCoefficient(book),controlDuration(tool));
        }
        public String toJson(){return "{\"revision\":"+REVISION+",\"deep\":"+deep+",\"mainhand\":"+gearJson(mainhand)+",\"offhand\":"+gearJson(offhand)+",\"helmet\":"+gearJson(helmet)+",\"chestplate\":"+gearJson(chestplate)+",\"leggings\":"+gearJson(leggings)+",\"boots\":"+gearJson(boots)+",\"effects\":"+effects().toJson()+"}";}
    }
    public record Effects(double attack,double miningMultiplier,int extraBlocks,int yieldLevel,double reach,double wearFactor,double regeneration,double bonusHealth,double protectionFactor,double criticalCoefficient,double projectileCoefficient,double controlSeconds){
        public double actualMaxHealth(){return Math.clamp(20+bonusHealth,1,1024);}
        public double actualBlockReach(){return Math.clamp(4.5+reach,0,64);}
        public double actualEntityReach(){return Math.clamp(3+reach,0,64);}
        public String toJson(){return "{\"attack\":"+attack+",\"miningMultiplier\":"+miningMultiplier+",\"extraBlocks\":"+extraBlocks+",\"yieldLevel\":"+yieldLevel+",\"reach\":"+reach+",\"wearFactor\":"+wearFactor+",\"regeneration\":"+regeneration+",\"bonusHealth\":"+bonusHealth+",\"protectionFactor\":"+protectionFactor+",\"criticalCoefficient\":"+criticalCoefficient+",\"projectileCoefficient\":"+projectileCoefficient+",\"controlSeconds\":"+controlSeconds+",\"actualMaxHealth\":"+actualMaxHealth()+",\"actualBlockReach\":"+actualBlockReach()+",\"actualEntityReach\":"+actualEntityReach()+"}";}
    }
    public static double universalValue(int level){return switch(level){case 1->.06;case 2->.12;case 3->.25;case 4->.5;default->0;};}
    @SafeVarargs public static List<Core> join(List<Core>... lists){var all=new ArrayList<Core>();for(var list:lists)all.addAll(list);return List.copyOf(all);}
    public static List<Core> without(List<Core> cores,int type){return cores.stream().filter(c->c.type()!=type).toList();}
    private static double sum(List<Core> cores,int type,boolean square){double n=0;for(var c:cores)if(c.type()==type){double v=c.level();n+=square?v*v:v;}return n;}
    public static int highest(List<Core> cores,int type){int n=0;for(var c:cores)if(c.type()==type)n=Math.max(n,c.level());return n;}
    public static double universal(List<Core> cores,boolean deep){double n=0;for(var c:cores)if(c.type()==8){double u=universalValue(c.level());n+=deep?u:u*u;}return deep?n:Math.sqrt(n);}
    public static double fixedUniversal(List<Core> cores){return universal(cores,false);}
    public static double attack(double base,List<Core> gear,List<Core> book,boolean deep){var all=join(gear,book);double a=sum(all,1,false),u=universal(all,deep);return deep?base*(1+.25*u)+a*(1+2*u):base+a+2*u;}
    public static double miningMultiplier(List<Core> gear,List<Core> book,boolean deep){double n=0;for(var c:book)if(c.type()==2){double s=1+(c.level()-1)*.5;n+=s*s;}double k=Math.max(1,deep?n:Math.sqrt(n)),u=universal(join(gear,book),deep);return deep?k*(1+2*u):k+u;}
    public static int spreadBudget(List<Core> gear,List<Core> book,boolean deep){double n=sum(gear,2,true);return (int)(deep?n:Math.floor(Math.sqrt(n)));}
    public static int yieldLevel(List<Core> gear,List<Core> book){return (int)sum(join(gear,book),0,false);}
    public static double reach(List<Core> gear,List<Core> book){return Math.sqrt(sum(gear,3,true))+.5*fixedUniversal(join(gear,book));}
    public static double wearFactor(List<Core> gear,List<Core> book){double d=0;for(var c:gear)if(c.type()==4)d+=16*Math.pow(4,c.level()-1);return Math.max(1,d)+16*fixedUniversal(join(gear,book));}
    public static int wearCostForFactor(int amount,double factor,java.util.function.DoubleSupplier random){if(amount<=0||factor<=1)return amount;double chance=1/factor;int spent=0;for(int i=0;i<amount;i++)if(random.getAsDouble()<chance)spent++;return spent;}
    public static double regen(List<Core> book){return Math.sqrt(sum(book,4,true))+.5*fixedUniversal(book);}
    private static double armorUniversal(List<Gear> armor,List<Core> book,boolean deep){double u=0;for(var g:armor)if(g.tier()>0&&g.kind()>=2&&g.kind()<6)for(var c:g.cores())if(c.type()==8){double v=universalValue(c.level());u+=armorShare(g.kind())*(deep?v:v*v);}for(var c:book)if(c.type()==8){double v=universalValue(c.level());u+=deep?v:v*v;}return deep?u:Math.sqrt(u);}
    public static double bonusHealth(List<Gear> armor,List<Core> book,boolean deep){double h=deep?10*sum(book,7,false):4*Math.sqrt(sum(book,7,true)),u=armorUniversal(armor,book,deep);return deep?(h>0?h*(1+2*u):4*u):h+4*u;}
    public static double protectionFactor(List<Gear> armor,List<Core> book,boolean deep){double base=0,q=sum(book,6,true);for(var g:armor)if(g.tier()>0&&g.kind()>=2&&g.kind()<6){double share=armorShare(g.kind());base+=basePower(g.tier())*share;q+=share*sum(g.cores(),6,true);}double u=armorUniversal(armor,book,deep),extra=.25*(deep?q:Math.sqrt(q));extra=deep?(extra>0?extra*(1+2*u):.25*u):extra+.25*u;return (1+base/8)*(1+extra);}
    private static double critical(List<Core> cores){double n=0;for(var c:cores)if(c.type()==5){double v=.075*(c.level()+1);n+=v*v;}return Math.sqrt(n);}
    public static double criticalCoefficient(List<Core> gear,List<Core> book,boolean deep){return critical(gear);}
    public static double projectileCoefficient(List<Core> book){return critical(book);}
    public static double projectileBurst(double attack,List<Core> book,boolean deep){return attack*projectileCoefficient(book)*(deep?1+2*universal(book,true):1);}
    public static double controlDuration(List<Core> gear){int highest=highest(gear,7);return highest==0?0:Math.min(8,3*Math.sqrt(sum(gear,7,true))/highest);}
    public static double oxygenConsumption(List<Core> helmet){return switch(highest(helmet,2)){case 1->.25;case 2->1d/16;case 3->0;default->1;};}
    private static String gearJson(Gear g){StringBuilder b=new StringBuilder("{\"tier\":").append(g.tier()).append(",\"kind\":").append(g.kind()).append(",\"digit\":").append(g.digit()).append(",\"cores\":[");for(var c:g.cores()){if(b.charAt(b.length()-1)!='[')b.append(',');b.append("{\"type\":").append(c.type()).append(",\"level\":").append(c.level()).append('}');}return b.append("]}").toString();}
    public static String rulesJson(){
        StringBuilder b=new StringBuilder("{\"revision\":41,\"universal\":[0.06,0.12,0.25,0.5],\"yieldType\":0,\"wearType\":4,\"miningBookType\":2,\"spreadType\":2,\"regenBookType\":4,\"wearGrades\":[16,64,256],\"bookMiningGrades\":[1,1.5,2],\"regenGrades\":[1,2,3],\"armorShares\":[0.2,0.4,0.25,0.15],\"baseMining\":[9,11,14,18],\"baseSwordAttack\":[8,10,12,16],\"basePickAttack\":[6,8,10,12],\"baseArmorFactor\":[2,2.5,3,4],\"gearSlots\":[1,2,4,6,8],\"bookSlots\":[0,2,3,4,6],\"regionalGrades\":[1,1,2,2,3],\"bookRegionalGrades\":[0,1,2,2,3],\"bookOrdinalGrades\":[0,2,3,3,4],\"coreIds\":[");
        for(int i=0;i<CORES.length;i++){if(i>0)b.append(',');b.append('"').append(CORES[i]).append('"');}
        b.append("],\"coreTranslationKeys\":[");for(int i=0;i<CORES.length;i++){if(i>0)b.append(',');b.append("\"block.googology.").append(CORES[i]).append('"');}
        b.append("],\"compatibility\":[");for(int kind=0;kind<=6;kind++){if(kind>0)b.append(',');b.append('[');for(int type=0;type<9;type++){if(type>0)b.append(',');b.append(compatible(kind,type));}b.append(']');}
        return b.append("],\"attributeCaps\":{\"attackDamage\":2048,\"maxHealth\":1024,\"blockReach\":64,\"entityReach\":64},\"baseBlockReach\":4.5,\"baseEntityReach\":3,\"baseMaxHealth\":20,\"oxygenGrades\":[0.25,0.0625,0],\"yieldAggregation\":\"sum(level)\",\"normalAggregation\":\"sqrt(sum(value^2))\",\"deepUniversal\":\"sum(u)\",\"deepMultiplier\":\"1+2*U\",\"wearFormula\":\"max(1,sum(laverProtection))+16*sqrt(sum(u^2))\",\"numericMiningFormula\":\"4+5*digit/9\",\"numericPickAttackFormula\":\"3+digit/3\",\"numericSwordAttackFormula\":\"5+digit/3\",\"durabilityTools\":[1800,2250,2700,3600],\"durabilityArmorBase\":[400,600,550,450],\"durabilityScales\":[1,1.25,1.5,2],\"numericDurabilityFormula\":\"round(131*(2031/131)^(digit/9))\",\"regenIntervalTicks\":80,\"maxControlSeconds\":8}").toString();
    }
    public static String casesJson(){
        StringBuilder b=new StringBuilder("{\"rules\":").append(rulesJson()).append(",\"cases\":[");int count=0;
        for(boolean deep:new boolean[]{false,true})for(int tier=0;tier<=4;tier++)for(int kind=0;kind<=6;kind++){
            if(tier==0&&kind>=2)continue;var empty=Gear.empty(-1);var armor=new ArrayList<Gear>();for(int slot=2;slot<6;slot++)armor.add(tier==0?Gear.empty(slot):new Gear(tier,slot,0,List.of()));
            for(int type=-1;type<9;type++){if(type>=0&&!compatible(kind,type))continue;int level=type==8?grade(tier)+1:grade(tier);var cores=type<0?List.<Core>of():java.util.Collections.nCopies(slots(tier,kind),new Core(type,level));var g=new Gear(tier,kind,tier==0?9:0,cores);var main=kind<2?g:Gear.empty(0);var book=kind==6?g:empty;if(kind>=2&&kind<6)armor.set(kind-2,g);var state=new Snapshot(main,book,armor.get(0),armor.get(1),armor.get(2),armor.get(3),deep);if(count++>0)b.append(',');b.append("{\"id\":\"").append(deep?"deep":"normal").append('-').append(tier).append('-').append(kind).append('-').append(type).append("\",\"snapshot\":").append(state.toJson()).append('}');}
        }
        var random=new java.util.Random(311);for(int i=0;i<256;i++){var gears=new ArrayList<Gear>();for(int kind:new int[]{1,6,2,3,4,5}){int tier=1+random.nextInt(4);var cores=new ArrayList<Core>();int n=random.nextInt(slots(tier,kind)+1);for(int j=0;j<n;j++){int type;do{type=random.nextInt(9);}while(!compatible(kind,type));cores.add(new Core(type,1+random.nextInt(type==8?grade(tier)+1:grade(tier))));}gears.add(new Gear(tier,kind,0,cores));}var state=new Snapshot(gears.get(0),gears.get(1),gears.get(2),gears.get(3),gears.get(4),gears.get(5),i%2==1);if(count++>0)b.append(',');b.append("{\"id\":\"mixed-").append(i).append("\",\"snapshot\":").append(state.toJson()).append('}');}
        return b.append("]}").toString();
    }
    public static void main(String[] args){System.out.println(args.length>0&&args[0].equals("cases")?casesJson():rulesJson());}
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
