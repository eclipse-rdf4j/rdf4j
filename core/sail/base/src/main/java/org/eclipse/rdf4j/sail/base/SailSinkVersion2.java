/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * An extension of the SailSink interface to support Statements directly for better performance.
 *
 * @author Håvard Ottestad
 */
public interface SailSinkVersion2 extends SailSink {

	/**
	 * Removes a statement.
	 *
	 * @param statement The statement that should be removed
	 * @throws SailException If the statement could not be removed, for example because no transaction is active.
	 */
	void deprecate(Statement statement) throws SailException;

	/**
	 * Removes all statements with the specified subject, predicate, object, and context. All four parameters may be
	 * null.
	 *
	 * @throws SailException If statements could not be removed, for example because no transaction is active.
	 */
	boolean deprecateByQuery(Resource subj, IRI pred, Value obj, Resource[] contexts);

}
