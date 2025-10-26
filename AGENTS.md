# AGENTS.md

Welcome, AI Agent! Your persistence, curiosity, and craftsmanship make a difference. Take your time, work methodically, validate thoroughly, and iterate. This repository is large and tests can take time — that’s expected and supported.

You need to read the entire AGENTS.md file and follow all instructions exactly. Keep this fresh in your context as you work.

---

## Read‑Me‑Now: Proportional Test‑First Rule (Default)

**Default:** Use **test‑first (TDD)** for any change that alters externally observable behavior.

**Proportional exceptions:** You may **skip writing a new failing test** *only* when **all** Routine B gates (below) pass, or when using Routine C (Spike/Investigate) with **no production code changes**.

**You may not touch production code for behavior‑changing work until a smallest‑scope failing automated test exists inside this repo and you have captured its report snippet.** A user‑provided stack trace or “obvious” contract violation is **not** a substitute for an in‑repo failing test.

**Auto‑stop:** If you realize you patched production before creating/observing the failing test for behavior‑changing work, **stop**, revert the patch, and resume from “Reproduce first”.

**Traceability trio (must appear in your handoff):**
1. **Descritpion** (what you’re about to do)
2. **Evidence** (Surefire/Failsafe snippet from this repo)
3. **Plan** (one and only one `in_progress` step)

It is illegal to `-am` when running tests!
It is illegal to `-q` when running tests!

> **Clarification:** For **strictly behavior‑neutral refactors** that are already **fully exercised by existing tests**, or for **bugfixes with an existing failing test**, you may use **Routine B — Change without new tests**. In that case you must capture **pre‑change passing evidence** at the smallest scope that hits the code you’re about to edit, prove **Hit Proof**, then show **post‑change passing evidence** from the **same selection**.
> **No exceptions for any behavior‑changing change** — for those, you must follow **Routine A — Full TDD** or **Routine D — ExecPlans**.

---

## Four Routines: Choose Your Path

**Routine A — Full TDD (Default)**
**Routine B — Change without new tests (Proportional, gated)**
**Routine C — Spike/Investigate (No production changes)**
**Routine D — ExecPlans: Complex features or significant refactors**

### Decision quickstart

1. **Is ExecPlans required (complex feature, significant refactor or requested by the user)?**
   → **Yes:** **Routine D (ExecPlans)**. Use an ExecPlan (as described in .agent/PLANS.md) from design to implementation.
   → **No:** continue.

2**Is new externally observable behavior required?**
   → **Yes:** **Routine A (Full TDD)**. Add the smallest failing test first.
   → **No:** continue.

3**Does a failing test already exist in this repo that pinpoints the issue?**
   → **Yes:** **Routine B (Bugfix using existing failing test).**
   → **No:** continue.

4**Is the edit strictly behavior‑neutral, local in scope, and clearly hit by existing tests?**
   → **Yes:** **Routine B (Refactor/micro‑perf/documentation/build).**
   → **No or unsure:** continue.

5**Is this purely an investigation/design spike with no production code changes?**
   → **Yes:** **Routine C (Spike/Investigate).**
   → **No or unsure:** **Routine A.**

**When in doubt, choose Routine A (Full TDD).** Ambiguity is risk; tests are insurance.

---

## ExecPlans

When writing complex features or significant refactors, use an ExecPlan (as described in PLANS.md) from design to implementation.

## PIOSEE Decision Model (Adopted)

Use this as a compact, repeatable loop for anything from a one‑line bug fix to a multi‑quarter program.

### P — **Problem**

**Goal:** State the core problem and what “good” looks like.
**Ask:** Who’s affected? What outcome is required? What happens if we do nothing?
**Tip:** Include measurable target(s): error rate ↓, latency p95 ↓, revenue ↑, risk ↓.

### I — **Information**

**Goal:** Gather only the facts needed to move.
**Ask:** What do logs/metrics/user feedback say? What constraints (security, compliance, budget, SLA/SLO)? What assumptions must we test?

### O — **Options**

