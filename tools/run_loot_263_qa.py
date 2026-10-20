"""Stage/run exact 26.3 loot compatibility controls in new build-only directories.

No normal saves, settings, accounts or resource packs are read or copied. The normal
fo263_2 installation supplies read-only launcher metadata, client, libraries and API.
The optional API/Loader overrides must already exist below this repository's build/.
"""
from __future__ import annotations

import argparse
from collections import Counter
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from zipfile import ZipFile

import run_release_263_qa as base

ROOT = base.ROOT
BUILD = (ROOT / "build").resolve()
WORLD = "loot067-fixture"
ENTRYPOINT = "dev.guogaology.releaseqa263.Loot067Checks"
BAD_TABLES = ("guogao_ruin", "guogao_sanctum", "hydra_ruin", "hydra_sanctum")
EXPECTED_VERSION = {"baseline": "0.6.0", "broken": "0.6.6", "fresh": "0.6.7", "upgrade": "0.6.7", "reopen": "0.6.7"}
OLD_SHA256 = {"baseline": "a101bfd94cdf5cb27646bba2b10f881665bad89e180e6e59cf67121cdf83fe39",
              "broken": "c72f91f4588b2271f812c4e0e0479a30ac06c66b7f0927a4cbe4cef38a9226f0"}


def in_build(path, description):
    value = path.resolve()
    base.require(value.is_relative_to(BUILD) and value != BUILD,
                 f"{description} must be below this repository's build/: {value}")
    return value


def tree_hashes(folder):
    # Includes the new 26.3 players/data and dimensions/... layout. Only lock bytes
    # are excluded; never assume the legacy region/ or playerdata/ layout.
    return {path.relative_to(folder).as_posix(): base.sha256(path)
            for path in sorted(folder.rglob("*")) if path.is_file() and path.name != "session.lock"}


def ensure_no_links(folder):
    base.require(not folder.is_symlink() and not getattr(folder, "is_junction", lambda: False)(), f"Linked fixture is forbidden: {folder}")
    for path in folder.rglob("*"):
        base.require(not path.is_symlink() and not getattr(path, "is_junction", lambda: False)(),
                     f"Linked fixture entry is forbidden: {path}")
        base.require(path.resolve().is_relative_to(folder), f"Fixture entry escapes source: {path}")


def library_path(library):
    relative = library.get("downloads", {}).get("artifact", {}).get("path")
    if relative:
        return Path(relative)
    group, artifact, version, *classifier = library["name"].split(":")
    suffix = "-" + classifier[0] if classifier else ""
    return Path(group.replace(".", "/")) / artifact / version / f"{artifact}-{version}{suffix}.jar"


def launcher_libraries(metadata, instance, args):
    if not args.loader_profile:
        base.require(args.loader_libraries is None, "--loader-libraries requires --loader-profile")
        classpath = base.library_classpath(metadata, instance.parent.parent, instance / "fo263_2.jar")
        loader = [entry["name"].split(":")[2] for entry in metadata["libraries"]
                  if entry["name"].startswith("net.fabricmc:fabric-loader:")]
        base.require(len(loader) == 1, "Installed metadata must name one Fabric Loader")
        return classpath, loader[0], None
    profile_path = in_build(args.loader_profile, "Loader profile")
    libraries_root = in_build(args.loader_libraries, "Loader library root") if args.loader_libraries else profile_path.parent / "libraries"
    profile = base.read_json(profile_path)
    base.require(profile.get("inheritsFrom") == "26.3", "Official Loader profile must inherit Minecraft26.3")
    base.require(profile.get("mainClass") == "net.fabricmc.loader.impl.launch.knot.KnotClient", "Unexpected Loader entrypoint")
    replacement = [entry for entry in profile["libraries"] if base.library_allowed(entry)]
    identities = {tuple(entry["name"].split(":")[:2]) for entry in replacement}
    filtered = dict(metadata)
    filtered["libraries"] = [entry for entry in metadata["libraries"]
                              if tuple(entry["name"].split(":")[:2]) not in identities]
    classpath = base.library_classpath(filtered, instance.parent.parent, instance / "fo263_2.jar")
    copied = []
    for entry in replacement:
        path = (libraries_root / library_path(entry)).resolve()
        base.require(path.is_relative_to(libraries_root.resolve()) and path.is_file(), f"Missing build-only Loader dependency: {path}")
        if entry.get("sha256"):
            base.require(base.sha256(path) == entry["sha256"], "Official Loader library SHA-256 mismatch: " + str(path))
        if path not in classpath:
            classpath.insert(len(classpath)-1, path)
        copied.append({"name": entry["name"], "path": str(path), "sha256": base.sha256(path)})
    loaders = [entry["name"].split(":")[2] for entry in replacement
               if entry["name"].startswith("net.fabricmc:fabric-loader:")]
    base.require(len(loaders) == 1, "Loader profile must name exactly one Fabric Loader")
    jvm_arguments = profile.get("arguments", {}).get("jvm", [])
    base.require(all(isinstance(value, str) and value.startswith("-D") for value in jvm_arguments), "Review non-property Loader JVM arguments before using this profile")
    return classpath, loaders[0], {"profile": str(profile_path), "sha256": base.sha256(profile_path), "libraries": copied, "jvmArguments": jvm_arguments}


