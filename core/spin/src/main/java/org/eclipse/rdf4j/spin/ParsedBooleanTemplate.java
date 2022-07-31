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
package org.eclipse.rdf4j.spin;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;

public class ParsedBooleanTemplate extends ParsedBooleanQuery implements ParsedTemplate {

	private final Template template;

	private final BindingSet args;

	public ParsedBooleanTemplate(Template template, BindingSet args) {
		this(template, (ParsedBooleanQuery) template.getParsedOperation(), args);
	}

	private ParsedBooleanTemplate(Template template, ParsedBooleanQuery query, BindingSet args) {
		super(query.getSourceString(), query.getTupleExpr());
		setDataset(query.getDataset());
		this.template = template;
		this.args = args;
	}

	@Override
	public Template getTemplate() {
		return template;
	}

	@Override
	public BindingSet getBindings() {
		return args;
	}
}
