"""Measure release portal travel in a new, hidden, unlocked fo262 copy only.

fixture060 creates a genuine released-0.6.0 save. upgrade copies that save and
checks it before any new fixture changes. The normal installation is read-only.
"""
from __future__ import annotations

import argparse
import json
import math
import shutil
import statistics
import subprocess
from pathlib import Path
from zipfile import ZipFile

from run_release_263_qa import library_classpath, read_json, sha256

ROOT = Path(__file__).resolve().parents[1]


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def mod_metadata(jar):
    with ZipFile(jar) as archive:
        return json.loads(archive.read("fabric.mod.json"), strict=False)


def inventory(instance):
    files = [instance / "fo262.json", instance / "fo262.jar"]
    files += sorted(path for path in (instance / "mods").rglob("*") if path.is_file())
    return {str(path.relative_to(instance)): sha256(path) for path in files}


def tree_inventory(folder):
    return {str(path.relative_to(folder)): sha256(path) for path in sorted(folder.rglob("*"))
            if path.is_file() and path.name != "session.lock"}


def summarize(result):
    """Descriptive measurements; injected failures never enter success rates."""
    summary = {}
    for group in ("warm", "cold", "compat"):
        rows = [row for row in result.get("trials", []) if row.get("group") == group]
        if not rows:
            continue
        times = sorted(row["wallMs"] for row in rows)
        ticks = sorted(row["ticks"] for row in rows)
        success = sum(row["success"] for row in rows)
        summary[group] = {
            "count": len(rows), "success": success, "failures": len(rows) - success,
            "successRate": success / len(rows),
            "wallMs": {"median": statistics.median(times), "p95": times[math.ceil(len(times) * .95) - 1], "max": max(times)},
            "ticks": {"median": statistics.median(ticks), "p95": ticks[math.ceil(len(ticks) * .95) - 1], "max": max(ticks)},
        }
    return summary