**Goal:** Generate viable ways forward, including “do nothing.”
**Ask:** What are 2–4 distinct approaches (patch, redesign, buy vs. build, defer)? What risks, costs, and second‑order effects?
**Tip:** Check guardrails: reliability, security/privacy, accessibility, performance, operability, unit economics.

### S — **Select**

**Goal:** Decide deliberately and document why.
**Ask:** Which option best meets the success criteria under constraints? Who is the decision owner? What’s the fallback/abort condition?
**Tip:** Use lightweight scoring (e.g., Impact×Confidence÷Effort) to avoid bike‑shedding.

### E — **Execute**

**Goal:** Ship safely and visibly.
**Ask:** What is the smallest safe slice? How do we de‑risk (feature flag, canary, dark launch, rollback)? Who owns what?
**Checklist:** Traces/logs/alerts; security & privacy checks; docs & changelog; incident plan if relevant.

### E — **Evaluate**

**Goal:** Verify outcomes and learn.
**Ask:** Did metrics hit targets? Any regressions or side effects? What will we keep/change next loop?
**Output:** Post‑release review (or retro), decision log entry, follow‑ups (tickets), debt captured.
**Tip:** If outcomes miss, either **iterate** (new Options) or **reframe** (back to Problem).

---

## Proportionality Model (Think before you test)

Score the change on these lenses. If any are **High**, prefer **Routine A or D**.

- **Behavioral surface:** affects outputs, serialization, parsing, APIs, error text, timing/order?
- **Blast radius:** number of modules/classes touched; public vs internal.
- **Reversibility:** quick revert vs migration/data change.
- **Observability:** can existing tests or assertions expose regressions?
- **Coverage depth:** do existing tests directly hit the edited code?
- **Concurrency / IO / Time:** any risk here is **High** by default.

---

## Purpose & Contract

* **Bold goal:** deliver correct, minimal, well‑tested changes with clear handoff. Fix root causes; avoid hacks.
* **Bias to action:** when inputs are ambiguous, choose a reasonable path, state assumptions, and proceed.
* **Ask only when blocked or irreversible:** permissions, missing deps, conflicting requirements, destructive repo‑wide changes.
* **Definition of Done**
    * Code formatted and imports sorted.
    * Compiles with a quick profile / targeted modules.
    * Relevant module tests pass; failures triaged or crisply explained.
    * Only necessary files changed; headers correct for new files.
    * Clear final summary: what changed, why, where, how verified, next steps.
    * **Evidence present:** failing test output (pre‑fix) and passing output (post‑fix) are shown for Routine A; for Routine B show **pre/post green** from the **same selection** plus **Hit Proof**; for Routine D NO EVIDENCE.

### No Monkey‑Patching or Band‑Aid Fixes (Non‑Negotiable)

Durable, root‑cause fixes only. No muting tests, no broad catch‑and‑ignore, no widening APIs “to make green”.

**Strictly avoid**
* Sleeping/timeouts to hide flakiness.
* Swallowing exceptions or weakening assertions.
* Reflection/internal state manipulation to bypass interfaces.
* Feature flags that disable validation instead of fixing logic.
* Changing public APIs/configs without necessity tied to root cause.

**Preferred approach**
* Reproduce the issue and isolate the smallest failing test (class → method).
* Trace to the true source; fix in the right module.
* Add focused tests for behavior/edge cases (Routine A) or prove coverage/neutrality (Routine B).
* Run tight, targeted verifies; broaden only if needed.

---

## Enforcement & Auto‑Fail Triggers

Your run is **invalid** and must be restarted from “Reproduce first” if any occur:

* You modify production code before adding and running the smallest failing test in this repo **for behavior‑changing work**.
* You proceed without pasting a Surefire/Failsafe report snippet from `target/*-reports/`.
* Your plan does not have **exactly one** `in_progress` step.
* You run tests using `-am` or `-q`.
* You treat a narrative failure description or external stack trace as equivalent to an in‑repo failing test.
* **Routine B specific:** you cannot demonstrate that existing tests exercise the edited code (**Hit Proof**), or you fail to capture both pre‑ and post‑change **matching** passing snippets from the same selection.
* **Routine C breach:** you change production code while in a spike.

