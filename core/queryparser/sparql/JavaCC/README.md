# SPARQL Abstract Syntax Tree

The SPARQL parser and its supporting AST sources are generated from
`src/main/java/org/eclipse/rdf4j/query/parser/sparql/ast/sparql.jjt`. RDF4J keeps the generated
sources in Git and carries parser and lexer optimizations as two ordered patches.

Use the repository management script from the RDF4J root instead of invoking JavaCC or Spotless
manually:

```bash
python3 scripts/manage-sparql-ast.py check
python3 scripts/manage-sparql-ast.py regenerate
python3 scripts/manage-sparql-ast.py record
```

`check` performs a complete replay, compares every JavaCC-touched Java file byte-for-byte with the
checked-in sources, restores the working tree, and exits nonzero if anything drifts. This is the
command run by pull-request CI.

`regenerate` refuses to overwrite modified generated outputs. It replaces clean generated outputs
with a fresh formatted and patched replay, and restores their original bytes if generation,
formatting, or patch application fails.

`record` treats the current customized parser and token-manager sources as the desired result. It
recreates the formatted JavaCC baseline, writes both patches deterministically, replays them, and
only keeps the new patch files when that replay exactly reproduces the original sources. A second
`record` run must make no patch changes. Differences in any other generated output fail visibly
instead of being silently omitted.

## Pinned JavaCC tool

The script uses the official Maven Central JavaCC 7.0.12 jar. It caches the jar under the module's
ignored `target/javacc/` directory and requires this SHA-256 digest:

```text
067da95992b900106afa7b775dcd32a4a044078863d6e57bb7b551f44baf5d13
```

Use `--javacc-jar PATH` to supply an existing copy; supplied jars are subject to the same checksum
check. Use `--offline` to prohibit both downloading the jar and Maven's online retry for missing
Spotless artifacts. Options may appear before or after the command, for example:

```bash
python3 scripts/manage-sparql-ast.py check --offline
python3 scripts/manage-sparql-ast.py --javacc-jar /path/to/javacc-7.0.12.jar regenerate
```

JavaCC upgrades are separate changes: do not replace the pinned jar merely to make a regeneration
diff disappear.

## Canonical generation order

The reproducible order is:

1. Run `jjtree` and `javacc` with JavaCC 7.0.12.
2. Restore the exact pre-existing RDF4J copyright header on every touched Java file.
3. Run Spotless only for the files touched or created by JavaCC, using Maven's `spotlessFiles`
   targeting.
4. Apply the patches below in lexical order.

Spotless runs before patch application. Formatting a customized result and then trying to apply a
patch made from an unformatted baseline will produce a stale patch.

JavaCC embeds an `OriginalChecksum` comment based on its pre-Spotless template bytes. When that
comment is the only post-format difference, the script retains the existing comment; any Java body
difference still fails the exact comparison.

The temporary `sparql.jj` is always removed. The script snapshots the AST directory before starting
JavaCC and restores changed, deleted, and newly created generated files after failures. A
pre-existing `sparql.jj` is rejected because the script cannot safely assume ownership of it.
Unrelated working-tree changes are never restored, formatted, or otherwise modified.

## Recording generated-code improvements

The current checked-in Java files are the source of truth for customized generated code. To retain a
new optimization:

1. Start from a successful `regenerate` or `check` result.
2. Edit `SyntaxTreeBuilder.java` and/or `SyntaxTreeBuilderTokenManager.java` directly and test the
   behavior.
3. Run `python3 scripts/manage-sparql-ast.py record`.
4. Review both patch files, then run `python3 scripts/manage-sparql-ast.py check`.

The patches are deliberately file-scoped:

- `patches/01-optimize-SyntaxTreeBuilder.diff` changes only `SyntaxTreeBuilder.java`.
- `patches/02-optimize-SyntaxTreeBuilderTokenManager.diff` changes only
  `SyntaxTreeBuilderTokenManager.java`.

Do not hand-edit patch offsets or copy patches from a differently formatted baseline. Make the
desired Java edit and let `record` serialize it.

## RDF4J-owned AST classes

JavaCC does not currently regenerate every `AST*.java` source in the package. RDF4J has additional
AST classes and customizations, including inheritance relationships and methods on `Node` and
`SimpleNode`, that are not represented solely by JavaCC's default node generation. The workflow
detects the files JavaCC actually touches and does not delete or rewrite the remaining classes.
