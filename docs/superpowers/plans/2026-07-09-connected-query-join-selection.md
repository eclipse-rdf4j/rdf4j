# Connected Query Join Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the standard query join optimizer avoid an optional cross join by choosing the cheapest remaining tuple expression that consumes an existing binding.

**Architecture:** Keep the existing cost formula and add connectivity as the primary selection key inside `selectNextTupleExpr`. Detect connectivity from the current visitor `boundVars`, using the existing variable map first and tuple-expression binding names as a fallback.

**Tech Stack:** Java 25, RDF4J query algebra, JUnit 5, AssertJ, Maven Surefire, repository `mvnf` runner.

---

### Task 1: Reproduce disconnected greedy selection

**Files:**
- Create: `core/queryalgebra/evaluation/src/test/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/QueryJoinOptimizerConnectivityTest.java`
- Create: `initial-evidence.txt`

- [x] **Step 1: Add the focused regression test**

Add `prefersCheapestConnectedTupleExprOverCheaperCrossJoin`, optimizing three parsed statement patterns with custom
cardinalities. Assert the flattened predicate order is `ex:pAnchor`, `ex:pConnected`, `ex:pDisconnected`.

```java
@Test
public void prefersCheapestConnectedTupleExprOverCheaperCrossJoin() {
	String query = String.join("\n",
			"PREFIX ex: <ex:>",
			"SELECT * WHERE {",
			"  ?root ex:pAnchor ?shared .",
			"  ?shared ex:pConnected ?next .",
			"  ?other ex:pDisconnected ?value .",
			"}");

	ParsedQuery parsedQuery = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
	QueryRoot root = new QueryRoot(parsedQuery.getTupleExpr());
	new QueryJoinOptimizer(new ConnectedJoinStatistics(), new EmptyTripleSource()).optimize(root, null, null);

	List<String> order = joinArgs(root).stream()
			.map(QueryJoinOptimizerConnectivityTest::predicate)
			.collect(Collectors.toList());
	assertThat(order).containsExactly("ex:pAnchor", "ex:pConnected", "ex:pDisconnected");
}
```

Add `ConnectedJoinStatistics`: return cardinalities `1`, `10_000`, and `10` for anchor, connected, and disconnected
patterns respectively.

- [x] **Step 2: Run test and capture RED**

Run:

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py QueryJoinOptimizerConnectivityTest#prefersCheapestConnectedTupleExprOverCheaperCrossJoin --retain-logs
```

Expected: assertion failure showing `ex:pDisconnected` before `ex:pConnected`.

Persist report evidence:

```bash
python3 scripts/agent-evidence.py --command "python3 .codex/skills/mvnf/scripts/mvnf.py QueryJoinOptimizerConnectivityTest#prefersCheapestConnectedTupleExprOverCheaperCrossJoin --retain-logs" core/queryalgebra/evaluation/target/surefire-reports > initial-evidence.txt
```

### Task 2: Prefer connected candidates

**Files:**
- Modify: `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryJoinOptimizer.java`

- [x] **Step 1: Add connectivity as primary key**

In `selectNextTupleExpr`, retain the single scan but allow a connected candidate to replace a disconnected result
regardless of numeric cost. Once connected, compare only against other connected candidates.

```java
boolean resultUsesExistingBinding = false;
for (TupleExpr tupleExpr : expressions) {
	double cost = getTupleExprCost(tupleExpr, cardinalityMap, varsMap, varFreqMap);
	boolean usesExistingBinding = usesExistingBinding(tupleExpr, varsMap.get(tupleExpr));

	if (usesExistingBinding && !resultUsesExistingBinding
			|| usesExistingBinding == resultUsesExistingBinding && (cost < lowestCost || result == null)) {
		lowestCost = cost;
		result = tupleExpr;
		resultUsesExistingBinding = usesExistingBinding;
	}
}
```

Add a private `usesExistingBinding` helper that checks non-constant vars against `boundVars`, then falls back to
`tupleExpr.getBindingNames()` for tuple-expression forms without collected vars.

- [x] **Step 2: Run focused test and verify GREEN**

Run:

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py QueryJoinOptimizerConnectivityTest#prefersCheapestConnectedTupleExprOverCheaperCrossJoin --retain-logs
```

Expected: one test run, zero failures and zero errors.

### Task 3: Verify and audit

**Files:**
- Verify: `core/queryalgebra/evaluation`

- [x] **Step 1: Run the optimizer test classes**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py QueryJoinOptimizerConnectivityTest --retain-logs
python3 .codex/skills/mvnf/scripts/mvnf.py QueryJoinOptimizerTest --retain-logs
```

Expected: all `QueryJoinOptimizerTest` tests pass.

- [x] **Step 2: Check headers and format**

```bash
cd scripts && ./checkCopyrightPresent.sh
mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources
```

Expected: no copyright findings; formatter exits successfully.

- [x] **Step 3: Run module verification**

```bash
python3 .codex/skills/mvnf/scripts/mvnf.py core/queryalgebra/evaluation --retain-logs
```

Expected: all Surefire and Failsafe reports are green.

- [x] **Step 4: Audit the final diff**

```bash
git status --short
git --no-pager diff --color=never -- core/queryalgebra/evaluation initial-evidence.txt docs/superpowers
```

Expected: only the focused optimizer/test changes, evidence, and planning documents, plus the pre-existing unrelated
benchmark modification.
