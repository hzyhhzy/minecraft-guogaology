# Portal routing review — 0.6.2

This is an independent source review of the working 0.6.2 portal changes on 2026-10-18. It covers `PortalTravel`, `PortalPreparation`, `PortalSiteSearch` and `PortalGeometry` in both `src/main/java` and `ports/common/main`. The reviewer did not build, run Minecraft, open a save, or execute native tests. Build and runtime evidence must be recorded separately.

## Findings and disposition

Two concrete findings were reported to the implementation owner and are corrected in the reviewed source:

1. The legacy prewarm filter briefly called nonexistent `Gate.getRegistryKey()`. It now correctly compares `g.dimension()` (`src/main/java/dev/guogaology/portal/PortalPreparation.java:144`).
2. Existing-gate filtering retained the precise mapped position, but its ordering initially used the floored terrain position. Both versions now sort the actual gate center against `Target.searchPosition()` (`ports/common/main/dev/guogaology/portal/PortalTravel.java:262`; legacy `:263`). For example, an Inner source at `(3,64,0)` maps horizontally to `(.875,.125)` in Outer. Gates at `(1,64,0)` and `(0,64,1)` have true center-distance squares `.53125` and `2.03125`; rounding had made them an incorrect tie.

After these corrections, this review found no further concrete defect in the requested routing, candidate loading, shared-ticket release or new height-policy paths. This is a source conclusion, not a runtime pass.

## Precise reuse windows and bounded construction

The registered-gate windows use actual geometric centers and the unrounded mapped X/Z: each axis is at most 24 blocks on the lower-scale side, or 96 on the corresponding higher-scale side. For centers A and B in adjacent 4:1 dimensions,

`abs(B - A/4) <= 24` is exactly equivalent to `abs(A - 4*B) <= 96`.

The source and candidate both include the half-block center offset, so positive and negative integer coordinates obey the same relationship without extra rounding tolerance. Radius selection covers the physical adjacent routes; it is not a promise of reciprocal 16:1 direct-command routes. World-border clamping, the Inner arrival-height policy, invalid landing geometry and another nearer valid gate remain intentional reasons to select a different gate. This is nearest-gate routing, with no permanent pair binding.

New construction remains inside the old 24-block terrain-search window and its nine explicitly awaited chunks. Generated columns additionally pass `inGateWindow`, so a new exit remains eligible for the reverse registered-gate search. The initial `boundedCenter` column is also inside that window: its center differs from the exact mapped point by at most 5.5 blocks on either horizontal axis. Candidate heights are checked separately before entering the build list. The wider registered-gate window does not cause the entire 96-block area to be generated.

Source: `PortalTravel.java:141`, `:155`, `:250`; `PortalSiteSearch.java:19`, `:35` under `ports/common/main/dev/guogaology/portal`.

## Async loading, cancellation and concurrency

- Registered candidates are selected from saved metadata, then their actual frame and landing neighborhood are requested individually. The +/-5-block footprint spans at most four explicitly awaited FULL chunks and covers all twelve landing choices plus their hazard checks.
- Switching candidate or switching to terrain first releases the previous chunk references. `USERS` keys include the world and chunk; overlapping preparations remove a shared ticket only when its last reference leaves. Clearing the local chunk map prevents repeated decrement on later cancellation.
- A future is joined only after `isDone()`. Exceptional/unsuccessful candidate loads advance to the next candidate, then to terrain; failed terrain loading ends the request. Shared Minecraft chunk futures are never canceled.
- The last pending user of a source gate releases its preparation. Failed, canceled, logged-out, timed-out and idle paths retain their release routes. The limits remain eight preparations, 600 ticks, 45 seconds wall time and a 40-tick idle expiry.
- Each requested terrain-search continuation rechecks newly registered candidates before further terrain work. Server-main-thread ordering therefore lets another request's newly built gate be discovered before the next request constructs its own exit. Same-source users share the preparation.
- The terrain/validation budget remains 64 steps per tick with a soft two-millisecond deadline. A single placement step, metadata sorting and chunk-ticket management are not a hard real-time two-millisecond guarantee. Ticket propagation may load more chunks than the explicit futures awaited above.

Source: `PortalPreparation.java:17`, `:34`, `:46`, `:67`, `:76`, `:100`, `:126`; `PortalTravel.java:42`, `:47` under `ports/common/main/dev/guogaology/portal`.

## Source-chunk readiness follow-up

The final legacy and modern `PortalTravel.tick` now check source readiness before interpreting an incomplete ritual gate as a broken frame (`PortalTravel.java:51` through `:60` in both source families). The order is: validate the player and entry position; enforce the existing tick/wall deadline; for physical ritual requests only, wait while any chunk intersecting the source frame's +/-2 footprint is not FULL; then validate the actual blocks and continue destination preparation. The waiting branch retains the pending request and registered gate, and neither collapses nor transfers it.

This guard is appropriate: an unavailable source chunk is not evidence of a missing block. It adds no synchronous chunk request or world mutation; ordinary player chunk loading supplies the source data. Death, disconnect, leaving the entry area and the 600-tick/45-second deadlines still release the request before the wait can repeat. Destination preparation remains tracked by the pending request and is released through the existing last-user cancellation path. Transient command requests do not gain a source-frame requirement. Once source data is available, a genuinely broken gate still reaches the original validation/collapse path.

