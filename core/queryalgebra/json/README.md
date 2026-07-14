# RDF4J tuple-expression JSON codec

`rdf4j-queryalgebra-json` exports and imports native RDF4J `TupleExpr`, `ValueExpr`, `Var`, `BindingSet`, and RDF value objects using a strict JSON representation. It is a semantic AST format, not Java object serialization: every accepted node type and field is explicitly defined, constructors and setters establish normal RDF4J parent links, and no class names are reflectively loaded.

## Usage

```java
TupleExprJsonParser parser = new TupleExprJsonParser();
TupleExpr expression = parser.parse(json);

TupleExprJsonWriter writer = new TupleExprJsonWriter();
String canonicalJson = writer.toJson(expression);
```

A custom `ValueFactory` can be supplied when RDF values need a repository-specific implementation:

```java
TupleExpr expression = new TupleExprJsonParser(valueFactory).parse(inputStream);
```

The parser accepts a `String`, `Reader`, `InputStream`, or Jackson `JsonNode`. The writer returns a `String` or Jackson `JsonNode`, and writes compact UTF-8 JSON to a `Writer` or `OutputStream`. Neither side closes or flushes caller-owned inputs or outputs.

## Document shape

The writer always emits this versioned envelope:

```json
{
  "format": "rdf4j-tuple-expr",
  "version": 1,
  "expression": {
    "type": "Projection",
    "arg": {
      "type": "StatementPattern",
      "subjectVar": { "type": "Var", "name": "s" },
      "predicateVar": {
        "type": "Var",
        "name": "p",
        "value": { "type": "IRI", "value": "urn:example:knows" },
        "constant": true
      },
      "objectVar": { "type": "Var", "name": "o" }
    },
    "projectionElements": [
      { "name": "s" },
      { "name": "o", "projectionAlias": "friend" }
    ]
  }
}
```

For small embedded payloads, the parser also accepts a tuple node as the document root:

```json
{ "type": "SingletonSet" }
```

The `type` discriminator is the RDF4J model class's simple name. Names are case-sensitive. Unknown types, unknown fields, duplicate JSON keys, trailing documents, missing required fields, and invalid field types are rejected with an exception containing a JSON path.

## Common node fields

Every object-shaped tuple, value, or helper node may carry these optional fields:

| Field | JSON type | Effect |
| --- | --- | --- |
| `variableScopeChange` | boolean | Calls `setVariableScopeChange` |
| `resultSizeEstimate` | finite number | Calls `setResultSizeEstimate` |
| `costEstimate` | finite number | Calls `setCostEstimate` |
| `optimizationTag` | 64-bit integer | Calls `setOptimizationTag` |
| `runtimeTelemetryEnabled` | boolean | Calls `setRuntimeTelemetryEnabled` |

Parent references are never represented. They are established by RDF4J constructors and setters while decoding.

## Canonical output

The writer produces deterministic compact JSON. Ordered model children and lists retain their order; unordered binding-name sets, group-name sets, lateral input-name sets, and prefix maps are sorted lexicographically. `BindingSetAssignment` rows retain row order and contain every declared binding in sorted schema order, with JSON `null` representing `UNDEF`.

Documented defaults are omitted. Non-default optimizer metadata, statement order/index, join flags, projection and service state, RDF-star values, directional literals, and arbitrary-path minimum length are preserved. Unsupported subclasses, shared or cyclic query-model nodes, malformed required state, non-finite numeric metadata, trees deeper than 512 nodes, and strings longer than 16 MiB are rejected with node-type and JSON-path context.

## RDF values and variables

Variables use this shape:

```json
{
  "type": "Var",
  "name": "x",
  "value": { "type": "IRI", "value": "urn:example:x" },
  "anonymous": false,
  "constant": true,
  "bNode": false
}
```

Only `type` and `name` are required. Supported RDF value forms are:

```json
{ "type": "IRI", "value": "urn:example:value" }
{ "type": "BNode", "id": "b1" }
{ "type": "Literal", "label": "plain" }
{ "type": "Literal", "label": "hello", "language": "en" }
{ "type": "Literal", "label": "hello", "language": "en", "direction": "ltr" }
{ "type": "Literal", "label": "7", "datatype": "http://www.w3.org/2001/XMLSchema#integer" }
{
  "type": "TripleTerm",
  "subject": { "type": "IRI", "value": "urn:s" },
  "predicate": { "type": "IRI", "value": "urn:p" },
  "object": { "type": "Literal", "label": "o" }
}
```

`language` and `datatype` are mutually exclusive. `direction` requires `language` and accepts RDF4J `Literal.BaseDirection` names case-insensitively.

## Binding-set assignments

`BindingSetAssignment` declares its schema separately from its rows. A JSON `null` row value represents SPARQL `UNDEF` and therefore creates no binding.

```json
{
  "type": "BindingSetAssignment",
  "declaredBindingNames": ["x", "label"],
  "bindingSets": [
    {
      "x": { "type": "IRI", "value": "urn:x" },
      "label": { "type": "Literal", "label": "example", "language": "en" }
    },
    { "x": null }
  ]
}
```

`bindingNames` is accepted as an alias for `declaredBindingNames`; exactly one of the two fields must be present. A row cannot contain an undeclared binding name.

## Tuple-expression reference

The codec supports every concrete native RDF4J `TupleExpr` listed below.

