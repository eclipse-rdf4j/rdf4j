AGENTS.md

Welcome, AI Agent! Your persistence, curiosity, and craftsmanship make a difference. Take your time, work methodically, validate thoroughly, and iterate. This repository is large and tests can take time — that’s expected and supported.

You need to read the entire AGENTS.md file and follow all instructions exactly. Keep this fresh in your context as you work.

> **Timebox:** Aim to complete each autonomous run in **15–30 minutes**.

---

## Read‑Me‑Now: Zero‑Exception Test‑First Rule (Stricter)

**You may not touch production code until a smallest‑scope failing automated test exists inside this repo and you have captured its report snippet.**
No exceptions. A user‑provided stack trace or “obvious” contract violation is **not** a substitute for a failing test in the repository.

**Auto‑stop:** If you realize you patched production before creating the failing test, **stop**, revert the patch, and resume from “Reproduce first”.

**Traceability trio (must appear in your handoff):**

1. **Preamble** (what you’re about to do + exact commands)
2. **Evidence** (surefire/failsafe snippet from this repo)
3. **Plan** (one and only one `in_progress` step)

It is illegal to `-am` when running tests!
It is illegal to `-q` when running tests!

---

## Purpose & Contract

* **Bold goal:** deliver correct, minimal, well‑tested changes with clear handoff. No monkey‑patching or band‑aid fixes — always fix the underlying problem at its source.
* **Bias to action:** when inputs are ambiguous, choose a reasonable path, state assumptions, and proceed.
* **Ask only when blocked or irreversible:** escalate only if truly blocked (permissions, missing deps, conflicting requirements) or if a choice is high‑risk/irreversible.
* **Definition of Done**

    * Code formatted and imports sorted.
    * Compiles with a quick profile / targeted modules.
    * Relevant module tests pass; failures triaged or crisply explained.
    * Only necessary files changed; headers correct for new files.
    * Clear final summary: what changed, why, where, how verified, next steps.
    * **Evidence present:** failing test output (pre‑fix) and passing output (post‑fix) are both shown.

### No Monkey‑Patching or Band‑Aid Fixes (Non‑Negotiable)

This repository requires durable, root‑cause fixes. Superficial changes that mask symptoms, mute tests, or add ad‑hoc toggles are not acceptable.

What this means in practice

* Find and fix the root cause in the correct layer/module.
* Add or adjust targeted tests that fail before the fix and pass after.
* Keep changes minimal and surgical; do not widen APIs/configs to “make tests green”.
* Maintain consistency with existing style and architecture; prefer refactoring over hacks.

Strictly avoid

* Sleeping/timeouts to hide race conditions or flakiness.
* Broad catch‑and‑ignore or logging‑and‑continue of exceptions.
* Muting, deleting, or weakening assertions in tests to pass builds.
* Reflection or internal state manipulation to bypass proper interfaces.
* Feature flags/toggles that disable validation or logic instead of fixing it.
* Changing public APIs or configs without necessity and clear rationale tied to the root cause.

Preferred approach (fast and rigorous)

* Reproduce the issue and isolate the smallest failing test (class → method).
* Trace to the true source; fix it in the right module.
* Add focused tests covering the behavior and any critical edge cases.
* Run tight, targeted verifies for the impacted module(s) and broaden scope only if needed.

Review bar and enforcement

* Treat this policy as a blocking requirement. Changes that resemble workarounds will be rejected.
* Your final handoff must demonstrate: failing test before the fix, explanation of the root cause, minimal fix at source, and passing targeted tests after.

---

## Enforcement & Auto‑Fail Triggers 

Your run is **invalid** and must be restarted from “Reproduce first” if any of the following occur:

* You modify production code before adding and running the smallest failing test in this repo.
* You proceed without pasting a surefire/failsafe report snippet from `target/*-reports/`.
* Your plan does not have **exactly one** `in_progress` step.
* You run tests using `-am` or `-q`.
* You treat a narrative failure description or external stack trace as equivalent to an in‑repo failing test.

