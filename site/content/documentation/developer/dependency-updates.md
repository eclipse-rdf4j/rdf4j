---
title: "Checking dependency updates"
layout: "doc"
toc: true
---

RDF4J includes a Maven-integrated script that reports the update status of all
external dependencies across the multi-module build. It queries
[Maven Central](https://repo1.maven.org/maven2) directly and prints two tables:
one grouped by version status, and one for dependencies that have been
re-released under different Maven coordinates.

## Running the report

From the project root, run:

```bash
mvn -N -P dependency-updates validate
```

The `-N` flag (non-recursive) is required so Maven only processes the root POM
and the report is printed once.

The script can also be invoked directly without Maven:

```bash
python3 scripts/dependency-updates.py [project-root-dir]
```

Python 3 is the only prerequisite. An internet connection is required to reach
Maven Central.

## Report structure

### Table 1 — Version status

Lists every external dependency managed in the root `pom.xml`, together with
the current version in use, the latest release version on Maven Central, and
the sub-modules that declare the dependency. Entries are grouped into five
sections:

| Section | Meaning |
|---|---|
| **Major version updates** | The latest version has a higher major number |
| **Minor / patch updates** | The latest version is a higher minor or patch release within the same major line |
| **Up to date** | The version in use matches the latest release |
| **Unknown** | `maven-metadata.xml` could not be fetched (artifact may have been removed or relocated) |
| **Current version unresolved (module-level BOM)** | The dependency is used by a module but its version is not declared in the root POM or any root-imported BOM; the current version shown is `?` |

The **Modules** column lists up to six sub-module artifact IDs that declare the
dependency, followed by `+N more` when the count exceeds that limit.

### Table 2 — Available under different coordinates

Some libraries publish a new generation under a completely different Maven
`groupId` or `artifactId` rather than bumping the major version of the existing
artifact. This table lists known such cases for dependencies currently used in
the project.

Examples covered by the built-in mapping:

| Current artifact | Successor artifact |
|---|---|
| `com.fasterxml.jackson.*` (2.x) | `tools.jackson.*` (3.x) |
| `org.apache.httpcomponents:httpclient` (HC4) | `org.apache.httpcomponents.client5:httpclient5` (HC5) |
| `org.apache.httpcomponents:httpcore` (HC4) | `org.apache.httpcomponents.core5:httpcore5` (HC5) |
| `org.elasticsearch.client:elasticsearch-rest-high-level-client` | `co.elastic.clients:elasticsearch-java` |
| `javax.servlet:javax.servlet-api` | `jakarta.servlet:jakarta.servlet-api` |

When a successor artifact is listed as `(not found — possibly merged)` it means
the expected coordinates do not exist on Maven Central. This typically indicates
that the functionality was merged into another artifact in the new generation
(for example, `jackson-annotations` and `jackson-datatype-jdk8` were absorbed
into `jackson-core` in Jackson 3.x).

## Extending the coordinate-rename mapping

The mapping is defined at the top of `scripts/dependency-updates.py` in two
constants:

**`EXPLICIT_SUCCESSORS`** — a dict of exact `groupId:artifactId` →
`groupId:artifactId` pairs, used when both coordinates change:

```python
EXPLICIT_SUCCESSORS: dict = {
    "org.apache.httpcomponents:httpclient": "org.apache.httpcomponents.client5:httpclient5",
    # add further mappings here ...
}
```

**`GROUP_PREFIX_SUCCESSORS`** — a list of `(old_group_prefix, new_group_prefix)`
tuples for artifact families where only the group prefix changes and the
artifact ID stays the same:

```python
GROUP_PREFIX_SUCCESSORS: list = [
    ("com.fasterxml.jackson", "tools.jackson"),
    # add further prefix pairs here ...
]
```

For a group-prefix rule, `com.fasterxml.jackson.core:jackson-databind` would
map to `tools.jackson.core:jackson-databind` — the suffix `.core` is preserved
and only the leading prefix is replaced.
