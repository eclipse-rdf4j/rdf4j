
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Exact left-deep join-order optimizer for connected acyclic statement-pattern queries.
 *
 * <p>The optimizer uses JoinEstimator as its cost model. The DP state is a connected subset
 * of patterns. For a transition S -> S ∪ {p}, where p shares exactly one variable x with S,
 * the step cost is:</p>
 *
 * <pre>
 *   OutRows(S, p) = dot( M_S^x, d_p^x )
 * </pre>
 *
 * <p>where M_S^x is the canonical message of subset S on attachment variable x and
 * d_p^x is the degree sketch of pattern p on x.</p>
 */
public final class JoinOrderOptimizer {

    private final JoinEstimator estimator;

    public JoinOrderOptimizer(final JoinEstimator estimator) {
        this.estimator = Objects.requireNonNull(estimator, "estimator");
    }

    /**
     * Finds the optimal left-deep order under the sketch-based cost model and returns the
     * corresponding left-deep join tree.
     */
    public OptimizationResult optimizeDp(final List<JoinEstimator.StatementPattern> patterns) {
        final PlanningContext ctx = new PlanningContext(patterns);
        final long all = ctx.allMask();

        final Map<Long, Double> bestCost = new HashMap<>();
        final Map<Long, Transition> parent = new HashMap<>();
        final Map<Integer, LinkedHashSet<Long>> statesBySize = new HashMap<>();

        for (int i = 0; i < ctx.size(); i++) {
            final long mask = bit(i);
            final double seedCost = estimator.estimatePatternRows(ctx.pattern(i));
            bestCost.put(mask, seedCost);
            statesBySize.computeIfAbsent(1, ignored -> new LinkedHashSet<>()).add(mask);
        }

        for (int size = 1; size < ctx.size(); size++) {
            final Set<Long> states = statesBySize.containsKey(size)
                    ? statesBySize.get(size)
                    : Collections.<Long>emptySet();
            for (final long mask : states) {
                final double prefixCost = bestCost.get(mask);

                final List<Integer> candidates = ctx.candidates(mask);
                for (final int nextIndex : candidates) {
                    final String shared = ctx.uniqueSharedVariable(mask, nextIndex);
                    if (shared == null) {
                        continue;
                    }

                    final JoinEstimator.Message message = message(ctx, mask, shared);
                    final double stepCost =
                            estimator.estimateOutputRows(message, ctx.pattern(nextIndex), shared);

                    final long nextMask = mask | bit(nextIndex);
                    final double candidateCost = prefixCost + stepCost;

                    if (candidateCost < bestCost.getOrDefault(nextMask, Double.POSITIVE_INFINITY)) {
                        bestCost.put(nextMask, candidateCost);
                        parent.put(nextMask, new Transition(mask, nextIndex));
                        statesBySize.computeIfAbsent(size + 1, ignored -> new LinkedHashSet<>()).add(nextMask);
                    }
                }
            }
        }

        if (!bestCost.containsKey(all)) {
            throw new IllegalStateException(
                    "Unable to find a full connected order. This implementation expects a connected, "
                            + "acyclic query where each extension shares exactly one variable.");
        }

        final List<JoinEstimator.StatementPattern> order = reconstructOrder(ctx, parent, all);
        final PlanNode tree = buildLeftDeepTree(order);
        return new OptimizationResult(order, tree, bestCost.get(all), Strategy.DP);
    }

    /**
     * Cheap fallback: choose the seed with the smallest estimated base rows, then repeatedly add
     * the adjacent pattern with the smallest estimated output rows.
     */
    public OptimizationResult optimizeGreedy(final List<JoinEstimator.StatementPattern> patterns) {
        final PlanningContext ctx = new PlanningContext(patterns);
        final List<JoinEstimator.StatementPattern> order = new ArrayList<>(ctx.size());

        int seed = -1;
        double bestSeedRows = Double.POSITIVE_INFINITY;
        for (int i = 0; i < ctx.size(); i++) {
            final double rows = estimator.estimatePatternRows(ctx.pattern(i));
            if (rows < bestSeedRows) {
                bestSeedRows = rows;
                seed = i;
            }
        }

        long mask = bit(seed);
        double totalCost = bestSeedRows;
        order.add(ctx.pattern(seed));

        while (mask != ctx.allMask()) {
            int bestNext = -1;
            double bestStep = Double.POSITIVE_INFINITY;

            for (final int candidate : ctx.candidates(mask)) {
                final String shared = ctx.uniqueSharedVariable(mask, candidate);
                if (shared == null) {
                    continue;
                }
                final JoinEstimator.Message message = message(ctx, mask, shared);
                final double step = estimator.estimateOutputRows(message, ctx.pattern(candidate), shared);
                if (step < bestStep) {
                    bestStep = step;
                    bestNext = candidate;
                }
            }

            if (bestNext < 0) {
                throw new IllegalStateException(
                        "Greedy planner got stuck. This implementation expects a connected, "
                                + "acyclic query where each extension shares exactly one variable.");
            }

            mask |= bit(bestNext);
            totalCost += bestStep;
            order.add(ctx.pattern(bestNext));
        }

        return new OptimizationResult(order, buildLeftDeepTree(order), totalCost, Strategy.GREEDY);
    }

