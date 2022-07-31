/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.structures;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

public class UnboundStatement implements Statement {

	private static final long serialVersionUID = 2612189412333330052L;

	protected final Resource subj;
	protected final IRI pred;
	protected final Value obj;

	public UnboundStatement(Resource subj, IRI pred, Value obj) {
		super();
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
	}

	@Override
	public Resource getContext() {
		return null;
	}

	@Override
	public Value getObject() {
		return obj;
	}

	@Override
	public IRI getPredicate() {
		return pred;
	}

	@Override
	public Resource getSubject() {
		return subj;
	}

}
