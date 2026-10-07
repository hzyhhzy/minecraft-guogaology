/* Reproducible offline extrema audit of production revision48.
 * Run from any directory with Node; no Minecraft, browser, or third-party package.
 * Legacy proposal filenames are aliases of the production rules and engine.
 */
'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const assert = require('assert');

const repo = path.resolve(__dirname, '../..');
const rulesPath = path.join(repo, 'build/enhancement-simulator/proposal-rules.json');
const enginePath = path.join(repo, 'tools/enhancement_proposal_engine.js');
const outputPath = path.join(repo, 'build/enhancement-simulator/proposal-audit.json');
for (const file of [rulesPath, enginePath]) {
  if (!fs.existsSync(file)) throw new Error('Proposal input not ready: ' + file);
}
const payload = JSON.parse(fs.readFileSync(rulesPath, 'utf8'));
const rules = payload.rules || payload;
assert.equal(rules.revision, 48, 'Current production rules are required');
const engine = require(enginePath);
assert.equal(typeof engine.calculate, 'function', 'Proposal engine calculate API');
assert.equal(typeof engine.defaults, 'function', 'Proposal engine defaults API');
engine.setRules(rules);

const T = { sequence: 0, power: 1, branch: 2, empty: 3, laver: 4, critical: 5, boundary: 6, guogao: 7, ordinal: 8 };
const armorKeys = ['helmet', 'chestplate', 'leggings', 'boots'];
const armorShares = rules.armorShares || [.2, .4, .25, .15];
const now = Date.now();
let evaluations = 0;

function crystals(type, n) {
  return Array.from({ length: n }, () => ({ type, level: type === T.ordinal ? 4 : 3 }));
}
function mix(...terms) {
  return terms.flatMap(([type, n]) => crystals(type, n));
}
function item(kind, cores = [], extra = {}) {
  return { tier: 4, kind, digit: 0, cores, ...extra };
}
function snapshot(deep, sword = true, main = [], book = [], armor) {
  const s = engine.defaults();
  s.deep = deep;
  s.mainhand = item(sword ? 1 : 0, main);
  s.offhand = item(6, book);
  if (armor) armorKeys.forEach((key, i) => { s[key] = item(i + 2, armor[i]); });
  return s;
}
function evaluate(s) {
  for (const key of ['mainhand', 'offhand', ...armorKeys]) {
    const g = s[key];
    assert((g.cores || []).every(c => rules.compatibility[g.kind]?.[c.type]), 'Core compatibility: ' + key);
  }
  evaluations++;
  const e = engine.calculate(s);
  for (const key of ['attack', 'actualMaxHealth', 'protectionFactor'])
    assert(Number.isFinite(e[key]), 'Non-finite engine metric: ' + key);
  return e;
}
function winner() { return { value: -Infinity, configuration: null }; }
function retain(best, value, s, e, detail = {}) {
  assert(Number.isFinite(value), 'Audit metric must be finite');
  if (value > best.value + 1e-10 * Math.max(1, Math.abs(value))) {
    best.value = value;
    best.configuration = s;
    best.effects = e;
    best.detail = detail;
  }
}
function finish(best) {
  assert(best.configuration, 'At least one admissible configuration must be evaluated');
  if (engine.misc) best.misc = engine.misc(best.configuration);
  best.itemCounts = Object.fromEntries(['mainhand', 'offhand', ...armorKeys].map(key => {
    const g = best.configuration[key], counts = {};
    for (const c of g.cores || []) {
      const name = (rules.coreNames?.zh || [])[c.type] || Object.keys(T).find(k => T[k] === c.type);
      const label = rules.coreStageNames?.zh?.[c.type]?.[c.level-1]
        || (c.type===8&&c.level===1?'序数晶体':name+' Lv'+(c.type===8?c.level-1:c.level));
      counts[label] = (counts[label] || 0) + 1;
    }
    return [key, counts];
  }));
  return best;
}
function* armorDistributions() {
  // Independent positive per-piece Boundary contributions are monotone.
  // All eight Boundary cores per piece maximize this component; see the
  // independent per-piece and manuscript empty-slot checks below.
  yield { counts: [8, 8, 8, 8], weightedBoundarySlots: 8,
    cores: armorKeys.map(() => crystals(T.boundary, 8)) };
}
function* bookDefenseDistributions() {
  for (let hearts = 0; hearts <= 6; hearts++)
    for (let protection = 0; protection <= 6 - hearts; protection++) {
      const ordinals = 6 - hearts - protection;
      yield { hearts, protection, ordinals,
        cores: mix([T.guogao, hearts], [T.boundary, protection], [T.ordinal, ordinals]) };
    }
}
function physicalHit(e, critical = true) {
  if (engine.combat) {
    const c = engine.combat(e, { cooldown: 1, critical, targetArmor: 0,
      targetToughness: 0, targetResistance: 0, targetEpf: 0, targetBurstEpf: 0,
      targetFactor: 1, targetAbsorption: 0 });
    if (Number.isFinite(c.totalHealthDamage)) return c.totalHealthDamage;
  }
  return e.attack * ((critical ? 1.5 : 1) + (e.criticalCoefficient || 0));
}

