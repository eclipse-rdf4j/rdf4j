# Release-specific third-party notices

`generate-third-party-notices.py` collects **local evidence** for the installed
RDF4J version. It does not download arbitrary license URLs, submit IP reviews,
approve licenses, or publish anything. Python 3.10+ and its standard library are
sufficient. License collection and Eclipse Dash approval are separate checks.

## Build and verify

From the repository root, with the project's JDK/Maven prerequisites and Python
3.10+ available, the normal developer build generates version-specific notices:

```sh
mvn -Dmaven.repo.local=.m2_repo -Pquick clean install
```

Developer builds explicitly allow incomplete evidence, but the resulting report
visibly lists everything still requiring review. For strict notice verification
before publication or release, run:

```sh
mvn -Dmaven.repo.local=.m2_repo -Pquick,strict-notices clean install
```

The current missing evidence blocks strict verification until it is resolved.
`quick` is a build convenience, not a substitute for the project's normal release
tests: use the corresponding non-quick release build with `strict-notices` when
performing the complete release checks. Neither a successful build nor strict
notice verification is a legal compliance certification or authorization to
publish.

## Inputs and integration

Maven installs/builds the frontend; the Vite collector emits actual bundled
module paths from both main and worker bundles into
`tools/workbench/target/licensing/npm-modules.json`. At `prepare-package`,
`maven-dependency-plugin:build-classpath` resolves runtime JARs including
transitive dependencies, excludes the project's own `org.eclipse.rdf4j` group,
and writes `tools/workbench/target/licensing/java-classpath.txt`. The generator
then consumes these exact inputs and writes the files bundled in the WAR under
`tools/workbench/target/frontend/legal`.

To rerun collection directly after those build inputs exist:

```sh
python3 scripts/generate-third-party-notices.py \
  --repo "$PWD" --version 6.2.0-SNAPSHOT \
  --npm-project "$PWD/tools/workbench" \
  --npm-modules "$PWD/tools/workbench/target/licensing/npm-modules.json" \
  --java-classpath "$PWD/tools/workbench/target/licensing/java-classpath.txt" \
  --maven-repo "$PWD/.m2_repo" \
  --extra-content-dir "$PWD/tools/workbench/src/main/webapp" \
  --output-dir "$PWD/tools/workbench/target/frontend/legal"
```

Use the actual installed project version rather than the example version above.
The standalone command is strict by default; add `--allow-incomplete` only for
an explicitly incomplete developer report. As an alternative to
`--java-classpath`, `--java-lib-dir DIR` accepts an already-built WAR's extracted
`WEB-INF/lib` or a clean directory containing the resolved runtime dependency
closure. Do not mix stale artifacts or build/test dependencies into that input.

All command-line paths are relative to the current working directory unless
absolute. `--maven-repo` defaults to `--repo/.m2_repo`. The npm module list is a
JSON array of actual bundled module paths, relative to the npm project (or
absolute). It must include worker chunks as well as main bundles. Only selected
packages are included, regardless of `dev` labels: a dev dependency accidentally
bundled is shipped content and must be included. Unused runtime dependencies and
pure development tools are not treated as shipped npm code. Package versions
are checked against the lockfile; installed package contents provide the texts.

The script includes every supplied JAR, including transitive dependencies.
Coordinates come from embedded Maven metadata, or a byte-identical artifact in
the local Maven repository. POM license declarations can inherit from locally
available parent POMs. JAR license and NOTICE resources are retained independently.
The tool does not resolve/download missing parent POMs. Shaded JARs with multiple
Maven identities are flagged for review rather than silently assumed complete.

Vendored assets come from the hash-verified
`tools/workbench/frontend-vendor-dependencies.json`. Uncatalogued Workbench static
JavaScript is flagged. Repeat `--extra-content-dir PATH` to inspect additional
external JAR/JavaScript directories. Unknown loose JavaScript is **not** assigned
a package/version or the project's license. The scan recognizes project-owned
Eclipse RDF4J/Aduna headers; it is not a general copyright-classification engine.

## Outputs and failures

- `third-party-notices.html`: offline, readable component list and full collected
  license/notice texts, with internal links and HTML-escaped upstream content.
- `third-party-inventory.json`: versioned component/source/license mapping,
  per-component attribution, license/notice document references, SHA-256 keyed
  exact-text deduplication, and unresolved evidence.

Deduplication uses **exact source bytes**, never just a license identifier. Each
component retains its own attribution, source, declared expression, and document
links. Different copyright-bearing license texts remain separate. SPDX `OR`,
`AND`, and `WITH` declarations are preserved rather than reduced to one license.
Compound declarations and embedded native/WASM engines require explicit review;
a package's MIT label does not establish its embedded engine's terms.

