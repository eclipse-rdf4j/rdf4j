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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
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
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
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
			return db.getHashSet(setName);
		} else {
			return new HashSet<>();
		}
	}

	private Iterator<BindingSet> createIterator() throws QueryEvaluationException {
		Collection<Entry> entries = buildEntries();
		Set<BindingSet> bindingSets = createSet("bindingsets");

		for (Entry entry : entries) {
			QueryBindingSet sol = new QueryBindingSet(parentBindings);

			for (String name : group.getGroupBindingNames()) {
				BindingSet prototype = entry.getPrototype();
				if (prototype != null) {
					Value value = prototype.getValue(name);
					if (value != null) {
						// Potentially overwrites bindings from super
						sol.setBinding(name, value);
					}
				}
			}

			entry.bindSolution(sol);

			bindingSets.add(sol);
		}

		return bindingSets.iterator();
	}

	private Collection<Entry> buildEntries() throws QueryEvaluationException {
		CloseableIteration<BindingSet, QueryEvaluationException> iter;
		iter = strategy.precompile(group.getArg(), context).evaluate(parentBindings);

		try {
			Map<Key, Entry> entries = new LinkedHashMap<>();

			if (!iter.hasNext()) {
				// no solutions, but if we are not explicitly grouping and aggregates are present,
				// we still need to process them to produce a zero-result.
				if (group.getGroupBindingNames().isEmpty()) {
					final Entry entry = new Entry(null);
					if (!entry.getAggregates().isEmpty()) {
						entry.addSolution(EmptyBindingSet.getInstance());
						entries.put(new Key(EmptyBindingSet.getInstance()), entry);
					}
				}
			}

			while (iter.hasNext()) {
				BindingSet sol;
				try {
					sol = iter.next();
				} catch (NoSuchElementException e) {
					break; // closed
				}
				Key key = new Key(sol);
				Entry entry = entries.get(key);

				if (entry == null) {
					entry = new Entry(sol);
					entries.put(key, entry);
				}

				entry.addSolution(sol);
			}

			return entries.values();
		} finally {
			iter.close();
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

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof Key && other.hashCode() == hash) {
				BindingSet otherSolution = ((Key) other).bindingSet;

				for (String name : group.getGroupBindingNames()) {
					Value v1 = bindingSet.getValue(name);
					Value v2 = otherSolution.getValue(name);

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

		private final Map<String, Aggregate> aggregates;
		private final Consumer<BindingSet> bs;

		public Entry(BindingSet prototype) throws QueryEvaluationException {
			this.prototype = prototype;
			this.aggregates = new LinkedHashMap<>();
			Consumer<BindingSet> bsPrototype = null;
			for (GroupElem ge : group.getGroupElements()) {
				Aggregate create = create(ge.getOperator());
				if (create != null) {
					aggregates.put(ge.getName(), create);
					Consumer<BindingSet> bsAgg = create::processAggregate;
					if (bsPrototype == null) {
						bsPrototype = bsAgg;
					} else {
						bsPrototype = bsPrototype.andThen(bsAgg);
					}
				}

			}
			if (bsPrototype == null) {
				bs = (bs) -> {
				};
			} else {
				bs = bsPrototype;
			}
		}

		public Map<String, Aggregate> getAggregates() {
			return aggregates;
		}

		public BindingSet getPrototype() {
			return prototype;
		}

		public void addSolution(BindingSet bindingSet) throws QueryEvaluationException {
			bs.accept(bindingSet);
		}

		public void bindSolution(QueryBindingSet sol) throws QueryEvaluationException {
			for (String name : aggregates.keySet()) {
				try {
					Value value = aggregates.get(name).getValue();
					if (value != null) {
						// Potentially overwrites bindings from super
						sol.setBinding(name, value);
					}
				} catch (ValueExprEvaluationException ex) {
					// There was a type error when calculating the value of the aggregate. We silently ignore the error,
					// resulting in no result value being bound.
				}
			}
		}

		private Aggregate create(AggregateOperator operator)
				throws QueryEvaluationException {
			if (operator instanceof Count) {
				if (((Count) operator).getArg() == null) {
					return new WildCardCountAggregate((Count) operator);
				} else {
					return new CountAggregate((Count) operator);
				}
			} else if (operator instanceof Min) {
				return new MinAggregate((Min) operator);
			} else if (operator instanceof Max) {
				return new MaxAggregate((Max) operator);
			} else if (operator instanceof Sum) {
				return new SumAggregate((Sum) operator);
			} else if (operator instanceof Avg) {
				return new AvgAggregate((Avg) operator);
			} else if (operator instanceof Sample) {
				return new SampleAggregate((Sample) operator);
			} else if (operator instanceof GroupConcat) {
				return new ConcatAggregate((GroupConcat) operator);
			}
			return null;
		}
	}

	private abstract class Aggregate {

		private final Set<Value> distinctValues;

		private final QueryValueEvaluationStep qes;

		public Aggregate(AbstractAggregateOperator operator) {
			this(operator, strategy.precompile(operator.getArg(), context));
		}

		public Aggregate(AbstractAggregateOperator operator, QueryValueEvaluationStep ves) {
			if (operator.isDistinct()) {
				distinctValues = createSet("distinct-values-" + this.hashCode());
			} else {
				distinctValues = null;
			}
			qes = ves;
		}

		public abstract Value getValue() throws ValueExprEvaluationException;

		public abstract void processAggregate(BindingSet bindingSet) throws QueryEvaluationException;

		protected boolean distinctValue(Value value) {
			if (distinctValues == null) {
				return true;
			}

			final boolean result = distinctValues.add(value);
			if (db != null && distinctValues.size() % iterationCacheSyncThreshold == 0) {
				// write to disk every $iterationCacheSyncThreshold items
				db.commit();
			}

			return result;
		}

		protected Value evaluate(BindingSet s) throws QueryEvaluationException {
			try {
				return qes.evaluate(s);
			} catch (ValueExprEvaluationException e) {
				return null; // treat missing or invalid expressions as null
			}
		}
	}

	private class CountAggregate extends Aggregate {

		private long count = 0;

		public CountAggregate(Count operator) {
			super(operator);
		}

		@Override
		public void processAggregate(BindingSet s) throws QueryEvaluationException {
			Value value = evaluate(s);
			if (value != null && distinctValue(value)) {
				count++;
			}
		}

		@Override
		public Value getValue() {
			return vf.createLiteral(Long.toString(count), XSD.INTEGER);
		}
	}

	private class WildCardCountAggregate extends Aggregate {

		private long count = 0;

		private final Set<BindingSet> distinctBindingSets;

		public WildCardCountAggregate(Count operator) {
			super(operator, null);

			// for a wildcarded count with a DISTINCT clause we need to filter on
			// distinct bindingsets rather than individual values.
			if (operator.isDistinct()) {
				distinctBindingSets = createSet("distinct-bs-" + this.hashCode());
			} else {
				distinctBindingSets = null;
			}
		}

		@Override
		public void processAggregate(BindingSet s) throws QueryEvaluationException {
			// wildcard count
			if (s.size() > 0 && distinctBindingSet(s)) {
				count++;
			}
		}

		protected boolean distinctBindingSet(BindingSet s) {
			if (distinctBindingSets == null) {
				return true;
			}

			final boolean result = distinctBindingSets.add(s);
			if (db != null && distinctBindingSets.size() % iterationCacheSyncThreshold == 0) {
				// write to disk every
				db.commit();
			}

			return result;
		}

		@Override
		public Value getValue() {
			return vf.createLiteral(Long.toString(count), XSD.INTEGER);
		}
	}

	private class MinAggregate extends Aggregate {

		private final ValueComparator comparator = new ValueComparator();

		private Value min = null;

		public MinAggregate(Min operator) {
			super(operator);
		}

		@Override
		public void processAggregate(BindingSet s) throws QueryEvaluationException {
			Value v = evaluate(s);
			if (strategy instanceof ExtendedEvaluationStrategy) {
				comparator.setStrict(false);
			}
			if (v != null && distinctValue(v)) {
				if (min == null) {
					min = v;
				} else if (comparator.compare(v, min) < 0) {
					min = v;
				}
			}
		}

		@Override
		public Value getValue() {
			if (min == null) {
				throw new ValueExprEvaluationException("MIN undefined");
			}
			return min;
		}
	}

	private class MaxAggregate extends Aggregate {

		private final ValueComparator comparator = new ValueComparator();

		private Value max = null;

		public MaxAggregate(Max operator) {
			super(operator);
		}

		@Override
		public void processAggregate(BindingSet s) throws QueryEvaluationException {
			if (strategy instanceof ExtendedEvaluationStrategy) {
				comparator.setStrict(false);
			}
			Value v = evaluate(s);
			if (v != null && distinctValue(v)) {
				if (max == null) {
					max = v;
				} else if (comparator.compare(v, max) > 0) {
					max = v;
				}
			}
		}

		@Override
		public Value getValue() throws ValueExprEvaluationException {
			if (max == null) {
				throw new ValueExprEvaluationException("max undefined");
			}
			return max;
		}

	}

	private class SumAggregate extends Aggregate {

		private Literal sum = vf.createLiteral("0", XSD.INTEGER);

		private ValueExprEvaluationException typeError = null;

		public SumAggregate(Sum operator) {
			super(operator);
		}

		@Override
		public void processAggregate(BindingSet s) throws QueryEvaluationException {
			if (typeError != null) {
				// halt further processing if a type error has been raised
				return;
			}

			Value v = evaluate(s);
			if (distinctValue(v)) {
				if (v instanceof Literal) {
					Literal nextLiteral = (Literal) v;
					if (nextLiteral.getDatatype() != null
							&& XMLDatatypeUtil.isNumericDatatype(nextLiteral.getDatatype())) {
						sum = MathUtil.compute(sum, nextLiteral, MathOp.PLUS);
					} else {
						typeError = new ValueExprEvaluationException("not a number: " + v);
					}
				} else if (v != null) {
					typeError = new ValueExprEvaluationException("not a number: " + v);
				}
			}
		}

		@Override
		public Value getValue() throws ValueExprEvaluationException {
			if (typeError != null) {
				throw typeError;
			}

			return sum;
		}
	}

	private class AvgAggregate extends Aggregate {

		private long count = 0;

		private Literal sum = vf.createLiteral("0", XSD.INTEGER);

		private ValueExprEvaluationException typeError = null;

		public AvgAggregate(Avg operator) {
			super(operator);
		}

		@Override
		public void processAggregate(BindingSet s) throws QueryEvaluationException {
			if (typeError != null) {
				// Prevent calculating the aggregate further if a type error has
				// occured.
				return;
			}

			Value v = evaluate(s);
			if (distinctValue(v)) {
				if (v instanceof Literal) {
					Literal nextLiteral = (Literal) v;
					// check if the literal is numeric.
					if (nextLiteral.getDatatype() != null
							&& XMLDatatypeUtil.isNumericDatatype(nextLiteral.getDatatype())) {
						sum = MathUtil.compute(sum, nextLiteral, MathOp.PLUS);
					} else {
						typeError = new ValueExprEvaluationException("not a number: " + v);
					}
					count++;
				} else if (v != null) {
					// we do not actually throw the exception yet, but record it and
					// stop further processing. The exception will be thrown when
					// getValue() is invoked.
					typeError = new ValueExprEvaluationException("not a number: " + v);
				}
			}
		}

		@Override
		public Value getValue() throws ValueExprEvaluationException {
			if (typeError != null) {
				// a type error occurred while processing the aggregate, throw it
				// now.
				throw typeError;
			}

			if (count == 0) {
				return vf.createLiteral("0", XSD.INTEGER);
			}

			Literal sizeLit = vf.createLiteral(count);
			return MathUtil.compute(sum, sizeLit, MathOp.DIVIDE);
		}
	}

	private class SampleAggregate extends Aggregate {

		private Value sample = null;

		private final Random random;

		public SampleAggregate(Sample operator) {
			super(operator);
			random = new Random(System.currentTimeMillis());
		}

		@Override
		public void processAggregate(BindingSet s) throws QueryEvaluationException {
			// we flip a coin to determine if we keep the current value or set a
			// new value to report.
			if (sample == null || random.nextFloat() < 0.5f) {
				final Value newValue = evaluate(s);
				if (newValue != null) {
					sample = newValue;
				}
			}
		}

		@Override
		public Value getValue() {
			if (sample == null) {
				throw new ValueExprEvaluationException("SAMPLE undefined");
			}
			return sample;
		}
	}

	private class ConcatAggregate extends Aggregate {

		private final StringBuilder concatenated = new StringBuilder();

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
		public void processAggregate(BindingSet s) throws QueryEvaluationException {
			Value v = evaluate(s);
			if (v != null && distinctValue(v)) {
				concatenated.append(v.stringValue());
				concatenated.append(separator);
			}
		}

		@Override
		public Value getValue() {
			if (concatenated.length() == 0) {
				return vf.createLiteral("");
			}

			// remove separator at the end.
			int len = concatenated.length() - separator.length();
			return vf.createLiteral(concatenated.substring(0, len));
		}
	}
}
