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
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;

public class DistanceQuerySpec extends AbstractSearchQueryEvaluator {

	private FunctionCall distanceFunction;

	private Literal from;

	private IRI units;

	private final ValueExpr distanceExpr;

	private double distance;

	private String distanceVar;

	private StatementPattern geoStatement;

	private String subjectVar;

	private Var contextVar;

	private IRI geoProperty;

	private final String geoVar;

	private Filter filter;

	public DistanceQuerySpec(FunctionCall distanceFunction, ValueExpr distanceExpr, String distVar, Filter filter) {
		this.distanceFunction = distanceFunction;
		this.distanceExpr = distanceExpr;
		this.distanceVar = distVar;
		this.filter = filter;
		if (distanceFunction != null) {
			List<ValueExpr> args = distanceFunction.getArgs();
			this.from = getLiteral(args.get(0));
			this.geoVar = getVarName(args.get(1));
			this.units = getURI(args.get(2));
		} else {
			this.from = null;
			this.geoVar = null;
			this.units = null;
		}
		if (distanceExpr != null) {
			Literal dist = getLiteral(distanceExpr);
			this.distance = (dist != null) ? dist.doubleValue() : Double.NaN;
		} else {
			this.distance = Double.NaN;
		}
	}

	public DistanceQuerySpec(Literal from, IRI units, double dist, String distVar, IRI geoProperty, String geoVar,
			String subjectVar, Var contextVar) {
		this.from = from;
		this.units = units;
		this.distance = dist;
		this.distanceVar = distVar;
		this.geoProperty = geoProperty;
		this.geoVar = geoVar;
		this.subjectVar = subjectVar;
		this.contextVar = contextVar;
		this.distanceFunction = null;
		this.distanceExpr = null;
		this.filter = null;
	}

	public void setFrom(Literal from) {
		this.from = from;
	}

	public Literal getFrom() {
		return from;
	}

	public void setUnits(IRI units) {
		this.units = units;
	}

	public IRI getUnits() {
		return units;
	}

	public void setDistance(double d) {
		this.distance = d;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistanceVar(String varName) {
		this.distanceVar = varName;
	}

	public String getDistanceVar() {
		return distanceVar;
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
		if (!sp.getObjectVar().getName().equals(geoVar)) {
			throw new IllegalArgumentException("Object var name does not match geometry var name");
		}
		this.geoStatement = sp;
		this.subjectVar = sp.getSubjectVar().getName();
		this.contextVar = sp.getContextVar();
		this.geoProperty = (IRI) sp.getPredicateVar().getValue();
	}

	public String getSubjectVar() {
		return subjectVar;
	}

	public Var getContextVar() {
		return contextVar;
	}

	public IRI getGeoProperty() {
		return geoProperty;
	}

	public String getGeoVar() {
		return geoVar;
	}

	public void setDistanceFunctionCall(FunctionCall distanceFunction) {
		this.distanceFunction = distanceFunction;
	}

	public FunctionCall getDistanceFunctionCall() {
		return distanceFunction;
	}

	public ValueExpr getDistanceExpr() {
		return distanceExpr;
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

		QueryModelNode functionParent = distanceFunction.getParentNode();
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

	public boolean isEvaluable() {
		return (getFrom() != null && !Double.isNaN(distance) && getUnits() != null && geoProperty != null);
	}

	static Literal getLiteral(ValueExpr v) {
		Value value = getValue(v);
		if (value instanceof Literal) {
			return (Literal) value;
		}
		return null;
	}

	static IRI getURI(ValueExpr v) {
		Value value = getValue(v);
		if (value instanceof IRI) {
			return (IRI) value;
		}
		return null;
	}

	static Value getValue(ValueExpr v) {
		Value value = null;
		if (v instanceof ValueConstant) {
			value = ((ValueConstant) v).getValue();
		} else if (v instanceof Var) {
			value = ((Var) v).getValue();
		}
		return value;
	}

	static String getVarName(ValueExpr v) {
		if (v instanceof Var) {
			Var var = (Var) v;
			if (!var.isConstant()) {
				return var.getName();
			}
		}
		return null;
	}
}