def helper_copy(source, destination):
    metadata = base.mod_metadata(source)
    base.require(metadata.get("id") == base.QA_ID, "Expected separate26.3 QA helper")
    metadata["entrypoints"] = {"client": [ENTRYPOINT]}
    metadata["depends"]["guogaology"] = "*"
    with ZipFile(source) as original, ZipFile(destination, "x") as changed:
        base.require(ENTRYPOINT.replace(".", "/") + ".class" in original.namelist(), "Rebuild releaseQa263Jar with Loot067Checks first")
        for entry in original.infolist():
            data = json.dumps(metadata, ensure_ascii=False, indent=2).encode("utf-8") if entry.filename == "fabric.mod.json" else original.read(entry.filename)
            changed.writestr(entry, data)
    return metadata


def source_fixture(args):
    if args.mode in ("baseline", "fresh"):
        base.require(args.source_world is None, "New-world modes do not accept --source-world")
        return None, None
    base.require(args.source_world is not None, "This mode requires --source-world")
    source = in_build(args.source_world, "Source fixture")
    base.require(source.name == WORLD and source.parent.name == "saves", "Source must be an earlier loot067-fixture save")
    ensure_no_links(source)
    base.require(source.is_dir(), "Source fixture does not exist")
    marker = base.read_json(source.parent.parent / "loot067-ok.json")
    base.require(marker.get("success") is True and marker.get("serverStopped") is True,
                 "Source fixture must come from an actual clean-stop QA pass")
    if args.mode in ("broken", "upgrade"):
        base.require(marker.get("mode") == "baseline" and marker.get("version") == "0.6.0",
                     "Broken/upgrade controls each require the pristine exact0.6.0 writer output")
    else:
        base.require(marker.get("mode") in ("fresh", "upgrade") and marker.get("version") == "0.6.7",
                     "Reopen verifies an actual0.6.7 saved fresh/upgrade world")
    fixture = base.read_json(source / "loot067-fixture.json")
    base.require(fixture.get("fixtureOnly") is True and len(fixture.get("dimensions", [])) == 4,
                 "Source must contain all four written fixture dimensions")
    return source, tree_hashes(source)


