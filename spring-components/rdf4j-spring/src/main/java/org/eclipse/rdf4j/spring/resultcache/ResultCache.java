package org.eclipse.rdf4j.spring.resultcache;

public interface ResultCache<K, T> extends Clearable {

	T get(K key);

	void put(K key, T cachedObject);

	/**
	 * Calling this method instructs the cache to return <code>null</code> to all {@link #get(K)} calls and ignore any
	 * {@link #put(K, T)} calls from the current thread until the cache is cleared. Context: after a write operation on
	 * a connection (which is assumed to be handled exclusively by a dedicated thread), the local cache must be cleared
	 * and the global cache bypassed until the connection is returned.
	 */
	void bypassForCurrentThread();
}
