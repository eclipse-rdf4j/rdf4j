/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values;

import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep.ConstantQueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility.Result;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

public class QueryValueEvaluationStepSupplier {
	private static Value bound(QueryValueEvaluationStep arg, BindingSet bindings)
			throws ValueExprEvaluationException, QueryEvaluationException {
		try {
			Value argValue = arg.evaluate(bindings);
			return BooleanLiteral.valueOf(argValue != null);
		} catch (ValueExprEvaluationException e) {
			return BooleanLiteral.FALSE;
		}
	}

	private QueryValueEvaluationStepSupplier() {

	}

	public static QueryValueEvaluationStep prepareStr(QueryValueEvaluationStep arg, ValueFactory valueFactory) {
		return make(arg, "Unknown constant argument for STR()", bs -> str(arg, valueFactory, bs));
	}

	private static Value str(QueryValueEvaluationStep arg, ValueFactory valueFactory, BindingSet bindings) {
		Value argValue = arg.evaluate(bindings);

		if (argValue instanceof IRI || argValue instanceof Triple) {
			return valueFactory.createLiteral(argValue.toString());
		} else if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			if (QueryEvaluationUtility.isSimpleLiteral(literal)) {
				return literal;
			} else {
				return valueFactory.createLiteral(literal.getLabel());
			}
		} else {
			throw new ValueExprEvaluationException("Unknown constant argument for STR()");
		}
	}

	public static QueryValueEvaluationStep prepareBound(QueryValueEvaluationStep arg, QueryEvaluationContext context) {
		return make(arg, "bound called on constant argument that throws", bs -> bound(arg, bs));
	}

	public static QueryValueEvaluationStep prepareDatatype(QueryValueEvaluationStep arg,
			QueryEvaluationContext context) {
		return make(arg, "datatype called on constant that throws", bs -> datatype(arg, bs));
	}

	public static QueryValueEvaluationStep prepareLabel(QueryValueEvaluationStep arg, ValueFactory vf) {
		return make(arg, "label called on constant that throws", bs -> label(arg, bs, vf));
	}

	private static QueryValueEvaluationStep make(QueryValueEvaluationStep arg, String errorMessage,
			Function<BindingSet, Value> function) {
		if (arg.isConstant()) {
			try {
				Value datatype = function.apply(EmptyBindingSet.getInstance());
				return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(datatype);
			} catch (ValueExprEvaluationException e) {
				return new QueryValueEvaluationStep.Fail(errorMessage);
			}

		}
		return new QueryValueEvaluationStep.ApplyFunctionForEachBinding(function);
	}

	private static Value label(QueryValueEvaluationStep arg, BindingSet bindings, ValueFactory vf) {
		Value argValue = arg.evaluate(bindings);

		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;

			if (QueryEvaluationUtility.isSimpleLiteral(literal)) {
				return literal;
			} else {
				return vf.createLiteral(literal.getLabel());
			}
		} else {
			throw new ValueExprEvaluationException();
		}
	}

	private static Value datatype(QueryValueEvaluationStep arg, BindingSet bindings) {
		Value v = arg.evaluate(bindings);

		if (v instanceof Literal) {
			Literal literal = (Literal) v;

			if (literal.getDatatype() != null) {
				// literal with datatype
				return literal.getDatatype();
			} else if (literal.getLanguage().isPresent()) {
				return CoreDatatype.RDF.LANGSTRING.getIri();
			} else {
				// simple literal
				return CoreDatatype.XSD.STRING.getIri();
			}

		}
		throw new ValueExprEvaluationException();
	}

	public static QueryValueEvaluationStep prepareVar(Var var, QueryEvaluationContext context) {
		if (var.getValue() != null) {
			return new ConstantQueryValueEvaluationStep(var.getValue());
		} else {
			Function<BindingSet, Value> getValue = context.getValue(var.getName());
			return new QueryValueEvaluationStep.ApplyFunctionForEachBinding(bindings -> {
				Value val = getValue.apply(bindings);
				if (val == null) {
					throw new ValueExprEvaluationException();
				}
				return val;
			});
		}
	}

	public static QueryValueEvaluationStep prepareNamespace(QueryValueEvaluationStep arg, ValueFactory vf) {
		return make(arg, "namespace called on constant that throws", bs -> namespace(arg, bs, vf));
	}

	private static Value namespace(QueryValueEvaluationStep arg, BindingSet bindings, ValueFactory valueFactory) {
		Value argValue = arg.evaluate(bindings);

		if (argValue instanceof IRI) {
			IRI uri = (IRI) argValue;
			return valueFactory.createIRI(uri.getNamespace());
		} else {
			throw new ValueExprEvaluationException();
		}
	}

	public static QueryValueEvaluationStep prepareLocalName(QueryValueEvaluationStep arg, ValueFactory vf) {
		return make(arg, "localName called on constant that throws", bs -> localName(arg, bs, vf));
	}

	private static Value localName(QueryValueEvaluationStep arg, BindingSet bindings, ValueFactory valueFactory) {
		Value argValue = arg.evaluate(bindings);
		if (argValue instanceof IRI) {
			IRI uri = (IRI) argValue;
			return valueFactory.createLiteral(uri.getLocalName());
		} else {
			throw new ValueExprEvaluationException();
		}
	}

	public static QueryValueEvaluationStep prepareLang(QueryValueEvaluationStep arg, ValueFactory vf) {
		return make(arg, "lang called on constant that throws", bs -> lang(arg, bs, vf));
	}

	private static Value lang(QueryValueEvaluationStep arg, BindingSet bindings, ValueFactory valueFactory) {
		Value argValue = arg.evaluate(bindings);

		if (argValue instanceof Literal) {
			Literal literal = (Literal) argValue;
			return valueFactory.createLiteral(literal.getLanguage().orElse(""));
		}

		throw new ValueExprEvaluationException();
	}

	public static QueryValueEvaluationStep bnode(QueryValueEvaluationStep nodeVes, ValueFactory vf) {
		try {
			if (nodeVes.isConstant()) {
				Value nodeId = nodeVes.evaluate(EmptyBindingSet.getInstance());
				if (nodeId instanceof Literal) {
					String nodeLabel = nodeId.stringValue();
					return new QueryValueEvaluationStep.ApplyFunctionForEachBinding(
							bs -> vf.createBNode(nodeLabel + bs.toString().hashCode()));
				} else {
					return new QueryValueEvaluationStep.Fail("BNODE function argument must be a literal");
				}
			} else {
				return new QueryValueEvaluationStep.ApplyFunctionForEachBinding(
						bs -> vf.createBNode(nodeVes.evaluate(bs).stringValue() + bs.toString().hashCode()));
			}
		} catch (ValueExprEvaluationException e) {
			return new QueryValueEvaluationStep.Fail("BNODE function argument must be a literal");
		}
	}

	public static QueryValueEvaluationStep prepareIs(QueryValueEvaluationStep arg, Predicate<Value> is) {
		return make(arg, "isResource called on constant that throws", bs -> {
			Value argValue = arg.evaluate(bs);
			return BooleanLiteral.valueOf(is.test(argValue));
		});
	}

	public static QueryValueEvaluationStep prepareAnd(QueryValueEvaluationStep leftStep,
			QueryValueEvaluationStep rightStep) {

		if (leftStep.isConstant()) {
			Result constantLeftValue = QueryEvaluationUtility
					.getEffectiveBooleanValue(leftStep.evaluate(EmptyBindingSet.getInstance()));
			if (constantLeftValue == QueryEvaluationUtility.Result._false) {
				return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.FALSE);
			} else if (constantLeftValue == QueryEvaluationUtility.Result._true && rightStep.isConstant()) {
				Result constantRightValue = QueryEvaluationUtility
						.getEffectiveBooleanValue(rightStep.evaluate(EmptyBindingSet.getInstance()));
				if (constantRightValue == QueryEvaluationUtility.Result._false) {
					return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.FALSE);
				} else if (constantRightValue == QueryEvaluationUtility.Result._true) {
					return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.TRUE);
				}
			}
		}
		if (rightStep.isConstant()) {
			Result constantRightValue = QueryEvaluationUtility
					.getEffectiveBooleanValue(rightStep.evaluate(EmptyBindingSet.getInstance()));
			if (constantRightValue == QueryEvaluationUtility.Result._false) {
				return new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(BooleanLiteral.FALSE);
			}
		}
		return new QueryValueEvaluationStep() {

			@Override
			public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
				try {

					if (QueryEvaluationUtility.getEffectiveBooleanValue(
							leftStep.evaluate(bindings)) == QueryEvaluationUtility.Result._false) {
						// Left argument evaluates to false, we don't need to look any
						// further
						return BooleanLiteral.FALSE;
					}
				} catch (ValueExprEvaluationException e) {
					// Failed to evaluate the left argument. Result is 'false' when
					// the right argument evaluates to 'false', failure otherwise.
					Value rightValue = rightStep.evaluate(bindings);
					if (QueryEvaluationUtility
							.getEffectiveBooleanValue(rightValue) == QueryEvaluationUtility.Result._false) {
						return BooleanLiteral.FALSE;
					} else {
						throw new ValueExprEvaluationException();
					}
				}

				// Left argument evaluated to 'true', result is determined
				// by the evaluation of the right argument.
				Value rightValue = rightStep.evaluate(bindings);
				return BooleanLiteral.valueOf(QueryEvaluationUtil.getEffectiveBooleanValue(rightValue));
			}
		};
	}

	public static QueryValueEvaluationStep prepareIriFunction(IRIFunction node, QueryValueEvaluationStep arg,
			ValueFactory valueFactory) {
		return make(arg, "IRI() called on constant that throws",
				bs -> iriFunction(node.getBaseURI(), arg.evaluate(bs), valueFactory));
	}

	private static IRI iriFunction(String baseURI, Value argValue, ValueFactory vf) {
		if (argValue instanceof Literal) {
			final Literal lit = (Literal) argValue;

			String uriString = lit.getLabel();
			try {
				ParsedIRI iri = ParsedIRI.create(uriString);
				if (!iri.isAbsolute() && baseURI != null) {
					// uri string may be a relative reference.
					uriString = ParsedIRI.create(baseURI).resolve(iri).toString();
				} else if (!iri.isAbsolute()) {
					throw new ValueExprEvaluationException("not an absolute IRI reference: " + uriString);
				}
			} catch (IllegalArgumentException e) {
				throw new ValueExprEvaluationException("not a valid IRI reference: " + uriString);
			}

			IRI result;

			try {
				result = vf.createIRI(uriString);
			} catch (IllegalArgumentException e) {
				throw new ValueExprEvaluationException(e.getMessage());
			}
			return result;
		} else if (argValue instanceof IRI) {
			return (IRI) argValue;
		}

		throw new ValueExprEvaluationException();
	}
}