def prepare(args):
    base.require(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", args.label) is not None
                 and not args.label.endswith((".", " ")) and args.label.upper().split(".")[0]
                 not in {"CON", "PRN", "AUX", "NUL", *(f"COM{i}" for i in range(1,10)), *(f"LPT{i}" for i in range(1,10))}, "Invalid one-directory --label")
    base.require(-(1 << 63) <= args.seed < (1 << 63) and args.timeout > 0, "Invalid seed/timeout")
    instance = args.instance.resolve()
    base.require(instance.name == "fo263_2" and instance.parent.name == "versions", "Read-only fo263_2 metadata only")
    metadata = base.read_json(instance / "fo263_2.json")
    base.require(metadata.get("id") == "fo263_2" and metadata.get("javaVersion", {}).get("majorVersion") == 25, "Expected26.3/Java25 metadata")
    base.require(metadata.get("mainClass") == "net.fabricmc.loader.impl.launch.knot.KnotClient", "Expected Fabric launcher")
    with ZipFile(instance / "fo263_2.jar") as archive:
        base.require(json.loads(archive.read("version.json"))["id"] == "26.3", "Expected actual26.3 client")
    source, source_hashes = source_fixture(args)
    production = (args.artifact or ROOT / "ports/26.3/build/libs/guogaology-dimension-26.3-0.6.7.jar").resolve()
    production_metadata = base.validate_mod(production, "guogaology", EXPECTED_VERSION[args.mode])
    if args.mode in OLD_SHA256:
        base.require(base.sha256(production) == OLD_SHA256[args.mode], "Old/broken control must use the exact independently recorded release SHA-256")
    helper = (args.helper or ROOT / "ports/26.3/build/qa/guogaology-release-263-qa-0.6.7.jar").resolve()
    base.require(helper.is_file(), f"Missing separate QA helper: {helper}")
    classpath, loader_version, loader_override = launcher_libraries(metadata, instance, args)
    java = base.java_executable(args.java_home)
    # selected_mods is shared with the original hidden launcher; Voxy is intentionally absent.
    extras = base.selected_mods(instance, argparse.Namespace(without_sodium=args.without_sodium, voxy=False))
    if args.api_jar:
        api = in_build(args.api_jar, "Fabric API override")
        api_metadata = base.mod_metadata(api)
        base.require(api_metadata.get("id") == "fabric-api" and "26.3" in api_metadata.get("version", ""), "Wrong API override")
        extras = [(jar, info) for jar, info in extras if info.get("id") != "fabric-api"] + [(api, api_metadata)]
    api_versions = [info["version"] for _, info in extras if info["id"] == "fabric-api"]
    base.require(len(api_versions) == 1, "Exactly one Fabric API")
    before = base.normal_inventory(instance)
    runtime_root = (BUILD / "runtime-qa263").resolve()
    game = (runtime_root / args.label).resolve()
    base.require(game.parent == runtime_root and not game.exists(), "Fresh direct build/runtime-qa263 directory required")
    runtime_root.mkdir(exist_ok=True, parents=True); game.mkdir()
    mods = game / "mods"; mods.mkdir()
    copied = []
    for jar, info in [(production, production_metadata), (helper, base.mod_metadata(helper)), *extras]:
        destination = mods / jar.name
        base.require(not destination.exists(), "Duplicate staged Mod filename")
        if jar == helper:
            info = helper_copy(jar, destination)
        else:
            shutil.copy2(jar, destination); base.require(base.sha256(jar) == base.sha256(destination), "Staged artifact hash mismatch")
        copied.append({"id": info["id"], "version": info["version"], "file": jar.name, "source": str(jar), "sourceSha256": base.sha256(jar), "sha256": base.sha256(destination)})
    input_record = {"mode": args.mode, "minecraft": "26.3", "productionVersion": production_metadata["version"],
                    "productionSha256": base.sha256(production), "buildRoot": str(BUILD), "gameDirectory": str(game),
                    "seed": args.seed, "apiVersion": api_versions[0], "loaderVersion": loader_version,
                    "loaderOverride": loader_override, "mods": copied, "language": args.language,
                    "sourceWorld": str(source) if source else None, "sourceWorldFiles": source_hashes,
                    "boundary": "Real hidden26.3 client/integrated server. Fresh build-only worlds/copies; no normal saves/configuration read or copied."}
    base.write_json_new(game / "loot067-inputs.json", input_record)
    base.write_json_new(game / "qa263-isolated.json", {"minecraft": "26.3", "mode": args.mode, "seed": args.seed})
    base.write_json_new(game / "normal-instance-before.json", before)
    if source:
        destination = game / "saves" / WORLD
        shutil.copytree(source, destination, ignore=shutil.ignore_patterns("session.lock"))
        base.require(tree_hashes(destination) == source_hashes, "Whole-save copy must match the clean source")
        base.write_json_new(game / "copied-world-before.json", source_hashes)
    (game / "config").mkdir(); temp = game / "temp"; temp.mkdir()
    texture = "textureFiltering:1\nmaxAnisotropyBit:1\n" if args.known_filter_workaround else "textureFiltering:0\nmaxAnisotropyBit:2\n"
    (game / "options.txt").write_text("pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:3\nsimulationDistance:3\nsoundCategory_master:0.0\n" + texture + "lang:" + args.language + "\n", encoding="utf-8")
    assets = instance.parent.parent / "assets"
    base.require((assets / "indexes" / f"{metadata['assetIndex']['id']}.json").is_file(), "Missing read-only Minecraft assets")
    command = [str(java), "-Xmx4G", "-XX:StackShadowPages=32", "--enable-native-access=ALL-UNNAMED",
               "--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED", "-Dfile.encoding=UTF-8",
               f"-XX:ErrorFile={game / 'hs_err_pid%p.log'}", "-Dfabric.development=false", "-Dguogaology.qa263=true",
               "-Dguogaology.loot067=true", f"-Dguogaology.qa263.seed={args.seed}",
               f"-Djava.io.tmpdir={temp}", f"-Djna.tmpdir={temp}", f"-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}",
               "-cp", ";".join(map(str, classpath)), metadata["mainClass"],
               "--username", "GuogaologyQA", "--version", "26.3", "--gameDir", str(game),
               "--assetsDir", str(assets), "--assetIndex", metadata["assetIndex"]["id"],
               "--uuid", "00000000000000000000000000000001", "--accessToken", "0", "--userType", "legacy",
               "--versionType", "release", "--width", "1600", "--height", "1000"]
    if loader_override:
        command[1:1] = loader_override["jvmArguments"]
    base.write_json_new(game / "staging.json", {**input_record, "command": command, "classpathEntries": len(classpath), "normalInstance": str(instance)})
    return game, instance, before, source, source_hashes, command