**Recovery procedure:**
Update the plan (`in_progress: create failing test`), post a preamble, create the failing test, run it, capture the report snippet, then resume.

---

## Preamble & Evidence Protocol (Mandatory)

Before any grouped actions (builds, tests, patches), post a **short preamble**:

**Preamble template**

```
Preamble: Reproduce bug at smallest scope.
Module: <module>
Commands:
  mvn -o -pl <module> -Dtest=Class#method verify | tail -500
Expectation: test fails with the reported error.
```

After each grouped action, post an **Evidence block**:

**Evidence template**

```
Evidence:
Command: mvn -o -pl <module> -Dtest=Class#method verify
Report: <module>/target/surefire-reports/<file>.txt
Snippet:
<copy 10–30 lines capturing the failure or success summary>
```

---

## Living Plan Protocol (Sharper)

Maintain a **living plan** with checklist items (5–7 words each). Keep **exactly one** `in_progress`.

**Plan format**

```
Plan
- [done] sanity build quick profile
- [in_progress] add smallest failing test
- [todo] minimal root-cause fix
- [todo] rerun focused then module tests
- [todo] format, verify, summary
```

**Rule:** If you deviate, update the plan **first** (switch `in_progress`), then proceed. Do not let plan and actions drift out of sync.

---

## Environment

* **JDK:** 11 (minimum). The project builds and runs on Java 11+.
* **Maven default:** run **offline** using `-o` whenever possible.
* **Network:** only when needed to fetch missing deps/plugins; then rerun the exact command **without** `-o` once, and return to offline.
* **Large project:** some module test suites can take **5–10 minutes**. Be patient, but bias toward **targeted** runs to keep momentum.

### Maven `-am` usage (house rule)

`-am` (also-make) pulls in required upstream modules. That’s helpful for **compiles**, but hazardous for **tests**: Maven will advance included modules to the same lifecycle phase and run **their** tests too.

**Rule of thumb**

* ✅ Use `-am` **only** for compile/verify with tests skipped (e.g. `-Pquick`).:

    * `mvn -o -pl <module> -am -Pquick install`
* ❌ Do **not** use `-am` with `verify` when tests are enabled.

**Two-step pattern (fast + safe)**

1. **Compile deps fast (skip tests):**
   `mvn -o -pl <module> -am -Pquick install`
2. **Run tests:**
   `mvn -o -pl <module> verify | tail -500`

It is illegal to `-am` when running tests!
It is illegal to `-q` when running tests!

---

## Always Install Before Tests (Required)

The Maven reactor resolves inter-module dependencies from the local Maven repository (`~/.m2/repository`).
Running `install` publishes your changed modules there so downstream modules and tests pick up the correct versions.

- Always run `mvn -o -Pquick install | tail -200` before you start working. This command typically takes between 10 and 30 seconds.
- Always run `mvn -o -pl <module> -am -Pquick install | tail -200` before any `verify` or test runs.
- If offline resolution fails due to a missing dependency or plugin, rerun the exact `install` command once without `-o`, then return offline.
- Skipping this step can lead to stale or missing artifacts during tests, producing confusing compilation or linkage errors.
- Never ever change the repo location. Never use `-Dmaven.repo.local=.m2_repo`. Instead, ask for permission the first time you run `mvn -o -Pquick install | tail -200`.
---

## Quick Start (First 10 Minutes)

1. **Discover**

    * List modules: inspect root `pom.xml` (aggregator) and the module tree (see “Maven Module Overview” below).
    * Search fast with ripgrep: `rg -n "<symbol or string>"`
2. **Build sanity (fast, skip tests)**

    * **Preferred:** `mvn -o -Pquick install | tail -200`
    * **Alternative:** `mvn -o -Pquick install | tail -200`
    * This step is required before any tests. It installs artifacts to `~/.m2` so the reactor resolves fresh inter-module dependencies.
3. **Format (Java, imports, XML)**

    * `mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format`