**Recovery procedure:**
Update the plan (`in_progress: create failing test`), post a description of your next step, create the failing test, run it, capture the report snippet, then resume.
For Routine B refactors: if any gate fails, **switch to Full TDD** and add the smallest failing test.

---

## Evidence Protocol (Mandatory)

After each grouped action, post an **Evidence block**, then continue working:

**Evidence template**
```
Evidence:
Command: mvn -o -Dmaven.repo.local=.m2_repo -pl <module> -Dtest=Class#method verify
Report: <module>/target/surefire-reports/<file>.txt
Snippet:
\<copy 10–30 lines capturing the failure or success summary>
```

**Routine B additions**
* **Pre‑green:** capture a pre‑change **passing** snippet from the **most specific** test selection that hits your code (ideally a class or method).
* **Hit Proof (choose one):**
    * An existing test class/method that directly calls the edited class/method, plus a short `rg -n` snippet showing the call site; **or**
    * A Surefire/Failsafe output line containing the edited class/method names; **or**
    * A temporary assertion or deliberate, isolated failing check in a **scratch test** proving the path is executed (then remove).
* **Post‑green:** after the patch, re‑run the **same selection** and capture a passing snippet.

---

### Initial Evidence Capture (Required)

To avoid losing the first test evidence when later runs overwrite `target/*-reports/`, immediately persist the initial verify results to a top‑level `initial-evidence.txt` file.

• On a fully green verify run:

- Capture and store the last 200 lines of the Maven verify output.
- Example (module‑scoped):
    - `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> verify | tee .initial-verify.log`
    - `tail -200 .initial-verify.log > initial-evidence.txt`

• On any failing verify run (unit or IT failures):

- Concatenate the Surefire and/or Failsafe report text files into `initial-evidence.txt`.
- Example (repo‑root):
    - `find . -type f \( -path "*/target/surefire-reports/*.txt" -o -path "*/target/failsafe-reports/*.txt" \) -print0 | xargs -0 cat > initial-evidence.txt`

Notes

- Keep `initial-evidence.txt` at the repository root alongside your final handoff.
- Do not rely on `target/*-reports/` for the final report; they may be overwritten by subsequent runs.
- Continue to include the standard Evidence block(s) in your messages as usual.

---

## Living Plan Protocol (Sharper)

Maintain a **living plan** with checklist items (5–7 words each). Keep **exactly one** `in_progress`.