def verify_positive(game, args):
    base.require(not (game / "loot067-failed.txt").exists() and (game / "loot067-world-started.json").is_file(), "Actual world must start without QA failure")
    result = base.read_json(game / "loot067-ok.json")
    base.require(result.get("success") is True and result.get("serverStopped") is True and result.get("nativeSaveCompleted") is True, "Positive marker requires native save and SERVER_STOPPED")
    base.require(result.get("mode") == args.mode and result.get("version") == EXPECTED_VERSION[args.mode] and result.get("minecraft") == "26.3", "Wrong positive result identity")
    base.require(result.get("fixtureDimensions") == 4 and result.get("unopenedChests") == 4 and result.get("persistedLegacyBlockStates") == 12 and result.get("persistedPlayerStacks") == 4, "Incomplete saved-fixture coverage")
    base.require(result.get("hidden") is True and result.get("mouseUnlocked") is True, "Hidden unlocked runtime evidence required")
    if args.mode != "baseline":
        base.require(result.get("loadedLootTables") == 248 and len(result.get("chests", [])) == 16
                     and result.get("chestFills") == 8196 and result.get("actualBlockEntityChestFills") == 4
                     and len(result.get("entityLoot", [])) == 2, "Incomplete actual loot execution")
    expected = [f"loot067-{args.mode}-world.png"] + ([] if args.mode == "baseline" else [f"loot067-{args.mode}-chest.png"])
    base.require(result.get("screenshots") == expected, "Required settled native screenshots")
    for name in expected:
        base.verify_png(game, name)
    world = game / "saves" / WORLD
    base.require((world / "level.dat").is_file() and (world / "data/minecraft/world_gen_settings.dat").is_file(), "26.3 native metadata must exist")
    base.require(any((world / "players/data").glob("*.dat")), "Actual26.3 player save must exist")
    for dimension in ("minecraft/overworld", "guogaology/outer", "guogaology/guogaology", "guogaology/guogao"):
        base.require(any((world / "dimensions" / dimension / "region").glob("*.mca")), "Actual saved native dimension regions missing: " + dimension)
    log = (game / "launch-output.log").read_text(encoding="utf-8", errors="replace")
    base.require("LOOT067_CLEAN_STOP_OK" in log and "Saving players" in log, "Clean server stop/save log is required")
    base.write_json_new(game / "saved-world-after.json", tree_hashes(world))
    return result


def verify_broken(game):
    base.require(not (game / "loot067-ok.json").exists() and not (game / "loot067-world-started.json").exists()
                 and not (game / "loot067-failed.txt").exists(), "Broken control must never report success/start a world or mask an unrelated QA exception")
    rejected = base.read_json(game / "loot067-rejected.json")
    base.require(rejected.get("screen") == "DatapackLoadFailureScreen" and rejected.get("productionVersion") == "0.6.6"
                 and rejected.get("serverStarted") is False, "Require actual native datapack failure before server startup")
    log = (game / "launch-output.log").read_text(encoding="utf-8", errors="replace")
    base.require("No key items" in log and ("Failed to load registries" in log or "Failed to load level data or datapacks" in log), "Exact missing-items registry failure required")
    for table in BAD_TABLES:
        base.require("guogaology:chests/" + table in log, "Missing original failing table in native log: " + table)
    # The outer registry loader may repeat the aggregated error. Report the actual
    # occurrence counts rather than pretending every repeated line is a distinct entry.
    diagnostics = re.findall(r"No key items in MapLike\[(\{[^\r\n]*?\})\]", log)
    missing_tags = Counter()
    for fragment in diagnostics:
        try:
            entry = json.loads(fragment)
        except json.JSONDecodeError:
            continue
        if entry.get("type") == "minecraft:tag":
            missing_tags[entry.get("name")] += 1
    for tag in ("fruit_guogao", "ordinal_branch_stones", "ordinal_flowers"):
        base.require(missing_tags["guogaology:" + tag] >= 2, "Native log must cover both original entries for tag " + tag)
    before = base.read_json(game / "copied-world-before.json")
    after = tree_hashes(game / "saves" / WORLD)
    changed = sorted(key for key in before.keys() | after.keys() if before.get(key) != after.get(key))
    base.require(not changed, "Rejected066 must not save/alter any fixture content: " + ", ".join(changed))
    base.verify_png(game, rejected["screenshot"])
    result = {"expectedRejectionConfirmed": True, "productionVersion": "0.6.6", "tables": list(BAD_TABLES),
              "expectedDistinctLegacyEntries": 6, "nativeMissingTagDiagnosticOccurrences": dict(missing_tags),
              "sourceCopyContentUnchanged": True, "successMarkerAbsent": True, "serverNeverStarted": True,
              "screenshot": rejected["screenshot"]}
    base.write_json_new(game / "loot067-negative-verification.json", result)
    return result


