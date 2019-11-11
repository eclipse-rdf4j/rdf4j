/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.client.Client;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public abstract class DataStructureInterface {

	public abstract void addStatement(Client client, Statement statement);

	public abstract void removeStatement(Client client, Statement statement);

	public abstract CloseableIteration<? extends Statement, SailException> getStatements(Client client,
			Resource subject,
			IRI predicate,
			Value object,
			Resource... context);

	public abstract void flush(Client client);

	protected boolean containsContext(Resource[] context, Resource context1) {
		for (Resource resource : context) {
			if (resource == null && context1 == null) {
				return true;
			}
			if (resource != null && resource.equals(context1)) {
				return true;
			}
		}
		return false;
	}

	abstract String getHostname();

	abstract int getPort();

	abstract String getClustername();

	abstract void init();

	public abstract void clear(Client client, Resource[] contexts);

	abstract void flushThrough(Client client);

}