function auditRealm(deep) {
const best = { maximumHealth: winner(), maximumAttack: winner(), maximumHitWithBurst: winner(), maximumCriticalHitWithBurst: winner(),
    maximumProtection: winner(), maximumSingleHitThreshold: winner(), maximumWearProtection: winner(),
    maximumExtraBlocks: winner(), maximumReach: winner(), maximumPeriodicHealing: winner(), maximumMiningSpeed: winner(), maximumGuogaoDuration: winner() };

  // Six-slot life optimization; armor is excluded from health by proposal design.
  for (let hearts = 0; hearts <= 6; hearts++) {
    const s = snapshot(deep, true, [], mix([T.guogao, hearts], [T.ordinal, 6 - hearts]));
    const e = evaluate(s);
    retain(best.maximumHealth, e.actualMaxHealth, s, e);
  }

  // Eight sword slots and six manuscript slots. A Critical core has a useful
  // effect only on the sword, so the manuscript candidates are power/ordinal.
  for (let critical = 0; critical <= 8; critical++)
    for (let power = 0; power <= 8 - critical; power++)
      for (let bookPower = 0; bookPower <= 6; bookPower++) {
        const s = snapshot(deep, true,
          mix([T.power, power], [T.critical, critical]),
          mix([T.power, bookPower], [T.ordinal, 6 - bookPower]));
        const e = evaluate(s);
        retain(best.maximumAttack, e.attack, s, e);
        retain(best.maximumHitWithBurst, physicalHit(e, false), s, e);
        retain(best.maximumCriticalHitWithBurst, physicalHit(e), s, e);
      }

  // Independent positive armor contributions use the proven all-Boundary
  // optimum, paired with all 28 relevant manuscript count allocations.
  const books = [...bookDefenseDistributions()];
  for (const arm of armorDistributions()) {
    for (const book of books) {
      const s = snapshot(deep, true, [], book.cores, arm.cores);
      const e = evaluate(s);
      const d = { armorBoundaryCounts: arm.counts, weightedBoundarySlots: arm.weightedBoundarySlots,
        manuscriptHearts: book.hearts, manuscriptBoundary: book.protection, manuscriptOrdinal: book.ordinals };
      retain(best.maximumProtection, e.protectionFactor, s, e, d);
      retain(best.maximumSingleHitThreshold, 5 * e.actualMaxHealth * e.protectionFactor, s, e,
        { ...d, fixed80ProtectionFactor: 5 * e.protectionFactor });
    }
  }

  // Realm-invariant durability uses only the corresponding item's cores. Search
  // same-item Empty choices, never an invalid armor-wide wear sum.
  for (let empty = 0; empty <= 8; empty++) {
    const s = snapshot(deep, true, crystals(T.empty, empty), crystals(T.ordinal, 6));
    const e = evaluate(s);
    retain(best.maximumWearProtection, e.wearFactor, s, e, { eachWearPointConsumptionProbability: 1 / e.wearFactor });
  }

  for (let sequence = 0; sequence <= 8; sequence++) {
    const s = snapshot(deep, false, crystals(T.sequence, sequence), crystals(T.ordinal, 6));
    const e = evaluate(s);
    retain(best.maximumExtraBlocks, e.extraBlocks, s, e, { totalBlocksPerOperation: 1 + e.extraBlocks });
  }
  for (let branch = 0; branch <= 6; branch++) {
    const s = snapshot(deep, false, [], mix([T.branch, branch], [T.ordinal, 6 - branch]));
    const e = evaluate(s);
    retain(best.maximumReach, e.reach, s, e, { actualBlockReach: e.actualBlockReach, actualEntityReach: e.actualEntityReach });
  }
  for (let laver = 0; laver <= 6; laver++) {
    const s = snapshot(deep, true, [], mix([T.laver, laver], [T.ordinal, 6 - laver]));
    const e = evaluate(s);
    retain(best.maximumPeriodicHealing, e.regeneration, s, e, {
      intervalTicks: rules.regenIntervalTicks || 80,
      hpPerSecondAt20TPS: e.regeneration * 20 / (rules.regenIntervalTicks || 80),
      vanillaNaturalRegenerationIncluded: false
    });
  }
  // Sequence flat/rate values form two independent RSS groups. Innate rate
  // remains outside RSS; enumerate all legal socket counts.
  for (let sequence = 0; sequence <= 6; sequence++) {
    const s = snapshot(deep, false, [],crystals(T.sequence,sequence));
    const e = evaluate(s);
    retain(best.maximumMiningSpeed, e.miningFinal, s, e, {
      rawMining: e.rawMining, multiplier: e.miningMultiplier,
      bookMiningBase: e.bookMiningBase, manuscriptMiningFlat: e.manuscriptMiningFlat, manuscriptMiningRate: e.manuscriptMiningRate,
      correctTool: true, submerged: false, airborne: false
    });
  }
  const durationConfig = snapshot(deep, true, crystals(T.guogao, 1), crystals(T.ordinal, 6));
  const durationEffects = evaluate(durationConfig);
  retain(best.maximumGuogaoDuration, durationEffects.controlSeconds, durationConfig, durationEffects);
  return Object.fromEntries(Object.entries(best).map(([name, result]) => [name, finish(result)]));
}

