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
package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;

/**
 * Forward-chaining RDF Schema inferencer, using the rules from the
 * <a href="http://www.w3.org/TR/2004/REC-rdf-mt-20040210/">RDF Semantics Recommendation (10 February 2004)</a>. This
 * inferencer can be used to add RDF Schema semantics to any Sail that returns {@link InferencerConnection}s from their
 * {@link Sail#getConnection()} method.
 *
 * @deprecated since 2.5. This inferencer implementation will be phased out. Consider switching to the
 *             {@link SchemaCachingRDFSInferencer} instead.
 */
@Deprecated
public class ForwardChainingRDFSInferencer extends AbstractForwardChainingInferencer {
	/*--------------*
	 * Constructors *
	 *--------------*/

	public ForwardChainingRDFSInferencer() {
		super();
	}

	public ForwardChainingRDFSInferencer(NotifyingSail baseSail) {
		super(baseSail);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public ForwardChainingRDFSInferencerConnection getConnection() throws SailException {
		try {
			InferencerConnection con = (InferencerConnection) super.getConnection();
			return new ForwardChainingRDFSInferencerConnection(this, con);
		} catch (ClassCastException e) {
			throw new SailException(e.getMessage(), e);
		}
	}

	/**
	 * Adds axiom statements to the underlying Sail.
	 */
	@Override
	public void init() throws SailException {
		super.init();

		try (ForwardChainingRDFSInferencerConnection con = getConnection()) {
			con.begin();
			con.addAxiomStatements();
			con.commit();
		}
	}
}
