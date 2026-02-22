/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.collection.factory.api.BindingSetEntry;
import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.impl.DefaultCollectionFactory;
import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.AggregateFunctionCall;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.UnaryValueOperator;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.util.MathUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateCollector;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.AggregateFunction;
import org.eclipse.rdf4j.query.parser.sparql.aggregate.CustomAggregateFunctionRegistry;

/**
 * @author David Huynh
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 * @author James Leigh
 * @author Jerven Bolleman
 * @author Tomas Kovachev
 */
public class GroupIterator extends AbstractCloseableIteratorIteration<BindingSet> {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final EvaluationStrategy strategy;

	private final BindingSet parentBindings;

	private final Group group;

	private final QueryEvaluationContext context;

	private final QueryEvaluationStep arguments;

	// The iteration of the arguments, stored while building entries for allowing premature closing
	private volatile CloseableIteration<BindingSet> argumentsIter;

	private final ValueFactory vf;

	private final CollectionFactory cf;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public GroupIterator(EvaluationStrategy strategy, Group group, BindingSet parentBindings,
			QueryEvaluationContext context) throws QueryEvaluationException {
		this(strategy, group, parentBindings, 0, context);
	}

	@Deprecated
	public GroupIterator(EvaluationStrategy strategy, Group group, BindingSet parentBindings,
			long iterationCacheSyncThreshold, QueryEvaluationContext context) throws QueryEvaluationException {
		this(strategy, group, parentBindings, iterationCacheSyncThreshold, context, SimpleValueFactory.getInstance(),
				new DefaultCollectionFactory());
	}

