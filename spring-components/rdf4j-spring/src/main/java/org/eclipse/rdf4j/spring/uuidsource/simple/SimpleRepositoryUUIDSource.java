package org.eclipse.rdf4j.spring.uuidsource.simple;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.support.Rdf4JTemplate;
import org.eclipse.rdf4j.spring.support.UUIDSource;
import org.eclipse.rdf4j.spring.util.QueryResultUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class SimpleRepositoryUUIDSource implements UUIDSource {

	@Autowired
	Rdf4JTemplate rdf4JTemplate;

	@Override
	public IRI nextUUID() {
		return rdf4JTemplate
				.tupleQuery("SELECT (UUID() as ?id) WHERE {}")
				.evaluateAndConvert()
				.toSingleton(b -> QueryResultUtils.getIRI(b, "id"));
	}
}
