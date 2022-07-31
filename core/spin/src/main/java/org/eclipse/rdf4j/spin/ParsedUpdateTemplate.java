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

import java.util.Map;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;

public class ParsedUpdateTemplate extends ParsedUpdate implements ParsedTemplate {

	private final Template template;

	private final BindingSet args;

	public ParsedUpdateTemplate(Template template, BindingSet args) {
		this(template, (ParsedUpdate) template.getParsedOperation(), args);
	}

	private ParsedUpdateTemplate(Template template, ParsedUpdate update, BindingSet args) {
		super(update.getSourceString(), update.getNamespaces());
		for (UpdateExpr updateExpr : update.getUpdateExprs()) {
			addUpdateExpr(updateExpr);
		}
		for (Map.Entry<UpdateExpr, Dataset> entry : update.getDatasetMapping().entrySet()) {
			map(entry.getKey(), entry.getValue());
		}
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