**Plan format**
```

Plan

* \[done] sanity build quick profile
* \[in\_progress] add smallest failing test
* \[todo] minimal root-cause fix
* \[todo] rerun focused then module tests
* \[todo] format, verify, summary

````

**Rule:** If you deviate, update the plan **first**, then proceed.

---

## Environment

* **JDK:** 11 (minimum). The project builds and runs on Java 11+.
* **Maven default:** run **offline** using `-o` whenever possible.
* **Maven local repo (required):** always pass `-Dmaven.repo.local=.m2_repo` on all Maven commands (install, verify, plugins, formatting). All examples in this document implicitly assume this flag, even if omitted.
* **Network:** only to fetch missing deps/plugins; then rerun once without `-o`, and return offline.
* **Large project:** some module test suites can take **5–10 minutes**. Prefer **targeted** runs.

### Maven `-am` usage (house rule)

`-am` is helpful for **compiles**, hazardous for **tests**.

* ✅ Use `-am` **only** for compile/verify with tests skipped (e.g. `-Pquick`):
    * `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> -am -Pquick install`
* ❌ Do **not** use `-am` with `verify` when tests are enabled.

**Two-step pattern (fast + safe)**
1. **Compile deps fast (skip tests):**
   `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> -am -Pquick install`
2. **Run tests:**
   `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> verify | tail -500`

It is illegal to `-am` when running tests!
It is illegal to `-q` when running tests!

---

## Always Install Before Tests (Required)

The Maven reactor resolves inter-module dependencies from the configured local Maven repository (here: `.m2_repo`).
Running `install` publishes your changed modules there so downstream modules and tests pick up the correct versions.

* Always run `mvn -o -Dmaven.repo.local=.m2_repo -Pquick install | tail -200` before you start working. This command typically takes up to 30 seconds. Never use a small timeout than 30,000 ms.
* Always run `mvn -o -Dmaven.repo.local=.m2_repo -Pquick install | tail -200` before any `verify` or test runs.
* If offline resolution fails due to a missing dependency or plugin, rerun the exact `install` command once without `-o`, then return offline.
* Skipping this step can lead to stale or missing artifacts during tests, producing confusing compilation or linkage errors.
* Always use a workspace-local Maven repository: append `-Dmaven.repo.local=.m2_repo` to all Maven commands (install, verify, formatter, etc.).
* Always try to run these commands first to see if they run without needing any approvals from the user w.r.t. the sandboxing.

Why this is mandatory

- Tests must not use `-am`. Without `-am`, Maven will not build upstream modules when you run tests; it will resolve cross‑module dependencies from the configured local repository (here: `.m2_repo`).
- Therefore, tests only see whatever versions were last published to the configured local repo (`.m2_repo`). If you change code in one module and then run tests in another, those tests will not see your changes unless the updated module has been installed to `.m2_repo` first.
- The reliable way to ensure all tests always use the latest code across the entire multi‑module build is to install all modules to the configured local repo (`.m2_repo`) before running any tests: run `mvn -o -Dmaven.repo.local=.m2_repo -Pquick install` at the repository root.
- In tight loops you may also install a specific module and its deps (`-pl <module> -am -Pquick install`) to iterate quickly, but before executing tests anywhere that depend on your changes, run a root‑level `mvn -o -Dmaven.repo.local=.m2_repo -Pquick install` so the latest jars are available to the reactor from `.m2_repo`.
---

## Quick Start (First 10 Minutes)

1. **Discover**
    * Inspect root `pom.xml` and module tree (see “Maven Module Overview”).
    * Search fast with ripgrep: `rg -n "<symbol or string>"`
2. **Build sanity (fast, skip tests)**
    * `mvn -o -Dmaven.repo.local=.m2_repo -Pquick install | tail -200`
3. **Format (Java, imports, XML)**
    * `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C formatter:format impsort:sort xml-format:xml-format`
4. **Targeted tests (tight loops)**
    * Module: `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> verify  | tail -500`
    * Class: `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> -Dtest=ClassName verify  | tail -500`
    * Method: `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> -Dtest=ClassName#method verify | tail -500`
5. **Inspect failures**
    * **Unit (Surefire):** `<module>/target/surefire-reports/`
    * **IT (Failsafe):** `<module>/target/failsafe-reports/`

It is illegal to `-am` when running tests!
It is illegal to `-q` when running tests!

---

## Routine A — Full TDD (Default)

> Use for **all behavior‑changing work** and whenever Routine B gates do not all pass.

### Bugfix Workflow (Mandatory)

* **Reproduce first:** write the smallest focused test (class/method) that reproduces the reported bug **inside this repo**. Confirm it fails.
* **Keep the test as‑is:** do not weaken assertions or mute the failure.
* **Fix at the root:** minimal, surgical change in the correct module.
* **Verify locally:** re‑run the focused test, then the module’s tests. Avoid `-am`/`-q` with tests.
* **Broaden if needed:** expand scope only after targeted greens.
* **Document clearly:** failing output (pre‑fix), root cause, minimal fix, passing output (post‑fix).

### Hard Gates

* A failing test exists at the smallest scope (method/class).
* **No production patch before the failing test is observed and recorded.**
* Test runs avoid `-am` and `-q`.

---

## Routine B — Change without new tests (Proportional, gated)

> Use **only** when at least one Allowed Case applies **and** all Routine B **Gates** pass.

