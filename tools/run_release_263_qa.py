"""Stage or run the exact release in a fresh, hidden Minecraft 26.3 QA world.

The fo263_2 installation supplies read-only metadata, libraries and selected mods.
Accounts, settings, resource packs and saves are never copied from that instance.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
TARGET = "26.3"
QA_ID = "guogaology-release-263-qa"


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_json_new(path, value):
    with path.open("x", encoding="utf-8") as output:
        json.dump(value, output, ensure_ascii=False, indent=2)
        output.write("\n")


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def normal_inventory(instance):
    # Deliberately read neither the normal saves/configuration nor launcher accounts.
    files = [instance / "fo263_2.json", instance / "fo263_2.jar"]
    files += sorted(path for path in (instance / "mods").rglob("*") if path.is_file())
    return {str(path.relative_to(instance)): sha256(path) for path in files}


def mod_metadata(jar, *, strict=True):
    with ZipFile(jar) as archive:
        return json.loads(archive.read("fabric.mod.json"), strict=strict)


def validate_mod(jar, identity, release):
    require(jar.is_file(), f"Missing {identity} JAR: {jar}")
    metadata = mod_metadata(jar)
    require(metadata.get("id") == identity, f"Wrong Mod ID in {jar.name}")
    require(metadata.get("version") == release, f"Wrong release version in {jar.name}")
    require(metadata.get("depends", {}).get("minecraft") == TARGET,
            f"{jar.name} must explicitly target Minecraft {TARGET}")
    return metadata


def copy_startup_helper(source, destination, metadata):
    """Change only the disposable helper's entrypoint and main-Mod dependency."""
    control_metadata = json.loads(json.dumps(metadata))
    control_metadata["entrypoints"] = {"client": ["dev.guogaology.releaseqa263.Startup263Control"]}
    control_metadata["depends"].pop("guogaology", None)
    with ZipFile(source) as original, ZipFile(destination, "x") as control:
        require("dev/guogaology/releaseqa263/Startup263Control.class" in original.namelist(),
                "QA helper must be rebuilt with Startup263Control before staging a control")
        for entry in original.infolist():
            data = (json.dumps(control_metadata, ensure_ascii=False, indent=2).encode("utf-8")
                    if entry.filename == "fabric.mod.json" else original.read(entry.filename))
            control.writestr(entry, data)


def library_allowed(library):
    allowed = not library.get("rules")
    for rule in library.get("rules", []):
        target_os = rule.get("os", {})
        if target_os.get("name", "windows") != "windows":
            continue
        if target_os.get("arch", "x86_64") not in ("x86_64", "amd64"):
            continue
        if target_os.get("version") and not re.search(target_os["version"], "10.0"):
            continue
        if any(value is not False for value in rule.get("features", {}).values()):
            continue
        allowed = rule.get("action") == "allow"
    name = library["name"]
    if "natives-" in name:
        allowed = allowed and ":natives-windows" in name
        allowed = allowed and not any(token in name for token in ("windows-arm64", "windows-x86"))
    return allowed


def library_classpath(metadata, minecraft, client_jar):
    folder = (minecraft / "libraries").resolve()
    classpath = []
    for library in metadata["libraries"]:
        if not library_allowed(library):
            continue
        relative = library.get("downloads", {}).get("artifact", {}).get("path")
        if not relative:
            group, artifact, version, *classifier = library["name"].split(":")
            suffix = "-" + classifier[0] if classifier else ""
            relative = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}{suffix}.jar"
        full = (folder / relative).resolve()
        require(full.is_relative_to(folder), f"Library path escapes libraries/: {relative}")
        require(full.is_file(), f"Missing installed library: {full}")
        if full not in classpath:
            classpath.append(full)
    classpath.append(client_jar)
    return classpath


def selected_mods(instance, args):
    required = {"fabric-api"}
    if not args.without_sodium:
        required.add("sodium")
    if args.voxy:
        require(not args.without_sodium, "--voxy requires Sodium; omit --without-sodium")
        required.update(("voxy", "cloth-config"))
    candidates = {identity: [] for identity in required}
    for jar in sorted((instance / "mods").glob("*.jar")):
        try:
            metadata = mod_metadata(jar, strict=False)
        except (KeyError, json.JSONDecodeError):  # Unrelated JARs are not candidates.
            continue
        if metadata.get("id") in candidates:
            candidates[metadata["id"]].append((jar, metadata))
    chosen = []
    for identity, matches in sorted(candidates.items()):
        require(len(matches) == 1,
                f"Expected one installed {identity} JAR, found {len(matches)} in {instance / 'mods'}")
        chosen.append(matches[0])
    return chosen


