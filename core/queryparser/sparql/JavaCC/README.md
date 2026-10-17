# SPARQL generated-source workflow

`ast/sparql.jjt` is the source grammar. JJTree and JavaCC generate the parser and
AST classes; tracked Git patches preserve RDF4J's optimizations, custom AST
inheritance, legacy nodes, license headers, agent attribution, and checksum
comments. Do not edit checked-in generated Java directly. Make any necessary
manual customization in scratch and record it as a replayable Git patch.

The tool is pinned to Maven Central's
[`net.java.dev.javacc:javacc:7.0.12`](https://repo.maven.apache.org/maven2/net/java/dev/javacc/javacc/7.0.12/)
([JavaCC downloads](https://javacc.github.io/javacc/downloads.html)). Its SHA-256 is
`067da95992b900106afa7b775dcd32a4a044078863d6e57bb7b551f44baf5d13`.
The checker verifies this digest even when an explicit local jar is supplied.
Use Python 3.10+, Git, Maven, and Java 25+ on Linux or macOS.

## Check locally

Run from the RDF4J repository root:

```sh
python3 scripts/manage-sparql-ast.py --check
```

Checks are offline by default. On first use, allow downloading the pinned jar and
missing Maven formatter dependencies, then return to offline operation:

```sh
python3 scripts/manage-sparql-ast.py --check --online
```

The jar is cached under `.m2_repo/net/java/dev/javacc/javacc/7.0.12/`.
Alternatively, pass `--jar /absolute/path/to/javacc-7.0.12.jar`. All Maven commands
use the workspace-local `.m2_repo`. The checker reports its retained scratch and
log directory. `--work-dir /new/evidence/directory` selects that location. Evidence
directories must be outside the checkout or under its `logs` directory. Exports
must be outside the checkout and outside the evidence directory.

The check copies **only the grammar** into an empty AST directory, runs
`org.javacc.jjtree.Main` followed by `org.javacc.parser.Main`, applies the ordered
tracked patches using `git apply --check` and `git apply`, and runs the project's
`process-resources` formatter in scratch, explicitly forcing `formatter.skip=false`. It then checks the complete file set and
exact bytes against checked-in sources. Expected Java is first read during that
final comparison. Checking never records patches or repairs a mismatch.

`generated-sources.json` declares 170 fresh generator outputs, 173 patched outputs,
and nine handwritten support files. The compatibility patch adds three marked
files that the current grammar no longer emits: `ASTAnnotation.java`,
`ASTPathNegatedPropertySet.java`, and `JavaCharStream.java`. Handwritten exclusions
are disjoint from generated files; new, missing, nested, or misclassified Java
files fail the check. The grammar digest detects even changes that would leave
Java output unchanged.

The patch sequence is explicit in the manifest:

1. `patches/01-optimize-SyntaxTreeBuilder..diff` (the two dots are intentional).
2. `patches/02-optimize-SyntaxTreeBuilderTokenManager.diff`.
3. `patches/03-preserve-ast-compatibility.diff`.

These are real Git diffs from fresh generator output to RDF4J's preserved source
postimages, including additions. Do not silently restore headers or checksum
comments from checked-in Java during replay. Their preservation belongs in patches.
Patch scope is restricted to manifest-listed generated Java files. Missing,
unlisted, corrupt, stale, or out-of-scope patches fail before comparison.

## Regenerate and maintain customizations

Export a regenerated, patched, formatted source set for review without writing
any checked-in Java:

```sh
python3 scripts/manage-sparql-ast.py --generate /tmp/sparql-reviewed-java
```

The export contains generated Java only. The tool retains the raw grammar,
intermediate `sparql.jj`, build configuration, logs, and generated sources in its
reported scratch directory, even when a patch fails.

For a grammar change, edit `sparql.jjt`, then use an explicit new work directory:

```sh
python3 scripts/manage-sparql-ast.py --generate /tmp/sparql-reviewed-java \
  --work-dir /tmp/sparql-replay-work
```

Generation intentionally bypasses the old grammar digest, but still checks file
sets and patch applicability. If the grammar adds a node or makes a patch stale,
the command fails and retains the fresh sources under
`/tmp/sparql-replay-work/build/core/queryparser/sparql/src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast`.
Rebase the remaining patches against that scratch tree with Git, resolve any
rejected hunks **there**, and review the resulting customizations. Patch logs show
which patches already applied; do not apply those again. For example, from the
scratch `build` directory, `git apply --reject /absolute/repo/path/to/remaining.diff`
retains rejected hunks for manual resolution. A new generator output must be
adopted explicitly; it cannot be reclassified as handwritten to hide a mismatch.

The scratch `build` directory includes the repository's POMs, formatter settings,
and a link to its Maven cache. After resolving the scratch changes, format there:

```sh
mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -Pformatting -Dformatter.skip=false -Denforcer.skip=true \
  -pl core/queryparser/sparql process-resources
```

Copy only the reviewed generated `.java` files into a new review directory. Do
not include the nine handwritten support files listed in the manifest. Record
those reviewed postimages as candidate Git patches:

```sh
python3 scripts/manage-sparql-ast.py --record-patches \
  --from-directory /tmp/sparql-reviewed-java
```

Recording computes real Git diffs, performs a second empty-directory replay,
applies the candidate patches, formats, and requires exact agreement with the
reviewed directory **before** updating the patch files and manifest. It updates
the grammar digest and output inventories. It never writes checked-in Java.
Without `--from-directory`, `--record-patches` preserves the current checked-in
Java bytes; use that mode to refresh the patch representation without changing
behavior. Recording is an explicit maintenance action and is never used in CI.

After reviewing the recorded patch diff, export once more into a fresh directory:

```sh
python3 scripts/manage-sparql-ast.py --generate /tmp/sparql-verified-java
```

Adopt that regenerated output into the AST source directory, preserving the nine
handwritten files. Review any explicitly removed generated filenames before
removing obsolete files; never clean the whole AST directory. Run `--check`, the
script tests, and the relevant parser tests before committing. Any behavior
change follows the repository's test-first requirements.

## Tests and CI

Fast contract tests use Python's standard library. Require real JavaCC, Git,
formatter, success, drift, failure, and patch-recording controls by supplying the
verified jar:

```sh
SPARQL_AST_JAVACC_JAR="$PWD/.m2_repo/net/java/dev/javacc/javacc/7.0.12/javacc-7.0.12.jar" \
  python3 -m unittest discover -s scripts -p test_sparql_ast.py -v
```

Without the environment variable, integration tests are explicitly skipped.
The `SPARQL generated sources` GitHub Actions workflow always supplies it and runs
the real checker on every pull request and push, without path filters. CI uses
`--online` to populate its local dependency cache. Failure logs and residual diffs
are uploaded. A green local check does not claim remote CI has run.
