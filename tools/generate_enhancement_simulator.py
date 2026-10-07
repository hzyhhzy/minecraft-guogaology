#!/usr/bin/env python3
"""Generate the current production simulator/reference, verified against Java golden cases."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def run(command: list[str]) -> str:
    result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, encoding="utf-8")
    if result.returncode:
        raise SystemExit(f"Command failed ({result.returncode}): {' '.join(command)}\n{result.stdout}{result.stderr}")
    return result.stdout


def java_home(explicit: str | None) -> Path:
    candidates = [explicit, os.environ.get("JAVA_HOME"),
                  "C:/Users/sigmoid/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon",
                  "C:/Program Files/Android/Android Studio/jbr"]
    executable = shutil.which("java")
    if executable:
        candidates.append(str(Path(executable).parent.parent))
    for candidate in candidates:
        if candidate:
            home = Path(candidate)
            if (home / "bin" / "java.exe").is_file() or (home / "bin" / "java").is_file():
                return home
    raise SystemExit("A Java 21+ JDK is required; pass --java-home.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java-home", help="Java 21+ JDK root")
    parser.add_argument("--proposal-preview", action="store_true", help="Compatibility alias: generate the current production rules")
    parser.add_argument("--health-preview", action="store_true", help="Retired health preview; production already contains the confirmed design")
    parser.add_argument("--classes-dir", type=Path, help="Reuse compiled EquipmentRules")
    parser.add_argument("--output", type=Path, default=ROOT / "docs/enhancement-simulator.html")
    parser.add_argument("--node", default=shutil.which("node"), help="Node executable for verification")
    args = parser.parse_args()
    if args.health_preview:
        parser.error("The old health preview is retired; run without --health-preview for production revision48.")
    if not args.node:
        raise SystemExit("Node is required for the Java/JavaScript comparison.")
    work = ROOT / "build/enhancement-simulator"
    work.mkdir(parents=True, exist_ok=True)
    home = java_home(args.java_home)
    suffix = ".exe" if (home / "bin/java.exe").exists() else ""
    java = str(home / ("bin/java" + suffix))
    source = ROOT / "src/main/java/dev/googology/mining/EquipmentRules.java"
    classes = args.classes_dir.resolve() if args.classes_dir else work / "java"
    if not args.classes_dir:
        classes.mkdir(parents=True, exist_ok=True)
        run([str(home / ("bin/javac" + suffix)), "--release", "21", "-encoding", "UTF-8", "-d", str(classes), str(source)])
    exported = json.loads(run([java, "-cp", str(classes), "dev.googology.mining.EquipmentRules", "cases"]))
    if not exported.get("cases") or exported["rules"].get("revision") != 48:
        raise SystemExit("EquipmentRules revision48 golden cases are required.")
    rules = dict(exported["rules"])
    rules["coreNames"] = {}
    rules["coreStageNames"] = {}
    for code, language in [("zh", "zh_cn"), ("en", "en_us")]:
        translations = json.loads((ROOT / f"src/main/resources/assets/googology/lang/{language}.json").read_text(encoding="utf-8"))
        stages = [[translations[key if stage == 1 else f"{key}_lv{stage}"]
                   for stage in range(1, 5 if core == 8 else 4)]
                  for core, key in enumerate(rules["coreTranslationKeys"])]
        rules["coreStageNames"][code] = stages
        # Ordinal stage1 is a crystal; its family name comes from the first core.
        rules["coreNames"][code] = [re.sub(r"\s*·\s*Lv1\s*$", "", names[1 if core == 8 else 0])
                                     for core, names in enumerate(stages)]
    exported["rules"] = rules
    golden = work / "java-golden-cases.json"
    golden.write_text(json.dumps(exported, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    # Legacy audit paths hold the same production data; no independent overlay is applied.
    (work / "proposal-rules.json").write_text(json.dumps(rules, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    engine_path = ROOT / "tools/enhancement_engine.js"
    engine = engine_path.read_text(encoding="utf-8")
    if engine != (ROOT / "tools/enhancement_proposal_engine.js").read_text(encoding="utf-8"):
        raise SystemExit("Legacy engine alias differs from production; synchronize it before generation.")
    build = {"caseCount": len(exported["cases"]), "ruleSourceSha256": hashlib.sha256(source.read_bytes()).hexdigest(),
             "engineSha256": hashlib.sha256(engine.encode("utf-8")).hexdigest(), "revision": rules["revision"],
             "healthPreview": False, "proposalPreview": False, "productionVersion": "0.4.4"}
    template = (ROOT / "tools/enhancement_simulator_current.template.html").read_text(encoding="utf-8")
    html = template.replace("__MODE_ZH__", "0.4.4 · 当前Mod规则").replace("__MODE_EN__", "0.4.4 · Current Mod rules")
    html = html.replace("__RULES_JSON__", json.dumps(rules, ensure_ascii=False, separators=(",", ":")).replace("<", "\\u003c"))
    html = html.replace("__ENGINE_JS__", engine).replace("__BUILD_JSON__", json.dumps(build, separators=(",", ":")))
    if re.search(r"__[A-Z_]+__", html):
        raise SystemExit("Unresolved template marker.")
    candidate = work / "enhancement-simulator.html"
    candidate.write_text(html, encoding="utf-8", newline="\n")
    print(run([args.node, str(ROOT / "tools/tests/check_enhancement_simulator.js"), str(golden), str(candidate)]).strip())
    print(run([args.node, str(ROOT / "tools/tests/check_enhancement_proposal.js"), str(work / "proposal-rules.json"), str(candidate)]).strip())
    (ROOT / "tools/enhancement_proposal_rules.json").write_text(json.dumps(rules, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(html, encoding="utf-8", newline="\n")
    print(f"Generated {output} ({len(html.encode('utf-8')):,} bytes; Java golden cases: {len(exported['cases'])})")
    reference = (ROOT / "tools/enhancement_reference.template.html").read_text(encoding="utf-8")
    reference = reference.replace("__RULES_JSON__", json.dumps(rules, ensure_ascii=False, separators=(",", ":")).replace("<", "\\u003c"))
    reference = reference.replace("__BUILD_JSON__", json.dumps(build, separators=(",", ":")))
    reference_path = output.with_name("core-effects-reference.html")
    reference_path.write_text(reference, encoding="utf-8", newline="\n")
    print(f"Generated {reference_path} (9 core types, 28 grade entries, ordinal bows)")


if __name__ == "__main__":
    main()
