/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.rdf4j.collection.factory.api.BindingSetEntry;
import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
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
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ExtendedEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.util.MathUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

/**
 * @author David Huynh
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 * @author James Leigh
 * @author Jerven Bolleman
 */
public class GroupIterator extends CloseableIteratorIteration<BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final EvaluationStrategy strategy;

	private final BindingSet parentBindings;

	private final Group group;

	private final QueryEvaluationContext context;

	private final CollectionFactory cf;

	private final QueryEvaluationStep arguments;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public GroupIterator(EvaluationStrategy strategy, Group group, BindingSet parentBindings,
			QueryEvaluationContext context)
			throws QueryEvaluationException {
		this(strategy, group, parentBindings, 0, context);
	}

	public GroupIterator(EvaluationStrategy strategy, Group group, BindingSet parentBindings,
			long iterationCacheSyncThreshold, QueryEvaluationContext context) throws QueryEvaluationException {
		this.strategy = strategy;
		this.group = group;
		this.parentBindings = parentBindings;
//		this is ignored as it is just a left over from earlier, this is now stored in the collection factory.
//		this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
		this.context = context;
		this.arguments = strategy.precompile(group.getArg(), context);
		this.cf = this.strategy.getCollectionFactory();
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		if (!super.hasIterator()) {
			super.setIterator(createIterator());
		}
		return super.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		if (!super.hasIterator()) {
			super.setIterator(createIterator());
		}
		return super.next();
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			cf.close();
		} finally {
			super.handleClose();
		}
	}

	private Iterator<BindingSet> createIterator() throws QueryEvaluationException {
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
		Set<BindingSet> bindingSets = cf.createSetOfBindingSets();
		for (Entry entry : entries) {
			MutableBindingSet sol = makeNewBindingSet.get();

			BindingSet prototype = entry.getPrototype();
			if (prototype != null) {
				for (int i = 0; i < getValues.size(); i++) {
					Function<BindingSet, Value> getBinding = getValues.get(i);
					Value value = getBinding.apply(prototype);
					if (value != null) {
						// Potentially overwrites bindings from super
						setBindings.get(i).accept(value, sol);
					}
				}
			}

			bindSolution.accept(entry, sol);
			bindingSets.add(sol);
		}

		return bindingSets.iterator();
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
					// There was a type error when calculating the value of the aggregate. We silently ignore the error,
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
			AggregatePredicateCollectorSupplier<?, ?> create = create(ge);
			if (create != null) {
				aggregates.add(create);
			}
		}
		return aggregates;
	}

	private Collection<Entry> buildEntries(List<AggregatePredicateCollectorSupplier<?, ?>> aggregates)
			throws QueryEvaluationException {
		CloseableIteration<BindingSet, QueryEvaluationException> iter = arguments.evaluate(parentBindings);

		try {
			if (!iter.hasNext()) {
				return emptySolutionSpecialCase(aggregates);
			} else {
				return constructEntries(aggregates, iter);
			}
		} finally {
			iter.close();
		}

	}

	private Collection<Entry> constructEntries(List<AggregatePredicateCollectorSupplier<?, ?>> aggregates,
			CloseableIteration<BindingSet, QueryEvaluationException> iter) {
		List<Function<BindingSet, Value>> getValues = getValueFunctions(context, group);
		Map<BindingSetKey, Entry> entries = new LinkedHashMap<>();
		while (iter.hasNext()) {
			BindingSet sol = iter.next();
			BindingSetKey key = cf.createBindingSetKey(sol, getValues);
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
		}
		return entries.values();
	}

	private List<Entry> emptySolutionSpecialCase(List<AggregatePredicateCollectorSupplier<?, ?>> aggregates) {
		// no solutions, but if we are not explicitly grouping and aggregates are present,
		// we still need to process them to produce a zero-result.
		if (group.getGroupBindingNames().isEmpty()) {
			if (group.getGroupElements().isEmpty()) {
				final Entry entry = new Entry(null, null, null);
				return List.of(entry);
			} else {
				List<AggregateCollector> collectors = makeCollectors(aggregates);
				List<Predicate<?>> predicates = new ArrayList<>(aggregates.size());
				for (int i = 0; i < aggregates.size(); i++) {
					predicates.add(ALWAYS_TRUE_BINDING_SET);
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

	private static List<Function<BindingSet, Value>> getValueFunctions(QueryEvaluationContext context, Group group) {
		List<Function<BindingSet, Value>> getValues = new ArrayList<>(group.getGroupBindingNames().size());
		for (String name : group.getGroupBindingNames()) {
			Function<BindingSet, Value> getValue = context.getValue(name);
			getValues.add(getValue);
		}
		return getValues;
	}

	private class Entry implements BindingSetEntry {

		private static final long serialVersionUID = 1L;
		private final BindingSet prototype;
		private final List<AggregateCollector> collectors;
		private final List<Predicate<?>> predicates;

		public Entry(BindingSet prototype, List<AggregateCollector> collectors,
				List<Predicate<?>> predicates)
				throws QueryEvaluationException {
			this.prototype = prototype;
			this.collectors = collectors;
			this.predicates = predicates;
		}

		public void addSolution(BindingSet bs, List<AggregatePredicateCollectorSupplier<?, ?>> operators) {
			for (int i = 0; i < operators.size(); i++) {
				AggregatePredicateCollectorSupplier<?, ?> aggregatePredicateCollectorSupplier = operators.get(i);
				aggregatePredicateCollectorSupplier.operate(bs, predicates.get(i), collectors.get(i));
			}
		}

		public BindingSet getPrototype() {
			return prototype;
		}
	}

	/**
	 * This is to collect together in operation an aggregate function the name of it. And the suppliers that will give
	 * the unique set and final value collectors per final binding set.
	 *
	 * Making an aggregate function is quite a lot of work and we do not want to repeat that for each final binding.
	 */
	private class AggregatePredicateCollectorSupplier<T extends AggregateCollector, D> {
		public final String name;
		private final Aggregate<T, D> agg;
		private final Supplier<Predicate<D>> makePotentialDistinctTest;
		private final Supplier<T> makeAggregateCollector;

		public AggregatePredicateCollectorSupplier(Aggregate<T, D> agg,
				Supplier<Predicate<D>> makePotentialDistinctTest,
				Supplier<T> makeAggregateCollector, String name) {
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

	private static final Predicate<BindingSet> ALWAYS_TRUE_BINDING_SET = (t) -> true;
	private static final Predicate<Value> ALWAYS_TRUE_VALUE = (t) -> true;
	private static final Supplier<Predicate<Value>> ALWAYS_TRUE_VALUE_SUPPLIER = () -> ALWAYS_TRUE_VALUE;

	private AggregatePredicateCollectorSupplier<?, ?> create(GroupElem ge)
			throws QueryEvaluationException {
		AggregateOperator operator = ge.getOperator();

		if (operator instanceof Count) {
			if (((Count) operator).getArg() == null) {
				WildCardCountAggregate wildCardCountAggregate = new WildCardCountAggregate();
				Supplier<Predicate<BindingSet>> potentialDistinctTest = operator.isDistinct() ? DistinctBindingSets::new
						: () -> ALWAYS_TRUE_BINDING_SET;
				return new AggregatePredicateCollectorSupplier<>(
						wildCardCountAggregate,
						potentialDistinctTest,
						CountCollector::new,
						ge.getName()
				);
			} else {
				CountAggregate agg = new CountAggregate(strategy.precompile(((Count) operator).getArg(), context));
				Supplier<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
						: ALWAYS_TRUE_VALUE_SUPPLIER;
				return new AggregatePredicateCollectorSupplier<>(
						agg,
						predicate,
						CountCollector::new,
						ge.getName()
				);
			}
		} else if (operator instanceof Min) {
			MinAggregate agg = new MinAggregate(strategy.precompile(((Min) operator).getArg(), context),
					strategy instanceof ExtendedEvaluationStrategy);
			Supplier<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					ValueCollector::new,
					ge.getName()
			);
		} else if (operator instanceof Max) {
			MaxAggregate agg = new MaxAggregate(strategy.precompile(((Max) operator).getArg(), context),
					strategy instanceof ExtendedEvaluationStrategy);
			Supplier<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					ValueCollector::new,
					ge.getName()
			);
		} else if (operator instanceof Sum) {

			SumAggregate agg = new SumAggregate(strategy.precompile(((Sum) operator).getArg(), context));
			Supplier<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					IntegerCollector::new,
					ge.getName()
			);
		} else if (operator instanceof Avg) {
			AvgAggregate agg = new AvgAggregate(strategy.precompile(((Avg) operator).getArg(), context));
			Supplier<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					AvgCollector::new,
					ge.getName()
			);
		} else if (operator instanceof Sample) {
			SampleAggregate agg = new SampleAggregate(strategy.precompile(((Sample) operator).getArg(), context));
			Supplier<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					SampleCollector::new,
					ge.getName()
			);
		} else if (operator instanceof GroupConcat) {
			ConcatAggregate agg = new ConcatAggregate((GroupConcat) operator, this.context, this.strategy,
					this.parentBindings);
			Supplier<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					StringBuilderCollector::new,
					ge.getName()
			);
		}
		return null;
	}

	private interface AggregateCollector {
		Value getFinalValue();
	}

	private static class CountCollector implements AggregateCollector {
		private long value;

		@Override
		public Value getFinalValue() {
			return SimpleValueFactory.getInstance().createLiteral(Long.toString(value), CoreDatatype.XSD.INTEGER);
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
		private Literal value = SimpleValueFactory.getInstance().createLiteral("0", CoreDatatype.XSD.INTEGER);

		@Override
		public Value getFinalValue() {
			return value;
		}
	}

	private static class AvgCollector implements AggregateCollector {
		private Literal sum = SimpleValueFactory.getInstance().createLiteral("0", CoreDatatype.XSD.INTEGER);
		private long count;
		private ValueExprEvaluationException typeError = null;

		@Override
		public Value getFinalValue() throws ValueExprEvaluationException {
			if (typeError != null) {
				// a type error occurred while processing the aggregate, throw it
				// now.
				throw typeError;
			}

			if (count == 0) {
				return SimpleValueFactory.getInstance().createLiteral("0", CoreDatatype.XSD.INTEGER);
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

	private static abstract class Aggregate<T extends AggregateCollector, D> {

		private final QueryValueEvaluationStep qes;

		public Aggregate() {
			qes = null;
		}

		public Aggregate(QueryValueEvaluationStep ves) {
			qes = ves;
		}

		public abstract void processAggregate(BindingSet bindingSet, Predicate<D> distinctValue, T agv)
				throws QueryEvaluationException;

		protected Value evaluate(BindingSet s) throws QueryEvaluationException {
			try {
				return qes.evaluate(s);
			} catch (ValueExprEvaluationException e) {
				return null; // treat missing or invalid expressions as null
			}
		}
	}

	private static class CountAggregate extends Aggregate<CountCollector, Value> {

		public CountAggregate(QueryValueEvaluationStep ves) {
			super(ves);
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

	private static class WildCardCountAggregate extends Aggregate<CountCollector, BindingSet> {

		public WildCardCountAggregate() {
			super();
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

	private static class MinAggregate extends Aggregate<ValueCollector, Value> {

		private final ValueComparator comparator = new ValueComparator();

		public MinAggregate(QueryValueEvaluationStep ves, boolean strict) {
			super(ves);
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

	private static class MaxAggregate extends Aggregate<ValueCollector, Value> {

		private final ValueComparator comparator = new ValueComparator();

		public MaxAggregate(QueryValueEvaluationStep ves, boolean strict) {
			super(ves);
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

	private static class SumAggregate extends Aggregate<IntegerCollector, Value> {

		private ValueExprEvaluationException typeError = null;

		public SumAggregate(QueryValueEvaluationStep ves) {
			super(ves);
		}

		@Override
		public void processAggregate(BindingSet s, Predicate<Value> distinctValue, IntegerCollector sum)
				throws QueryEvaluationException {
			if (typeError != null) {
				// halt further processing if a type error has been raised
				return;
			}

			Value v = evaluate(s);
			if (v instanceof Literal) {
				if (distinctValue.test(v)) {
					Literal nextLiteral = (Literal) v;
					if (nextLiteral.getDatatype() != null
							&& XMLDatatypeUtil.isNumericDatatype(nextLiteral.getDatatype())) {
						sum.value = MathUtil.compute(sum.value, nextLiteral, MathOp.PLUS);
					} else {
						typeError = new ValueExprEvaluationException("not a number: " + v);
					}
				} else if (v != null) {
					typeError = new ValueExprEvaluationException("not a number: " + v);
				}
			}
		}
	}

	private static class AvgAggregate extends Aggregate<AvgCollector, Value> {

		public AvgAggregate(QueryValueEvaluationStep ves) {
			super(ves);
		}

		@Override
		public void processAggregate(BindingSet s, Predicate<Value> distinctValue, AvgCollector avg)
				throws QueryEvaluationException {
			if (avg.typeError != null) {
				// Prevent calculating the aggregate further if a type error has
				// occured.
				return;
			}

			Value v = evaluate(s);
			if (distinctValue.test(v)) {
				if (v instanceof Literal) {
					Literal nextLiteral = (Literal) v;
					// check if the literal is numeric.
					if (nextLiteral.getDatatype() != null
							&& XMLDatatypeUtil.isNumericDatatype(nextLiteral.getDatatype())) {
						avg.sum = MathUtil.compute(avg.sum, nextLiteral, MathOp.PLUS);
					} else {
						avg.typeError = new ValueExprEvaluationException("not a number: " + v);
					}
					avg.count++;
				} else if (v != null) {
					// we do not actually throw the exception yet, but record it and
					// stop further processing. The exception will be thrown when
					// getValue() is invoked.
					avg.typeError = new ValueExprEvaluationException("not a number: " + v);
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

	private static class SampleAggregate extends Aggregate<SampleCollector, Value> {

		private final Random random;

		public SampleAggregate(QueryValueEvaluationStep ves) {
			super(ves);
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

		@Override
		public Value getFinalValue() throws ValueExprEvaluationException {
			if (concatenated == null || concatenated.length() == 0) {
				return SimpleValueFactory.getInstance().createLiteral("");
			}
			return SimpleValueFactory.getInstance().createLiteral(concatenated.toString());
		}
	}

	private static class ConcatAggregate extends Aggregate<StringBuilderCollector, Value> {

		private String separator = " ";

		public ConcatAggregate(GroupConcat groupConcatOp, QueryEvaluationContext context, EvaluationStrategy strategy,
				BindingSet parentBindings)
				throws QueryEvaluationException {
			super(strategy.precompile(groupConcatOp.getArg(), context));
			ValueExpr separatorExpr = groupConcatOp.getSeparator();
			if (separatorExpr != null) {
				Value separatorValue = strategy.evaluate(separatorExpr, parentBindings);
				separator = separatorValue.stringValue();
			}
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
}
