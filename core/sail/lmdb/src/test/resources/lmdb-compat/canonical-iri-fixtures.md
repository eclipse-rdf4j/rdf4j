# Canonical IRI format fixtures

These stores were written by independent binaries built from their actual source commits. They were not created by
format-7 code and relabeled with an old version. The current implementation supports the released format 2 and the new
canonical format 7. The unreleased format 6 is deliberately unsupported.

| Asset | Purpose | Original writer | Persisted format | Producer commit |
| --- | --- | --- | --- | --- |
| `canonical-iri-format-2.zip` | Positive read/write/reopen compatibility | Eclipse RDF4J 6.1.0 | 2 | `46a2d41ab16fead609884ee655e26e6baa1ce325` |
| `unsupported-format-6.zip` | Rejection without modifying persistent files | HASMAC-AS/rdf4j main before this change | 6 | `db572a45a401d9e7284b321efba0adbd6d577e6c` |

Both original binaries were built with Eclipse Temurin 25.0.5+8 and Maven 3.9.11 on Linux x86-64. The format-6 fixture
was produced from the unmodified module jar saved before implementation edits. Each generated store was closed and
reopened successfully in a separate process running its original binary before packaging. The producer helper was
compiled separately against each original jar and dependency classpath, with no current implementation classes in
either classpath.

## Contents and provenance

Each archive contains `store/` and `manifest.properties`. The manifest records the exact source commit, SHA-256 of the
actual loaded `ValueStore.class`, SHA-256 of the separately compiled producer, the original store-properties SHA-256,
the persisted format, literal inlining policy, and original dictionary IDs. Lock files are excluded.

The values deliberately preserve caller-supplied namespace boundaries that differ from `URIUtil.getLocalNameIndex`:

- An ordinary `urn:` with a slash in the supplied local name.
- A hash before later slash/colon characters.
- An empty local name.
- Unicode and a supplementary character.
- A long hash-indexed namespace and a long hash-indexed local name.
- A custom datatype IRI, a typed literal, a triple term and a nested triple term.

All ten values are referenced by asserted statements. Both fixtures also persist a namespace declaration; the
format-6 fixture therefore includes a populated namespace snapshot and a prepared-startup certificate from its own
writer. Inlining is disabled so tests observe persisted dictionary records directly. The format-6 asset is used only
to prove safe rejection, including native and prepared-store entry points.

The 6.1.0 producer requests an empty triple-index string at initial creation and explicitly requests term indexes
`spoc,cspo`. That writer's dirty-flag behavior otherwise omits `store.properties` or its term-index descriptor with
the default configuration. Its own normal initialization resolves the statement indexes to `spoc,posc` and writes
version 2; the generator never edits that file. Reopening supplies both original index settings explicitly because
6.1.0's term-index initialization also assigns the statement-index setting from its configuration. The release's
default-config reopening bugs are excluded from the comparison; native records, properties and read/write operations
still come from the unmodified release binary.

SHA-256 of the checked-in archives:

```text
a990be76dc64b5b76f9c5daa4b49db16913fdb0f4f865311348aa370c135fb6c  canonical-iri-format-2.zip
e69814ab34cb2cbb1915850c6b7426c44c52c150fd446daeb1a7db4a8f1c50e9  unsupported-format-6.zip
```

Loaded `ValueStore.class` SHA-256:

```text
d3c344fd45b4dc83ad032bec84881997f609fb3c0ab4ca660aa5439c17b69101  actual release format 2
f89b0ae5eb7030262be0a2d8790ce9f17d5ce0499cf8970750eded25d74856c4  unsupported format 6
```

## Verification and regeneration

`LmdbCanonicalIriCompatibilityTest` opens the actual release fixture, verifies every ID and statement, looks up and
reinserts the same lexical IRIs with canonical splits, writes new statements, reopens, and checks that the original
store-properties bytes did not change. `LmdbCanonicalIriFormatPolicyTest` checks rejection of the real unsupported
format-6 fixture without changing persistent files.

To build the actual 6.1.0 binary and run its cross-process checks as well:

```bash
scripts/verify-canonical-iri-compatibility.sh
```

The script pins the original commit and uses a separate checkout and Maven repository. The process tests alternate
current and release writers, clear with the current writer, and reopen with the release writer. They also compare
every persistent file around the release's rejection of format 7, covering both an ordinary canonical store and an
actual empty interrupted-initial-import marker.

The optional pre-change format-6 binary is useful as an independent performance baseline and to prove that its bulk
reader rejects an interrupted canonical publication before a publication manifest exists. It does not add current
format-6 read/write/admission support:

```bash
scripts/verify-canonical-iri-compatibility.sh --with-prechange-baseline
```

Regenerate the positive release fixture and the negative unsupported fixture with separate explicit options:

```bash
scripts/verify-canonical-iri-compatibility.sh --regenerate-fixtures
scripts/verify-canonical-iri-compatibility.sh --regenerate-unsupported-format6
```

Set `RDF4J_CANONICAL_COMPAT_WORKDIR` to choose the build/evidence directory. Generated files include timestamps and
native transaction metadata, so archive hashes may change on regeneration; inspect the new manifests and update the
hashes above. The original writer source commits remain fixed.
