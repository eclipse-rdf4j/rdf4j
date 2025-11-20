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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.TupleFunctionCall;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.sail.SailException;

public class DistanceQuerySpecBuilder implements SearchQueryInterpreter {

	private final SearchIndex index;

	public DistanceQuerySpecBuilder(SearchIndex index) {
		this.index = index;
	}

	@Override
	public void process(TupleExpr tupleExpr, BindingSet bindings, final Collection<SearchQueryEvaluator> results)
			throws SailException {

		tupleExpr.visit(new AbstractQueryModelVisitor<SailException>() {

			final Map<String, DistanceQuerySpec> specs = new HashMap<>();
			final Map<String, StatementPattern> pendingPatterns = new HashMap<>();

			@Override
			public void meet(FunctionCall f) throws SailException {
				if (GEOF.DISTANCE.stringValue().equals(f.getURI())) {
					List<ValueExpr> args = f.getArgs();
					if (args.size() != 3) {
						return;
					}
					Filter filter = null;
					ValueExpr dist = null;
					String distanceVar = null;
					QueryModelNode parent = f.getParentNode();

					// distance is directly bound in an extension
					if (parent instanceof ExtensionElem) {
						distanceVar = ((ExtensionElem) parent).getName();
						QueryModelNode extension = parent.getParentNode();
						Object[] rv = getFilterAndDistance(extension.getParentNode(), distanceVar);
						if (rv == null) {
							return;
						}
						filter = (Filter) rv[0];
						dist = (ValueExpr) rv[1];
					} else if (parent instanceof MathExpr && parent.getParentNode() instanceof ExtensionElem) {
						// distance is wrapped in a math expression inside an extension, e.g.
						// bind( geof:distance(...) / 1000 as ?dist )
						ExtensionElem ext = (ExtensionElem) parent.getParentNode();
						distanceVar = ext.getName();
						QueryModelNode extension = ext.getParentNode();
						Object[] rv = getFilterAndDistance(extension.getParentNode(), distanceVar);
						if (rv == null) {
							return;
						}
						filter = (Filter) rv[0];
						dist = (ValueExpr) rv[1];
					} else if (parent instanceof Compare) {
						filter = (Filter) parent.getParentNode();
						Compare compare = (Compare) parent;
						CompareOp op = compare.getOperator();
						if (op == CompareOp.LT && compare.getLeftArg() == f) {
							dist = compare.getRightArg();
						} else if (op == CompareOp.GT && compare.getRightArg() == f) {
							dist = compare.getLeftArg();
						}
					}

					double distanceScaling = 1.0;
					if (f.getParentNode() instanceof MathExpr) {
						MathExpr mathExpr = (MathExpr) f.getParentNode();
						if (mathExpr.getOperator() == MathExpr.MathOp.DIVIDE) {
							Literal divisor = DistanceQuerySpec.getLiteral(mathExpr.getRightArg());
							if (divisor != null) {
								distanceScaling = divisor.doubleValue();
							}
						}
					}

					DistanceQuerySpec spec = new DistanceQuerySpec(f, dist, distanceVar, filter);
					if (!Double.isNaN(spec.getDistance()) && distanceScaling != 1.0) {
						spec.setDistance(spec.getDistance() * distanceScaling);
					}
					specs.put(spec.getGeoVar(), spec);
					StatementPattern pending = pendingPatterns.remove(spec.getGeoVar());
					if (pending != null) {
						attachPattern(pending, specs, results);
					}
				}
			}

			@Override
			public void meet(StatementPattern sp) {
				attachPattern(sp, specs, results);
			}

			private void attachPattern(StatementPattern sp, Map<String, DistanceQuerySpec> specs,
					final Collection<SearchQueryEvaluator> results) {
				IRI propertyName = (IRI) sp.getPredicateVar().getValue();
				if (propertyName != null && index.isGeoField(SearchFields.getPropertyField(propertyName))
						&& !sp.getObjectVar().hasValue()) {
					String objectVarName = sp.getObjectVar().getName();
					DistanceQuerySpec spec = specs.get(objectVarName);
					boolean matchesFilter = spec != null && isChildOf(sp, spec.getFilter());
					if (spec != null && (matchesFilter || sp.getContextVar() != null)) {
						specs.remove(objectVarName);
						spec.setGeometryPattern(sp);
						if (spec.isEvaluable()) {
							// constant optimizer
							results.add(spec);
						} else if (spec.getDistanceFunctionCall() != null && spec.getDistanceExpr() != null
								&& spec.getGeoProperty() != null) {
							// evaluate later
							TupleFunctionCall funcCall = new TupleFunctionCall();
							funcCall.setURI(LuceneSailSchema.WITHIN_DISTANCE.toString());
							FunctionCall df = spec.getDistanceFunctionCall();
							List<ValueExpr> dfArgs = df.getArgs();
							funcCall.addArg(dfArgs.get(0));
							funcCall.addArg(spec.getDistanceExpr());
							funcCall.addArg(dfArgs.get(2));
							funcCall.addArg(new ValueConstant(spec.getGeoProperty()));
							funcCall.addResultVar(sp.getSubjectVar());
							funcCall.addResultVar(sp.getObjectVar());
							if (spec.getDistanceVar() != null) {
								funcCall.addArg(new ValueConstant(LuceneSailSchema.DISTANCE));
								funcCall.addResultVar(Var.of(spec.getDistanceVar()));
							}
							if (spec.getContextVar() != null) {
								Resource context = (Resource) spec.getContextVar().getValue();
								if (context != null) {
									funcCall.addArg(new ValueConstant(context));
								} else {
									funcCall.addArg(new ValueConstant(LuceneSailSchema.CONTEXT));
									funcCall.addResultVar(spec.getContextVar());
								}
							}

							Join join = new Join();
							sp.replaceWith(join);
							join.setLeftArg(sp);
							join.setRightArg(funcCall);

							spec.removeQueryPatterns();
						}
					} else if (spec == null) {
						pendingPatterns.put(objectVarName, sp);
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

	private static Object[] getFilterAndDistance(QueryModelNode node, String compareArgVarName) {
		Object[] rv = null;
		if (node instanceof Filter) {
			Filter f = (Filter) node;
			ValueExpr condition = f.getCondition();
			if (condition instanceof Compare) {
				Compare compare = (Compare) condition;
				CompareOp op = compare.getOperator();
				ValueExpr dist = null;
				if (op == CompareOp.LT
						&& compareArgVarName.equals(DistanceQuerySpec.getVarName(compare.getLeftArg()))) {
					dist = compare.getRightArg();
				} else if (op == CompareOp.GT
						&& compareArgVarName.equals(DistanceQuerySpec.getVarName(compare.getRightArg()))) {
					dist = compare.getLeftArg();
				}
				rv = new Object[] { f, dist };
			}
		} else if (node != null) {
			rv = getFilterAndDistance(node.getParentNode(), compareArgVarName);
		}
		return rv;
	}
}