4. **Targeted tests (tight loops)**

    * By module: `mvn -o -pl <module> verify  | tail -500`
    * Single class: `mvn -o -pl <module> -Dtest=ClassName verify  | tail -500`
    * Single method: `mvn -o -pl <module> -Dtest=ClassName#method verify | tail -500`
    * Prerequisite: ensure `mvn -o -Pquick install` (root or `-pl <module> -am`) has just run so artifacts are available in `~/.m2`.
5. **Inspect failures**

    * **Unit (Surefire):** `<module>/target/surefire-reports/`
    * **IT (Failsafe):** `<module>/target/failsafe-reports/`

It is illegal to `-am` when running tests!
It is illegal to `-q` when running tests!

---

## Bugfix Workflow (Mandatory)

* **Reproduce first:** write the smallest focused test (class/method) that reproduces the reported bug **inside this repo**. Run it and confirm it fails with the same error/stacktrace. **Do not proceed without this.**
* **Keep the test as‑is:** do not weaken assertions or mute the failure. The failing test is your proof you’ve hit the right code path.
* **Fix at the root:** implement the minimal, surgical change in the correct module that addresses the underlying cause (no band‑aids).
* **Verify locally:** re‑run the focused test, then the surrounding module’s tests. Use targeted Maven invocations (class/method → module). Avoid `-am` with tests.
* **Broaden if needed:** only after green targeted runs, expand scope to neighboring modules when changes cross boundaries.
* **Document clearly:** in your final handoff, show the failing test before the fix, the root cause, the minimal fix, and passing tests after. Include **preamble** and **evidence** blocks.

### Hard Gates (Do Not Proceed Unless True)

* A failing test exists at the smallest scope (method/class) reproducing the report.

    * Show the failing command and include a snippet of the error/stack from `target/*-reports/`.
* **No production patch before the failing test is observed and recorded.**
* Test runs avoid `-am` and `-q`.

    * Use `-am` only with `-Pquick` to compile deps with tests skipped, then run tests without `-am`.
* Maintain a living plan with exactly one `in_progress` step; send a short preamble before long actions.

### Required Sequence

1. **Reproduce first**

    * Add the smallest failing test in the correct module.
    * Run it directly: `mvn -o -pl <module> -Dtest=Class#method verify | tail -500`
    * Inspect `target/surefire-reports/` (or `target/failsafe-reports/`) and capture the failure.
2. **Fix at the root (minimal, surgical)**

    * Change the correct layer; avoid widening APIs/configs.
3. **Verify locally (tight loops)**

    * Re-run the exact test selection; then run the whole module.
4. **Broaden only if necessary**

    * Expand scope when changes cross module boundaries or neighbors fail.
5. **Document clearly**

    * Include: failing output (pre‑fix), root cause, minimal fix, passing output (post‑fix).

### Quick Self‑Check Before First Code Patch

1. Do I have a failing test and its report snippet saved?
2. Am I using legal Maven flags for tests (no `-am`, no `-q`)?
3. Is my next step in the plan marked `in_progress` and did I state a preamble?
4. Is my fix located at the correct source of truth, not a workaround?

---

## Working Loop

* **Plan**

    * Break task into **small, verifiable steps**; keep one step in progress.
    * Announce a short preamble before long actions (builds/tests).
    * Decide and proceed autonomously; document assumptions inline.
* **Change**

    * Make minimal, surgical edits. Keep style and structure consistent.
* **Format**

    * `mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format`
* **Compile (fast)**

    * **Iterate locally:** `mvn -o -pl <module> -am -Pquick install | tail -500`
* **Test**

    * Start with the smallest scope that exercises your change (class → module).
    * For integration‑impacted changes, run module `verify` (includes ITs).
* **Triage**

    * Read reports; fix root cause; expand scope **only when needed**.
* **Iterate**

    * Keep moving without waiting for permission between steps. Escalate only at blocking points.
    * Repeat until **Definition of Done** is satisfied.

It is illegal to `-am` when running tests!
It is illegal to `-q` when running tests!