def run(args):
    game, instance, before, source, source_hashes, command = prepare(args)
    print(f"Hidden26.3 loot QA {args.mode}: {game}", flush=True)
    try:
        if args.prepare_only:
            print("PREPARED_ONLY: no Minecraft process was launched", flush=True)
            return
        base.require(os.name == "nt", "Hidden launcher supports Windows only")
        environment = os.environ.copy()
        for key in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
            environment.pop(key, None)
        environment.update(TEMP=str(game / "temp"), TMP=str(game / "temp"))
        with (game / "launch-output.log").open("x", encoding="utf-8") as output:
            process = subprocess.Popen(command, cwd=game, env=environment, stdout=output,
                                       stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
            base.write_json_new(game / "qa-process.json", {"pid": process.pid, "gameDirectory": str(game), "mode": args.mode})
            try:
                code = process.wait(timeout=args.timeout)
            except subprocess.TimeoutExpired as error:
                process.terminate()
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    process.kill(); process.wait(timeout=20)
                raise RuntimeError("Own QA JVM timed out; timeout is never an expected-rejection pass") from error
        base.require(code == 0, f"Own QA JVM exit {code}; no crash counts as a successful control")
        result = verify_broken(game) if args.mode == "broken" else verify_positive(game, args)
        print("LOOT067_EXPECTED_REJECTION_OK" if args.mode == "broken" else "LOOT067_RUNTIME_OK", json.dumps(result, ensure_ascii=False), flush=True)
    finally:
        base.verify_normal_unchanged(game, instance, before)
        if source:
            unchanged = tree_hashes(source) == source_hashes
            base.write_json_new(game / "source-world-after.json", {"unchanged": unchanged, "files": tree_hashes(source)})
            base.require(unchanged, "Read-only source fixture changed")
        print("Normal instance metadata/mods and original source fixture: unchanged", flush=True)


def parser():
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--instance", type=Path, required=True)
    result.add_argument("--java-home", type=Path, required=True)
    result.add_argument("--label", required=True, help="New build/runtime-qa263 child directory")
    result.add_argument("--mode", choices=tuple(EXPECTED_VERSION), required=True)
    result.add_argument("--artifact", type=Path, help="Exact old/broken/fixed26.3 production JAR")
    result.add_argument("--helper", type=Path, help="Separate current releaseQa263Jar")
    result.add_argument("--source-world", type=Path, help="Earlier clean-stop build-only saves/loot067-fixture")
    result.add_argument("--api-jar", type=Path, help="Optional build-only Fabric API override")
    result.add_argument("--loader-profile", type=Path, help="Optional build-only official Fabric profile JSON")
    result.add_argument("--loader-libraries", type=Path, help="Build-only Maven tree for that profile")
    result.add_argument("--seed", type=int, default=67060)
    result.add_argument("--language", choices=("zh_cn", "en_us"), default="zh_cn")
    result.add_argument("--without-sodium", action="store_true")
    result.add_argument("--known-filter-workaround", action="store_true")
    result.add_argument("--prepare-only", action="store_true")
    result.add_argument("--timeout", type=int, default=1100)
    return result


if __name__ == "__main__":
    try:
        run(parser().parse_args())
    except (OSError, KeyError, ValueError, RuntimeError, subprocess.SubprocessError) as error:
        print(f"LOOT067_QA_ERROR: {error}", file=sys.stderr)
        sys.exit(1)
