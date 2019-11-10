package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleStatement;

public class ElasticsearchStatement extends SimpleStatement implements ElasticsearchId {

	private String elasticsearchId;

	ElasticsearchStatement(String elasticsearchId, Resource subject, IRI predicate, Value object) {
		super(subject, predicate, object);
		this.elasticsearchId = elasticsearchId;
	}

	public String getElasticsearchId() {
		return elasticsearchId;
	}
}
