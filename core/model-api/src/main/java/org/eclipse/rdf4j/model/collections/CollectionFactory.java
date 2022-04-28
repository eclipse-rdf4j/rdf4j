package org.eclipse.rdf4j.model.collections;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;

/**
 * A Factory that may generate optimised and/or disk based collections
 *
 */
public interface CollectionFactory {
	/**
	 * @param <T> of the list
	 * @return a list that may be optimised and/or disk based
	 */
	public <T> List<T> createList();

	/**
	 * @param <T> of the list
	 * @return a list that may be optimised and/or disk based for Values only
	 */
	public <T extends Value> List<T> createValueList();

	/**
	 * @param <T> of the set
	 * @return a set that may be optimised and/or disk based
	 */
	public <T> Set<T> createSet();

	/**
	 * @param <T> of the set
	 * @return a set that may be optimised and/or disk based for Values
	 */
	public <T extends Value> Set<T> createValueSet();

	/**
	 * @param <K> key type
	 * @param <V> value type
	 * @return a map
	 */
	public <K, V> Map<K, V> createMap();

	/**
	 * @param <K> key type that must be a kind of value
	 * @param <V> value type
	 * @return a map
	 */
	public <K extends Value, V> Map<K, V> createValueKeyedMap();

	/**
	 * @param <T> of the contents of the queue
	 * @return a new queue
	 */
	public <T> Queue<T> createQueue();

	/**
	 * @param <T> of the contents of the queue, that must be a value
	 * @return a new queue
	 */
	public <T extends Value> Queue<T> createValueQueue();

	public default byte[] toByte(Value v) {
		try (ByteArrayOutputStream boas = new ByteArrayOutputStream()) {
			try (ObjectOutputStream out = new ObjectOutputStream(boas)) {
				out.writeObject(v);
			}
			return boas.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
