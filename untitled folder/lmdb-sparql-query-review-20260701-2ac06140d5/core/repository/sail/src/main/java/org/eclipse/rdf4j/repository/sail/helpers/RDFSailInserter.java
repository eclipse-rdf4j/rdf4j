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
package org.eclipse.rdf4j.repository.sail.helpers;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.util.AbstractRDFInserter;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UpdateContext;

/**
 * An RDFHandler that adds RDF data to a sail.
 *
 * @author jeen
 */
public class RDFSailInserter extends AbstractRDFInserter {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The connection to use for the add operations.
	 */
	private final SailConnection con;

	private final UpdateContext uc;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFInserter object that preserves bnode IDs and that does not enforce any context upon statements
	 * that are reported to it.
	 *
	 * @param con The connection to use for the add operations.
	 */
	public RDFSailInserter(SailConnection con, ValueFactory vf, UpdateContext uc) {
		super(vf);
		this.con = con;
		this.uc = uc;
	}

	public RDFSailInserter(SailConnection con, ValueFactory vf) {
		this(con, vf, null);
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
			addStatement(uc, subj, pred, obj, contexts);
		} else {
			if (uc != null && ctxt == null) {
				final IRI insertGraph = uc.getDataset().getDefaultInsertGraph();
				if (insertGraph != null) {
					addStatement(uc, subj, pred, obj, insertGraph);
				} else {
					addStatement(uc, subj, pred, obj);
				}
			} else {
				addStatement(uc, subj, pred, obj, ctxt);
			}
		}
	}

	private void addStatement(UpdateContext uc, Resource subj, IRI pred, Value obj, Resource... ctxts)
			throws SailException {
		if (uc != null) {
			con.addStatement(uc, subj, pred, obj, ctxts);
		} else {
			con.addStatement(subj, pred, obj, ctxts);
		}
	}
}