The implementation owner's saved `build/portal-qa/portal062-warm-d/portal-summary.json` was read for corroboration, without rerunning Minecraft. Its separate `native-source-neighbor-readiness` case records `sourceFrameFullAtQueue=false`, observes the request and gate record surviving a production tick, then reaches the expected old exit after 4 ticks / 2106.1277 milliseconds with zero new gates. The same output reports 1530 assertion calls and 24/24 successful warm trials; the source-guard case is outside that warm denominator. These are server-side request-completion measurements. The earlier warm-b saved-frame inspection was consistent with an unavailable source neighbor, but lacked a contemporaneous loading-predicate measurement and is not treated as direct proof of that earlier tick.

## Entry, landing and Inner height policy

Physical entry now uses the player's horizontal center within the inset 3x3 interior and feet Y within the inclusive `centerY +/- .5` band. The same predicate gates initial queueing and continued physical requests. Transient command requests retain their previous AABB containment check. Route kinds and activation/frame rules are unchanged.

Landing tries south, east, north and west, with lateral offsets `0,-1,+1` for each side. Selection, cached-exit validation, registered-candidate validation and final transfer all use this same helper. Recomputing a currently safe landing at each stage is consistent; a cached south-only assumption is no longer present. Final yaw faces the selected gate from the chosen side. The helper only performs existing loaded-only safety reads.

The actual portal feet position is capped at Y200 when the destination is Inner: reuse filtering, terrain candidate collection, site validation and final landing all enforce it. The general `isSafe` predicate is unchanged, so a safe old high-altitude source or retreat point is not globally reclassified as unsafe. An existing Inner gate above Y200 is preserved and may still be used to depart, but is no longer a permissible incoming exit. The separate Outer-void fall transition still arrives at Y500; it is not a portal-gate arrival and was not changed by this patch.

The existing native cooldown counter is still written after transfer, but the custom physical queue does not read it. Do not describe this review as proving a new enforced 40-tick custom-gate cooldown. Safe landings remain outside the entry area, and native regression should verify no automatic bounce.

Source: `PortalGeometry.java:15`, `:21`, `:33`; `PortalTravel.java:34`, `:87`, `:163`, `:220`; `PortalSiteSearch.java:35` under `ports/common/main/dev/guogaology/portal`.

## Save compatibility and remaining native checks

`PortalState` and `PortalRitual` are unchanged in the reviewed diff. There is no new saved-data schema, peer field, registry ID, portal block state or loading-payload format. Existing gate records, return records and portal blocks are retained without scanning or rewriting old chunks. The two source families implement the same new policy with their respective APIs; the legacy filter error noted above has been corrected.

The static reports `build/save-schema-060-062.json` and `build/portal-artifact-delta-062-final.json` were checked against the actual local release JAR entries. In every target, these eight entries are byte-identical to 0.6.0: `PortalState`, its `Gate` and `ReturnPoint` classes, `PortalRitual`, `PortalLoadingPayload`, `PortalActivationPayload`, `GuogaologyBlocks` and `MiningContent`. This is evidence for the listed saved-data, gate and payload definitions; it does not claim every class in the mod is unchanged.

| Target | assets/data files | Byte differences from 0.6.0 | Semantic differences |
|---|---:|---:|---:|
| 1.21.1 | 3372 | 85 JSON entries | 0 |
| 1.21.11 | 3605 | 69 JSON entries | 0 |
| 26.2 | 3605 | 69 JSON entries | 0 |
| 26.3 | 3611 | 0 | 0 |

The resource path sets match. All differing entries in the first three targets are JSON serialization/layout differences with equal parsed content; no PNG or other non-JSON resource differs. The 26.3 assets/data entries match byte for byte. This is static resource comparison, not an all-world migration test.

The final artifact-delta report has a different baseline, 0.6.1. Its hashes and exact added/removed/changed entry lists match all four current 0.6.2 JARs: changes stay in the portal implementation and version metadata, plus the legacy nested bridge's versioned filename. The inspected JARs contain no `PortalOptimizationChecks`, `PortalOptimizationRuntime`, `/mergeqa/` or `/qa/` entries. Do not describe this 0.6.1-to-0.6.2 delta as the complete code delta from 0.6.0.

Native tests should still cover:

- Offset 4:1 reuse and return in both dimension pairs, including negative coordinates, exact window edges, stable gate count and cold registered endpoints.
- Inner ground at Y199/200/201, an existing gate above Y200, lower ground beneath a high surface, and unchanged high-source departure/retreat.
- Each of the twelve safe landing positions, all-unsafe rejection, and a changed landing between preparation and transfer.
- Entry boundaries, standing on a frame, corner contact, hovering/jumping above the feet band, leaving during a pending request, and command containment.
- Two overlapping requests, same-source shared users, stale/broken candidates, failed futures, timeout/cancellation, disconnect and restart persistence.

The reviewer did not execute native tests. The saved runtime output cited above is corroborating evidence supplied by the implementation owner; other runtime coverage must be recorded separately. The reviewed changes preserve old save data while deliberately changing which gates are eligible destinations.
