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

/**
 * @author Håvard Mikkelsen Ottestad
 */
public abstract class DataStructureInterface {

	public abstract void addStatement(Statement statement);

	public abstract void removeStatement(Statement statement);

	public abstract CloseableIteration<? extends Statement, SailException> getStatements(
			Resource subject,
			IRI predicate,
			Value object,
			Resource... context);

	public abstract void flush();

	abstract void init();

	public abstract void clear(Resource[] contexts);

	abstract void flushThrough();

	abstract boolean removeStatementsByQuery(Resource subj, IRI pred, Value obj, Resource[] contexts);
}
