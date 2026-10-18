# 0.6.2 — asymmetric portal search

## Authorized scope

2026-10-18: implement scale-aware asymmetric portal reuse without permanent endpoint binding; try multiple safe arrival directions; Inner portal arrivals must be at Y≤200. Shrink physical entry so standing on a frame or brushing a corner does not teleport. Keep the work on its own branch, test elapsed time/failure rates, and remain compatible with 0.6.0 saves. No main merge, push or Release is authorized.

Baseline: `0a890b9`, backed up at `codex/backup-0.6.1-before-portal-routing`. Development branch: `codex/0.6.2-asymmetric-portals`.

## Routing and loading

Existing gates retain their saved IDs, blocks and three-field gate records. There is no new peer/owner binding and the old return-address storage is unchanged. Each trip selects the nearest eligible complete gate with a safe landing; another closer gate may legitimately change the result.

| Route kind | Target on smaller-coordinate side | Target on larger-coordinate side |
|---|---|---|
| Overworld / Outer | 24 blocks | 24 blocks |
| Outer / Inner | Outer: 24 blocks | Inner: 96 blocks |
| Inner / Underworld | Inner: 24 blocks | Underworld: 96 blocks |

These are square horizontal windows. Compare the candidate's geometric block center against the **unrounded** scaled source center. This makes the 24/96 inclusion tests reciprocal even at negative coordinates and exact range edges. Integer target coordinates are only used for terrain placement; existing gates are ranked by their exact center distance. Border clamping, different eligible competitors and the explicit height cap remain reasons for a different return gate.

The radius is a coordinate-record filter, never a full-region chunk scan. Prepare one candidate's actual ±5-block footprint at a time (at most four explicitly requested FULL chunks), then validate its frame and all landing choices. Release that candidate's tickets before trying another. If none works, retain the old 24-block, local 3×3-chunk terrain search. Asynchronous FULL requests never join unfinished futures. Preserve the shared 64-step / soft 2 ms search budget, eight-preparation cap, 600-tick / 45-second travel deadline, cancellation, silent safe retreat and token-bound loading screen.

New gate records are reconsidered before each terrain search batch so concurrent requests can reuse a gate just constructed by another request. A bad candidate does not require loading all other candidate chunks simultaneously.

A newly arrived player's source frame may straddle neighboring chunks which have not reached FULL yet. Such missing source data now waits for native player-chunk loading rather than being classified as a destroyed gate. The travel deadline is checked before this wait, so it cannot keep an expired request alive. There is no synchronous source-chunk load added to gameplay.

## Entry and arrival geometry

- Actual physical entry uses the player's horizontal center, within the inner 3×3 field inset by 0.15 blocks on every side. Feet must be from gate Y−0.5 through Y+0.5. Merely intersecting the outer field with a bounding-box corner or standing above it is insufficient. Command travel keeps its separate original cancellation volume.
- Arrival tries south, east, north and west, three positions per side. Every candidate uses the existing floor, headroom, fluid, border, loaded-chunk and neighboring hazard checks. Search, READY-cache validation and final transfer use this same landing function. No unsafe side is forcibly cleared.
- The Inner Y≤200 limit applies to reused exits, candidate generation, new gate construction, cached exits and final transfer. Existing higher gates are **not deleted or moved**; they can still be departure gates. A new lower exit may be needed when no existing eligible lower gate exists. General physical safety and source-side retreat do not adopt the height cap. The separate void-fall transition is unchanged.

## Validation

Four-target final builds and package checks pass, as do the 84 existing Python tests. Independent source review is in `PORTAL-062-REVIEW.md`. Static comparison against the actual 0.6.0 JARs finds the saved-state/return codecs, rituals, payloads and block/mining registration classes byte-identical in all four targets. All packaged assets and data are semantically unchanged; some JSON formatting differs in the three older targets. Exact ignored evidence: `build/save-schema-060-062.json` and `build/portal-artifact-delta-062-final.json`. No QA classes enter the production JARs.

Final hidden disposable fo262 `portal062-warm-d` passes 1,530 assertions and 29 real trips, including 24 measured warm reuse trips. All 24 reuse the exact expected original gates, with zero new gates and no failures. Each takes one server tick; measured queue-to-server-result wall time has median 29.06 ms, p95 72.32 ms and maximum 87.16 ms. Tick timing depends on the call's phase and current machine load, so these milliseconds are not a promise of client loading-screen completion. Setup, explicit fixture residency and photographs are excluded.

This run also checks 768 exact square-window boundaries, positive/negative chunk remainders, multiple safe sides, rejected high Inner destinations, preserved high departures, failed-candidate advancement, overlapping ticket reference counts, two concurrent preparations producing one exit, real frame/corner/top exclusion and actual field contact. Its separately measured source-loading case directly observes `sourceFrameFullAtQueue=false`; the pending request/gate survives and succeeds after four native ticks (2.106 s, including native source loading), without an extra gate. Four settled native arrival screenshots were inspected.

`portal062-fixture060-a` successfully creates and cleanly saves a genuine 0.6.0 world with eight registered/physical gates, including an Inner Y230 gate. `portal062-upgrade-a` opens a copied save with the final candidate and passes 110 assertions and all eight trips. Saved gate records/blocks, ReturnPoint, hub, a chest with 13 diamonds, a player-placed core, and a digit7 tool with damage11 are retained. Old lower gates are reused; the high Inner gate still permits departure, its return chooses a lower exit, and the old high gate remains complete. The source fixture's complete file hashes are unchanged. This is a tested upgrade fixture covering those saved features, not a claim to have checked every possible third-party or user world state.

