package org.eclipse.rdf4j.sail.federation.optimizers;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * A bloom filter that is exact (no false positives) at the cost of always having to query the repository.
 */
public class AccurateRepositoryBloomFilter implements RepositoryBloomFilter {

	private final boolean includeInferred;

	public AccurateRepositoryBloomFilter(boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	@Override
	public boolean mayHaveStatement(RepositoryConnection conn, Resource subj, IRI pred, Value obj,
			Resource... contexts)
	{
		return conn.hasStatement(subj, pred, obj, includeInferred, contexts);
	}

}
