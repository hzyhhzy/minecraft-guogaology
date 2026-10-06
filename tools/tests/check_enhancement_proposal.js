/* Independent revision 46 requirements; never a Minecraft/game build. */
'use strict';
const fs=require('fs'),path=require('path'),assert=require('assert'),vm=require('vm');
const root=path.resolve(__dirname,'../..');
const payload=JSON.parse(fs.readFileSync(process.argv[2]||path.join(root,'build/enhancement-simulator/proposal-rules.json'),'utf8'));
const rules=payload.rules?{...payload.rules,...payload.overlay}:payload;
const E=require('../enhancement_proposal_engine.js');E.setRules(rules);
let checks=0;const recorded=[];
function near(actual,expected,label){assert(Number.isFinite(actual),label+': finite');assert(Math.abs(actual-expected)<=1e-10*Math.max(1,Math.abs(expected)),label+': '+actual+' != '+expected);checks++;}
function eq(actual,expected,label){assert.deepStrictEqual(actual,expected,label);checks++;}
function ok(value,label){assert(value,label);checks++;}
const core=(type,level=3)=>({type,level});
const cs=(type,n,level=3)=>Array.from({length:n},()=>core(type,level));
const gear=(kind,cores=[],tier=4,extra={})=>({tier,kind,digit:0,cores,...extra});
const armorKeys=['helmet','chestplate','leggings','boots'],shares=[.2,.4,.25,.15];
const state=(deep=false,main=[],book=[])=>({...E.defaults(),deep,mainhand:gear(0,main),offhand:gear(6,book)});
function calc(s){const copy=JSON.parse(JSON.stringify(s)),e=E.calculate(s);eq(s,copy,'Non-mutating calculation');for(const[k,v]of Object.entries(e))if(typeof v==='number')ok(Number.isFinite(v),'Finite '+k);recorded.push({snapshot:copy,effects:e});return e;}
function equipArmor(s,cores=[],tier=4){armorKeys.forEach((key,i)=>s[key]=gear(i+2,JSON.parse(JSON.stringify(cores)),tier));return s;}
function withoutSlot(e){const r={...e};delete r.activeBookSlot;delete r.totalMinedBlocks;return r;}

eq(rules.proposalRevision,46,'Current proposal revision');
for(let kind=0;kind<6;kind++)eq(rules.compatibility[kind][8],false,'Ordinal incompatible with gear kind '+kind);
eq(rules.compatibility[6][8],true,'Ordinal remains manuscript-compatible');
eq(rules.nativeArmorByTier,[16,18,20,22],'Native armor totals');
eq(rules.nativeToughnessByTier,[4,8,12,16],'Native toughness totals');
eq(rules.normalOrdinalAttackGrades,[.5,1,1.5,2.5],'Confirmed normal flat Ordinal HP');

for(const deep of[false,true])for(let tier=1;tier<=4;tier++){
 const s=state(deep);s.offhand.tier=tier;const e=calc(s),b=.05*tier;
 near(e.bookBase,b,'Manuscript innate attack/protection tier '+tier);
 near(e.bookInnateAttackHp,.5*tier,'Manuscript normal HP tier '+tier);
 near(e.bookHealingBase,.1*tier,'Manuscript healing baseline tier '+tier);
 near(e.bookDurationBase,.25*tier,'Manuscript duration baseline tier '+tier);
 near(e.attack,deep?12*(1+b):12+.5*tier,'Empty manuscript actual attack');
 near(e.protectionFactor,1+b,'Empty manuscript extra protection');
 near(e.regeneration,0,'Empty manuscript cannot heal');
 near(e.controlSeconds,0,'Empty manuscript cannot inflict a debuff');
 near(e.actualMaxHealth,20,'Empty manuscript cannot add HP');
 near(e.miningMultiplier,1,'Empty manuscript has no mining multiplier');
 near(e.wearFactor,1,'Empty manuscript has no wear protection');
 near(e.reach,0,'Empty manuscript has no reach');
}

