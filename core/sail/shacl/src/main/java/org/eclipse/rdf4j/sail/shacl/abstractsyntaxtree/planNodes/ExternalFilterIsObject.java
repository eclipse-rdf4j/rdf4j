/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;

/**
 * @author Håvard Ottestad
 */
public class ExternalFilterIsObject extends FilterPlanNode {

	private final SailConnection connection;

	public ExternalFilterIsObject(SailConnection connection, PlanNode parent) {
		super(parent);
		this.connection = connection;
	}

	@Override
	boolean checkTuple(ValidationTuple t) {

		Value value = t.getValue();

		return connection.hasStatement(null, null, value, true);

	}

	@Override
	public String toString() {
		return "ExternalFilterIsObject{" +
				'}';
	}
}
