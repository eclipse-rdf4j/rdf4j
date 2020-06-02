/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.model.Value;

/**
 * @author Håvard Ottestad
 */
public class MaxLengthFilter extends FilterPlanNode {

	private final long maxLength;

	public MaxLengthFilter(PlanNode parent, long maxLength) {
		super(parent);
		this.maxLength = maxLength;
	}

	@Override
	boolean checkTuple(Tuple t) {
		Value literal = t.getLine().get(1);

		return literal.stringValue().length() <= maxLength;
	}

	@Override
	public String toString() {
		return "MaxLengthFilter{" + "maxLength=" + maxLength + '}';
	}
}