### Allowed cases (one or more)
1. **Bugfix with existing failing test** in this repo (pinpoints class/method).
2. **Strictly behavior‑neutral refactor / cleanup / micro‑perf** with clear existing coverage hitting the edited path.
3. **Migration/rename/autogen refresh** where behavior is already characterized by existing tests.
4. **Build/CI/docs/logging/message changes** that do not alter runtime behavior or asserted outputs.
5. **Data/resource tweaks** not asserted by tests and not affecting behavior.

### Routine B Gates (all must pass)
- **Neutrality/Scope:** No externally observable behavior change. Localized edit.
- **Hit Proof:** Demonstrate tests exercise the edited code.
- **Pre/Post Green Match:** Same smallest‑scope selection, passing before and after.
- **Risk Check:** No concurrency/time/IO semantics touched; no public API, serialization, parsing, or ordering changes.
- **Reversibility:** Change is easy to revert if needed.

**If any gate fails → switch to Routine A.**

---

## Routine C — Spike / Investigate (No production changes)

> Use for exploration, triage, design spikes, and measurement. **No production code edits.**

**You may:**
- Add temporary scratch tests, assertions, scripts, or notes.
- Capture measurements, traces, logs.

**Hand‑off must include:**
- Description, commands, and artifacts (logs/notes).
- Findings, options, and a proposed next routine (A or B).
- Removal of any temporary code if not adopted.

---

## Routine D — ExecPlans

> Use for **complex features or significant refactors**.

When writing complex features or significant refactors, use an ExecPlan (as described in .agent/PLANS.md) from design to implementation.

---

## Where to Draw the Line — A Short Debate

> **Purist:** “All changes must start with a failing test.”
> **Pragmatist:** “For refactors that can’t fail first without faking it, prove coverage and equality of behavior.”

**In‑scope for Routine B (examples)**
* Rename private methods; extract helper; dead‑code removal.
* Replace straightforward loop with stream (same results, same ordering).
* Tighten generics/nullability/annotations without observable change.
* Micro‑perf cache within a method with deterministic inputs and strong coverage.
* Logging/message tweaks **not** asserted by tests.
* Build/CI config that doesn’t alter runtime behavior.

**Out‑of‑scope (use Routine A/D)**
* Changing query results, serialization, or parsing behavior.
* Altering error messages that tests assert.
* Anything touching concurrency, timeouts, IO, or ordering.
* New SPARQL function support or extended syntax (even “tiny”).
* Public API changes or cross‑module migrations with unclear blast radius.

---

## Working Loop

* **PIOSEE first:** restate Problem, gather Information, list Options; then Select, Execute, Evaluate.
* **Plan:** small, verifiable steps; keep one `in_progress`, or follow PLANS.md (ExecPlans)
* **Change:** minimal, surgical edits; keep style/structure consistent.
* **Format:** `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C formatter:format impsort:sort xml-format:xml-format`
* **Compile (fast):** `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> -am -Pquick install | tail -500`
* **Test:** start smallest (class/method → module). For integration, run module `verify`.
* **Triage:** read reports; fix root cause; expand scope only when needed.
* **Iterate:** keep momentum; escalate only when blocked or irreversible.

It is illegal to `-am` when running tests!
It is illegal to `-q` when running tests!

---

## Testing Strategy

