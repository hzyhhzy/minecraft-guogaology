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
  check((await page.locator('body').innerText()).includes('尚未应用'),'proposal notice visible');
  check(!(await page.locator('#testStatus').innerText()).includes('Java'),'no false production-parity claim');
  check(await page.evaluate(()=>GuogaologySimulator.rules.proposalRevision)===46,'revision46 payload');
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
  check(Math.abs(result.miningFinal-2.2)<1e-10,'water and airborne penalties applied without manuscript multiplier');
  await page.locator('#miningApplicable').uncheck();
  result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  check(result.efficiencyBonus===0,'wrong block removes Efficiency bonus');
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
   check(result.miningFinal===18&&result.regeneration===0,'bare manuscript has no mining multiplier or independent healing '+deep);
   await apply(page,config([], [crystal(8,4)],{deep}));
   result=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
   check(result.miningFinal===18&&result.wearFactor===1&&result.reach===0&&result.actualMaxHealth===20&&result.regeneration===0,'book Ordinal excluded from five passive channels '+deep);
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
  for(const name of ['defense','combat','mining']){
   await page.locator('[data-preset="'+name+'"]').click();
   imported=await page.evaluate(()=>GuogaologySimulator.getState().snapshot);
   check(['mainhand','helmet','chestplate','leggings','boots'].every(k=>!imported[k].cores.some(c=>c.type===8)),'legal preset '+name);
  }
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
  await ref.goto(pathToFileURL(path.join(root,'docs/core-effects-reference.html')).href+'?lang=zh&realm=both&levels=1,2,3,4');
  check(await ref.locator('#overviewBody tr').count()===9,'nine overview rows');
  check(await ref.locator('#detailsBody tr[data-core]').count()===28,'28 grade rows');
  check(await ref.locator('#detailsHead th').count()===7,'both realm columns');
  check((await ref.locator('#notice').innerText()).includes('0.3.11'),'reference proposal disclaimer');
  for(const [level,value]of[[1,25],[2,50],[3,100]])
   check((await ref.locator('#detailsBody tr[data-core="1"][data-level="'+level+'"]').innerText()).includes('+'+value+'%'),'reference actual deep Power '+level);
  const ordinalIV=await ref.locator('#detailsBody tr[data-core="8"][data-level="4"]').innerText();
  check(ordinalIV.includes('2.5 HP')&&ordinalIV.includes('40%')&&ordinalIV.includes('50%')&&ordinalIV.includes('125%'),'reference four distinct Ordinal IV channels');
  for(let type=0;type<9;type++){
   await ref.locator('#coreFilter').selectOption(String(type));
   check(await ref.locator('#detailsBody tr[data-core]').count()===(type===8?4:3),'core filter '+type);
  }
  await ref.locator('#coreFilter').selectOption('-1');
  for(let level=1;level<=4;level++)await ref.locator('input[name="level"][value="'+level+'"]').setChecked(level===2);
  check(await ref.locator('#detailsBody tr[data-core]').count()===9,'Lv2 filter');
  await ref.locator('#realm').selectOption('deep');
  check(await ref.locator('#detailsHead th').count()===5,'one realm columns');
  await ref.reload();check(await ref.locator('#detailsBody tr[data-core]').count()===9,'filters reload');
  await ref.locator('#reset').click();await ref.locator('#language').click();
  check((await ref.locator('#overviewBody').innerText()).includes('Stealth')||(await ref.locator('#overviewBody').innerText()).includes('targeting'),'English roles');
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
  check(auditText.includes('36.021')&&auditText.includes('345.261'),'new direct attack maxima');
  check(auditText.includes('127.681')&&auditText.includes('187.606'),'fixed80 defense maxima');
  check(auditText.includes('48.201')&&auditText.includes('58.935'),'second-grade defense comparison');
  check(auditText.includes('16')&&auditText.includes('22')&&auditText.includes('韧性'),'native armor table present');
  check(!auditText.includes('1,222')&&!auditText.includes('517.578'),'obsolete deep calibration absent');
  const auditData=JSON.parse(fs.readFileSync(path.join(root,'build/enhancement-simulator/proposal-audit.json'),'utf8'));
  check(auditData.rules.proposalRevision===46,'audit recalculated from revision46');
  await audit.screenshot({path:path.join(out,'audit-zh.png'),fullPage:true});
  const firstLink=await audit.locator('[data-lang="zh"] table').first().locator('tbody tr').first().locator('a').first().getAttribute('href');
  const load=await browser.newPage();await load.goto(new URL(firstLink,audit.url()).href);
  result=await load.evaluate(()=>{const a=GuogaologySimulator;return a.engine.calculate(a.getState().snapshot);});
  check(Math.abs(result.actualMaxHealth-(20+12*Math.sqrt(6)))<1e-10,'audit optimal-loadout link reproduces Guogao-only health');
  await load.close();await audit.locator('#language').click();
  check(await audit.locator('[data-lang="en"]').isVisible(),'audit English visible');
  check((await audit.locator('[data-lang="en"]').innerText()).includes('per-item root-sum-square'),'audit English grouping explained');
  await audit.setViewportSize({width:390,height:844});
  check(await audit.locator('body').evaluate(e=>e.scrollWidth<=innerWidth),'audit mobile scroll contained');
  await audit.screenshot({path:path.join(out,'audit-mobile-en.png'),fullPage:true});
  check(errors.length===0,'no browser script errors: '+errors.join('; '));
  const report={ok:true,assertions:checks,errors,screenshots:out};fs.writeFileSync(path.join(out,'result.json'),JSON.stringify(report,null,2));console.log(JSON.stringify(report));
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
