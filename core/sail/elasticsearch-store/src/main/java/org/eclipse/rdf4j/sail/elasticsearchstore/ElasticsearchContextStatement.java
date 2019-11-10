package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.ContextStatement;

public class ElasticsearchContextStatement extends ContextStatement implements ElasticsearchId {

	private String elasticsearchId;

	public ElasticsearchContextStatement(String elasticsearchId, Resource subject, IRI predicate, Value object,
			Resource context) {
		super(subject, predicate, object, context);
		this.elasticsearchId = elasticsearchId;
	}

	public String getElasticsearchId() {
		return elasticsearchId;
	}
}