---

## Testing Strategy

* **Prefer module tests you touched:** `-pl <module>`
* **Narrow further** to a class/method for tight loops; then broaden to the module.
* **Expand scope** when:

    * Your change crosses module boundaries, or
    * Neighbor module failures indicate integration impact.
* **Read reports**

    * Surefire (unit): `target/surefire-reports/`
    * Failsafe (IT): `target/failsafe-reports/`
* **Helpful flags**

    * `-Dtest=Class#method` (unit selection)
    * `-Dit.test=ITClass#method` (integration selection)
    * `-DtrimStackTrace=false` (full traces)
    * `-DskipITs` (focus on unit tests)
    * `-DfailIfNoTests=false` (when selecting a class that has no tests on some platforms)

### Optional: Redirect test stdout/stderr to files

To help automated agents inspect what a test printed to the console, you may redirect `System.out`/`System.err` to per‑class files generated by Surefire/Failsafe. This is **optional** and should be used only when it aids triage—house rules still apply (no `-am` with tests, no `-q`).

**Unit tests (Surefire):**

```bash
mvn -o -pl <module> -Dtest=ClassName[#method] -Dmaven.test.redirectTestOutputToFile=true verify | tail -500
```

Logs will appear under:

```
<module>/target/surefire-reports/ClassName-output.txt
```

**Integration tests (Failsafe):**

```bash
mvn -o -pl <module> -Dit.test=ITClassName[#method] -Dmaven.test.redirectTestOutputToFile=true verify | tail -500
```

Logs will appear under:

```
<module>/target/failsafe-reports/ITClassName-output.txt
```

Notes:
* Capture is **per test class**, not per method. Multiple methods in the same class share one `*-output.txt`.
* Only output actually written to the console is captured. If your logging configuration writes solely to files, you won’t see it here.
* Continue to include the normal **Evidence** snippet from the Surefire/Failsafe report. You may additionally quote lines from the corresponding `*-output.txt` when useful for debugging.

---

## Assertions: Make invariants explicit

Assertions are executable claims about what must be true. They’re the fastest way to surface “impossible” states and to localize bugs at the line that crossed a boundary it had no business crossing. Use them both as **temporary tripwires** during investigation and as **permanent contracts** once an invariant is known to matter.

**Two useful flavors**

* **Temporary tripwires (debug asserts):** Add while hunting a failing test or weird behavior. Keep them cheap, contextual, and local to the suspect path. Remove after the mystery is solved **or** convert to permanent checks if the invariant is genuinely important.
* **Permanent contracts:** Encode **preconditions** (valid inputs), **postconditions** (valid outputs), and **invariants** (state that must always hold). These stay and prevent regressions.

**Where to add assertions**

* At **module boundaries** and **after parsing/external calls** (validate assumptions about returned/decoded data).
* Around **state transitions** (illegal transitions should fail loudly).
* In **concurrency hotspots** (e.g., “lock must be held”, “no concurrent mutation”).
* Before/after **caching, batching, or memoization** (keys, sizes, ordering, monotonicity).
* For **exhaustive enums** in `switch` statements (treat unexpected values as hard errors).

**How to write good assertions**

* One fact per assert. Fail **fast**, fail **usefully**.
* Include **stable context** in the message (ids, sizes, states) so the failure is self‑explanatory.
* Avoid side effects in the condition or message. Assertions may be disabled in some runtimes.
* Keep them **cheap**: no I/O, heavy allocations, or deep logging in the message.
* Don’t use asserts for **user‑facing validation**. Raise exceptions for expected bad inputs.

**Java specifics**

* **Enable VM assertions in tests.** Tests must run with `-ea` so `assert` is active.
* Use **`assert`** for debug‑only invariants that “cannot happen.” Use **exceptions** for runtime guarantees:

    * Preconditions: `IllegalArgumentException` / `Objects.requireNonNull` (or Guava `Preconditions` if present).
    * Invariants: `IllegalStateException`.
