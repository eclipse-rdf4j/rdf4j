/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
import org.eclipse.rdf4j.query.algebra.AbstractAggregateOperator;
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
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ExtendedEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.util.MathUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.mapdb.DB;
import org.mapdb.DBMaker;

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

	private final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private final EvaluationStrategy strategy;

	private final BindingSet parentBindings;

	private final Group group;

	private final DB db;
	/**
	 * Number of items cached before internal collections are synced to disk. If set to 0, no disk-syncing is done and
	 * all internal caching is kept in memory.
	 */
	private final long iterationCacheSyncThreshold;

	private final QueryEvaluationContext context;

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
		this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
		this.context = context;

		if (this.iterationCacheSyncThreshold > 0) {
			try {
				this.db = DBMaker
						.newFileDB(File.createTempFile("group-eval", null))
						.deleteFilesAfterClose()
						.closeOnJvmShutdown()
						.make();
			} catch (IOException e) {
				throw new QueryEvaluationException("could not initialize temp db", e);
			}
		} else {
			this.db = null;
		}
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
			super.handleClose();
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}

	private <T> Set<T> createSet(String setName) {
		if (db != null) {
			return new MemoryTillSizeXSet<>(setName);
		} else {
			return new HashSet<>();
		}
	}

	// The size 16 seems like a nice starting value but others could well
	// be better.
	private static final int SWITCH_TO_DISK_BASED_SET_AT_SIZE = 16;

	/**
	 * Only create a disk based set once the contents are large enough that it starts to pay off.
	 *
	 * @param <T> of the contents of the set.
	 */
	private class MemoryTillSizeXSet<T> extends AbstractSet<T> {
		private Set<T> wrapped = new HashSet<>();
		private final String setName;

		public MemoryTillSizeXSet(String setName) {
			super();
			this.setName = setName;
		}

		@Override
		public boolean add(T e) {
			if (wrapped instanceof HashSet && wrapped.size() > SWITCH_TO_DISK_BASED_SET_AT_SIZE) {
				Set<T> disk = db.getHashSet(setName);
				disk.addAll(wrapped);
				wrapped = disk;
			}
			return wrapped.add(e);
		}

		@Override
		public boolean addAll(Collection<? extends T> arg0) {
			if (wrapped instanceof HashSet && arg0.size() > SWITCH_TO_DISK_BASED_SET_AT_SIZE) {
				Set<T> disk = db.getHashSet(setName);
				disk.addAll(wrapped);
				wrapped = disk;
			}
			return wrapped.addAll(arg0);
		}

		@Override
		public void clear() {
			wrapped.clear();
		}

		@Override
		public boolean contains(Object o) {
			return wrapped.contains(o);
		}

		@Override
		public boolean containsAll(Collection<?> arg0) {
			return wrapped.containsAll(arg0);
		}

		@Override
		public boolean isEmpty() {
			return wrapped.isEmpty();
		}

		@Override
		public boolean remove(Object o) {
			return wrapped.remove(o);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return wrapped.retainAll(c);
		}

		@Override
		public Object[] toArray() {
			return wrapped.toArray();
		}

		@Override
		public <T> T[] toArray(T[] arg0) {
			return wrapped.toArray(arg0);
		}

		@Override
		public Iterator<T> iterator() {
			return wrapped.iterator();
		}

		@Override
		public int size() {
			return wrapped.size();
		}

	}

	private Iterator<BindingSet> createIterator() throws QueryEvaluationException {
		List<AggregatePredicateCollectorSupplier<?, ?>> aggregates = makeAggregates();
		Collection<Entry> entries = buildEntries(aggregates);
		Set<BindingSet> bindingSets = createSet("bindingsets");

		Supplier<MutableBindingSet> makeNewBindingSet;
		if (parentBindings.isEmpty()) {
			makeNewBindingSet = context::createBindingSet;
		} else {
			makeNewBindingSet = () -> context.createBindingSet(parentBindings);
		}

		List<Function<BindingSet, Value>> getValues = new ArrayList<>();
		List<BiConsumer<Value, MutableBindingSet>> setBindings = new ArrayList<>();
		BiConsumer<Entry, MutableBindingSet> bindSolution = makeBindSolution(aggregates);
		for (String name : group.getGroupBindingNames()) {
			Function<BindingSet, Value> getValue = context.getValue(name);
			BiConsumer<Value, MutableBindingSet> setBinding = context.setBinding(name);
			if (getValue != null) {
				getValues.add(getValue);
				setBindings.add(setBinding);
			}
		}

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

		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = strategy
				.precompile(group.getArg(), context)
				.evaluate(parentBindings)) {
			long setId = 0;
			Function<BindingSet, Integer> hashMaker = hashMaker(context, group);
			Map<Key, Entry> entries = new LinkedHashMap<>();

			if (!iter.hasNext()) {
				emptySolutionSpecialCase(aggregates, entries);
			} else {
				while (iter.hasNext()) {
					BindingSet sol = iter.next();
					Key key = new Key(sol, hashMaker.apply(sol));
					Entry entry = entries.get(key);
					if (entry == null) {
						List<AggregateCollector> collectors = makeCollectors(aggregates);
						List<Predicate<?>> predicates = new ArrayList<>(aggregates.size());
						for (AggregatePredicateCollectorSupplier<?, ?> a : aggregates) {
							predicates.add(a.predicate.apply(setId++));
						}

						entry = new Entry(sol, collectors, predicates);
						entries.put(key, entry);
					}

					entry.addSolution(sol, aggregates);
				}
			}
			return entries.values();
		}

	}

	private void emptySolutionSpecialCase(List<AggregatePredicateCollectorSupplier<?, ?>> aggregates,
			Map<Key, Entry> entries) {
		// no solutions, but if we are not explicitly grouping and aggregates are present,
		// we still need to process them to produce a zero-result.
		if (group.getGroupBindingNames().isEmpty()) {
			if (group.getGroupElements().isEmpty()) {
				final Entry entry = new Entry(null, null, null);
				entries.put(new Key(EmptyBindingSet.getInstance()), entry);
			} else {
				List<AggregateCollector> collectors = makeCollectors(aggregates);
				List<Predicate<?>> predicates = new ArrayList<>(aggregates.size());
				for (int i = 0; i < aggregates.size(); i++) {
					predicates.add(ALWAYS_TRUE_BINDING_SET);
				}
				final Entry entry = new Entry(null, collectors, predicates);
				entry.addSolution(EmptyBindingSet.getInstance(), aggregates);
				entries.put(new Key(EmptyBindingSet.getInstance()), entry);
			}
		}
	}

	private List<AggregateCollector> makeCollectors(List<AggregatePredicateCollectorSupplier<?, ?>> aggregates) {
		List<AggregateCollector> collectors = new ArrayList<>(aggregates.size());
		for (AggregatePredicateCollectorSupplier<?, ?> a : aggregates) {
			collectors.add(a.supplier.get());
		}

		return collectors;
	}

	private static Function<BindingSet, Integer> hashMaker(QueryEvaluationContext context, Group group) {
		Set<String> groupBindingNames = group.getGroupBindingNames();
		int size = groupBindingNames.size();

		if (size == 1) {
			Function<BindingSet, Value> getValue = context.getValue(groupBindingNames.iterator().next());
			return (bs) -> {
				Value value = getValue.apply(bs);
				return value != null ? value.hashCode() : 0;
			};
		} else {

			List<Function<BindingSet, Value>> getValues = new ArrayList<>(size);
			for (String name : groupBindingNames) {
				Function<BindingSet, Value> getValue = context.getValue(name);
				getValues.add(getValue);
			}

			return (bs) -> {
				int nextHash = 0;
				for (Function<BindingSet, Value> getValue : getValues) {
					Value value = getValue.apply(bs);
					if (value != null) {
						nextHash ^= value.hashCode();
					}
				}
				return nextHash;
			};
		}
	}

	/**
	 * A unique key for a set of existing bindings.
	 *
	 * @author David Huynh
	 */
	protected class Key implements Serializable {

		private static final long serialVersionUID = 4461951265373324084L;

		private final BindingSet bindingSet;

		private final int hash;

		public Key(BindingSet bindingSet) {
			this.bindingSet = bindingSet;
			int nextHash = 0;
			for (String name : group.getGroupBindingNames()) {
				Value value = bindingSet.getValue(name);
				if (value != null) {
					nextHash ^= value.hashCode();
				}
			}
			this.hash = nextHash;
		}

		public Key(BindingSet bindingSet, int hash) {
			this.bindingSet = bindingSet;
			this.hash = hash;
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}

			if (other instanceof Key && other.hashCode() == hash) {
				BindingSet otherBindingSet = ((Key) other).bindingSet;

				for (String name : group.getGroupBindingNames()) {
					Value v1 = bindingSet.getValue(name);
					Value v2 = otherBindingSet.getValue(name);

					if (!Objects.equals(v1, v2)) {
						return false;
					}
				}

				return true;
			}

			return false;
		}
	}

	private class Entry {

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
	 * <p>
	 * Making an aggregate function is quite a lot of work and we do not want to repeat that for each final binding.
	 */
	private class AggregatePredicateCollectorSupplier<T extends AggregateCollector, D> {
		public final String name;
		private final Aggregate<T, D> agg;
		private final LongFunction<Predicate<D>> predicate;
		private final Supplier<T> supplier;

		public AggregatePredicateCollectorSupplier(Aggregate<T, D> agg, LongFunction<Predicate<D>> predicate,
				Supplier<T> supplier, String name) {
			super();
			this.agg = agg;
			this.predicate = predicate;
			this.supplier = supplier;
			this.name = name;
		}

		private void operate(BindingSet bs, Predicate<?> predicate, Object t) {
			agg.processAggregate(bs, (Predicate<D>) predicate, (T) t);
		}
	}

	private static final Predicate<BindingSet> ALWAYS_TRUE_BINDING_SET = (t) -> true;
	private static final Predicate<Value> ALWAYS_TRUE_VALUE = (t) -> true;
	private static final LongFunction<Predicate<Value>> ALWAYS_TRUE_VALUE_SUPPLIER = (l) -> ALWAYS_TRUE_VALUE;

	private AggregatePredicateCollectorSupplier<?, ?> create(GroupElem ge)
			throws QueryEvaluationException {
		AggregateOperator operator = ge.getOperator();

		if (operator instanceof Count) {
			if (((Count) operator).getArg() == null) {
				WildCardCountAggregate wildCardCountAggregate = new WildCardCountAggregate((Count) operator);
				LongFunction<Predicate<BindingSet>> predicate = operator.isDistinct() ? DistinctBindingSets::new
						: (l) -> ALWAYS_TRUE_BINDING_SET;
				return new AggregatePredicateCollectorSupplier<>(
						wildCardCountAggregate,
						predicate,
						CountCollector::new,
						ge.getName()
				);
			} else {
				CountAggregate agg = new CountAggregate((Count) operator);
				LongFunction<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
						: ALWAYS_TRUE_VALUE_SUPPLIER;
				return new AggregatePredicateCollectorSupplier<>(
						agg,
						predicate,
						CountCollector::new,
						ge.getName()
				);
			}
		} else if (operator instanceof Min) {
			MinAggregate agg = new MinAggregate((Min) operator);
			LongFunction<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					ValueCollector::new,
					ge.getName()
			);
		} else if (operator instanceof Max) {
			MaxAggregate agg = new MaxAggregate((Max) operator);
			LongFunction<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					ValueCollector::new,
					ge.getName()
			);
		} else if (operator instanceof Sum) {

			SumAggregate agg = new SumAggregate((Sum) operator);
			LongFunction<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					IntegerCollector::new,
					ge.getName()
			);
		} else if (operator instanceof Avg) {
			AvgAggregate agg = new AvgAggregate((Avg) operator);
			LongFunction<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					AvgCollector::new,
					ge.getName()
			);
		} else if (operator instanceof Sample) {
			SampleAggregate agg = new SampleAggregate((Sample) operator);
			LongFunction<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
					: ALWAYS_TRUE_VALUE_SUPPLIER;
			return new AggregatePredicateCollectorSupplier<>(
					agg,
					predicate,
					SampleCollector::new,
					ge.getName()
			);
		} else if (operator instanceof GroupConcat) {
			ConcatAggregate agg = new ConcatAggregate((GroupConcat) operator);
			LongFunction<Predicate<Value>> predicate = operator.isDistinct() ? DistinctValues::new
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

	private <T> Predicate<T> createPredicate(AggregateOperator operator, long setId)
			throws QueryEvaluationException {
		if (operator.isDistinct()) {
			if (operator instanceof Count && ((Count) operator).getArg() == null) {
				return (Predicate<T>) new DistinctBindingSets(setId);
			} else {
				return (Predicate<T>) new DistinctValues(setId);
			}
		}
		return (v) -> true;
	}

	private interface AggregateCollector {
		Value getFinalValue();
	}

	private class CountCollector implements AggregateCollector {
		private long value;

		@Override
		public Value getFinalValue() {
			return vf.createLiteral(Long.toString(value), CoreDatatype.XSD.INTEGER);
		}
	}

	private class ValueCollector implements AggregateCollector {
		private Value value;

		@Override
		public Value getFinalValue() {
			return value;
		}
	}

	private class IntegerCollector implements AggregateCollector {
		private Literal value = vf.createLiteral("0", CoreDatatype.XSD.INTEGER);

		@Override
		public Value getFinalValue() {
			return value;
		}
	}

	private class AvgCollector implements AggregateCollector {
		private Literal sum = vf.createLiteral("0", CoreDatatype.XSD.INTEGER);
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
				return vf.createLiteral("0", CoreDatatype.XSD.INTEGER);
			}

			Literal sizeLit = vf.createLiteral(count);
			return MathUtil.compute(sum, sizeLit, MathOp.DIVIDE);
		}
	}

	private class DistinctValues implements Predicate<Value> {
		private final Set<Value> distinctValues;

		public DistinctValues(long setId) {
			distinctValues = createSet("distinct-values-" + setId);
		}

		@Override
		public boolean test(Value value) {
			final boolean result = distinctValues.add(value);
			if (db != null && distinctValues.size() % iterationCacheSyncThreshold == 0) {
				// write to disk every $iterationCacheSyncThreshold items
				db.commit();
			}
			return result;
		}
	}

	private class DistinctBindingSets implements Predicate<BindingSet> {
		private final Set<BindingSet> distinctValues;

		public DistinctBindingSets(long setId) {
			distinctValues = createSet("distinct-values-" + setId);
		}

		@Override
		public boolean test(BindingSet value) {
			final boolean result = distinctValues.add(value);
			if (db != null && distinctValues.size() % iterationCacheSyncThreshold == 0) {
				// write to disk every $iterationCacheSyncThreshold items
				db.commit();
			}
			return result;
		}
	}

	private abstract class Aggregate<T extends AggregateCollector, D> {

		private final QueryValueEvaluationStep qes;

		public Aggregate(AbstractAggregateOperator operator) {
			this(operator, strategy.precompile(operator.getArg(), context));
		}

		public Aggregate(AbstractAggregateOperator operator, QueryValueEvaluationStep ves) {
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

	private class CountAggregate extends Aggregate<CountCollector, Value> {

		public CountAggregate(Count operator) {
			super(operator);
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

	private class WildCardCountAggregate extends Aggregate<CountCollector, BindingSet> {

		public WildCardCountAggregate(Count operator) {
			super(operator, null);
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

	private class MinAggregate extends Aggregate<ValueCollector, Value> {

		private final ValueComparator comparator = new ValueComparator();

		public MinAggregate(Min operator) {
			super(operator);
			if (strategy instanceof ExtendedEvaluationStrategy) {
				comparator.setStrict(false);
			}
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

	private class MaxAggregate extends Aggregate<ValueCollector, Value> {

		private final ValueComparator comparator = new ValueComparator();

		public MaxAggregate(Max operator) {
			super(operator);
			if (strategy instanceof ExtendedEvaluationStrategy) {
				comparator.setStrict(false);
			}
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

	private class SumAggregate extends Aggregate<IntegerCollector, Value> {

		private ValueExprEvaluationException typeError = null;

		public SumAggregate(Sum operator) {
			super(operator);
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

	private class AvgAggregate extends Aggregate<AvgCollector, Value> {

		public AvgAggregate(Avg operator) {
			super(operator);
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

	private class SampleCollector implements AggregateCollector {
		private Value sample;

		@Override
		public Value getFinalValue() throws ValueExprEvaluationException {
			if (sample == null) {
				throw new ValueExprEvaluationException("SAMPLE undefined");
			}
			return sample;
		}
	}

	private class SampleAggregate extends Aggregate<SampleCollector, Value> {

		private final Random random;

		public SampleAggregate(Sample operator) {
			super(operator);
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

	private class StringBuilderCollector implements AggregateCollector {
		private StringBuilder concatenated;

		@Override
		public Value getFinalValue() throws ValueExprEvaluationException {
			if (concatenated == null || concatenated.length() == 0) {
				return vf.createLiteral("");
			}
			return vf.createLiteral(concatenated.toString());
		}
	}

	private class ConcatAggregate extends Aggregate<StringBuilderCollector, Value> {

		private String separator = " ";

		public ConcatAggregate(GroupConcat groupConcatOp)
				throws QueryEvaluationException {
			super(groupConcatOp);
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