// Every attack/protection grade is applied before same-item RSS.
for(const deep of[false,true]){
 for(let level=1;level<=3;level++){
  const s=state(deep,[],[core(1,level)]),p=[.25,.5,1][level-1];
  near(calc(s).attack,deep?12*1.2*(1+p):14+level,'Book Power grade '+level);
  s.mainhand=gear(1,[core(1,level)]);
  near(calc(s).attack,deep?16*1.2*(1+p)**2:18+2*level,'Distinct sword/book Power sources');
  s.offhand.cores=[core(6,level)];
  near(calc(s).protectionFactor,deep?1.2*(1+p):1.2+p,'Book Boundary grade '+level);
 }
 for(let level=1;level<=4;level++){
  const s=state(deep,[],[core(8,level)]),u=[.05,.1,.2,.4][level-1],e=calc(s);
  near(e.attack,deep?12*1.2*(1+u):14+[.5,1,1.5,2.5][level-1],'Ordinal attack grade '+level);
  near(e.protectionFactor,deep?1.2*(1+u):1.2+u,'Ordinal protection grade '+level);
  s.offhand.cores.push(core(4,1));
  near(calc(s).regeneration,1*(1+.4+[.1,.2,.3,.5][level-1]),'Ordinal healing grade '+level);
  s.mainhand=gear(1,[core(7,1)]);
  near(calc(s).controlSeconds,2*(1+1+[.25,.5,.75,1.25][level-1]),'Ordinal duration grade '+level);
 }
 const s=state(deep,[core(1,2),core(1,3),core(8,4)],[core(1,1),core(1,3),core(8,1),core(8,4)]);
 s.mainhand.kind=1;
 near(calc(s).attack,deep?16*1.2*(1+Math.hypot(.05,.4))*(1+Math.hypot(.25,1))*(1+Math.hypot(.5,1))
   :18+Math.hypot(2,3)+Math.hypot(1,3)+Math.hypot(.5,2.5),'Per-item RSS before cross-source aggregation');
 const d=state(deep,[],[core(6,1),core(6,3),core(8,1),core(8,4)]);
 near(calc(d).protectionFactor,deep?1.2*(1+Math.hypot(.25,1))*(1+Math.hypot(.05,.4))
   :1.2+Math.hypot(.25,1)+Math.hypot(.05,.4),'Book Boundary and Ordinal remain separate sources');
}

// Four armor pieces calculate their own RSS, then weighted SUM, never another RSS.
for(const deep of[false,true]){
 const s=state(deep,[],[core(6,2),core(8,4)]);
 const pieceCores=[[core(6,1),core(6,3)],[core(6,2)],[core(6,3),core(6,3)],[]];
 armorKeys.forEach((key,i)=>s[key]=gear(i+2,pieceCores[i]));
 const armor=2*(.2*Math.hypot(.25,1)+.4*.5+.25*Math.sqrt(2));
 const book=deep?1.2*1.5*1.4:1.2+.5+.4;
 near(calc(s).protectionFactor,(1+armor)*book,'Weighted per-piece armor then manuscript multiplication');
 const blank=equipArmor(state(deep),cs(6,8));
 near(calc(blank).protectionFactor,(1+2*Math.sqrt(8))*1.2,'Eight cores per armor, shares sum to one');
 const half=equipArmor(state(deep),cs(6,8,2));
 near(calc(half).protectionFactor,(1+Math.sqrt(8))*1.2,'Armor scale affects armor only, not book');
}
// Actual native armor belongs in the vanilla stage, not custom extra protection.
for(let tier=1;tier<=4;tier++){
 const s=equipArmor(E.defaults(),[],tier),e=calc(s);
 near(e.nativeArmor,[16,18,20,22][tier-1],'Native armor tier '+tier);
 near(e.nativeToughness,[4,8,12,16][tier-1],'Native toughness tier '+tier);
 near(e.protectionFactor,1,'Bare armor has no old custom F0');
 const d=E.damage(10,'player_attack',{armor:e.nativeArmor,toughness:e.nativeToughness,factor:e.protectionFactor});
 const a=e.nativeArmor,t=e.nativeToughness;
 near(d.healthDamage,10*(1-Math.min(20,Math.max(a/5,a-10/(2+t/4)))/25),'Native armor arithmetic remains real');
}
const mixed=E.defaults();armorKeys.forEach((k,i)=>mixed[k]=gear(i+2,[],i+1));
near(calc(mixed).nativeArmor,16*.2+18*.4+20*.25+22*.15,'Mixed native armor weighted values');
near(calc(mixed).nativeToughness,4*.2+8*.4+12*.25+16*.15,'Mixed native toughness weighted values');

