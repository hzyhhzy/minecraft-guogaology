/* Meaningful pure-JS checks: Java golden cases, damage paths, and UI interactions. */
'use strict';
const fs = require('fs'), vm = require('vm'), assert = require('assert');
const engine = require('../enhancement_engine.js');
const [goldenPath, htmlPath] = process.argv.slice(2);
if (!goldenPath || !htmlPath) throw Error('Usage: node check_enhancement_simulator.js java-golden-cases.json page.html');
const payload = JSON.parse(fs.readFileSync(goldenPath, 'utf8'));
engine.setRules(payload.rules);
let comparisons = 0;
function near(actual, expected, label) {
  assert(Number.isFinite(actual), label + ': non-finite actual');
  assert(Math.abs(actual - expected) <= 1e-10 * Math.max(1, Math.abs(expected)), label + ': ' + actual + ' != ' + expected);
  comparisons++;
}
for (const item of payload.cases) {
  const snapshot = item.snapshot, actual = engine.calculate(snapshot);
  for (const [key, expected] of Object.entries(snapshot.effects)) near(actual[key], expected, item.id + '.' + key);
}
// Independent proposal requirements, not just a mirrored golden-case comparison.
if(payload.rules.healthModel === 'manuscript_only_rms') {
  for(const deep of [false,true]) {
    const state=engine.defaults();state.deep=deep;
    for(const [i,key] of ['helmet','chestplate','leggings','boots'].entries())
      state[key]={tier:4,kind:i+2,cores:Array.from({length:9},(_,type)=>({type,level:type===8?4:3}))};
    near(engine.calculate(state).actualMaxHealth,20,'every armor core excluded from proposal health '+deep);
    state.offhand={tier:4,kind:6,cores:[{type:7,level:3}]};
    near(engine.calculate(state).actualMaxHealth,32,'one Guogao III has identical base in both realms '+deep);
    let best=-1,max=-Infinity;
    for(let hearts=0;hearts<=6;hearts++) {
      state.offhand.cores=[...Array.from({length:hearts},()=>({type:7,level:3})),...Array.from({length:6-hearts},()=>({type:8,level:4}))];
      const hp=engine.calculate(state).actualMaxHealth;
      if(hp>max){max=hp;best=hearts;}
    }
    near(max,deep?20+60*Math.sqrt(2):20+12*Math.sqrt(6),'optimal health proposal '+deep);
    assert.equal(best,deep?2:6,'proposal optimum distribution');
  }
}
// A manuscript held in the main hand is not a passive source, even with every core type.
for (const deep of [false,true]) {
  const invalidMainBook=engine.defaults();invalidMainBook.deep=deep;
  invalidMainBook.mainhand={tier:4,kind:6,digit:0,cores:Array.from({length:9},(_,type)=>({type,level:type===8?4:3}))};
  const baseline=engine.defaults();baseline.deep=deep;
  assert.deepStrictEqual(engine.calculate(invalidMainBook),engine.calculate(baseline),'main-hand manuscript must have no core effects, realm='+deep);
  assert.equal(engine.calculate(invalidMainBook).attack,1,'main-hand book attack stays 1');
}
near(engine.damage(10, 'player_attack', { armor: 20, toughness: 8 }).healthDamage, 3, 'vanilla armor/toughness');
near(engine.damage(4, 'out_of_world', { armor: 20, toughness: 12, resistance: 5, epf: 20, factor: 4 }).healthDamage, 1, 'void ignores vanilla defenses, keeps core defense');
near(engine.damage(10, 'sonic_boom', { armor: 20, resistance: 1, epf: 20, factor: 4 }).healthDamage, 2, 'sonic ignores armor and EPF');
near(engine.damage(10, 'fall', { fallFactor: .25, factor: 2 }).healthDamage, 1.25, 'Absence fall factor applies once');
near(engine.damage(10, 'lava', { fireResistance: true }).healthDamage, 0, 'fire resistance');
near(engine.damage(10, 'player_attack', { factor: 2, absorption: 3 }).healthDamage, 2, 'absorption follows protection');
near(engine.combat({attack:10,criticalCoefficient:0},{cooldown:.5,critical:true}).directRaw,4,'low cooldown cannot produce a vanilla critical');
near(engine.combat({attack:10,criticalCoefficient:0},{cooldown:1,critical:true}).directRaw,15,'full cooldown critical');
near(engine.combat({attack:10,criticalCoefficient:.3},{cooldown:1,targetEpf:0,targetBurstEpf:20}).burst.beforeAbsorption,.6,'separate explosion protection');
const plain = engine.defaults(), fall = engine.voidForecast(plain, {});
assert(fall.dead && !fall.reached, 'Unprotected fall must die before -1000');
assert.equal(fall.hits,5,'20HP is exhausted by five 4HP void hits without healing');
const protectedState = engine.defaults();
protectedState.deep = true;
for (const [key, kind] of [['helmet',2],['chestplate',3],['leggings',4],['boots',5]])
  protectedState[key] = { tier:4, kind, digit:0, cores:Array.from({length:8},()=>({type:6,level:3})) };
protectedState.offhand = {tier:4,kind:6,digit:0,cores:[{type:4,level:3},{type:7,level:3}]};
assert(engine.voidForecast(protectedState, {}).reached, 'Protected, healed fall should reach target');
const invariant = {...protectedState,mainhand:{tier:4,kind:0,digit:0,cores:[{type:3,level:3},{type:4,level:3},{type:8,level:4}]}};
const normal = engine.calculate({...invariant,deep:false}), deep = engine.calculate({...invariant,deep:true});
for (const key of ['reach','wearFactor','regeneration']) near(normal[key],deep[key],'realm invariant '+key);