	public GroupIterator(EvaluationStrategy strategy, Group group, BindingSet parentBindings,
			long iterationCacheSyncThreshold, QueryEvaluationContext context, ValueFactory vf, CollectionFactory cf)
			throws QueryEvaluationException {
		this.strategy = strategy;
		this.group = group;
		this.parentBindings = parentBindings;
//		this is ignored as it is just a left over from earlier, this is now stored in the collection factory.
//		this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
		this.context = context;
		this.vf = vf;
		this.cf = cf;
		this.arguments = strategy.precompile(group.getArg(), context);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void handleClose() throws QueryEvaluationException {
		try {
			cf.close();
		} finally {
			var iter = argumentsIter;
			if (iter != null)
				iter.close();
		}
	}

	@Override
	protected Iterator<BindingSet> getIterator() throws QueryEvaluationException {
		List<AggregatePredicateCollectorSupplier<?, ?>> aggregates = makeAggregates();

		Supplier<MutableBindingSet> makeNewBindingSet;
		if (parentBindings.isEmpty()) {
			makeNewBindingSet = context::createBindingSet;
		} else {
			makeNewBindingSet = () -> context.createBindingSet(parentBindings);
		}

		List<Function<BindingSet, Value>> getValues = new ArrayList<>();
		List<BiConsumer<Value, MutableBindingSet>> setBindings = new ArrayList<>();
		for (String name : group.getGroupBindingNames()) {
			Function<BindingSet, Value> getValue = context.getValue(name);
			BiConsumer<Value, MutableBindingSet> setBinding = context.setBinding(name);
			if (getValue != null) {
				getValues.add(getValue);
				setBindings.add(setBinding);
			}
		}

		BiConsumer<Entry, MutableBindingSet> bindSolution = makeBindSolution(aggregates);
		Collection<Entry> entries = buildEntries(aggregates);
		Set<BindingSet> bindingSets = cf.createSetOfBindingSets(context::createBindingSet, context::hasBinding,
				context::getValue, context::setBinding);
		BiConsumer<BindingSet, MutableBindingSet> setValues = makeSetValues(getValues, setBindings);
		for (Entry entry : entries) {
			MutableBindingSet sol = makeNewBindingSet.get();

			BindingSet prototype = entry.getPrototype();
			if (prototype != null) {
				setValues.accept(prototype, sol);
			}

			bindSolution.accept(entry, sol);
			bindingSets.add(sol);
		}

		return bindingSets.iterator();
	}

	/**
	 * Build a single method that sets all values without a loop or lookups during evaluation.
	 *
	 * @param getValues   the methods to access values in a bindingset
	 * @param setBindings the methods to set values in a bindingset
	 * @return a BiConsumer that takes the prototype and sets parts into solution as required
	 */
	private BiConsumer<BindingSet, MutableBindingSet> makeSetValues(List<Function<BindingSet, Value>> getValues,
			List<BiConsumer<Value, MutableBindingSet>> setBindings) {
		if (getValues.isEmpty()) {
			return (prototype, solution) -> {
			};
		}
		BiConsumer<BindingSet, MutableBindingSet> consumeAValue = makeSetAValue(getValues, setBindings, 0);
		for (int i = 1; i < getValues.size(); i++) {
			consumeAValue = consumeAValue.andThen(makeSetAValue(getValues, setBindings, i));
		}
		return consumeAValue;
	}

	private BiConsumer<BindingSet, MutableBindingSet> makeSetAValue(List<Function<BindingSet, Value>> getValues,
			List<BiConsumer<Value, MutableBindingSet>> setBindings, int i) {
		Function<BindingSet, Value> getBinding = getValues.get(i);
		BiConsumer<Value, MutableBindingSet> setBinding = setBindings.get(i);
		BiConsumer<BindingSet, MutableBindingSet> nextConsumeAValue = (prototype, solution) -> {
			Value value = getBinding.apply(prototype);
			if (value != null) {
				// Potentially overwrites bindings from super
				setBinding.accept(value, solution);
			}
		};
		return nextConsumeAValue;
	}

	private BiConsumer<Entry, MutableBindingSet> makeBindSolution(
			List<AggregatePredicateCollectorSupplier<?, ?>> aggregates) {
		BiConsumer<Entry, MutableBindingSet> bindSolution = null;
		for (int i = 0; i < aggregates.size(); i++) {
			AggregatePredicateCollectorSupplier<?, ?> a = aggregates.get(i);
			BiConsumer<Value, MutableBindingSet> setBinding = context.setBinding(a.name);
			final int j = i;
			BiConsumer<Entry, MutableBindingSet> biConsumer = (e, bs) -> {
				try {
					Value value = e.collectors.get(j).getFinalValue();
					if (value != null) {
						// Potentially overwrites bindings from super
						setBinding.accept(value, bs);
					}
				} catch (ValueExprEvaluationException ex) {
					// There was a type error when calculating the value of the aggregate. We
					// silently ignore the error,
					// resulting in no result value being bound.
				}
			};
			if (bindSolution == null) {
				bindSolution = biConsumer;
			} else {
				bindSolution = bindSolution.andThen(biConsumer);
			}
		}
		if (bindSolution == null) {
			return (e, bs) -> {
			};
		} else {
			return bindSolution;
		}
	}

	private List<AggregatePredicateCollectorSupplier<?, ?>> makeAggregates() {
		List<AggregatePredicateCollectorSupplier<?, ?>> aggregates = new ArrayList<>(
				group.getGroupBindingNames().size());
		for (GroupElem ge : group.getGroupElements()) {
			AggregatePredicateCollectorSupplier<?, ?> create = create(ge, vf);
			if (create != null) {
				aggregates.add(create);
			}
		}
		return aggregates;
	}

	private Collection<Entry> buildEntries(List<AggregatePredicateCollectorSupplier<?, ?>> aggregates)
			throws QueryEvaluationException {
		// store the arguments' iterator so it can be closed while building entries
		this.argumentsIter = arguments.evaluate(parentBindings);
		try (var iter = argumentsIter) {
			long inputRows = 0;
			long aggregateEvalCount = 0;
			if (!iter.hasNext()) {
				Collection<Entry> emptyEntries = emptySolutionSpecialCase(aggregates);
				recordGroupMetrics(0, emptyEntries, aggregateEvalCount);
				return emptyEntries;
			}

			List<Function<BindingSet, Value>> getValues = group.getGroupBindingNames()
					.stream()
					.map(n -> context.getValue(n))
					.collect(Collectors.toList());

			// TODO: this is an in memory map with no backing into any disk form.
			// Fixing this requires separating the computation of the aggregates and their
			// distinct sets if needed from the intermediary values.

			Map<BindingSetKey, Entry> entries = cf.createGroupByMap();
			// Make an optimized hash function valid during this query evaluation step.
			ToIntFunction<BindingSet> hashMaker = cf.hashOfBindingSetFuntion(getValues);
			while (!isClosed() && iter.hasNext()) {
				BindingSet sol = iter.next();
				inputRows++;
				// The binding set key will be constant
				BindingSetKey key = cf.createBindingSetKey(sol, getValues, hashMaker);
				Entry entry = entries.get(key);
				if (entry == null) {
					List<AggregateCollector> collectors = makeCollectors(aggregates);
					List<Predicate<?>> predicates = new ArrayList<>(aggregates.size());
					for (AggregatePredicateCollectorSupplier<?, ?> a : aggregates) {
						predicates.add(a.makePotentialDistinctTest.get());
					}

					entry = new Entry(sol, collectors, predicates);
					entries.put(key, entry);
				}

				entry.addSolution(sol, aggregates);
				aggregateEvalCount += aggregates.size();
			}
			Collection<Entry> values = entries.values();
			recordGroupMetrics(inputRows, values, aggregateEvalCount);
			return values;
		} finally {
			this.argumentsIter = null;
		}
	}

	private void recordGroupMetrics(long inputRows, Collection<Entry> entries, long aggregateEvalCount) {
		long groupsCreated = entries == null ? 0 : entries.size();
		long maxGroupSize = entries == null ? 0 : entries.stream().mapToLong(Entry::getSize).max().orElse(0);

		group.setLongMetricActual(TelemetryMetricNames.GROUPS_CREATED_ACTUAL, groupsCreated);
		group.setLongMetricActual(TelemetryMetricNames.MAX_GROUP_SIZE_ACTUAL, maxGroupSize);
		group.setLongMetricActual(TelemetryMetricNames.AGGREGATE_EVAL_COUNT_ACTUAL, aggregateEvalCount);
		if (groupsCreated > 0) {
			group.setDoubleMetricActual(TelemetryMetricNames.AVG_GROUP_SIZE_ACTUAL, inputRows / (double) groupsCreated);
		}
	}

	private List<Entry> emptySolutionSpecialCase(List<AggregatePredicateCollectorSupplier<?, ?>> aggregates) {
		// no solutions, but if we are not explicitly grouping and aggregates are
		// present, we still need to process them to produce a zero-result.
		if (group.getGroupBindingNames().isEmpty()) {
			if (group.getGroupElements().isEmpty()) {
				final Entry entry = new Entry(null, null, null);
				return List.of(entry);
			} else {
				List<AggregateCollector> collectors = makeCollectors(aggregates);
				List<Predicate<?>> predicates = new ArrayList<>(aggregates.size());
				for (var ag : aggregates) {
					if (ag.agg instanceof WildCardCountAggregate) {
						predicates.add(ALWAYS_TRUE_BINDING_SET);
					} else if (ag.agg instanceof CountAggregate) {
						// Counts are special, because they always return a number related to the number of solutions.
						// which in the empty case should be 0. So we should never accept a value here.
						// Even in the case that the Count is of a constant value.
						predicates.add(ALWAYS_FALSE_VALUE);
					} else {
						predicates.add(ALWAYS_TRUE_VALUE);
					}
				}
				final Entry entry = new Entry(null, collectors, predicates);
				entry.addSolution(EmptyBindingSet.getInstance(), aggregates);
				return List.of(entry);
			}
		}
		return Collections.emptyList();
	}

	private List<AggregateCollector> makeCollectors(List<AggregatePredicateCollectorSupplier<?, ?>> aggregates) {
		List<AggregateCollector> collectors = new ArrayList<>(aggregates.size());
		for (AggregatePredicateCollectorSupplier<?, ?> a : aggregates) {
			collectors.add(a.makeAggregateCollector.get());
		}

		return collectors;
	}

	private static class Entry implements BindingSetEntry {

		private static final long serialVersionUID = 1L;
		private final BindingSet prototype;
		private final List<AggregateCollector> collectors;
		private final List<Predicate<?>> predicates;
		private long size;

		public Entry(BindingSet prototype, List<AggregateCollector> collectors, List<Predicate<?>> predicates)
				throws QueryEvaluationException {
			this.prototype = prototype;
			this.collectors = collectors;
			this.predicates = predicates;
		}

		public void addSolution(BindingSet bs, List<AggregatePredicateCollectorSupplier<?, ?>> operators) {
			size++;
			for (int i = 0; i < operators.size(); i++) {
				AggregatePredicateCollectorSupplier<?, ?> aggregatePredicateCollectorSupplier = operators.get(i);
				aggregatePredicateCollectorSupplier.operate(bs, predicates.get(i), collectors.get(i));
			}
		}

		public BindingSet getPrototype() {
			return prototype;
		}

		public long getSize() {
			return size;
		}
	}

	/**
	 * This is to collect together in operation an aggregate function the name of it. And the suppliers that will give
	 * the unique set and final value collectors per final binding set.
	 * <p>
	 * Making an aggregate function is quite a lot of work and we do not want to repeat that for each final binding.
	 */
	private static class AggregatePredicateCollectorSupplier<T extends AggregateCollector, D> {
		public final String name;
		private final AggregateFunction<T, D> agg;
		private final Supplier<Predicate<D>> makePotentialDistinctTest;
		private final Supplier<T> makeAggregateCollector;

		public AggregatePredicateCollectorSupplier(AggregateFunction<T, D> agg,
				Supplier<Predicate<D>> makePotentialDistinctTest, Supplier<T> makeAggregateCollector, String name) {
			super();
			this.agg = agg;
			this.makePotentialDistinctTest = makePotentialDistinctTest;
			this.makeAggregateCollector = makeAggregateCollector;
			this.name = name;
		}

		private void operate(BindingSet bs, Predicate<?> predicate, Object t) {
			agg.processAggregate(bs, (Predicate<D>) predicate, (T) t);
		}
	}

	private static final Predicate<BindingSet> ALWAYS_TRUE_BINDING_SET = t -> true;
	private static final Predicate<Value> ALWAYS_TRUE_VALUE = t -> true;
	private static final Predicate<Value> ALWAYS_FALSE_VALUE = t -> false;
	private static final Supplier<Predicate<Value>> ALWAYS_TRUE_VALUE_SUPPLIER = () -> ALWAYS_TRUE_VALUE;

	private AggregatePredicateCollectorSupplier<?, ?> create(GroupElem ge, ValueFactory vf)
			throws QueryEvaluationException {
		AggregateOperator operator = ge.getOperator();

		if (operator instanceof Count) {
			if (((Count) operator).getArg() == null) {
				WildCardCountAggregate wildCardCountAggregate = new WildCardCountAggregate();
				Supplier<Predicate<BindingSet>> potentialDistinctTest = operator.isDistinct() ? DistinctBindingSets::new
						: () -> ALWAYS_TRUE_BINDING_SET;
				return new AggregatePredicateCollectorSupplier<>(wildCardCountAggregate, potentialDistinctTest,
						() -> new CountCollector(vf), ge.getName());
			} else {
				QueryStepEvaluator f = precompileArg(operator);
				CountAggregate agg = new CountAggregate(f);
				Supplier<Predicate<Value>> predicate = createDistinctValueTest(operator);
				return new AggregatePredicateCollectorSupplier<>(agg, predicate, () -> new CountCollector(vf),
						ge.getName());
			}
		} else if (operator instanceof Min) {
			MinAggregate agg = new MinAggregate(precompileArg(operator), shouldValueComparisonBeStrict());
			Supplier<Predicate<Value>> predicate = createDistinctValueTest(operator);
			return new AggregatePredicateCollectorSupplier<>(agg, predicate, ValueCollector::new, ge.getName());
		} else if (operator instanceof Max) {
			MaxAggregate agg = new MaxAggregate(precompileArg(operator), shouldValueComparisonBeStrict());
			Supplier<Predicate<Value>> predicate = createDistinctValueTest(operator);
			return new AggregatePredicateCollectorSupplier<>(agg, predicate, ValueCollector::new, ge.getName());
		} else if (operator instanceof Sum) {

			SumAggregate agg = new SumAggregate(precompileArg(operator));
			Supplier<Predicate<Value>> predicate = createDistinctValueTest(operator);
			return new AggregatePredicateCollectorSupplier<>(agg, predicate, () -> new IntegerCollector(vf),
					ge.getName());
		} else if (operator instanceof Avg) {
			AvgAggregate agg = new AvgAggregate(precompileArg(operator));
			Supplier<Predicate<Value>> predicate = createDistinctValueTest(operator);
			return new AggregatePredicateCollectorSupplier<>(agg, predicate, () -> new AvgCollector(vf), ge.getName());
		} else if (operator instanceof Sample) {
			SampleAggregate agg = new SampleAggregate(precompileArg(operator));
			Supplier<Predicate<Value>> predicate = createDistinctValueTest(operator);
			return new AggregatePredicateCollectorSupplier<>(agg, predicate, SampleCollector::new, ge.getName());
		} else if (operator instanceof GroupConcat) {
			ValueExpr separatorExpr = ((GroupConcat) operator).getSeparator();
			ConcatAggregate agg;
			if (separatorExpr != null) {
				Value separatorValue = strategy.evaluate(separatorExpr, parentBindings);
				agg = new ConcatAggregate(precompileArg(operator), separatorValue.stringValue());
			} else {
				agg = new ConcatAggregate(precompileArg(operator));
			}
			Supplier<Predicate<Value>> predicate = createDistinctValueTest(operator);
			return new AggregatePredicateCollectorSupplier<>(agg, predicate, () -> new StringBuilderCollector(vf),
					ge.getName());
		} else if (operator instanceof AggregateFunctionCall) {
			var aggOperator = (AggregateFunctionCall) operator;
			Supplier<Predicate<Value>> predicate = createDistinctValueTest(operator);
			var factory = CustomAggregateFunctionRegistry.getInstance().get(aggOperator.getIRI());

			var function = factory.orElseThrow(
					() -> new QueryEvaluationException("Unknown aggregate function '" + aggOperator.getIRI() + "'"))
					.buildFunction(precompileArg(aggOperator));
			return new AggregatePredicateCollectorSupplier<>(function, predicate, () -> factory.get().getCollector(),
					ge.getName());

		}

		return null;
	}

	/**
	 * Create a predicate that tests if the value is distinct (returning true if the value was not seen yet), or always
	 * returns true if the operator is not distinct.
	 *
	 * @param operator
	 * @return a supplier that returns a predicate that tests if the value is distinct, or always returns true if the
	 *         operator is not distinct.
	 */
	private Supplier<Predicate<Value>> createDistinctValueTest(AggregateOperator operator) {
		return operator.isDistinct() ? DistinctValues::new : ALWAYS_TRUE_VALUE_SUPPLIER;
	}

	private QueryStepEvaluator precompileArg(AggregateOperator operator) {
		ValueExpr ve = ((UnaryValueOperator) operator).getArg();
		return new QueryStepEvaluator(strategy.precompile(ve, context));
	}

	private boolean shouldValueComparisonBeStrict() {
		return strategy.getQueryEvaluationMode() == QueryEvaluationMode.STRICT;
	}

	private static class CountCollector implements AggregateCollector {
		private long value;
		private final ValueFactory vf;

		public CountCollector(ValueFactory vf) {
			super();
			this.vf = vf;
		}

		@Override
		public Value getFinalValue() {
			return vf.createLiteral(value, CoreDatatype.XSD.INTEGER);
		}
	}

	private static class ValueCollector implements AggregateCollector {
		private Value value;

		@Override
		public Value getFinalValue() {
			return value;
		}
	}

	private static class IntegerCollector implements AggregateCollector {
		private ValueExprEvaluationException typeError;

		private Literal value;

		public IntegerCollector(ValueFactory vf) {
			super();
			this.value = vf.createLiteral("0", CoreDatatype.XSD.INTEGER);
		}

		public void setTypeError(ValueExprEvaluationException typeError) {
			this.typeError = typeError;
		}

		public boolean hasError() {
			return typeError != null;
		}

		@Override
		public Value getFinalValue() {
			if (typeError != null) {
				// a type error occurred while processing the aggregate, throw it now.
				throw typeError;
			}
			return value;
		}
	}

	private class AvgCollector implements AggregateCollector {
		private final ValueFactory vf;
		private Literal sum;
		private long count;
		private ValueExprEvaluationException typeError;

		public AvgCollector(ValueFactory vf) {
			super();
			this.vf = vf;
			sum = vf.createLiteral("0", CoreDatatype.XSD.INTEGER);
		}

		public void setTypeError(ValueExprEvaluationException typeError) {
			this.typeError = typeError;
		}

		public boolean hasError() {
			return typeError != null;
		}

		@Override
		public Value getFinalValue() throws ValueExprEvaluationException {
			if (typeError != null) {
				// a type error occurred while processing the aggregate, throw it
				// now.
				throw typeError;
			}

			if (count == 0) {
				return vf.createLiteral("0", CoreDatatype.XSD.INTEGER);
			}

			Literal sizeLit = SimpleValueFactory.getInstance().createLiteral(count);
			return MathUtil.compute(sum, sizeLit, MathOp.DIVIDE);
		}
	}

	private class DistinctValues implements Predicate<Value> {
		private final Set<Value> distinctValues;

		public DistinctValues() {
			distinctValues = cf.createValueSet();
		}

		@Override
		public boolean test(Value value) {
			return distinctValues.add(value);
		}
	}

	private class DistinctBindingSets implements Predicate<BindingSet> {
		private final Set<BindingSet> distinctValues;

		public DistinctBindingSets() {
			distinctValues = cf.createSet();
		}

		@Override
		public boolean test(BindingSet value) {
			return distinctValues.add(value);
		}
	}

	private static class CountAggregate extends AggregateFunction<CountCollector, Value> {

		public CountAggregate(Function<BindingSet, Value> f) {
			super(f);
		}

		@Override
		public void processAggregate(BindingSet s, Predicate<Value> distinctValue, CountCollector agv)
				throws QueryEvaluationException {
			Value value = evaluate(s);
			if (value != null && distinctValue.test(value)) {
				agv.value++;
			}
		}
	}

	private static class WildCardCountAggregate extends AggregateFunction<CountCollector, BindingSet> {

		public WildCardCountAggregate() {
			super(null);
		}

		@Override
		public void processAggregate(BindingSet s, Predicate<BindingSet> distinctValue, CountCollector agv)
				throws QueryEvaluationException {
			// wildcard count
			if (!s.isEmpty() && distinctValue.test(s)) {
				agv.value++;
			}
		}
	}

	private class MinAggregate extends AggregateFunction<ValueCollector, Value> {

		private final ValueComparator comparator = new ValueComparator();

		public MinAggregate(Function<BindingSet, Value> f, boolean strict) {
			super(f);
			comparator.setStrict(strict);
		}

		@Override
		public void processAggregate(BindingSet s, Predicate<Value> distinctValue, ValueCollector min)
				throws QueryEvaluationException {
			Value v = evaluate(s);

			if (v != null && distinctValue.test(v)) {
				if (min.value == null) {
					min.value = v;
				} else if (comparator.compare(v, min.value) < 0) {
					min.value = v;
				}
			}
		}
	}

	private static class MaxAggregate extends AggregateFunction<ValueCollector, Value> {

		private final ValueComparator comparator = new ValueComparator();

		public MaxAggregate(Function<BindingSet, Value> f, boolean strict) {
			super(f);
			comparator.setStrict(strict);
		}

		@Override
		public void processAggregate(BindingSet s, Predicate<Value> distinctValue, ValueCollector max)
				throws QueryEvaluationException {
			Value v = evaluate(s);
			if (v != null && distinctValue.test(v)) {
				if (max.value == null) {
					max.value = v;
				} else if (comparator.compare(v, max.value) > 0) {
					max.value = v;
				}
			}
		}
	}

	private static class SumAggregate extends AggregateFunction<IntegerCollector, Value> {
		public SumAggregate(Function<BindingSet, Value> f) {
			super(f);
		}

		@Override
		public void processAggregate(BindingSet s, Predicate<Value> distinctValue, IntegerCollector sum)
				throws QueryEvaluationException {
			if (sum.hasError()) {
				// halt further processing if a type error has been raised
				return;
			}

			Value v = evaluate(s);
			if (v != null) {
				if (v.isLiteral()) {
					if (distinctValue.test(v)) {
						Literal literal = (Literal) v;
						CoreDatatype.XSD coreDatatype = literal.getCoreDatatype().asXSDDatatypeOrNull();
						if (coreDatatype != null && coreDatatype.isNumericDatatype()) {
							sum.value = MathUtil.compute(sum.value, literal, MathOp.PLUS);
						} else {
							sum.setTypeError(new ValueExprEvaluationException("not a number: " + v));
						}
					}
				} else {
					sum.setTypeError(new ValueExprEvaluationException("not a number: " + v));
				}
			}
		}
	}

	private static class AvgAggregate extends AggregateFunction<AvgCollector, Value> {

		public AvgAggregate(Function<BindingSet, Value> operator) {
			super(operator);
		}

		@Override
		public void processAggregate(BindingSet s, Predicate<Value> distinctValue, AvgCollector avg)
				throws QueryEvaluationException {
			if (avg.hasError()) {
				// Prevent calculating the aggregate further if a type error has
				// occurred.
				return;
			}

			Value v = evaluate(s);
			if (distinctValue.test(v)) {
				if (v instanceof Literal) {
					Literal nextLiteral = (Literal) v;
					// check if the literal is numeric.
					CoreDatatype.XSD datatype = nextLiteral.getCoreDatatype().asXSDDatatypeOrNull();

					if (datatype != null && datatype.isNumericDatatype()) {
						avg.sum = MathUtil.compute(avg.sum, nextLiteral, MathOp.PLUS);
					} else {
						avg.setTypeError(new ValueExprEvaluationException("not a number: " + v));
					}
					avg.count++;
				} else if (v != null) {
					// we do not actually throw the exception yet, but record it and
					// stop further processing. The exception will be thrown when
					// getValue() is invoked.
					avg.setTypeError(new ValueExprEvaluationException("not a number: " + v));
				}
			}
		}
	}

	private static class SampleCollector implements AggregateCollector {
		private Value sample;

		@Override
		public Value getFinalValue() throws ValueExprEvaluationException {
			if (sample == null) {
				throw new ValueExprEvaluationException("SAMPLE undefined");
			}
			return sample;
		}
	}

	private static class SampleAggregate extends AggregateFunction<SampleCollector, Value> {

		private final Random random;

		public SampleAggregate(Function<BindingSet, Value> f) {
			super(f);
			random = new Random(System.currentTimeMillis());
		}

		@Override
		public void processAggregate(BindingSet s, Predicate<Value> distinct, SampleCollector sample)
				throws QueryEvaluationException {
			// we flip a coin to determine if we keep the current value or set a
			// new value to report.
			if (sample.sample == null || random.nextFloat() < 0.5f) {
				final Value newValue = evaluate(s);
				if (newValue != null) {
					sample.sample = newValue;
				}
			}
		}
	}

	private static class StringBuilderCollector implements AggregateCollector {
		private StringBuilder concatenated;
		private final ValueFactory vf;

		public StringBuilderCollector(ValueFactory vf) {
			super();
			this.vf = vf;
		}

		@Override
		public Value getFinalValue() throws ValueExprEvaluationException {
			if (concatenated == null || concatenated.length() == 0) {
				return vf.createLiteral("");
			}
			return vf.createLiteral(concatenated.toString());
		}
	}

	private static class ConcatAggregate extends AggregateFunction<StringBuilderCollector, Value> {

		private static final String DEFAULT_SEPERATOR = " ";
		private final String separator;

		public ConcatAggregate(Function<BindingSet, Value> f, String seperator) throws QueryEvaluationException {
			super(f);
			this.separator = seperator;
		}

		public ConcatAggregate(Function<BindingSet, Value> f) throws QueryEvaluationException {
			super(f);
			this.separator = DEFAULT_SEPERATOR;
		}

		@Override
		public void processAggregate(BindingSet s, Predicate<Value> distinctValue, StringBuilderCollector collector)
				throws QueryEvaluationException {
			Value v = evaluate(s);
			if (v != null && distinctValue.test(v)) {
				if (collector.concatenated == null) {
					collector.concatenated = new StringBuilder();
				} else {
					collector.concatenated.append(separator);
				}
				collector.concatenated.append(v.stringValue());
			}
		}
	}

	private static class QueryStepEvaluator implements Function<BindingSet, Value> {
		private final QueryValueEvaluationStep evaluationStep;

		public QueryStepEvaluator(QueryValueEvaluationStep evaluationStep) {
			this.evaluationStep = evaluationStep;
		}

		@Override
		public Value apply(BindingSet bindings) {
			try {
				return evaluationStep.evaluate(bindings);
			} catch (ValueExprEvaluationException e) {
				return null; // treat missing or invalid expressions as null
			}
		}
	}
}