Exit 0 means the collector found no unresolved evidence under its stated scope,
**not a legal compliance certification**. Exit 1 writes the report but identifies
missing or incomplete evidence. Exit 2 is invalid input or a processing error.
`--allow-incomplete` changes exit 1 to success for an explicitly requested local
developer build; it never removes warnings from the delivered report. Release
builds must not use that option. Text presence is evidence, not automated proof
that an upstream text is complete or applies to every embedded component.

## Manual evidence template

The default `scripts/third-party-notices.manual.json` is prefilled with two
verifiable YASQE-embedded components that lack standalone npm metadata: json2's
dated public-domain notice and Saurabh Odhyan's dated MIT trie implementation.
Do not invent npm coordinates for embedded snippets.

Use `--manual FILE` to select a different inventory. The schema is:

```json
{
  "schemaVersion": 1,
  "components": [{
    "id": "embedded:example@1.0",
    "name": "Exact component name",
    "version": "1.0",
    "source": "https://upstream.example/exact-release",
    "reason": "Why automatic discovery is insufficient",
    "declaredLicenses": ["MIT"],
    "attribution": ["Copyright holder and years, verbatim"],
    "documents": [{
      "path": "repo-relative/path/to/LICENSE",
      "sha256": "SHA-256 of the complete source file",
      "kind": "license"
    }],
    "unresolved": ["Describe anything still unverified; omit only when resolved"]
  }]
}
```

An entry with the exact same component ID adds evidence and attribution to an
automatically discovered component. It does not suppress structural discovery
issues or replace them with blanket approvals. `documents` can use inclusive
one-based `startLine`/`endLine` for an intact embedded notice; the hash must still
cover the **entire** original source file. Use `kind: "notice"` for NOTICE files.
Keep complete copyright, license, exception and disclaimer text. All paths must
remain inside the repository, and every source hash must match. A new upstream
version requires refreshed evidence, not an unchecked version edit.

Components for which automated discovery still reports structural problems must
be investigated. The template is not an approval-bypass mechanism. Review and
any submission/publication remain manual.

## Focused offline tests

```sh
python3 -m unittest scripts/test_third_party_notices.py
node --test tools/workbench/scripts/license-bundle-inventory.test.mjs
```

Fixtures cover transitive npm/JAR inventory, build-only filtering, exact-text
deduplication and attribution, deterministic output, missing evidence, compound
license retention, HTML escaping, local Maven provenance recovery, hash-anchored
manual source slices, and extra loose JavaScript discovery.

### Maven build modes and project-owned exceptions

The normal Workbench build explicitly uses `--allow-incomplete` so developers
receive a usable, visibly flagged report even when local evidence is missing.
Use Maven profile `-Pstrict-notices` for release verification; it passes `--strict`
and fails unresolved evidence. The bare script is strict by default. `--strict`
and `--allow-incomplete` cannot be combined. Neither mode approves IP or publishes
anything. The HTML links to sibling `NOTICE.md` and `edl-v1.0.txt`; the packaging
integration must copy those canonical project documents alongside it.

The manual inventory can also contain an `ownedFiles` array, solely for explicit
project-owned files that lack a recognizable header. Each entry requires `path`,
complete-file `sha256`, exact `source`, `reason`, `licensePath`, and
`licenseSha256`. Both content and project-license hashes must match. The seeded
entry covers only the tracked Workbench theme implementation, not arbitrary
JavaScript. These provenance records appear separately as `projectFiles`, not as
third-party components.

### Exact Maven classpath input

Prefer `--java-classpath FILE` instead of `--java-lib-dir DIR` in incremental
builds. The file is Maven `dependency:build-classpath` output with runtime scope,
using the host's path separator. Every entry must exist and be a JAR; removed
versions left in an old staging directory are therefore never accidentally
included. These flags are mutually exclusive. Extra-content directories remain
additive. Verified `org.eclipse.rdf4j` coordinates are explicitly marked
`role: "project"` in the inventory and labeled as project components in HTML;
filenames alone cannot grant that classification.

Manual entries may use `includedWhenComponent` with an exact existing component
ID, so an embedded-engine supplement applies only to the verified containing
package version. The template identifies Graphviz 16.1.0, Expat 2.8.5 and
Emscripten 6.0.10 runtime support embedded in Viz.js 3.31.0. The packaged
provenance subject matches the actual backend hash; the exact Graphviz/Expat
source archives also match its resolved-material hashes. Full primary texts,
authors and source notice blocks are retained, rather than inferred from
Viz.js's MIT declaration. This publisher-supplied provenance is not an
independently authenticated attestation or a reproduced build. Exact linked
runtime-object coverage and release distribution obligations remain explicit.
The Emscripten compiler/image is build tooling, not shipped content. Additional
runtime-library documents are reference evidence where exact linkage is unknown.

