
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;

import org.apache.datasketches.common.ResizeFactor;
import org.apache.datasketches.tuple.arrayofdoubles.ArrayOfDoublesCombiner;
import org.apache.datasketches.tuple.arrayofdoubles.ArrayOfDoublesCompactSketch;
import org.apache.datasketches.tuple.arrayofdoubles.ArrayOfDoublesIntersection;
import org.apache.datasketches.tuple.arrayofdoubles.ArrayOfDoublesSetOperationBuilder;
import org.apache.datasketches.tuple.arrayofdoubles.ArrayOfDoublesSketch;
import org.apache.datasketches.tuple.arrayofdoubles.ArrayOfDoublesSketchIterator;
import org.apache.datasketches.tuple.arrayofdoubles.ArrayOfDoublesUpdatableSketch;
import org.apache.datasketches.tuple.arrayofdoubles.ArrayOfDoublesUpdatableSketchBuilder;

/**
 * JoinEstimator is the sketch algebra used by the optimizer.
 *
 * <p>It assumes that each base statement-pattern synopsis has already been built offline.
 * The synopsis must expose:</p>
 *
 * <ul>
 *   <li>a degree sketch d_p^x keyed by bindings of x, with summary value = number of
 *       rows produced by pattern p when x is fixed;</li>
 *   <li>for binary patterns p(x,y), a neighborhood sketch N_p^{x->y}(x=a) keyed by y
 *       with summary value = multiplicity f_p(a,y).</li>
 * </ul>
 *
 * <p>The code below keeps one double summary per retained key. The main operators are:</p>
 *
 * <ul>
 *   <li>dot(A, B) = sum_k A[k] * B[k]</li>
 *   <li>multiply(A, B) = pointwise product on the same key domain</li>
 *   <li>push(M_x, p(x,y)) = sketch on y with summaries sum_x M_x[x] * f_p(x,y)</li>
 * </ul>
 *
 * <p>Important implementation detail: DataSketches iterators expose retained hash keys,
 * not the original keys. The estimator therefore depends on a SketchKeyResolver that can
 * map a retained hash back to the original term id used when the synopsis was built.</p>
 */
public final class JoinEstimator {

    public static final int DEFAULT_NUM_VALUES = 1;

    private final SynopsisCatalog catalog;
    private final SketchKeyResolver keyResolver;
    private final int nominalEntries;
    private final long seed;
    private final ArrayOfDoublesUpdatableSketchBuilder updatableBuilder;
    private final ArrayOfDoublesSetOperationBuilder setOperationBuilder;
    private final ArrayOfDoublesCombiner multiplyCombiner = (a, b) -> new double[] { a[0] * b[0] };

    public JoinEstimator(
            final SynopsisCatalog catalog,
            final SketchKeyResolver keyResolver,
            final int nominalEntries,
            final long seed) {

        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver");
        this.nominalEntries = nominalEntries;
        this.seed = seed;

        this.updatableBuilder = new ArrayOfDoublesUpdatableSketchBuilder()
                .setNominalEntries(nominalEntries)
                .setNumberOfValues(DEFAULT_NUM_VALUES)
                .setSeed(seed)
                .setResizeFactor(ResizeFactor.X8)
                .setSamplingProbability(1.0f);

        this.setOperationBuilder = new ArrayOfDoublesSetOperationBuilder()
                .setNominalEntries(nominalEntries)
                .setNumberOfValues(DEFAULT_NUM_VALUES)
                .setSeed(seed);
    }

    public int nominalEntries() {
        return nominalEntries;
    }

    public long seed() {
        return seed;
    }

    public PatternSynopsis synopsisOf(final StatementPattern pattern) {
        return catalog.synopsis(pattern);
    }

