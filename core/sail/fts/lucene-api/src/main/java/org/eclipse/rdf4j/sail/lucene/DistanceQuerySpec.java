/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.util.List;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;

public class DistanceQuerySpec implements SearchQueryEvaluator {
	private QueryModelNode functionParent;
	private Literal from;
	private URI units;
	private double distance;
	private String distanceVar;
	private StatementPattern geoStatement;
	private Filter filter;

	public void setFunctionParent(QueryModelNode functionParent) {
		this.functionParent = functionParent;
	}

	public void setFrom(Literal from) {
		this.from = from;
	}

	public Literal getFrom() {
		return from;
	}

	public void setUnits(URI units) {
		this.units = units;
	}

	public URI getUnits() {
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
		if(sp.getSubjectVar().hasValue()) {
			throw new IllegalArgumentException("Subject cannot be bound: "+sp);
		}
		if(!sp.getPredicateVar().hasValue()) {
			throw new IllegalArgumentException("Predicate must be bound: "+sp);
		}
		if(sp.getObjectVar().hasValue()) {
			throw new IllegalArgumentException("Object cannot be bound: "+sp);
		}
		this.geoStatement = sp;
	}

	public String getSubjectVar() {
		return geoStatement.getSubjectVar().getName();
	}

	public Var getContextVar() {
		return geoStatement.getContextVar();
	}

	public URI getGeoProperty() {
		return (URI) geoStatement.getPredicateVar().getValue();
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
	public void updateQueryModelNodes(boolean hasResult) {
		QueryModelNode replacementNode = hasResult ? new SingletonSet() : new EmptySet();
		geoStatement.replaceWith(replacementNode);

		if(hasResult) {
			filter.replaceWith(filter.getArg());
		} else {
			filter.replaceWith(new EmptySet());
		}

		if(functionParent instanceof ExtensionElem) {
			Extension extension = (Extension) functionParent.getParentNode();
			List<ExtensionElem> elements = extension.getElements();
			if(elements.size() > 1) {
				elements.remove(functionParent);
			} else {
				extension.replaceWith(extension.getArg());
			}
		}
	}
}