// Other pieces influence this piece's wear only through their weighted
// Boundary defense contribution. Other pieces have no wear tradeoff for the
// selected piece, so their independently monotone eight-Boundary optimum wins.
function otherArmorRepresentatives(target) {
  const counts = armorKeys.map((_, i) => i === target ? 0 : 8);
  return [{ counts, weightedBoundarySlots: counts.reduce((n, c, i) => n + c * armorShares[i], 0),
    cores: counts.map(n => crystals(T.boundary, n)) }];
}
function jointArmorWear(deep) {
  const results = {};
  for (const [target, key] of armorKeys.entries()) {
    const best = winner(), other = otherArmorRepresentatives(target);
    for (const rep of other)
      for (let empty = 0; empty <= 8; empty++)
        for (let boundary = 0; boundary <= 8 - empty; boundary++) {
          const ordinal = 0;
          const armor = rep.cores.slice();
          armor[target] = mix([T.empty, empty], [T.boundary, boundary]);
          for (let bookBoundary = 0; bookBoundary <= 6; bookBoundary++) {
            const book = mix([T.boundary, bookBoundary], [T.ordinal, 6 - bookBoundary]);
            const s = snapshot(deep, true, [], book, armor), e = evaluate(s);
            const wear = engine.wearFactor(armor[target], book, e.bookBase);
            retain(best, e.protectionFactor * wear, s, e, {
              protectionFactor: e.protectionFactor, ownWearFactor: wear,
              expectedWearPerNativeBudgetPoint: 1 / (e.protectionFactor * wear),
              targetPiece: key, targetEmpty: empty, targetBoundary: boundary, targetOrdinal: ordinal,
              otherBoundaryCounts: rep.counts, otherWeightedBoundarySlots: rep.weightedBoundarySlots,
              manuscriptBoundary: bookBoundary, manuscriptOrdinal: 6 - bookBoundary,
              deduplicatedOtherArmorConfigurations: other.length
            });
          }
        }
    finish(best);
    if (engine.armorWearBudget) best.detail.expectedWearFrom10HPMobHit = engine.armorWearBudget(best.configuration, 10, 'mob_attack')[target];
    results[key] = best;
  }
  return results;
}

