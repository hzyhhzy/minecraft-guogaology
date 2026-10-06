#!/usr/bin/env python3
"""Generate an offline simulator: verified production mirror or independent proposal preview."""
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
    return subprocess.run(command, cwd=ROOT, check=True, capture_output=True, text=True, encoding="utf-8").stdout


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
    parser.add_argument("--proposal-preview", action="store_true", help="Generate the independent pending nine-core proposal; never change production rules")
    parser.add_argument("--health-preview", action="store_true", help="Preview pending manuscript-only health without editing Mod source")
    parser.add_argument("--classes-dir", type=Path, help="Reuse an already compiled EquipmentRules")
    parser.add_argument("--output", type=Path, default=ROOT / "docs/enhancement-simulator.html")
    parser.add_argument("--node", default=shutil.which("node"), help="Node executable for verification")
    args = parser.parse_args()
    if args.proposal_preview and args.health_preview:
        parser.error("--proposal-preview and --health-preview are mutually exclusive")
    if not args.node:
        raise SystemExit("Node is required for the Java/JavaScript comparison.")
    work = ROOT / "build/enhancement-simulator"
    work.mkdir(parents=True, exist_ok=True)
    home = java_home(args.java_home)
    suffix = ".exe" if (home / "bin/java.exe").exists() else ""
    java = str(home / ("bin/java" + suffix))
    source = ROOT / "src/main/java/dev/googology/mining/EquipmentRules.java"
    compile_source = source
    if args.health_preview:
        if args.classes_dir:
            raise SystemExit("--health-preview cannot reuse production classes.")
        original = source.read_text(encoding="utf-8")
        old_health = "double h=deep?10*sum(book,7,false):4*Math.sqrt(sum(book,7,true)),u=armorUniversal(armor,book,deep);"
        proposed_health = "double h=4*Math.sqrt(sum(book,7,true)),u=universal(book,deep);"
        if old_health not in original:
            raise SystemExit("Production health rules changed; review the pending preview adapter.")
        # Compile a disposable pure-algorithm copy, never the Minecraft Mod sources.
        compile_source = work / "preview-src/dev/googology/mining/EquipmentRules.java"
        compile_source.parent.mkdir(parents=True, exist_ok=True)
        compile_source.write_text(original.replace(old_health, proposed_health), encoding="utf-8")
    classes = args.classes_dir.resolve() if args.classes_dir else work / ("preview-java" if args.health_preview else "java")
    if not args.classes_dir:
        classes.mkdir(parents=True, exist_ok=True)
        run([str(home / ("bin/javac" + suffix)), "--release", "21", "-encoding", "UTF-8", "-d", str(classes), str(compile_source)])
    exported = json.loads(run([java, "-cp", str(classes), "dev.googology.mining.EquipmentRules", "cases"]))
    if not isinstance(exported, dict) or not exported.get("cases"):
        raise SystemExit("EquipmentRules cases export is missing.")
    rules = dict(exported["rules"])
    if args.proposal_preview:
        overlay_path = ROOT / "tools/enhancement_proposal_rules.json"
        rules.update(json.loads(overlay_path.read_text(encoding="utf-8")))
        rules.pop("bookMiningGrades", None)  # Retired Branch manuscript multiplier, not Efficiency.
    if args.health_preview:
        rules["healthModel"] = "manuscript_only_rms"
    names: dict[str, list[str]] = {}
    for code, language in [("zh", "zh_cn"), ("en", "en_us")]:
        language_file = ROOT / f"src/main/resources/assets/googology/lang/{language}.json"
        translations = json.loads(language_file.read_text(encoding="utf-8"))
        names[code] = [re.sub(r"\s*·\s*Lv1\s*$", "", translations[key]) for key in rules["coreTranslationKeys"]]
    rules["coreNames"] = names
    if not args.proposal_preview:
        exported["rules"] = rules
    golden = work / "java-golden-cases.json"
    golden.write_text(json.dumps(exported, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    engine_path = ROOT / ("tools/enhancement_proposal_engine.js" if args.proposal_preview else "tools/enhancement_engine.js")
    engine = engine_path.read_text(encoding="utf-8")
    template = (ROOT / ("tools/enhancement_simulator.template.html" if args.proposal_preview else "tools/enhancement_simulator_current.template.html")).read_text(encoding="utf-8")
    build = {"caseCount": len(exported["cases"]), "ruleSourceSha256": hashlib.sha256(source.read_bytes()).hexdigest(),
             "engineSha256": hashlib.sha256(engine.encode("utf-8")).hexdigest(), "revision": rules["revision"], "healthPreview": args.health_preview, "proposalPreview": args.proposal_preview,
             "calculationSourceSha256": hashlib.sha256(compile_source.read_bytes()).hexdigest()}
    if args.proposal_preview:
        template = template.replace("__MODE_ZH__", "九核新方案预览（尚未应用于 Mod 0.3.11）").replace("__MODE_EN__", "Nine-core proposal preview (not applied to Mod 0.3.11)")
    elif args.health_preview:
        template = template.replace("__MODE_ZH__", "0.3.11 · 生命方案预览（尚未应用于 Mod）").replace("__MODE_EN__", "0.3.11 · Health proposal preview (not applied to the Mod)")
    else:
        template = template.replace("__MODE_ZH__", "0.3.11 · 当前 Mod 规则").replace("__MODE_EN__", "0.3.11 · Current Mod rules")
    if args.proposal_preview:
        build["proposalRevision"] = rules["proposalRevision"]
        build["proposalRulesSha256"] = hashlib.sha256(overlay_path.read_bytes()).hexdigest()
        (work / "proposal-rules.json").write_text(json.dumps(rules, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    health_zh = "生命仅取手稿晶核；果糕基值全维度相同，深层仍由手稿序数放大。" if args.health_preview else "生命按当前 Mod 规则计算，包含护甲序数与手稿效果。"
    health_en = "Health uses manuscript cores only; Guogao base health is realm-independent, while deep manuscript Ordinal cores still multiply it." if args.health_preview else "Health follows current Mod rules, including armor Ordinal and manuscript effects."
    template = template.replace("__HEALTH_INFO_ZH__", health_zh).replace("__HEALTH_INFO_EN__", health_en)
    html = template.replace("__RULES_JSON__", json.dumps(rules, ensure_ascii=False, separators=(",", ":")).replace("<", "\\u003c"))
    html = html.replace("__ENGINE_JS__", engine).replace("__BUILD_JSON__", json.dumps(build, separators=(",", ":")))
    if re.search(r"__(?:RULES_JSON|ENGINE_JS|BUILD_JSON|MODE_ZH|MODE_EN|HEALTH_INFO_ZH|HEALTH_INFO_EN)__", html):
        raise SystemExit("Unresolved template marker.")
    candidate = work / "enhancement-simulator.html"
    candidate.write_text(html, encoding="utf-8", newline="\n")
    if args.proposal_preview:
        print(run([args.node, str(ROOT / "tools/tests/check_enhancement_proposal.js"), str(work / "proposal-rules.json"), str(candidate)]).strip())
    else:
        print(run([args.node, str(ROOT / "tools/tests/check_enhancement_simulator.js"), str(golden), str(candidate)]).strip())
    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(html, encoding="utf-8", newline="\n")
    validation_label = "independent proposal" if args.proposal_preview else f"Java golden cases: {len(exported['cases'])}"
    print(f"Generated {output} ({len(html.encode('utf-8')):,} bytes; {validation_label})")
    # The reference is a design proposal, never label it as production rules.
    if not args.proposal_preview:
        return
    reference = (ROOT / "tools/enhancement_reference.template.html").read_text(encoding="utf-8")
    reference = reference.replace("__RULES_JSON__", json.dumps(rules, ensure_ascii=False, separators=(",", ":")).replace("<", "\\u003c"))
    reference = reference.replace("__BUILD_JSON__", json.dumps(build, separators=(",", ":")))
    mode_zh = "九核新方案预览（尚未应用于 Mod 0.3.11）" if args.proposal_preview else "0.3.11 · 生命方案预览（尚未应用于 Mod）" if args.health_preview else "0.3.11 · 当前 Mod 规则"
    mode_en = "Nine-core proposal preview (not applied to Mod 0.3.11)" if args.proposal_preview else "0.3.11 · Health proposal preview (not applied to the Mod)" if args.health_preview else "0.3.11 · Current Mod rules"
    reference = reference.replace("__MODE_ZH__", mode_zh).replace("__MODE_EN__", mode_en)
    if re.search(r"__(?:RULES_JSON|BUILD_JSON|MODE_ZH|MODE_EN)__", reference):
        raise SystemExit("Unresolved reference template marker.")
    reference_path = output.with_name("core-effects-reference.html")
    reference_path.write_text(reference, encoding="utf-8", newline="\n")
    print(f"Generated {reference_path} ({len(reference.encode('utf-8')):,} bytes; 9 core types, 28 grade entries)")


if __name__ == "__main__":
    main()
