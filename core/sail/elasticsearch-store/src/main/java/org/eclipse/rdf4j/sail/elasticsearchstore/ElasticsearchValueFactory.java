package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.AbstractValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class ElasticsearchValueFactory extends AbstractValueFactory {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final ElasticsearchValueFactory sharedInstance = new ElasticsearchValueFactory();

	/**
	 * Provide a single shared instance of a SimpleValueFactory.
	 *
	 * @return a singleton instance of SimpleValueFactory.
	 */
	static ElasticsearchValueFactory getInstance() {
		return sharedInstance;
	}

	/**
	 * Hidden constructor to enforce singleton pattern.
	 */
	private ElasticsearchValueFactory() {
	}

	public Statement createStatement(String elasticsearchID, Resource subject, IRI predicate, Value object) {
		return new ElasticsearchStatement(elasticsearchID, subject, predicate, object);
	}

	public Statement createStatement(String elasticsearchID, Resource subject, IRI predicate, Value object,
			Resource context) {
		return new ElasticsearchContextStatement(elasticsearchID, subject, predicate, object, context);
	}
}