// A small deterministic DOM harness exercises local UI without opening a browser/game.
class Element {
  constructor(tag='div') {this.tagName=tag;this.children=[];this.dataset={};this.style={};this.value='';this.type='';this.checked=false;this.attributes={};this.textContent='';this.innerHTML='';this.className='';this.classList={toggle:()=>{}};}
  append(...items){this.children.push(...items);} replaceChildren(...items){this.children=[...items];}
  setAttribute(k,v){this.attributes[k]=v;} showModal(){this.open=true;} close(){this.open=false;} click(){this.onclick?.();}
}
const html = fs.readFileSync(htmlPath,'utf8');
assert(html.startsWith('<!doctype html>'), 'Standalone document required');
assert(!/<script\b[^>]*\bsrc=/i.test(html), 'No external scripts');
assert(!/<link\b[^>]*\bhref=/i.test(html), 'No external stylesheets');
assert(!/\b(?:fetch|XMLHttpRequest|WebSocket)\s*\(/.test(html), 'No network calls');
const elements=new Map(),i18n=[],inputs=[],presets=[];
for(const match of html.matchAll(/<([a-z][a-z0-9]*)\b([^>]*)>/gi)) {
  const el=new Element(match[1]),attrs=match[2];
  for(const a of attrs.matchAll(/([\w-]+)="([^"]*)"/g)){if(a[1]==='id')elements.set(a[2],el);else if(a[1].startsWith('data-'))el.dataset[a[1].slice(5)]=a[2];else el[a[1]]=a[2];}
  if(el.dataset.i18n)i18n.push(el);if(el.dataset.combat)inputs.push(el);if(el.dataset.preset)presets.push(el);
}
elements.get('ruleData').textContent=JSON.stringify(payload.rules);
const storage=new Map(),context={console,Intl,Math,JSON,Number,String,Array,Object,Boolean,Error,Set,Promise,URL,Blob,setTimeout,clearTimeout,btoa,atob,encodeURIComponent,decodeURIComponent,escape,unescape};
context.document={documentElement:{},title:'',getElementById:id=>elements.get(id)||null,createElement:tag=>new Element(tag),createTextNode:text=>text,querySelectorAll:q=>q==='[data-i18n]'?i18n:q==='[data-combat]'?inputs:q==='[data-preset]'?presets:[]};
context.localStorage={getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v)};
context.location={hash:'',href:'file:///enhancement-simulator.html'};context.history={replaceState:(_a,_b,hash)=>{context.location.hash=hash;}};
context.navigator={clipboard:{writeText:async()=>{}}};context.addEventListener=()=>{};context.window=context;
vm.createContext(context);
for(const script of html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi))if(!script[1].includes('application/json'))new vm.Script(script[2]).runInContext(context);
assert(context.GuogaologySimulator,'UI calculator initialized');
assert.equal(elements.get('metrics').children.length,6,'Initial metrics');
assert(i18n.every(el=>el.textContent!==el.dataset.i18n),'Chinese static UI labels translated');
for(const preset of presets){preset.onclick();const state=context.GuogaologySimulator.getState();for(const key of ['mainhand','offhand','helmet','chestplate','leggings','boots'])assert(state.snapshot[key].cores.length<=(key==='offhand'?6:8),'preset socket bound');assert(engine.calculate(state.snapshot).attack>0,'preset computed');}
elements.get('language').onclick();assert.equal(context.document.documentElement.lang,'en','language switch');
assert(i18n.every(el=>el.textContent!==el.dataset.i18n),'English static UI labels translated');
elements.get('mode').value='single';elements.get('mode').onchange();assert.equal(context.GuogaologySimulator.getState().mode,'single','single-item mode');
elements.get('realm').value='deep';elements.get('realm').onchange();assert(context.GuogaologySimulator.getState().snapshot.deep,'realm switch');
elements.get('json').onclick();assert(elements.get('jsonDialog').open,'JSON dialog');
const invalid=context.GuogaologySimulator.getState();delete invalid.snapshot.mainhand.sockets;invalid.snapshot.mainhand.cores=Array.from({length:20},()=>({type:2,level:99}));
elements.get('jsonText').value=JSON.stringify(invalid);elements.get('importJson').onclick();assert.equal(context.GuogaologySimulator.getState().snapshot.mainhand.cores.length,0,'invalid cores rejected');
const sparse=context.GuogaologySimulator.getState();sparse.selected='mainhand';sparse.snapshot.mainhand={tier:4,kind:0,digit:0,cores:[]};
elements.get('jsonText').value=JSON.stringify(sparse);elements.get('importJson').onclick();
let lastSocket=elements.get('cores').children[7];lastSocket.children[1].value='4';lastSocket.children[1].onchange();
assert.equal(context.GuogaologySimulator.getState().snapshot.mainhand.cores.length,1,'one newly installed core');
assert.equal(elements.get('cores').children[0].children[1].value,-1,'empty first socket stays empty');
assert.equal(elements.get('cores').children[7].children[1].value,4,'last socket stays in place');
lastSocket=elements.get('cores').children[7];lastSocket.children[1].value='-1';lastSocket.children[1].onchange();
assert.equal(context.GuogaologySimulator.getState().snapshot.mainhand.cores.length,0,'core removal leaves no ghost copy');
const invalidText=context.GuogaologySimulator.getState();elements.get('jsonText').value='{bad';elements.get('importJson').onclick();assert(elements.get('jsonError').textContent,'bad JSON reported');
assert(storage.size>0 && context.location.hash.startsWith('#config='),'local/hash persistence');
console.log('ENHANCEMENT_SIMULATOR_OK cases='+payload.cases.length+' comparisons='+comparisons+'; damage, fall, limits and local UI checks passed');