function miningTierComparisons() {
  const rows = [];
  for (const deep of [false, true]) for (let toolTier = 1; toolTier <= 4; toolTier++)
    for (let manuscriptTier = 1; manuscriptTier <= 4; manuscriptTier++) {
      const best = winner(), bookSlots = (rules.bookSlots || [0, 2, 3, 4, 6])[manuscriptTier],
        sequenceLevel = (rules.bookRegionalGrades || [0, 3, 3, 3, 3])[manuscriptTier];
      for (let sequence = 0; sequence <= bookSlots; sequence++) {
        const s = snapshot(deep, false);
        s.mainhand = { ...item(0, []), tier: toolTier };
        s.offhand = { ...item(6, [
          ...Array.from({ length: sequence }, () => ({ type: T.sequence, level: sequenceLevel }))
        ]), tier: manuscriptTier };
        const e = evaluate(s);
        retain(best, e.miningFinal, s, e, { toolTier, manuscriptTier, sequenceLevel, sequenceCount: sequence,
          rawMining: e.rawMining, bookMiningBase: e.bookMiningBase, manuscriptMiningFlat: e.manuscriptMiningFlat, manuscriptMiningRate: e.manuscriptMiningRate, multiplier: e.miningMultiplier });
      }
      rows.push(finish(best));
    }
  return rows;
}

function protectionCounterexamples() {
  const rows = [];
  for (const deep of [false, true]) for (const [target, key] of armorKeys.entries()) {
    const armor = armorKeys.map(() => crystals(T.boundary, 8));
    const full = snapshot(deep, true, [], crystals(T.ordinal, 6), armor), fullE = evaluate(full);
    const withGapArmor = armor.map(cs => cs.slice());
    withGapArmor[target].pop();
    const gap = snapshot(deep, true, [], crystals(T.ordinal, 6), withGapArmor), gapE = evaluate(gap);
    const addedArmor = withGapArmor.map(cs => cs.slice());
    addedArmor[target].push({ type: T.boundary, level: 1 });
    const added = snapshot(deep, true, [], crystals(T.ordinal, 6), addedArmor), addedE = evaluate(added);
    assert(addedE.protectionFactor>gapE.protectionFactor, 'Adding Boundary must improve protection in '+key);
    rows.push({ deep, targetPiece: key, operation: 'Add Boundary Lv1 to an empty socket; do not remove another bonus.',
      monotonic: addedE.protectionFactor >= gapE.protectionFactor,
      protectionLostFromAddingCore: gapE.protectionFactor - addedE.protectionFactor,
      fullBoundary: { configuration: full, protectionFactor: fullE.protectionFactor },
      emptySocket: { configuration: gap, protectionFactor: gapE.protectionFactor },
      addBoundary: { configuration: added, protectionFactor: addedE.protectionFactor },
      implementedFormula: '(1+2*sum(armorShare*RSS(pieceBoundary)))*bookFactor; book normal adds, deep multiplies.' });
  }
  for (const deep of [false, true]) for (const [target, key] of armorKeys.entries()) for (let n = 0; n < 8; n++) {
    const armor = armorKeys.map(() => crystals(T.boundary, 8));
    armor[target] = crystals(T.boundary, n);
    const before = snapshot(deep, true, [], mix([T.boundary, 3], [T.ordinal, 3]), armor), beforeE = evaluate(before);
    const after = { ...before, [key]: item(target + 2, [...armor[target], { type: T.boundary, level: 1 }]) }, afterE = evaluate(after);
    assert(afterE.protectionFactor > beforeE.protectionFactor, 'Per-piece monotonicity ' + key + ' at ' + n);
    rows.push({ deep, targetPiece: key, boundaryBefore: n, monotonic: true,
      emptySocket: { configuration: before, protectionFactor: beforeE.protectionFactor },
      addBoundary: { configuration: after, protectionFactor: afterE.protectionFactor } });
  }
  for (const deep of [false, true]) for (const type of [T.boundary, T.ordinal]) {
    const before = snapshot(deep, true, [], [], armorKeys.map(() => crystals(T.boundary, 8))), beforeE = evaluate(before);
    const after = { ...before, offhand: item(6, [{ type, level: 1 }]) }, afterE = evaluate(after);
    assert(afterE.protectionFactor > beforeE.protectionFactor, 'Independent manuscript monotonicity');
    rows.push({ deep, targetPiece: 'offhand', addedCore: type, monotonic: true,
      emptySocket: { configuration: before, protectionFactor: beforeE.protectionFactor },
      addBoundary: { configuration: after, protectionFactor: afterE.protectionFactor } });
  }
  return rows;
}

