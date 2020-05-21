/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.util;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.util.AbstractRDFInserter;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;

/**
 * An RDFHandler that adds RDF data to a sail as inferred statements.
 */
public class RDFInferencerInserter extends AbstractRDFInserter {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The connection to use for the add operations.
	 */
	private final InferencerConnection con;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFInserter object that preserves bnode IDs and that does not enforce any context upon statements
	 * that are reported to it.
	 *
	 * @param con The connection to use for the add operations.
	 */
	public RDFInferencerInserter(InferencerConnection con, ValueFactory vf) {
		super(vf);
		this.con = con;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void addNamespace(String prefix, String name) throws RDF4JException {
		if (con.getNamespace(prefix) == null) {
			con.setNamespace(prefix, name);
		}
	}

	@Override
	protected void addStatement(Resource subj, IRI pred, Value obj, Resource ctxt) throws RDF4JException {
		if (enforcesContext()) {
			con.addInferredStatement(subj, pred, obj, contexts);
		} else {
			con.addInferredStatement(subj, pred, obj, ctxt);
		}
	}
}
