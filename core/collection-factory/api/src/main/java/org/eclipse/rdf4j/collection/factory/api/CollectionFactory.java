/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.api;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * A Factory that may generate optimised and/or disk based collections
 *
 * Factories like this should not be cached but created a new everytime as the closing is important if they are disk
 * based.
 */
@InternalUseOnly
@Experimental
public interface CollectionFactory extends AutoCloseable {

	@Override
	void close() throws RDF4JException;

	/**
	 * @return a set that may be optimised and/or disk based
	 */
	public Set<BindingSet> createSetOfBindingSets();

	/**
	 * @return a set that may be optimised and/or disk based for Values
	 */
	public Set<Value> createValueSet();

	/**
	 * @param <V> value type
	 * @return a map
	 */
	public <V> Map<Value, V> createValueKeyedMap();

	@InternalUseOnly
	public <E> Map<BindingSetKey, E> createGroupByMap();

	@InternalUseOnly
	public BindingSetKey createBindingSetKey(BindingSet bindingSet, List<Function<BindingSet, Value>> getValues);

	@InternalUseOnly
	@Experimental
	public ValuePair createValuePair(Value start, Value end);

	@InternalUseOnly
	@Experimental
	public Set<ValuePair> createValuePairSet();

	@InternalUseOnly
	@Experimental
	public Queue<ValuePair> createValuePairQueue();
}