// Unsupported equipment Ordinal in old shared links is inert, not a hidden loophole.
for(const deep of[false,true])for(let kind=0;kind<6;kind++){
 const s=state(deep);const key=kind<2?'mainhand':armorKeys[kind-2];s[key]=gear(kind,[core(3,1)]);
 const before=calc(s);s[key].cores.push(core(8,4),core(8,4));eq(calc(s),before,'Old equipment Ordinal ignored kind '+kind);
}
for(const deep of[false,true]){
 const s=state(deep,[core(3,1)],[core(2,1),core(2,3),core(4,1),core(7,3)]);
 const before=calc(s);s.offhand.cores.push(core(8,4));const after=calc(s);
 for(const key of['miningMultiplier','miningFinal','wearFactor','reach','actualMaxHealth','extraBlocks','yieldLevel','efficiencyLevel','projectileBurstMultiplier'])
  near(after[key],before[key],'Book Ordinal excluded from '+key);
 near(after.regeneration,1*(1+.4+.5),'Only existing Laver term is multiplied');
 near(before.actualMaxHealth,32,'Only Guogao adds life');
 s.offhand.cores=[core(8,4)];near(calc(s).regeneration,0,'Ordinal without Laver never heals');
}

// Every removed or reordered channel has an independent regression check.
for(const deep of[false,true]){
 const seq=calc(state(deep,[core(0)],[]));
 near(seq.extraBlocks,deep?9:3,'Sequence pick additional mining');
 near(seq.yieldLevel,0,'Sequence does not grant Fortune');
 const branch=calc(state(deep,[core(2)],[]));near(branch.yieldLevel,6,'Branch Fortune VI');near(branch.extraBlocks,0,'Branch no extra mining');
 const absent=calc(state(deep,[core(3)],[]));near(absent.wearFactor,64,'Absence durability');near(absent.reach,0,'Absence gear no reach');
 const laver=calc(state(deep,[core(4)],[]));near(laver.wearFactor,1,'Laver gear no wear');
 const spread=calc(state(deep,[core(0),core(0)],[]));
 near(spread.extraBlocks,Math.floor(Math.sqrt(2)*(deep?9:3)),'Extra mining same-item RSS');
}
for(let level=1;level<=3;level++){
 const s=state(false,[core(2,level),core(2,level)],[core(0,level),core(0,level)]),e=calc(s);
 near(e.yieldLevel,2*level,'Same-source Fortune max');
 near(e.efficiencyLevel,2*level,'Same-source Efficiency max');
 near(e.efficiencyBonus,(2*level)**2+1,'Native Efficiency squared addend');
 s.mainhand.kind=1;near(calc(s).yieldLevel,2*level,'Looting shares Branch policy');
}
const vanilla=state(false,[],[core(0,3)]);
vanilla.mainhand=gear(0,[],-1,{baseAttack:1,baseMining:8,nativeEfficiency:5,nativeYield:3});
near(calc(vanilla).efficiencyLevel,6,'Manuscript/native Efficiency max');
near(calc(vanilla).yieldLevel,3,'Native yield exists without Branch');
vanilla.mainhand=gear(0,[core(2)],0,{nativeYield:3});
near(calc(vanilla).yieldLevel,9,'Native and crystal yield add');
vanilla.mainhand=gear(0,[core(2,1),core(0)],4,{silkTouch:true});
eq(calc(vanilla).silkTouch,true,'Branch I unlocks selected Silk');near(calc(vanilla).yieldLevel,0,'Silk overrides Fortune');
near(calc(vanilla).extraBlocks,3,'Silk retains extra mining');
vanilla.mainhand.cores=[core(0)];eq(calc(vanilla).silkTouch,false,'Removing Branch disables Silk');

