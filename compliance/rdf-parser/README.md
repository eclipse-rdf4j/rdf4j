# RDF parsing inside SPARQL

Built-in RDF4J query support providing two complementary interfaces over one Rio parser:
property functions producing RDF terms in rows, and a local SERVICE evaluating
SPARQL against a private parsed dataset. No parser operation writes to the base
store. INSERT/DELETE templates remain the only update mechanism.

Requires RDF4J 6.2.0-SNAPSHOT and Java 25 or newer. The supplied implementation
was integrated with current main through issue #6259.

## Installation

Use an ordinary SailRepository. The standard RDF4J evaluator includes the parser
runtime and supported Rio formats; no additional Sail, artifact, or configuration
is required. For example:

```java
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;

var repository = new SailRepository(new LmdbStore());
repository.init();
try (var connection = repository.getConnection()) {
    var query = connection.prepareTupleQuery("""
        PREFIX parse: <http://rdf4j.org/function/parse#>
        SELECT ?s ?p ?o WHERE {
            (?document "text/turtle") parse:triples (?s ?p ?o)
        }
        """);
    query.setBinding("document", repository.getValueFactory().createLiteral(
            "<urn:alice> <urn:name> \"Alice\" ."));
    try (var rows = query.evaluate()) {
        while (rows.hasNext()) {
            System.out.println(rows.next());
        }
    }
} finally {
    repository.shutDown();
}
```

MemoryStore, NativeStore and existing Sail stacks use the same query interfaces.
Server and Workbench repositories inherit support through ordinary repository
configuration. Native and custom evaluation factories retain their normal
selection; parser syntax becomes standard tuple-function and LATERAL algebra
before optimization. EXPLAIN uses that normalized algebra. Independent custom
query engines must implement RDF4J's extension algebra and local SERVICE contract.
Bind generated content with the query API; do not concatenate LLM text into query
source. This extension does not call an LLM or manage API keys.

## Property functions

Use `PREFIX parse: <http://rdf4j.org/function/parse#>`.

```sparql
(?document "text/turtle") parse:triples (?s ?p ?o) .
(?document "text/turtle" <https://example.org/base/>) parse:triples (?s ?p ?o) .
?document parse:turtle (?s ?p ?o) .
(?document <https://example.org/base/>) parse:turtle (?s ?p ?o) .
(?document "application/trig") parse:quads (?s ?p ?o ?g) .
(?document "text/turtle") parse:tripleTerms ?triple .
```

The parenthesized inputs and outputs are syntactic SPARQL collections, rewritten
into TupleFunctionCall algebra; they are not RDF lists read from the store. Calls
must be outside GRAPH clauses. Input variables must have a bound provider; output
variables can already be bound or repeated, in which case term-equality constraints
apply. Dependent calls use LATERAL algebra, not accidental join ordering. The
query execution applies RDF4J's scope-aware API-binding substitution to a private clone
of the original algebra before adding those dependencies. This is important
because Sail backends execute the optimized plan with an empty runtime input
mapping. Chained calls can appear before their providers in the same conjunctive
group; tuple calls and the local SERVICE can feed each other. SERVICE expression
inputs used only by FILTER, BIND, EXISTS, or NOT EXISTS also retain their outer
bindings and can wait for a parser producer. Private, unprojected subquery names
remain local to that subquery.

The parser accepts xsd:string content and media type, plus an optional absolute
IRI base. The SPARQL BASE/PREFIX prologue is not inherited by the document.
Turtle's own BASE/PREFIX declarations work normally. Triple and triple-term
functions reject datasets containing named statements rather than silently
flattening them. Triple terms represent propositions and do not assert them.

`parse:quads` uses the IRI `parse:defaultGraph` for the default graph. It is a tuple
marker, not a magic insert graph name. Explicitly route it when inserting:

```sparql
PREFIX parse: <http://rdf4j.org/function/parse#>
INSERT {
  ?defaultSubject ?p ?o .
  GRAPH ?namedGraph { ?s ?p ?o }
}
WHERE {
  (?document "application/trig") parse:quads (?s ?p ?o ?g) .
  BIND(IF(?g = parse:defaultGraph, ?s, 1/0) AS ?defaultSubject)
  BIND(IF(?g != parse:defaultGraph, ?g, 1/0) AS ?namedGraph)
}
```

