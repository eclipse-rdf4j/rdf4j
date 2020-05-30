/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;

/**
 * @author Håvard Ottestad
 */
public class ExternalFilterIsSubject extends FilterPlanNode {

	private final SailConnection connection;
	private final int index;

	public ExternalFilterIsSubject(SailConnection connection, PlanNode parent, int index) {
		super(parent);
		this.connection = connection;
		this.index = index;
	}

	@Override
	boolean checkTuple(Tuple t) {

		Value value = t.getLine().get(index);

		if (value instanceof Resource) {
			return connection.hasStatement((Resource) value, null, null, true);
		} else
			return false;

	}

	@Override
	public String toString() {
		return "ExternalFilterIsSubject{" +
				"index=" + index +
				'}';
	}
}
