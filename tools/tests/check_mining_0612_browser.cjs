/* Production mining budget and bilingual reference checks in an actual offline browser. */
'use strict';
const fs=require('fs'),path=require('path'),assert=require('assert');
const {pathToFileURL}=require('url'),{chromium}=require('playwright');
const root=path.resolve(__dirname,'../..'),out=path.join(root,'build/mining-0612/browser');
fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 let checks=0;const errors=[];const check=(ok,label)=>{assert(ok,label);checks++;};
 try{
  const page=await browser.newPage({viewport:{width:1440,height:1000}});
  page.on('pageerror',error=>errors.push(error.message));
  await page.goto(pathToFileURL(path.join(root,'docs/enhancement-simulator.html')).href);
  check(await page.evaluate(()=>GuogaologySimulator.rules.revision)===51,'Production revision51');
  for(const deep of [false,true])for(let tier=1;tier<=4;tier++){
   const actual=await page.evaluate(({deep,tier})=>{
    const api=GuogaologySimulator,state=api.engine.defaults();state.deep=deep;
    state.mainhand={tier,kind:0,cores:Array.from({length:[0,2,4,6,8][tier]},()=>({type:0,level:3}))};
    return api.engine.calculate(state).extraBlocks;
   },{deep,tier});
   check(actual===[0,5,8,9,11][tier],'Exact pick budget '+JSON.stringify({deep,tier,actual}));
  }
  for(const lang of ['zh','en']){
   await page.goto(pathToFileURL(path.join(root,'docs/core-effects-reference.html')).href+'?lang='+lang+'&realm=both&core=0');
   const text=await page.locator('#detailsBody').innerText();
   check(text.includes('1 / 2 / 4'),'Uniform budgets documented '+lang);
   check(text.includes('5×5'),'Vertical editor documented '+lang);
   check(!text.includes('25 extra blocks')&&!text.includes('25额外格'),'Old budget absent '+lang);
   check(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'No page overflow '+lang);
   await page.screenshot({path:path.join(out,'reference-'+lang+'.png')});
  }
  check(errors.length===0,'No browser errors');
  fs.writeFileSync(path.join(out,'verification.json'),JSON.stringify({success:true,checks,errors,revision:51},null,2)+'\n');
  console.log('MINING_0612_BROWSER_OK checks='+checks);
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
