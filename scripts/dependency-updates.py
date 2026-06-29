#!/usr/bin/env python3
"""
Dependency update checker for RDF4J multi-module Maven project.

Parses all pom.xml files, resolves dependency versions managed in the root POM,
queries Maven Central for the latest available versions, and prints a report
grouped by update type: major, minor/patch, up-to-date, or unknown.

Usage via Maven (project root only, no sub-module recursion):
    mvn -N -P dependency-updates validate

Direct usage:
    python3 scripts/dependency-updates.py [project-root-dir]
"""

import io
import os
import re
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path
from typing import Optional

# Ensure UTF-8 output on all platforms (avoids UnicodeEncodeError for box-drawing
# characters when Maven/Python stdout is not UTF-8, e.g. on Windows CI locales).
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
MAVEN_CENTRAL_REPO = "https://repo1.maven.org/maven2"
INTERNAL_GROUP = "org.eclipse.rdf4j"
RATE_LIMIT_DELAY = 0.25   # seconds between Maven Central requests
MAX_MODULES_SHOWN = 6     # truncate module list beyond this count

# ---------------------------------------------------------------------------
# Known successor artifact mappings
#
# These cover cases where a new major generation of a library was published
# under *different Maven coordinates* rather than just a higher version number.
#
# EXPLICIT_SUCCESSORS: exact {current groupId:artifactId -> successor groupId:artifactId}
# GROUP_PREFIX_SUCCESSORS: list of (old_group_prefix, new_group_prefix) — the
#   artifactId is kept unchanged; only the group prefix is swapped.
#   E.g. com.fasterxml.jackson.core:jackson-core → tools.jackson.core:jackson-core
# ---------------------------------------------------------------------------

EXPLICIT_SUCCESSORS: dict = {
    # Apache HttpComponents 4 → 5
    "org.apache.httpcomponents:httpclient":       "org.apache.httpcomponents.client5:httpclient5",
    "org.apache.httpcomponents:httpclient-cache": "org.apache.httpcomponents.client5:httpclient5",
    "org.apache.httpcomponents:httpmime":         "org.apache.httpcomponents.client5:httpclient5-fluent",
    "org.apache.httpcomponents:fluent-hc":        "org.apache.httpcomponents.client5:httpclient5-fluent",
    "org.apache.httpcomponents:httpcore":         "org.apache.httpcomponents.core5:httpcore5",
    "org.apache.httpcomponents:httpcore-nio":     "org.apache.httpcomponents.core5:httpcore5-h2",
    # Elasticsearch: REST high-level client (deprecated) → new Java API client
    "org.elasticsearch.client:elasticsearch-rest-high-level-client": "co.elastic.clients:elasticsearch-java",
    # javax.servlet → Jakarta EE
    "javax.servlet:javax.servlet-api":            "jakarta.servlet:jakarta.servlet-api",
}

GROUP_PREFIX_SUCCESSORS: list = [
    # Jackson 2.x (com.fasterxml.jackson.*) → Jackson 3.x (tools.jackson.*)
    # The group prefix is replaced; the artifactId is preserved.
    ("com.fasterxml.jackson", "tools.jackson"),
]


# ---------------------------------------------------------------------------
# POM parsing helpers
# ---------------------------------------------------------------------------

def find_text(el: ET.Element, path: str) -> Optional[str]:
    node = el.find(path, NS)
    return node.text.strip() if node is not None and node.text else None


def resolve(value: Optional[str], props: dict) -> Optional[str]:
    """Substitute ${...} property references up to 10 levels deep."""
    if value is None:
        return None
    for _ in range(10):
        m = re.search(r'\$\{([^}]+)\}', value)
        if not m:
            break
        key = m.group(1)
        repl = props.get(key, "")
        value = value[: m.start()] + repl + value[m.end() :]
    return value


def parse_properties(pom_el: ET.Element) -> dict:
    props = {}
    props_el = pom_el.find("m:properties", NS)
    if props_el is not None:
        for child in props_el:
            tag = child.tag.split("}")[-1] if "}" in child.tag else child.tag
            if child.text:
                props[tag] = child.text.strip()
    return props


