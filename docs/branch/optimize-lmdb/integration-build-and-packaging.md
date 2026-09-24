# Build, dependency, workspace, and packaging changes

This page records build-system behavior visible in the comparison from merge
base `4aec7e9a2223d873b1c1a7703aad4c87bf8354df` to pinned `HEAD`
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. It distinguishes new branch
wiring from build context that is already required by the pinned tree. This
documentation pass did not invoke Maven, JavaCC, or any helper.

## Runtime and test dependencies

The root POM adds managed `org.codehaus.janino:janino` and
`org.codehaus.janino:commons-compiler` dependencies at `3.1.12` for the LMDB
IR code-generation path. Runtime class generation is an optional evaluator
strategy and its config/fallback semantics are covered in
[query IR and code generation](query-ir-and-codegen.md). LWJGL remains pinned
at `3.3.6` in the current root POM; do not treat a dependency version's
presence as proof that a particular vector/native strategy was selected.
The pinned root build targets Java 25 (POM and CI); this is part of the
current build baseline, not evidence that the branch changed the Java target.

Surefire/Failsafe now receive `rdf4j.test.outputDirectory` and
`rdf4j.test.tmpDirectory`; default output is the module build directory and
default temp is its `tmp` child. The root fork heap argument increases from
4 GiB to 8 GiB. This is test JVM configuration, not a production heap
recommendation or runtime cap. The change gives memory-heavy test selections a
larger configured ceiling; actual resident memory remains workload/JVM
dependent.

The `lmdb-sync-validation` profile sets Surefire/Failsafe properties that
make Janino compilation enabled, threshold rows zero, synchronous, and
fail-on-error, and makes direct-adjacency maintenance synchronous and
fail-on-maintenance-error. It is a diagnostic validation profile: activating
it opts the selected test execution into stricter failure visibility and
timing, not a production default. It does not itself select tests or prove
that any gate passed.

## Isolated Maven workspaces

The `workspace-build-root` profile activates when `rdf4j.build.root` is set.
It relocates each artifact's target directory to
`<build-root>/<groupId>/<artifactId>/<version>` and gives tests a
module-specific temporary path under `rdf4j.test.tmpRoot`. It stages the
Mockito Java agent under each module's output, makes that path available to the
fork configuration, sets the OBR repository to `NONE`, and redirects the
assembly's API-doc/WAR inputs through Maven properties. This supports
concurrent/isolated workspaces by separating module outputs and temp files;
callers still need to choose a unique build root and temp root when running
independent workspaces.

Shade-plugin generated dependency-reduced POM names are made unique, and
Spotless excludes both the ordinary and suffixed generated POM patterns. This
prevents workspace-generated POMs from colliding with one another or being
mistaken for authored formatting input. Related helper changes are in the
`.agent` Maven workspace support and
[`test_maven_workspace_pom.py`](../../../scripts/test_maven_workspace_pom.py).
The helper's implementation is developer tooling; this guide states its
intended build contract from source, not a run result.

## SPARQL AST generation and CI

Generated JavaCC outputs stay checked in. The POM does not regenerate them as
part of ordinary compilation. Instead, `scripts/manage-sparql-ast.py` provides
the manual `check`, `regenerate`, and `record` lifecycle and the PR workflow
invokes `check` after Java 25 setup. See
[AST patch maintenance](integration-developer-tooling.md#sparql-ast-patch-workflow)
and [language behavior](integration-sparql-language.md). The check is a
deterministic source-consistency guard; no invocation of it occurred for this
documentation task.

## Assembly and command distribution

The SDK assembly depends on the new bulk-loader artifact and places shell and
batch launchers under `bin`. The shell launcher has executable mode and Unix
line endings; the batch launcher has DOS line endings. API docs and server /
Workbench WAR paths are properties rather than hard-coded relative paths so
`workspace-build-root` can feed the assembly from its alternate output tree.
The onejar output now uses `${project.build.directory}` so Maven's configured
output location controls it. User-facing behavior is described in
[bulk-loader CLI and packaging](integration-bulk-loading.md).

## Compatibility and reader notes

The build configuration does not by itself establish runtime performance or
successful packaging. Java 25, Janino, and LWJGL versions should be interpreted
from the pinned root POM and dependency management. If a profile is newly
activated, document its scope and side effects next to the caller; do not
assume one test module's Maven system properties mutate a running RDF4J
Server JVM. Likewise, generated Java diffs need to be interpreted with the
grammar source and JavaCC patch workflow, not as independent handwritten
sources.

Source anchors: [root POM](../../../pom.xml), [assembly POM](../../../assembly/pom.xml),
[SDK assembly descriptor](../../../assembly/src/main/assembly/sdk.xml),
[PR verification workflow](../../../.github/workflows/pr-verify.yml),
[JavaCC README](../../../core/queryparser/sparql/JavaCC/README.md).
