/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;

/**
 * @author Håvard Ottestad
 */
public class ExternalFilterIsSubject extends FilterPlanNode {

	private final SailConnection connection;

	public ExternalFilterIsSubject(SailConnection connection, PlanNode parent) {
		super(parent);
		this.connection = connection;
	}

	@Override
	boolean checkTuple(ValidationTuple t) {

		Value value = t.getValue();

		if (value.isResource()) {
			return connection.hasStatement((Resource) value, null, null, true);
		} else {
			return false;
		}

	}

	@Override
	public String toString() {
		return "ExternalFilterIsSubject{" +
				'}';
	}
}
