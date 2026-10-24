"""Reproduce the 1,000-pattern iBLP runtime pool from its complete offline source.

The complete, byte-exact source is kept under tools/reference, outside resources
and all release JARs. Only whole iBLP records are selected. BMS records, diagram
coordinates, marks, steps, expressions and provenance within each record remain
unchanged. No PRNG, platform hash iteration or floating-point distance is used.
"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from fractions import Fraction
import gzip
import hashlib
from io import BytesIO
import json
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "src/main/resources/data/guogaology/notation/catalog.json"
ARCHIVE = ROOT / "tools/reference/iblp-catalog-full.json.gz"
SOURCE_SHA256 = "27dc49d04ffdc388352ebe8be1b52f9536672b92f24cd79776f1a221d08d901e"
SOURCE_BYTES = 17_996_947
SOURCE_COUNT = 14_661
SOURCE_ORIGINAL = 3_859
SOURCE_DERIVED = 10_802
BMS_COUNT = 1_070
MAX_ROWS = 69
FEATURE_SCALE = 1_000_000
ALGORITHM = "marked-density-stratified-farthest-point-v1"

# Each of the four feature groups has the same total integer distance weight.
FEATURE_GROUPS = (
    ("shape", ("occupancy", "width_per_row", "horizontal_centroid",
               "vertical_centroid", "adjacent_row_change", "row_point_variation")),
    ("marks", ("marked_point_rate", "marked_row_rate", "mark_horizontal_centroid",
               "mark_vertical_centroid")),
    ("steps", ("mean_step", "step_change_rate", *(f"step_{i}_rate" for i in range(1, 12)))),
    ("step_positions", ("step_vertical_centroid", "last_first_step_delta",
                        "peak_step_position", "step_mirror_asymmetry")),
)
FEATURE_NAMES = tuple(name for _, names in FEATURE_GROUPS for name in names)
FEATURE_WEIGHTS = tuple(156 // len(names) for _, names in FEATURE_GROUPS for _ in names)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def sha256(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def encode(value) -> bytes:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def decode(raw: bytes):
    def unique(pairs):
        value = {}
        for key, item in pairs:
            require(key not in value, f"Duplicate JSON key: {key}")
            value[key] = item
        return value
    return json.loads(raw, object_pairs_hook=unique)


def shape_key(record) -> bytes:
    return encode([record["rows"], record["marks"], record["steps"]])


def quota(size: int) -> int:
    return {1: 1, 2: 3, 3: 8, 4: 14, 5: 14}.get(size, 15)


def validate_source(source) -> None:
    require(len(source["bms"]) == BMS_COUNT, "Complete source must contain all 1,070 BMS records")
    patterns = source["iblp"]
    require(len(patterns) == SOURCE_COUNT, "Archive is not the complete 14,661-pattern iBLP pool")
    original = sum(not record["operations"] for record in patterns)
    require(original == SOURCE_ORIGINAL and len(patterns) - original == SOURCE_DERIVED,
            "Complete source must preserve the original 3,859 + derived 10,802 pool")
    require(source["metadata"]["sourcePatterns"] == SOURCE_ORIGINAL
            and source["metadata"]["derivedPatterns"] == SOURCE_DERIVED,
            "Complete source provenance counts changed")
    require(len({shape_key(record) for record in patterns}) == SOURCE_COUNT,
            "Complete source contains duplicate diagram/mark/step patterns")
    sizes = Counter(len(record["rows"]) for record in patterns)
    require(set(sizes) == set(range(1, MAX_ROWS + 1)), "Complete source must cover every row size 1..69")
    require(all(sizes[size] >= quota(size) for size in sizes), "A source bucket is smaller than its quota")
    for index, record in enumerate(patterns):
        size = len(record["rows"])
        require(len(record["marks"]) == len(record["steps"]) == size,
                f"Inconsistent rows/marks/steps at source record {index}")
        require(all(row for row in record["rows"]), f"Empty occupied row at source record {index}")
        require(all(1 <= step <= 11 for step in record["steps"]), f"Unexpected step at source record {index}")


def read_source(check_only: bool):
    if not ARCHIVE.exists():
        require(not check_only, "Full offline archive is missing; initialize it from the untouched source first")
        raw = CATALOG.read_bytes()
        require(len(raw) == SOURCE_BYTES and sha256(raw) == SOURCE_SHA256,
                "Refusing to back up anything except the original byte-exact full catalog")
        validate_source(decode(raw))
        ARCHIVE.parent.mkdir(parents=True, exist_ok=True)
        compressed = BytesIO()
        # Empty filename avoids embedding a machine path; mtime=0 removes clock dependence.
        with gzip.GzipFile(filename="", mode="wb", fileobj=compressed, compresslevel=9, mtime=0) as out:
            out.write(raw)
        ARCHIVE.write_bytes(compressed.getvalue())
    compressed = ARCHIVE.read_bytes()
    require(compressed[:3] == b"\x1f\x8b\x08" and struct.unpack("<I", compressed[4:8])[0] == 0,
            "Offline source must be gzip with an explicit zero timestamp")
    require(compressed[3] == 0, "Offline source must not embed filenames or extra headers")
    raw = gzip.decompress(compressed)
    require(len(raw) == SOURCE_BYTES and sha256(raw) == SOURCE_SHA256,
            "Offline source differs from the original full catalog")
    source = decode(raw)
    validate_source(source)
    return source, raw, compressed


def raw_features(record) -> tuple[Fraction, ...]:
    rows, marks, steps = record["rows"], record["marks"], record["steps"]
    height = len(rows)
    width = max(max(row) for row in rows) + 1
    x_extent, y_extent = max(1, width - 1), max(1, height - 1)
    points = sum(map(len, rows))
    marked = sum(map(len, marks))
    step_sum = sum(steps)
    peak = max(steps)
    peak_positions = [i for i, step in enumerate(steps) if step == peak]
    histogram = Counter(steps)
    return (
        Fraction(points, height * width),
        Fraction(width, height),
        Fraction(sum(sum(row) for row in rows), points * x_extent),
        Fraction(sum(i * len(row) for i, row in enumerate(rows)), points * y_extent),
        Fraction(sum(len(set(a) ^ set(b)) for a, b in zip(rows, rows[1:])), 2 * points),
        Fraction(height * sum(len(row) ** 2 for row in rows) - points ** 2, points ** 2),
        Fraction(marked, points),
        Fraction(sum(bool(row) for row in marks), height),
        Fraction(sum(sum(row) for row in marks), max(1, marked) * x_extent),
        Fraction(sum(i * len(row) for i, row in enumerate(marks)), max(1, marked) * y_extent),
        Fraction(step_sum, height),
        Fraction(sum(a != b for a, b in zip(steps, steps[1:])), max(1, height - 1)),
        *(Fraction(histogram[step], height) for step in range(1, 12)),
        Fraction(sum(i * step for i, step in enumerate(steps)), step_sum * y_extent),
        Fraction(steps[-1] - steps[0], 11),
        Fraction(sum(peak_positions), len(peak_positions) * y_extent),
        Fraction(sum(abs(a - b) for a, b in zip(steps, reversed(steps))), 11 * height),
    )


def squared_distance(a, b) -> int:
    return sum(weight * (left - right) ** 2 for weight, left, right in zip(FEATURE_WEIGHTS, a, b))


def select_bucket(indices, patterns, raw, mandatory):
    size = len(patterns[indices[0]]["rows"])
    limit = quota(size)
    minima = [min(raw[index][feature] for index in indices) for feature in range(len(FEATURE_NAMES))]
    maxima = [max(raw[index][feature] for index in indices) for feature in range(len(FEATURE_NAMES))]
    normalized = {
        index: tuple(int(FEATURE_SCALE * (value - low) / (high - low)) if high != low else 0
                     for value, low, high in zip(raw[index], minima, maxima))
        for index in indices
    }
    density_values = sorted({raw[index][0] for index in indices})
    density_bands = {value: min(3, 4 * rank // len(density_values)) for rank, value in enumerate(density_values)}
    strata = {index: (int(any(patterns[index]["marks"])), density_bands[raw[index][0]]) for index in indices}
    by_stratum = defaultdict(list)
    for index in indices:
        by_stratum[strata[index]].append(index)
    selected = []
    nearest = {index: None for index in indices}

    def add(index):
        require(index not in selected and len(selected) < limit, "Invalid selection state")
        selected.append(index)
        for other in indices:
            distance = squared_distance(normalized[index], normalized[other])
            if nearest[other] is None or distance < nearest[other]:
                nearest[other] = distance

    def farthest(candidates):
        # Earliest mother-pool record wins exact ties; no PRNG or hash order.
        return max(candidates, key=lambda index: (nearest[index], -index))

    for index in sorted(mandatory):
        add(index)
    if size <= 3:
        for index in indices:
            if index not in selected:
                add(index)
    else:
        for stratum, candidates in sorted(by_stratum.items()):
            if not any(strata[index] == stratum for index in selected):
                add(farthest(candidates))
        while len(selected) < limit:
            add(farthest(index for index in indices if index not in selected))
    selected.sort()
    require(len(selected) == limit, f"Wrong quota for {size} rows")
    require({strata[index] for index in selected} == set(by_stratum), f"Uncovered shape stratum at {size} rows")
    return selected, {
        "rows": size,
        "source": len(indices),
        "selected": len(selected),
        "sourceOriginal": sum(not patterns[index]["operations"] for index in indices),
        "selectedOriginal": sum(not patterns[index]["operations"] for index in selected),
        "mandatorySourceIndices": sorted(mandatory),
        "selectedSourceIndices": selected,
        "strata": [{"marked": bool(marked), "occupancyRankQuartile": band,
                    "source": len(candidates), "selected": sum(index in selected for index in candidates)}
                   for (marked, band), candidates in sorted(by_stratum.items())],
        "featureRanges": {name: {"sourceMin": float(minima[feature]), "sourceMax": float(maxima[feature]),
                                  "selectedMin": float(min(raw[index][feature] for index in selected)),
                                  "selectedMax": float(max(raw[index][feature] for index in selected))}
                          for feature, name in enumerate(FEATURE_NAMES)},
        "maximumNearestSquaredDistance": max(nearest.values()),
    }


def build_selection(source):
    patterns = source["iblp"]
    by_size = defaultdict(list)
    for index, record in enumerate(patterns):
        by_size[len(record["rows"])].append(index)
    maximum_step = max(max(record["steps"]) for record in patterns)
    max_step_anchor = next(index for index, record in enumerate(patterns) if maximum_step in record["steps"])
    mandatory = {indices[0] for indices in by_size.values()} | {max_step_anchor}
    raw = [raw_features(record) for record in patterns]
    selected, buckets = [], []
    for size in sorted(by_size):
        indices = by_size[size]
        chosen, bucket = select_bucket(indices, patterns, raw, mandatory.intersection(indices))
        selected.extend(chosen)
        buckets.append(bucket)
    selected.sort()
    records = [patterns[index] for index in selected]
    original = sum(not record["operations"] for record in records)
    metadata = dict(source["metadata"])
    # These formerly described the entire generated pool. They now describe the
    # selected runtime catalog; the untouched original metadata is nested below.
    metadata.update(sourcePatterns=original, derivedPatterns=len(records) - original)
    metadata["selection"] = {
        "algorithm": ALGORITHM,
        "selectedPatterns": len(records),
        "selectedOriginalPatterns": original,
        "selectedDerivedPatterns": len(records) - original,
        "smallPatterns": 12,
        "wildPatterns": 988,
        "rowQuotas": {str(size): quota(size) for size in range(1, MAX_ROWS + 1)},
        "mandatoryRule": "first mother-pool record of every row size, plus earliest global maximum-step record",
        "maximumStep": maximum_step,
        "maximumStepSourceIndex": max_step_anchor,
        "featureNormalization": "exact rational features; per-bucket min/max to integer 0..1000000; equal feature-group distance weights",
        "featureGroups": {group: list(names) for group, names in FEATURE_GROUPS},
        "sourcePool": {
            "patterns": SOURCE_COUNT,
            "originalPatterns": SOURCE_ORIGINAL,
            "derivedPatterns": SOURCE_DERIVED,
            "uncompressedBytes": SOURCE_BYTES,
            "sha256": SOURCE_SHA256,
            "offlineArchive": "tools/reference/iblp-catalog-full.json.gz",
            "metadata": source["metadata"],
        },
    }
    return {"metadata": metadata, "bms": source["bms"], "iblp": records}, {
        "algorithm": ALGORITHM,
        "featureScale": FEATURE_SCALE,
        "featureGroups": {group: list(names) for group, names in FEATURE_GROUPS},
        "featureWeights": dict(zip(FEATURE_NAMES, FEATURE_WEIGHTS)),
        "selectedSourceIndices": selected,
        "mandatorySourceIndices": sorted(mandatory),
        "maximumStepSourceIndex": max_step_anchor,
        "buckets": buckets,
    }


def validate_selection(current, expected, source, selection_report) -> None:
    records = current["iblp"]
    require(len(records) == 1_000, "Runtime catalog must contain exactly 1,000 iBLP records")
    require(len({shape_key(record) for record in records}) == 1_000, "Runtime patterns must be unique")
    counts = Counter(len(record["rows"]) for record in records)
    require(counts == {size: quota(size) for size in range(1, MAX_ROWS + 1)}, "Runtime row coverage/quota mismatch")
    require(sum(counts[size] for size in range(1, 4)) == 12 and sum(counts[size] for size in range(4, 70)) == 988,
            "Runtime pool must retain 12 small and 988 wild patterns")
    mother_records = {encode(record): index for index, record in enumerate(source["iblp"])}
    indices = []
    for record in records:
        key = encode(record)
        require(key in mother_records, "Selected record content was changed or is absent from the mother pool")
        indices.append(mother_records[key])
    require(indices == sorted(set(indices)), "Selected records must retain unique mother-pool order")
    require(set(selection_report["mandatorySourceIndices"]).issubset(indices), "A required original anchor was omitted")
    require(current["bms"] == source["bms"] and len(current["bms"]) == BMS_COUNT,
            "BMS content/order/count must remain completely unchanged")
    require(current == expected, "Catalog differs from deterministic whole-record selection or provenance metadata")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Recompute from the offline full archive; do not write runtime resources")
    parser.add_argument("--report", type=Path, help="Optional detailed audit JSON outside runtime resources")
    args = parser.parse_args()
    source, full_raw, archive_raw = read_source(args.check)
    expected, report = build_selection(source)
    selected_raw = encode(expected)
    if args.check:
        current_raw = CATALOG.read_bytes()
        validate_selection(decode(current_raw), expected, source, report)
        require(current_raw == selected_raw, "Runtime catalog serialization is not reproducible")
    else:
        validate_selection(expected, expected, source, report)
        CATALOG.write_bytes(selected_raw)
    report.update(
        passed=True,
        checkOnly=args.check,
        sourcePool={"patterns": SOURCE_COUNT, "original": SOURCE_ORIGINAL, "derived": SOURCE_DERIVED,
                    "bytes": len(full_raw), "sha256": sha256(full_raw)},
        selectedPool={"patterns": len(expected["iblp"]), "original": expected["metadata"]["sourcePatterns"],
                      "derived": expected["metadata"]["derivedPatterns"], "small": 12, "wild": 988,
                      "bytes": len(selected_raw), "sha256": sha256(selected_raw)},
        offlineArchive={"path": str(ARCHIVE.relative_to(ROOT)), "bytes": len(archive_raw),
                        "sha256": sha256(archive_raw), "mtime": 0, "packaged": False},
        bms={"patterns": BMS_COUNT, "unchanged": True, "sha256": sha256(encode(source["bms"]))},
        bytesSaved=len(full_raw) - len(selected_raw),
        reductionPercent=100 * (len(full_raw) - len(selected_raw)) / len(full_raw),
        validation=["exact source SHA-256", "gzip timestamp zero", "all 1..69 sizes and quotas",
                    "1,000 unique patterns", "all small patterns", "every size-first anchor", "earliest maximum-step anchor",
                    "all occupied shape strata covered", "all record fields unchanged", "mother-pool order",
                    "1,070 BMS records unchanged", "deterministic source and selected provenance counts"],
    )
    if args.report:
        report_path = args.report if args.report.is_absolute() else ROOT / args.report
        report_path.parent.mkdir(parents=True, exist_ok=True)
        report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: report[key] for key in ("passed", "checkOnly", "sourcePool", "selectedPool", "offlineArchive", "bms", "bytesSaved", "reductionPercent")}, indent=2))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (KeyError, TypeError, ValueError, OSError, json.JSONDecodeError) as error:
        raise SystemExit(f"iBLP catalog selection failed: {error}") from error
