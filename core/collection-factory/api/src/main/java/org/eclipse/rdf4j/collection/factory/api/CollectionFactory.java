/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
public interface CollectionFactory extends AutoCloseable {

	@Override
	void close() throws RDF4JException;

	/**
	 * @param <T> of the list
	 * @return a list that may be optimised and/or disk based
	 */
	public <T> List<T> createList();

	/**
	 * @return a list that may be optimised and/or disk based for Values only
	 */
	public List<Value> createValueList();

	/**
	 * @param <T> of the set
	 * @return a set that may be optimised and/or disk based
	 */
	public <T> Set<T> createSet();

	/**
	 * @return a set that may be optimised and/or disk based
	 */
	public Set<BindingSet> createSetOfBindingSets();

	/**
	 * @return a set that may be optimised and/or disk based for Values
	 */
	public Set<Value> createValueSet();

	/**
	 * @param <K> key type
	 * @param <V> value type
	 * @return a map
	 */
	public <K, V> Map<K, V> createMap();

	/**
	 * @param <V> value type
	 * @return a map
	 */
	public <V> Map<Value, V> createValueKeyedMap();

	/**
	 * @param <T> of the contents of the queue
	 * @return a new queue
	 */
	public <T> Queue<T> createQueue();

	/**
	 * @return a new queue
	 */
	public Queue<Value> createValueQueue();

	@InternalUseOnly
	public <E> Map<BindingSetKey, E> createGroupByMap();

	@InternalUseOnly
	public BindingSetKey createBindingSetKey(BindingSet bindingSet, List<Function<BindingSet, Value>> getValues);

	@InternalUseOnly
	@Experimental
	private byte[] valueIntoByteArray(Value value) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(value);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return baos.toByteArray();
	}

	@InternalUseOnly
	@Experimental
	private void valueIntoObjectOutputStream(Value value, ObjectOutputStream oos) throws IOException {
		oos.writeObject(value);
	}

	@InternalUseOnly
	@Experimental
	private Value valueFromObjectInputStream(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		return (Value) ois.readObject();
	}
}