// Passive manuscript fallback and offhand priority cannot duplicate bonuses.
const off={...E.defaults(),offhand:gear(6,[core(1)])};
const main={...E.defaults(),mainhand:gear(6,[core(1)])};
eq(withoutSlot(calc(main)),withoutSlot(calc(off)),'Main hand fallback equivalent passive source');
eq(calc(main).activeBookSlot,'mainhand','Fallback identified');
const both={...main,offhand:gear(6,[core(4)],1)},onlyOff={...both,mainhand:E.empty(0)};
eq(withoutSlot(calc(both)),withoutSlot(calc(onlyOff)),'Offhand wins; second manuscript inert');

// Healing and duration RSS use their own grades, not attack percentages.
for(const deep of[false,true]){
 const s=state(deep,[],[core(4,1),core(4,3),core(8,1),core(8,4)]);
 near(calc(s).regeneration,Math.sqrt(10)*(1+.4+Math.hypot(.1,.5)),'Healing two RSS groups with additive innate baseline');
 s.mainhand=gear(1,[core(7,3)]);
 near(calc(s).controlSeconds,2*(1+1+Math.hypot(.25,1.25)),'Duration distinct channel RSS');
 near(E.misc(s).debuffSeconds,calc(s).controlSeconds,'Misc reports actual duration');
 s.mainhand.cores=[];near(calc(s).controlSeconds,0,'No Guogao sword: no duration effect');
}
for(let level=1;level<=3;level++){
 const s=state(false,[],[core(4,level),core(4,level)]);
 near(calc(s).regeneration,Math.sqrt(2)*level*1.4,'Laver healing with innate IV baseline');
 const m=E.misc(s);near(m.foodFloor,[0,0,10,19][level],'Laver hunger threshold');
 s.helmet=gear(2,[core(4,level)]);s.boots=gear(5,[core(4,level)]);
 near(E.misc(s).oxygenConsumption,[1,.25,1/16,0][level],'Laver oxygen');
 eq(E.misc(s).aquaAffinity,true,'Laver Aqua Affinity');near(E.misc(s).depthStrider,level,'Laver Depth Strider');
 const normal=calc(s);s.deep=true;near(calc(s).regeneration,normal.regeneration,'Healing realm invariant');
 const control=state(false,[],[core(7,level)]);control.mainhand=gear(1,[core(7,level),core(7,level)]);
 const cm=E.misc(control),ce=calc(control);near(ce.controlSeconds,4,'Innate IV manuscript doubles existing debuff duration');
 eq(cm.debuffs.map(d=>d.effect),[[],['slowness','poison'],['slowness','wither','weakness'],['slowness','wither','weakness','nausea','blindness']][level],'Guogao status list');
 if(level===3)near(cm.debuffs.find(d=>d.effect==='blindness').chance,.2,'Blindness chance not scaled');
 if(level===2)eq(cm.immunities,['poison','hunger','weakness'],'Guogao II immunity subset');
 eq(cm.autoTotem,level>=2,'Guogao invokes inventory totem');
}
const wet=state(false,[],[core(0,3)]),dry=calc(wet).miningFinal;wet.submerged=true;
near(calc(wet).miningFinal,dry/5,'Submerged penalty');wet.helmet=gear(2,[core(4,1)]);near(calc(wet).miningFinal,dry,'Aqua Affinity removes water penalty');
wet.airborne=true;near(calc(wet).miningFinal,dry/5,'Airborne penalty retained');
wet.helmet=E.empty(2);near(calc(wet).miningFinal,dry/25,'Water and air penalties multiply');
wet.miningApplicable=false;near(calc(wet).efficiencyBonus,0,'Incorrect mining tool no Efficiency');
for(let level=1;level<=3;level++){
 const s=state(false,[],[core(3,level)]),m=E.misc(s);
 near(m.detectionFactor,level===1?.3:0,'Ordinary targeting range');near(m.retaliationChance,level===3?0:1,'Ordinary retaliation');
 eq(m.nightVision,level>=2,'Absence night vision');ok(E.damage(10,'mob_attack',{factor:calc(s).protectionFactor}).healthDamage>0,'Stealth does not negate actual damage');
}
const move=state(false,[],[core(6,1),core(6,1),core(6,2),core(6,3)]);
near(E.misc(move).jumpExtra,2,'Lv1 jumps coexist with flight');eq(E.misc(move).flight,'creative','Lv3 flight priority');
near(E.misc(move).fallFactor,0,'Flight fall immunity');near(E.misc(move).wallFactor,0,'Lv3 wall immunity');
move.offhand.cores.pop();eq(E.misc(move).flight,'slow','Lv2 flight fallback');
move.offhand.cores.pop();near(E.misc(move).fallFactor,.25,'Duplicate Lv1 fall reduction max');
const burst=state(true,[core(5),core(5)],[core(5),core(8,4)]);burst.mainhand.kind=1;
near(calc(burst).criticalCoefficient,.3*Math.sqrt(2),'Sword burst RSS');
near(calc(burst).projectileBurstMultiplier,1.2,'Deep projectile quantity without universal boost');
eq(E.misc(burst).fireResistance,true,'Critical II+ fire resistance');