    /**
     * Estimates the row count of a single base pattern.
     */
    public double estimatePatternRows(final StatementPattern pattern) {
        final PatternSynopsis synopsis = synopsisOf(pattern);
        if (synopsis.groundRows() != null) {
            return synopsis.groundRows();
        }
        if (pattern.variables().isEmpty()) {
            throw new IllegalStateException("Ground pattern requires groundRows override: " + pattern);
        }
        final String anchor = pattern.variables().iterator().next();
        return estimateSum(synopsis.degreeSketch(anchor));
    }

    /**
     * Degree sketch d_p^x keyed by x.
     */
    public ArrayOfDoublesSketch degreeSketch(final StatementPattern pattern, final String variable) {
        return synopsisOf(pattern).degreeSketch(variable);
    }

    /**
     * Message for a singleton leaf pattern rooted at one of its variables.
     * For unary and binary patterns this is simply d_p^x.
     */
    public Message leafMessage(final StatementPattern pattern, final String rootVariable) {
        requireVariableOnPattern(pattern, rootVariable);
        return new Message(rootVariable, snapshot(degreeSketch(pattern, rootVariable)));
    }

    /**
     * Estimates rows produced when a partial plan summarized on {@code sharedVariable}
     * is extended with {@code nextPattern}.
     */
    public double estimateOutputRows(
            final Message partialOnSharedVariable,
            final StatementPattern nextPattern,
            final String sharedVariable) {

        requireSameVariable(partialOnSharedVariable.variable(), sharedVariable);
        return dot(partialOnSharedVariable.sketch(), degreeSketch(nextPattern, sharedVariable));
    }

    /**
     * Pointwise product of multiple sketches on the same variable domain.
     */
    public Message multiply(final String variable, final List<Message> factors) {
        if (factors.isEmpty()) {
            throw new IllegalArgumentException("Cannot multiply an empty factor list.");
        }
        ArrayOfDoublesCompactSketch current = null;
        for (final Message factor : factors) {
            requireSameVariable(variable, factor.variable());
            current = (current == null)
                    ? factor.sketch()
                    : intersect(current, factor.sketch(), multiplyCombiner);
        }
        return new Message(variable, current);
    }

    /**
     * Convenience overload for multiplying two messages on the same variable.
     */
    public Message pointwiseMultiply(final Message left, final Message right) {
        requireSameVariable(left.variable(), right.variable());
        return new Message(left.variable(), intersect(left.sketch(), right.sketch(), multiplyCombiner));
    }

    /**
     * Dot product between two tuple sketches on the same key space.
     * Implemented as an intersection with value multiplication, followed by
     * Horvitz-Thompson scaling using theta(result).
     */
    public double dot(final ArrayOfDoublesSketch left, final ArrayOfDoublesSketch right) {
        return estimateSum(intersect(left, right, multiplyCombiner));
    }

    /**
     * Estimated sum of summary[0] over the full key domain represented by the sketch.
     */
    public double estimateSum(final ArrayOfDoublesSketch sketch) {
        if (sketch == null || sketch.isEmpty()) {
            return 0.0d;
        }
        double retained = 0.0d;
        final ArrayOfDoublesSketchIterator it = sketch.iterator();
        while (it.next()) {
            retained += firstValue(it.getValues());
        }
        return retained / sketch.getTheta();
    }

