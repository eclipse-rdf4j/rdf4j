# Advanced Coding Techniques

Use this reference when the problem needs more than basic loops and collections: dynamic programming, advanced search, state compression, offline transforms, or optimization patterns that materially change runtime.

## Dynamic programming checklist

Before writing code, define:
- state: the minimum information needed to continue
- transition: how one state moves to the next
- base case: the smallest solved states
- order: top-down memoization or bottom-up tabulation
- objective: min, max, count, feasibility, reconstruction
- memory plan: full table, rolling rows, bitset, or sparse map

If any of those are fuzzy, the DP is not ready.

## DP implementation bias in Java

- Prefer flat primitive arrays over nested object graphs.
- Flatten `dp[row][col]` into one array when locality matters.
- Use sentinel values (`INF`, `-1`, impossible masks) instead of wrapper objects.
- Compress dimensions aggressively when a transition only needs prior rows or prior prefixes.
- Use iterative tabulation when recursion depth or call overhead is risky.
- Use memoization when the reachable state space is sparse or pruning is strong.

## Common DP families

### 1D DP

Use for:
- linear decisions
- prefix optimization
- classic knapsack-style transitions

Java notes:
- Often compresses to one array.
- Direction matters: reverse iterate for 0/1 knapsack; forward iterate for unbounded knapsack.

### 2D grid / sequence DP

Use for:
- edit distance
- LCS variants
- path counting
- interval composition

Java notes:
- Two rolling rows often replace the full matrix.
- Keep row-major iteration consistent with memory layout.

### Interval DP

Use for:
- merge cost
- matrix chain multiplication
- optimal parenthesization
- palindrome partitioning

Heuristic:
- Try increasing interval length order.
- Precompute reusable range costs.

### Tree DP

Use for:
- subtree aggregation
- rerooting
- independent set / matching variants on trees

Java notes:
- Iterative traversal can avoid stack overflow.
- Store parent/index arrays once; reuse buffers for passes.

### DAG DP

Use for:
- longest path in DAG
- path counts
- dependency-ordered optimization

Heuristic:
- Topological order first, transitions second.

### Bitmask DP

Use for:
- small `n` subset problems
- travelling-salesman-style state
- assignment and partition variants

Java notes:
- Use `int` masks up to 31 bits, `long` masks up to 63.
- Precompute subset transitions when reused heavily.
- Beware exponential memory growth; consider meet-in-the-middle.

### Digit DP

Use for:
- counting numbers with digit constraints
- lexicographic numeric constraints

State usually includes:
- position
- tight/limited flag
- started/leading-zero flag
- problem-specific accumulator

## DP optimization patterns

### Prefix/suffix acceleration

If a transition scans prior states, ask whether prefix minima/maxima/sums can reduce it from `O(n^2)` to `O(n)`.

### Monotonic queue optimization

Use when transitions need min/max over a sliding window.

### Divide-and-conquer DP optimization

Use when the optimal split point is monotonic across rows or columns.

### Convex hull trick / Li Chao tree

Use when transitions are of the form:
- `dp[i] = min_j(m[j] * x[i] + b[j])`
- `max` variant of the same

Only use when the algebra really matches.

### Bitset DP

Use when boolean subset transitions can become word-parallel bit operations.

Examples:
- subset sum
- knapsack feasibility
- reachability layers

### State compression

Reduce dimensions by:
- keeping only prior row/column
- encoding booleans into bits
- coordinate-compressing sparse values
- using ids instead of objects

## Search and optimization patterns

### Binary search on answer

Use when:
- feasibility is monotonic
- exact objective is hard but checking a threshold is easier

### Meet-in-the-middle

Use when:
- brute force is `2^n`
- `n` is small enough to split into two `2^(n/2)` halves

### Branch and bound

Use when:
- you can compute tight upper/lower bounds
- a good heuristic ordering prunes much of the tree

### Iterative deepening

Use when:
- memory is tight
- solution depth is unknown but usually shallow

### Offline query processing

Use when:
- query order is irrelevant
- sorting queries/events lets you reuse structure updates

## Greedy and exchange-thinking

Before building DP or search, test whether a greedy proof exists:
- local choice stays globally optimal
- exchange argument repairs any non-greedy optimal solution
- matroid-like or interval-scheduling structure is present

If greedy works, it often beats DP both asymptotically and operationally.

## Range and sequence patterns

- Sliding window: monotonic boundary expansion or contraction.
- Two pointers: sorted arrays, pair/triple sums, dedup, partitioning.
- Monotonic stack: next greater/smaller, histogram, span problems.
- Difference arrays: batch range updates.
- Prefix sums / xor / hashes: cheap repeated range queries.

## Java-specific implementation notes

- Avoid recursion for deep graphs, trees, or DP unless the depth bound is small.
- Replace tuple objects with parallel arrays or packed longs in hot paths.
- Pre-size arrays and reusable buffers for repeated test cases.
- Be explicit about overflow; use `long` for counts/costs unless `int` is proven safe.
- Separate correctness code from hot code paths once the algorithm is clear.

## Problem-solving ladder

When stuck, try this order:
1. Can I sort or batch the work?
2. Can I precompute prefix, suffix, or compressed state?
3. Can a different data structure remove a nested loop?
4. Is the problem actually graph, interval, or DP in disguise?
5. Can the state shrink to primitives or bits?
6. Can I prove greedy, monotonicity, or convexity?

## Red flags

- DP state includes fields that do not affect future transitions.
- Memoization key is a heavyweight object when a few ints suffice.
- Full `O(n^2)` table retained even though only one frontier is used.
- Search explores symmetric states repeatedly.
- A library data structure is used where a flat array plus sort is enough.