    /**
     * Canonical message M_S^rootVariable for a connected subset S.
     *
     * <p>This is the object that makes the DP valid: the same subset always gets the same message,
     * independent of the order in which the subset was constructed.</p>
     */
    private JoinEstimator.Message message(
            final PlanningContext ctx,
            final long subsetMask,
            final String rootVariable) {

        final MessageKey key = new MessageKey(subsetMask, rootVariable);
        final JoinEstimator.Message cached = ctx.messageMemo.get(key);
        if (cached != null) {
            return cached;
        }

        final List<JoinEstimator.Message> factors = new ArrayList<>();
        for (final int patternIndex : ctx.incidentPatterns(rootVariable)) {
            if (!contains(subsetMask, patternIndex)) {
                continue;
            }

            final JoinEstimator.StatementPattern pattern = ctx.pattern(patternIndex);

            if (pattern.arity() == 1) {
                factors.add(estimator.leafMessage(pattern, rootVariable));
                continue;
            }

            if (pattern.arity() != 2) {
                throw new IllegalStateException(
                        "This implementation supports only unary/binary patterns. Offending pattern: "
                                + pattern);
            }

            final String other = pattern.otherVariable(rootVariable);
            final long childMask = ctx.childComponentMask(subsetMask, patternIndex, rootVariable, other);

            if (childMask == 0L) {
                factors.add(estimator.leafMessage(pattern, rootVariable));
            } else {
                final JoinEstimator.Message childMessage = message(ctx, childMask, other);
                factors.add(estimator.push(childMessage, pattern, other, rootVariable));
            }
        }

        if (factors.isEmpty()) {
            throw new IllegalStateException(
                    "Subset " + Long.toBinaryString(subsetMask)
                            + " has no factors incident to root variable " + rootVariable);
        }

        final JoinEstimator.Message result = estimator.multiply(rootVariable, factors);
        ctx.messageMemo.put(key, result);
        return result;
    }

    private static List<JoinEstimator.StatementPattern> reconstructOrder(
            final PlanningContext ctx,
            final Map<Long, Transition> parent,
            final long fullMask) {

        final ArrayDeque<JoinEstimator.StatementPattern> reversed = new ArrayDeque<>();
        long current = fullMask;

        while (Long.bitCount(current) > 1) {
            final Transition step = parent.get(current);
            if (step == null) {
                throw new IllegalStateException("Broken DP parent chain at mask " + Long.toBinaryString(current));
            }
            reversed.addFirst(ctx.pattern(step.addedPatternIndex()));
            current = step.previousMask();
        }

        final int seedIndex = Long.numberOfTrailingZeros(current);
        reversed.addFirst(ctx.pattern(seedIndex));
        return List.copyOf(reversed);
    }

    private static PlanNode buildLeftDeepTree(final List<JoinEstimator.StatementPattern> order) {
        if (order.isEmpty()) {
            throw new IllegalArgumentException("Cannot build a join tree for an empty order.");
        }

        PlanNode root = new ScanNode(order.get(0));
        final LinkedHashSet<String> currentVariables = new LinkedHashSet<>(root.outputVariables());

        for (int i = 1; i < order.size(); i++) {
            final JoinEstimator.StatementPattern next = order.get(i);
            final LinkedHashSet<String> joinVariables = new LinkedHashSet<>(currentVariables);
            joinVariables.retainAll(next.variables());

            root = new JoinNode(root, new ScanNode(next), Set.copyOf(joinVariables));
            currentVariables.addAll(next.variables());
        }

        return root;
    }

    private static boolean contains(final long mask, final int index) {
        return (mask & bit(index)) != 0L;
    }

    private static long bit(final int index) {
        return 1L << index;
    }

    // --------------------------------------------------------------------------------------------
    // Result model
    // --------------------------------------------------------------------------------------------

    public enum Strategy {
        DP,
        GREEDY
    }

    public record OptimizationResult(
            List<JoinEstimator.StatementPattern> orderedPatterns,
            PlanNode plan,
            double totalCost,
            Strategy strategy) {
    }

    public interface PlanNode {
        Set<String> outputVariables();
    }

    public record ScanNode(JoinEstimator.StatementPattern pattern) implements PlanNode {
        @Override
        public Set<String> outputVariables() {
            return pattern.variables();
        }
    }

    public record JoinNode(PlanNode left, PlanNode right, Set<String> joinVariables) implements PlanNode {
        @Override
        public Set<String> outputVariables() {
            final LinkedHashSet<String> vars = new LinkedHashSet<>(left.outputVariables());
            vars.addAll(right.outputVariables());
            return Set.copyOf(vars);
        }
    }

