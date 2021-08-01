package org.eclipse.rdf4j.spring.support;

import java.util.UUID;

import org.eclipse.rdf4j.model.IRI;

public class DefaultUUIDSource implements UUIDSource {
	@Override
	public IRI nextUUID() {
		return toURNUUID(UUID.randomUUID().toString());
	}
}