* **Prefer module tests you touched:** `-pl <module>`
* **Narrow further** to a class/method; then broaden to the module.
* **Expand scope** when changes cross boundaries or neighbor modules fail.
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
```bash
mvn -o -Dmaven.repo.local=.m2_repo -pl <module> -Dtest=ClassName[#method] -Dmaven.test.redirectTestOutputToFile=true verify | tail -500
````

Logs under:

```
<module>/target/surefire-reports/ClassName-output.txt
```

(Use similarly for Failsafe via `-Dit.test=`.)

---

## Assertions: Make invariants explicit

Assertions are executable claims about what must be true. Use **temporary tripwires** during investigation and **permanent contracts** once an invariant matters.

* One fact per assert; fail fast and usefully.
* Include stable context in messages; avoid side effects.
* Keep asserts cheap; don’t replace user input validation with asserts.

**Java specifics**

* Enable VM assertions in tests (`-ea`).
* Use exceptions for runtime guarantees; `assert` for “cannot happen”.

(Concrete examples omitted here for brevity; keep your current patterns.)

---

## Triage Playbook

* **Missing dep/plugin offline:** rerun the exact command once **without** `-o`, then return offline.
* **Compilation errors:** fix imports/generics/visibility; quick install in the module.
* **Flaky/slow tests:** run the specific failing test; stabilize root cause before broad runs.
* **Formatting failures:** run formatter/import/XML sort; re‑verify.
* **License header missing:** add for **new** files only; do not change years on existing files.

---

## Code Formatting

* Always run before finalizing:

    * `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C formatter:format impsort:sort xml-format:xml-format`
* Style: no wildcard imports; 120‑char width; curly braces always; LF endings.

---

## Source File Headers

Strict requirement — copy/paste exactly. All new Java source files MUST begin with the exact header below. The text, spacing, punctuation, URL, and SPDX line must be identical. Replace `${year}` with the correct current year at the time the file is created.

Hint: get the current year with `date +%Y`.

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

Do **not** modify existing headers’ years.

---

## Pre‑Commit Checklist

* **Format:** `mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C formatter:format impsort:sort xml-format:xml-format`
* **Compile (fast path):** `mvn -o -Dmaven.repo.local=.m2_repo -Pquick install | tail -200`
* **Tests (targeted):** `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> verify | tail -500` (broaden as needed)
* **Reports:** zero new failures in Surefire/Failsafe, or explain precisely.
* **Evidence:** Routine A — failing pre‑fix + passing post‑fix.
  Routine B — **pre/post green** from same selection + **Hit Proof**.

---

## Branching & Commit Conventions

* Branch names: start with `GH-XXXX` (GitHub issue number). Optional short slug, e.g., `GH-1234-trig-writer-check`.
* Commit messages: `GH-XXXX <short imperative summary>` on every commit.

---

## Branch & PR Workflow (Agent)

* Confirm issue number first (mandatory).
* Branch: `git checkout -b GH-XXXX-your-slug`
* Stage: `git add -A` (ensure new Java files have the required header).
* Optional: formatter + quick install.
* Commit: `git commit -m "GH-XXXX <short imperative summary>"`
* Push & PR: use the default template; fill all fields; include `Fixes #XXXX`.

---

## Navigation & Search

* Files: `rg --files`
* Content: `rg -n "<pattern>"`
* Read big files in chunks:

    * `sed -n '1,200p' path/to/File.java`
    * `sed -n '201,400p' path/to/File.java`

---

## Autonomy Rules (Act > Ask)

* **Default:** act with assumptions; document them.
* **Keep going:** chain steps; short progress updates before long actions.
* **Ask only when:** blocked by sandbox/approvals/network, or change is destructive/irreversible, or impacts public APIs/dependencies/licensing.
* **Prefer reversible moves:** smallest local change that unblocks progress; validate with targeted tests first.

**Defaults**

* **Tests:** start with `-pl <module>`, then `-Dtest=Class#method` / `-Dit.test=ITClass#method`.
* **Build:** use `-o`; drop `-o` once only to fetch; return offline.
* **Formatting:** run formatter/import/XML before verify.
* **Reports:** read surefire/failsafe locally; expand scope only when necessary.

---

## Answer Template (Use This)

* **What changed:** summary of approach and rationale.
* **Files touched:** list file paths.
* **Commands run:** key build/test commands.
* **Verification:** which tests passed, where you checked reports.
* **PIOSEE trace (concise):** P/I/O summary, selected option/routine, key evaluate outcomes.
* **Evidence:**
  *Routine A:* failing output (pre‑fix) and passing output (post‑fix).
  *Routine B:* pre‑ and post‑green snippets from the **same selection** + **Hit Proof**.
  *Routine C:* artifacts from investigation (logs/notes/measurements) and proposed next steps.
  *Routine D:* NO EVIDENCE REQUIRED.
* **Assumptions:** key assumptions and autonomous decisions.
* **Limitations:** anything left or risky edge cases.
* **Next steps:** optional follow‑ups.