### Generated unresolved-component template

Every run also writes `third-party-review-template.json`, ready to fill for the
specific unresolved components found in that build. Entries retain exact
identity/version, source, declared terms, Maven license URLs, JAR SHA-256 or npm
lock integrity and selected-module hashes, existing document references, and
specific missing evidence. No license text is fabricated from an SPDX label.
Add reviewed documents with exact hashes, preserve attributions and any
license choice/exception, and merge the reviewed entries into the maintained
manual inventory. This generated file does not automatically suppress findings
or submit/publish anything.

### Explicit human coverage reviews

Some structural findings need documented review even after texts are collected:
license combinations/exceptions (`license-choice`), embedded
native components (`embedded-components`), and shaded component attribution
(`shaded-attribution`). The generated review template has **pending** `reviews`
records for those specific conditions. The collector never completes them.

A human can finish a record with `status: "completed"`, their `reviewer` name,
ISO `date`, and a concrete `reason` (including the selected licensing alternative
where applicable). The record must retain the exact `component`, `version`,
`type`, and `evidenceSha256`, and reference collected documents through a nonempty
`documentSha256` array including a license document. The evidence hash covers
component/version, sources, exact selected source/JAR hashes, npm lock integrity,
declared license expressions and collected document hashes/provenance. Add any
missing documents first, regenerate the report/template, then review that final
evidence. Editing source, artifacts or documents invalidates the old review.

Add completed records to the maintained manual inventory's top-level `reviews`
array. There are no wildcard components or review types. Missing reviewer/date/
reason, stale evidence, unknown types, or missing document references fail
validation. A matching completed record resolves **only its specific structural
coverage finding**; it cannot suppress missing license declarations/texts,
unresolved inventory items, hash mismatches, or other findings. Completed records
remain visible in HTML and JSON as human coverage decisions, not automated IP
approval or a legal compliance certification.

`license-choice` is a broad coverage-check name, not a statement that every
compound declaration requires a new license election. `AND` can describe
different components with cumulative obligations; `OR` preserves an upstream
offer of alternatives; a secondary license can be conditional. First verify
the exact upstream statement and retain all applicable texts and notices.
Do not convert a conditional secondary license into an unrestricted `OR`,
relicense upstream content, or invent a choice merely to clear a flag. A
reviewer may record that the verified alternatives are deliberately retained.
These collector checks are not 1:1 Eclipse IP review requests or approvals.

### Java-only developer builds

After a clean build with `-DskipFrontend=true`, the Vite module inventory may not
exist. The collector records **Missing npm bundle inventory**, emits an explicitly
incomplete HTML/JSON report, and does not infer shipped npm membership from the
manifest or lockfile. `--allow-incomplete` allows that Java-only developer build
to finish. Strict mode still emits the report but exits 1: release notice
verification requires the full frontend build first. Malformed existing JSON,
a directory or broken symlink instead of the inventory file, and invalid npm
project/lockfile inputs remain processing errors, even in developer mode.

When multiple installed npm locations contain the same package/version, the
component retains every selected module hash and per-location `installations`
records (source, integrity, declarations and attribution). License documents
retain their installation-relative paths. Coverage-review hashes bind all copies;
changing either a top-level or nested copy invalidates an earlier review. Extra
JAR directories are inventoried before manual supplements and conditional entries
are applied; loose JavaScript is scanned afterward so hash-verified manual
provenance can cover it.

### Native binaries in JARs

Every supplied JAR is scanned for native payload filenames: `.so` (including
numeric version suffixes such as `.so.1.2`), `.dll`, `.dylib`, `.jnilib`, `.wasm`,
and static-library `.a`/`.lib` entries, case-insensitively. Detected entries are
recorded in `nativePayloads` with archive provenance, full JAR SHA-256, internal
path and payload SHA-256. The HTML and generated review template retain this
inventory. This is independent of package names and manual supplements, and also
applies to project-classified JARs because they may embed third-party binaries.

Detection creates a `native-components` coverage finding. A wrapper's POM license
or supplied wrapper license text does not clear it. The same narrow human-review
record rules apply: pending records do not clear the finding; an explicitly
completed record must bind the exact component/version, artifacts, native paths
and hashes, declarations and documents, and retain reviewer/date/reason and
license-document references. Native payload changes invalidate that review.
Missing declarations or complete license texts still fail independently. No
native coverage records are automatically completed. Filename scanning does not
prove that an archive has no embedded code under other names or formats.
