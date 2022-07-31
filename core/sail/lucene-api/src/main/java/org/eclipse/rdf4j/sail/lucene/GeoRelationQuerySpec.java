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
package org.eclipse.rdf4j.sail.lucene;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;

public class GeoRelationQuerySpec extends AbstractSearchQueryEvaluator {

	private String relation;

	private QueryModelNode functionParent;

	private Literal qshape;

	private String valueVar;

	private StatementPattern geoStatement;

	private Filter filter;

	public void setRelation(String relation) {
		this.relation = relation;
	}

	public String getRelation() {
		return relation;
	}

	public void setFunctionParent(QueryModelNode functionParent) {
		this.functionParent = functionParent;
	}

	public void setQueryGeometry(Literal shape) {
		this.qshape = shape;
	}

	public Literal getQueryGeometry() {
		return qshape;
	}

	public void setFunctionValueVar(String varName) {
		this.valueVar = varName;
	}

	public String getFunctionValueVar() {
		return valueVar;
	}

	public void setGeometryPattern(StatementPattern sp) {
		if (sp.getSubjectVar().hasValue()) {
			throw new IllegalArgumentException("Subject cannot be bound: " + sp);
		}
		if (!sp.getPredicateVar().hasValue()) {
			throw new IllegalArgumentException("Predicate must be bound: " + sp);
		}
		if (sp.getObjectVar().hasValue()) {
			throw new IllegalArgumentException("Object cannot be bound: " + sp);
		}
		this.geoStatement = sp;
	}

	public String getSubjectVar() {
		return geoStatement.getSubjectVar().getName();
	}

	public Var getContextVar() {
		return geoStatement.getContextVar();
	}

	public IRI getGeoProperty() {
		return (IRI) geoStatement.getPredicateVar().getValue();
	}

	public String getGeoVar() {
		return geoStatement.getObjectVar().getName();
	}

	public void setFilter(Filter f) {
		this.filter = f;
	}

	public Filter getFilter() {
		return filter;
	}

	@Override
	public QueryModelNode getParentQueryModelNode() {
		return filter;
	}

	@Override
	public QueryModelNode removeQueryPatterns() {
		final QueryModelNode placeholder = new SingletonSet();

		filter.replaceWith(filter.getArg());

		geoStatement.replaceWith(placeholder);

		if (functionParent instanceof ExtensionElem) {
			Extension extension = (Extension) functionParent.getParentNode();
			List<ExtensionElem> elements = extension.getElements();
			if (elements.size() > 1) {
				elements.remove(functionParent);
			} else {
				extension.replaceWith(extension.getArg());
			}
		}

		return placeholder;
	}
}