def parse_managed_deps(pom_el: ET.Element, props: dict) -> tuple:
    """
    Return (managed, bom_imports) where:
      managed     — {groupId:artifactId -> version} for explicitly versioned entries
      bom_imports — [(group, artifact, version), ...] for type=pom scope=import entries

    Internal rdf4j artifacts are skipped in both.
    """
    managed: dict = {}
    bom_imports: list = []
    dm = pom_el.find("m:dependencyManagement/m:dependencies", NS)
    if dm is None:
        return managed, bom_imports
    for dep in dm.findall("m:dependency", NS):
        group = resolve(find_text(dep, "m:groupId"), props)
        artifact = resolve(find_text(dep, "m:artifactId"), props)
        version = resolve(find_text(dep, "m:version"), props)
        scope = find_text(dep, "m:scope")
        dep_type = find_text(dep, "m:type")
        if not group or not artifact or group == INTERNAL_GROUP:
            continue
        if scope == "import" and dep_type == "pom":
            if version:
                bom_imports.append((group, artifact, version))
        elif version:
            managed[f"{group}:{artifact}"] = version
    return managed, bom_imports


def fetch_bom_managed(group: str, artifact: str, version: str) -> dict:
    """
    Download a BOM POM from Maven Central and return the versions it declares
    as {groupId:artifactId -> version}.  The BOM's own <properties> are resolved
    so that version strings like ${jackson.version} are substituted.
    Only one level of BOM resolution is performed (transitive BOM-of-BOMs are
    not followed).
    """
    group_path = group.replace(".", "/")
    url = f"{MAVEN_CENTRAL_REPO}/{group_path}/{artifact}/{version}/{artifact}-{version}.pom"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "rdf4j-dep-checker/1.0"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            root = ET.fromstring(resp.read())
    except Exception as e:
        print(f"  [warn] Could not fetch BOM {group}:{artifact}:{version} — {e}", file=sys.stderr)
        return {}

    bom_props = parse_properties(root)
    # Seed the BOM version itself so ${project.version} refs resolve correctly.
    bom_props.setdefault("project.version", version)

    dm = root.find("m:dependencyManagement/m:dependencies", NS)
    if dm is None:
        return {}
    result: dict = {}
    for dep in dm.findall("m:dependency", NS):
        g = resolve(find_text(dep, "m:groupId"), bom_props)
        a = resolve(find_text(dep, "m:artifactId"), bom_props)
        v = resolve(find_text(dep, "m:version"), bom_props)
        scope = find_text(dep, "m:scope")
        dep_type = find_text(dep, "m:type")
        if not g or not a or g == INTERNAL_GROUP:
            continue
        if scope == "import" and dep_type == "pom":
            continue  # skip nested BOM imports
        if v and not v.startswith("$"):
            result[f"{g}:{a}"] = v
    return result


def collect_module_deps(pom_path: Path, props: dict, managed: dict) -> list:
    """
    Return [(dep_key, resolved_version_or_None), ...] for all external
    dependencies declared in <dependencies> of the given module POM.
    """
    try:
        root = ET.parse(pom_path).getroot()
    except Exception:
        return []
    deps_el = root.find("m:dependencies", NS)
    if deps_el is None:
        return []
    result = []
    for dep in deps_el.findall("m:dependency", NS):
        group = resolve(find_text(dep, "m:groupId"), props)
        artifact = resolve(find_text(dep, "m:artifactId"), props)
        version = resolve(find_text(dep, "m:version"), props)
        if not group or not artifact or group == INTERNAL_GROUP:
            continue
        key = f"{group}:{artifact}"
        result.append((key, version or managed.get(key)))
    return result


def module_name(pom_path: Path) -> str:
    try:
        root = ET.parse(pom_path).getroot()
        name = find_text(root, "m:artifactId")
        return name or pom_path.parent.name
    except Exception:
        return pom_path.parent.name


def find_all_poms(root_dir: Path) -> list:
    skip = {".git", "target", ".mvn", ".codex"}
    poms = []
    for dirpath, dirnames, filenames in os.walk(root_dir):
        dirnames[:] = sorted(d for d in dirnames if d not in skip)
        if "pom.xml" in filenames:
            poms.append(Path(dirpath) / "pom.xml")
    return poms


# ---------------------------------------------------------------------------
# Version comparison
# ---------------------------------------------------------------------------

def version_tuple(v: str) -> tuple:
    """Extract (major, minor, patch) numeric parts from a version string."""
    parts = re.split(r"[\.\-]", v)
    nums = []
    for p in parts:
        if re.fullmatch(r"\d+", p):
            nums.append(int(p))
        else:
            break
    while len(nums) < 3:
        nums.append(0)
    return tuple(nums[:3])


