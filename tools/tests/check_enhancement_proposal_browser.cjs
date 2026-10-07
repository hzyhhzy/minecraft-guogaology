/* Hidden offline browser regression; never launches Minecraft or captures the mouse. */
'use strict';
const {chromium}=require('playwright');
const fs=require('fs'),path=require('path'),{pathToFileURL}=require('url');
const root=path.resolve(__dirname,'../..');
const out=path.join(root,'build/enhancement-simulator/proposal-browser');
fs.mkdirSync(out,{recursive:true});
let checks=0;const errors=[];
const check=(condition,label)=>{checks++;if(!condition)throw Error(label);};
const crystal=(type,level=3)=>({type,level});
const close=(actual,expected,label)=>check(Math.abs(actual-expected)<1e-9*Math.max(1,Math.abs(expected)),label+': '+actual+' vs '+expected);
function config(main=[],book=[],extra={}) {return {version:1,lang:'zh',mode:'set',selected:'mainhand',snapshot:{mainhand:{tier:4,kind:0,cores:main},offhand:{tier:4,kind:6,cores:book},deep:false,...extra}};}
async function apply(page,value){
  await page.locator('#json').click();await page.locator('#jsonText').fill(JSON.stringify(value));
  await page.locator('#importJson').click();
  check(!await page.locator('#jsonDialog').isVisible(),'valid config closes dialog');
}
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try {
  const page=await browser.newPage({viewport:{width:1440,height:1100},colorScheme:'light'});
  page.on('pageerror',e=>errors.push(e.message));
  await page.goto(pathToFileURL(path.join(root,'docs/enhancement-simulator.html')).href);
  check((await page.locator('body').innerText()).includes('0.4.6'),'current production version visible');
  check((await page.locator('#testStatus').innerText()).includes('Java'),'Java golden parity status');
  check(await page.evaluate(()=>GuogaologySimulator.rules.revision)===48,'revision48 payload');
  check(await page.locator('[data-combat="endY"]').inputValue()==='-500','fresh forecast target defaults to -500');
  await page.locator('[data-combat="endY"]').fill('-750');
  check(await page.evaluate(()=>GuogaologySimulator.getState().combat.endY)===-750,'custom forecast target stays editable');
  await page.reload();
  check(await page.locator('[data-combat="endY"]').inputValue()==='-750','custom target persists through reload');
  await page.locator('#reset').click();
  check(await page.locator('[data-combat="endY"]').inputValue()==='-500','reset restores new default target');
  await apply(page,{...config([], [crystal(8,4)]),selected:'offhand'});
  let stages=await page.locator('#cores .core').first().locator('select').nth(1).locator('option').evaluateAll(es=>es.map(e=>({value:e.value,text:e.textContent})));
  check(JSON.stringify(stages.map(e=>e.value))===JSON.stringify(['1','2','3','4']),'Ordinal raw option values stay1..4');
  check(JSON.stringify(stages.map(e=>e.text))===JSON.stringify(['序数晶体','序数晶核 · Lv1','序数晶核 · Lv2','序数晶核 · Lv3']),'Chinese Ordinal stage names');
  check((await page.locator('#cores .core').first().locator('select').first().locator('option[value="8"]').innerText())==='序数晶核','Chinese Ordinal family name');
  check(!await page.locator('#socketInfo').isVisible(),'redundant grade cap is absent');
  check(await page.evaluate(()=>GuogaologySimulator.getState().snapshot.offhand.cores[0].level)===4,'import preserves raw stage4');
  await page.locator('#language').click();
  stages=await page.locator('#cores .core').first().locator('select').nth(1).locator('option').evaluateAll(es=>es.map(e=>({value:e.value,text:e.textContent})));
  check(JSON.stringify(stages.map(e=>e.text))===JSON.stringify(['Ordinal Crystal','Ordinal Core · Lv1','Ordinal Core · Lv2','Ordinal Core · Lv3']),'English Ordinal stage names');
  check(!/Lv\s*4/.test(await page.locator('body').innerText()),'English simulator has no old Lv4 label');
  await page.locator('#language').click();
  await page.locator('#cores .core').first().locator('select').nth(1).selectOption('1');
  check(await page.evaluate(()=>GuogaologySimulator.getState().snapshot.offhand.cores[0].level)===1,'ungraded crystal still stores raw stage1');
  await page.locator('#gearChoice').selectOption('1:6');
  check(await page.locator('#cores .core').first().locator('select').nth(1).locator('option').count()===4,'omega manuscript offers crystal and every Ordinal Core grade');
  check(!/Lv\s*4/.test(await page.locator('body').innerText()),'Chinese simulator has no old Lv4 label');
  for(let tier=1;tier<=4;tier++){
   await apply(page,{...config([], [crystal(8,4)],{offhand:{tier,kind:6,cores:[crystal(8,4)]}}),selected:'offhand'});
   check(await page.locator('#cores .core').count()===[2,3,4,6][tier-1],'manuscript socket count unchanged '+tier);
   check(await page.locator('#cores .core').first().locator('select').nth(1).inputValue()==='4','every manuscript tier keeps top Ordinal '+tier);
   await page.locator('#cores .core').first().locator('select').first().selectOption('6');
   await page.locator('#cores .core').first().locator('select').nth(1).selectOption('3');
   check(await page.evaluate(()=>GuogaologySimulator.engine.misc(GuogaologySimulator.getState().snapshot).flight)==='creative','every manuscript tier activates Boundary III '+tier);
   await apply(page,config([], [],{mainhand:{tier,kind:0,cores:[crystal(0)]}}));
   check(await page.locator('#cores .core').count()===[2,4,6,8][tier-1],'gear socket count unchanged '+tier);
   check(await page.locator('#cores .core').first().locator('select').nth(1).inputValue()==='3','every equipment tier keeps regional III '+tier);
  }
  await apply(page,config([],[],{mainhand:{tier:4,kind:1,sockets:[crystal(1),crystal(8,4),null,crystal(5)]},helmet:{tier:4,kind:2,sockets:[crystal(8,4),null,crystal(6)]}}));
  let imported=await page.evaluate(()=>GuogaologySimulator.getState().snapshot);
  check(imported.mainhand.sockets[1]===null&&imported.mainhand.sockets[2]===null&&imported.mainhand.sockets[3].type===5&&imported.helmet.sockets[2].type===6,'legacy Ordinal removal preserves valid cores and holes');
  check((await page.locator('#message').innerText()).includes('序数'),'legacy removal explained visibly');
  await apply(page,config([crystal(2,1),crystal(0)], [crystal(0)]));
  check(await page.locator('#silkTouch').isVisible(),'Silk switch shown with Branch');
  check(await page.locator('#silkTouch').isChecked()===false,'Fortune default');
  await page.locator('#silkTouch').check();
  let result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  check(result.silkTouch&&result.yieldLevel===0&&result.extraBlocks===3,'Silk suppresses yield but keeps extra mining');
  await page.reload();check(await page.locator('#silkTouch').isChecked(),'Silk survives reload/share fragment');
  await page.locator('#silkTouch').uncheck();
  await page.locator('#submerged').check();await page.locator('#airborne').check();
  result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  close(result.miningFinal,(18+16)*2.4/25,'water and airborne penalties applied after manuscript channels');
  close(result.miningMultiplier,2.4,'same additive mining rate region combines before multiplication');
  const rateDisplay=await page.locator('#misc dt').filter({hasText:'挖速百分比区合计'}).evaluate(el=>el.nextElementSibling.textContent);
  check(rateDisplay==='2.4×','mining rate displays one combined factor');
  await page.locator('#miningApplicable').uncheck();
  result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  check(result.efficiencyBonus===0,'wrong block removes Efficiency bonus');
  close(result.miningFinal,(1+16)*2.4/25,'wrong block uses base1 and retains manuscript bonuses');
  await page.locator('#submerged').uncheck();await page.locator('#airborne').uncheck();
  await page.locator('#gearChoice').selectOption('hand');
  result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  close(result.miningFinal,17*2.4,'hand benefits from flat and rate');
  check(!(await page.locator('#metrics .metric').nth(1).innerText()).includes('—'),'hand mining speed is displayed');
  await page.locator('#hasteLevel').fill('2');
  result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  close(result.miningFinal,17*2.8,'Haste II adds .4 to rate rather than multiplying');
  await page.locator('#fatigueLevel').fill('1');await page.locator('#otherMiningPenalty').fill('0.5');
  result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  close(result.miningFinal,17*2.8*.3*.5,'fatigue and other penalty inputs multiply last');
  await page.reload();
  check(await page.locator('#hasteLevel').inputValue()==='2'&&await page.locator('#fatigueLevel').inputValue()==='1','mining inputs persist after reload');
  await apply(page,config([],[],{mainhand:{tier:4,kind:6,cores:[crystal(7)]},offhand:{tier:-1,kind:6,cores:[]}}));
  check(await page.locator('#gearChoice').inputValue()==='4:6','main-hand manuscript selectable');
  check((await page.locator('#misc').innerText()).includes('主手'),'main-hand activation displayed');
  await apply(page,config([], [crystal(3)],{mainhand:{tier:4,kind:6,cores:[crystal(7)]}}));
  result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  check(result.activeBookSlot==='offhand'&&result.actualMaxHealth===20,'offhand wins; books do not stack or supply baseline HP');
  check((await page.locator('#misc').innerText()).includes('0%'),'stealth zero shown');
  for(const deep of [false,true]){
   await apply(page,config([],[],{deep}));
   result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
   close(result.attack,deep?14.4:14,'bare manuscript attack '+deep);
   close(result.protectionFactor,1.2,'bare manuscript protection '+deep);
   close(result.miningFinal,18*1.4,'bare manuscript innate mining multiplier '+deep);
   check(result.regeneration===0,'bare manuscript has no independent healing '+deep);
   await apply(page,config([], [crystal(8,4)],{deep}));
   result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
   close(result.miningFinal,18*1.4,'book Ordinal adds no mining beyond innate '+deep);
   check(result.wearFactor===1&&result.reach===0&&result.actualMaxHealth===20&&result.regeneration===0,'book Ordinal excluded from passive channels '+deep);
   await apply(page,config([], [crystal(4),crystal(8,4)],{deep}));
   result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
   close(result.regeneration,5.7,'Laver healing multiplied, identical realms '+deep);
   await apply(page,config([crystal(0),crystal(0)], [crystal(7),crystal(8,4)],{deep}));
   result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
   check(result.actualMaxHealth===32&&result.extraBlocks===(deep?12:4),'Guogao-only HP and common extra-block RSS '+deep);
  }
  for(const [level,value]of[[1,.25],[2,.5],[3,1]]){
   await apply(page,config([], [crystal(1,level)],{deep:true}));
   result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
   close(result.attack,12*1.2*(1+value),'deep grade-specific Power attack '+level);
  }
  for(const [level,value]of[[1,1],[2,2],[3,3]]){
   await apply(page,config([], [crystal(1,level)]));
   result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
   close(result.attack,14+value,'normal fixed HP Power '+level);
  }
  await apply(page,config([], [crystal(8,4)]));
  result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  close(result.attack,16.5,'normal Ordinal IV adds2.5HP');
  const armor=Object.fromEntries(['helmet','chestplate','leggings','boots'].map((key,i)=>[key,{tier:4,kind:i+2,cores:[]}]));
  await apply(page,config([],[],{...armor,offhand:{tier:-1,kind:6,cores:[]}}));
  result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  close(result.nativeArmor,22,'native Omega armor');close(result.nativeToughness,16,'native Omega toughness');close(result.protectionFactor,1,'no custom bare armor factor');
  check((await page.locator('#damageMetrics .metric').nth(1).innerText()).includes('2'),'native armor automatically included in incoming10HP hit');
  await page.locator('#copyDefense').click();
  let combat=await page.evaluate(()=>GuogaologySimulator.getState().combat);
  close(combat.targetArmor,22,'copy native armor');close(combat.targetToughness,16,'copy native toughness');close(combat.targetFactor,1,'copy core defense');
  await page.locator('[data-combat="ownArmor"]').fill('3');
  await page.locator('#copyDefense').click();
  combat=await page.evaluate(()=>GuogaologySimulator.getState().combat);
  close(combat.targetArmor,25,'manual additional armor and selected armor sum once');
  const mixedArmor=Object.fromEntries(['helmet','chestplate','leggings','boots'].map((key,i)=>[key,{tier:i+1,kind:i+2,cores:[]}]));
  await apply(page,config([],[],{...mixedArmor,offhand:{tier:-1,kind:6,cores:[]}}));
  const mixedEffects=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  close(mixedEffects.nativeArmor,18.7,'mixed native armor attribute remains fractional');close(mixedEffects.nativeToughness,9.4,'mixed native toughness remains fractional');
  await page.locator('[data-combat="ownArmor"]').fill('0.4');await page.locator('[data-combat="ownToughness"]').fill('0.2');
  await page.locator('#copyDefense').click();combat=await page.evaluate(()=>GuogaologySimulator.getState().combat);
  close(combat.targetArmor,19.1,'manual fraction added before native flooring');close(combat.targetToughness,9.6,'toughness is not floored');
  check((await page.locator('#damageDetails').innerText()).includes('19.1 → 19'),'effective integer armor shown with original attribute');
  await apply(page,config([],[],{mainhand:{tier:4,kind:7,cores:[crystal(2),crystal(5),crystal(7)]},offhand:{tier:-1,kind:6,cores:[]}}));
  check(await page.locator('#gearChoice').inputValue()==='4:7','ordinal bow selectable');
  check(await page.locator('#bowInputs').isVisible()&&!await page.locator('#meleeInputs').isVisible(),'bow conditions replace melee cooldown');
  const shot=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.ranged(a.getState().snapshot,a.getState().combat);});
  close(shot.directRaw,9,'bare Omega bow uses vanilla velocity damage');close(shot.burstRaw,2.7,'bow burst owns its payload');
  check(shot.arrowCount===3&&shot.targetsPerArrow===2,'Multishot and piercing shown without stacking');
  close(shot.totalHealthDamage,11.7,'one target damage not multiplied by three');
  await page.locator('[data-combat="bowCharge"]').fill('0.5');
  const partial=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.ranged(a.getState().snapshot,a.getState().combat);});
  check(partial.criticalMaximum===0,'partial draw disables vanilla critical bonus');
  await apply(page,config([], [crystal(6,2)],{mainhand:{tier:4,kind:7,cores:[]}}));
  check(await page.evaluate(()=>GuogaologySimulator.engine.misc(GuogaologySimulator.getState().snapshot).fireResistance),'Boundary manuscript fire resistance');
  check((await page.locator('#misc').innerText()).includes('1.815 / 1.815 / 1.25 / 1.25'),'Lv2 flight display halves both axes and never adds sprint speed');
  check((await page.locator('#misc').innerText()).includes('缓速飞行'),'Lv2 manuscript flight has its own mode name');
  check((await page.locator('#formula').textContent()).includes('独立于创造飞行调速滑块'),'manuscript flight independence is explained');
  for(const name of ['defense','combat','ranged','mining']){
   await page.locator('[data-preset="'+name+'"]').click();
   imported=await page.evaluate(()=>GuogaologySimulator.getState().snapshot);
   check(['mainhand','helmet','chestplate','leggings','boots'].every(k=>!imported[k].cores.some(c=>c.type===8)),'legal preset '+name);
  }
  for(const [deep,underworld]of[[false,false],[true,false],[true,true]])for(const level of [2,3]){
   await apply(page,config([], [crystal(6,level)],{deep,underworld}));
   const speeds=level===2?(deep?'3.63 / 3.63 / 2.50 / 2.50':'1.815 / 1.815 / 1.25 / 1.25'):(deep?'10.89 / 43.56 / 7.50 / 30.00':'10.89 / 21.78 / 7.50 / 15.00');
   check((await page.locator('#misc').innerText()).includes(speeds),'final per-realm flight speeds level'+level+' deep'+deep+' underworld'+underworld);
  }
  for(const [realm,deep,underworld,wall,safe]of[['normal',false,false,.5,2],['deep',true,false,.25,4],['underworld',true,true,.5,2]]){
   await apply(page,config([], [crystal(6,1),crystal(6,2)],{deep,underworld}));
   check(await page.locator('#realm').inputValue()===realm,'three-realm selection '+realm);
   const movement=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.misc(a.getState().snapshot);});
   close(movement.walkingRate,.5*Math.sqrt(2),'mixed-grade walking RSS '+realm);
   close(movement.fallFactor,0,'Lv2 fall immunity '+realm);close(movement.wallFactor,wall,'mixed Lv1 wall protection '+realm);
   close(movement.safeFallFactor,safe,'mixed Lv1 safe fall '+realm);
   check((await page.locator('#misc').innerText()).includes('步行 / 跑步加速'),'Chinese walking bonus visible');
   check((await page.locator('#formula').textContent()).includes('50%×√颗数'),'walking RSS explained');
  }
  for(const [realm,deep,underworld]of[['normal',false,false],['deep',true,false],['underworld',true,true]]){
   await page.locator('#realm').selectOption(realm);
   const s=await page.evaluate(()=>GuogaologySimulator.getState().snapshot);
   check(s.deep===deep&&s.underworld===underworld,'realm UI round trip '+realm);
  }
  for(const name of ['defense','combat','ranged','mining']){
   await page.locator('[data-preset="'+name+'"]').click();
   const s=await page.evaluate(()=>GuogaologySimulator.getState().snapshot);
   check(s.deep&&s.underworld,'Underworld retained by '+name+' preset');
   check(await page.locator('#realm').inputValue()==='underworld','preset keeps Underworld UI '+name);
  }
  await page.reload();
  check(await page.locator('#realm').inputValue()==='underworld','Underworld persists on reload');
  close(await page.evaluate(()=>GuogaologySimulator.engine.misc(GuogaologySimulator.getState().snapshot).safeFallFactor),1,'Underworld mining preset has no Inner environment bonus');
  await page.locator('[data-preset="mining"]').click();
  check((await page.locator('#message').innerText()).includes('更新'),'new mining preset legal');
  await page.screenshot({path:path.join(out,'simulator-zh.png'),fullPage:true});
  check(await page.locator('body').evaluate(e=>e.scrollWidth<=innerWidth),'desktop simulator no overflow');
  await page.locator('#language').click();
  check((await page.locator('#misc').innerText()).includes('food floor'),'English new labels');
  await page.screenshot({path:path.join(out,'simulator-en.png'),fullPage:true});
  await page.setViewportSize({width:390,height:844});
  await page.screenshot({path:path.join(out,'simulator-mobile.png'),fullPage:true});
  check(await page.locator('body').evaluate(e=>e.scrollWidth<=innerWidth),'mobile simulator no overflow');
  const ref=await browser.newPage({viewport:{width:1440,height:1100},colorScheme:'light'});ref.on('pageerror',e=>errors.push(e.message));
  await ref.goto(pathToFileURL(path.join(root,'docs/core-effects-reference.html')).href+'?lang=zh&realm=both&levels=0,1,2,3');
  check(await ref.locator('#overviewBody tr').count()===9,'nine overview rows');
  check(await ref.locator('#detailsBody tr[data-core]').count()===28,'28 grade rows');
  check(await ref.locator('#detailsHead th').count()===7,'both realm columns');
  check((await ref.locator('#notice').innerText()).includes('0.4.6'),'reference current rule notice');
  check((await ref.locator('#overviewBody').innerText()).includes('弓'),'reference includes bows');
  const freeFlightReference=await ref.locator('#detailsBody tr[data-core="6"][data-level="3"]').innerText();
  check(freeFlightReference.includes('普通飞行2倍')&&freeFlightReference.includes('普通飞行4倍'),'reference shows Outer2x and Inner/Underworld4x on both axes');
  for(let level=1;level<=3;level++){
   const boundary=await ref.locator('#detailsBody tr[data-core="6"][data-level="'+level+'"]').innerText();
   check(boundary.includes('步行/跑步速度+50%')&&boundary.includes('50%×√界限颗数'),'reference every Boundary grade walking RSS');
   check(boundary.includes('Lv1+Lv2')&&boundary.includes('安全高度×4'),'reference independent Lv1/environment stacking');
  }
  check((await ref.locator('#detailsBody tr[data-core="6"][data-level="1"]').innerText()).includes('鞘翅撞墙伤害×0.5'),'reference Lv1 halves both impacts');
  const slowFlightReference=await ref.locator('#detailsBody tr[data-core="6"][data-level="2"]').innerText();
  check(slowFlightReference.includes('1/6')&&slowFlightReference.includes('1.815')&&slowFlightReference.includes('1.25'),'reference gives slower Outer Lv2 values');
  check(slowFlightReference.includes('1/3')&&slowFlightReference.includes('3.63')&&slowFlightReference.includes('2.5'),'reference preserves Inner/Underworld Lv2 values');
  check(slowFlightReference.includes('不受创造飞行调速滑块影响'),'reference separates manuscript and creative speed');
  for(const [level,flat,rate]of[[1,4,25],[2,8,50],[3,16,100]]){
   const row=await ref.locator('#detailsBody tr[data-core="0"][data-level="'+level+'"]').innerText();
   check(row.includes('+'+flat)&&row.includes('+'+rate+'%')&&row.includes('RSS'),'reference Sequence flat and rate '+level);
  }
  const miningReference=await ref.locator('body').innerText();
  check(miningReference.includes('原始基础>1')&&miningReference.includes('空手/错误工具')&&miningReference.includes('急迫'),'reference explains Efficiency guard, hand and Haste');
  for(const [level,value]of[[1,25],[2,50],[3,100]])
   check((await ref.locator('#detailsBody tr[data-core="1"][data-level="'+level+'"]').innerText()).includes('+'+value+'%'),'reference actual deep Power '+level);
  const ordinalIV=await ref.locator('#detailsBody tr[data-core="8"][data-level="4"]').innerText();
  check(ordinalIV.includes('2.5 HP')&&ordinalIV.includes('40%')&&ordinalIV.includes('50%')&&ordinalIV.includes('125%'),'reference four distinct Ordinal IV channels');
  check((await ref.locator('#detailsBody tr[data-core="8"][data-level="1"] th').innerText())==='序数晶体','reference raw stage1 is the ungraded crystal');
  check((await ref.locator('#detailsBody tr[data-core="8"][data-level="4"] .level').innerText())==='Lv3','reference raw stage4 is display Lv3');
  await ref.locator('summary[data-i18n="limitsTitle"]').click();
  check(await ref.locator('#slotsHead th').count()===5,'reference removes redundant grade-cap columns');
  check((await ref.locator('[data-i18n="limitsHelp"]').innerText()).includes('均可用Lv1～3晶核'),'reference states unified four-tier eligibility once');
  const socketRows=await ref.locator('#slotsBody tr').evaluateAll(rows=>rows.map(row=>[...row.querySelectorAll('td')].slice(0,2).map(cell=>Number(cell.textContent))));
  check(JSON.stringify(socketRows)===JSON.stringify([[2,2],[4,3],[6,4],[8,6]]),'reference retains all four slot pairs');
  check(!/Lv\s*4/.test(await ref.locator('body').innerText()),'Chinese reference has no old Lv4');
  for(let type=0;type<9;type++){
   await ref.locator('#coreFilter').selectOption(String(type));
   check(await ref.locator('#detailsBody tr[data-core]').count()===(type===8?4:3),'core filter '+type);
  }
  await ref.locator('#coreFilter').selectOption('-1');
  for(let level=0;level<=3;level++)await ref.locator('input[name="level"][value="'+level+'"]').setChecked(level===0);
  check(await ref.locator('#detailsBody tr[data-core]').count()===1,'Crystal filter contains only the ungraded Ordinal crystal');
  check(await ref.locator('#detailsBody tr[data-core="8"][data-level="1"]').count()===1,'Crystal filter uses raw stage1');
  for(let level=0;level<=3;level++)await ref.locator('input[name="level"][value="'+level+'"]').setChecked(level===2);
  check(await ref.locator('#detailsBody tr[data-core]').count()===9,'Lv2 filter');
  check(await ref.locator('#detailsBody tr[data-core="8"][data-level="3"][data-display-grade="2"]').count()===1,'display Lv2 filter selects raw Ordinal stage3');
  await ref.locator('#realm').selectOption('deep');
  check(await ref.locator('#detailsHead th').count()===5,'one realm columns');
  await ref.reload();check(await ref.locator('#detailsBody tr[data-core]').count()===9,'filters reload');
  await ref.locator('#reset').click();await ref.locator('#language').click();
  check((await ref.locator('#overviewBody').innerText()).includes('Stealth')||(await ref.locator('#overviewBody').innerText()).includes('targeting'),'English roles');
  check((await ref.locator('#overviewBody tr[data-core="8"] th').innerText())==='Ordinal Core','English reference Ordinal family name');
  check((await ref.locator('input[name="level"][value="0"]').locator('..').innerText())==='Crystal','English crystal filter');
  check(!/Lv\s*4/.test(await ref.locator('body').innerText()),'English reference has no old Lv4');
  await ref.screenshot({path:path.join(out,'reference-en.png'),fullPage:true});
  await ref.setViewportSize({width:390,height:844});
  check(await ref.locator('body').evaluate(e=>e.scrollWidth<=innerWidth),'reference horizontal scroll contained');
  await ref.screenshot({path:path.join(out,'reference-mobile.png'),fullPage:true});
  const audit=await browser.newPage({viewport:{width:1440,height:1100},colorScheme:'light'});audit.on('pageerror',e=>errors.push(e.message));
  await audit.goto(pathToFileURL(path.join(root,'docs/enhancement-numeric-audit.html')).href);
  check(await audit.locator('[data-lang="zh"]').isVisible(),'audit Chinese visible');
  check((await audit.locator('[data-lang="zh"]').innerText()).includes('单调性'),'protection monotonicity results present');
  check((await audit.locator('[data-lang="zh"]').innerText()).includes('治疗'),'healing audit present');
  const auditText=await audit.locator('[data-lang="zh"]').innerText();
  check(auditText.includes('序数晶体')&&auditText.includes('序数晶核 · Lv3'),'Chinese audit stage names and top Core Lv3');
  check(!/Lv\s*4/.test(auditText),'Chinese audit has no old Lv4');
  check(auditText.includes('36.021')&&auditText.includes('345.261'),'new direct attack maxima');
  check(auditText.includes('127.681')&&auditText.includes('187.606'),'fixed80 defense maxima');
  check(auditText.includes('48.201')&&auditText.includes('58.935'),'second-grade defense comparison');
  check(auditText.includes('16')&&auditText.includes('22')&&auditText.includes('韧性'),'native armor table present');
  check(!auditText.includes('1,222')&&!auditText.includes('517.578'),'obsolete deep calibration absent');
  const auditData=JSON.parse(fs.readFileSync(path.join(root,'build/enhancement-simulator/proposal-audit.json'),'utf8'));
  check(auditData.rules.revision===48,'audit recalculated from revision48');
  check(auditData.rangedExamples.length===8,'bow audit scenarios use four tiers and two realms');
  close(auditData.normal.maximumMiningSpeed.value,(18+16*Math.sqrt(6))*(1.4+Math.sqrt(6)),'audit six Sequence mining optimum');
  check(auditData.miningExamples.length===16,'eight mining scenarios in each realm');
  check(auditText.includes('挖速拆解')&&auditText.includes('序列加值RSS')&&auditText.includes('急迫'),'audit mining breakdown visible');
  await audit.screenshot({path:path.join(out,'audit-zh.png'),fullPage:true});
  const firstLink=await audit.locator('[data-lang="zh"] table').first().locator('tbody tr').first().locator('a').first().getAttribute('href');
  const load=await browser.newPage();await load.goto(new URL(firstLink,audit.url()).href);
  result=await load.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  check(Math.abs(result.actualMaxHealth-(20+12*Math.sqrt(6)))<1e-10,'audit optimal-loadout link reproduces Guogao-only health');
  await load.close();await audit.locator('#language').click();
  check(await audit.locator('[data-lang="en"]').isVisible(),'audit English visible');
  check((await audit.locator('[data-lang="en"]').innerText()).includes('per-item root-sum-square'),'audit English grouping explained');
  const auditEnglish=await audit.locator('[data-lang="en"]').innerText();
  check(auditEnglish.includes('Ordinal Crystal')&&auditEnglish.includes('Ordinal Core · Lv3'),'English audit stage names');
  check(!/Lv\s*4/.test(auditEnglish),'English audit has no old Lv4');
  await audit.setViewportSize({width:390,height:844});
  check(await audit.locator('body').evaluate(e=>e.scrollWidth<=innerWidth),'audit mobile scroll contained');
  await audit.screenshot({path:path.join(out,'audit-mobile-en.png'),fullPage:true});
  check(errors.length===0,'no browser script errors: '+errors.join('; '));
  const report={ok:true,assertions:checks,errors,screenshots:out};fs.writeFileSync(path.join(out,'result.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report));
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
