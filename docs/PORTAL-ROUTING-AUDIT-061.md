# Portal reuse and return inspection — 0.6.1

The user reports that a journey can use an existing destination gate, while its return constructs another gate. This is a read-only review of the installed0.6.1 algorithm, not a portal patch. No normal world is edited or opened for the inspection.

## Confirmed cause

`PortalTravel.scale` assigns Overworld/Outer/Inner/Underworld scales1/1/4/16. Every trip maps the CURRENT gate center into the destination, independently. `PortalState.Gate` persists dimension, center and route kind, but no peer endpoint. `ReturnPoint` is written during transfer but is never read by gameplay. Automatically built exits ARE registered normally; the defect is not simply a missing addGate call.

`PortalSiteSearch.RADIUS` is always24. `PortalTravel.inSearchWindow` additionally requires the candidate center to be within one chunk in both axes of the mapped target. This search is not reciprocal when a trip towards a smaller-scale dimension chooses an offset exit.

Exact counterexample using the source's center/floor calculations:

| Step | Result |
|---|---|
| Inner gate A | `(0,80,0)` |
| Ideal Outer target | `(0,80,0)` |
| Complete, safe existing Outer INNER gate B | `(5,80,5)`, distance7.07; accepted |
| B mapped back to Inner | `(22,80,22)` |
| A's return search distance |31.11, greater than24; A is excluded |

Provided another build site exists, the remaining search creates a new gate although A is intact. Underworld→Inner→Underworld has the same4:1 problem. A larger20-block exit offset maps the return82blocks away. For comparison, the same1:1 journey accepts A on return, explaining why this is intermittent. The minimal Outer `(5,5)` center is also a possible automatic flat-ground choice because new gate footprints are inset within chunks.

Source evidence in `ports/common/main/dev/guogaology/portal`: `PortalTravel.java:99` (scale),`:149` (mapping),`:235` (search filter),`:239` (existing gates); `PortalSiteSearch.java:10` and`:69`; `PortalState.java:20`. Legacy1.21.1 uses the same algorithm in `src/main/java`. No26.3 override replaces these rules. Ignored numerical evidence is `build/portal-routing-audit-061.json`; this is an exact arithmetic/filter counterexample, not a new native-game regression run.

## Independent existing-gate rejection

Reuse, cached-exit validation and final transfer all accept only `gate.center+(0,0,3)` as the landing point. A player-built gate may be entered from another safe side while that specific south point is blocked, unsupported or hazardous. Its complete frame is still rejected and another gate can be constructed. This should be addressed without weakening the real collision/fluid/hazard checks.

Unloaded gate geometry is also treated as not complete. Destination preparation explicitly awaits nine FULL chunks; ticket propagation may load more, so the exact boundary/race frequency needs native testing before claiming a deterministic separate defect. Simultaneous searches snapshot the existing-gate list and deserve duplicate-generation coverage as well.

## Recommended repair boundaries

Preserve the bounded asynchronous loading, timeouts, cancellation and safe retreat. Give known actual return endpoints priority and load their geometry asynchronously before treating them as absent. If no valid route endpoint exists, use an appropriately scale-aware search of registered gates, then build only as the last resort. Try multiple safe landing sides of a valid gate rather than rejecting it solely for south-side obstruction.

For persistent peer routing, define the multiplayer/one-to-many policy first: do not silently overwrite a gate's reverse partner when a second source reuses it. Transient command-generated targets should not accidentally become physical peer records. Legacy unlinked gates still need a search fallback. Merely enlarging24 to a universal large radius would increase chunk work and would not resolve the unsafe-single-landing or endpoint ambiguity.

Future regression cases should assert the actual return gate/position and stable gate count, not only the destination dimension:4:1 offset reuse in both realm pairs; freshly generated offset exits; a blocked south side with another safe side; unloaded registered endpoints; server restart; multiple players/source gates; destroyed exit and timeout retreat. Existing tests cover portal construction, recipes, safe arrival and loading feedback but do not guarantee reciprocal endpoint reuse.
