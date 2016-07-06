package org.eclipse.rdf4j.sail.federation.optimizers;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * A zero-cost bloom filter that always returns true (no negatives).
 */
public class InaccurateRepositoryBloomFilter implements RepositoryBloomFilter {
	@Override
	public boolean mayHaveStatement(RepositoryConnection conn, Resource subj, IRI pred, Value obj,
			Resource... contexts)
	{
		return true;
	}

}
