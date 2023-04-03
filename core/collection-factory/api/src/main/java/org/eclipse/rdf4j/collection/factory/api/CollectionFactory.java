/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

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
	public BindingSetKey createBindingSetKey(BindingSet bindingSet, List<Function<BindingSet, Value>> getValues,
			ToIntFunction<BindingSet> hashOfBindingSetCalculator);

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

	/**
	 * Hashes a value that complies with the hashCode/equals conception but only in context of this collection/factory
	 * storage layer. Potentially also only valid during a single transaction scope.
	 *
	 * @param getValue the function to extract the value to hash
	 * @param nextHash any previously calculated hash value for earlier values in the BindingSet
	 * @param bs       the bindingset to take the value from
	 * @return a hash function
	 */
	@InternalUseOnly
	default int hashAValue(Function<BindingSet, Value> getValue, int nextHash, BindingSet bs) {
		Value value = getValue.apply(bs);
		if (value != null) {
			return 31 * nextHash + value.hashCode();
		} else {
			return nextHash;
		}
	}

	/**
	 * Generate a method that calculates a hash code that is valid in context of a single store implementation and
	 * QueryExecutionContext.
	 *
	 * @param getValues that should be considered in the hash
	 * @return a hash function
	 *
	 * @implNote this method is unlikely to require overriding, check if overriding the hashAValue method is not
	 *           sufficient first
	 */
	@InternalUseOnly
	public default ToIntFunction<BindingSet> hashOfBindingSetFuntion(List<Function<BindingSet, Value>> getValues) {
		if (!getValues.isEmpty()) {
			// Special case the getValues to remove a loop if we know there is only one
			// value
			Iterator<Function<BindingSet, Value>> iterator = getValues.iterator();
			Function<BindingSet, Value> getFirstValue = iterator.next();
			ToIntFunction<BindingSet> hashFirstValue = (bs) -> {
				Value value = getFirstValue.apply(bs);
				if (value != null) {
					return 31 + value.hashCode();
				}
				return 1;
			};
			if (!iterator.hasNext()) {
				// There is only one value to hash so no loop no multiplication.
				return hashFirstValue;
			} else {
				// There are multiple values so we collect a set of functions
				// Note that we reuse the hashFirstValue function created before so the size of
				// the array is one smaller;
				@SuppressWarnings("unchecked")
				ToIntBiFunction<BindingSet, Integer>[] hashOtherValues = new ToIntBiFunction[getValues.size() - 1];
				for (int i = 0; iterator.hasNext(); i++) {
					Function<BindingSet, Value> getValue = iterator.next();
					hashOtherValues[i] = (bs, nextHash) -> hashAValue(getValue, nextHash, bs);
				}
				// Again a set of special cased hashcode methods which avoid an array value and
				// length checks;
				switch (hashOtherValues.length) {
				case 1: {
					ToIntBiFunction<BindingSet, Integer> hashSecondValue = hashOtherValues[0];
					return (bs) -> {
						// Take the hash of the first value
						int nextHash = hashFirstValue.applyAsInt(bs);
						return hashSecondValue.applyAsInt(bs, nextHash);
					};
				}
				case 2: {
					ToIntBiFunction<BindingSet, Integer> hashSecondValue = hashOtherValues[0];
					ToIntBiFunction<BindingSet, Integer> hashThirdValue = hashOtherValues[1];
					return (bs) -> {
						int nextHash = hashFirstValue.applyAsInt(bs);
						nextHash = hashSecondValue.applyAsInt(bs, nextHash);
						nextHash = hashThirdValue.applyAsInt(bs, nextHash);
						return nextHash;
					};
				}
				case 3: {
					ToIntBiFunction<BindingSet, Integer> hashSecondValue = hashOtherValues[0];
					ToIntBiFunction<BindingSet, Integer> hashThirdValue = hashOtherValues[1];
					ToIntBiFunction<BindingSet, Integer> hashFourthValue = hashOtherValues[2];
					return (bs) -> {
						int nextHash = hashFirstValue.applyAsInt(bs);
						nextHash = hashSecondValue.applyAsInt(bs, nextHash);
						nextHash = hashThirdValue.applyAsInt(bs, nextHash);
						nextHash = hashFourthValue.applyAsInt(bs, nextHash);
						return nextHash;
					};
				}
				default: {
					return (bs) -> {
						// Take the hash of the first value
						int nextHash = hashFirstValue.applyAsInt(bs);
						for (int i = 0; i < hashOtherValues.length; i++) {
							// hash the next values in order.
							nextHash = hashOtherValues[i].applyAsInt(bs, nextHash);
						}
						return nextHash;
					};
				}
				}
			}
		} else {
			// If the values is empty hash is always one.
			return (bs) -> 1;
		}
	}
}