The deliberate expression errors leave the inapplicable template variable
unbound. To discard graph names deliberately, insert all rows into one fixed
target graph. Input graph names equal to parse:defaultGraph, RDF4J.NIL or the
legacy Sesame nil marker are rejected rather than conflated with the default.

For a simpler graph import:

```sparql
PREFIX parse: <http://rdf4j.org/function/parse#>
INSERT { GRAPH <urn:candidates> { ?s ?p ?o } }
WHERE { (?document "text/turtle") parse:triples (?s ?p ?o) }
```

Use INSERT WHERE, not INSERT DATA. CONSTRUCT and SELECT consume the same bindings.

## Local parsed dataset SERVICE

```sparql
PREFIX parse: <http://rdf4j.org/function/parse#>
PREFIX ex: <https://example.org/>
SELECT ?person ?companyName WHERE {
  SERVICE <urn:rdf4j:parse> {
    [] a parse:Source ;
       parse:content ?document ;
       parse:format "text/turtle" ;
       parse:baseIRI <https://example.org/> .
    ?person ex:knows/ex:worksFor/ex:name ?companyName .
  }
}
```

Exactly one top-level configuration node is required: content, format, and
optional baseIRI. Mark it with `a parse:Source`; use `[]` or a constant IRI as
its subject. Only that node's options and marker are removed before querying.
Ordinary data patterns such as `?item parse:format ?format` remain graph patterns.
The original unmarked shorthand still works when exactly one eligible subject
has both required options. When the body also queries complete data records
with these predicates and constant/anonymous subjects, use the explicit marker
to remove ambiguity. Duplicate options,
unbound input variables, and configuration hidden inside GRAPH, OPTIONAL's
right arm, UNION branches or subqueries are rejected/missing, not guessed.
A constant SERVICE IRI is the supported correlated form; query execution does not
infer a variable SERVICE endpoint's input dependencies.

The body runs against an immutable LinkedHashModel-backed NativeTripleTermSource using
RDF4J's normal evaluator. Joins, paths, OPTIONAL, MINUS, UNION, EXISTS, subqueries
and aggregates use graph matching, not manually joined triple rows. For TriG or
N-Quads, the parsed default graph is separate from named graphs, including
blank-node graph names. Ordinary GRAPH patterns query only the parsed dataset.
They cannot accidentally see the repository's graphs. Results can be joined with
repository data outside SERVICE or consumed by an INSERT template.

Direct and nested RDF 1.2 triple-term patterns are supported. Their component
index is built lazily, only when term matching is requested. Exact components
use a hash lookup; partial components use the smallest available candidate
list. Repeated component variables are equality constraints, incompatible
bound term types yield no match, and matching a term does not assert it.

Each correlated invocation owns a distinct parsed document. All accesses inside
that invocation share its blank-node identity. Repeated invocations (even with
identical strings), independent queries and concurrent users do not share a
cached graph or blank-node identity. Parsed data is released with the result
iteration; close query results explicitly. There is intentionally no global or
cross-invocation parsed-document cache.

Nested SERVICE is disabled inside this local service. Other repository SERVICE
endpoints still use the application's delegated resolver. SERVICE SILENT follows
RDF4J's failure behavior: an invocation failure preserves its incoming solution,
whereas a valid empty graph with no match returns zero rows.

## Safety, semantics and costs

Supported formats are **Turtle, TriG, N-Triples and N-Quads**, using the installed
Rio parsers, including RDF 1.2 support. RDF/XML, JSON-LD and other
formats are deliberately rejected. Enabling them requires explicit secure XML
and JSON-LD document-loader policies and additional tests; merely registering a
Rio parser does not enable it here. Content is inline only, never a URL to fetch.

Default per-document limits: 8 MiB UTF-8 input, 100,000 emitted statements,
64 structural nesting levels, and 10 seconds of cooperative parsing time.
These immutable defaults apply to the standard query functions. Direct parser
callers may supply `RdfParseOptions` to `RdfDocumentParser`.
Nesting is checked in the structural preflight and at Rio's actual recursive
parser entry points. Comments, strings and IRI contents are excluded from the
preflight count. Malformed datatype syntax is rejected before recursive value
parsing; invalid serialization escapes are fatal independently of datatype
lexical validation. Syntactically valid ill-typed literals remain representable.

