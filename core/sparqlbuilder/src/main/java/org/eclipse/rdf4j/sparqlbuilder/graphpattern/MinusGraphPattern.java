/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

class MinusGraphPattern extends GroupGraphPattern {
	private static final String MINUS = "MINUS";

	@Override
	public String getQueryString() {
		return MINUS + " " + super.getQueryString();
	}
}
