package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LeftJoinPreFilterQueryEvaluationStepTest {

    private static final ValueFactory VALUE_FACTORY = SimpleValueFactory.getInstance();

    private QueryBindingSet bindingSet;

    @BeforeEach
    void setUp() {
        bindingSet = new QueryBindingSet(1);
        bindingSet.addBinding("a", VALUE_FACTORY.createLiteral(42));
    }

    @Test
    @DisplayName("when condition evaluates to false, then don't evaluate wrapped")
    void skipWrappedEvaluation() {
        var wrapped = new TestQueryEvaluationStep();
        QueryEvaluationStep preFilter = createPreFilter(wrapped, bindings -> BooleanLiteral.valueOf(false));

        try (var ignored = preFilter.evaluate(bindingSet)) {
            assertThat(wrapped.isEvaluated).isFalse();
        }
    }

    @Test
    @DisplayName("when condition evaluates to false, then return empty iteration")
    void returnOnlyInput() {
        var wrapped = new TestQueryEvaluationStep();
        QueryEvaluationStep preFilter = createPreFilter(wrapped, bindings -> BooleanLiteral.valueOf(false));

        var result = preFilter.evaluate(bindingSet);

        assertThat(result).isEqualTo(QueryEvaluationStep.EMPTY_ITERATION);
    }

    @Test
    @DisplayName("when condition evaluates to true, then evaluate and return wrapped output")
    void evaluateWrapped() {
        var wrapped = new TestQueryEvaluationStep();
        QueryEvaluationStep preFilter = createPreFilter(wrapped, bindings -> BooleanLiteral.valueOf(true));

        var result = preFilter.evaluate(bindingSet);

        assertThat(result)
                .toIterable()
                .map(bindings -> bindings.getValue("b"))
                .containsExactly(VALUE_FACTORY.createLiteral("abc"), VALUE_FACTORY.createLiteral("xyz"));
    }

    @Test
    @DisplayName("when condition evaluates to true, then only evaluate condition once")
    void onlyEvaluatesConditionOnce() {
        var evaluations = new AtomicInteger(0);
        QueryValueEvaluationStep condition = bindings -> {
            evaluations.incrementAndGet();
            return BooleanLiteral.valueOf(true);
        };
        QueryEvaluationStep preFilter = createPreFilter(new TestQueryEvaluationStep(), condition);

        try (var ignored = preFilter.evaluate(bindingSet)) {
            assertThat(evaluations).hasValue(1);
        }
    }

    private QueryEvaluationStep createPreFilter(TestQueryEvaluationStep wrapped, QueryValueEvaluationStep condition) {
        var evaluator = new ScopeBindingsJoinConditionEvaluator(Set.of("a", "b"), condition);
        return new LeftJoinPreFilterQueryEvaluationStep(wrapped, evaluator);
    }

    private static class TestQueryEvaluationStep implements QueryEvaluationStep {

        private final Queue<String> values = new LinkedList<>();
        private boolean isEvaluated = false;

        private TestQueryEvaluationStep() {
            values.add("abc");
            values.add("xyz");
        }

        @Override
        public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
            isEvaluated = true;
            return new CloseableIteration<>() {
                @Override
                public void close() {
                    // Nothing to close
                }

                @Override
                public boolean hasNext() {
                    return !values.isEmpty();
                }

                @Override
                public BindingSet next() {
                    var output = new QueryBindingSet(2);
                    bindings.forEach(output::addBinding);
                    output.addBinding("b", VALUE_FACTORY.createLiteral(values.poll()));
                    return output;
                }
            };
        }
    }
}