These safeguards use Rio's parser recursion limits.
`BasicParserSettings.MAX_NESTING_DEPTH` defaults to effectively unbounded
for compatibility with general Rio callers; this extension explicitly supplies
its finite per-document limit. Generic N-Triples/N-Quads callers can still opt
into invalid-line recovery; this extension does not enable it. Interrupts are checked during preflight, reader
consumption and statement production. Time limits are cooperative, not an OS
sandbox. Query evaluation and output size still require normal query/server
limits: the parser's statement limit does not cap a join's result cardinality or
SERVICE SILENT's result buffering. Custom SPARQL functions are not sandboxed.

A document must parse completely before any result row is exposed. Therefore a
malformed suffix cannot be hidden by LIMIT and cannot expose a successfully
parsed prefix. **Parsing is bounded materialization, not constant-memory
streaming.** Tuple rows are produced lazily afterwards. Duplicate statements are
collapsed as RDF set semantics require, but still consume the parsing budget.
A later failed document in a multi-document update still requires the caller's
normal transaction/rollback discipline. No out-of-band connection is opened.

RDF4J models represent statements, not empty graph declarations; empty named
graphs in TriG cannot be preserved by this adapter. Namespace declarations are
used during parsing but are not installed into the persistent repository. SHACL
validation is not automatically run on the temporary model; use the normal
validation/transaction layer for accepted persistent data.

Tuple functions materialize an insertion-ordered statement set without building
subject/predicate/object/context indexes. SERVICE still builds an indexed model,
because it supports graph matching. Triple/quad rows are immutable views of a
statement, without per-row backing arrays; retaining a row does not retain the
whole parsed document. No mutable row object is recycled.

Ordinary conjunctive groups are flattened and inspected once by the rewriter,
rather than repeatedly flattening each join subtree. Calls in one group share
collection indexes, validating only cells reachable from parser argument heads.
Ordinary graph patterns involving `rdf:first` or `rdf:rest` are not required to
be well-formed lists. Call scheduling distinguishes assured from merely
possible OPTIONAL/UNION/SILENT outputs. A stable ready queue and per-variable
wait lists avoid repeated scans of every remaining call. This is a scheduling
improvement, not a claim of linear complexity for the entire SPARQL compiler.
SERVICE removes a redundant algebra clone; its batch SPI
prepares a private template once per batch, but clones the template and parses a
fresh dataset for every input. Scalar SERVICE invocations do not share a cache.
Already-compatible projected rows are returned without copying; other rows keep
the original merge and conflict checks.

The downloadable verification bundle includes historical benchmark methodology
and measurements. Those are implementation-level comparisons with the previous
commit, not evidence of a speedup over application-side Rio import or another
triple store. The review corrections do not inherit those historical timing claims.

## Build and verification

From a full RDF4J checkout with Java 25 or newer and Maven, run the focused
verification helper. It installs current dependencies and runs all module tests,
the affected Rio suites, and shared evaluator regressions:

```sh
bash compliance/rdf-parser/verify.sh
```

Both helpers use the workspace-local `.m2_repo` and offline Maven by default.
For a first build with missing dependencies, set `MVN_OFFLINE=false` once, then
return to the offline default. Full logs remain in `maven-build.log` and
`logs/rdf-parser-integration/`; reports remain under each module's `target`.

The original suite contains 23 parser unit cases and 24 integration scenarios
parameterized over MemoryStore, NativeStore and LMDB (72 backend invocations).
The optimization suite adds direct tests of tuple term equality, row immutability,
iterator cleanup, algebra rewriting, projection, batch isolation and concurrency,
plus additional scenarios parameterized over the same three stores. Review
regressions exercise actual parser recursion limits independently of preflight,
serialization syntax errors, native term matching and its shared evaluator,
uncertain dependency outputs, and configuration/data separation. The exact
executed counts and outcomes are recorded in the delivery verification report.
The complete upstream RDF4J suite is not included in this focused command.

To measure the current implementation with JMH after building and testing:

```sh
bash compliance/rdf-parser/benchmark.sh
```

Results and logs are retained in a fresh directory under
`compliance/rdf-parser/target/benchmarks/`. To compare runtime implementations,
set `BASELINE_COMMIT` to a locally available commit containing the standard
`core/queryalgebra/evaluation` parser package.
Both variants use current shared Rio/evaluator dependencies, so this comparison
does not measure a complete old repository against the current repository.
No historical feature commit is required for a current-only run.