def classify(current: str, latest: str) -> str:
    cur = version_tuple(current)
    lat = version_tuple(latest)
    if lat <= cur:
        return "up_to_date"
    if lat[0] > cur[0]:
        return "major"
    return "minor_patch"


# ---------------------------------------------------------------------------
# Successor coordinate resolution
# ---------------------------------------------------------------------------

def find_successor(group: str, artifact: str) -> Optional[str]:
    """
    Return the successor groupId:artifactId for a dependency that has been
    re-released under different coordinates, or None if no mapping is known.
    """
    key = f"{group}:{artifact}"
    if key in EXPLICIT_SUCCESSORS:
        return EXPLICIT_SUCCESSORS[key]
    for old_prefix, new_prefix in GROUP_PREFIX_SUCCESSORS:
        if group == old_prefix or group.startswith(old_prefix + "."):
            new_group = new_prefix + group[len(old_prefix):]
            return f"{new_group}:{artifact}"
    return None


# ---------------------------------------------------------------------------
# Maven Central query  (uses maven-metadata.xml — authoritative release info)
# ---------------------------------------------------------------------------

def fetch_latest(group: str, artifact: str) -> Optional[str]:
    """
    Fetch the latest *release* version from repo1.maven.org/maven2 via
    maven-metadata.xml.  Falls back to <latest> if <release> is absent.
    """
    group_path = group.replace(".", "/")
    url = f"{MAVEN_CENTRAL_REPO}/{group_path}/{artifact}/maven-metadata.xml"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "rdf4j-dep-checker/1.0"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            raw = resp.read()
        meta = ET.fromstring(raw)
        versioning = meta.find("versioning")
        if versioning is not None:
            for tag in ("release", "latest"):
                el = versioning.find(tag)
                if el is not None and el.text:
                    return el.text.strip()
    except Exception as e:
        print(f"  [warn] {group}:{artifact} — {e}", file=sys.stderr)
    return None


# ---------------------------------------------------------------------------
# Table formatting
# ---------------------------------------------------------------------------

def fmt_modules(modules: list) -> str:
    if not modules:
        return "(managed only)"
    if len(modules) <= MAX_MODULES_SHOWN:
        return ", ".join(modules)
    shown = ", ".join(modules[:MAX_MODULES_SHOWN])
    return f"{shown}, +{len(modules) - MAX_MODULES_SHOWN} more"


