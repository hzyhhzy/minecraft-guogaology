/* Focused offline-browser regression for the approved 0.6.11 combat design. */
'use strict';
const fs=require('fs'),path=require('path'),assert=require('assert'),crypto=require('crypto');
const {pathToFileURL}=require('url'),{chromium}=require('playwright');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'build/enhancement-simulator/combat-0611-browser'+(process.argv.includes('--final')?'-final':'')+(process.argv.includes('--native-float')?'-native-float':''));
fs.mkdirSync(out,{recursive:true});
const errors=[];let checks=0;
const check=(v,label)=>{assert(v,label);checks++;};
const near=(a,b,label)=>check(Math.abs(a-b)<1e-9*Math.max(1,Math.abs(b)),label+': '+a+' vs '+b);
const cores=n=>Array.from({length:n},()=>({type:1,level:3}));
function config(kind,deep,withBook,n=8,nativePower=0){return {version:1,lang:'zh',mode:'set',selected:'mainhand',snapshot:{mainhand:{tier:4,kind,cores:cores(n),nativePower},offhand:{tier:withBook?4:-1,kind:6,cores:withBook?cores(6):[]},deep},combat:{bowCharge:1,arrowCriticalBonus:0,critical:false}};}
async function apply(page,value){await page.locator('#json').click();await page.locator('#jsonText').fill(JSON.stringify(value));await page.locator('#importJson').click();check(!await page.locator('#jsonDialog').isVisible(),'Valid JSON closes import');}
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try{
  const page=await browser.newPage({viewport:{width:1440,height:1100},colorScheme:'light'});page.on('pageerror',e=>errors.push(e.message));
  await page.goto(pathToFileURL(path.join(root,'docs/enhancement-simulator.html')).href);
  check(await page.evaluate(()=>GuogaologySimulator.rules.revision)===50,'Production revision50');
  check((await page.locator('body').innerText()).includes('0.6.11'),'Current mod version displayed');
  const actual=[];
  for(const deep of [false,true])for(const [kind,power,n]of[[1,0,8],[7,0,8],[7,5,0]])for(const book of[false,true]){
   await apply(page,config(kind,deep,book,n,power));
   const value=await page.evaluate(()=>{const a=GuogaologySimulator,s=a.getCalculationSnapshot(),e=a.engine.calculate(s);return {hit:s.mainhand.kind===7?a.engine.ranged(s,a.getState().combat).directRaw:e.attack,protection:e.protectionFactor};});
   const expected=kind===1?(deep?(book?1627.0982297193164:197.01933598375618):(book?60.15278257941478:41.45584412271572)):
    power?(deep?(book?247.75713839378693:30):(book?48.696938456699066:30)):
     deep?(book?660.6857023834318:80):(book?56.696938456699066:38);
   near(value.hit,expected,'Agreed combat '+JSON.stringify({deep,kind,power,book}));near(value.protection,book?1.2:1,'Attack update leaves protection unchanged');
   actual.push({deep,kind,power,book,...value});
  }
  for(let tier=1;tier<=4;tier++){
   const v=config(7,false,false,0);v.snapshot.mainhand.tier=tier;await apply(page,v);
   const shot=await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.ranged(a.getCalculationSnapshot());});
   near(shot.launchSpeed,Math.fround([3,3.9,4.8,6][tier-1]),'Bare launch tier '+tier);near(shot.directRaw,[6,8,10,12][tier-1],'Bare hit tier '+tier);
  }
  const gamma=config(7,false,false,0);gamma.snapshot.mainhand.tier=3;gamma.snapshot.mainhand.cores=[{type:1,level:1}];await apply(page,gamma);
  near(await page.evaluate(()=>{const a=GuogaologySimulator;return a.engine.ranged(a.getCalculationSnapshot()).directRaw;}),13,'Native Gamma float ceil boundary in real page');
  check((await page.locator('#metrics .metric').first().innerText()).includes('13'),'Gamma13HP shown in rendered metric');
  await apply(page,config(7,false,true,0,5));check(await page.locator('#nativePower').inputValue()==='5','Power value in editor');
  check(await page.locator('#cores .core select').first().isDisabled(),'Enchanted bow sockets disabled');
  await page.locator('#nativePower').fill('0');await page.locator('#cores .core select').first().selectOption('1');
  check(await page.locator('#nativePower').isDisabled(),'Socketed bow Power disabled');
  const imported=config(7,false,true,1,5);await apply(page,imported);
  check(await page.locator('#configConflict').isVisible(),'Legacy core/Power conflict displayed');
  check((await page.locator('#metrics').innerText()).includes('暂不计算'),'Illegal combination is not calculated');
  await page.locator('#keepSockets').click();check(await page.locator('#nativePower').inputValue()==='0','Explicit socket path clears Power');
  await apply(page,config(7,false,true));await page.screenshot({path:path.join(out,'simulator-zh.png')});
  await page.locator('#language').click();check((await page.locator('body').innerText()).includes('Arrow coefficient'),'English coefficient label');await page.screenshot({path:path.join(out,'simulator-en.png')});
  await page.setViewportSize({width:390,height:900});check(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Simulator narrow layout avoids page overflow');await page.screenshot({path:path.join(out,'simulator-mobile.png')});
  await page.setViewportSize({width:1440,height:1100});
  await page.goto(pathToFileURL(path.join(root,'docs/core-effects-reference.html')).href+'?lang=zh&realm=both&core=1');
  const reference=await page.locator('#detailsBody').innerText();
  for(const text of['剑攻击 +3 HP','弓箭伤系数 +0.5','主手攻击 +2 HP','+400%','+200%'])check(reference.includes(text),'Reference distinct carrier value '+text);
  check(!(await page.locator('body').innerText()).includes('__PRODUCTION_VERSION__'),'No unexpanded version marker');
  await page.locator('#detailsBody').scrollIntoViewIfNeeded();await page.screenshot({path:path.join(out,'reference-zh.png')});
  await page.locator('#language').click();check((await page.locator('#detailsBody').innerText()).includes('bow arrow coefficient'),'English bow coefficient text');await page.screenshot({path:path.join(out,'reference-en.png')});
  await page.goto(pathToFileURL(path.join(root,'docs/enhancement-numeric-audit.html')).href+'?lang=zh');
  const audit=await page.locator('body').innerText();for(const text of['0.6.11','1,627.098','660.686','247.757','防护b（保持不变）'])check(audit.includes(text),'Audit updated comparison '+text);
  await page.screenshot({path:path.join(out,'audit-zh.png')});
  check(errors.length===0,'No browser exceptions: '+errors.join(';'));
  const sha256=Object.fromEntries(['docs/enhancement-simulator.html','docs/core-effects-reference.html','docs/enhancement-numeric-audit.html','tools/enhancement_engine.js'].map(file=>[file,crypto.createHash('sha256').update(fs.readFileSync(path.join(root,file))).digest('hex')]));
  fs.writeFileSync(path.join(out,'verification.json'),JSON.stringify({checks,errors,actual,version:'0.6.11',revision:50,sha256},null,2)+'\n');
  console.log('COMBAT_0611_BROWSER_OK checks='+checks+' screenshots=6');
 }finally{await browser.close();}
})().catch(error=>{console.error(error.stack);process.exitCode=1;});