    private record Transition(long previousMask, int addedPatternIndex) {
    }

    private record MessageKey(long subsetMask, String rootVariable) {
    }

    // --------------------------------------------------------------------------------------------
    // Query-shape helpers
    // --------------------------------------------------------------------------------------------

    private static final class PlanningContext {
        private final List<JoinEstimator.StatementPattern> patterns;
        private final Map<String, List<Integer>> incidentPatternsByVariable;
        private final Map<Long, Set<String>> variablesMemo = new HashMap<>();
        private final Map<MessageKey, JoinEstimator.Message> messageMemo = new HashMap<>();

        private PlanningContext(final List<JoinEstimator.StatementPattern> patterns) {
            this.patterns = List.copyOf(Objects.requireNonNull(patterns, "patterns"));
            if (patterns.isEmpty()) {
                throw new IllegalArgumentException("At least one pattern is required.");
            }
            if (patterns.size() > 60) {
                throw new IllegalArgumentException(
                        "This demo implementation uses a long bit-mask and supports up to 60 patterns.");
            }
            this.incidentPatternsByVariable = buildIncidence(patterns);
        }

        private int size() {
            return patterns.size();
        }

        private JoinEstimator.StatementPattern pattern(final int index) {
            return patterns.get(index);
        }

        private long allMask() {
            return (1L << patterns.size()) - 1L;
        }

        private List<Integer> incidentPatterns(final String variable) {
            return incidentPatternsByVariable.getOrDefault(variable, List.of());
        }

        private List<Integer> candidates(final long mask) {
            final List<Integer> result = new ArrayList<>();
            for (int i = 0; i < patterns.size(); i++) {
                if (contains(mask, i)) {
                    continue;
                }
                if (mask == 0L) {
                    result.add(i);
                    continue;
                }
                final String shared = uniqueSharedVariable(mask, i);
                if (shared != null) {
                    result.add(i);
                }
            }
            return result;
        }

        private String uniqueSharedVariable(final long mask, final int candidateIndex) {
            final LinkedHashSet<String> shared = new LinkedHashSet<>(variables(mask));
            shared.retainAll(pattern(candidateIndex).variables());
            return shared.size() == 1 ? shared.iterator().next() : null;
        }

        private Set<String> variables(final long mask) {
            return variablesMemo.computeIfAbsent(mask, ignored -> {
                final LinkedHashSet<String> vars = new LinkedHashSet<>();
                for (int i = 0; i < patterns.size(); i++) {
                    if (contains(mask, i)) {
                        vars.addAll(pattern(i).variables());
                    }
                }
                return Set.copyOf(vars);
            });
        }

        /**
         * Returns the pattern-subset on the far side of patternIndex when the subset is rooted at parentVariable
         * and we cross into childVariable.
         *
         * <p>Because the query is assumed to be acyclic, removing the binary factor (patternIndex) separates the
         * factor graph into two components, and this BFS returns the child component.</p>
         */
        private long childComponentMask(
                final long subsetMask,
                final int patternIndex,
                final String parentVariable,
                final String childVariable) {

            long childMask = 0L;
            final ArrayDeque<Object> queue = new ArrayDeque<>();
            final LinkedHashSet<String> seenVariables = new LinkedHashSet<>();
            final BitSet seenPatterns = new BitSet(patterns.size());

            seenVariables.add(childVariable);
            queue.add(childVariable);

            while (!queue.isEmpty()) {
                final Object node = queue.removeFirst();

                if (node instanceof String variable) {
                    for (final int incident : incidentPatterns(variable)) {
                        if (incident == patternIndex || !contains(subsetMask, incident)) {
                            continue;
                        }
                        if (!seenPatterns.get(incident)) {
                            seenPatterns.set(incident);
                            queue.addLast(Integer.valueOf(incident));
                        }
                    }
                    continue;
                }

                final int currentPattern = ((Integer) node).intValue();
                childMask |= bit(currentPattern);
                final JoinEstimator.StatementPattern pattern = pattern(currentPattern);
                for (final String v : pattern.variables()) {
                    if (v.equals(parentVariable)) {
                        continue;
                    }
                    if (seenVariables.add(v)) {
                        queue.addLast(v);
                    }
                }
            }

            return childMask;
        }

        private static Map<String, List<Integer>> buildIncidence(
                final List<JoinEstimator.StatementPattern> patterns) {

            final Map<String, List<Integer>> incidence = new HashMap<>();
            for (int i = 0; i < patterns.size(); i++) {
                for (final String variable : patterns.get(i).variables()) {
                    incidence.computeIfAbsent(variable, ignored -> new ArrayList<>()).add(i);
                }
            }
            final Map<String, List<Integer>> frozen = new HashMap<>();
            for (final Map.Entry<String, List<Integer>> e : incidence.entrySet()) {
                frozen.put(e.getKey(), List.copyOf(e.getValue()));
            }
            return Collections.unmodifiableMap(frozen);
        }
    }
}
