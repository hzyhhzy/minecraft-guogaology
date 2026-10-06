/* Independent pending-design engine. Never compiled into the Minecraft Mod. */
(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.EnhancementEngine = api;
})(typeof globalThis === 'object' ? globalThis : this, function () {
  'use strict';
  let rules = {};
  const clamp = (v, a, b) => Math.max(a, Math.min(b, Number(v) || 0));
  // Legacy saved configurations may still contain equipment Ordinal cores.
  // Ignore them here as well as disallowing them in the editor compatibility table.
  const list = g => g && g.tier >= 0 && Array.isArray(g.cores)
    ? g.cores.filter(c=>c.type!==8||g.kind===6) : [];
  const sum = (cs, type, square = false) => cs.reduce((n,c) => n+(c.type===type ? (square?c.level*c.level:c.level):0),0);
  const highest = (cs,type) => cs.reduce((n,c) => c.type===type?Math.max(n,c.level):n,0);
  const empty = kind => ({tier:-1,kind,digit:0,cores:[]});
  function defaults() {return {mainhand:empty(0),offhand:empty(6),helmet:empty(2),chestplate:empty(3),leggings:empty(4),boots:empty(5),deep:false};}
  function setRules(value) {rules=value||{};}
  function activeBook(s) {
    for(const key of ['offhand','mainhand']) {const g=s[key];if(g&&g.kind===6&&g.tier>0)return g;}
    return empty(6);
  }
  const bookChannelBase = (g,field) => g&&g.kind===6&&g.tier>0?(rules[field]||[])[g.tier]||0:0;
  const bookBase = g => bookChannelBase(g,'bookInnateAttackProtection');
  const grade = (field,level) => (rules[field]||[])[level-1]||0;
  const rss = (cs,type,field) => Math.sqrt(cs.reduce((n,c)=>{
    const value=c.type===type?grade(field,c.level):0;return n+value*value;
  },0));
  function universal(cs) {return rss(cs,8,'ordinalPercentGrades');}
  const scale = (field,deep) => deep?(rules.deepScales||{})[field]||1:1;
  const armorShare = kind => (rules.armorShares||[.2,.4,.25,.15])[kind-2]||0;
  function baseAttack(g) {
    if(!g||g.kind===6||g.kind>=2)return 1;
    if(g.tier<0)return g.baseAttack===undefined?1:Number(g.baseAttack);
    if(g.tier===0)return (g.kind===1?5:3)+clamp(Math.floor(g.digit),0,9)/3;
    return (g.kind===1?rules.baseSwordAttack||[8,10,12,16]:rules.basePickAttack||[6,8,10,12])[g.tier-1];
  }
  function baseMining(g) {
    if(!g||g.kind!==0)return 1;
    if(g.tier<0)return g.baseMining===undefined?1:Number(g.baseMining);
    return g.tier===0?4+5*clamp(Math.floor(g.digit),0,9)/9:(rules.baseMining||[9,11,14,18])[g.tier-1];
  }
  function attack(base,gear,book,deep,b=0,innateHp=0) {
    if(!deep)return base+innateHp+rss(gear,1,'normalPowerAttackGrades')
      +rss(book,1,'normalPowerAttackGrades')+rss(book,8,'normalOrdinalAttackGrades');
    return base*(1+b)*(1+universal(book))*(1+rss(book,1,'powerPercentGrades'))
      *(1+rss(gear,1,'powerPercentGrades'));
  }
  function miningMultiplier() {return 1;}
  function spreadBudget(gear,book,deep) {const grades=deep?(rules.deepSpreadGrades||[1,4,9]):[1,2,3];return Math.floor(Math.sqrt(gear.reduce((n,c)=>c.type===0?n+(grades[c.level-1]||0)**2:n,0)));}
  const yieldLevel = gear => (rules.yieldGrades||[2,4,6])[highest(gear,2)-1]||0;
  const reach = (gear,book) => Math.sqrt(sum(book,2,true));
  function wearFactor(gear,book,b=0) {
    let d=0;for(const c of gear)if(c.type===3)d+=(rules.wearGrades||[4,16,64])[c.level-1];
    return Math.max(1,d);
  }
  const regen = (book,healingBase=0) => Math.sqrt(sum(book,4,true))
    *(1+healingBase+rss(book,8,'ordinalHealingGrades'));
  function bonusHealth(armor,book,deep,b=0) {
    return 4*Math.sqrt(sum(book,7,true));
  }
  function protectionFactor(armor,book,deep,b=0) {
    let armorContribution=0;
    for(const g of armor)if(g.tier>0&&g.kind>=2&&g.kind<6)
      armorContribution+=armorShare(g.kind)*rss(list(g),6,'powerPercentGrades');
    const armorFactor=1+(rules.armorBoundaryScale||2)*armorContribution;
    const q=rss(book,6,'powerPercentGrades'),u=universal(book);
    const bookFactor=deep?(1+b)*(1+q)*(1+u):1+b+q+u;
    return armorFactor*bookFactor;
  }
  function nativeArmorStats(armor) {
    let armorValue=0,toughness=0;
    for(const g of armor) {
      if(g.tier>0&&g.kind>=2&&g.kind<6){
        armorValue+=armorShare(g.kind)*((rules.nativeArmorByTier||[16,18,20,22])[g.tier-1]||0);
        toughness+=armorShare(g.kind)*((rules.nativeToughnessByTier||[4,8,12,16])[g.tier-1]||0);
      }else if(g.tier<0){
        armorValue+=Math.max(0,Number(g.nativeArmor)||0);
        toughness+=Math.max(0,Number(g.nativeToughness)||0);
      }
    }
    return {nativeArmor:armorValue,nativeToughness:toughness};
  }
  function critical(cs) {return Math.sqrt(cs.reduce((n,c)=>c.type===5?n+(.075*(c.level+1))**2:n,0));}
  const criticalCoefficient = gear => critical(gear);
  const projectileCoefficient = (book,deep=false) => critical(book)*scale('projectileBurst',deep);
  const projectileBurst = (value,book,deep,b=0) => value*projectileCoefficient(book,deep);
  const controlDuration = (gear,book=[],durationBase=0) => highest(gear,7)
    ?(rules.baseControlSeconds||2)*(1+durationBase+rss(book,8,'ordinalDurationGrades')):0;
  const oxygenConsumption = helmet => [1,.25,1/16,0][highest(helmet,4)];
  function calculate(snapshot) {
    const s={...defaults(),...snapshot},m=s.mainhand,armor=[s.helmet,s.chestplate,s.leggings,s.boots],bg=activeBook(s),book=list(bg),b=bookBase(bg),deep=!!s.deep;
    const innateHp=bookChannelBase(bg,'bookInnateAttackHp'),healingBase=bookChannelBase(bg,'bookInnateHealing'),durationBase=bookChannelBase(bg,'bookInnateDuration');
    const gear=m.kind>=0&&m.kind<2?list(m):[],pick=m.kind===0,sword=m.kind===1;
    const nativeEfficiency=m.tier<=0?clamp(m.nativeEfficiency,0,255):0;
    const manuscriptEfficiency=(rules.efficiencyGrades||[2,4,6])[highest(book,0)-1]||0;
    const efficiencyLevel=Math.max(nativeEfficiency,manuscriptEfficiency),baseSpeed=baseMining(m);
    const applicable=pick&&s.miningApplicable!==false&&baseSpeed>1;
    const efficiencyBonus=applicable&&efficiencyLevel>0?efficiencyLevel**2+1:0;
    const aquaAffinity=highest(list(s.helmet),4)>0;
    const miningMultiplierValue=miningMultiplier(pick?gear:[],book,deep,b);
    const waterPenalty=s.submerged&&!aquaAffinity?.2:1,airPenalty=s.airborne?.2:1;
    const coreYield=pick||sword?yieldLevel(gear):0;
    const silkTouch=pick&&highest(gear,2)>0&&!!m.silkTouch;
    const nativeYield=m.tier<=0?clamp(m.nativeYield,0,255):0;
    const effects={
      attack:attack(baseAttack(m),sword?gear:gear.filter(c=>c.type!==1),book,deep,b,innateHp),
      miningMultiplier:miningMultiplierValue,efficiencyLevel,efficiencyBonus,rawMining:baseSpeed+efficiencyBonus,
      miningFinal:(baseSpeed+efficiencyBonus)*miningMultiplierValue*waterPenalty*airPenalty,
      extraBlocks:pick?spreadBudget(gear,book,deep):0,
      yieldLevel:silkTouch?0:coreYield+nativeYield,coreYieldLevel:coreYield,nativeYieldLevel:nativeYield,silkTouch,
      reach:reach(gear,book),wearFactor:wearFactor(gear,book),regeneration:regen(book,healingBase),
      bonusHealth:bonusHealth(armor,book,deep,b),protectionFactor:protectionFactor(armor,book,deep,b),
      ...nativeArmorStats(armor),
      criticalCoefficient:sword?criticalCoefficient(gear):0,projectileCoefficient:projectileCoefficient(book,deep),
      projectileBurstMultiplier:projectileCoefficient(book,deep),
      controlSeconds:sword?controlDuration(gear,book,durationBase):0,bookBase:b,
      bookInnateAttackHp:innateHp,bookHealingBase:healingBase,bookDurationBase:durationBase,
      activeBookSlot:bg.tier<0?'none':bg===s.offhand?'offhand':'mainhand'
    };
    effects.totalMinedBlocks=pick?1+effects.extraBlocks:0;
    effects.actualMaxHealth=clamp((rules.baseMaxHealth||20)+effects.bonusHealth,1,rules.attributeCaps?.maxHealth||1024);
    effects.actualBlockReach=clamp((rules.baseBlockReach||4.5)+effects.reach,0,rules.attributeCaps?.blockReach||64);
    effects.actualEntityReach=clamp((rules.baseEntityReach||3)+effects.reach,0,rules.attributeCaps?.entityReach||64);
    return effects;
  }
  function misc(snapshot) {
    const s={...defaults(),...snapshot},bg=activeBook(s),book=list(bg),b=bookBase(bg),mode=highest(book,6),g=highest(book,7),hidden=highest(book,3),laver=highest(book,4);
    const sword=s.mainhand.kind===1?highest(list(s.mainhand),7):0;
    const debuffs=[[],[{effect:'slowness',level:1},{effect:'poison',level:2}],
      [{effect:'slowness',level:2},{effect:'wither',level:2},{effect:'weakness',level:1}],
      [{effect:'slowness',level:3},{effect:'wither',level:3},{effect:'weakness',level:2},{effect:'nausea',level:1},{effect:'blindness',level:1,chance:.2}]][sword];
    return {maxHealth:calculate(s).actualMaxHealth,oxygenConsumption:oxygenConsumption(list(s.helmet)),
      aquaAffinity:highest(list(s.helmet),4)>0,depthStrider:highest(list(s.boots),4),foodFloor:laver>=3?19:laver>=2?10:0,
      jumpExtra:book.filter(c=>c.type===6&&c.level===1).length,
      flight:mode>=3?'creative':mode===2?'slow':'none',fallFactor:mode>=2?0:mode===1?.25:1,
      wallFactor:mode>=3?0:1,nightVision:hidden>=2,fireResistance:highest(book,5)>=2,
      stealthLevel:hidden,detectionFactor:hidden>=2?0:hidden===1?.3:1,retaliationChance:hidden>=3?0:1,
      autoTotem:g>=2,immuneLevel:g,debuffs,debuffSeconds:calculate(s).controlSeconds,
      immunities:g>=3?['poison','hunger','weakness','nausea','slowness','wither','blindness','darkness']:g>=2?['poison','hunger','weakness']:[],
      armorWear:[s.helmet,s.chestplate,s.leggings,s.boots].map(a=>wearFactor(list(a),book,b))};
  }
  // Flags follow local Minecraft 26.2 damage_type tags. EPF is already source-specific.
  const SOURCES = {
    mob_attack: {}, player_attack: {}, arrow: {}, trident: {}, explosion: {},
    in_fire: { fire: true }, on_fire: { armor: false, fire: true }, lava: { fire: true },
    fall: { armor: false, fall: true }, fly_into_wall: { armor: false, wall: true },
    drown: { armor: false }, magic: { armor: false }, wither: { armor: false },
    sonic_boom: { armor: false, enchantments: false },
    out_of_world: { armor: false, resistance: false, enchantments: false }
  };
  function damage(raw, source, defense = {}) {
    const flags = SOURCES[source] || {}, values = [{ stage: 'raw', value: Math.max(0, Number(raw) || 0) }];
    let d = values[0].value;
    if (flags.fire && defense.fireResistance) d = 0;
    if (flags.fall) d *= defense.fallFactor === undefined ? 1 : defense.fallFactor;
    if (flags.wall) d *= defense.wallFactor === undefined ? 1 : defense.wallFactor;
    values.push({ stage: 'ability', value: d });
    if (flags.armor !== false) {
      const a = Math.max(0, Number(defense.armor) || 0), t = Math.max(0, Number(defense.toughness) || 0);
      d *= 1 - Math.min(20, Math.max(a / 5, a - d / (2 + t / 4))) / 25;
    }
    values.push({ stage: 'armor', value: d });
    if (flags.resistance !== false) d *= 1 - clamp(defense.resistance, 0, 5) * .2;
    if (flags.enchantments !== false) d *= 1 - clamp(defense.epf, 0, 20) * .04;
    values.push({ stage: 'vanilla', value: d });
    d /= Math.max(1, Number(defense.factor) || 1);
    values.push({ stage: 'cores', value: d });
    const absorbed = Math.min(d, Math.max(0, Number(defense.absorption) || 0));
    d -= absorbed; values.push({ stage: 'health', value: d });
    return { healthDamage: d, absorbed, beforeAbsorption: d + absorbed, stages: values };
  }
  function combat(effects, params) {
    const p = params || {}, cooldown = clamp(p.cooldown === undefined ? 1 : p.cooldown, 0, 1), scale = .2 + .8 * cooldown ** 2;
    const directRaw = effects.attack * scale * (p.critical && cooldown > .9 ? 1.5 : 1);
    const burstRaw = effects.attack * effects.criticalCoefficient;
    const target = { armor: p.targetArmor, toughness: p.targetToughness, resistance: p.targetResistance, epf: p.targetEpf, factor: p.targetFactor };
    const direct = damage(directRaw, 'player_attack', target), burst = damage(burstRaw, 'explosion', { ...target, epf: p.targetBurstEpf === undefined ? p.targetEpf : p.targetBurstEpf });
    const total = direct.beforeAbsorption + burst.beforeAbsorption, absorbed = Math.min(total, Math.max(0, Number(p.targetAbsorption) || 0));
    return { directRaw, burstRaw, direct, burst, totalHealthDamage: total - absorbed, absorbed };
  }
  function voidForecast(snapshot, params = {}) {
    const e = calculate(snapshot), max = e.actualMaxHealth;
    let hp = params.health > 0 ? Math.min(max, params.health) : max, shield = Math.max(0, Number(params.ownAbsorption) || 0);
    let y = Number(params.startY === undefined ? -64 : params.startY), v = Number(params.initialVelocity) || 0;
    const end = Number(params.endY === undefined ? -1000 : params.endY), threshold = Number(params.voidY === undefined ? -128 : params.voidY);
    const interval = 10, healing = 80, tps = clamp(params.tps === undefined ? 20 : params.tps, 1, 20), history = [{ seconds: 0, y, health: hp }];
    let ticks = 0, nextVoid = -1, hits = 0, reached = y <= end, dead = false;
    while (!reached && !dead && ticks < 24000) {
      ticks++; v = (v - .08) * .98; y += v;
      if (y < threshold && nextVoid < 0) nextVoid = ticks;
      if (nextVoid >= 0 && ticks >= nextVoid) {
        const hit = damage(4, 'out_of_world', { factor: e.protectionFactor, absorption: shield });
        hp -= hit.healthDamage; shield -= hit.absorbed; hits++; nextVoid += interval;
        if (hp <= 0) dead = true;
      }
      if (!dead && params.regen !== false && ticks % healing === 0) hp = Math.min(max, hp + e.regeneration);
      reached = y <= end;
      if (ticks % 10 === 0 || reached || dead) history.push({ seconds: ticks / tps, y, health: Math.max(0, hp) });
    }
    return { reached: reached && !dead, dead, health: Math.max(0, hp), seconds: ticks / tps, y, hits, maxHealth: max, history,
      netHpPerSecond: e.regeneration * tps / 80 - (4 / e.protectionFactor) * tps / 10 };
  }
  // Expected durability loss per worn piece, evaluated from the pre-hit snapshot.
  // Vanilla's integer floor is taken before custom attenuation. Fractions stay probabilities.
  function armorWearBudget(snapshot,raw,source) {
    const s={...defaults(),...snapshot},m=misc(s),e=calculate(s),flags=SOURCES[source]||{};
    const immune=(flags.fire&&m.fireResistance)||(flags.fall&&m.fallFactor===0)||(flags.wall&&m.wallFactor===0);
    const nativeBudget=flags.armor===false||immune||raw<=0?0:Math.floor(Math.max(1,Number(raw)/4));
    return [s.helmet,s.chestplate,s.leggings,s.boots].map((g,i)=>g.tier>0?nativeBudget/e.protectionFactor/m.armorWear[i]:0);
  }
  return {setRules,defaults,empty,calculate,misc,damage,combat,voidForecast,SOURCES,baseAttack,baseMining,
    activeBook,bookBase,attack,miningMultiplier,spreadBudget,yieldLevel,reach,wearFactor,regen,bonusHealth,protectionFactor,
    criticalCoefficient,projectileCoefficient,projectileBurst,controlDuration,oxygenConsumption,universal,highest,armorWearBudget,
    nativeArmorStats,bookChannelBase};
});