// Low-cost comparisons expose dominance/cliffs without altering the proposal.
function bookBaselines() {
  const rows = [];
  for (const deep of [false, true]) for (let tier = 1; tier <= 4; tier++) {
    const s = snapshot(deep, false, [], []);
    s.offhand.tier = tier;
    const plain = evaluate(s);
    const withEmpty = { ...s, mainhand: item(0, [{ type: T.empty, level: 1 }]) };
    const enhanced = evaluate(withEmpty);
    rows.push({ deep, tier, emptyManuscript: plain, oneEmptyLevel1: enhanced,
      wearRelativeImprovement: enhanced.wearFactor / plain.wearFactor });
  }
  return rows;
}
function miningExamples() {
  const rows=[];
  for(const deep of[false,true])for(const example of[
    {id:'bare_hand',mainhand:{...engine.empty(0),vanilla:'hand'},book:[]},
    {id:'hand_one_sequence',mainhand:{...engine.empty(0),vanilla:'hand'},book:[{type:T.sequence,level:1}]},
    {id:'hand_mixed_sequence',mainhand:{...engine.empty(0),vanilla:'hand'},book:[{type:T.sequence,level:1},{type:T.sequence,level:3}]},
    {id:'omega_six_sequence',mainhand:item(0,[]),book:crystals(T.sequence,6)},
    {id:'diamond_efficiency_v',mainhand:{tier:-1,kind:0,vanilla:'diamond',baseMining:8,nativeEfficiency:5,cores:[]},book:crystals(T.sequence,2)},
    {id:'wrong_tool',mainhand:{tier:-1,kind:0,vanilla:'diamond',baseMining:8,nativeEfficiency:5,cores:[]},book:crystals(T.sequence,2),miningApplicable:false},
    {id:'haste_ii',mainhand:item(0,[]),book:crystals(T.sequence,2),hasteLevel:2},
    {id:'fatigue_water_air',mainhand:item(0,[]),book:crystals(T.sequence,2),hasteLevel:2,fatigueLevel:1,submerged:true,airborne:true}
  ]) {
    const s={...snapshot(deep,false),mainhand:example.mainhand,offhand:item(6,example.book),
      miningApplicable:example.miningApplicable,hasteLevel:example.hasteLevel||0,
      fatigueLevel:example.fatigueLevel||0,submerged:!!example.submerged,airborne:!!example.airborne};
    const e=evaluate(s);
    rows.push({id:example.id,deep,configuration:s,effects:e,value:e.miningFinal});
  }
  return rows;
}

