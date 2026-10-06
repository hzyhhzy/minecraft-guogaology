/* Offline mirror of EquipmentRules. The generator verifies Java golden cases. */
(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.EnhancementEngine = api;
})(typeof globalThis === 'object' ? globalThis : this, function () {
  'use strict';
  let rules = {};
  const clamp = (v, a, b) => Math.max(a, Math.min(b, Number(v) || 0));
  const list = g => g && Array.isArray(g.cores) ? g.cores : [];
  const join = (...ls) => ls.flat();
  const sum = (cs, type, square = false) => cs.reduce((n, c) => n + (c.type === type ? (square ? c.level * c.level : c.level) : 0), 0);
  const highest = (cs, type) => cs.reduce((n, c) => c.type === type ? Math.max(n, c.level) : n, 0);
  const empty = kind => ({ tier: -1, kind, digit: 0, cores: [] });
  function defaults() {
    return { mainhand: empty(0), offhand: empty(6), helmet: empty(2), chestplate: empty(3), leggings: empty(4), boots: empty(5), deep: false };
  }
  function setRules(value) { rules = value || {}; }
  function ordinalValue(level) { return (rules.universal || [.06, .12, .25, .5])[level - 1] || 0; }
  function universal(cs, deep) {
    const n = cs.reduce((n, c) => { const u = c.type === 8 ? ordinalValue(c.level) : 0; return n + (deep ? u : u * u); }, 0);
    return deep ? n : Math.sqrt(n);
  }
  const fixedUniversal = cs => universal(cs, false);
  const armorShare = kind => (rules.armorShares || [.2, .4, .25, .15])[kind - 2] || 0;
  function baseAttack(g) {
    if (g.tier < 0) return g.baseAttack === undefined ? 1 : Number(g.baseAttack);
    if (g.tier === 0) return (g.kind === 1 ? 5 : 3) + clamp(Math.floor(g.digit), 0, 9) / 3;
    return (g.kind === 1 ? rules.baseSwordAttack || [8, 10, 12, 16] : rules.basePickAttack || [6, 8, 10, 12])[g.tier - 1];
  }
  function baseMining(g) {
    if (g.tier < 0) return g.baseMining === undefined ? 1 : Number(g.baseMining);
    return g.tier === 0 ? 4 + 5 * clamp(Math.floor(g.digit), 0, 9) / 9 : (rules.baseMining || [9, 11, 14, 18])[g.tier - 1];
  }
  function attack(base, gear, book, deep) {
    const cs = join(gear, book), a = sum(cs, 1), u = universal(cs, deep);
    return deep ? base * (1 + .25 * u) + a * (1 + 2 * u) : base + a + 2 * u;
  }
  function miningMultiplier(gear, book, deep) {
    let n = 0;
    for (const c of book) if (c.type === (rules.miningBookType === undefined ? 2 : rules.miningBookType)) {
      const s = (rules.bookMiningGrades || [1, 1.5, 2])[c.level - 1]; n += s * s;
    }
    const k = Math.max(1, deep ? n : Math.sqrt(n)), u = universal(join(gear, book), deep);
    return deep ? k * (1 + 2 * u) : k + u;
  }
  function spreadBudget(gear, book, deep) {
    const n = sum(gear, rules.spreadType === undefined ? 2 : rules.spreadType, true);
    return Math.floor(deep ? n : Math.sqrt(n));
  }
  const yieldLevel = (gear, book) => sum(join(gear, book), rules.yieldType === undefined ? 0 : rules.yieldType);
  const reach = (gear, book) => Math.sqrt(sum(gear, 3, true)) + .5 * fixedUniversal(join(gear, book));
  function wearFactor(gear, book) {
    let d = 0;
    for (const c of gear) if (c.type === (rules.wearType === undefined ? 4 : rules.wearType)) d += (rules.wearGrades || [16, 64, 256])[c.level - 1];
    return Math.max(1, d) + 16 * fixedUniversal(join(gear, book));
  }
  const regen = book => Math.sqrt(sum(book, rules.regenBookType === undefined ? 4 : rules.regenBookType, true)) + .5 * fixedUniversal(book);
  function armorUniversal(armor, book, deep) {
    let n = 0;
    for (const g of armor) if (g.tier > 0 && g.kind >= 2 && g.kind < 6)
      for (const c of list(g)) if (c.type === 8) { const u = ordinalValue(c.level); n += armorShare(g.kind) * (deep ? u : u * u); }
    for (const c of book) if (c.type === 8) { const u = ordinalValue(c.level); n += deep ? u : u * u; }
    return deep ? n : Math.sqrt(n);
  }
  function bonusHealth(armor, book, deep) {
    const preview = rules.healthModel === 'manuscript_only_rms';
    const h = preview ? 4 * Math.sqrt(sum(book, 7, true)) : (deep ? 10 * sum(book, 7) : 4 * Math.sqrt(sum(book, 7, true)));
    const u = preview ? universal(book, deep) : armorUniversal(armor, book, deep);
    return deep ? (h > 0 ? h * (1 + 2 * u) : 4 * u) : h + 4 * u;
  }
  function protectionFactor(armor, book, deep) {
    let base = 0, q = sum(book, 6, true);
    for (const g of armor) if (g.tier > 0 && g.kind >= 2 && g.kind < 6) {
      const share = armorShare(g.kind);
      base += ((rules.baseArmorFactor || [2, 2.5, 3, 4])[g.tier - 1] - 1) * 8 * share;
      q += share * sum(list(g), 6, true);
    }
    const u = armorUniversal(armor, book, deep);
    let extra = .25 * (deep ? q : Math.sqrt(q));
    extra = deep ? (extra > 0 ? extra * (1 + 2 * u) : .25 * u) : extra + .25 * u;
    return (1 + base / 8) * (1 + extra);
  }
  function critical(cs) {
    return Math.sqrt(cs.reduce((n, c) => c.type === 5 ? n + (.075 * (c.level + 1)) ** 2 : n, 0));
  }
  const criticalCoefficient = gear => critical(gear);
  const projectileCoefficient = book => critical(book);
  const projectileBurst = (value, book, deep) => value * critical(book) * (deep ? 1 + 2 * universal(book, true) : 1);
  const controlDuration = gear => highest(gear, 7) ? Math.min(8, 3 * Math.sqrt(sum(gear, 7, true)) / highest(gear, 7)) : 0;
  const oxygenConsumption = helmet => [1, .25, 1 / 16, 0][highest(helmet, 2)];
  function calculate(snapshot) {
    const s = { ...defaults(), ...snapshot }, m = s.mainhand, armor = [s.helmet, s.chestplate, s.leggings, s.boots];
    const gear = m.kind >= 0 && m.kind < 2 ? list(m) : [], book = s.offhand.kind === 6 ? list(s.offhand) : [], deep = !!s.deep;
    const base = m.kind < 0 || m.kind >= 2 ? 1 : baseAttack(m);
    const effects = {
      attack: attack(base, m.kind === 1 ? gear : gear.filter(c => c.type !== 1), book, deep),
      miningMultiplier: miningMultiplier(m.kind === 0 ? gear : [], book, deep),
      extraBlocks: m.kind === 0 ? spreadBudget(gear, book, deep) : 0,
      yieldLevel: yieldLevel(m.kind < 2 ? gear : [], book), reach: reach(m.kind < 2 ? gear : [], book),
      wearFactor: wearFactor(gear, book), regeneration: regen(book), bonusHealth: bonusHealth(armor, book, deep),
      protectionFactor: protectionFactor(armor, book, deep), criticalCoefficient: m.kind === 1 ? criticalCoefficient(gear, book, deep) : 0,
      projectileCoefficient: projectileCoefficient(book), controlSeconds: controlDuration(gear)
    };
    effects.actualMaxHealth = clamp((rules.baseMaxHealth || 20) + effects.bonusHealth, 1, rules.attributeCaps?.maxHealth || 1024);
    effects.actualBlockReach = clamp((rules.baseBlockReach || 4.5) + effects.reach, 0, rules.attributeCaps?.blockReach || 64);
    effects.actualEntityReach = clamp((rules.baseEntityReach || 3) + effects.reach, 0, rules.attributeCaps?.entityReach || 64);
    return effects;
  }
  function misc(snapshot) {
    const s = { ...defaults(), ...snapshot }, book = s.offhand.kind === 6 ? list(s.offhand) : [], mode = highest(book, 3), g = highest(book, 7);
    return { maxHealth: calculate(s).actualMaxHealth, oxygenConsumption: oxygenConsumption(list(s.helmet)),
      jumpExtra: book.filter(c => c.type === 3 && c.level === 1).length,
      flight: mode >= 3 ? 'creative' : mode === 2 ? 'slow' : 'none', fallFactor: mode >= 2 ? 0 : mode === 1 ? .25 : 1,
      wallFactor: mode >= 3 ? 0 : 1, nightVision: highest(book, 4) >= 2, fireResistance: highest(book, 5) >= 2,
      autoTotem: g >= 2, immuneLevel: g, armorWear: [s.helmet, s.chestplate, s.leggings, s.boots].map(a => wearFactor(list(a), book)) };
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
    const directRaw = Math.min(effects.attack, rules.attributeCaps?.attackDamage || 2048) * scale * (p.critical && cooldown > .9 ? 1.5 : 1);
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
  return { setRules, defaults, empty, calculate, misc, damage, combat, voidForecast, SOURCES, baseAttack, baseMining,
    attack, miningMultiplier, spreadBudget, yieldLevel, reach, wearFactor, regen, bonusHealth, protectionFactor,
    criticalCoefficient, projectileCoefficient, projectileBurst, controlDuration, oxygenConsumption, universal, highest };
});
