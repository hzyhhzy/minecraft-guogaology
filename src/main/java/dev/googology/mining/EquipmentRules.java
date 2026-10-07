package dev.googology.mining;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

/** Minecraft-free balance shared by gameplay and the equipment simulator. */
public final class EquipmentRules {
    private EquipmentRules() {}
    public static final int MAX_SOCKETS=10, REVISION=48, MANUSCRIPT=6, BOW=7;
    public static final String[] MINERALS={"omega","epsilon","gamma","true_omega"};
    public static final String[] CORES={"sequence_core","power_tower_core","hydra_bud","lho_trace","laver_core","astra_critical_core","boundary_core","guogao_heart","ordinal_crystal"};
    public static final String[] KINDS={"pickaxe","sword","helmet","chestplate","leggings","boots","manuscript","bow"};
    private static final int[] SLOTS={1,2,4,6,8};
    private static final double[] MINING={0,9,11,14,18}, PICK={0,6,8,10,12}, SWORD={0,8,10,12,16};
    private static final double[] SCALE={0,1,1.25,1.5,2};
    private static final double[] POWER_HP={1,2,3}, POWER_PERCENT={.25,.5,1}, ORDINAL_HP={.5,1,1.5,2.5}, ORDINAL_PERCENT={.05,.1,.2,.4}, ORDINAL_HEALING={.1,.2,.3,.5}, ORDINAL_DURATION={.25,.5,.75,1.25};
    public static int slots(int tier){return SLOTS[tier];}
    public static int slots(int tier,int kind){return kind==6?new int[]{0,2,3,4,6}[tier]:slots(tier);}
    public static int grade(int tier){return new int[]{1,3,3,3,3}[tier];}
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
    /** Internal material stage: Ordinal remains 1..4 so recipes/effects do not shift on a display rename. */
    public static int coreLevel(String id){int p=id.lastIndexOf("_lv");return p<0?1:Integer.parseInt(id.substring(p+3));}
    /** Visible grade. Ordinal stage1 is an ungraded crystal; stages2..4 are core grades1..3. */
    public static int displayedCoreGrade(int type,int materialStage){return type==8?materialStage-1:materialStage;}
    public static int stationGrade(int type,int level){return type==8?Math.max(1,level-1):level;}
    public static int denxiLevel(int type,int level){return Math.max(0,type==8?level-1:level);}
    public static double weight(int type,int level){return type==8?level*.5:1+(level-1)*.5;}
    public static boolean compatible(int kind,int type){
        if(kind<0||kind>BOW||type<0||type>8)return false;
        if(kind==MANUSCRIPT)return type!=5;
        return switch(type){case 0->kind==0;case 1,5,7->kind==1||kind==BOW;case 2->kind==0||kind==1||kind==BOW;case 3->true;case 4->kind==2||kind==5;case 6->kind>=2&&kind<=5;default->false;};
    }
    /** Bare armor has no additional custom protection factor. */
    public static double basePower(int tier){return 0;}
    public static double baseAttack(int tier,int kind,double value){if(kind!=0&&kind!=1)return 1;return tier==0?(kind==1?5:3)+digit(value)/3d:(kind==1?SWORD[tier]:PICK[tier]);}
    public static double baseMining(int tier,double value){return tier==0?4+5*digit(value)/9d:MINING[tier];}
    public static double baseDenxi(int tier,double value){return baseMining(tier,value);}
    public static double denxi(int tier,double value,int levels){return baseMining(tier,value);}
    public static double oreWork(int tier){return 3+(tier-1)*.5;}
    public static int durability(int tier,int kind){return durability(tier,kind,0);}
    public static int durability(int tier,int kind,double value){if(kind==6)return 0;if(kind==BOW)return new int[]{0,768,1152,1536,2304}[tier];return tier==0?(int)Math.round(131*Math.pow(2031d/131,digit(value)/9d)):(int)Math.round((kind<2?1800:new int[]{400,600,550,450}[kind-2])*SCALE[tier]);}
    public static double armorShare(int kind){return switch(kind){case 2->.2;case 3->.4;case 4->.25;case 5->.15;default->0;};}
    public static double nativeArmor(int tier,int kind){return tier>0?new double[]{0,16,18,20,22}[tier]*armorShare(kind):0;}
    public static double nativeToughness(int tier,int kind){return tier>0?new double[]{0,4,8,12,16}[tier]*armorShare(kind):0;}
    public static int visualArmor(int tier,int kind){return (int)Math.round(nativeArmor(tier,kind));}
    public static double arrowSpeedMultiplier(int tier){return new double[]{1,1.1,1.2,1.3,1.5}[Math.clamp(tier,0,4)];}
    public static int spreadBudget(double weight){return (int)Math.floor(weight*4);}
    public static double bookBase(int tier){return .05*Math.clamp(tier,0,4);}
    public static double bookMiningBase(int tier){return .1*Math.clamp(tier,0,4);}
    public static double bookAttackHp(int tier){return .5*Math.clamp(tier,0,4);}
    public static double bookHealingBase(int tier){return .1*Math.clamp(tier,0,4);}
    public static double bookDurationBase(int tier){return .25*Math.clamp(tier,0,4);}
    /** Minecraft-free input model, also used by the equipment simulator. */
    /** level stores the internal material stage, not the Ordinal Core display grade. */
    public record Core(int type,int level){}
    public record Gear(int tier,int kind,int digit,List<Core> cores){
        public Gear{cores=cores.stream().filter(c->compatible(kind,c.type())).toList();}
        public static Gear empty(int kind){return new Gear(-1,kind,0,List.of());}
    }
    /** Preview only the edited item, never incidental equipment currently worn by the player. */
    public static Snapshot itemPreview(Gear item,boolean deep){
        var main=Gear.empty(1);var book=Gear.empty(MANUSCRIPT);
        var head=Gear.empty(2);var chest=Gear.empty(3);var legs=Gear.empty(4);var feet=Gear.empty(5);
        switch(item.kind()){case 0,1,BOW->main=item;case 2->head=item;case 3->chest=item;case 4->legs=item;case 5->feet=item;case MANUSCRIPT->book=item;}
        return new Snapshot(main,book,head,chest,legs,feet,deep);
    }
    /** Each value is an independent +HP term outside, or a multiplicative factor inside. */
    public static List<Double> attackPreviewTerms(Gear item,boolean deep){
        var terms=new ArrayList<Double>();
        if(item.kind()==MANUSCRIPT){
            terms.add(deep?1+bookBase(item.tier()):bookAttackHp(item.tier()));
            double power=rss(item.cores(),1,deep?POWER_PERCENT:POWER_HP);
            double ordinal=rss(item.cores(),8,deep?ORDINAL_PERCENT:ORDINAL_HP);
            if(power>0)terms.add(deep?1+power:power);
            if(ordinal>0)terms.add(deep?1+ordinal:ordinal);
        }else if(item.kind()==1||item.kind()==BOW){
            double power=rss(item.cores(),1,deep?POWER_PERCENT:POWER_HP);
            if(power>0)terms.add(deep?1+power:power);
        }
        return List.copyOf(terms);
    }
    public record Snapshot(Gear mainhand,Gear offhand,Gear helmet,Gear chestplate,Gear leggings,Gear boots,boolean deep){
        public List<Gear> armor(){return List.of(helmet,chestplate,leggings,boots);}
        public Gear activeBook(){if(offhand.kind()==MANUSCRIPT&&offhand.tier()>0)return offhand;if(mainhand.kind()==MANUSCRIPT&&mainhand.tier()>0)return mainhand;return Gear.empty(MANUSCRIPT);}
        public List<Core> book(){return activeBook().cores();}
        public Effects effects(){
            int kind=mainhand.kind(),tier=activeBook().tier();boolean weapon=kind==1||kind==BOW;
            var tool=kind==0||weapon?mainhand.cores():List.<Core>of();var book=book();double base=mainhand.tier()<0?1:baseAttack(mainhand.tier(),kind,mainhand.digit());
            return new Effects(attack(base,weapon?tool:without(tool,1),book,deep,bookBase(tier),bookAttackHp(tier)),1+bookMiningBase(tier)+manuscriptMiningRate(book),kind==0?spreadBudget(tool,book,deep):0,kind<2?yieldLevel(tool,book):0,reach(tool,book),wearFactor(tool,book),regen(book,bookHealingBase(tier)),bonusHealth(armor(),book,deep),protectionFactor(armor(),book,deep,bookBase(tier)),weapon?criticalCoefficient(tool,book,deep):0,0,weapon?controlDuration(tool,book,bookDurationBase(tier)):0,boundaryWalkRate(book));
        }
        public String toJson(){return "{\"revision\":"+REVISION+",\"deep\":"+deep+",\"mainhand\":"+gearJson(mainhand)+",\"offhand\":"+gearJson(offhand)+",\"helmet\":"+gearJson(helmet)+",\"chestplate\":"+gearJson(chestplate)+",\"leggings\":"+gearJson(leggings)+",\"boots\":"+gearJson(boots)+",\"effects\":"+effects().toJson()+"}";}
    }
    public record Effects(double attack,double miningMultiplier,int extraBlocks,int yieldLevel,double reach,double wearFactor,double regeneration,double bonusHealth,double protectionFactor,double criticalCoefficient,double projectileCoefficient,double controlSeconds,double walkingRate){
        public double actualMaxHealth(){return Math.clamp(20+bonusHealth,1,1024);}
        public double actualBlockReach(){return Math.clamp(4.5+reach,0,64);}
        public double actualEntityReach(){return Math.clamp(3+reach,0,64);}
        public String toJson(){return "{\"attack\":"+attack+",\"miningMultiplier\":"+miningMultiplier+",\"extraBlocks\":"+extraBlocks+",\"yieldLevel\":"+yieldLevel+",\"reach\":"+reach+",\"wearFactor\":"+wearFactor+",\"regeneration\":"+regeneration+",\"bonusHealth\":"+bonusHealth+",\"protectionFactor\":"+protectionFactor+",\"criticalCoefficient\":"+criticalCoefficient+",\"projectileCoefficient\":"+projectileCoefficient+",\"walkingRate\":"+walkingRate+",\"controlSeconds\":"+controlSeconds+",\"actualMaxHealth\":"+actualMaxHealth()+",\"actualBlockReach\":"+actualBlockReach()+",\"actualEntityReach\":"+actualEntityReach()+"}";}
    }
    private static double grade(double[] values,int level){return level>0&&level<=values.length?values[level-1]:0;}
    private static double rss(List<Core> cores,int type,double[] values){double n=0;for(var c:cores)if(c.type()==type){double v=grade(values,c.level());n+=v*v;}return Math.sqrt(n);}
    public static double universalValue(int level){return grade(ORDINAL_PERCENT,level);}
    @SafeVarargs public static List<Core> join(List<Core>... lists){var all=new ArrayList<Core>();for(var list:lists)all.addAll(list);return List.copyOf(all);}
    public static List<Core> without(List<Core> cores,int type){return cores.stream().filter(c->c.type()!=type).toList();}
    private static double sum(List<Core> cores,int type,boolean square){double n=0;for(var c:cores)if(c.type()==type){double v=c.level();n+=square?v*v:v;}return n;}
    public static int highest(List<Core> cores,int type){int n=0;for(var c:cores)if(c.type()==type)n=Math.max(n,c.level());return n;}
    public static double universal(List<Core> cores,boolean deep){return rss(cores,8,ORDINAL_PERCENT);}
    public static double fixedUniversal(List<Core> cores){return universal(cores,false);}
    public static double attack(double base,List<Core> gear,List<Core> book,boolean deep){return attack(base,gear,book,deep,0,0);}
    public static double attack(double base,List<Core> gear,List<Core> book,boolean deep,double b,double innateHp){return deep?base*(1+b)*(1+universal(book,true))*(1+rss(book,1,POWER_PERCENT))*(1+rss(gear,1,POWER_PERCENT)):base+innateHp+rss(gear,1,POWER_HP)+rss(book,1,POWER_HP)+rss(book,8,ORDINAL_HP);}
    public static double miningMultiplier(List<Core> gear,List<Core> book,boolean deep){return 1;}
    public static double manuscriptMiningFlat(List<Core> book){return rss(book,0,new double[]{4,8,16});}
    public static double manuscriptMiningRate(List<Core> book){return rss(book,0,new double[]{.25,.5,1});}
    /** Every Boundary grade grants the same walking/sprinting rate; aggregate before multiplying movement speed. */
    public static double boundaryWalkRate(List<Core> book){return rss(book,6,new double[]{.5,.5,.5});}
    /** Called after native Efficiency, before native fatigue/water/air penalties. */
    public static double manuscriptMiningSpeed(double nativeToolAndEfficiency,int tier,List<Core> book,double hasteRate){
        return (nativeToolAndEfficiency+manuscriptMiningFlat(book))*(1+bookMiningBase(tier)+manuscriptMiningRate(book)+hasteRate);
    }
    public static int spreadBudget(List<Core> gear,List<Core> book,boolean deep){return (int)Math.floor(rss(gear,0,deep?new double[]{1,4,9}:new double[]{1,2,3}));}
    public static int yieldLevel(List<Core> gear,List<Core> book){return 2*highest(gear,2);}
    /** Use the actual vanilla effect name for this carrier, not a generic yield label. */
    public static String coreEffectKey(int kind,int type,int level){
        if(type==2){
            if(kind==0)return "enchantment.minecraft.fortune";
            if(kind==1)return "enchantment.minecraft.looting";
            if(kind==BOW)return "enchantment.minecraft."+(level>=3?"piercing":level>=2?"multishot":"infinity");
        }
        return "mining.googology.effect."+type;
    }
    public static double reach(List<Core> gear,List<Core> book){return Math.sqrt(sum(book,2,true));}
    /** Manuscript sockets never protect either the manuscript or another item from wear. */
    public static double wearFactor(int kind,List<Core> cores){return kind<0||kind==MANUSCRIPT?1:wearFactor(cores,List.of());}
    public static double wearFactor(List<Core> gear,List<Core> book){double d=0;for(var c:gear)if(c.type()==3)d+=grade(new double[]{4,16,64},c.level());return Math.max(1,d);}
    public static int wearCostForFactor(int amount,double factor,java.util.function.DoubleSupplier random){if(amount<=0||factor<=1)return amount;double chance=1/factor;int spent=0;for(int i=0;i<amount;i++)if(random.getAsDouble()<chance)spent++;return spent;}
    public static double regen(List<Core> book){return regen(book,0);}
    public static double regen(List<Core> book,double healingBase){return Math.sqrt(sum(book,4,true))*(1+healingBase+rss(book,8,ORDINAL_HEALING));}
    public static double bonusHealth(List<Gear> armor,List<Core> book,boolean deep){return 4*Math.sqrt(sum(book,7,true));}
    public static double protectionFactor(List<Gear> armor,List<Core> book,boolean deep){return protectionFactor(armor,book,deep,0);}
    public static double protectionFactor(List<Gear> armor,List<Core> book,boolean deep,double b){double a=0;for(var g:armor)if(g.tier()>0&&g.kind()>=2&&g.kind()<6)a+=armorShare(g.kind())*rss(g.cores(),6,POWER_PERCENT);double q=rss(book,6,POWER_PERCENT),u=universal(book,deep);return (1+2*a)*(deep?(1+b)*(1+q)*(1+u):1+b+q+u);}
    private static double critical(List<Core> cores){return rss(cores,5,new double[]{.15,.225,.3});}
    public static double criticalCoefficient(List<Core> gear,List<Core> book,boolean deep){return critical(gear);}
    /** Manuscripts no longer produce projectile bursts; bow payloads use their own cores. */
    public static double projectileCoefficient(List<Core> book){return 0;}
    public static double projectileBurst(double attack,List<Core> book,boolean deep){return 0;}
    public static double controlDuration(List<Core> gear){return controlDuration(gear,List.of(),0);}
    public static double controlDuration(List<Core> gear,List<Core> book,double durationBase){return highest(gear,7)>0?2*(1+durationBase+rss(book,8,ORDINAL_DURATION)):0;}
    public static double oxygenConsumption(List<Core> helmet){return switch(highest(helmet,4)){case 1->.25;case 2->1d/16;case 3->0;default->1;};}
    private static String gearJson(Gear g){StringBuilder b=new StringBuilder("{\"tier\":").append(g.tier()).append(",\"kind\":").append(g.kind()).append(",\"digit\":").append(g.digit()).append(",\"cores\":[");for(var c:g.cores()){if(b.charAt(b.length()-1)!='[')b.append(',');b.append("{\"type\":").append(c.type()).append(",\"level\":").append(c.level()).append('}');}return b.append("]}").toString();}
    public static String rulesJson(){
        StringBuilder b=new StringBuilder("{\"revision\":48,\"proposalRevision\":48,\"universal\":[0.05,0.1,0.2,0.4],\"yieldType\":2,\"wearType\":3,\"miningBookType\":0,\"spreadType\":0,\"regenBookType\":4,\"reachBookType\":2,\"flightBookType\":6,\"stealthBookType\":3,\"waterGearType\":4,\"wearGrades\":[4,16,64],\"manuscriptMiningFlatGrades\":[4,8,16],\"manuscriptMiningRateGrades\":[0.25,0.5,1],\"bookInnateMiningRate\":[0,0.1,0.2,0.3,0.4],\"yieldGrades\":[2,4,6],\"regenGrades\":[1,2,3],\"armorShares\":[0.2,0.4,0.25,0.15],\"baseMining\":[9,11,14,18],\"baseSwordAttack\":[8,10,12,16],\"basePickAttack\":[6,8,10,12],\"baseArmorFactor\":null,\"nativeArmorByTier\":[16,18,20,22],\"nativeToughnessByTier\":[4,8,12,16],\"gearSlots\":[1,2,4,6,8],\"bookSlots\":[0,2,3,4,6],\"regionalGrades\":[1,3,3,3,3],\"bookRegionalGrades\":[0,3,3,3,3],\"bookOrdinalGrades\":[0,4,4,4,4],\"attributeCaps\":{\"attackDamage\":2048,\"maxHealth\":1024,\"blockReach\":64,\"entityReach\":64},\"baseBlockReach\":4.5,\"baseEntityReach\":3,\"baseMaxHealth\":20,\"oxygenGrades\":[0.25,0.0625,0],\"yieldAggregation\":\"max(2*toolBranchLevel)+applicableNativeLevel; Silk Touch overrides Fortune\",\"normalAggregation\":\"sqrt(sum(value^2))\",\"wearFormula\":\"max(1,sum(itemAbsenceProtection))\",\"numericMiningFormula\":\"4+5*digit/9\",\"numericPickAttackFormula\":\"3+digit/3\",\"numericSwordAttackFormula\":\"5+digit/3\",\"durabilityTools\":[1800,2250,2700,3600],\"durabilityArmorBase\":[400,600,550,450],\"durabilityScales\":[1,1.25,1.5,2],\"durabilityBows\":[768,1152,1536,2304],\"bowSpeedMultipliers\":[1.1,1.2,1.3,1.5],\"numericDurabilityFormula\":\"round(131*(2031/131)^(digit/9))\",\"regenIntervalTicks\":80,\"maxControlSeconds\":null,\"normalPowerAttackGrades\":[1,2,3],\"normalOrdinalAttackGrades\":[0.5,1,1.5,2.5],\"powerPercentGrades\":[0.25,0.5,1],\"ordinalPercentGrades\":[0.05,0.1,0.2,0.4],\"ordinalHealingGrades\":[0.1,0.2,0.3,0.5],\"ordinalDurationGrades\":[0.25,0.5,0.75,1.25],\"bookInnateAttackHp\":[0,0.5,1,1.5,2],\"bookInnateAttackProtection\":[0,0.05,0.1,0.15,0.2],\"bookInnateHealing\":[0,0.1,0.2,0.3,0.4],\"bookInnateDuration\":[0,0.25,0.5,0.75,1],\"bookUniversalBase\":[0,0.05,0.1,0.15,0.2],\"armorBoundaryScale\":2,\"deepSpreadGrades\":[1,4,9],\"baseControlSeconds\":2,\"healthModel\":\"guogao_only_realm_invariant_rms\"");
        b.append(",\"boundaryWalkingRatePerCore\":0.5,\"boundaryLv1ImpactMultiplier\":0.5,\"boundaryLv1SafeFallMultiplier\":2");
        b.append(",\"coreIds\":[");for(int i=0;i<CORES.length;i++){if(i>0)b.append(',');b.append('"').append(CORES[i]).append('"');}
        b.append("],\"coreTranslationKeys\":[");for(int i=0;i<CORES.length;i++){if(i>0)b.append(',');b.append("\"block.googology.").append(CORES[i]).append('"');}
        b.append("],\"compatibility\":[");for(int kind=0;kind<=BOW;kind++){if(kind>0)b.append(',');b.append('[');for(int type=0;type<9;type++){if(type>0)b.append(',');b.append(compatible(kind,type));}b.append(']');}
        return b.append("]}").toString();
    }
    public static String casesJson(){
        StringBuilder b=new StringBuilder("{\"rules\":").append(rulesJson()).append(",\"cases\":[");int count=0;
        for(boolean deep:new boolean[]{false,true})for(int tier=0;tier<=4;tier++)for(int kind=0;kind<=BOW;kind++){
            if(tier==0&&kind>=2)continue;var empty=Gear.empty(-1);var armor=new ArrayList<Gear>();for(int slot=2;slot<6;slot++)armor.add(tier==0?Gear.empty(slot):new Gear(tier,slot,0,List.of()));
            for(int type=-1;type<9;type++){if(type>=0&&!compatible(kind,type))continue;int level=type==8?grade(tier)+1:grade(tier);var cores=type<0?List.<Core>of():java.util.Collections.nCopies(slots(tier,kind),new Core(type,level));var g=new Gear(tier,kind,tier==0?9:0,cores);var main=kind<2||kind==BOW?g:Gear.empty(0);var book=kind==6?g:empty;if(kind>=2&&kind<6)armor.set(kind-2,g);var state=new Snapshot(main,book,armor.get(0),armor.get(1),armor.get(2),armor.get(3),deep);if(count++>0)b.append(',');b.append("{\"id\":\"").append(deep?"deep":"normal").append('-').append(tier).append('-').append(kind).append('-').append(type).append("\",\"snapshot\":").append(state.toJson()).append('}');}
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