def java_executable(java_home):
    executable = java_home.resolve() / "bin" / "java.exe"
    require(executable.is_file(), f"Missing Java executable: {executable}")
    result = subprocess.run([str(executable), "-version"], capture_output=True,
                            text=True, timeout=15, check=True)
    version = result.stderr + result.stdout
    require(re.search(r'(?:java|openjdk) version "25(?:[.\-+\"]|$)', version) is not None,
            f"Minecraft {TARGET} QA requires Java 25: {version.strip()}")
    return executable


def prepare(args):
    require(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", args.label) is not None,
            "--label must be one new directory name using letters, digits, '.', '_' or '-'")
    require(not args.label.endswith((".", " ")) and args.label.upper().split(".")[0]
            not in {"CON", "PRN", "AUX", "NUL", *(f"COM{i}" for i in range(1, 10)),
                    *(f"LPT{i}" for i in range(1, 10))}, "Invalid Windows directory label")
    require(-(1 << 63) <= args.seed < (1 << 63), "--seed must fit a signed Java long")
    require(args.timeout > 0, "--timeout must be positive")
    require(not (args.startup_control and args.voxy), "--startup-control supports Fabric API and optional Sodium only")
    instance = args.instance.resolve()
    require(instance.name == "fo263_2" and instance.parent.name == "versions",
            "Only the read-only versions/fo263_2 installation is supported")
    metadata = read_json(instance / "fo263_2.json")
    require(metadata.get("id") == "fo263_2", "Unexpected instance metadata ID")
    require(metadata.get("mainClass") == "net.fabricmc.loader.impl.launch.knot.KnotClient",
            "Expected the installed Fabric Knot client main class")
    require(metadata.get("javaVersion", {}).get("majorVersion") == 25,
            "Instance metadata must specify Java 25")
    client_jar = instance / "fo263_2.jar"
    with ZipFile(client_jar) as archive:
        require(json.loads(archive.read("version.json")).get("id") == TARGET,
                "The installed client JAR is not Minecraft 26.3")
    properties = (ROOT / "gradle.properties").read_text(encoding="utf-8-sig")
    match = re.search(r"^mod_version\s*=\s*(\S+)\s*$", properties, re.MULTILINE)
    require(match is not None, "Missing mod_version in gradle.properties")
    release = match[1]
    production = ROOT / "ports/26.3/build/libs" / f"guogaology-dimension-26.3-{release}.jar"
    helper = ROOT / "ports/26.3/build/qa" / f"{QA_ID}-{release}.jar"
    if not args.startup_control:
        validate_mod(production, "guogaology", release)
    helper_metadata = validate_mod(helper, QA_ID, release)
    require(helper_metadata.get("entrypoints", {}).get("client"), "QA helper has no client entrypoint")
    minecraft = instance.parent.parent
    classpath = library_classpath(metadata, minecraft, client_jar)
    assets = minecraft / "assets"
    require((assets / "indexes" / f"{metadata['assetIndex']['id']}.json").is_file(),
            "Installed Minecraft asset index is missing")
    java = java_executable(args.java_home)
    extras = selected_mods(instance, args)
    before = normal_inventory(instance)
    runtime_root = (ROOT / "build/runtime-qa263").resolve()
    game = (runtime_root / args.label).resolve()
    require(game.parent == runtime_root, "QA directory must be a direct child of build/runtime-qa263")
    require(not game.exists(), "QA directory already exists; use a new --label to avoid stale results")
    runtime_root.mkdir(parents=True, exist_ok=True)
    game.mkdir()  # Never reuse an existing world or success marker.
    write_json_new(game / "qa263-isolated.json", {"version": release, "label": args.label,
                   "minecraft": TARGET, "seed": args.seed, "gameDirectory": str(game),
                   "control": args.startup_control})
    write_json_new(game / "normal-instance-before.json", before)
    mods = game / "mods"
    mods.mkdir()
    copied = []
    main_mods = [] if args.startup_control else [(production, mod_metadata(production))]
    for jar, artifact_metadata in [*main_mods, (helper, helper_metadata), *extras]:
        destination = mods / jar.name
        require(not destination.exists(), f"Conflicting copied JAR filename: {jar.name}")
        source_digest = sha256(jar)
        if args.startup_control and jar == helper:
            copy_startup_helper(jar, destination, helper_metadata)
        else:
            shutil.copy2(jar, destination)
            require(sha256(destination) == source_digest, f"Copied JAR hash mismatch: {jar.name}")
        copied.append({"file": jar.name, "id": artifact_metadata["id"], "version": artifact_metadata["version"],
                       "sha256": sha256(destination), "sourceSha256": source_digest, "source": str(jar)})
    (game / "config").mkdir()
    temp = game / "temp"
    temp.mkdir()
    texture_options = ("textureFiltering:1\nmaxAnisotropyBit:1\n" if args.known_filter_workaround
                       else "textureFiltering:0\nmaxAnisotropyBit:2\n")
    (game / "options.txt").write_text(
        "pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:5\nsimulationDistance:5\n"
        + texture_options + "soundCategory_master:0.0\nlang:" + args.language + "\n", encoding="utf-8")
    # Match the installed 26.3 launcher metadata's native-stack/Unsafe access settings.
    command = [str(java), "-Xmx4G", "-XX:StackShadowPages=32",
               "--enable-native-access=ALL-UNNAMED", "--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED",
               f"-XX:ErrorFile={game / 'hs_err_pid%p.log'}", "-Dfile.encoding=UTF-8",
               "-Dfabric.development=false", "-Dguogaology.qa263=true",
               f"-Dguogaology.qa263.control={str(args.startup_control).lower()}",
               f"-Dguogaology.qa263.seed={args.seed}", f"-Dguogaology.qa263.release={release}",
               f"-Djava.io.tmpdir={temp}", f"-Djna.tmpdir={temp}",
               f"-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}",
               "-cp", ";".join(map(str, classpath)), metadata["mainClass"],
               "--username", "GuogaologyQA", "--version", TARGET, "--gameDir", str(game),
               "--assetsDir", str(assets), "--assetIndex", metadata["assetIndex"]["id"],
               "--uuid", "00000000000000000000000000000001", "--accessToken", "0",
               "--userType", "legacy", "--versionType", "release", "--width", "1600", "--height", "1000"]
    write_json_new(game / "staging.json", {"release": release, "minecraft": TARGET,
                   "normalInstance": str(instance), "mods": copied, "classpathEntries": len(classpath),
                   "sodium": not args.without_sodium, "voxy": args.voxy, "seed": args.seed,
                   "knownFilterWorkaround": args.known_filter_workaround,
                   "control": args.startup_control,
                   "command": command})
    return game, instance, before, release, command