| Type or group | Type-specific fields |
| --- | --- |
| `EmptySet`, `SingletonSet` | none |
| `DescribeOperator`, `Distinct`, `QueryRoot`, `Reduced` | `arg` |
| `Difference`, `Intersection`, `Union` | `leftArg`, `rightArg` |
| `Join` | `leftArg`, `rightArg`; optional `mergeJoin`, `cacheable` |
| `Lateral` | `leftArg`, `rightArg`; optional `rightInputBindingNames` |
| `LeftJoin` | `leftArg`, `rightArg`; optional `condition` |
| `Filter` | `arg`, `condition` |
| `Extension` | `arg`; optional `elements` (`name`, `expr`) |
| `Group` | `arg`; optional `groupBindingNames`, `groupElements` (`name`, aggregate `operator`) |
| `Order` | `arg`; optional `elements` (`expr`, optional `ascending`) |
| `Projection` | `arg`, `projectionElements`; optional `projectionContext`, `subquery` |
| `MultiProjection` | `arg`, `projections` (an array of projection-element arrays) |
| `Slice` | `arg`; optional `offset`, `limit` (defaults are `-1`) |
| `StatementPattern` | `subjectVar`, `predicateVar`, `objectVar`; optional `scope`, `contextVar`, `statementOrder`, `indexName` |
| `ZeroLengthPath` | `subjectVar`, `objectVar`; optional `scope`, `contextVar` |
| `ArbitraryLengthPath` | `subjectVar`, `pathExpression`, `objectVar`, `minLength`; optional `scope`, `contextVar` |
| `TripleRef` | `subjectVar`, `predicateVar`, `objectVar`; optional `exprVar` |
| `ReifiedTripleRef`, `AnnotationTripleRef` | `subjectVar`, `predicateVar`, `objectVar`, `reifVar`; optional `exprVar` |
| `Service` | `serviceRef`, `arg`, `serviceExpressionString`; optional `prefixDeclarations`, `baseURI`, `silent` |
| `TupleFunctionCall` | `uri`, `resultVars`; optional `args` |
| `BindingSetAssignment` | declared names and rows as described above |

`scope` accepts `DEFAULT_CONTEXTS`/`DEFAULT` or `NAMED_CONTEXTS`/`NAMED`. `statementOrder` accepts `S`, `P`, `O`, or `C`.

Projection elements have a required `name` and optional `projectionAlias`, `aggregateOperatorInExpression`, and `sourceExpression`. A source expression uses the same `{ "name": ..., "expr": ... }` shape as an extension element.

## Value-expression reference

Value expressions can appear wherever a tuple node calls for an expression.

| Type or group | Type-specific fields |
| --- | --- |
| `Var` | `name`; optional `value`, `anonymous`, `constant`, `bNode` |
| `ValueConstant` | `value` |
| Unary operators (`Datatype`, `HasLang`, `HasLangDir`, `IRIFunction`, `IsBNode`, `IsLiteral`, `IsNumeric`, `IsResource`, `IsTriple`, `IsURI`, `Label`, `Lang`, `LangDir`, `LocalName`, `Namespace`, `Not`, `Str`) | `arg`; `IRIFunction` also accepts `baseURI` |
| Binary operators (`And`, `LangMatches`, `Or`, `SameTerm`) | `leftArg`, `rightArg` |
| `Compare` | `leftArg`, `rightArg`, `operator` |
| `MathExpr` | `leftArg`, `rightArg`, `operator` |
| `CompareAll`, `CompareAny` | `valueExpr`, `subQuery`, `operator` |
| `In` | `valueExpr`, `subQuery` |
| `Exists` | `subQuery` |
| `Bound` | variable `arg` |
| `BNodeGenerator` | optional `nodeIdExpr` |
| `Coalesce`, `ListMemberOperator` | `args` |
| `FunctionCall` | `uri`; optional `args` |
| `If` | `condition`; optional `result`, `alternative` |
| `Regex` | `arg`, `patternArg`; optional `flagsArg` |
| `StrLangDir` | `lexicalFormArg`, `langArg`, `dirArg` |
| Aggregates (`Avg`, `Count`, `Max`, `Min`, `Sample`, `Sum`) | optional `arg`, `distinct` |
| `GroupConcat` | optional `arg`, `distinct`, `separator` |
| `AggregateFunctionCall` | `iri`; optional `args`, `distinct` |
| `TripleComponent` | variable `tripleRefVar`, `role` |
| `ValueExprTripleRef` | `extVarName`, `subjectVar`, `predicateVar`, `objectVar` |

Comparison and math operators accept either their enum name or their SPARQL symbol, case-insensitively for names. `TripleComponent.role` accepts `SUBJECT`, `PREDICATE`, or `OBJECT`.

## Deliberate exclusions

The format accepts query tuple expressions, not RDF4J `UpdateExpr` objects or arbitrary third-party subclasses. It does not import parent links, `QueryScopeSeed`, prepared `SERVICE` query caches, arbitrary object-keyed query metadata, cardinality caches, selected execution-algorithm labels, or actual/planned telemetry maps. Those values are internal, derived, runtime-specific, or not safely representable as portable JSON.

The byte and character parsers enforce duplicate-key detection, a maximum nesting depth of 512, a maximum JSON string length of 16 MiB, and a single top-level JSON value. A caller supplying an already-built `JsonNode` remains responsible for equivalent limits while constructing that tree. The writer independently enforces the same model depth and string limits.
