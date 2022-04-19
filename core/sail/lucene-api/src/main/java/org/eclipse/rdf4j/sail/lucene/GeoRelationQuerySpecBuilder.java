/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.sail.SailException;

public class GeoRelationQuerySpecBuilder implements SearchQueryInterpreter {

	private final SearchIndex index;

	public GeoRelationQuerySpecBuilder(SearchIndex index) {
		this.index = index;
	}

	@Override
	public void process(TupleExpr tupleExpr, BindingSet bindings, final Collection<SearchQueryEvaluator> results)
			throws SailException {

		tupleExpr.visit(new AbstractQueryModelVisitor<SailException>() {

			final Map<String, GeoRelationQuerySpec> specs = new HashMap<>();

			@Override
			public void meet(FunctionCall f) throws SailException {
				if (f.getURI().startsWith(GEOF.NAMESPACE)) {
					List<ValueExpr> args = f.getArgs();
					if (args.size() != 2) {
						return;
					}

					Literal qshape = getLiteral(args.get(0));
					String varShape = getVarName(args.get(1));

					if (qshape == null || varShape == null) {
						return;
					}

					Filter filter = null;
					String fVar = null;
					QueryModelNode parent = f.getParentNode();
					if (parent instanceof ExtensionElem) {
						fVar = ((ExtensionElem) parent).getName();
						QueryModelNode extension = parent.getParentNode();
						filter = getFilter(extension.getParentNode(), fVar);
					} else if (parent instanceof Filter) {
						filter = (Filter) parent;
					}

					if (filter == null) {
						return;
					}

					GeoRelationQuerySpec spec = new GeoRelationQuerySpec();
					spec.setRelation(f.getURI());
					spec.setFunctionParent(parent);
					spec.setQueryGeometry(qshape);
					spec.setFunctionValueVar(fVar);
					spec.setFilter(filter);
					specs.put(varShape, spec);
				}
			}

			@Override
			public void meet(StatementPattern sp) {
				IRI propertyName = (IRI) sp.getPredicateVar().getValue();
				if (propertyName != null && index.isGeoField(SearchFields.getPropertyField(propertyName))
						&& !sp.getObjectVar().hasValue()) {
					String objectVarName = sp.getObjectVar().getName();
					GeoRelationQuerySpec spec = specs.remove(objectVarName);
					if (spec != null && isChildOf(sp, spec.getFilter())) {
						spec.setGeometryPattern(sp);
						results.add(spec);
					}
				}
			}
		});
	}

	private static boolean isChildOf(QueryModelNode child, QueryModelNode parent) {
		if (child.getParentNode() == parent) {
			return true;
		}
		return isChildOf(child.getParentNode(), parent);
	}

	private static Filter getFilter(QueryModelNode node, String varName) {
		Filter filter = null;
		if (node instanceof Filter) {
			Filter f = (Filter) node;
			ValueExpr condition = f.getCondition();
			if (varName.equals(getVarName(condition))) {
				filter = f;
			}
		} else if (node != null) {
			filter = getFilter(node.getParentNode(), varName);
		}
		return filter;
	}

	private static Literal getLiteral(ValueExpr v) {
		Value value = getValue(v);
		if (value instanceof Literal) {
			return (Literal) value;
		}
		return null;
	}

	private static Value getValue(ValueExpr v) {
		Value value = null;
		if (v instanceof ValueConstant) {
			value = ((ValueConstant) v).getValue();
		} else if (v instanceof Var) {
			value = ((Var) v).getValue();
		}
		return value;
	}

	private static String getVarName(ValueExpr v) {
		if (v instanceof Var) {
			Var var = (Var) v;
			if (!var.isConstant()) {
				return var.getName();
			}
		}
		return null;
	}
}
