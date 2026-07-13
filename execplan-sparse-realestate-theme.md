# Add a sparse REAL_ESTATE benchmark theme that stresses the native LMDB query engine's gaps

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with `.agent/PLANS.md` (checked into this repository).

## Purpose / Big Picture

The LMDB sail (`core/sail/lmdb`) has a "native query engine": instead of evaluating SPARQL through RDF4J's generic iterator-based evaluation strategy, it compiles certain query shapes down to slot-based programs that run directly against LMDB indexes (see `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbNativeQueryCompiler.java` and `LmdbNativeAggregateCompiler.java`). Queries the compiler cannot handle silently fall back to the generic evaluator, which is often 10-100x slower.

The existing benchmark "themes" (deterministic synthetic datasets plus a query catalog, in `testsuites/benchmark-common`) are dense: nearly every entity has every property. That means OPTIONAL, MINUS, FILTER NOT EXISTS and similar constructs rarely change the result and rarely stress the engine in a realistic way.

After this change, a new theme `REAL_ESTATE` exists whose generated data is deliberately *sparse* — most properties of an entity are present only with some probability between 10% and 70% — and whose 13-query catalog deliberately uses constructs that the native LMDB engine currently does NOT compile (so they fall back to the generic evaluator) or compiles only on a slow path. This gives the optimize-lmdb work a measurable target list: every query in this theme is either a fallback or a known slow path today, so future engine work can be validated by watching these benchmark numbers drop.

Observable outcome: `ThemeDataSetGenerator.generate(Theme.REAL_ESTATE)` produces a deterministic model of roughly 600-800k statements; `ThemeQueryCatalog.queriesFor(Theme.REAL_ESTATE)` returns 13 queries; all `testsuites/benchmark-common` unit tests pass; a new memory-sail test proves every catalog expected count matches actual query results on a MemoryStore; the existing LMDB theme benchmarks (`core/sail/lmdb/.../benchmark/ThemeQueryBenchmark.java`) automatically pick up the new theme because they iterate `Theme.values()`.

## Progress

- [x] (2026-07-03) Researched existing generator, catalog, tests, and native-engine capabilities.
- [x] (2026-07-03) ExecPlan written.
- [x] (2026-07-03) Milestone 1: generator (`Theme.REAL_ESTATE`, `RealEstateConfig`, `generateRealEstate`) + generator tests.
- [x] (2026-07-03) Milestone 2: 13 catalog queries + expected-count scaffolding (placeholder counts baked as 0/-1 first); benchmark-common 28/28 green with placeholders.
- [x] (2026-07-03) Milestone 3: ground-truth harness `RealEstateThemeQueryCountTest` run on MemoryStore; actual counts baked into catalog and `ThemeQueryCatalogExpectedCountTest`; harness green.
- [x] (2026-07-03) Milestone 4: copyright check green, formatter run, benchmark-common 28/28 green, memory count test green; evidence persisted to `initial-evidence.txt`.

## Surprises & Discoveries

- Observation: `ThemeQueryCatalog`'s static initializer validates ALL themes (13 queries each, expected-count binding arrays sized 13). Adding the enum constant without simultaneously adding catalog entries breaks every catalog consumer.
  Evidence: `validateQueries()` at `ThemeQueryCatalog.java:1726-1756` throws `IllegalStateException("Missing query list for theme ...")`.
- Observation: Two test gates constrain every query's text: `ThemeQueryCatalogComplexityTest` requires >= 2 complexity markers (OPTIONAL, UNION, FILTER EXISTS, FILTER NOT EXISTS, MINUS, BIND(, VALUES, GROUP BY, HAVING) unless the query is a large denormalized `SELECT DISTINCT *` shape; `ThemeQueryCatalogOptimizerGapTest` additionally requires `FILTER(?opt...`, `||`, ` IN `, or one of two denormalized shapes.
- Observation: `testsuites/benchmark-common` has no sail/repository dependencies, so ground-truth counts cannot be computed inside that module; `core/sail/memory` already depends on benchmark-common in test scope (its `ThemeQueryBenchmark` imports the generator), so the ground-truth test lives there.
- Observation: The LMDB `ThemeQueryBenchmark.loadData()` loads ALL themes' datasets into one store (`for (var themeDataset : Theme.values())`), so the new theme increases every LMDB theme-benchmark store size; dataset size is therefore kept moderate (~60k properties).
- Observation: The `jitterInt` mechanism swings entity counts by +/-50%, and for seed 42 the REAL_ESTATE draws landed low: 75 districts (base 150) and 520 amenity groups survived HAVING out of a base 400 amenity pool jittered upward. Total dataset size is 686,470 statements (measured with a scratch runner against the installed jar).
  Evidence: `RealEstateThemeQueryCountTest` stdout: `REAL_ESTATE query 3 rows=75`, `query 10 rows=520`; scratch runner printed `statements=686470`.