`portal062-cold-a` passes 696 assertions and all 12 cold-destination trips across the three adjacent routes, both directions and positive/negative coordinates. Before **each** queue, all nine target FULL chunks are absent and the relevant region files are absent; the evidence checks both recognized Overworld storage layouts. No target fixture or prior target generation is hidden in these samples. Median server-result time is 1.867 s, p95/max 10.529 s; median 12.5 native ticks and maximum23. The slowest sample is the first cold Overworld generation. No parallel test JVM, Gradle build or bulk root-job workload runs during these measurements. They measure local integrated-server behavior, not remote-server network latency or client mesh readiness.

`portal062-timeout-a` passes 701 assertions. A deliberately held, unfinished terrain FULL future causes a real elapsed wait to tick601 (28.189 s on this run), then a safe source-side retreat with no new gate. Completing that future afterward and observing45 more native ticks produces no late teleport or construction. The shared future is not canceled. This is an injected fault testing real deadline/cleanup execution, not an observed natural loading failure, and is excluded from the normal failure-rate denominators.

| Final measured group | Successful requests | Server wall median | Server wall p95 / max |
|---|---:|---:|---:|
| Already loaded gates | 24 / 24 | 29.06 ms | 72.32 / 87.16 ms |
| Unvisited terrain | 12 / 12 | 1.867 s | 10.529 / 10.529 s |
| 0.6.0 saved gates, including a newly lowered high-gate return | 8 / 8 | 56.35 ms | 415.72 / 415.72 ms |

These are descriptive finite samples, not a zero-failure guarantee for arbitrary terrain, server load or other mods. No normal saves are test fixtures. Actual runtime testing uses hidden isolated Minecraft26.2; the other three targets have build/package/static compatibility checks, not new runtime tests in this round.

The final historical command/loading-screen regression `portal062-ui-c` passes: Portal040 safety36, Ground62, Frame cumulative91 and Portal05023 checks, including actual C2S `tp outer`/`tp overworld`, failure/timeout cleanup, loading-request token ownership and unrelated-screen preservation. Initial loading feedback is126.08ms; actual cold-command feedback54.38ms, native handoff1.427s and warm UI return254.56ms. These are separate UI samples, not additions to the warm/cold table. Both native screenshots were visually inspected. Final aggregated evidence is `build/portal062-final-summary.json`; production JAR and core QA class hashes match across the final runs.

## Final artifacts

| Minecraft | SHA-256 of release JAR |
|---|---|
| 1.21.1 | `d1a35bcce2d6601fcebc18de2b8ca24f2f278693b43772d4aa35084f979a4013` |
| 1.21.11 | `0bee7ef3003bfa1dc4a75040307c9c11c7634c4560fb4b7287c2732327f4651d` |
| 26.2 | `30f3f01c5037d07ca226a3475a942d2d93e7a2e607fe8dc3b698261a75816016` |
| 26.3 | `6c4e4c5bcfceda21e9297ad7c8d5b0e3652d66cb66e4b7ef77571af83e0d0c68` |

All four are in `build/releases/0.6.2/`. Work remains on `codex/0.6.2-asymmetric-portals`; real main is unchanged at1900d1e. The implementation turn ended with local commit3da38d5 and no push or Release.

## Local installation

After all final tests exited, the normal fo262 instance was confirmed closed and updated under the user's standing installation authorization. Previous0.6.1 is backed up at `build/backups/fo262-before-0.6.2/`; exactly one actual guogaology Mod remains and the installed hash matches the final tested26.2 artifact above. All55 other Mod hashes,129 configuration hashes, and48 fo263_2 Mod hashes remain unchanged. The21,178 fo262 and356 fo263_2 normal-save file length/mtime records also remain unchanged; this metadata check is not described as a full save-content hash comparison. Normal fo263_2 remains0.6.1. Receipt: `build/install-062.json`.

## Publication follow-up

The user subsequently explicitly requests `push+release`. This authorizes pushing the current development branch and publishing v0.6.2, while the previous instruction to leave main unmerged remains in force. The release includes0.6.1's26.3 optional-JEI/tag/recipe synchronization fixes as well as this portal work. Bilingual release notes and READMEs are updated without changing production code or the tested JARs.

Generate the complete corresponding source archive from the exact final release commit on this branch, verify every archived file against Git, preserve executable Gradle wrappers and all attribution/license notices, and include the source and all other attachments in the final SHA-256 list. Check the four-target GitHub workflow before publication. Final packaging and remote evidence are recorded after the release commit in ignored `build/bundle-062-proof.json` and `build/release-062-remote.json`; source and tag must identify that same commit. No main merge is part of this publication.

Preflight records are retained rather than overwritten. `portal062-warm-a` was canceled to correct a QA-only concurrent-fixture counter before trials. `portal062-warm-b` exposed a boundary-source fixture which had unloaded after setup; its timing series is incomplete and is not the final warm-rate sample. Offline decoding confirms all 21 physical source portal/frame blocks and its registered gate remained intact. The report did not directly log the transient source-loaded boolean, so its unloaded-source diagnosis is a strongly supported explanation rather than a recorded boolean observation. Gameplay gained the source-loading guard above; final warm fixtures explicitly establish source/target residency before starting their clocks.

`portal062-warm-c` already completed the same24 reuse trips but had a QA-only final preservation assertion which forgot to load the distant old gate; the final warm-d run loads it before checking and fully passes. The optional historical UI preflight `portal062-ui-a` used the old SiteSearch reuse responsibility; `portal062-ui-b` then used the pre0.5.0 removed `visit` command. Those QA assertions/calls are updated to the current preparation split and `tp outer`/`tp overworld` command tree. No production command or gameplay policy is changed to satisfy obsolete tests.
