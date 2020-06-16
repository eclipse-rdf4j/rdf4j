/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;

/**
 * @author Håvard Ottestad
 */
public class ExternalFilterIsObject extends FilterPlanNode {

	private final SailConnection connection;
	private final int index;

	public ExternalFilterIsObject(SailConnection connection, PlanNode parent, int index) {
		super(parent);
		this.connection = connection;
		this.index = index;
	}

	@Override
	boolean checkTuple(Tuple t) {

		Value value = t.getLine().get(index);

		return connection.hasStatement(null, null, value, true);

	}

	@Override
	public String toString() {
		return "ExternalFilterIsObject{" +
				"index=" + index +
				'}';
	}
}