- Observation: The MemoryStore ground-truth run is fast (~2 s for load + all 13 queries) because the jittered dataset is small; the denormalized dossier view (query 11) still produces 99,720 distinct ordered rows and the union-heavy view (query 12) 108,771 rows, so both remain meaningful row-surface stress tests.
  Evidence: surefire summary `tests=1, failures=0, errors=0, skipped=0, time=1.952s`.

## Decision Log

- Decision: Name the theme `REAL_ESTATE` (namespace `http://example.com/theme/realestate/`, prefix `re:`).
  Rationale: Existing themes are domain-named (PHARMA, LIBRARY, ...). A property-listings domain justifies natural sparsity: not every listing has a price, floor area, energy rating, sale date, offers, or viewings.
  Date/Author: 2026-07-03 / Claude Code.
- Decision: Sparsity is implemented with per-entity probability draws from the seeded `contentRandom` (e.g. `if (contentRandom.nextDouble() < config.priceProbability)`), with probabilities exposed on `RealEstateConfig` builders; entity counts are jittered through the existing `jitterInt(jitterRandom, ...)` mechanism like the other themes.
  Rationale: Deterministic for a fixed seed (required: expected counts are baked into the catalog), configurable for future experiments, and consistent with existing generator style.
  Date/Author: 2026-07-03 / Claude Code.
- Decision: The 13 queries each target a specific native-engine gap (see the table in "Context and Orientation"); none of them is expected to run fully native-fast today.
  Rationale: The user's goal is a benchmark suite that highlights currently-unsupported or slow constructs so engine improvements are measurable.
  Date/Author: 2026-07-03 / Claude Code.
- Decision: Ground truth is computed by a permanent JUnit test `core/sail/memory/src/test/java/org/eclipse/rdf4j/sail/memory/benchmark/RealEstateThemeQueryCountTest.java` that loads the generated dataset into a MemoryStore and asserts each query's row count and, where applicable, the `?count` binding value against the catalog.
  Rationale: MemoryStore uses the generic evaluation strategy (no LMDB native engine), so it is the reference implementation; keeping the test permanent guards the catalog numbers against future generator changes.
  Date/Author: 2026-07-03 / Claude Code.
- Decision: Keep `ThemeQueryCatalog.QUERY_COUNT` at 13 and give the new theme exactly 13 queries.
  Rationale: The catalog validates that every theme has exactly `QUERY_COUNT` queries; changing the count would ripple through all themes.
  Date/Author: 2026-07-03 / Claude Code.

## Outcomes & Retrospective

Completed 2026-07-03. All four milestones delivered: `Theme.REAL_ESTATE` generates 686,470 deterministic statements with verified sparsity (price on ~60% of properties, soldDate on ~15%); the 13 catalog queries pass all four catalog gate tests; ground-truth counts were computed on MemoryStore and baked into `ThemeQueryCatalog`, `ThemeQueryCatalogExpectedCountTest`, and the permanent `RealEstateThemeQueryCountTest` (which re-verifies them on every run in ~2 s). benchmark-common finished 28/28 green. The LMDB-side benchmarks pick the theme up automatically via `Theme.values()` (`-p themeName=REAL_ESTATE -p z_queryIndex=0..12`). Lesson learned: the catalog's meta-tests (count, complexity, optimizer-gap, expected-count) make query authoring constraint-driven — write the query, the gates tell you what is missing. Remaining follow-up (optional): record LMDB baseline numbers for the new theme with `ThemeQueryBenchmark`, and consider a `QueryPlanSnapshotCli` snapshot for plan-drift tracking.

## Context and Orientation

Read this section as if you know nothing about the repository.

RDF4J is a Java framework for RDF data and SPARQL queries. This repo is a Maven multi-module build. Three modules matter here:

1. `testsuites/benchmark-common` — a small library of benchmark support code shared by several storage backends. It contains:
   - `src/main/java/org/eclipse/rdf4j/benchmark/rio/util/ThemeDataSetGenerator.java`: a deterministic synthetic RDF data generator. It has an enum `Theme` (MEDICAL_RECORDS, SOCIAL_MEDIA, LIBRARY, ENGINEERING, HIGHLY_CONNECTED, TRAIN, ELECTRICAL_GRID, PHARMA and now REAL_ESTATE). For each theme there is a config class (e.g. `PharmaConfig`) holding entity counts and a seed, a factory method (e.g. `pharmaConfig()`), and a `generateXxx(config, handler)` method that streams statements to an `RDFHandler`. Helper methods `jitterInt`/`jitterDouble` derive slightly-randomized counts from a secondary random seeded with `seed ^ JITTER_SEED_XOR` so datasets are deterministic per seed but not perfectly regular. `generate(Theme, RDFHandler)` dispatches via a switch.
   - `src/main/java/org/eclipse/rdf4j/benchmark/common/ThemeQueryCatalog.java`: a static catalog mapping each `Theme` to exactly `QUERY_COUNT` (13) `BenchmarkQuery` objects (name, SPARQL string, expected result-row count) plus a parallel `long[13]` of "expected count binding values" — for queries that project `(COUNT(...) AS ?count)` the value the single `?count` binding must have, or `-1` when the query does not project `?count`. A static initializer validates all of this for every theme; getting it wrong breaks every consumer of the catalog.
   - Test gates in `src/test/java/org/eclipse/rdf4j/benchmark/common/`: `ThemeQueryCatalogComplexityTest` (every query needs >= 2 of: OPTIONAL, UNION, FILTER EXISTS, FILTER NOT EXISTS, MINUS, BIND(, VALUES, GROUP BY, HAVING — or be a `SELECT DISTINCT *` shape with >= 6 OPTIONALs and >= 8 ` .` occurrences), `ThemeQueryCatalogOptimizerGapTest` (every query needs `FILTER(?opt`-style filtering on an optional variable, or `||`, or ` IN `, or one of two denormalized shapes), `ThemeQueryCatalogExpansionTest` (13 queries per theme), `ThemeQueryCatalogExpectedCountTest` (hard-coded maps of the expected counts per theme — must be extended when adding a theme).
2. `core/sail/memory` — the in-memory store. Its tests already depend on benchmark-common (see `src/test/java/org/eclipse/rdf4j/sail/memory/benchmark/ThemeQueryBenchmark.java`). Because MemoryStore evaluates SPARQL with the generic (reference) strategy, it is used here to compute ground-truth counts. New file: `src/test/java/org/eclipse/rdf4j/sail/memory/benchmark/RealEstateThemeQueryCountTest.java`.
3. `core/sail/lmdb` — the LMDB store with the native query engine this theme targets. Nothing in it needs changing: its `benchmark/ThemeQueryBenchmark.java` loads all `Theme.values()` datasets and takes the theme name as a JMH parameter, so the new theme is automatically available as `-p themeName=REAL_ESTATE -p z_queryIndex=0..12`.

What the native LMDB engine supports today (from reading `LmdbNativeQueryCompiler.java` and `LmdbNativeAggregateCompiler.java` on the `optimize-lmdb` branch): joins of statement patterns; LeftJoin/Union/Difference/Values/EXISTS/NOT EXISTS — but only under an aggregation root (`Group` or `Filter(Group)`); aggregate function COUNT (plain and DISTINCT) only; ID-level filters (BOUND, sameTerm, =/!= against IRI constants, IN over constants, isIRI-style type checks, and AND/OR/NOT over those); other filter expressions run per-row through the generic evaluator inside the native plan (slow-ish but native). Everything else falls back entirely: ORDER BY, LIMIT/OFFSET, DISTINCT at the root, property paths, subqueries outside EXISTS, BIND of any non-variable expression, and every aggregate other than COUNT (SUM, AVG, MIN, MAX, GROUP_CONCAT, SAMPLE). Additionally the "factorized tail aggregation" fast path (see `execplan-factorized-tail-aggregation.md`) is disabled by a filter on the tail pattern's depth or by aggregates that read tail slots.

The 13 REAL_ESTATE queries map to those gaps as follows (index: name — gap targeted):

    0  avg price per district           — AVG + HAVING(AVG): non-COUNT aggregate, full fallback
    1  price statistics for large homes — SUM/MIN/MAX multi-aggregate: full fallback
    2  top listings by price            — ORDER BY + LIMIT: full fallback
    3  districts with unsold stock      — DISTINCT at root (non-aggregate): full fallback
    4  affordable or spacious unsold    — COALESCE in FILTER: native plan with generic per-row filter
    5  price per square meter           — BIND(?price/?area): Extension with computed expr, fallback
    6  viewing agents in own district   — property path (re:viewingOf/re:inDistrict): fallback
    7  stale expensive listings         — IN + || + NOT EXISTS + MINUS: native, exercises generic-filter slow path
    8  agents with high avg feedback    — GROUP BY subquery under non-aggregate root: fallback
    9  district offer counts            — COUNT DISTINCT with range filter at tail depth: native, factorized tail disabled
    10 amenity exposure                 — SUM + UNION + HAVING(SUM): fallback
    11 denormalized dossier view        — nested OPTIONALs + DISTINCT * + ORDER BY: fallback, huge row surface
    12 union-heavy sparse entity view   — UNION root + 9 OPTIONALs, non-aggregate: fallback

The generated dataset (defaults; all counts jittered +/-50% by `jitterInt`, all probabilities exact):

    District  150; name always.
    Agent     2500; name always; phone 30%; worksIn district 80%.
    Amenity   400; name always.
    Property  60000; rdf:type + inDistrict always; listedBy 70%; price (xsd:int, 50k-1.5M) 60%;
              floorArea (xsd:int, 20-350) 55%; yearBuilt (xsd:int, 1900-2024) 50%;
              energyRating ("A".."G") 25%; description (random sentence) 20%; renovatedIn 10%;
              soldDate (xsd:date in 2024) 15%; hasAmenity links: 35% of properties get 1-4 links.
    Viewing   25% of properties get 1-3 viewings: viewingOf, byAgent, onDate always; feedbackScore (1-10) 40%.
    Offer     20% of properties get 1-2 offers: offerFor, amount always; madeOn 70%; accepted 30%.

`ThemeQueryBenchmark` verification semantics (already implemented, no change needed): when a catalog entry's expected count is 1 and the single row binds `?count`, the benchmark compares the `?count` value against `expectedCountBindingValueFor`; otherwise it compares the result-row count against `expectedCountFor`.

## Plan of Work

Milestone 1 — generator. In `ThemeDataSetGenerator.java`: add `REAL_ESTATE` to `Theme`; add `REALESTATE_NS = BASE + "realestate/"`; add `realEstateConfig()` factory; add a `case REAL_ESTATE` to `generate(Theme, RDFHandler)`; add `generateRealEstate(RealEstateConfig)` (Model convenience) and `generateRealEstate(RealEstateConfig, RDFHandler)` implementing the dataset described above in a single pass (districts, agents, amenities first; then properties with probability-gated attributes; viewings and offers nested in the property loop); add `RealEstateConfig` as a final class near the other configs, with `withXxx` builders for all counts and probabilities, `withSeed`, and `validate()` (counts positive via the existing `requirePositive`, probabilities in [0,1] via a new `requireProbability` helper). In `ThemeDataSetGeneratorTest.java` add `realEstateGeneratorProducesSparseProperties`: assert the model is non-empty, contains `re:Property`, `re:Viewing`, `re:Offer` types, and — the sparsity contract — that the number of `re:price` triples is strictly between 25% and 90% of the number of Property instances, and that `re:soldDate` triples are fewer than 30% of properties.

Milestone 2 — catalog. In `ThemeQueryCatalog.java`: add a `realEstatePrefix` and `QUERIES.put(Theme.REAL_ESTATE, List.of(...13 queries...))` block (exact query text in "Artifacts and Notes" below), plus a `EXPECTED_COUNT_BINDING_VALUES.put(Theme.REAL_ESTATE, expectedCountBindingValues(...))` line in `registerExpectedCountBindingValues()`. Initially use 0 for unknown expected counts and -1 for all binding values; they will be corrected in Milestone 3 (the static validator only checks array sizes and that binding-value queries project `?count`).

Milestone 3 — ground truth. Add `RealEstateThemeQueryCountTest` in `core/sail/memory/src/test/java/org/eclipse/rdf4j/sail/memory/benchmark/`. The test loads `ThemeDataSetGenerator.generate(Theme.REAL_ESTATE)` into a `SailRepository(new MemoryStore())`, then for each of the 13 queries counts rows (and captures the `?count` binding when a single row binds it), prints `index / rows / countBinding` to stdout, and asserts against `ThemeQueryCatalog.expectedCountFor` / `expectedCountBindingValueFor`. Run it once expecting failures, read the printed actuals from the surefire output, bake them into `ThemeQueryCatalog` (both the per-query expected count argument and the binding-value array) and into `ThemeQueryCatalogExpectedCountTest`'s two maps, then re-run until green.

Milestone 4 — verification. Run `scripts/checkCopyrightPresent.sh`, the formatter (`mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources`), then `python3 .codex/skills/mvnf/scripts/mvnf.py testsuites/benchmark-common` and `python3 .codex/skills/mvnf/scripts/mvnf.py RealEstateThemeQueryCountTest`. Update this plan's Progress/Outcomes.

## Concrete Steps

All commands run from the repository root `/Users/havardottestad/Documents/Programming/rdf4j`.

    # once per session
    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

    # after each milestone's edits
    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
    (cd scripts && ./checkCopyrightPresent.sh)

    # milestone 1+2 verification
    python3 .codex/skills/mvnf/scripts/mvnf.py testsuites/benchmark-common

    # milestone 3: run the ground-truth harness, read printed actual counts from
    # core/sail/memory/target/surefire-reports/*RealEstateThemeQueryCountTest*-output.txt
    python3 .codex/skills/mvnf/scripts/mvnf.py RealEstateThemeQueryCountTest --retain-logs

Expected transcript for the final ground-truth run (abridged):

    [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

## Validation and Acceptance

Acceptance is behavioral:

1. `python3 .codex/skills/mvnf/scripts/mvnf.py testsuites/benchmark-common` passes: the catalog exposes 13 REAL_ESTATE queries that satisfy the complexity and optimizer-gap format gates and the hard-coded expected-count maps.
2. `python3 .codex/skills/mvnf/scripts/mvnf.py RealEstateThemeQueryCountTest` passes: every expected count in the catalog matches actual MemoryStore query results for the generated dataset.
3. The LMDB benchmark can name the new theme: `ThemeQueryCatalog.queryFor(Theme.REAL_ESTATE, 0)` returns SPARQL, and `ThemeQueryBenchmark` (core/sail/lmdb) accepts `-p themeName=REAL_ESTATE` without code changes (it uses `Theme.valueOf`).

## Idempotence and Recovery

All edits are additive (new enum constant, new methods, new catalog block, new test files). Re-running the generator or the tests is side-effect free (stores are created under `target/`). If ground-truth counts drift because the generator was edited after counts were baked, re-run Milestone 3 and re-bake. To roll back entirely, revert the touched files; no data migration exists.

## Artifacts and Notes

The exact 13 query texts live in `ThemeQueryCatalog.java` under `QUERIES.put(Theme.REAL_ESTATE, ...)`; the dataset defaults live in `RealEstateConfig`. Both are the single source of truth; this plan intentionally does not duplicate the final query strings (see the gap-target table above for their intent).

Final ground-truth actuals (MemoryStore, defaults, seed 42), baked into the catalog on 2026-07-03:

    idx  rows    ?count  (query)
    0    35      -       average price per district
    1    1       -       price statistics for large homes
    2    25      -       top listings by price
    3    75      -       districts with unsold expensive stock
    4    1       39314   affordable or spacious unsold properties
    5    75      -       price per square meter by district
    6    1       333     viewing agents working in the property district
    7    1       18524   stale expensive listings
    8    1       519     agents with high average feedback
    9    75      -       district offer counts above threshold
    10   520     -       amenity exposure of active listings
    11   99720   -       denormalized property dossier view
    12   108771  -       union-heavy sparse entity view

## Interfaces and Dependencies

No new Maven dependencies. New/changed public surface:

In `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/rio/util/ThemeDataSetGenerator.java`:

    public enum Theme { ..., REAL_ESTATE }
    public static RealEstateConfig realEstateConfig()
    public static Model generateRealEstate(RealEstateConfig config)
    public static void generateRealEstate(RealEstateConfig config, RDFHandler handler)
    public static final class RealEstateConfig {
        RealEstateConfig withDistrictCount(int)
        RealEstateConfig withAgentCount(int)
        RealEstateConfig withAmenityCount(int)
        RealEstateConfig withPropertyCount(int)
        RealEstateConfig withPriceProbability(double)        // and the other with*Probability builders
        RealEstateConfig withSeed(long)
    }

In `testsuites/benchmark-common/src/main/java/org/eclipse/rdf4j/benchmark/common/ThemeQueryCatalog.java`: a new `QUERIES` entry and `EXPECTED_COUNT_BINDING_VALUES` entry for `Theme.REAL_ESTATE`; no signature changes.

New test files: `testsuites/benchmark-common/src/test/java/org/eclipse/rdf4j/benchmark/rio/util/ThemeDataSetGeneratorTest.java` (extended, not new) and `core/sail/memory/src/test/java/org/eclipse/rdf4j/sail/memory/benchmark/RealEstateThemeQueryCountTest.java` (new, with the standard copyright header and a `// Some portions generated by Claude Code` signature line).

Revision note (2026-07-03, Claude Code): initial version of the plan, written after research and before implementation. Revision note (2026-07-03, Claude Code): updated at completion — Progress checked off, Surprises & Discoveries and Outcomes & Retrospective filled in, final ground-truth counts recorded in Artifacts and Notes.