* Prefer treating unexpected enum values as **hard errors** rather than adding a quiet `default` path.

**Concrete examples**

Precondition (permanent)

```java
void setPort(int port) {
  if (port < 1 || port > 65_535) {
    throw new IllegalArgumentException("port out of range: " + port);
  }
  this.port = port;
}
```

Invariant (permanent)

```java
void advance(State next) {
  if (!allowedTransitions.get(state).contains(next)) {
    throw new IllegalStateException("Illegal transition " + state + " → " + next);
  }
  state = next;
}
```

Debug tripwire (temporary; remove or convert later)

```java
// Narrow a flaky failure around ordering
assert isSorted(results) : "unsorted results, size=" + results.size() + " ids=" + ids(results);
```

Unreachable (hard error)

```java
switch (kind) {
  case A: return handleA();
  case B: return handleB();
  default:
    throw new IllegalStateException("Unhandled kind: " + kind);
}
```

Concurrency assumption

```java
synchronized void put(String k, String v) {
  assert Thread.holdsLock(this) : "put must hold instance monitor";
  // ...
}
```

House rule: Asserts are allowed and encouraged. Removing or weakening an assertion to “make it pass” is strictly forbidden — fix the cause, not the guardrail.

---

## Triage Playbook

* **Missing dep/plugin offline**

    * Remedy: **rerun the exact command without `-o`** once to fetch; then return offline.
* **Compilation errors**

    * Fix imports, generics, visibility; re‑run quick install (skip tests) in the **module**.
* **Flaky/slow tests**

    * Run the specific failing test; read its report; stabilize root cause before broad runs.
* **Formatting failures**

    * Run formatter/import/XML sort; re‑verify.
* **License header missing**

    * Add header for **new** files only (see “Source File Headers”); **do not** change years on existing files.

---

## Code Formatting

* **Always run before finalizing:**

    * `mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format`
* **Style:** no wildcard imports; 120‑char width; curly braces always; LF line endings.
* **Tip:** formatting/import sort may be validated during `verify`. Running the commands proactively avoids CI/style failures.

---

## Source File Headers

Use this exact header for **new Java files only** (replace `${year}` with current year):

```
/*******************************************************************************
 * Copyright (c) ${year} Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
```

Use this exact header. Be very precise.

Do **not** modify existing headers’ years.

---

## Pre‑Commit Checklist

* **Format:** `mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format`
* **Compile (fast path):** `mvn -o -Pquick install | tail -200`
* **Tests (targeted):** `mvn -o -pl <module> verify | tail -500` (broaden scope if needed)
* **Reports:** zero new failures in `target/surefire-reports/` or `target/failsafe-reports/`, or explain precisely.
* **Evidence:** include pre‑fix failing snippet and post‑fix passing summary.

---

## Branching & Commit Conventions

- Branch names: start with `GH-XXXX` where `XXXX` is the GitHub issue number. Prefer a short, kebab‑case slug after the number when helpful, e.g., `GH-1234-add-trig-writer-check`.
- Commit messages: start with the same prefix, `GH-XXXX <short summary>`, on every commit in the branch.
- Keep summaries concise, in imperative mood (e.g., “Fix NPE in TriG writer”).
- Example:
  - Branch: `GH-1234-add-shacl-validation-metric`
  - Commit: `GH-1234 Fix NPE when serializing empty graph`

---

## Branch & PR Workflow (Agent)

- Name branch: `GH-<issue>-<short-slug>` (kebab‑case slug).
- Create branch: `git checkout -b GH-XXXX-your-slug`.
- Stage changes: `git add -A` (ensure new Java files have the required header).
- Optional but recommended: run format + quick install.
  - `mvn -o -q -T 2C formatter:format impsort:sort xml-format:xml-format`
  - `mvn -o -Pquick install | tail -200`