    /**
     * Pushes a message from {@code fromVariable} to {@code toVariable} across a binary pattern.
     *
     * <p>For each retained x in the input message, this uses the x-conditioned neighborhood
     * synopsis N_p^{x->y}(x) and contributes:</p>
     *
     * <pre>
     *   contribution(y) = M(x) * f_p(x,y) / (theta_input * theta_neighborhood)
     * </pre>
     *
     * <p>The double scaling is the Horvitz-Thompson correction for the retained x sample and
     * the retained neighborhood sample.</p>
     */
    public Message push(
            final Message input,
            final StatementPattern edgePattern,
            final String fromVariable,
            final String toVariable) {

        requireSameVariable(input.variable(), fromVariable);

        final PatternSynopsis synopsis = synopsisOf(edgePattern);
        final ArrayOfDoublesUpdatableSketch output = newMutableSketch();
        final double thetaInput = safeTheta(input.sketch());

        final ArrayOfDoublesSketchIterator sourceIterator = input.sketch().iterator();
        while (sourceIterator.next()) {
            final long retainedFromHash = sourceIterator.getKey();
            final long fromOriginalKey = resolveOriginalKey(retainedFromHash);
            final double messageWeight = firstValue(sourceIterator.getValues());

            final ArrayOfDoublesSketch neighborhood =
                    synopsis.neighborhood(fromVariable, toVariable, fromOriginalKey);

            if (neighborhood == null || neighborhood.isEmpty()) {
                continue;
            }

            final double thetaNeighborhood = safeTheta(neighborhood);
            final double scale = messageWeight / (thetaInput * thetaNeighborhood);

            final ArrayOfDoublesSketchIterator neighborhoodIterator = neighborhood.iterator();
            while (neighborhoodIterator.next()) {
                final long retainedToHash = neighborhoodIterator.getKey();
                final long toOriginalKey = resolveOriginalKey(retainedToHash);
                final double edgeMultiplicity = firstValue(neighborhoodIterator.getValues());
                output.update(toOriginalKey, singleton(edgeMultiplicity * scale));
            }
        }

        return new Message(toVariable, output.compact());
    }

    /**
     * Materializes a mutable sketch with the standard configuration used by this estimator.
     */
    public ArrayOfDoublesUpdatableSketch newMutableSketch() {
        return updatableBuilder.build();
    }

    /**
     * Creates an immutable snapshot. Useful when a caller wants a compact sketch boundary.
     */
    public ArrayOfDoublesCompactSketch snapshot(final ArrayOfDoublesSketch sketch) {
        if (sketch instanceof ArrayOfDoublesCompactSketch compact) {
            return compact;
        }
        if (sketch instanceof ArrayOfDoublesUpdatableSketch updatable) {
            return updatable.compact();
        }
        final ArrayOfDoublesUpdatableSketch copy = newMutableSketch();
        final ArrayOfDoublesSketchIterator it = sketch.iterator();
        while (it.next()) {
            final long originalKey = resolveOriginalKey(it.getKey());
            copy.update(originalKey, singleton(firstValue(it.getValues()) / safeTheta(sketch)));
        }
        return copy.compact();
    }

    private ArrayOfDoublesCompactSketch intersect(
            final ArrayOfDoublesSketch left,
            final ArrayOfDoublesSketch right,
            final ArrayOfDoublesCombiner combiner) {

        final ArrayOfDoublesIntersection intersection = setOperationBuilder.buildIntersection();
        intersection.intersect(left, combiner);
        intersection.intersect(right, combiner);
        return intersection.getResult();
    }

    private long resolveOriginalKey(final long retainedHash) {
        return keyResolver.resolve(retainedHash).orElseThrow(
                () -> new IllegalStateException(
                        "No original-key mapping found for retained sketch hash " + retainedHash
                                + ". Populate SketchKeyResolver when you build the synopses."));
    }

    private static void requireSameVariable(final String actual, final String expected) {
        if (!Objects.equals(actual, expected)) {
            throw new IllegalArgumentException("Variable mismatch. Expected " + expected + " but got " + actual);
        }
    }

    private static void requireVariableOnPattern(final StatementPattern pattern, final String variable) {
        if (!pattern.variables().contains(variable)) {
            throw new IllegalArgumentException("Variable " + variable + " is not on pattern " + pattern);
        }
    }

    private static double safeTheta(final ArrayOfDoublesSketch sketch) {
        final double theta = sketch.getTheta();
        if (theta <= 0.0d) {
            throw new IllegalStateException("Invalid theta <= 0 for sketch: " + sketch);
        }
        return theta;
    }

