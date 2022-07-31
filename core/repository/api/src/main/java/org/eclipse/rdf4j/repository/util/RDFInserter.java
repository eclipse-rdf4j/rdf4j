/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.util;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * An RDFHandler that adds RDF data to a repository.
 *
 * @author jeen
 */
public class RDFInserter extends AbstractRDFInserter {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The connection to use for the add operations.
	 */
	protected final RepositoryConnection con;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFInserter object that preserves bnode IDs and that does not enforce any context upon statements
	 * that are reported to it.
	 *
	 * @param con The connection to use for the add operations.
	 */
	public RDFInserter(RepositoryConnection con) {
		super(con.getValueFactory());
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
			con.add(subj, pred, obj, contexts);
		} else {
			con.add(subj, pred, obj, ctxt);
		}
	}
}
