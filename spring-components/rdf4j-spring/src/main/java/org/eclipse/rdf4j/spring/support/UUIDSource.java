package org.eclipse.rdf4j.spring.support;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.MultiIRI;

/**
 * Interface for making different approaches of obtaining new UUIDs pluggable into the
 * {@link org.eclipse.rdf4j.spring.support.Rdf4JTemplate Rdf4JTemplate}. The {@link org.eclipse.rdf4j.spring.Rdf4JConfig
 * Rdf4JConfig}.
 *
 * <p>
 * For more information, see {@link org.eclipse.rdf4j.spring.uuidsource}.
 */
public interface UUIDSource {
	IRI nextUUID();

	default IRI toURNUUID(String uuid) {
		return new MultiIRI("urn:uuid:", uuid);
	}
}