    private static double firstValue(final double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalStateException("Expected at least one summary value.");
        }
        return values[0];
    }

    private static double[] singleton(final double value) {
        return new double[] { value };
    }

    // --------------------------------------------------------------------------------------------
    // Catalog / resolver abstractions
    // --------------------------------------------------------------------------------------------

    public interface SynopsisCatalog {
        PatternSynopsis synopsis(StatementPattern pattern);
    }

    public interface SketchKeyResolver {
        OptionalLong resolve(long retainedHash);
    }

    public static final class InMemorySynopsisCatalog implements SynopsisCatalog {
        private final Map<StatementPattern, PatternSynopsis> synopses;

        public InMemorySynopsisCatalog(final Map<StatementPattern, PatternSynopsis> synopses) {
            this.synopses = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(synopses, "synopses")));
        }

        @Override
        public PatternSynopsis synopsis(final StatementPattern pattern) {
            final PatternSynopsis synopsis = synopses.get(pattern);
            if (synopsis == null) {
                throw new IllegalArgumentException("No synopsis registered for pattern: " + pattern);
            }
            return synopsis;
        }
    }

    public static final class MapBackedSketchKeyResolver implements SketchKeyResolver {
        private final Map<Long, Long> retainedHashToOriginal = new LinkedHashMap<>();

        public void register(final long retainedHash, final long originalKey) {
            final Long previous = retainedHashToOriginal.putIfAbsent(retainedHash, originalKey);
            if (previous != null && previous.longValue() != originalKey) {
                throw new IllegalStateException(
                        "Hash collision in retained-key dictionary: " + retainedHash
                                + " mapped to both " + previous + " and " + originalKey);
            }
        }

        @Override
        public OptionalLong resolve(final long retainedHash) {
            final Long original = retainedHashToOriginal.get(retainedHash);
            return original == null ? OptionalLong.empty() : OptionalLong.of(original);
        }
    }

    // --------------------------------------------------------------------------------------------
    // Base synopsis model
    // --------------------------------------------------------------------------------------------

    /**
     * Immutable statement-pattern synopsis.
     *
     * <p>degreeByVariable contains d_p^x.</p>
     * <p>neighborhoods contains N_p^{from->to}(bindingOfFrom).</p>
     */
    public static final class PatternSynopsis {
        private final StatementPattern pattern;
        private final Map<String, ArrayOfDoublesCompactSketch> degreeByVariable;
        private final Map<Direction, Map<Long, ArrayOfDoublesCompactSketch>> neighborhoods;
        private final Double groundRows;

        public PatternSynopsis(
                final StatementPattern pattern,
                final Map<String, ArrayOfDoublesCompactSketch> degreeByVariable,
                final Map<Direction, Map<Long, ArrayOfDoublesCompactSketch>> neighborhoods,
                final Double groundRows) {

            this.pattern = Objects.requireNonNull(pattern, "pattern");
            this.degreeByVariable = copyOneLevel(degreeByVariable);
            this.neighborhoods = deepCopy(neighborhoods);
            this.groundRows = groundRows;
        }

        public StatementPattern pattern() {
            return pattern;
        }

        public Double groundRows() {
            return groundRows;
        }

        public ArrayOfDoublesCompactSketch degreeSketch(final String variable) {
            final ArrayOfDoublesCompactSketch sketch = degreeByVariable.get(variable);
            if (sketch == null) {
                throw new IllegalArgumentException("No degree sketch for variable " + variable + " on " + pattern);
            }
            return sketch;
        }

        public ArrayOfDoublesCompactSketch neighborhood(
                final String fromVariable,
                final String toVariable,
                final long fromBinding) {

            final Map<Long, ArrayOfDoublesCompactSketch> byBinding =
                    neighborhoods.get(new Direction(fromVariable, toVariable));

            if (byBinding == null) {
                return null;
            }
            return byBinding.get(fromBinding);
        }

        public Set<String> supportedVariables() {
            return degreeByVariable.keySet();
        }

        private static <K, V> Map<K, V> copyOneLevel(final Map<K, V> input) {
            if (input == null) {
                return Map.of();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(input));
        }

        private static Map<Direction, Map<Long, ArrayOfDoublesCompactSketch>> deepCopy(
                final Map<Direction, Map<Long, ArrayOfDoublesCompactSketch>> input) {

            if (input == null) {
                return Map.of();
            }

            final Map<Direction, Map<Long, ArrayOfDoublesCompactSketch>> outer = new LinkedHashMap<>();
            for (final Map.Entry<Direction, Map<Long, ArrayOfDoublesCompactSketch>> e : input.entrySet()) {
                outer.put(e.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())));
            }
            return Collections.unmodifiableMap(outer);
        }
    }

    /**
     * Builder used offline while materializing synopses from data.
     */
    public static final class PatternSynopsisBuilder {
        private final StatementPattern pattern;
        private final ArrayOfDoublesUpdatableSketchBuilder builder;
        private final Map<String, ArrayOfDoublesUpdatableSketch> degreeByVariable = new LinkedHashMap<>();
        private final Map<Direction, Map<Long, ArrayOfDoublesUpdatableSketch>> neighborhoods = new LinkedHashMap<>();
        private Double groundRows;

        public PatternSynopsisBuilder(
                final StatementPattern pattern,
                final int nominalEntries,
                final long seed) {

            this.pattern = Objects.requireNonNull(pattern, "pattern");
            this.builder = new ArrayOfDoublesUpdatableSketchBuilder()
                    .setNominalEntries(nominalEntries)
                    .setNumberOfValues(DEFAULT_NUM_VALUES)
                    .setSeed(seed)
                    .setResizeFactor(ResizeFactor.X8)
                    .setSamplingProbability(1.0f);
        }

        public PatternSynopsisBuilder setGroundRows(final double rows) {
            this.groundRows = rows;
            return this;
        }

        /**
         * Adds one unary match (or one grounded projection row) for a unary pattern.
         */
        public PatternSynopsisBuilder addUnaryMatch(
                final String variable,
                final long bindingKey,
                final double multiplicity) {

            ensurePatternContains(variable);
            degree(variable).update(bindingKey, singleton(multiplicity));
            return this;
        }

        /**
         * Adds one binary row (x = leftKey, y = rightKey) with the provided multiplicity.
         * This updates both degree directions and both conditioned neighborhoods.
         */
        public PatternSynopsisBuilder addBinaryMatch(
                final String leftVariable,
                final long leftKey,
                final String rightVariable,
                final long rightKey,
                final double multiplicity) {

            ensurePatternContains(leftVariable);
            ensurePatternContains(rightVariable);

            degree(leftVariable).update(leftKey, singleton(multiplicity));
            degree(rightVariable).update(rightKey, singleton(multiplicity));

            neighborhood(leftVariable, rightVariable, leftKey).update(rightKey, singleton(multiplicity));
            neighborhood(rightVariable, leftVariable, rightKey).update(leftKey, singleton(multiplicity));
            return this;
        }

        public PatternSynopsis build() {
            final Map<String, ArrayOfDoublesCompactSketch> degreeSnapshots = new LinkedHashMap<>();
            for (final Map.Entry<String, ArrayOfDoublesUpdatableSketch> e : degreeByVariable.entrySet()) {
                degreeSnapshots.put(e.getKey(), e.getValue().compact());
            }

            final Map<Direction, Map<Long, ArrayOfDoublesCompactSketch>> neighborhoodSnapshots = new LinkedHashMap<>();
            for (final Map.Entry<Direction, Map<Long, ArrayOfDoublesUpdatableSketch>> outer : neighborhoods.entrySet()) {
                final Map<Long, ArrayOfDoublesCompactSketch> inner = new LinkedHashMap<>();
                for (final Map.Entry<Long, ArrayOfDoublesUpdatableSketch> innerEntry : outer.getValue().entrySet()) {
                    inner.put(innerEntry.getKey(), innerEntry.getValue().compact());
                }
                neighborhoodSnapshots.put(outer.getKey(), inner);
            }

            return new PatternSynopsis(pattern, degreeSnapshots, neighborhoodSnapshots, groundRows);
        }

        private ArrayOfDoublesUpdatableSketch degree(final String variable) {
            return degreeByVariable.computeIfAbsent(variable, ignored -> builder.build());
        }

        private ArrayOfDoublesUpdatableSketch neighborhood(
                final String fromVariable,
                final String toVariable,
                final long fromBinding) {

            final Direction direction = new Direction(fromVariable, toVariable);
            final Map<Long, ArrayOfDoublesUpdatableSketch> byBinding =
                    neighborhoods.computeIfAbsent(direction, ignored -> new LinkedHashMap<>());

            return byBinding.computeIfAbsent(fromBinding, ignored -> builder.build());
        }

        private void ensurePatternContains(final String variable) {
            if (!pattern.variables().contains(variable)) {
                throw new IllegalArgumentException("Variable " + variable + " is not present on " + pattern);
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // Query / pattern model
    // --------------------------------------------------------------------------------------------

    public record Message(String variable, ArrayOfDoublesCompactSketch sketch) {
        public Message {
            Objects.requireNonNull(variable, "variable");
            Objects.requireNonNull(sketch, "sketch");
        }
    }

    public record Direction(String fromVariable, String toVariable) {
        public Direction {
            Objects.requireNonNull(fromVariable, "fromVariable");
            Objects.requireNonNull(toVariable, "toVariable");
        }
    }

    public interface Slot {
        boolean isVariable();
        String asVariable();
        Long asConstant();
    }

    public record Var(String name) implements Slot {
        public Var {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Variable name must not be blank.");
            }
        }

        @Override
        public boolean isVariable() {
            return true;
        }

        @Override
        public String asVariable() {
            return name;
        }

        @Override
        public Long asConstant() {
            return null;
        }

        @Override
        public String toString() {
            return "?" + name;
        }
    }

    public record Const(long termId) implements Slot {
        @Override
        public boolean isVariable() {
            return false;
        }

        @Override
        public String asVariable() {
            return null;
        }

        @Override
        public Long asConstant() {
            return termId;
        }

        @Override
        public String toString() {
            return Long.toString(termId);
        }
    }

    public record StatementPattern(String id, Slot subject, Slot predicate, Slot object) {
        public StatementPattern {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(subject, "subject");
            Objects.requireNonNull(predicate, "predicate");
            Objects.requireNonNull(object, "object");
        }

        public Set<String> variables() {
            final LinkedHashSet<String> vars = new LinkedHashSet<>();
            addIfVariable(vars, subject);
            addIfVariable(vars, predicate);
            addIfVariable(vars, object);
            return Collections.unmodifiableSet(vars);
        }

        public boolean containsVariable(final String variable) {
            return variables().contains(variable);
        }

        public int arity() {
            return variables().size();
        }

        public boolean isUnary() {
            return arity() == 1;
        }

        public boolean isBinary() {
            return arity() == 2;
        }

        public String otherVariable(final String variable) {
            if (!isBinary()) {
                throw new IllegalStateException("otherVariable() requires a binary pattern: " + this);
            }
            for (final String v : variables()) {
                if (!v.equals(variable)) {
                    return v;
                }
            }
            throw new IllegalArgumentException("Variable " + variable + " is not on " + this);
        }

        public Set<String> sharedVariables(final StatementPattern other) {
            final LinkedHashSet<String> shared = new LinkedHashSet<>(variables());
            shared.retainAll(other.variables());
            return Collections.unmodifiableSet(shared);
        }

        private static void addIfVariable(final Set<String> output, final Slot slot) {
            if (slot.isVariable()) {
                output.add(slot.asVariable());
            }
        }
    }
}