// One legal balanced loadout is compared to itself in each realm. This is a
// concrete PvP example, not a claim that all maximum-tier loadouts have one TTK.
function auditObjectives(deep) {
  const af = winner(), afh = winner(), rows = [];
  for (let p = 0; p <= 6; p++) for (let q = 0; q <= 6 - p; q++) for (let o = 0; o <= 6 - p - q; o++) {
    const h = 6 - p - q - o;
    const s = snapshot(deep, true, crystals(T.power, 8), mix([T.power, p], [T.boundary, q], [T.ordinal, o], [T.guogao, h]), armorKeys.map(() => crystals(T.boundary, 8)));
    const e = evaluate(s), detail = { manuscriptCounts: { power: p, boundary: q, ordinal: o, guogao: h },
      attack: e.attack, extraProtection: e.protectionFactor, totalFixed80Protection: 5 * e.protectionFactor, health: e.actualMaxHealth };
    retain(af, e.attack * e.protectionFactor, s, e, detail);
    retain(afh, e.attack * e.protectionFactor * e.actualMaxHealth, s, e, detail);
    rows.push({ value: e.attack * e.protectionFactor * e.actualMaxHealth, configuration: s, effects: e, detail });
  }
  rows.sort((a, b) => b.value - a.value);
  return { attackDefense: finish(af), attackDefenseHealth: finish(afh), topHealthObjectives: rows.slice(0, 10).map(finish) };
}
function balancedPvp(objectives) {
  const rows = [];
  for (const deep of [false, true]) {
    const realm = objectives[deep ? 'deep' : 'normal'];
    for (const r of realm.topHealthObjectives.filter(v => Math.abs(v.value - realm.attackDefenseHealth.value) < 1e-7)) {
      const e = r.effects, totalFactor = 5 * e.protectionFactor, hitDamage = e.attack / totalFactor, criticalHitDamage = 1.5 * e.attack / totalFactor;
      rows.push({ deep, configuration: r.configuration, effects: e, hitDamage, criticalHitDamage,
        totalFixed80Protection: totalFactor, attackIntervalTicks: 12.5,
        noHealingHits: Math.ceil(e.actualMaxHealth / hitDamage - 1e-12),
        withPeriodicHealingHits: Math.ceil(e.actualMaxHealth / hitDamage - 1e-12),
        criticalWithPeriodicHealingHits: Math.ceil(e.actualMaxHealth / criticalHitDamage - 1e-12),
        assumptions: 'Fixed native80% only for comparison; no burst, healing, DOT, food, shield, totem or potions.' });
    }
  }
  return rows;
}
function lowerGradeProtection(deep) {
  const best = winner();
  for (let q = 0; q <= 6; q++) {
    const s = snapshot(deep, true, [], [...crystals(T.boundary, q).map(c => ({ ...c, level: 2 })), ...crystals(T.ordinal, 6 - q).map(c => ({ ...c, level: 3 }))],
      armorKeys.map(() => crystals(T.boundary, 8).map(c => ({ ...c, level: 2 }))));
    const e = evaluate(s); retain(best, e.protectionFactor, s, e, { extraProtection: e.protectionFactor, totalFixed80Protection: 5 * e.protectionFactor });
  }
  return finish(best);
}