- Commit: `git commit -m "GH-XXXX <short imperative summary>"`.
- Push branch: `git push -u origin GH-XXXX-your-slug`.
- Create PR using default template:
  - Preferred: `gh pr create --title "GH-XXXX <summary>" --body-file .github/pull_request_template.md`
  - Fallback: `gh pr create --title "GH-XXXX <summary>" --body "$(cat .github/pull_request_template.md)"`
- Immediately fill the template (do not leave placeholders):
  - Set `GitHub issue resolved: #XXXX`.
  - Write a short, accurate change summary (what/why).
  - Tick applicable checklist items only (self-contained, tests, squashed, commit message prefix, formatting if run).
  - Include `Fixes #XXXX` to auto-close the issue on merge.
- Target the repo default branch (e.g., `origin/HEAD`).

---

## Navigation & Search

* Fast file search: `rg --files`
* Fast content search: `rg -n "<pattern>"`
* Read big files in chunks:

    * `sed -n '1,200p' path/to/File.java`
    * `sed -n '201,400p' path/to/File.java`

---

## Autonomy Rules (Act > Ask)

* **Default:** act with assumptions. Document assumptions in your plan and final answer.
* **Keep going:** chain steps without waiting for permission; send short progress updates before long actions.
* **Ask only when:**

    * Blocked by sandbox/approvals/network policy or missing secrets.
    * The decision is destructive/irreversible, repo‑wide, or impacts public APIs.
    * Adding dependencies, changing build profiles, or altering licensing.
* **Prefer reversible moves:** take the smallest local change that unblocks progress; validate with targeted tests before expanding scope.
* **Choose defaults**

    * **Tests:** start with `-pl <module>`, then `-Dtest=Class#method` / `-Dit.test=ITClass#method`.
    * **Build:** use `-o` quick/profiled commands; briefly drop `-o` to fetch missing deps, then return offline.
    * **Formatting:** run formatter/impsort/xml‑format proactively before verify.
    * **Reports:** read surefire/failsafe locally; expand scope only when necessary.
* **Error handling**

    * On compile/test failure: fix root cause locally, rerun targeted tests, then broaden.
    * On flaky tests: rerun class/method; stabilize cause before repo‑wide runs.
    * On formatting/license issues: apply prescribed commands/headers immediately.
* **Communication**

    * **Preambles:** 1–2 sentences grouping upcoming actions.
    * **Updates:** inform to maintain visibility; do **not** request permission unless in “Ask only when” above.

---

## Answer Template (Use This)

* **What changed:** summary of approach and rationale.
* **Files touched:** list file paths.
* **Commands run:** key build/test commands.
* **Verification:** which tests passed, where you checked reports.
* **Evidence:** failing output (pre‑fix) and passing output (post‑fix) snippets.
* **Assumptions:** key assumptions and autonomous decisions you made.
* **Limitations:** anything left or risky edge cases.
* **Next steps:** optional suggestions for follow‑ups.

---

## Running Tests

* By module:

    * `mvn -o -pl core/sail/shacl verify | tail -500`
* Entire repo:

    * `mvn -o verify` (long; only when appropriate)
* Useful flags:

    * `-Dtest=ClassName`
    * `-Dtest=ClassName#method`
    * `-Dit.test=ITClass#method`
    * `-DtrimStackTrace=false`

---

## Build

* **Build without tests (fast path):**

    * `mvn -o -Pquick install`
* **Verify with tests:**

    * Targeted module(s): `mvn -o -pl <module> verify`
    * Entire repo: `mvn -o verify` (use only when appropriate)
* **When offline fails due to missing deps:**

    * Re‑run the **exact** command **without** `-o` once to fetch, then return to `-o`.

---

## Prohibited Misinterpretations 

* A user stack trace, reproduction script, or verbal description **is not evidence**. You must implement the smallest failing test **inside this repo**.
* “Obvious” contract violations (e.g., iterator returns `null`) still require a failing test first. Assumptions are not substitutes for evidence.
* “Quick fixes” are not quick if they bypass the workflow. They create audit gaps and regressions.

---

## Maven Module Overview

The project is organised as a multi-module Maven build. The diagram below lists
all modules and submodules with a short description for each.

