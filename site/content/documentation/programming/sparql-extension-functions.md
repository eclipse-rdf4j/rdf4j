---
title: "SPARQL extension functions"
toc: true
weight: 9
autonumbering: true
---

RDF4J ships a library of SPARQL extension functions that goes well beyond the SPARQL 1.1 built-ins: mathematical, string, date/time, aggregate, window (analytical), text-similarity, JSON/array, geospatial, bitwise and encoding functions. They are available in every RDF4J repository (memory, native, LMDB, remote through the RDF4J Server) without configuration.
<!--more-->

All functions follow SPARQL 1.1 semantics where relevant: string functions accept simple, `xsd:string` and language-tagged literals and keep the language tag of their input (like `UCASE` or `SUBSTR`), positions and lengths count Unicode code points (like `STRLEN`), numeric functions apply XPath type promotion, and errors are SPARQL evaluation errors, so a failing `BIND` leaves the variable unbound and a failing `FILTER` is false.

Each family has its own namespace:

| family | prefix | namespace |
|---|---|---|
| math | `mathf` | `http://rdf4j.org/function/math#` |
| string | `strf` | `http://rdf4j.org/function/string#` |
| date/time | `dtf` | `http://rdf4j.org/function/datetime#` |
| aggregates | `agg` | `http://rdf4j.org/aggregate#` |
| window functions | `winf` | `http://rdf4j.org/function/window#` |
| text similarity | `textf` | `http://rdf4j.org/function/text#` |
| JSON | `jsonf` | `http://rdf4j.org/function/json#` |
| arrays | `arrf` | `http://rdf4j.org/function/array#` |
| geospatial | `geox` | `http://rdf4j.org/function/geo#` |
| bitwise | `bitf` | `http://rdf4j.org/function/bitwise#` |
| encoding and hashing | `encf` | `http://rdf4j.org/function/encoding#` |
| miscellaneous | `miscf` | `http://rdf4j.org/function/misc#` |

The Java constants live in `org.eclipse.rdf4j.model.vocabulary` (`MATHF`, `STRF`, `DTF`, `AGGF`, `WINF`, `TEXTF`, `JSONF`, `ARRAYF`, `GEOX`, `BITF`, `ENCF`, `MISCF`).

## Mathematical functions

`mathf:sqrt(x)`, `cbrt(x)`, `pow(x, y)`, `exp(x)`, `ln(x)`, `log10(x)`, `log2(x)`, `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2(y, x)`, `hypot(x, y)` and `pi()` return `xsd:double`, following the XPath 3 `math:` functions (which are also registered under `http://www.w3.org/2005/xpath-functions/math#`). `sign(x)` returns -1, 0 or 1. `round(x, n)` and `trunc(x, n)` keep the datatype of `x` and round to `n` decimals (negative `n` rounds to tens, hundreds, ...); `round` rounds ties towards positive infinity like SPARQL `ROUND`. `mod(x, y)` follows XPath numeric promotion with the dividend's sign. `greatest(...)` and `least(...)` pick the extreme argument under SPARQL ordering. `decimalFormat(x, pattern [, locale])` formats with a `java.text.DecimalFormat` pattern.

## String functions

`strf:trim`, `ltrim`, `rtrim` (optional character set), `lpad(s, n [, fill])`, `rpad`, `repeat(s, n)`, `reverse`, `left(s, n)`, `right(s, n)`, `position(needle, haystack)` (1-based, 0 when absent), `lastIndexOf(s, needle)`, `splitPart(s, delimiter, n)` (negative `n` counts from the end), `split(s, delimiter)` (an `rdf:JSON` array), `concatWs(separator, ...)`, `translate(s, from, to)`, `initCap(s)`, `ascii(s)`, `chr(n)`, `normalize(s [, form])` (also `fn:normalize-unicode`), `regexpExtract(s, pattern [, group] [, flags])` and `regexpExtractAll(...)` (an `rdf:JSON` array).

## Date and time functions

`dtf:dateTrunc(unit, dt)`, `dateBin(stride, dt [, origin])`, `dateDiff(unit, start, end)`, `dateAdd(dt, duration)`, `dayOfWeek` (ISO, Monday = 1), `dayOfYear`, `week` (ISO), `quarter`, `epoch` (seconds as `xsd:decimal`), `epochMillis`, `fromUnixtime(seconds)`, `fromEpochMillis(millis)`, `dateFormat(dt, pattern [, locale])`, `parseDate(s, pattern [, locale])`, `makeDate(y, m, d)`, `age(later, earlier)` (an `xsd:duration` with calendar components) and `atTimeZone(dt, zone)`. Inputs are `xsd:dateTime`, `xsd:dateTimeStamp`, `xsd:date` or `xsd:time`; results keep the datatype and timezone of their input. Durations are `xsd:duration` literals (`"P1M"`, `"PT15M"`); patterns are `java.time` patterns.

## Aggregates

Used like any aggregate, with optional `DISTINCT`:

    PREFIX agg: <http://rdf4j.org/aggregate#>
    SELECT ?dept (agg:median(?salary) AS ?median) (agg:percentileCont(?salary, 0.9) AS ?p90)
           (agg:argMax(?employee, ?salary) AS ?topEarner)
    WHERE { ?employee :dept ?dept ; :salary ?salary } GROUP BY ?dept

