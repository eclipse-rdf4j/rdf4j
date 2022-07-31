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

package org.eclipse.rdf4j.sparqlbuilder.constraint;

import java.util.List;

import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class NotIn extends Expression<NotIn> {
	private List<RdfValue> options;
	private Variable var;

	NotIn(Variable var, RdfValue... options) {
		super(null, ", ");
		setOperatorName(var.getQueryString() + " NOT IN");
		parenthesize(true);
		addOperand(options);
	}
}