def verify_normal_unchanged(game, instance, before):
    after = normal_inventory(instance)
    changed = sorted(key for key in before.keys() | after.keys() if before.get(key) != after.get(key))
    write_json_new(game / "normal-instance-after.json", {"unchanged": not changed,
                   "changed": changed, "files": after})
    require(not changed, "Normal instance changed during QA: " + ", ".join(changed))


def verify_png(game, name):
    require(isinstance(name, str) and Path(name).name == name and name.endswith(".png"),
            "Invalid QA screenshot filename")
    picture = game / "screenshots" / name
    require(picture.is_file() and picture.stat().st_size > 8, f"Missing native screenshot: {name}")
    with picture.open("rb") as source:
        require(source.read(8) == b"\x89PNG\r\n\x1a\n", f"Invalid PNG screenshot: {name}")


def verify_control_result(game, release, args):
    result = read_json(game / "startup-263-control-ok.json")
    require(result.get("success") is True and result.get("control") is True
            and result.get("version") == release and result.get("minecraft") == TARGET,
            "Invalid startup-only control result")
    require(result.get("sodium") is (not args.without_sodium) and result.get("voxy") is False,
            "Startup control renderer combination differs from the staged mods")
    require(result.get("hidden") is True and result.get("mouseUnlocked") is True,
            "Startup control must confirm a hidden window and unlocked mouse")
    require(result.get("screenshot") == "startup263-title.png", "Startup control did not capture its title screen")
    verify_png(game, result["screenshot"])
    return result


def verify_result(game, release, args):
    result_file = game / "release-263-ok.json"
    result = read_json(result_file)
    require(result.get("success") is True and result.get("version") == release
            and result.get("minecraft") == TARGET and result.get("control") is not True,
            f"Invalid QA result: {result_file}")
    require(result.get("cores") == 28, "QA must verify all 28 core appearances and GUI items")
    require(result.get("movingBodiesSubmitted", 0) > 0, "QA rendered no animated core bodies")
    require(result.get("motionBlocksAudited") == 226,
            "QA must compare all 226 registered blocks against native collision/heightmap tags")
    require(result.get("sodium") is (not args.without_sodium)
            and result.get("voxy") is args.voxy, "QA renderer combination differs from the staged mods")
    expected_dimensions = {"guogaology:outer", "guogaology:guogaology", "guogaology:guogao"}
    generation = result.get("generation", [])
    require(isinstance(generation, list) and len(generation) == 3
            and {entry.get("dimension") for entry in generation} == expected_dimensions,
            "QA must generate all three custom dimensions")
    for entry in generation:
        require(entry.get("fullChunks", 0) >= 9 and entry.get("occupiedHeightColumns", 0) > 0,
                f"Incomplete terrain generation evidence: {entry.get('dimension')}")
        require(entry.get("sampledColumns") == 49 and entry.get("solidColumns", 0) > 0
                and entry.get("motionBlockingColumns", 0) > 0
                and entry.get("worldSurfaceMismatches") == 0,
                f"Terrain heightmaps must match real voxels and include solids: {entry.get('dimension')}")
    expected_scenes = ("all-28-cores", "core-detail", "core-item-gui", "outer", "inner",
                       "underworld", "overworld-return")
    expected_pictures = {f"release263-{index:02d}-{scene}.png"
                         for index, scene in enumerate(expected_scenes)}
    pictures = result.get("screenshots", [])
    require(isinstance(pictures, list) and all(isinstance(name, str) for name in pictures)
            and len(set(pictures)) >= 7 and expected_pictures.issubset(pictures),
            "QA must report all seven native screenshots")
    for name in pictures:
        verify_png(game, name)
    return result, len(set(pictures))