def print_table(rows: list, headers: list) -> None:
    if not rows:
        print("  (none)")
        return
    widths = [len(h) for h in headers]
    for row in rows:
        for i, cell in enumerate(row):
            widths[i] = max(widths[i], len(str(cell)))
    sep = "  ".join("-" * w for w in widths)
    hdr = "  ".join(h.ljust(widths[i]) for i, h in enumerate(headers))
    print(hdr)
    print(sep)
    for row in rows:
        print("  ".join(str(cell).ljust(widths[i]) for i, cell in enumerate(row)))


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    root_dir = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path.cwd()
    root_pom = root_dir / "pom.xml"

    if not root_pom.exists():
        sys.exit(f"ERROR: pom.xml not found in {root_dir}")

    print(f"Project root : {root_dir}")
    print("Parsing root POM ...")
    root_el = ET.parse(root_pom).getroot()
    props = parse_properties(root_el)
    props.setdefault("project.version", find_text(root_el, "m:version") or "")

    managed, bom_imports = parse_managed_deps(root_el, props)
    # root_managed tracks only the explicitly pinned entries from the root POM;
    # these are included in the report even if no module directly declares them
    # (e.g. managed-but-unused overrides kept for security patching).
    root_managed = set(managed.keys())

    # Resolve imported BOMs and merge their dependencyManagement into managed.
    # BOM-derived entries are used for version lookup when scanning module POMs
    # but are only included in the report if a module actually uses them.
    # Entries already present in managed (explicit overrides) are not replaced.
    if bom_imports:
        print(f"Resolving {len(bom_imports)} imported BOM(s) ...")
        for bom_group, bom_artifact, bom_version in bom_imports:
            bom_managed = fetch_bom_managed(bom_group, bom_artifact, bom_version)
            for key, ver in bom_managed.items():
                managed.setdefault(key, ver)

    print(f"Managed deps : {len(managed)} (including BOM-resolved)")

    # ── collect which modules use each dependency ──────────────────────────
    all_poms = find_all_poms(root_dir)
    dep_to_modules: dict = defaultdict(set)
    dep_versions: dict = {}

    print(f"Module POMs  : {len(all_poms) - 1} (excluding root)")
    for pom_path in all_poms:
        if pom_path == root_pom:
            continue
        name = module_name(pom_path)
        for key, ver in collect_module_deps(pom_path, props, managed):
            dep_to_modules[key].add(name)
            # ver may be None when the version comes from a module-level BOM
            # import not covered by the root POM.  Record it anyway so the dep
            # appears in the report with current version shown as "?".
            if key not in dep_versions:
                dep_versions[key] = ver

    # Include root-pom explicit entries even when no module declares them directly
    # (BOM-only entries are excluded here to avoid flooding the report with
    # hundreds of unused artifacts from imported BOMs).
    for key in root_managed:
        ver = managed.get(key)
        if key not in dep_versions and ver:
            dep_versions[key] = ver

    # ── query Maven Central ────────────────────────────────────────────────
    print(f"\nQuerying Maven Central for {len(dep_versions)} dependencies ...\n")

    GROUPS: dict = {
        "major":       ("MAJOR VERSION UPDATES",                        []),
        "minor_patch": ("MINOR / PATCH UPDATES",                        []),
        "up_to_date":  ("UP TO DATE",                                   []),
        "unknown":     ("UNKNOWN (fetch failed)",                        []),
        "unresolved":  ("CURRENT VERSION UNRESOLVED (module-level BOM)", []),
    }
    headers = ["Dependency", "Current", "Latest", "Modules"]

    for key in sorted(dep_versions):
        group, artifact = key.split(":", 1)
        current = dep_versions[key]
        if current is None:
            # Version not resolvable from root POM or root-imported BOMs;
            # still fetch latest so the dep is informative rather than blank.
            latest = fetch_latest(group, artifact)
            time.sleep(RATE_LIMIT_DELAY)
            status = "unresolved"
        else:
            latest = fetch_latest(group, artifact)
            time.sleep(RATE_LIMIT_DELAY)
            status = "unknown" if latest is None else classify(current, latest)

        modules_str = fmt_modules(sorted(dep_to_modules.get(key, [])))
        row = (key, current or "?", latest or "?", modules_str)
        GROUPS[status][1].append(row)

    # ── query successors under different coordinates ───────────────────────
    # Collect unique successor targets to avoid querying the same artifact twice.
    # successor_ga -> [(current_key, current_version, modules_str), ...]
    successor_candidates: dict = defaultdict(list)
    for key in sorted(dep_versions):
        group, artifact = key.split(":", 1)
        succ = find_successor(group, artifact)
        if succ is None:
            continue
        modules_str = fmt_modules(sorted(dep_to_modules.get(key, [])))
        successor_candidates[succ].append((key, dep_versions[key], modules_str))

    successor_rows = []
    if successor_candidates:
        print(f"Querying Maven Central for {len(successor_candidates)} successor artifacts ...\n")
        for succ_ga in sorted(successor_candidates):
            succ_group, succ_artifact = succ_ga.split(":", 1)
            succ_latest = fetch_latest(succ_group, succ_artifact)
            time.sleep(RATE_LIMIT_DELAY)
            # A None result means the successor coordinates don't exist on Central —
            # the artifact may have been merged into another artifact in the new generation.
            latest_str = succ_latest if succ_latest else "(not found — possibly merged)"
            for current_key, current_ver, modules_str in successor_candidates[succ_ga]:
                successor_rows.append(
                    (current_key, current_ver, succ_ga, latest_str, modules_str)
                )

    # ── print report ───────────────────────────────────────────────────────
    total = sum(len(g[1]) for g in GROUPS.values())
    width = 80
    print("=" * width)
    print(f"  DEPENDENCY UPDATE REPORT   ({total} external dependencies)")
    print("=" * width)
    print()

    for status_key in ("major", "minor_patch", "up_to_date", "unknown", "unresolved"):
        label, rows = GROUPS[status_key]
        count = len(rows)
        print(f"{'─' * 2}  {label}  ({count})  {'─' * max(0, width - len(label) - 12)}")
        print()
        print_table(rows, headers)
        print()

    # ── successor table ────────────────────────────────────────────────────
    print("=" * width)
    print(f"  AVAILABLE UNDER DIFFERENT COORDINATES   ({len(successor_rows)} entries)")
    print("=" * width)
    print()
    succ_headers = ["Current Dependency", "Current Ver", "Successor Dependency", "Latest", "Modules"]
    print_table(successor_rows, succ_headers)
    print()


if __name__ == "__main__":
    main()