// Source flags keep native protection and custom extra protection separate.
near(E.damage(10,'player_attack',{armor:20,toughness:8}).healthDamage,3,'Native damage equation');
near(E.damage(4,'out_of_world',{armor:22,toughness:16,resistance:5,epf:20,factor:4}).healthDamage,1,'Void uses custom protection, not armor or EPF');
near(E.damage(10,'sonic_boom',{armor:22,resistance:1,epf:20,factor:4}).healthDamage,2,'Sonic bypasses armor and EPF');
near(E.damage(10,'fall',{fallFactor:.25,factor:2}).healthDamage,1.25,'Fall ability and core layers multiply');
near(E.damage(10,'lava',{fireResistance:true}).healthDamage,0,'Fire immunity');
const worn=equipArmor(state(true,[],[core(6),core(8,4)]),[core(6),core(3)]),we=calc(worn),wm=E.misc(worn);
E.armorWearBudget(worn,10,'mob_attack').forEach((n,i)=>near(n,2/we.protectionFactor/wm.armorWear[i],'Pre-hit fractional armor wear'));
eq(E.armorWearBudget(worn,100,'sonic_boom'),[0,0,0,0],'Sonic no armor wear');
eq(E.armorWearBudget(worn,100,'out_of_world'),[0,0,0,0],'Void no armor wear');
const huge=state(true,[],[...cs(7,8000),...cs(2,2000),...cs(8,2000,4)]),he=calc(huge);
near(he.actualMaxHealth,rules.attributeCaps?.maxHealth||1024,'Actual max health cap');
near(he.actualBlockReach,rules.attributeCaps?.blockReach||64,'Block reach cap');
near(he.actualEntityReach,rules.attributeCaps?.entityReach||64,'Entity reach cap');
near(E.combat({...he,attack:3000,criticalCoefficient:0},{cooldown:1}).directRaw,3000,'Actual compensated attack not capped by display attribute');
ok(E.voidForecast(E.defaults(),{}).dead,'Unprotected void lethal');

// A compact sweep catches non-finite, mutable or decreasing grade effects.
for(const deep of[false,true])for(let type=0;type<=8;type++)for(let level=1;level<=(type===8?4:3);level++){
 const s=state(deep,[],[core(type,level)]);equipArmor(s,[core(6,1)]);
 const e=calc(s);ok(e.protectionFactor>=1,'Every legal grade preserves nonnegative protection');
 if(type===7)near(e.actualMaxHealth,20+4*level,'Realm-invariant Guogao life per grade');
}
const htmlPath=process.argv[3];
if(htmlPath){
 const html=fs.readFileSync(htmlPath,'utf8');ok(html.startsWith('<!doctype html>'),'Standalone HTML');ok(!/<script\b[^>]*\bsrc=/i.test(html),'No external scripts');
 const scripts=[...html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi)];
 const script=scripts.find(m=>!m[1].includes('application/json')&&m[2].includes('Independent pending-design engine'));
 ok(script,'Proposal engine embedded');const context={Math,Number,Array,Object,Boolean,String};context.globalThis=context;
 vm.createContext(context);new vm.Script(script[2]).runInContext(context);context.EnhancementEngine.setRules(rules);
 for(const c of recorded)eq(JSON.parse(JSON.stringify(context.EnhancementEngine.calculate(c.snapshot))),c.effects,'Embedded engine parity');
}
console.log('ENHANCEMENT_PROPOSAL_CHECK_OK assertions='+checks+' configurations='+recorded.length+(htmlPath?' embeddedHtml=true':''));