def run(args):
    instance = args.instance.resolve()
    require(instance.name == "fo262", "Only the read-only fo262 installation is accepted")
    before = inventory(instance)
    metadata = read_json(instance / "fo262.json")
    release = args.release_jar.resolve()
    main = mod_metadata(release)
    require(main["id"] == "guogaology" and main["depends"]["minecraft"] == "26.2", "Explicit Minecraft26.2 main release required")
    if args.phase == "fixture060":
        require(main["version"] == "0.6.0" and args.fixture is None, "fixture060 needs exact0.6.0 and a new world")
    require(args.phase != "upgrade" or args.fixture is not None, "upgrade requires --fixture from fixture060")
    require(1 <= args.rounds <= 20, "--rounds must be1..20")
    qa = args.qa_jar.resolve() if args.qa_jar else ROOT / "ports/26.2/build/qa" / ("guogaology-merge-qa-" + next(
        line.split("=", 1)[1].strip() for line in (ROOT / "gradle.properties").read_text().splitlines() if line.startswith("mod_version=")) + ".jar")
    helper = mod_metadata(qa)
    require(helper["id"] == "guogaology-merge-qa", "Explicit mergeQa helper required")
    with ZipFile(qa) as archive:
        require("dev/guogaology/mergeqa/PortalOptimizationRuntime.class" in archive.namelist(), "Compile latest :26.2:mergeQaJar first")
        require("dev/guogaology/qa/mixin/HiddenWindowMixin.class" in archive.namelist()
                and "dev/guogaology/qa/mixin/UnlockedMouseMixin.class" in archive.namelist(), "Hidden/unlocked QA mixins required")
    game = (ROOT / "build/portal-qa" / args.label).resolve()
    require(game.parent == (ROOT / "build/portal-qa").resolve() and not game.exists(), "New single-directory label required")
    fixture = args.fixture.resolve() if args.fixture else None
    fixture_before = None
    manifest = None
    if fixture:
        require(fixture.is_relative_to((ROOT / "build").resolve()), "Only a disposable build/ QA save may be copied")
        require((fixture / "level.dat").is_file(), "Fixture must be an actual saved world")
        if args.phase == "upgrade" or args.group == "compat":
            manifest = args.fixture_report.resolve() if args.fixture_report else fixture.parents[1] / "portal-fixture.json"
            require(manifest.is_relative_to((ROOT / "build").resolve()) and manifest.is_file(), "Actual fixture060 manifest required")
            require(read_json(manifest)["creatorVersion"] == "0.6.0", "Expected actual0.6.0 fixture provenance")
        fixture_before = tree_inventory(fixture)
    mods = game / "mods"
    mods.mkdir(parents=True)
    shutil.copy2(release, mods / release.name)
    shutil.copy2(qa, mods / qa.name)
    required = {"fabric-api"} | (set() if args.without_sodium else {"sodium"})
    copied = {}
    for jar in sorted((instance / "mods").glob("*.jar")):
        try:
            identity = mod_metadata(jar)["id"]
        except (KeyError, json.JSONDecodeError):
            continue
        if identity in required:
            require(identity not in copied, "Duplicate selected mod: " + identity)
            shutil.copy2(jar, mods / jar.name)
            copied[identity] = {"name": jar.name, "sha256": sha256(jar)}
    require(set(copied) == required, "Missing installed FabricAPI/Sodium")
    if fixture:
        shutil.copytree(fixture, game / "saves/port-qa", ignore=shutil.ignore_patterns("session.lock"))
        if manifest:
            shutil.copy2(manifest, game / "portal-fixture-input.json")
    (game / "options.txt").write_text("pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:4\nsimulationDistance:4\nsoundCategory_master:0.0\nlang:en_us\n", encoding="utf8")
    temp = game / "temp"
    temp.mkdir()
    classpath = library_classpath(metadata, instance.parent.parent, instance / "fo262.jar")
    command = [str(args.java_home.resolve() / "bin/java.exe"), "-Xmx4G", "-XX:StackShadowPages=32",
               "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED", "--enable-native-access=ALL-UNNAMED", "-Dfile.encoding=UTF-8",
               f"-XX:ErrorFile={game / 'hs_err_pid%p.log'}", "-Dfabric.development=false", "-Dguogaology.qa.portalOptimization=true",
               f"-Dguogaology.qa.portalPhase={args.phase}", f"-Dguogaology.qa.portalGroup={args.group}", f"-Dguogaology.qa.portalRounds={args.rounds}",
               f"-Dguogaology.qa.portalDeadlineSeconds={args.timeout - 30}", f"-Dguogaology.qa.portalNoPhotos={str(args.no_photos).lower()}",
               f"-Dguogaology.qa.freshworld={str(fixture is None).lower()}", f"-Dguogaology.qa.seed={args.seed}",
               f"-Djava.io.tmpdir={temp}", f"-Djna.tmpdir={temp}", f"-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}",
               "-cp", ";".join(map(str, classpath)), metadata["mainClass"], "--username", "GuogaologyQA", "--version", "26.2",
               "--gameDir", str(game), "--assetsDir", str(instance.parent.parent / "assets"), "--assetIndex", metadata["assetIndex"]["id"],
               "--uuid", "00000000000000000000000000000001", "--accessToken", "0", "--userType", "legacy", "--versionType", "release", "--width", "1600", "--height", "1000"]
    staged = {"phase": args.phase, "group": args.group, "rounds": args.rounds, "release": str(release), "releaseVersion": main["version"],
              "releaseSha256": sha256(release), "qaSha256": sha256(qa), "fixtureSource": str(fixture) if fixture else None,
              "fixtureFiles": fixture_before, "selectedMods": copied, "normalInventoryBefore": before, "hidden": True, "mouseUnlocked": True}
    (game / "portal-run.json").write_text(json.dumps(staged, indent=2), encoding="utf8")
    print(f"Hidden portal QA staged: {game}; {args.phase}/{args.group}, main={main['version']}", flush=True)
    if args.stage_only:
        require(before == inventory(instance), "Normal installation changed during staging")
        return
    with (game / "launch-output.log").open("w", encoding="utf8") as output:
        process = subprocess.Popen(command, cwd=game, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        (game / "qa-process.json").write_text(json.dumps({"pid": process.pid, "gameDir": str(game)}), encoding="utf8")
        try:
            code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            process.terminate()
            process.wait(timeout=30)
            code = -1
    require(before == inventory(instance), "Normal installation changed during QA")
    if fixture:
        require(fixture_before == tree_inventory(fixture), "Source old QA save changed; only its copied save may be opened")
    result_file = game / "portal-optimization-result.json"
    result = read_json(result_file) if result_file.is_file() else {"passed": False, "error": "No native result"}
    result["summary"] = summarize(result)
    result["processExitCode"] = code
    result["normalInstallationUnchanged"] = True
    result["oldFixtureSourceUnchanged"] = fixture is not None
    (game / "portal-summary.json").write_text(json.dumps(result, indent=2), encoding="utf8")
    print(json.dumps({"passed": result.get("passed"), "assertions": result.get("assertionCount"), "summary": result["summary"], "exit": code}, indent=2), flush=True)
    require(code == 0 and result.get("passed") is True and (game / "port-client-ok.txt").is_file(), f"Portal QA failed; inspect {result_file} and launch-output.log")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--instance", type=Path, required=True)
    parser.add_argument("--java-home", type=Path, required=True)
    parser.add_argument("--release-jar", type=Path, required=True)
    parser.add_argument("--qa-jar", type=Path)
    parser.add_argument("--label", required=True)
    parser.add_argument("--phase", choices=("fixture060", "upgrade", "suite"), default="suite")
    parser.add_argument("--group", choices=("all", "warm", "cold", "compat", "timeout"), default="all")
    parser.add_argument("--fixture", type=Path)
    parser.add_argument("--fixture-report", type=Path)
    parser.add_argument("--rounds", type=int, default=1)
    parser.add_argument("--seed", type=int, default=516062)
    parser.add_argument("--timeout", type=int, default=1800)
    parser.add_argument("--no-photos", action="store_true")
    parser.add_argument("--without-sodium", action="store_true")
    parser.add_argument("--stage-only", action="store_true")
    run(parser.parse_args())