Unary: `agg:median`, `mode`, `countIf(condition)`, `boolAnd`, `boolOr`, `first`, `last`, `approxCountDistinct` (HyperLogLog), `histogram` (an `rdf:JSON` array of `{"value", "count"}` objects) and `arrayAgg` (an `rdf:JSON` array). Two-argument: `percentileCont(x, p)`, `percentileDisc(x, p)`, `approxQuantile(x, p)` (KLL sketch), `corr(x, y)`, `covarPop(x, y)`, `covarSamp(x, y)`, `argMin(value, key)` and `argMax(value, key)`. Medians and percentiles are exact for integer and decimal input; empty groups leave the aggregate unbound (`countIf` yields 0).

## Window functions

Window functions are written as ordinary function calls in `SELECT` expressions, with the window specification passed as marker arguments: `winf:partitionBy(...)`, `winf:orderBy(...)` (keys optionally wrapped in `winf:asc(...)`/`winf:desc(...)`) and an optional frame `winf:rows(start, end)` or `winf:range(start, end)` whose bounds are integer row offsets (negative = preceding), `"unbounded"` or `"current"`.

    PREFIX winf: <http://rdf4j.org/function/window#>
    SELECT ?employee ?salary
           (winf:rank(winf:partitionBy(?dept), winf:orderBy(winf:desc(?salary))) AS ?rank)
           (winf:sum(?salary, winf:partitionBy(?dept), winf:orderBy(?salary)) AS ?running)
           (winf:avg(?salary, winf:orderBy(?date), winf:rows(-6, 0)) AS ?weeklyAverage)
    WHERE { ?employee :dept ?dept ; :salary ?salary ; :hired ?date }

Available: `rowNumber`, `rank`, `denseRank`, `ntile(n)`, `lag(x [, offset [, default]])`, `lead(...)`, `firstValue(x)`, `lastValue(x)`, `nthValue(x, n)`, `percentRank`, `cumeDist`, `sum(x)`, `avg(x)`, `count(x)`, `min(x)`, `max(x)`. Without a frame, an ordered window covers the partition start up to the current row and its peers; an unordered window covers the whole partition. Window functions are evaluated after `GROUP BY`/`HAVING` (their inputs may be aggregates) and before `ORDER BY`, so results can be sorted and, inside a sub-select, filtered.

## Text search and similarity

`textf:levenshtein(a, b)`, `damerauLevenshtein(a, b)`, `jaroSimilarity(a, b)`, `jaroWinkler(a, b [, scaling])`, `soundex(s)`, `metaphone(s [, maxLength])`, `trigramSimilarity(a, b)` (PostgreSQL `pg_trgm` semantics), `ilike(s, pattern [, escape])` (case-insensitive SQL `LIKE`), `tokenize(s)` and `ngrams(s, n)` (`rdf:JSON` arrays).

## JSON and arrays

JSON values are `rdf:JSON` literals; string literals holding JSON text are accepted as input. Paths use a JSONPath subset: `$`, `.name`, `['name']`, `[n]` (negative from the end), `[*]`, `..name`.

`jsonf:jsonValue(json, path)` (one scalar as a typed literal), `jsonQuery(json, path)` (`rdf:JSON`, several matches are wrapped in an array), `jsonExists`, `jsonKeys(json [, path])`, `jsonArrayLength(json [, path])`, `jsonObject(k1, v1, ...)`, `jsonArray(...)`, `jsonSet(json, path, value)`.

Arrays are `rdf:JSON` arrays. RDF terms are stored losslessly: plain strings, numbers and booleans as JSON primitives, IRIs as `{"@id": ...}` and other literals as `{"@value": ..., "@type"|"@language": ...}`. `arrf:arrayContains(array, x)`, `arrf:arrayLength(array)` and `arrf:generateSeries(start, stop [, step])` are scalar functions.

### Set-returning functions (magic properties)

Functions that produce several rows are invoked as *magic properties*: the arguments are the subject (a single term or an RDF collection), the results the object:

    ?array arrf:unnest ?element .                 # one solution per element
    ?array arrf:unnestWithOrdinality (?element ?index) .
    (1 10 2) arrf:generateSeries (?n) .
    (?text ",") strf:split (?part) .
    (?text "[0-9]+") strf:regexpExtractAll (?match) .
    ?text textf:tokenize ?token .
    (?text 3) textf:ngrams (?gram) .

The RDF4J query optimizer rewrites such patterns into tuple function calls evaluated once per solution of the surrounding group; argument variables must be bound by that group (or by the enclosing query).

## Geospatial functions

In addition to the GeoSPARQL functions of the `rdf4j-queryalgebra-geosparql` module, that module registers `geox:area(geometry [, uom:metre])`, `length(geometry [, uom:metre])`, `centroid`, `transform(geometry, crs)` (between CRS84, EPSG:4326 and EPSG:3857), `simplify(geometry, tolerance)`, `makePoint(x, y [, crs])`, `asGeoJSON(geometry)` (an `rdf:JSON` literal) and `closestPoint(a, b)`. Geometries are `geo:wktLiteral`s, optionally prefixed with a `<crs>` IRI.

## Bitwise, encoding and miscellaneous functions

`bitf:bitAnd`, `bitOr`, `bitXor`, `bitNot`, `bitCount`, `bitShiftLeft`, `bitShiftRight` operate on `xsd:integer` values with arbitrary precision. `encf:base64Encode`, `base64Decode`, `toHex`, `fromHex`, `crc32`, `xxhash64(s [, seed])` and `hmac(message, key [, algorithm])` accept strings (UTF-8), `xsd:hexBinary` and `xsd:base64Binary`. `miscf:nullIf(a, b)` returns `a` unless `a = b`, in which case it raises an error, leaving a `BIND` variable unbound (the SPARQL counterpart of SQL `NULL`).
