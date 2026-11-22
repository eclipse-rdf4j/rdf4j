---
title: "SPARQL extension functions"
toc: true
weight: 9
autonumbering: true
---

RDF4J extends SPARQL with mathematical, string, date/time, aggregate, window, text-similarity, JSON, array, geospatial, bitwise and encoding operations. This reference gives their invocation syntax, result types, defaults and boundary behavior, and distinguishes **functions** from **magic properties**.
<!--more-->

## Functions and magic properties

A **scalar function** is an expression that returns one RDF term for its arguments. A **magic property** is a predicate in a graph pattern that computes zero or more solution mappings, potentially binding several result variables. Its input and output lists describe a computation; they do not require matching list triples in the repository.

<div class="table-responsive">

| Kind | Where and how it is used | Result |
|---|---|---|
| **Function** | An expression such as `BIND(strf:split(?text, ",") AS ?parts)`. Also usable in other expression positions that allow scalar function calls. | One RDF term; an `rdf:JSON` array is still one term. An evaluation error leaves a `BIND` target unbound. |
| **Magic property** | A graph pattern such as `(?text ",") strf:split ?part .` | Zero or more solution mappings; here, one binding of `?part` per field. |
| **Function + magic property** | The same IRI is registered for both of the forms above. | Function-call syntax produces one array-valued term; predicate syntax expands the elements into rows. |
| **Aggregate** | An aggregate expression such as `(agg:median(?x) AS ?median)`, optionally with `DISTINCT`. | One value per group. |
| **Window function** | A `SELECT` expression such as `(winf:rowNumber(winf:orderBy(?x)) AS ?row)`. | One value per input solution, computed using its partition and frame. |
| **Window marker** | An argument such as `winf:orderBy(?x)` inside a window function. | Describes the window; it is not a standalone value-producing function. |

</div>

