# 0.6.8 observation-only audit helpers

These helpers observe the **unchanged released** Mod. They are not a repair,
release gate, benchmark, or substitute for the individual suite results.
In particular, the final 26.3 bow suite has two genuine numeric failures even
though the enclosing runner completes successfully and collects screenshots.

- `prepare_263_checks.py` copies six existing opt-in 26.2 QA suites into the
  26.3 QA source set, adapting native APIs and two obsolete pre-0.5.5 bow
  expectations. It also copies `CombatCooldownAudit.java` into both QA packages.
- The Java template compares a real direct hit followed by the production
  burst with a QA-only control that clears the actual damage cooldown field.
  It does not patch any production class or change the production JAR.
- `build_general_audit_report.py` requires the final observation JSON files,
  all five successful new loopback cases, unchanged production sources and
  unchanged published JAR hashes. It produces the tracked audit note and a
  standalone local HTML report with selected original screenshots.

The copied Java suites are checked in under `ports/qa263`; both target helpers
are opt-in source sets. The normal Mod build must not include them. For the
historical 0.6.8 audit, each run used a new directory and disposable world;
normal saves, account data and configuration were not used as fixtures.

Build only the QA JARs with the existing Java 25 runtime and workspace Gradle
cache, then run `tools/run_release_263_qa.py --general-audit` or
`tools/run_merge_qa.py --item068-only --audit068-focus --fresh-world`.
Each requires its documented isolated instance, Java and fresh label options.
The 26.3 runner needs the existing local `--known-filter-workaround` on this
machine; this is not a production Mod fix. Do not reuse old labels or generated
evidence to certify a future repair.

The audit also reuses `tools/run_network_263_qa.py` with exact released
0.6.8 / 0.6.0 inputs and the existing frozen network QA JAR. The new case names
start with `audit068-network-`. All listeners must remain `127.0.0.1` and be
closed on completion. See `docs/AUDIT-068-GENERAL.md` for results and limits.