def run(args):
    game, instance, before, release, command = prepare(args)
    mode = "startup control" if args.startup_control else "production QA"
    print(f"Isolated hidden {TARGET} {mode}: {game}", flush=True)
    try:
        if args.prepare_only:
            print("PREPARED_ONLY: no Minecraft process was launched", flush=True)
            return
        require(os.name == "nt", "The hidden runner supports Windows only")
        environment = os.environ.copy()
        for key in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
            environment.pop(key, None)
        environment.update(TEMP=str(game / "temp"), TMP=str(game / "temp"))
        with (game / "launch-output.log").open("x", encoding="utf-8") as output:
            process = subprocess.Popen(command, cwd=game, env=environment, stdout=output,
                                       stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
            write_json_new(game / "qa-process.json", {"pid": process.pid, "gameDirectory": str(game),
                           "mode": mode, "minecraft": TARGET})
            try:
                code = process.wait(timeout=args.timeout)
            except subprocess.TimeoutExpired as error:
                process.terminate()  # This Popen instance owns only the disposable QA JVM.
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=20)
                raise RuntimeError(f"Own QA JVM exceeded {args.timeout} seconds") from error
        result_file = game / ("startup-263-control-ok.json" if args.startup_control else "release-263-ok.json")
        if code or not result_file.is_file():
            print((game / "launch-output.log").read_text(encoding="utf-8", errors="replace")[-14000:])
            raise RuntimeError(f"Minecraft QA failed (exit {code}); see {game / 'launch-output.log'}")
        if args.startup_control:
            result = verify_control_result(game, release, args)
            print(f"STARTUP_CONTROL_OK {TARGET} {release}; title screenshot only", flush=True)
        else:
            result, screenshot_count = verify_result(game, release, args)
            print(f"PRODUCTION_PORT_OK {TARGET} {release}; {screenshot_count} screenshots", flush=True)
        print(json.dumps(result, ensure_ascii=False), flush=True)
    finally:
        verify_normal_unchanged(game, instance, before)
        print("Normal fo263_2 metadata and mods: unchanged", flush=True)


def parser():
    argument_parser = argparse.ArgumentParser(description=__doc__)
    argument_parser.add_argument("--instance", type=Path, required=True,
                                 help="Read-only .minecraft/versions/fo263_2 directory")
    argument_parser.add_argument("--java-home", type=Path, required=True, help="Installed Java 25 home")
    argument_parser.add_argument("--label", required=True, help="New directory name in build/runtime-qa263")
    argument_parser.add_argument("--seed", type=int, default=651600, help="Fresh world seed (default: 651600)")
    argument_parser.add_argument("--language", choices=("zh_cn", "en_us"), default="zh_cn")
    argument_parser.add_argument("--without-sodium", action="store_true", help="Run the Fabric API baseline without Sodium")
    argument_parser.add_argument("--startup-control", action="store_true",
                                 help="Title-screen control without the production Mod; never counts as a production QA pass")
    argument_parser.add_argument("--known-filter-workaround", action="store_true",
                                 help="Use isolated1/1 texture filtering instead of the0/2 settings matching fo263_2")
    argument_parser.add_argument("--voxy", action="store_true", help="Require installed Voxy and Cloth Config in addition to Sodium")
    argument_parser.add_argument("--prepare-only", action="store_true", help="Validate and stage files; never launch Minecraft")
    argument_parser.add_argument("--timeout", type=int, default=1100, help="Own QA JVM time limit in seconds (default: 1100)")
    return argument_parser


if __name__ == "__main__":
    try:
        run(parser().parse_args())
    except (OSError, KeyError, ValueError, RuntimeError, subprocess.SubprocessError) as error:
        print(f"QA ERROR: {error}", file=sys.stderr)
        sys.exit(1)
