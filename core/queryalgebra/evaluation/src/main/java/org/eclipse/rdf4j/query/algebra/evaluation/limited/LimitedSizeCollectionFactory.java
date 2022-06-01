package org.eclipse.rdf4j.query.algebra.evaluation.limited;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.api.ValuePair;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;

public class LimitedSizeCollectionFactory implements CollectionFactory {

	private final CollectionFactory delegate;
	private AtomicLong used;
	private long maxSize;

	public LimitedSizeCollectionFactory(CollectionFactory delegate, AtomicLong used, long maxSize) {
		this.delegate = delegate;
		this.used = used;
		this.maxSize = maxSize;

	}

	@Override
	public void close() throws RDF4JException {
		delegate.close();
	}

	@Override
	public Set<BindingSet> createSetOfBindingSets(Supplier<MutableBindingSet> supplier) {
		return new LimitedSizeSet<>(delegate.createSetOfBindingSets(supplier), used, maxSize);
	}

	@Override
	public Set<Value> createValueSet() {
		return new LimitedSizeSet<>(delegate.createValueSet(), used, maxSize);
	}

	@Override
	public <V> Map<Value, V> createValueKeyedMap() {
		return delegate.createValueKeyedMap();
	}

	@Override
	public <E> Map<BindingSetKey, E> createGroupByMap() {
		return delegate.createGroupByMap();
	}

	@Override
	public BindingSetKey createBindingSetKey(BindingSet bindingSet, List<Function<BindingSet, Value>> getValues) {
		return delegate.createBindingSetKey(bindingSet, getValues);
	}

	@Override
	public ValuePair createValuePair(Value start, Value end) {
		return delegate.createValuePair(start, end);
	}

	@Override
	public Set<ValuePair> createValuePairSet() {
		return delegate.createValuePairSet();
	}

	@Override
	public Queue<ValuePair> createValuePairQueue() {
		return delegate.createValuePairQueue();
	}

}