The [magic-property reference](#set-returning-functions-magic-properties) lists every supported row-producing form. In particular, `arrf:unnest` and `arrf:unnestWithOrdinality` are **magic properties only**: they cannot be called as `BIND(arrf:unnest(...) AS ?x)`.

### Availability

The scalar and tuple implementations are registered automatically by the `rdf4j-queryalgebra-evaluation` module. Aggregates and window expressions also rely on RDF4J's SPARQL parser and evaluator. The `geox:` functions require the additional `rdf4j-queryalgebra-geosparql` module. For a remote query, support depends on the server evaluating that query; adding a library to a client does not add these extensions to an arbitrary SPARQL endpoint.

### Minimum RDF4J versions

The **Minimum RDF4J** column gives the first release containing that invocation form, including milestone releases. `6.2.0-M2` means milestone 2 of the 6.2.0 release line; it must not be read as `6.2.0-M1`, or as a claim that a final 6.2.0 release is available. The version applies to both forms when an entry is marked **Function + magic property**.

<div class="table-responsive">

| Operations | Minimum RDF4J | Availability |
|---|---|---|
| `agg:stdev`, `agg:stdev_population`, `agg:variance`, `agg:variance_population` | **4.2.0** | Introduced with [custom aggregate support in 4.2.0](/release-notes/4.2.0/). |
| All other RDF4J extension functions and window markers documented here | **6.2.0-M2** | Introduced by [the extension-function implementation](https://github.com/eclipse-rdf4j/rdf4j/commit/7f1f2d9dbb48f434e6d02c89549878575d238370) and first included in the [6.2.0-M2 release tag](https://github.com/eclipse-rdf4j/rdf4j/tree/6.2.0-M2). Absent from 6.1.0 and 6.2.0-M1. |
| All seven magic properties documented here | **6.2.0-M2** | Present in the [6.2.0-M2 tuple-function registrations](https://github.com/eclipse-rdf4j/rdf4j/blob/6.2.0-M2/core/queryalgebra/evaluation/src/main/resources/META-INF/services/org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction). The five dual-use IRIs gained both forms in this release. |
| The XPath math aliases and `fn:normalize-unicode` alias documented here | **6.2.0-M2** | Available under their XPath IRIs as well as the RDF4J forms indicated below. |

</div>

### Reading the signatures

In the tables, square brackets denote optional arguments and `...` denotes repeated arguments; neither is literal SPARQL syntax. Function names and IRIs are case-sensitive. A **string** means a simple, `xsd:string` or language-tagged literal unless a row states otherwise. A **numeric** argument must be a well-formed literal of an XSD numeric datatype; an **integer** argument must have `xsd:integer` or an integer-derived datatype, so the string `"2"` and the decimal `2.0` are not integer arguments. Unicode lengths and positions count code points, not UTF-16 code units or grapheme clusters.

Unless a function defines a different outcome, wrong argument counts, wrong types and unbound arguments are evaluation errors. A failed scalar expression in `BIND` leaves its target unbound while retaining the solution; an error evaluating a `FILTER` expression rejects that solution. Aggregates, window functions and magic properties have the more specific error behavior described in their sections.

### Namespaces

Prefixes are abbreviations chosen by the query author. Declare the namespaces you use with `PREFIX`; for example, `PREFIX strf: <http://rdf4j.org/function/string#>`.

<div class="table-responsive">

| Family | Prefix | Namespace | Java vocabulary |
|---|---|---|---|
| Mathematics | `mathf` | `http://rdf4j.org/function/math#` | `MATHF` |
| Strings | `strf` | `http://rdf4j.org/function/string#` | `STRF` |
| Date and time | `dtf` | `http://rdf4j.org/function/datetime#` | `DTF` |
| Aggregates | `agg` | `http://rdf4j.org/aggregate#` | `AGGF` |
| Windows | `winf` | `http://rdf4j.org/function/window#` | `WINF` |
| Text similarity | `textf` | `http://rdf4j.org/function/text#` | `TEXTF` |
| JSON | `jsonf` | `http://rdf4j.org/function/json#` | `JSONF` |
| Arrays | `arrf` | `http://rdf4j.org/function/array#` | `ARRAYF` |
| Geospatial extensions | `geox` | `http://rdf4j.org/function/geo#` | `GEOX` |
| Bitwise operations | `bitf` | `http://rdf4j.org/function/bitwise#` | `BITF` |
| Encoding and hashing | `encf` | `http://rdf4j.org/function/encoding#` | `ENCF` |
| Miscellaneous | `miscf` | `http://rdf4j.org/function/misc#` | `MISCF` |

</div>

These Java vocabulary classes are in `org.eclipse.rdf4j.model.vocabulary` and require **RDF4J 6.2.0-M2**, including `AGGF`. The four statistical aggregate IRIs can nevertheless be used directly in SPARQL from 4.2.0. Other prefixes used below are `xsd: <http://www.w3.org/2001/XMLSchema#>`, `rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>`, `geo: <http://www.opengis.net/ont/geosparql#>` and `uom: <http://www.opengis.net/def/uom/OGC/1.0/>`.

## Mathematical functions

All entries in this section are **scalar functions**. Numeric arguments must be well-formed XSD numeric literals; a string such as `"2"` is not implicitly converted to a number. Trigonometric functions use radians. Functions returning `xsd:double` use floating-point arithmetic and can return `NaN`, `INF`, or `-INF`; a result outside the real-number domain is not necessarily an evaluation error.

<div class="table-responsive">

| Invocation | Kind | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- | --- |
| `mathf:sqrt(x)` | Function | 6.2.0-M2 | `xsd:double` | Square root of `x`; negative finite inputs produce `NaN`. |
| `mathf:cbrt(x)` | Function | 6.2.0-M2 | `xsd:double` | Real cube root of `x`, including negative inputs. |
| `mathf:pow(x, y)` | Function | 6.2.0-M2 | `xsd:double` | `x` raised to `y`; integer exponents retain their odd/even distinction even when too large to represent exactly as a `double`. |
| `mathf:exp(x)` | Function | 6.2.0-M2 | `xsd:double` | Euler's number raised to `x`. |
| `mathf:ln(x)` | Function | 6.2.0-M2 | `xsd:double` | Natural logarithm; zero produces `-INF` and negative inputs produce `NaN`. |
| `mathf:log10(x)` | Function | 6.2.0-M2 | `xsd:double` | Base-10 logarithm. |
| `mathf:log2(x)` | Function | 6.2.0-M2 | `xsd:double` | Base-2 logarithm. |
| `mathf:sin(x)` | Function | 6.2.0-M2 | `xsd:double` | Sine of `x`. |
| `mathf:cos(x)` | Function | 6.2.0-M2 | `xsd:double` | Cosine of `x`. |
| `mathf:tan(x)` | Function | 6.2.0-M2 | `xsd:double` | Tangent of `x`. |
| `mathf:asin(x)` | Function | 6.2.0-M2 | `xsd:double` | Inverse sine in `[-π/2, π/2]`; inputs outside `[-1, 1]` produce `NaN`. |
| `mathf:acos(x)` | Function | 6.2.0-M2 | `xsd:double` | Inverse cosine in `[0, π]`; inputs outside `[-1, 1]` produce `NaN`. |
| `mathf:atan(x)` | Function | 6.2.0-M2 | `xsd:double` | Inverse tangent in `[-π/2, π/2]`. |
| `mathf:atan2(y, x)` | Function | 6.2.0-M2 | `xsd:double` | Angle of the point `(x, y)` in `[-π, π]`, with the quadrant determined by both arguments. The first argument is `y`. |
| `mathf:hypot(x, y)` | Function | 6.2.0-M2 | `xsd:double` | Euclidean norm, mathematically `sqrt(x² + y²)`, calculated without intermediate overflow or underflow. |
| `mathf:pi()` | Function | 6.2.0-M2 | `xsd:double` | The floating-point approximation of π. |
| `mathf:sign(x)` | Function | 6.2.0-M2 | `xsd:integer` | `-1`, `0`, or `1` according to the sign of `x`. Both signed zeros produce `0`; `NaN` is an evaluation error. |
| `mathf:round(x [, n])` | Function | 6.2.0-M2 | Numeric literal | Rounds to `n` decimal places, default `0`; ties go towards positive infinity, so `2.5` becomes `3` and `-2.5` becomes `-2`. Negative `n` rounds to tens, hundreds, and so on. |
| `mathf:trunc(x [, n])` | Function | 6.2.0-M2 | Numeric literal | Truncates towards zero to `n` decimal places, default `0`. Negative `n` truncates to tens, hundreds, and so on. |
| `mathf:mod(x, y)` | Function | 6.2.0-M2 | Promoted numeric datatype | Remainder of division, with the dividend's sign. Promotion order is integer → decimal → float → double. A zero divisor is an error for integer/decimal results and produces `NaN` for float/double results. |
| `mathf:greatest(value, ...)` | Function | 6.2.0-M2 | One input RDF term | Greatest argument under RDF4J's SPARQL `ORDER BY` comparison. Requires at least one argument; ties retain the first encountered term. |
| `mathf:least(value, ...)` | Function | 6.2.0-M2 | One input RDF term | Least argument under the same ordering. Requires at least one argument; ties retain the first encountered term. |
| `mathf:decimalFormat(x, pattern [, locale])` | Function | 6.2.0-M2 | `xsd:string` | Formats `x` using a `java.text.DecimalFormat` pattern, such as `"#,##0.00"`. `locale` is a BCP 47 language tag; the default is `Locale.ROOT`. Invalid patterns are errors. |

</div>

For `mathf:round` and `mathf:trunc`, `n` must be an integer in `[-1000, 1000]`. The result preserves the numeric category (`xsd:integer`, `xsd:decimal`, `xsd:float`, or `xsd:double`), rather than necessarily preserving a derived integer datatype: an integer input with nonnegative `n` is returned unchanged; rounding an integer at a negative scale returns `xsd:integer`. Floating-point `NaN` and infinities are returned unchanged. Finite floats and doubles are rounded through their shortest decimal representation.

### XPath mathematical aliases

With `PREFIX xmath: <http://www.w3.org/2005/xpath-functions/math#>`, the following **scalar functions** are also available. Only the listed names are registered in this namespace; other `mathf:` functions do not automatically acquire XPath aliases.

<div class="table-responsive">

| Invocation | Minimum RDF4J | Equivalent or definition |
|---|---|---|
| `xmath:pi()` | 6.2.0-M2 | `mathf:pi()` |
| `xmath:sqrt(x)` | 6.2.0-M2 | `mathf:sqrt(x)` |
| `xmath:pow(x, y)` | 6.2.0-M2 | `mathf:pow(x, y)` |
| `xmath:exp(x)` | 6.2.0-M2 | `mathf:exp(x)` |
| `xmath:exp10(x)` | 6.2.0-M2 | `10` raised to `x`, as `xsd:double`. |
| `xmath:log(x)` | 6.2.0-M2 | `mathf:ln(x)` |
| `xmath:log10(x)` | 6.2.0-M2 | `mathf:log10(x)` |
| `xmath:sin(x)` | 6.2.0-M2 | `mathf:sin(x)` |
| `xmath:cos(x)` | 6.2.0-M2 | `mathf:cos(x)` |
| `xmath:tan(x)` | 6.2.0-M2 | `mathf:tan(x)` |
| `xmath:asin(x)` | 6.2.0-M2 | `mathf:asin(x)` |
| `xmath:acos(x)` | 6.2.0-M2 | `mathf:acos(x)` |
| `xmath:atan(x)` | 6.2.0-M2 | `mathf:atan(x)` |
| `xmath:atan2(y, x)` | 6.2.0-M2 | `mathf:atan2(y, x)` |

</div>

## String functions

String inputs accept simple literals, `xsd:string`, and language-tagged string literals. **Same kind as `s`** means the result retains the input's language tag and base direction when present; otherwise it is an `xsd:string`. Lengths and positions count Unicode code points, rather than UTF-16 code units or user-perceived grapheme clusters. Integer arguments accept XSD integer datatypes, including derived types; numerically integral decimal and floating-point literals are not accepted in their place.

<div class="table-responsive">

| Invocation | Kind | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- | --- |
| `strf:trim(s [, chars])` | Function | 6.2.0-M2 | Same kind as `s` | Removes code points from both ends. By default, removes Java `Character.isWhitespace` characters; when `chars` is supplied, removes any code point present in that string. An empty `chars` removes nothing. |
| `strf:ltrim(s [, chars])` | Function | 6.2.0-M2 | Same kind as `s` | As `strf:trim`, but only at the beginning. |
| `strf:rtrim(s [, chars])` | Function | 6.2.0-M2 | Same kind as `s` | As `strf:trim`, but only at the end. |
| `strf:lpad(s, n [, fill])` | Function | 6.2.0-M2 | Same kind as `s` | Pads on the left to `n` code points by repeating `fill`, default one space. If `s` is longer, retains its first `n` code points. `n ≤ 0` gives an empty string; empty `fill` leaves a shorter input unchanged. |
| `strf:rpad(s, n [, fill])` | Function | 6.2.0-M2 | Same kind as `s` | As `strf:lpad`, but pads on the right. Truncation still retains the first `n` code points. |
| `strf:repeat(s, n)` | Function | 6.2.0-M2 | Same kind as `s` | Repeats `s` `n` times; `n ≤ 0` gives an empty string. |
| `strf:reverse(s)` | Function | 6.2.0-M2 | Same kind as `s` | Reverses code-point order, keeping surrogate pairs intact. Combining marks are not kept together as grapheme clusters. |
| `strf:left(s, n)` | Function | 6.2.0-M2 | Same kind as `s` | First `n` code points; negative `n` removes the last `-n` code points. A requested length beyond the input is clamped to its length. |
| `strf:right(s, n)` | Function | 6.2.0-M2 | Same kind as `s` | Last `n` code points; negative `n` removes the first `-n` code points. A requested length beyond the input is clamped to its length. |
| `strf:position(needle, haystack)` | Function | 6.2.0-M2 | `xsd:integer` | 1-based position of the first occurrence, or `0` if absent. An empty needle occurs at position `1`. Arguments must satisfy SPARQL string compatibility, as for `CONTAINS(haystack, needle)`. |
| `strf:lastIndexOf(haystack, needle)` | Function | 6.2.0-M2 | `xsd:integer` | 1-based position of the last occurrence, or `0` if absent. An empty needle occurs at one past the input's code-point length. Uses the same compatibility rule as `strf:position`. |
| `strf:splitPart(s, delimiter, n)` | Function | 6.2.0-M2 | Same kind as `s` | Selects the 1-based field after splitting on a literal delimiter; negative `n` counts from the end. `n = 0` is an error; an out-of-range field is empty. An empty delimiter treats the whole input as one field. |
| `strf:split(s, delimiter)` | **[Function + magic property](#set-returning-functions-magic-properties)** | 6.2.0-M2 | `rdf:JSON` array | Splits on a literal delimiter, preserving empty fields, including a trailing empty field. An empty delimiter splits into individual code points. Element encodings preserve the input's language and direction. |
| `strf:concatWs(separator, ...)` | Function | 6.2.0-M2 | String literal | Joins zero or more strings with `separator` between them. The separator is required; a separator alone returns `""`. Language and direction are retained only when the separator and all joined strings share them. Unbound arguments are errors, not skipped values. |
| `strf:translate(s, from, to)` | Function | 6.2.0-M2 | Same kind as `s` | Replaces each code point found in `from` with the corresponding code point in `to`; removes it when no corresponding position exists in `to`. If `from` repeats a code point, its first position is used. |
| `strf:initCap(s)` | Function | 6.2.0-M2 | Same kind as `s` | Within each maximal sequence of letters and digits, title-cases the first code point and lowercases the rest. Other code points delimit words and remain unchanged. |
| `strf:ascii(s)` | Function | 6.2.0-M2 | `xsd:integer` | Unicode code point of the first character, including non-ASCII characters; `0` for an empty string. |
| `strf:chr(n)` | Function | 6.2.0-M2 | `xsd:string` | String containing code point `n`. Accepts `1` through `0x10FFFF`, excluding the surrogate range `0xD800`–`0xDFFF`; zero and other values are errors. |
| `strf:normalize(s [, form])` | Function | 6.2.0-M2 | Same kind as `s` | Unicode normalization in `NFC` (default), `NFD`, `NFKC`, or `NFKD`. The form is trimmed and case-insensitive; an empty form returns the input unchanged. Also registered as `fn:normalize-unicode` in `http://www.w3.org/2005/xpath-functions#`. |
| `strf:regexpExtract(s, pattern [, group] [, flags])` | Function | 6.2.0-M2 | Same kind as `s` | Selected capture from the first regex match. `group` defaults to `0` (the whole match). No match or a nonparticipating optional group returns an empty string. |
| `strf:regexpExtractAll(s, pattern [, group] [, flags])` | **[Function + magic property](#set-returning-functions-magic-properties)** | 6.2.0-M2 | `rdf:JSON` array | Selected capture from every successive regex match, in match order. No matches produce `[]`; a nonparticipating optional group contributes an empty string. Elements preserve the input's language and direction. |

</div>

For the two regex functions, the integer `group` and literal `flags` may be supplied independently or in either order, but at most once each. A nonexistent or negative capture group is an error. Patterns use Java regular-expression syntax. Supported flag characters are `s` (dot matches newlines), `m` (multiline anchors), `i` (Unicode-aware case-insensitive matching), `x` (ignore pattern whitespace and permit comments), `q` (literal pattern), `d` (Unix line endings), and `u` (Unicode case handling; does not enable case-insensitive matching by itself). Patterns and flags are read from literal lexical forms; invalid patterns or flags are errors.

String counts and indices must fit a signed 32-bit integer, except `strf:repeat` and `strf:chr`, which accept signed 64-bit integers. Padding targets above `2^26` code points are errors. Repetition that would allocate more than `2^26` UTF-16 code units is also an error.

## Date and time functions

All entries below are **scalar functions**. A temporal argument `dt` must be an `xsd:dateTime`, `xsd:dateTimeStamp`, `xsd:date`, or `xsd:time` literal; a string containing a date is not automatically parsed. Functions marked **date required** reject `xsd:time`. Fractional seconds are processed to nanosecond precision; digits after the ninth decimal place are discarded.

<div class="table-responsive">

| Invocation | Kind | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- | --- |
| `dtf:dateTrunc(unit, dt)` | Function | 6.2.0-M2 | Same temporal datatype and offset as `dt` | Truncates the local calendar/clock value to the start of `unit`. Weeks start on Monday; decades start at years divisible by 10; centuries and millennia start in years ending in `01` and `001`, respectively. |
| `dtf:dateBin(stride, dt [, origin])` | Function | 6.2.0-M2 | Same temporal datatype and offset as `dt` | Start of the interval containing `dt`, measured from `origin`, default `2000-01-01T00:00:00`. `stride` must be positive and contain either months/years or days/time, not both. Binning uses local fields; offsets on `dt` and `origin` do not shift the bin boundaries. |
| `dtf:dateDiff(unit, start, end)` | Function | 6.2.0-M2 | `xsd:integer` | Number of complete units from `start` to `end`, truncated towards zero. It is negative when `end` precedes `start`. If either input has an offset, calculations account for offsets and treat an offset-free input as UTC; otherwise they compare local values. |
| `dtf:dateAdd(dt, duration)` | Function | 6.2.0-M2 | Same temporal datatype and offset as `dt` | Applies years/months first, clamping the day to the target month's last valid day, then adds days/time. Negative durations subtract. For a date, arithmetic starts at midnight and returns the resulting date; for a time, returns only the resulting time of day. |
| `dtf:dayOfWeek(dt)` | Function | 6.2.0-M2 | `xsd:integer` | ISO weekday: Monday `1` through Sunday `7`. Date required. |
| `dtf:dayOfYear(dt)` | Function | 6.2.0-M2 | `xsd:integer` | Day within the calendar year: `1` through `365`, or `366` in a leap year. Date required. |
| `dtf:week(dt)` | Function | 6.2.0-M2 | `xsd:integer` | ISO week-of-week-based-year: `1` through `53`. The week-based year can differ from the calendar year near New Year. Date required. |
| `dtf:quarter(dt)` | Function | 6.2.0-M2 | `xsd:integer` | Calendar quarter: January–March `1`, April–June `2`, July–September `3`, October–December `4`. Date required. |
| `dtf:epoch(dt)` | Function | 6.2.0-M2 | `xsd:decimal` | Seconds since `1970-01-01T00:00:00Z`, including fractional seconds up to nanosecond precision. Missing offsets are interpreted as UTC. Date required. |
| `dtf:epochMillis(dt)` | Function | 6.2.0-M2 | `xsd:integer` | Milliseconds since the same epoch, rounded down when fractional milliseconds remain. Missing offsets are interpreted as UTC. Date required. |
| `dtf:fromUnixtime(seconds)` | Function | 6.2.0-M2 | UTC `xsd:dateTime` | Date/time for a finite numeric count of seconds since the epoch, including fractional seconds to nanosecond precision. |
| `dtf:fromEpochMillis(millis)` | Function | 6.2.0-M2 | UTC `xsd:dateTime` | Date/time for a signed 64-bit integer count of milliseconds since the epoch, subject to the supported date/time range. |
| `dtf:dateFormat(dt, pattern [, locale])` | Function | 6.2.0-M2 | `xsd:string` | Formats with a `java.time.format.DateTimeFormatter` pattern. `locale` is a BCP 47 language tag; the default is `Locale.ROOT`. Unavailable fields or invalid patterns are errors. |
| `dtf:parseDate(s, pattern [, locale])` | Function | 6.2.0-M2 | `xsd:dateTime`, `xsd:date`, or `xsd:time` | Strictly parses a string using a `DateTimeFormatter` pattern. The parsed fields determine the result datatype; a parsed offset or zone supplies an offset. Impossible dates and times are errors. The default locale is `Locale.ROOT`. |
| `dtf:makeDate(year, month, day)` | Function | 6.2.0-M2 | `xsd:date`, no offset | Constructs a date from integer components. The month is `1`–`12`; the day must exist in that month and year. Invalid or out-of-range dates are errors. |
| `dtf:age(later, earlier)` | Function | 6.2.0-M2 | `xsd:duration` | Calendar difference with years, months, days, and time components; negative when `later` precedes `earlier`. If either input has an offset, first aligns both to the first argument's offset, treating a missing offset as UTC. |
| `dtf:atTimeZone(dt, zone)` | Function | 6.2.0-M2 | `xsd:dateTime`, or `xsd:time` for a time input | With an input offset, converts the same instant into `zone`. Without one, interprets the local value in `zone`. `zone` accepts a region such as `"Europe/Oslo"` or a fixed offset such as `"+02:00"`. The result carries the resolved offset, not the region name. |

</div>

`dtf:dateTrunc` and `dtf:dateDiff` recognize `millennium`, `century`, `decade`, `year`, `quarter`, `month`, `week`, `day`, `hour`, `minute`, `second`, `millisecond`, `microsecond`, and `nanosecond`. Names are trimmed, case-insensitive, and accept plural forms. Supported abbreviations are `y`, `mon`, `w`, `d`, `h`, `min`/`m`, `sec`, `milli`/`ms`, `micro`/`u`, and `nano`/`n`. Unsupported units are errors. A date uses midnight when a time is needed; a time uses `1970-01-01` when a date is needed internally. A time truncation that moves that implicit date into another year is rejected.

Durations accept `xsd:duration`, `xsd:yearMonthDuration`, `xsd:dayTimeDuration`, or `xsd:string` literals containing valid XSD duration syntax. For example, `"P1M"^^xsd:duration` is one calendar month and `"PT15M"^^xsd:duration` is fifteen minutes. A language-tagged duration string is not accepted. Arithmetic keeps a literal's fixed offset; `dtf:dateAdd` does not apply regional daylight-saving transitions. `dtf:atTimeZone` does apply the requested region's rules: an offset-free time in a daylight-saving gap moves forward by the gap, and an ambiguous time uses the earlier offset.

For `dtf:parseDate`, an unquoted `y` in a pattern without an era marker `G` is interpreted as the proleptic year `u`, so `"yyyy-MM-dd"` works with strict date resolution. `dtf:atTimeZone` promotes date inputs to date-times and returns `xsd:dateTime` for `xsd:dateTimeStamp` inputs. Arithmetic outside the implementation's supported temporal or integer range is an evaluation error.

## Aggregates

**Kind: aggregate.** These calls calculate one value per solution group; they are not scalar functions or magic properties. Use them in aggregate expression positions such as `SELECT` and `HAVING`, with the usual SPARQL grouping rules. The signatures below show all required arguments. Every aggregate accepts an optional `DISTINCT` before its first argument, for example `agg:median(DISTINCT ?salary)` or `agg:corr(DISTINCT ?x, ?y)`.

`DISTINCT` compares RDF terms; for a two-argument aggregate it compares the complete argument pair. Thus `agg:countIf(DISTINCT ?condition)` counts distinct condition values with a true effective boolean value, rather than counting all rows for which the condition is true.

<div class="table-responsive">

| Invocation | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- |
| `agg:median(x)` | 6.2.0-M2 | `xsd:decimal` or `xsd:double` | Median of the numeric values: the middle value for an odd count, or the arithmetic mean of the two middle values for an even count. Equivalent to `agg:percentileCont(x, 0.5)`. |
| `agg:percentileCont(x, p)` | 6.2.0-M2 | `xsd:decimal` or `xsd:double` | Continuous percentile of the numeric values. For `N` sorted values, linearly interpolate at zero-based position `p * (N - 1)`. |
| `agg:percentileDisc(x, p)` | 6.2.0-M2 | An input numeric literal | Discrete percentile: return the sorted input value at one-based position `max(1, ceil(p * N))`, preserving its datatype. |
| `agg:approxQuantile(x, p)` | 6.2.0-M2 | `xsd:double` | Approximate numeric quantile using a KLL sketch. Values are converted to double precision; this does not provide the exact-decimal behavior of the exact percentiles. |
| `agg:mode(x)` | 6.2.0-M2 | An input RDF term | Most frequent RDF term, using RDF term equality. Ties keep the term encountered first. |
| `agg:countIf(condition)` | 6.2.0-M2 | `xsd:integer` | Number of input values whose SPARQL effective boolean value is true. A value without an effective boolean value is not counted. |
| `agg:boolAnd(x)` | 6.2.0-M2 | `xsd:boolean` | True if every contributing value has a true effective boolean value. A bound value without an effective boolean value makes the aggregate result unbound. |
| `agg:boolOr(x)` | 6.2.0-M2 | `xsd:boolean` | True if any contributing value has a true effective boolean value. A bound value without an effective boolean value makes the aggregate result unbound, even when another value is true. |
| `agg:first(x)` | 6.2.0-M2 | An input RDF term | First bound, successfully evaluated value in the group's evaluation order. |
| `agg:last(x)` | 6.2.0-M2 | An input RDF term | Last bound, successfully evaluated value in the group's evaluation order. |
| `agg:approxCountDistinct(x)` | 6.2.0-M2 | `xsd:integer` | Rounded HyperLogLog estimate of the number of distinct RDF terms, using a sketch with `lgK = 12`. This is an estimate, not an exact count. |
| `agg:histogram(x)` | 6.2.0-M2 | `rdf:JSON` array | One `{"value": ..., "count": n}` object per distinct RDF term. Buckets are sorted by SPARQL value ordering; values use the RDF-term JSON encoding described below. |
| `agg:arrayAgg(x)` | 6.2.0-M2 | `rdf:JSON` array | Contributing values in evaluation order, including duplicates unless `DISTINCT` is used. Values use the RDF-term JSON encoding described below; expand the result with the `arrf:unnest` magic property. |
| `agg:corr(x, y)` | 6.2.0-M2 | `xsd:double` | Pearson correlation of the numeric pairs. Unbound with fewer than two contributing pairs or zero variance in either series. |
| `agg:covarPop(x, y)` | 6.2.0-M2 | `xsd:double` | Population covariance: `sum((x - mean(x)) * (y - mean(y))) / N`. A single finite pair gives zero. |
| `agg:covarSamp(x, y)` | 6.2.0-M2 | `xsd:double` | Sample covariance, with denominator `N - 1`. Unbound with fewer than two contributing pairs. |
| `agg:argMin(value, key)` | 6.2.0-M2 | An input RDF term | Return the `value` associated with the smallest `key` under SPARQL ordering. Ties keep the first encountered value. |
| `agg:argMax(value, key)` | 6.2.0-M2 | An input RDF term | Return the `value` associated with the largest `key` under SPARQL ordering. Ties keep the first encountered value. |

</div>

For these aggregates, an unbound argument or an error while evaluating an argument expression skips that solution. A two-argument aggregate skips the solution if either argument has no value. This differs from a successfully evaluated argument of an unsuitable type: for example, a string value supplied to `agg:median` invalidates the aggregate and leaves its result unbound. `agg:countIf` additionally skips values without an effective boolean value.

For the three percentile/quantile functions, `p` must be numeric, finite, between `0` and `1` inclusive, and constant across contributing solutions of a group. Exact percentiles compare this fraction exactly; `agg:approxQuantile` compares it after conversion to double precision. `agg:median` and the exact percentiles reject `NaN` input values. `agg:median` and `agg:percentileCont` return an exact `xsd:decimal` when every input value is an integer or decimal; if any input is a float or double, they return an `xsd:double`. Correlation and covariance use double-precision arithmetic.

An empty group, including a group with no contributing values, gives an unbound result for the table above except for `agg:countIf` and `agg:approxCountDistinct` (`0` as `xsd:integer`), and `agg:histogram` and `agg:arrayAgg` (`[]` as `rdf:JSON`). A grouped query with no groups still produces no rows.

`agg:first`, `agg:last`, `agg:arrayAgg`, and tie-breaking in `agg:mode`, `agg:argMin`, and `agg:argMax` depend on evaluation order. An outer `ORDER BY` sorts the aggregate results; it does not order the values fed to an aggregate. Use an ordered subquery when the input order matters, with sufficient tie-breakers to distinguish the relevant rows.

```sparql
PREFIX agg: <http://rdf4j.org/aggregate#>
PREFIX ex: <http://example.org/>

SELECT ?department
       (agg:median(?salary) AS ?median)
       (agg:percentileCont(?salary, 0.9) AS ?p90)
       (agg:argMax(?employee, ?salary) AS ?topEarner)
WHERE {
  VALUES (?department ?employee ?salary) {
    (ex:engineering ex:Alice 10)
    (ex:engineering ex:Bob   20)
    (ex:engineering ex:Carol 30)
  }
}
GROUP BY ?department
```

This returns median `20`, percentile `28` (both `xsd:decimal`), and `ex:Carol` as the top earner.

### Standard deviation and variance

The same namespace also provides these **aggregates**. All four accept one numeric argument and optional `DISTINCT`, and compute in double precision.

<div class="table-responsive">

| Invocation | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- |
| `agg:stdev(x)` | 4.2.0 | `xsd:double`; see empty-input rule below | Sample standard deviation: square root of the sample variance. |
| `agg:stdev_population(x)` | 4.2.0 | `xsd:double`; see empty-input rule below | Population standard deviation: square root of the population variance. |
| `agg:variance(x)` | 4.2.0 | `xsd:double`; see empty-input rule below | Sample variance: `sum((x - mean(x))^2) / (N - 1)` for `N > 1`. |
| `agg:variance_population(x)` | 4.2.0 | `xsd:double`; see empty-input rule below | Population variance: `sum((x - mean(x))^2) / N` for `N > 0`. |

</div>

These four functions return `0` as `xsd:integer` for empty input, and zero as `xsd:double` for a single finite value. Their current implementation also replaces a computed `NaN` statistic with integer zero. Unbound or erroring argument expressions are skipped; bound nonnumeric inputs are invalid.

## Window functions

**Kind: window function.** A window call computes a value for each input solution while preserving the number of solutions. It is not an aggregate that collapses a group, and it is not a magic property. Use it inside an aliased `SELECT` expression, for example `(winf:rank(winf:orderBy(?salary)) AS ?rank)`.

The signatures below show the function's value arguments. Add optional window specification markers as additional arguments inside the same call. Window functions do not accept `DISTINCT`. With no `winf:partitionBy`, all input solutions form one partition. The window's ordering controls the calculation, but does not sort the query's final output; use the query's `ORDER BY` for that.

### Window specification markers

These markers describe a window; they are not standalone scalar functions or magic properties. A call may contain at most one `winf:partitionBy`, one `winf:orderBy`, and one frame marker (`winf:rows` or `winf:range`). Multiple keys belong inside a single marker. Empty `winf:partitionBy()` and `winf:orderBy()` markers have the same effect as omitting the corresponding marker.

<div class="table-responsive">

| Marker | Minimum RDF4J | Definition |
| --- | --- | --- |
| `winf:partitionBy(key1, ...)` | 6.2.0-M2 | Partition solutions by their key values under RDF4J's SPARQL value comparator. Numerically equal keys can share a partition even when their numeric datatypes differ. |
| `winf:orderBy(key1, ...)` | 6.2.0-M2 | Order each partition lexicographically by the keys, ascending by default. Solutions with equal ordering keys are **peers**. |
| `winf:asc(key)` | 6.2.0-M2 | Ascending key, valid only as an argument of `winf:orderBy`. |
| `winf:desc(key)` | 6.2.0-M2 | Descending key, valid only as an argument of `winf:orderBy`. |
| `winf:rows(start, end)` | 6.2.0-M2 | Inclusive frame defined by physical row offsets relative to the current row. Negative offsets mean preceding rows, positive offsets following rows, and `0` the current row. |
| `winf:range(start, end)` | 6.2.0-M2 | Inclusive frame defined by peer groups. Only unbounded and current-row bounds are supported; numeric distance bounds are not supported. A current-row start includes the first peer, and a current-row end includes the last peer. |

</div>

Frame bounds must be integer literal constants within the signed 64-bit range, or one of the following literal keywords. Variables and computed expressions are not accepted as bounds.

<div class="table-responsive">

| Bound | Meaning |
|---|---|
| `0`, `"current"`, `"current row"` | Current row for `winf:rows`, or the current peer-group boundary for `winf:range`. |
| `"unbounded"` | Partition start when used as `start`; partition end when used as `end`. |
| `"unbounded preceding"` | Partition start; valid only as `start`. |
| `"unbounded following"` | Partition end; valid only as `end`. |

</div>

Keywords are case-insensitive. A frame cannot start after it ends; bounds extending beyond the partition are clipped to that partition. `winf:rows(-6, 0)` means at most seven rows, not seven calendar days.

If the frame is omitted, an ordered window runs from the partition start through the current row **and all its peers**. An unordered window covers its entire partition. Consequently, `winf:lastValue(x, winf:orderBy(?key))` usually returns the last value in the current peer group; to select the partition's last value, explicitly use `winf:rows("unbounded", "unbounded")`.

### Window function reference

In this table, `N` is the number of rows in the current partition. Rankings, `ntile`, `lag`, and `lead` use the partition and its ordering and ignore the frame. The value-selection and aggregate-like functions use the frame.

<div class="table-responsive">

| Invocation | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- |
| `winf:rowNumber()` | 6.2.0-M2 | `xsd:integer` | One-based row position within the partition. Peer rows receive different numbers. |
| `winf:rank()` | 6.2.0-M2 | `xsd:integer` | One-based position of the first row in the current peer group. Ties share a rank and leave gaps, for example `1, 1, 3`. |
| `winf:denseRank()` | 6.2.0-M2 | `xsd:integer` | One-based peer-group number. Ties share a rank without gaps, for example `1, 1, 2`. |
| `winf:ntile(n)` | 6.2.0-M2 | `xsd:integer` | One-based bucket number after dividing the ordered partition into `n` buckets as evenly as possible; larger buckets come first. `n` must be positive. If `n > N`, each row occupies its own bucket and trailing buckets are empty. |
| `winf:lag(x [, offset [, default]])` | 6.2.0-M2 | RDF term or unbound | Evaluate `x` at the row `offset` positions before this row; default offset is `1`, and `0` selects this row. If the target is outside the partition, evaluate `default` at the current row, or leave unbound when no default is supplied. |
| `winf:lead(x [, offset [, default]])` | 6.2.0-M2 | RDF term or unbound | As `lag`, looking `offset` positions after the current row. |
| `winf:firstValue(x)` | 6.2.0-M2 | RDF term or unbound | Evaluate `x` at the first row of the frame. An unbound value at that row is not skipped. |
| `winf:lastValue(x)` | 6.2.0-M2 | RDF term or unbound | Evaluate `x` at the last row of the frame. An unbound value at that row is not skipped. |
| `winf:nthValue(x, n)` | 6.2.0-M2 | RDF term or unbound | Evaluate `x` at the one-based `n`th row of the frame. `n` must be positive; a position beyond the frame leaves the result unbound. |
| `winf:percentRank()` | 6.2.0-M2 | `xsd:double` | `(rank - 1) / (N - 1)`, or `0.0` for a one-row partition. |
| `winf:cumeDist()` | 6.2.0-M2 | `xsd:double` | Number of rows preceding or belonging to the current peer group, divided by `N`. All peers receive the same result. |
| `winf:sum(x)` | 6.2.0-M2 | Numeric literal | Sum of successfully evaluated, bound numeric values in the frame, using SPARQL numeric promotion. Empty/no-value frame: `0` as `xsd:integer`. |
| `winf:avg(x)` | 6.2.0-M2 | Numeric literal | Sum divided by the number of contributing values, using SPARQL numeric promotion and division. Integer-only input gives `xsd:decimal`. Empty/no-value frame: `0` as `xsd:integer`. |
| `winf:count(x)` | 6.2.0-M2 | `xsd:integer` | Number of bound, successfully evaluated values of `x` in the frame. Accepts one expression, not `*`; use `winf:count(1)` to count rows. Empty/no-value frame: `0`. |
| `winf:min(x)` | 6.2.0-M2 | An input RDF term or unbound | Smallest bound, successfully evaluated value in the frame under SPARQL ordering. Empty/no-value frame: unbound. |
| `winf:max(x)` | 6.2.0-M2 | An input RDF term or unbound | Largest bound, successfully evaluated value in the frame under SPARQL ordering. Empty/no-value frame: unbound. |

</div>

Without an ordering key, every row is a peer: `rank` and `denseRank` are `1`, `percentRank` is `0.0`, and `cumeDist` is `1.0`. For deterministic row-number, offset, or physical-row-frame results, specify ordering keys that break relevant ties.

`ntile`'s bucket count, `lag`/`lead`'s offset, and `nthValue`'s position must be integer-typed literals within the signed 64-bit range; offsets must be nonnegative, and counts/positions must be positive. They may vary per row. An invalid constant argument fails query evaluation setup; an invalid or unbound data-dependent argument leaves only that solution's window value unbound. For `lag` and `lead`, the default applies only when the target row is outside the partition, not when `x` is unbound or errors at an existing target row.

An error while evaluating `x` is treated as no value. The frame functions `sum`, `avg`, `count`, `min`, and `max` skip such values. A bound nonnumeric or malformed numeric value makes `sum` and `avg` unbound for every frame containing it. `firstValue`, `lastValue`, and `nthValue` return unbound when their selected row has no value, or when their frame has no selected row. A window over zero input solutions produces zero output solutions.

Window functions are evaluated after `GROUP BY`/`HAVING` and before the query's `ORDER BY`. Aggregate expressions can therefore be window inputs, for example `winf:sum(SUM(?amount), winf:orderBy(?department))` in a query grouped by `?department`. In grouped queries, window arguments and keys must use group variables or aggregates. A window cannot use another expression alias introduced by the same `SELECT`; introduce such values in a subquery first.

Direct window calls in `BIND`, `FILTER`, `HAVING`, `GROUP BY`, `ORDER BY`, or `EXISTS` graph patterns are rejected. A window call cannot be nested inside another window call, its specification, or an aggregate. To filter on a window result, calculate it in a subquery and filter its projected variable outside that subquery.

```sparql
PREFIX winf: <http://rdf4j.org/function/window#>
PREFIX ex: <http://example.org/>

SELECT ?employee ?salary
       (winf:rank(winf:partitionBy(?department),
                  winf:orderBy(winf:desc(?salary))) AS ?rank)
       (winf:sum(?salary, winf:partitionBy(?department),
                 winf:orderBy(?salary)) AS ?runningTotal)
       (winf:avg(?salary, winf:partitionBy(?department),
                 winf:orderBy(?salary, ?employee),
                 winf:rows(-1, 0)) AS ?movingAverage)
WHERE {
  VALUES (?department ?employee ?salary) {
    (ex:engineering ex:Alice 100)
    (ex:engineering ex:Bob   100)
    (ex:engineering ex:Carol 300)
  }
}
ORDER BY ?salary ?employee
```

Alice and Bob are salary peers: both have rank `2` and running total `200`; Carol has rank `1` and running total `500`. The two-row moving averages are `100`, `100`, and `200`, with `?employee` breaking the equal-salary tie for that calculation.

## Text search and similarity

These operations accept string literals. Edit distances and matching operate on Unicode code points. Similarity and distance functions are case-sensitive unless the definition says otherwise. Phonetic codes are untagged `xsd:string` literals; the array-valued forms of `tokenize` and `ngrams` contain untagged JSON strings, and their magic-property forms return untagged `xsd:string` literals as well.

<div class="table-responsive">

| Invocation | Kind | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- | --- |
| `textf:levenshtein(a, b)` | Function | 6.2.0-M2 | `xsd:integer` | Minimum number of single-code-point insertions, deletions and substitutions needed to change `a` into `b`, each costing 1. Identical strings have distance 0. |
| `textf:damerauLevenshtein(a, b)` | Function | 6.2.0-M2 | `xsd:integer` | Unrestricted Damerau-Levenshtein distance: the same edits plus adjacent transposition, also costing 1. Inputs are rejected when `(length(a) + 2) * (length(b) + 2)` exceeds 4,194,304. |
| `textf:jaroSimilarity(a, b)` | Function | 6.2.0-M2 | `xsd:double` | Jaro similarity in `[0, 1]`, defined below. Two empty strings give 1; exactly one empty string gives 0. |
| `textf:jaroWinkler(a, b [, scaling])` | Function | 6.2.0-M2 | `xsd:double` | `j + l * scaling * (1 - j)`, where `j` is Jaro similarity and `l` is the common-prefix length capped at 4 code points. `scaling` defaults to `0.1`; values outside `[0, 0.25]` are errors, while `NaN` produces `NaN`. The prefix boost is applied without a similarity threshold. |
| `textf:soundex(s)` | Function | 6.2.0-M2 | `xsd:string` | American Soundex code: the first letter and three digits, padded with zeros, e.g. `"R163"` for `"Robert"`. Considers only ASCII letters and ignores case. Returns `""` if none are present. |
| `textf:metaphone(s [, maxLength])` | Function | 6.2.0-M2 | `xsd:string` | Original Metaphone phonetic code, using only ASCII letters and ignoring case. Truncates to the positive 32-bit integer `maxLength`, default 4. Returns `""` if no ASCII letters are present. |
| `textf:trigramSimilarity(a, b)` | Function | 6.2.0-M2 | `xsd:double` | Jaccard similarity of the distinct trigram sets: intersection size divided by union size. Splits into runs of Unicode letters/digits, lowercases each code point and pads each word with two leading spaces and one trailing space. Returns 0 if both sets are empty. |
| `textf:ilike(s, pattern [, escape])` | Function | 6.2.0-M2 | `xsd:boolean` | Case-insensitive whole-string SQL `LIKE` matching: `%` matches zero or more code points and `_` matches exactly one. `escape` defaults to a backslash and must contain one code point; it makes the following pattern character literal. A trailing escape is an error. |
| `textf:tokenize(s)` | **Function + magic property** | 6.2.0-M2 | `rdf:JSON` array in function form | Extracts maximal runs of Unicode letters, digits, ASCII apostrophes and connector punctuation such as `_`, preserving spelling and encounter order. No tokens gives `[]`. See the [magic-property form](#set-returning-functions-magic-properties) for one row per token. |
| `textf:ngrams(s, n)` | **Function + magic property** | 6.2.0-M2 | `rdf:JSON` array in function form | All overlapping contiguous substrings of `n` code points, in encounter order, including whitespace and punctuation. `n` must be a positive 32-bit integer. A shorter input gives `[]`; `textf:ngrams("abcd", 2)` gives `["ab","bc","cd"]`. See the [magic-property form](#set-returning-functions-magic-properties) for one row per substring. |

</div>

For Jaro similarity, let `m` be the number of matched code points within a window of `max(0, floor(max(length(a), length(b)) / 2) - 1)` positions, and `t` half the number of mismatches between the matched subsequences. The score is `(m / length(a) + m / length(b) + (m - t) / m) / 3`; it is 0 when there are no matches. Matching scans each string from left to right and uses each matched position once.

## JSON and arrays

JSON documents are `rdf:JSON` literals. String literals containing JSON text are also accepted wherever a function expects a JSON document. A JSON array is a single RDF literal, not a sequence of SPARQL solutions. Use the [magic properties below](#set-returning-functions-magic-properties) to expand it into solutions.

### JSON paths

The `path` argument supplies the lexical form of a literal, normally an `xsd:string`. RDF4J supports the following JSONPath subset; array indices are **zero-based**, unlike the one-based indices returned by `arrf:unnestWithOrdinality`.

<div class="table-responsive">

| Path syntax | Definition |
|---|---|
| `$` | The complete input document. |
| `$.name`, `$['name']`, `$["name"]` | The member with the given name in an object. |
| `$[n]` | Array element `n`; a negative index counts from the end, so `$[-1]` selects the last element. An out-of-range index selects nothing. |
| `$[*]`, `$.*` | Every immediate array element or object member value, in document order. |
| `$..name` | Every member with the given name found recursively in objects and arrays. |
| `$..*` | Every descendant value, recursively. |

</div>

The leading `$` is optional. JSONPath filters, slices and unions are not supported. Malformed JSON or an invalid path raises an evaluation error; an existing JSON `null` is distinct from a missing path.

### JSON functions

All entries in this table are **Functions**: they use expression syntax and return one RDF term.

<div class="table-responsive">

| Invocation | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- |
| `jsonf:jsonValue(json, path)` | 6.2.0-M2 | RDF term | Requires exactly one selected scalar or encoded RDF term. JSON strings, booleans and numbers become `xsd:string`, `xsd:boolean`, and `xsd:integer` / `xsd:decimal` / `xsd:double` literals respectively; encoded IRIs, blank nodes and literals other than `rdf:JSON` are restored. No match, multiple matches, JSON `null`, and ordinary objects or arrays are errors. |
| `jsonf:jsonQuery(json, path)` | 6.2.0-M2 | `rdf:JSON` | Returns one selected value as JSON; wraps multiple selected values in an array. JSON scalars and `null` are allowed. No match is an error. |
| `jsonf:jsonExists(json, path)` | 6.2.0-M2 | `xsd:boolean` | `true` when the path selects at least one value, including JSON `null`; `false` when it selects nothing. Invalid JSON and invalid paths remain errors. |
| `jsonf:jsonKeys(json [, path])` | 6.2.0-M2 | `rdf:JSON` array of strings | Returns the member names of an object in document order. Without `path`, the input must be an object; with `path`, exactly one object must be selected. An empty object returns `[]`. |
| `jsonf:jsonArrayLength(json [, path])` | 6.2.0-M2 | `xsd:integer` | Returns the number of array elements, counting `null` elements. Without `path`, the input must be an array; with `path`, exactly one array must be selected. An empty array returns `0`. |
| `jsonf:jsonObject(key1, value1, ...)` | 6.2.0-M2 | `rdf:JSON` object | Constructs an object from zero or more key/value pairs. Keys are literals whose lexical forms become member names; values use the RDF-to-JSON mapping below. Later duplicate keys replace earlier values. Zero arguments return `{}`; an odd argument count is an error. |
| `jsonf:jsonArray(value1, ...)` | 6.2.0-M2 | `rdf:JSON` array | Constructs an array from zero or more RDF terms using the mapping below. An `rdf:JSON` argument is embedded as JSON, rather than quoted as text. Zero arguments return `[]`. |
| `jsonf:jsonSet(json, path, value)` | 6.2.0-M2 | `rdf:JSON` | Returns a modified copy with the encoded `value` assigned at a definite path containing only member names and indices. `$` replaces the document. Missing object members can create intermediate objects or arrays. At the final array step, an index beyond the end appends and an index before the beginning prepends; an out-of-range intermediate array index is an error. Wildcards and recursive descent are errors. |

</div>

### RDF terms in JSON

For RDF terms other than `rdf:JSON`, the constructors and array functions preserve the datatype and lexical form when encoding supported terms. They use JSON primitives only when decoding them preserves both; otherwise they use explicit term objects. An `rdf:JSON` argument is embedded as parsed JSON, so its original whitespace and lexical formatting are not preserved.

<div class="table-responsive">

| RDF input | JSON representation and decoding |
|---|---|
| `xsd:string` | A JSON string; decoded as `xsd:string`. |
| Numeric or boolean literal | A JSON primitive when its datatype and lexical form survive decoding; otherwise an `{"@value": "...", "@type": "datatype IRI"}` object. This preserves derived numeric datatypes, floating-point datatypes, noncanonical or malformed lexical forms, and non-finite values. |
| Language-tagged literal | `{"@value": "...", "@language": "..."}`; a directional language literal also carries `"@direction"`. |
| Other typed literal | `{"@value": "...", "@type": "datatype IRI"}`. |
| IRI or blank node | `{"@id": "IRI"}` or `{"@id": "_:blank-node-id"}`. |
| `rdf:JSON` | Its parsed JSON value is embedded directly. |

</div>

When reading ordinary JSON, numbers written without a decimal point or exponent become `xsd:integer`; numbers with a decimal point but no exponent become `xsd:decimal`, even when their value is integral; ordinary exponent numbers become `xsd:double`. Compact exponent numbers outside the finite, nonzero `double` range stay exact in JSON results, but extracting them as RDF numbers with `jsonValue` or `unnest` raises an evaluation error. JSON `null` has no RDF term representation: `jsonValue` rejects it, while the unnest magic properties skip it. Nested objects and arrays emitted by `unnest` remain `rdf:JSON` literals unless they encode RDF terms.

### Array functions

<div class="table-responsive">

| Invocation | Kind | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- | --- |
| `arrf:arrayContains(array, value)` | **Function** | 6.2.0-M2 | `xsd:boolean` | Tests whether a JSON array contains an element equal to the JSON encoding of `value`. Numbers compare by numeric value across primitive and encoded numeric forms; arrays and objects compare recursively. The first argument must be an array. |
| `arrf:arrayLength(json)` | **Function** | 6.2.0-M2 | `xsd:integer` | Counts the elements of an array **or the members of an object**, including entries whose value is `null`. Other JSON values are errors. |
| `arrf:generateSeries(start, stop [, step])` | **Function + magic property** | 6.2.0-M2 | `rdf:JSON` array | Generates values beginning at `start` and repeatedly adding `step` while within the inclusive `stop` bound. Numeric inputs use numeric type promotion; the default step is `1`. Date/time inputs use a duration step, defaulting to one day, and preserve the start value's datatype and offset. A negative step descends; a step pointing away from `stop` returns `[]`. Zero step is an error. The magic-property form emits the same values as separate solutions. |

</div>

`generateSeries` permits at most 10,000,000 elements per call; a larger result is an evaluation error in either invocation form.

### Set-returning functions (magic properties)

A **magic property** is an executable predicate in a graph pattern. It consumes arguments from its subject and emits zero or more solutions through its object. It is not a lookup of triples stored under that predicate. Use a single term for one argument or result, and an inline RDF collection `(arg1 arg2 ...)` for several. Collection members are separated by spaces, not commas; the parentheses here denote SPARQL collection syntax, not a scalar function call.

**Function + magic property** means that the same IRI also has a scalar function form. That form returns one `rdf:JSON` array; the magic-property form returns one solution per element. `arrf:unnest` and `arrf:unnestWithOrdinality` are **magic properties only** and cannot be called as scalar expressions in `BIND` or `SELECT`.

<div class="table-responsive">

| Magic-property pattern | Kind | Minimum RDF4J | Output columns | Definition |
| --- | --- | --- | --- | --- |
| `?json arrf:unnest ?value .` | **Magic property only** | 6.2.0-M2 | One: `?value` | Emits each non-null array element, or each non-null object member value, decoded to an RDF term. It does not recursively flatten nested arrays. |
| `?json arrf:unnestWithOrdinality (?value ?position) .` | **Magic property only** | 6.2.0-M2 | Two: value and position/key | For an array, `?position` is its original **one-based** `xsd:integer` index. For an object, it is the member name as `xsd:string`. Nulls are skipped without renumbering: `[null,"x",null,"y"]` emits `("x", 2)` and `("y", 4)`. |
| `(?start ?stop ?step) arrf:generateSeries ?value .` | **Function + magic property** | 6.2.0-M2 | One: series value | Emits the same numeric or date/time values as the scalar `generateSeries`. Omit `?step` from the subject collection to use its default. |
| `(?text ?delimiter) strf:split ?part .` | **Function + magic property** | 6.2.0-M2 | One: string literal | Splits on the literal delimiter, preserving leading, adjacent and trailing empty parts. An empty delimiter splits into Unicode code points. Each part retains the source's language tag and direction, if present. |
| `(?text ?pattern) strf:regexpExtractAll ?match .` | **Function + magic property** | 6.2.0-M2 | One: string literal | Emits each regex match, or each selected capture-group value. The subject collection may also contain an integer group number and/or flags, as in the scalar signature; the default group is `0` (whole match). An unmatched optional group emits an empty string. Results retain the source's language tag and direction, if present. No matches produce no solutions. |
| `?text textf:tokenize ?token .` | **Function + magic property** | 6.2.0-M2 | One: `xsd:string` | Emits each maximal run of Unicode letters, digits, ASCII apostrophes and connector punctuation such as `_`. The original case is retained; the source language tag is not copied. |
| `(?text ?n) textf:ngrams ?gram .` | **Function + magic property** | 6.2.0-M2 | One: `xsd:string` | Emits every contiguous substring of `n` Unicode code points. `n` must be a positive integer in the 32-bit signed range. Input shorter than `n` produces no solutions. The source language tag is not copied. |

</div>

The object must have the number of result columns shown above. A produced row with a mismatched column count raises a query evaluation error. A single output variable can also be written as a one-element collection, such as `(?value)`. Output constants and already-bound output variables constrain the result by exact RDF term equality: the integer `2` does not match decimal `2.0` or `"2"^^xsd:int`. Use distinct variables for separate output columns.

#### One array value versus several solutions

This scalar call produces one solution, with `?parts` bound to the `rdf:JSON` literal `["red","green","blue"]`:

```sparql
PREFIX strf: <http://rdf4j.org/function/string#>

SELECT (strf:split("red,green,blue", ",") AS ?parts)
WHERE {}
```

This magic property produces three solutions, with `?part` bound to one string in each solution:

```sparql
PREFIX strf: <http://rdf4j.org/function/string#>

SELECT ?part
WHERE {
    ("red,green,blue" ",") strf:split ?part .
}
ORDER BY ?part
```

To retain an array's positions and explicitly order the query results by them:

```sparql
PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX arrf: <http://rdf4j.org/function/array#>

SELECT ?value ?index
WHERE {
    VALUES ?array { '[null,"x",null,"y"]'^^rdf:JSON }
    ?array arrf:unnestWithOrdinality (?value ?index) .
}
ORDER BY ?index
```

The result is `("x", 2)` followed by `("y", 4)`.

#### Binding scope and errors

RDF4J evaluates these calls for the solutions of their surrounding group. Input variables must be bound within the call's visible scope, for example by ordinary triple patterns, `VALUES`, `BIND`, or another magic property in that group. Calls with dependencies can be scheduled after the calls that supply their inputs. A subquery or `UNION` branch does not automatically receive variables bound only outside its scope; bind the inputs inside that scope.

An input solution with an unbound argument or a rejected argument value contributes no solutions for that call. An empty array or object, an all-null array, and a function with no matches also contribute no solutions. Other input solutions continue normally. For `unnest`, an element that cannot be decoded, such as an invalid encoded IRI or an unsupported exponent number, rejects that entire input call; it does not emit partial results. Use `OPTIONAL { ... }` around a call when the surrounding input solution must survive with its output variables unbound.

These magic properties calculate results from their arguments rather than reading triples. Surrounding `GRAPH` clauses still control graph scope, dataset selection, and the ordinary data patterns that bind those arguments. `GRAPH ?g` can evaluate a call separately for multiple named graphs. Keep graph-independent generation outside `GRAPH`; do not use an absent constant graph as a portable way to enable or suppress a call. The order of solutions in a query result requires `ORDER BY`, even when a function produces its elements in a defined sequence.

## Geospatial functions

These are **scalar functions** supplied by `rdf4j-queryalgebra-geosparql`, in addition to the standard functions described in [GeoSPARQL support](/documentation/programming/geosparql/). Geometry inputs must be `geo:wktLiteral` literals. A lexical form such as `"<http://www.opengis.net/def/crs/EPSG/0/3857> POINT (10 20)"` declares the coordinate reference system (CRS); without a CRS prefix, the default is CRS84.

<div class="table-responsive">

| CRS | IRI | Coordinate order and units |
|---|---|---|
| CRS84 | `http://www.opengis.net/def/crs/OGC/1.3/CRS84` | Longitude, latitude in degrees. |
| EPSG:4326 | `http://www.opengis.net/def/crs/EPSG/0/4326` | Latitude, longitude in degrees. |
| EPSG:3857 | `http://www.opengis.net/def/crs/EPSG/0/3857` | Web Mercator easting, northing in metres. |

</div>

A `crs` argument can be an IRI, an integer EPSG code such as `3857`, or a literal containing `"EPSG:3857"`, `"CRS84"`, `"OGC:CRS84"` or a CRS IRI. Reprojection between different CRSs supports only the three systems above; a request to retain the same CRS does not reproject.

<div class="table-responsive">

| Invocation | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- |
| `geox:area(geometry [, units])` | 6.2.0-M2 | `xsd:double` | Polygon area, subtracting holes and summing polygon components in collections; points and lines contribute zero. Without `units`, uses planar coordinates and squared CRS units. `uom:metre` selects spherical square metres for CRS84/EPSG:4326; for a projected CRS it leaves the planar calculation unchanged. |
| `geox:length(geometry [, units])` | 6.2.0-M2 | `xsd:double` | Line length, or polygon perimeter including hole boundaries, summed across components; points contribute zero. Without `units`, uses planar CRS units. `uom:metre` selects great-circle metres for CRS84/EPSG:4326; for a projected CRS it leaves the planar calculation unchanged. |
| `geox:centroid(geometry)` | 6.2.0-M2 | `geo:wktLiteral` | Planar geometric centroid as a point in the input CRS. An empty geometry produces an empty point. |
| `geox:transform(geometry, crs)` | 6.2.0-M2 | `geo:wktLiteral` | Reprojects to `crs`, including the axis swap between CRS84 and EPSG:4326. An unsupported change of CRS is an error. Projection to EPSG:3857 rejects latitudes at or beyond either pole. |
| `geox:simplify(geometry, tolerance)` | 6.2.0-M2 | `geo:wktLiteral` | Douglas-Peucker simplification in the input CRS. `tolerance` is a numeric distance in CRS units; negative values and `NaN` are errors. |
| `geox:makePoint(x, y [, crs])` | 6.2.0-M2 | `geo:wktLiteral` | Constructs a point from finite numeric coordinates. Defaults to CRS84, with `x` as longitude and `y` as latitude. An explicit CRS labels the supplied coordinates; it does not transform them. |
| `geox:asGeoJSON(geometry)` | 6.2.0-M2 | `rdf:JSON` | Serializes a GeoJSON geometry object in CRS84 longitude/latitude order. EPSG:4326 axes are swapped and EPSG:3857 coordinates are reprojected first. Other source CRSs are rejected. |
| `geox:closestPoint(a, b)` | 6.2.0-M2 | `geo:wktLiteral` | A point on `a` minimizing planar distance to `b`, after transforming `b` into `a`'s CRS when necessary. Returns the point in `a`'s CRS. Either geometry being empty is an error. |

</div>

For `area` and `length`, the only accepted unit IRIs are `uom:metre`, `uom:degree` and `uom:unity`. The last two select the planar calculation without converting coordinates. Spherical measurements use a mean Earth radius of 6,371,008.8 metres, rather than an ellipsoidal model. A projected result is only in metres or square metres when the input CRS has metre units.

## Bitwise, encoding and miscellaneous functions

### Bitwise functions

All entries in this table are **scalar functions**. Operands must be integer-typed literals and results have datatype `xsd:integer`. Operations use arbitrary-precision signed integers with two's-complement semantics; `bitCount` has the explicit negative-value rule below.

<div class="table-responsive">

| Invocation | Minimum RDF4J | Definition |
| --- | --- | --- |
| `bitf:bitAnd(x, y)` | 6.2.0-M2 | Bitwise AND. A result bit is set only when both operand bits are set. |
| `bitf:bitOr(x, y)` | 6.2.0-M2 | Bitwise inclusive OR. A result bit is set when either operand bit is set. |
| `bitf:bitXor(x, y)` | 6.2.0-M2 | Bitwise exclusive OR. A result bit is set when exactly one operand bit is set. |
| `bitf:bitNot(x)` | 6.2.0-M2 | Bitwise complement, equal to `-x - 1`. |
| `bitf:bitCount(x)` | 6.2.0-M2 | Number of set bits. Nonnegative values have no fixed width. Negative values are counted in a signed 64-bit two's-complement representation and must be at least `-9223372036854775808`; for example, `bitCount(-1)` is 64. |
| `bitf:bitShiftLeft(x, n)` | 6.2.0-M2 | Shift left by `n` bits without fixed-width overflow. A negative `n` shifts right arithmetically. |
| `bitf:bitShiftRight(x, n)` | 6.2.0-M2 | Arithmetic right shift by `n` bits, preserving the sign. A negative `n` shifts left. For example, `bitShiftRight(-5, 1)` is `-3`. |

</div>

The shift distance `n` must be an integer in `[-1048576, 1048575]`; values outside that range are evaluation errors.

### Encoding and hashing functions

All entries in this table are **scalar functions**. A **byte input** is a string literal encoded as UTF-8, an `xsd:hexBinary` literal decoded from hexadecimal, or an `xsd:base64Binary` literal decoded from Base64. For example, the string `"61"` supplies the bytes for the characters `6` and `1`, whereas `"61"^^xsd:hexBinary` supplies the single byte for `a`.

<div class="table-responsive">

| Invocation | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- |
| `encf:base64Encode(input)` | 6.2.0-M2 | `xsd:string` | Standard Base64 encoding of a byte input, including padding. |
| `encf:base64Decode(s)` | 6.2.0-M2 | `xsd:string` | Decodes a string containing standard or URL-safe Base64, ignoring whitespace and allowing omitted padding. The decoded bytes must be valid UTF-8; invalid Base64 or invalid UTF-8 is an error. |
| `encf:toHex(input)` | 6.2.0-M2 | `xsd:string` | Lowercase hexadecimal encoding of a byte input. Also accepts integers: nonnegative values use their arbitrary-precision hexadecimal representation without leading padding; negative values use 16 hexadecimal digits for signed 64-bit two's complement and must be at least `-9223372036854775808`. |
| `encf:fromHex(s)` | 6.2.0-M2 | `xsd:string` | Decodes a hexadecimal string after trimming leading/trailing whitespace. Requires an even number of hexadecimal digits and valid UTF-8 decoded bytes. |
| `encf:crc32(input)` | 6.2.0-M2 | `xsd:integer` | CRC-32 checksum of a byte input as an unsigned value in `[0, 4294967295]`. |
| `encf:xxhash64(input [, seed])` | 6.2.0-M2 | `xsd:integer` | XXH64 hash of a byte input as an unsigned value in `[0, 18446744073709551615]`. `seed` defaults to 0 and must be an integer in the signed 64-bit range. |
| `encf:hmac(message, key [, algorithm])` | 6.2.0-M2 | `xsd:string` | HMAC of `message` under `key`, both byte inputs, encoded as lowercase hexadecimal. `algorithm` defaults to `"sha256"`; accepted names are `"md5"`, `"sha1"`, `"sha224"`, `"sha256"`, `"sha384"` and `"sha512"`, ignoring case and hyphens. Empty messages and keys are supported. |

</div>

`base64Decode` and `fromHex` accept string literals, not binary literals, and return text rather than a binary datatype. Encoding, decoding and phonetic-code results do not inherit the input's language tag.

### Miscellaneous functions

<div class="table-responsive">

| Invocation | Kind | Minimum RDF4J | Result | Definition |
| --- | --- | --- | --- | --- |
| `miscf:nullIf(a, b)` | Function | 6.2.0-M2 | The RDF term `a`, or an evaluation error | Compares using SPARQL value equality, so `1` and `1.0` compare equal. If value comparison raises an error, falls back to RDF-term equality. Equal arguments raise an evaluation error; otherwise returns `a` unchanged. Both arguments must be bound. |

</div>

For example, `BIND(miscf:nullIf(?value, 0) AS ?nonzero)` leaves `?nonzero` unbound when `?value` equals zero. This does not create an RDF term representing SQL `NULL`.

## Further reading

For implementing application-specific extensions, see [Creating custom SPARQL functions](/documentation/tutorials/custom-sparql-functions/). The built-in registrations and implementations are maintained in RDF4J's [query evaluation module](https://github.com/eclipse-rdf4j/rdf4j/tree/main/core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/function); geospatial extensions are in the [GeoSPARQL module](https://github.com/eclipse-rdf4j/rdf4j/tree/main/core/queryalgebra/geosparql/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/function/geosparql/ext).