```
rdf4j: root project
├── assembly-descriptors: RDF4J: Assembly Descriptors
├── core: Core modules for RDF4J
    ├── common: RDF4J common: shared classes
    │   ├── annotation: RDF4J common annotation classes
    │   ├── exception: RDF4J common exception classes
    │   ├── io: RDF4J common IO classes
    │   ├── iterator: RDF4J common iterators
    │   ├── order: Order of vars and statements
    │   ├── text: RDF4J common text classes
    │   ├── transaction: RDF4J common transaction classes
    │   └── xml: RDF4J common XML classes
    ├── model-api: RDF model interfaces.
    ├── model-vocabulary: Well-Known RDF vocabularies.
    ├── model: RDF model implementations.
    ├── sparqlbuilder: A fluent SPARQL query builder
    ├── rio: Rio (RDF I/O) is an API for parsers and writers of various RDF file formats.
    │   ├── api: Rio API.
    │   ├── languages: Rio Language handler implementations.
    │   ├── datatypes: Rio Datatype handler implementations.
    │   ├── binary: Rio parser and writer implementation for the binary RDF file format.
    │   ├── hdt: Experimental Rio parser and writer implementation for the HDT file format.
    │   ├── jsonld-legacy: Rio parser and writer implementation for the JSON-LD file format.
    │   ├── jsonld: Rio parser and writer implementation for the JSON-LD file format.
    │   ├── n3: Rio writer implementation for the N3 file format.
    │   ├── nquads: Rio parser and writer implementation for the N-Quads file format.
    │   ├── ntriples: Rio parser and writer implementation for the N-Triples file format.
    │   ├── rdfjson: Rio parser and writer implementation for the RDF/JSON file format.
    │   ├── rdfxml: Rio parser and writer implementation for the RDF/XML file format.
    │   ├── trix: Rio parser and writer implementation for the TriX file format.
    │   ├── turtle: Rio parser and writer implementation for the Turtle file format.
    │   └── trig: Rio parser and writer implementation for the TriG file format.
    ├── queryresultio: Query result IO API and implementations.
    │   ├── api: Query result IO API
    │   ├── binary: Query result parser and writer implementation for RDF4J's binary query results format.
    │   ├── sparqljson: Query result writer implementation for the SPARQL Query Results JSON Format.
    │   ├── sparqlxml: Query result parser and writer implementation for the SPARQL Query Results XML Format.
    │   └── text: Query result parser and writer implementation for RDF4J's plain text boolean query results format.
    ├── query: Query interfaces and implementations
    ├── queryalgebra: Query algebra model and evaluation.
    │   ├── model: A generic query algebra for RDF queries.
    │   ├── evaluation: Evaluation strategy API and implementations for the query algebra model.
    │   └── geosparql: Query algebra implementations to support the evaluation of GeoSPARQL.
    ├── queryparser: Query parser API and implementations.
    │   ├── api: Query language parsers API.
    │   └── sparql: Query language parser implementation for SPARQL.
    ├── http: Client and protocol for repository communication over HTTP.
    │   ├── protocol: HTTP protocol (REST-style)
    │   └── client: Client functionality for communicating with an RDF4J server over HTTP.
    ├── queryrender: Query Render and Builder tools
    ├── repository: Repository API and implementations.
    │   ├── api: API for interacting with repositories of RDF data.
    │   ├── manager: Repository manager
    │   ├── sail: Repository that uses a Sail stack.
    │   ├── dataset: Implementation that loads all referenced datasets into a wrapped repository
    │   ├── event: Implementation that notifies listeners of events on a wrapped repository
    │   ├── http: "Virtual" repository that communicates with a (remote) repository over the HTTP protocol.
    │   ├── contextaware: Implementation that allows default values to be set on a wrapped repository
    │   └── sparql: The SPARQL Repository provides a RDF4J Repository interface to any SPARQL end-point.
    ├── sail: Sail API and implementations.
    │   ├── api: RDF Storage And Inference Layer ("Sail") API.
    │   ├── base: RDF Storage And Inference Layer ("Sail") API.
    │   ├── inferencer: Stackable Sail implementation that adds RDF Schema inferencing to an RDF store.
    │   ├── memory: Sail implementation that stores data in main memory, optionally using a dump-restore file for persistence.
    │   ├── nativerdf: Sail implementation that stores data directly to disk in dedicated file formats.
    │   ├── model: Sail implementation of Model.
    │   ├── shacl: Stacked Sail with SHACL validation capabilities
    │   ├── lmdb: Sail implementation that stores data to disk using LMDB.
    │   ├── lucene-api: StackableSail API offering full-text search on literals, based on Apache Lucene.
    │   ├── lucene: StackableSail implementation offering full-text search on literals, based on Apache Lucene.
    │   ├── solr: StackableSail implementation offering full-text search on literals, based on Solr.
    │   ├── elasticsearch: StackableSail implementation offering full-text search on literals, based on Elastic Search.
    │   ├── elasticsearch-store: Store for utilizing Elasticsearch as a triplestore.
    │   └── extensible-store: Store that can be extended with a simple user-made backend.
    ├── spin: SPARQL input notation interfaces and implementations
    ├── client: Parent POM for all RDF4J parsers, APIs and client libraries
    ├── storage: Parent POM for all RDF4J storage and inferencing libraries
    └── collection-factory: Collection Factories that may be reused for RDF4J
        ├── api: Evaluation
        ├── mapdb: Evaluation
        └── mapdb3: Evaluation
├── tools: Server, Workbench, Console and other end-user tools for RDF4J.
    ├── config: RDF4J application configuration classes
    ├── console: Command line user interface to RDF4J repositories.
    ├── federation: A federation engine for virtually integrating SPARQL endpoints
    ├── server: HTTP server implementing a REST-style protocol
    ├── server-spring: HTTP server implementing a REST-style protocol
    ├── workbench: Workbench to interact with RDF4J servers.
    ├── runtime: Runtime dependencies for an RDF4J application
    └── runtime-osgi: OSGi Runtime dependencies for an RDF4J application
├── spring-components: Components to use with Spring
    ├── spring-boot-sparql-web: HTTP server component implementing only the SPARQL protocol
    ├── rdf4j-spring: Spring integration for RDF4J
    └── rdf4j-spring-demo: Demo of a spring-boot project using an RDF4J repo as its backend
├── testsuites: Test suites for Eclipse RDF4J modules
    ├── model: Reusable tests for Model API implementations
    ├── rio: Test suite for Rio
    ├── queryresultio: Reusable tests for QueryResultIO implementations
    ├── sparql: Test suite for the SPARQL query language
    ├── repository: Reusable tests for Repository API implementations
    ├── sail: Reusable tests for Sail API implementations
    ├── lucene: Generic tests for Lucene Sail implementations.
    ├── geosparql: Test suite for the GeoSPARQL query language
    └── benchmark: RDF4J: benchmarks
├── compliance: Eclipse RDF4J compliance and integration tests
    ├── repository: Compliance testing for the Repository API implementations
    ├── rio: Tests for parsers and writers of various RDF file formats.
    ├── model: RDF4J: Model compliance tests
    ├── sparql: Tests for the SPARQL query language implementation
    ├── lucene: Compliance Tests for LuceneSail.
    ├── solr: Tests for Solr Sail.
    ├── elasticsearch: Tests for Elasticsearch.
    └── geosparql: Tests for the GeoSPARQL query language implementation
├── examples: Examples and HowTos for use of RDF4J in Java
├── bom: RDF4J Bill of Materials (BOM)
└── assembly: Distribution bundle assembly
```

## Safety & Boundaries

* Don’t commit or push unless explicitly asked.
* Don’t add new dependencies without explicit approval.

It is illegal to `-am` when running tests!
It is illegal to `-q` when running tests!
You must follow these rules and instructions exactly as stated.
