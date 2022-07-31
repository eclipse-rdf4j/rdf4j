/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  *
 * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class SequencePath implements PropertyPath {
	private final PropertyPath left;
	private final PropertyPath right;

	public SequencePath(PropertyPath left, PropertyPath right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public String getQueryString() {
		return left.getQueryString() + " / " + right.getQueryString();
	}
}
