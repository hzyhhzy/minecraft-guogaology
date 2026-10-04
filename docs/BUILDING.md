# Building Guogaology 0.3.2

Use JDK 21 for Minecraft 1.21.1; the combined modern build runs with JDK 25 and targets the appropriate Java class version for each game. Python 3.10+ is required for the modern API/resource adapters and release audit. The normal build needs no pip packages.

## All four targets

PowerShell 7 works on Windows, Linux and macOS:

```powershell
./build-all.ps1 -Java21 '/path/to/jdk21' -Java25 '/path/to/jdk25' -Python python3
```

On Windows, use Windows JDK paths and `-Python python` (or the full path to Python). `JAVA21_HOME` and `JAVA25_HOME` can supply the JDK paths. `-Targets 26.2` builds one target. Use `-Offline` only after dependencies and Gradle distributions have been cached.

`-GradleCache` optionally selects a shared Gradle cache; `-Gradle21` and `-Gradle25` optionally select installed Gradle 8.13 / 9.6.0 launchers. Ordinary builds use the included Wrappers. No local paths are hardcoded.

## Individual targets without PowerShell

Use the appropriate `JAVA_HOME`. On Windows replace `gradlew` with `gradlew.bat`.

```sh
# JDK 21, Minecraft 1.21.1
./gradlew --no-daemon :build

# JDK 21 or 25, Minecraft 1.21.11
./ports/gradlew --no-daemon -p ports -Ptarget=1.21.11 -PportPython=python3 :1.21.11:build

# JDK 25, Minecraft 26.2
./ports/gradlew --no-daemon -p ports -Ptarget=26.2 -PportPython=python3 :26.2:build

# JDK 25, Minecraft 26.3
./ports/gradlew --no-daemon -p ports -Ptarget=26.3 -PportPython=python3 :26.3:build
```

Run `python tools/audit_resources.py --check` to validate the model/texture graph and `python tools/audit_localization.py --check` to check English and Chinese keys, arguments, live item/block names, UI references and Mod Menu descriptions. After all targets are built, `python tools/package_multiversion.py` verifies metadata, Java class versions, localization and core meshes, then collects the four JARs. For one target, pass e.g. `--targets 26.2`.

Never install `-sources.jar`. Normal JARs include the assets, notation catalog and architecture data. Building does not launch Minecraft or alter any installed instance.

## Asset editing

Texture and model files are checked in. `tools/generate_brand.py` is the current logo source and only requires Pillow if you regenerate the logo. Copy changes belong in `tools/copy_catalog.json`, followed by `python tools/copy_catalog.py`. Version-specific generated files are disposable build outputs.

The `*_sad` lamp textures and many numeric model filenames are still live variants, even though they are not separate registered blocks. Use the graph audit; do not delete assets based on a filename suffix alone.