const objectives = { normal: auditObjectives(false), deep: auditObjectives(true) };
const output = {
  schemaVersion: 3,
  scope: 'Production revision48 formulas, offline audit; sword extrema and velocity-dependent bow examples. No game execution.',
  assumptions: {
    tier: 4, swordSlots: 8, manuscriptSlots: 6, armorSlotsPerPiece: 8,
    regionalLevel: 3, ordinalLevel: 4,
    enumeration: 'Maximum-grade relevant-family count allocations; excludes lower-grade utility tradeoffs.',
    melee: 'Full cooldown, vanilla critical, no target armor/resistance/absorption; excludes status damage over time.',
    ranged: 'Examples use full draw, impact speed equal to launch speed, and the complete vanilla critical range. They are not bow optima; one target receives at most one direct hit and one burst per volley.',
    defense: 'Engine protectionFactor is extra custom protection only. Native armor/toughness are separate. Fixed80% comparison total factor=5*extraF.',
    threshold: 'Comparison lethal threshold=5*H*extraF; equality exhausts HP. Actual native armor changes with incoming damage.',
    wear: 'Expected per-point wear protection. Joint armor audit optimizes extraF and the selected piece W in the same legal configuration; independent maxima cannot be multiplied.',
    healing: 'Periodic manuscript healing only; food-floor-driven vanilla natural regeneration is not included.',
    inputs: { rulesPath: path.relative(repo, rulesPath), enginePath: path.relative(repo, enginePath) }
  },
  inputSha256: Object.fromEntries([['rules', rulesPath], ['engine', enginePath]].map(([k, p]) =>
    [k, crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex')])),
  rules,
  normal: auditRealm(false),
  deep: auditRealm(true),
  objectives,
  lowerGradeProtection: { normal: lowerGradeProtection(false), deep: lowerGradeProtection(true) },
  jointArmorWear: { normal: jointArmorWear(false), deep: jointArmorWear(true) },
  miningTierComparisons: miningTierComparisons(),
  protectionCounterexamples: protectionCounterexamples(),
  bookBaselineComparisons: bookBaselines(),
  miningExamples: miningExamples(),
  balancedPvp: balancedPvp(objectives)
};
output.rangedExamples = [false, true].flatMap(deep => [1, 2, 3, 4].map(tier => {
  const s = snapshot(deep); s.mainhand = item(7, [], {tier}); s.offhand = item(6, [], {tier});
  const shot = engine.ranged(s, {bowCharge: 1});
  return {deep, tier, configuration: s, shot};
}));
output.balanceCalibration={
  attackRatio:output.deep.maximumAttack.value/output.normal.maximumAttack.value,
  protectionRatio:output.deep.maximumProtection.value/output.normal.maximumProtection.value,
  normalUnchanged:false,deepMaximumProtectionUnchanged:false,
  goal:'Production revision48; independent per-source RSS, manuscript-only Ordinal, native armor and independent set/book protection.'
};
const close = (a, b, label) => assert(Math.abs(a - b) < 1e-8 * Math.max(1, Math.abs(b)), label + ': ' + a + ' vs ' + b);
const R = Math.sqrt(8), armorF = 1 + 2 * R;
close(output.normal.maximumAttack.value, 16 + 2 + 3 * R + 6 + 2.5 * Math.sqrt(2), 'Normal independent peak');
close(output.deep.maximumAttack.value, 16 * 1.2 * 3 * (1 + .4 * Math.sqrt(2)) * (1 + R), 'Deep independent peak');
close(output.normal.maximumProtection.value, armorF * (1.2 + Math.sqrt(5) + .4), 'Normal extra defense independent peak');
close(output.deep.maximumProtection.value, armorF * 1.2 * 3 * (1 + .4 * Math.sqrt(2)), 'Deep extra defense independent peak');
close(output.normal.maximumHealth.value, 20 + 12 * Math.sqrt(6), 'Maximum health');
close(output.deep.maximumHealth.value, output.normal.maximumHealth.value, 'Realm-invariant health');
close(output.normal.maximumMiningSpeed.value,(18+16*Math.sqrt(6))*(1.4+Math.sqrt(6)),'Six Sequence mining maximum, separate RSS');
close(output.deep.maximumMiningSpeed.value,output.normal.maximumMiningSpeed.value,'Realm-invariant mining maximum');
for(const example of output.miningExamples.filter(row=>!row.deep)) {
  const peer=output.miningExamples.find(row=>row.deep&&row.id===example.id);
  close(example.value,peer.value,'Mining scenario matches across realms: '+example.id);
}
output.vanillaComparison = { rawSwordAttack: 11, criticalSwordAttack: 15, health: 20,
  fixed80ProtectionWithProtectionIV: 5 / .36, fixed80LethalInput: 20 * 5 / .36,
  attackRatio: { normal: output.normal.maximumAttack.value / 11, deep: output.deep.maximumAttack.value / 11 },
  fixed80ThresholdRatio: { normal: output.normal.maximumSingleHitThreshold.value / (20 * 5 / .36), deep: output.deep.maximumSingleHitThreshold.value / (20 * 5 / .36) },
  note: 'Netherite SharpnessV generic target; four ProtectionIV EPF16; fixed80% native reduction. No potions, shield or totem.' };
const nativeDamage = (raw, armor, toughness) => raw * (1 - Math.min(20, Math.max(.2 * Math.floor(armor), Math.floor(armor) - raw / (2 + toughness / 4))) / 25);
output.nativeArmorTable = [1, 2, 3, 4].map(tier => ({ tier, armor: rules.nativeArmorByTier[tier - 1], toughness: rules.nativeToughnessByTier[tier - 1],
  damage: [5, 10, 15, 20, 40, 80].map(raw => ({ raw, hp: nativeDamage(raw, rules.nativeArmorByTier[tier - 1], rules.nativeToughnessByTier[tier - 1]) })) }));
output.evaluations = evaluations;
output.elapsedMilliseconds = Date.now() - now;
fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, JSON.stringify(output, null, 2) + '\n', 'utf8');
console.log('ENHANCEMENT_PRODUCTION_AUDIT_OK evaluations=' + evaluations + ' elapsedMs=' + output.elapsedMilliseconds);
for (const realm of ['normal', 'deep']) console.log(realm + ': ' + JSON.stringify(Object.fromEntries(
  Object.entries(output[realm]).map(([name, result]) => [name, result.value]))));
for (const realm of ['normal', 'deep']) console.log('jointArmorWear ' + realm + ': ' + JSON.stringify(Object.fromEntries(
  Object.entries(output.jointArmorWear[realm]).map(([name, result]) => [name,
    { combined: result.value, defense: result.detail.protectionFactor, wear: result.detail.ownWearFactor }]))));
console.log('Protection monotonicity violations: ' + output.protectionCounterexamples.filter(c => !c.monotonic).length);
console.log('Report: ' + outputPath);
