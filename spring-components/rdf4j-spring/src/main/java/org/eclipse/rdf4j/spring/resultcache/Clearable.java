package org.eclipse.rdf4j.spring.resultcache;

public interface Clearable {
	void markDirty();

	void clearCachedResults();
}