---

## Running Tests

* By module: `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/shacl verify | tail -500`
* Entire repo: `mvn -o -Dmaven.repo.local=.m2_repo verify` (long; only when appropriate)
* Slow tests (entire repo):
  `mvn -o -Dmaven.repo.local=.m2_repo verify -PslowTestsOnly,-skipSlowTests | tail -500`
* Slow tests (by module):
  `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> verify -PslowTestsOnly,-skipSlowTests | tail -500`
* Slow tests (specific test):

    * `mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/shacl -PslowTestsOnly,-skipSlowTests -Dtest=ClassName#method verify | tail -500`
* Integration tests (entire repo):
  `mvn -o -Dmaven.repo.local=.m2_repo verify -PskipUnitTests | tail -500`
* Integration tests (by module):
  `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> verify -PskipUnitTests | tail -500`
* Useful flags:

    * `-Dtest=ClassName`
    * `-Dtest=ClassName#method`
    * `-Dit.test=ITClass#method`
    * `-DtrimStackTrace=false`

---

## Build

* **Build without tests (fast path):**
  `mvn -o -Dmaven.repo.local=.m2_repo -Pquick install`
* **Verify with tests:**
  Targeted module(s): `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> verify`
  Entire repo: `mvn -o -Dmaven.repo.local=.m2_repo verify` (use judiciously)
* **When offline fails due to missing deps:**
  Re‑run the **exact** command **without** `-o` once to fetch, then return to `-o`.

---

## Using JaCoCo (Coverage)

JaCoCo is configured via the `jacoco` Maven profile in the root POM. Surefire/Failsafe honor the prepared agent `argLine`, so no extra flags are required beyond `-Pjacoco`.

- Run with coverage
    - Module: `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> -Pjacoco verify | tail -500`
    - Class: `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> -Pjacoco -Dtest=ClassName verify | tail -500`
    - Method: `mvn -o -Dmaven.repo.local=.m2_repo -pl <module> -Pjacoco -Dtest=ClassName#method verify | tail -500`

- Where to find reports (per module)
    - Exec data: `<module>/target/jacoco.exec`
    - HTML report: `<module>/target/site/jacoco/index.html`
    - XML report: `<module>/target/site/jacoco/jacoco.xml`

- Check if a specific test covers code X
    - Run only that test (class or method) with `-Dtest=...` (see above) and `-Pjacoco`.
    - Open the HTML report and navigate to the class/method of interest; non-zero line/branch coverage indicates the selected test touched it.
    - For multiple tests, run them in small subsets to localize coverage quickly.

- Troubleshooting
    - If you see “Skipping JaCoCo execution due to missing execution data file”, ensure you passed `-Pjacoco` and ran the install step first.
    - If offline resolution fails for the JaCoCo plugin, rerun the exact command once without `-o`, then return offline.

- Notes
    - The default JaCoCo reports do not list “which individual tests” hit each line. Use single-test runs to infer per-test coverage. If you need true per-test mapping, add a JUnit 5 extension that sets a JaCoCo session per test and writes per-test exec files.
    - Do not use `-am` when running tests; keep runs targeted by module/class/method.

---

## Prohibited Misinterpretations

* A user stack trace, reproduction script, or verbal description **is not evidence** for behavior‑changing work. You must implement the smallest failing test **inside this repo**.
* For Routine B, a stack trace is neither required nor sufficient; **Hit Proof** plus **pre/post green** snippets are mandatory.
* Routine C must not change production code.

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

### Version Control Conventions

* Branch names must always start with the GitHub issue identifier in the form `GH-XXXX`, where `XXXX` is the numeric issue number.
* Every commit message must be prefixed with the corresponding `GH-XXXX` label.
* Exception: if no GitHub issue number is available for the task, clearly note this in your handoff and align with the requester on an appropriate branch/commit prefix before proceeding.

It is illegal to `-am` when running tests!
It is illegal to `-q` when running tests!
You must follow these rules and instructions exactly as stated.
