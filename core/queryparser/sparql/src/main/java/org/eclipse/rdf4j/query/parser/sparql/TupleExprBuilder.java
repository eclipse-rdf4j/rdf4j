/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Coalesce;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupConcat;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.IRIFunction;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LangMatches;
import org.eclipse.rdf4j.query.algebra.ListMemberOperator;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.ValueExprTripleRef;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.VariableScopeChange;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;
import org.eclipse.rdf4j.query.algebra.helpers.VarNameCollector;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.StatementPatternCollector;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTAbs;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTAnd;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTAskQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTAvg;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBNodeFunc;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBind;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBindingSet;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBindingValue;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBindingsClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBlankNode;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBlankNodePropertyList;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBound;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCeil;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCoalesce;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCollection;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCompare;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConcat;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConstTripleRef;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConstraint;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConstruct;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConstructQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTContains;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCount;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDatatype;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDay;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDescribe;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDescribeQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTEncodeForURI;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTExistsFunc;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTFalse;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTFloor;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTFunctionCall;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGraphGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGraphPatternGroup;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGroupClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGroupConcat;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTGroupCondition;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTHavingClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTHours;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIRI;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIRIFunc;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIf;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIn;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTInfix;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTInlineData;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIsBlank;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIsIRI;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIsLiteral;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIsNumeric;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTLang;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTLangMatches;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTLimit;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTLowerCase;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMD5;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMath;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMax;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMin;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMinusGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMinutes;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTMonth;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTNot;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTNotExistsFunc;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTNotIn;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTNow;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTNumericLiteral;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTObjectList;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOffset;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOptionalGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOr;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOrderClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOrderCondition;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPathAlternative;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPathElt;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPathMod;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPathOneInPropertySet;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPathSequence;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTProjectionElem;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPropertyList;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTPropertyListPath;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTQName;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTQueryContainer;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTRDFLiteral;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTRand;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTRegexExpression;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTReplace;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTRound;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSHA1;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSHA224;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSHA256;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSHA384;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSHA512;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSTRUUID;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSameTerm;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSample;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSeconds;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSelect;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSelectQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTServiceGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStr;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrAfter;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrBefore;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrDt;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrEnds;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrLang;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrLen;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTStrStarts;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTString;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSubstr;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSum;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTimezone;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTripleRef;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTriplesSameSubjectPath;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTrue;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTz;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUUID;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUnionGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUpperCase;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTVar;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTYear;
import org.eclipse.rdf4j.query.parser.sparql.ast.Node;
import org.eclipse.rdf4j.query.parser.sparql.ast.SimpleNode;
import org.eclipse.rdf4j.query.parser.sparql.ast.VisitorException;

/**
 * A SPARQL AST visitor implementation that creates a query algebra representation of the query.
 *
 * @author Arjohn Kampman
 * @apiNote This feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class TupleExprBuilder extends AbstractASTVisitor {

	// static UUID as prefix together with a thread safe incrementing long ensures a unique identifier.
	private final static String uniqueIdPrefix = UUID.randomUUID().toString().replace("-", "");
	private final static AtomicLong uniqueIdSuffix = new AtomicLong();

	/*-----------*
	 * Variables *
	 *-----------*/

	protected ValueFactory valueFactory;

	GraphPattern graphPattern = new GraphPattern();

	// private Map<ValueConstant, Var> mappedValueConstants = new
	// HashMap<ValueConstant, Var>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public TupleExprBuilder(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Maps the given valueExpr to a Var. If the supplied ValueExpr is a Var, the object itself will be returned. If it
	 * is a ValueConstant, this method will check if an existing variable mapping exists and return that mapped
	 * variable, otherwise it will create and store a new mapping.
	 *
	 * @param valueExpr
	 * @return a Var for the given valueExpr.
	 * @throws IllegalArgumentException if the supplied ValueExpr is null or of an unexpected type.
	 */
	protected Var mapValueExprToVar(Object valueExpr) {
		if (valueExpr instanceof Var) {
			return (Var) valueExpr;
		} else if (valueExpr instanceof ValueConstant) {
			return TupleExprs.createConstVar(((ValueConstant) valueExpr).getValue());
		} else if (valueExpr instanceof TripleRef) {
			return ((TripleRef) valueExpr).getExprVar();
		} else if (valueExpr == null) {
			throw new IllegalArgumentException("valueExpr is null");
		} else {
			throw new IllegalArgumentException("valueExpr is a: " + valueExpr.getClass());
		}
	}

	/**
	 * Retrieve the associated Value (if any) for the given valueExpr.
	 *
	 * @param valueExpr
	 * @return the value of the given ValueExpr, or null if no value exists.
	 * @throws IllegalArgumentException if the supplied ValueExpr is null or of an unexpected type.
	 */
	protected Value getValueForExpr(ValueExpr valueExpr) {
		if (valueExpr instanceof Var) {
			return ((Var) valueExpr).getValue();
		} else if (valueExpr instanceof ValueConstant) {
			ValueConstant vc = (ValueConstant) valueExpr;
			return vc.getValue();
		} else if (valueExpr == null) {
			throw new IllegalArgumentException("valueExpr is null");
		} else {
			throw new IllegalArgumentException("valueExpr is a: " + valueExpr.getClass());
		}
	}

	/**
	 * Creates an anonymous Var with a unique, randomly generated, variable name.
	 *
	 * @return an anonymous Var with a unique, randomly generated, variable name
	 */
	protected Var createAnonVar() {
		// dashes ('-') in the generated UUID are replaced with underscores so
		// the
		// varname
		// remains compatible with the SPARQL grammar. See SES-2310.
		return new Var("_anon_" + uniqueIdPrefix + uniqueIdSuffix.incrementAndGet(), true);
	}

	private FunctionCall createFunctionCall(String uri, SimpleNode node, int minArgs, int maxArgs)
			throws VisitorException {
		FunctionCall functionCall = new FunctionCall(uri);

		int noOfArguments = node.jjtGetNumChildren();

		if (noOfArguments > maxArgs || noOfArguments < minArgs) {
			throw new VisitorException("unexpected number of arguments (" + noOfArguments + ") for function " + uri);
		}

		for (int i = 0; i < noOfArguments; i++) {
			Node argNode = node.jjtGetChild(i);
			functionCall.addArg((ValueExpr) argNode.jjtAccept(this, null));
		}

		return functionCall;
	}

	@Override
	public TupleExpr visit(ASTQueryContainer node, Object data) throws VisitorException {

		// Skip the prolog, any information it contains should already have been
		// processed
		return (TupleExpr) node.getQuery().jjtAccept(this, null);
	}

	@Override
	public TupleExpr visit(ASTSelectQuery node, Object data) throws VisitorException {
		final GraphPattern parentGP = graphPattern;

		// Start with building the graph pattern
		graphPattern = new GraphPattern(parentGP);
		node.getWhereClause().jjtAccept(this, null);
		TupleExpr tupleExpr = graphPattern.buildTupleExpr();

		// Apply grouping
		Group group = null;
		ASTGroupClause groupNode = node.getGroupClause();
		if (groupNode != null) {
			tupleExpr = (TupleExpr) groupNode.jjtAccept(this, tupleExpr);
			group = (Group) tupleExpr;
		}

		final ASTHavingClause havingClause = node.getHavingClause();
		if (havingClause != null) {
			if (group == null) {
				// create implicit group
				group = new Group(tupleExpr);
			}

			// Apply HAVING group filter condition
			tupleExpr = processHavingClause(havingClause, tupleExpr, group);
		}

		// process external VALUES clause
		final ASTBindingsClause bindingsClause = node.getBindingsClause();
		if (bindingsClause != null) {
			// values clause should be treated as scoped to the where clause
			((VariableScopeChange) tupleExpr).setVariableScopeChange(false);
			tupleExpr = new Join((BindingSetAssignment) bindingsClause.jjtAccept(this, null), tupleExpr);
		}

		final ASTOrderClause orderClause = node.getOrderClause();
		if (orderClause != null) {
			if (group == null) {
				// create implicit group
				group = new Group(tupleExpr);
			}

			// Apply result ordering
			tupleExpr = processOrderClause(node.getOrderClause(), tupleExpr, group);
		}

		// Apply projection
		tupleExpr = (TupleExpr) node.getSelect().jjtAccept(this, tupleExpr);

		// Process limit and offset clauses
		ASTLimit limitNode = node.getLimit();
		long limit = -1L;
		if (limitNode != null) {
			limit = (Long) limitNode.jjtAccept(this, null);
		}

		ASTOffset offsetNode = node.getOffset();
		long offset = -1L;
		if (offsetNode != null) {
			offset = (Long) offsetNode.jjtAccept(this, null);
		}

		if (offset >= 1L || limit >= 0L) {
			tupleExpr = new Slice(tupleExpr, offset, limit);
		}

		parentGP.addRequiredTE(tupleExpr);
		graphPattern = parentGP;
		return tupleExpr;
	}

	private TupleExpr processHavingClause(ASTHavingClause havingNode, TupleExpr tupleExpr, Group group)
			throws VisitorException {
		if (havingNode != null) {
			// create an implicit group
			if (group == null) {
				group = new Group(tupleExpr);
			}

			ValueExpr expr = (ValueExpr) havingNode.jjtGetChild(0).jjtAccept(this, tupleExpr);

			// retrieve any aggregate operators from the expression.
			AggregateCollector collector = new AggregateCollector();
			collector.meetOther(expr);

			// replace operator occurrences with an anonymous var, and alias it
			// to the group
			Extension extension = new Extension();
			for (AggregateOperator operator : collector.getOperators()) {
				Var var = createAnonVar();

				// replace occurrence of the operator in the filter expression
				// with the variable.
				AggregateOperatorReplacer replacer = new AggregateOperatorReplacer(operator, var);
				replacer.meetOther(expr);

				String alias = var.getName();

				// create an extension linking the operator to the variable
				// name.
				ExtensionElem pe = new ExtensionElem(operator, alias);
				extension.addElement(pe);

				// add the aggregate operator to the group.
				GroupElem ge = new GroupElem(alias, operator);

				// FIXME quite often the aggregate in the HAVING clause will be
				// a duplicate of an aggregate in the projection. We could
				// perhaps
				// optimize for that, to avoid having to evaluate twice.
				group.addGroupElement(ge);
			}

			extension.setArg(group);
			tupleExpr = new Filter(extension, expr);
		}

		return tupleExpr;
	}

	private TupleExpr processOrderClause(ASTOrderClause orderNode, TupleExpr tupleExpr, Group group)
			throws VisitorException {
		if (orderNode != null) {
			@SuppressWarnings("unchecked")
			List<OrderElem> orderElements = (List<OrderElem>) orderNode.jjtAccept(this, null);

			for (OrderElem orderElem : orderElements) {
				// retrieve any aggregate operators from the order element.
				AggregateCollector collector = new AggregateCollector();
				collector.meet(orderElem);

				Extension extension = new Extension();

				for (AggregateOperator operator : collector.getOperators()) {
					Var var = createAnonVar();

					// replace occurrence of the operator in the order condition
					// with the variable.
					AggregateOperatorReplacer replacer = new AggregateOperatorReplacer(operator, var);
					replacer.meet(orderElem);

					// create an extension linking the operator to the variable
					// name.
					String alias = var.getName();

					ExtensionElem pe = new ExtensionElem(operator, alias);
					extension.addElement(pe);

					// add the aggregate operator to the group.
					GroupElem ge = new GroupElem(alias, operator);
					group.addGroupElement(ge);
				}

				if (!extension.getElements().isEmpty()) {
					extension.setArg(tupleExpr);
					tupleExpr = extension;
				}
			}

			tupleExpr = new Order(tupleExpr, orderElements);

		}
		return tupleExpr;
	}

	@Override
	public TupleExpr visit(ASTSelect node, Object data) throws VisitorException {
		TupleExpr result = (TupleExpr) data;

		final Order orderClause = result instanceof Order ? (Order) result : null;

		Extension extension = new Extension();

		ProjectionElemList projElemList = new ProjectionElemList();

		GroupFinder groupFinder = new GroupFinder();
		result.visit(groupFinder);
		Group group = groupFinder.getGroup();
		boolean existingGroup = group != null;

		List<String> aliasesInProjection = new ArrayList<>();
		for (ASTProjectionElem projElemNode : node.getProjectionElemList()) {

			Node child = projElemNode.jjtGetChild(0);

			String alias = projElemNode.getAlias();
			if (alias != null) {
				// aliased projection element
				if (aliasesInProjection.contains(alias)) {
					throw new VisitorException("duplicate use of alias '" + alias + "' in projection.");
				}

				// check if alias is not previously used.
				if (result.getBindingNames().contains(alias)) {
					throw new VisitorException("projection alias '" + alias + "' was previously used");
				}

				aliasesInProjection.add(alias);

				ValueExpr valueExpr = castToValueExpr(child.jjtAccept(this, null));
				if (valueExpr == null) {
					throw new VisitorException("Either TripleRef or Expression expected in projection.");
				}

				String sourceName = alias;
				if (child instanceof ASTVar) {
					sourceName = ((ASTVar) child).getName();
				}
				ProjectionElem elem = new ProjectionElem(sourceName, alias);
				projElemList.addElement(elem);

				AggregateCollector collector = new AggregateCollector();
				valueExpr.visit(collector);

				if (collector.getOperators().size() > 0) {
					elem.setAggregateOperatorInExpression(true);
					for (AggregateOperator operator : collector.getOperators()) {
						// Apply implicit grouping if necessary
						if (group == null) {
							group = new Group(result);
						}

						if (operator.equals(valueExpr)) {
							group.addGroupElement(new GroupElem(alias, operator));
							extension.setArg(group);
						} else {
							ValueExpr expr = (ValueExpr) operator.getParentNode();

							Extension anonymousExtension = new Extension();
							Var anonVar = createAnonVar();
							expr.replaceChildNode(operator, anonVar);
							anonymousExtension.addElement(new ExtensionElem(operator, anonVar.getName()));

							anonymousExtension.setArg(result);
							result = anonymousExtension;
							group.addGroupElement(new GroupElem(anonVar.getName(), operator));

						}

						if (!existingGroup) {
							result = group;
						}
					}
				}

				if (!(child instanceof ASTVar)) {
					// source of the aliased projection is not a simple variable:
					// add extension element reference to the projection element and
					// to the extension
					ExtensionElem extElem = new ExtensionElem(valueExpr, alias);
					extension.addElement(extElem);
					elem.setSourceExpression(extElem);
				}
			} else if (child instanceof ASTVar) {
				Var projVar = (Var) child.jjtAccept(this, null);
				ProjectionElem elem = new ProjectionElem(projVar.getName());
				projElemList.addElement(elem);
			} else {
				throw new IllegalStateException("required alias for non-Var projection elements not found");
			}
		}

		if (!extension.getElements().isEmpty()) {
			if (orderClause != null) {
				// Extensions produced by SELECT expressions should be nested
				// inside
				// the ORDER BY clause, to make sure
				// sorting can work on the newly introduced variable. See
				// SES-892
				// and SES-1809.
				TupleExpr arg = orderClause.getArg();
				extension.setArg(arg);
				orderClause.setArg(extension);
				result = orderClause;
			} else {
				extension.setArg(result);
				result = extension;
			}
		}

		result = new Projection(result, projElemList);
		if (group != null) {
			Set<String> groupNames = group.getBindingNames();
			List<ProjectionElem> elements = projElemList.getElements();

			for (ProjectionElem elem : elements) {
				if (!elem.hasAggregateOperatorInExpression()) {
					// non-aggregate projection elem is only allowed to be a constant or a simple expression (see
					// https://www.w3.org/TR/sparql11-query/#aggregateRestrictions)
					ExtensionElem extElem = elem.getSourceExpression();
					if (extElem != null) {
						ValueExpr expr = extElem.getExpr();
						if (isIllegalCombinedWithGroupByExpression(expr, elements, groupNames)) {
							throw new VisitorException("non-aggregate expression '" + expr
									+ "' not allowed in projection when using GROUP BY.");
						}

					} else {

						if (!elem.getSourceName().equals(elem.getTargetName())) {
							// Projection element is a SELECT expression using a simple var (e.g. (?a AS ?b)).
							// Source var must be present in GROUP BY.
							if (!groupNames.contains(elem.getSourceName())) {
								if (isIllegalCombinedWithGroupByExpression(elem.getSourceName(), elements,
										groupNames)) {
									throw new VisitorException("variable '" + elem.getSourceName()
											+ "' in projection not present in GROUP BY.");
								}

							}
						} else {
							// projection element is simple var. Must be present in GROUP BY.
							if (!groupNames.contains(elem.getTargetName())) {
								throw new VisitorException("variable '" + elem.getTargetName()
										+ "' in projection not present in GROUP BY.");
							}
						}
					}
				}
			}
		}

		if (node.isSubSelect()) {
			// set context var at the level of the projection. This allows us
			// to distinguish named graphs selected in the
			// outer query from named graphs selected as part of the sub-select.
			((Projection) result).setProjectionContext(graphPattern.getContextVar());
		}

		if (node.isDistinct()) {
			result = new Distinct(result);
		} else if (node.isReduced()) {
			result = new Reduced(result);
		}

		return result;
	}

	private static boolean isIllegalCombinedWithGroupByExpression(ValueExpr expr, List<ProjectionElem> elements,
			Set<String> groupNames) {
		if (expr instanceof ValueConstant) {
			return false;
		}

		VarNameCollector varNameCollector = new VarNameCollector();
		expr.visit(varNameCollector);
		Set<String> varNames = varNameCollector.getVarNames();

		for (String varName : varNames) {
			if (isIllegalCombinedWithGroupByExpression(varName, elements, groupNames)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isIllegalCombinedWithGroupByExpression(String varName, List<ProjectionElem> elements,
			Set<String> groupNames) {
		if (groupNames.contains(varName)) {
			return false;
		}

		do {
			String prev = varName;

			for (ProjectionElem element : elements) {
				if (element.getTargetName().equals(varName)) {
					if (element.hasAggregateOperatorInExpression()) {
						return false;
					} else {
						ExtensionElem sourceExpression = element.getSourceExpression();
						if (sourceExpression != null) {
							if (sourceExpression.getExpr() != null) {
								return isIllegalCombinedWithGroupByExpression(sourceExpression.getExpr(), elements,
										groupNames);
							}
						}

						varName = element.getSourceName();
						break;
					}
				}
			}

			// check if we didn't find a new element
			if (prev.equals(varName)) {
				return true;
			}

		} while (!groupNames.contains(varName));

		return false;
	}

	private static class GroupFinder extends AbstractQueryModelVisitor<VisitorException> {

		private Group group;

		@Override
		public void meet(Projection projection) {
			// stop tree traversal on finding a projection: we do not wish to
			// find
			// the group in a sub-select.
		}

		@Override
		public void meet(Group group) {
			this.group = group;
		}

		public Group getGroup() {
			return group;
		}
	}

	@Override
	public TupleExpr visit(ASTConstructQuery node, Object data) throws VisitorException {
		// Start with building the graph pattern
		graphPattern = new GraphPattern();
		node.getWhereClause().jjtAccept(this, null);
		TupleExpr tupleExpr = graphPattern.buildTupleExpr();

		// Apply grouping
		ASTGroupClause groupNode = node.getGroupClause();
		if (groupNode != null) {

			tupleExpr = (TupleExpr) groupNode.jjtAccept(this, tupleExpr);
		}

		Group group;
		if (tupleExpr instanceof Group) {
			group = (Group) tupleExpr;
		} else {
			// create a new implicit group. Note that this group will only
			// actually
			// be used in the query model if the query has HAVING or ORDER BY
			// clause
			group = new Group(tupleExpr);
		}

		// Apply HAVING group filter condition
		tupleExpr = processHavingClause(node.getHavingClause(), tupleExpr, group);

		// process bindings clause
		ASTBindingsClause bindingsClause = node.getBindingsClause();
		if (bindingsClause != null) {
			tupleExpr = new Join((BindingSetAssignment) bindingsClause.jjtAccept(this, null), tupleExpr);
		}

		// Apply result ordering
		tupleExpr = processOrderClause(node.getOrderClause(), tupleExpr, null);

		// process limit and offset clauses
		ASTLimit limitNode = node.getLimit();
		long limit = -1L;
		if (limitNode != null) {
			limit = (Long) limitNode.jjtAccept(this, null);
		}

		ASTOffset offsetNode = node.getOffset();
		long offset = -1;
		if (offsetNode != null) {
			offset = (Long) offsetNode.jjtAccept(this, null);
		}

		if (offset >= 1 || limit >= 0) {
			tupleExpr = new Slice(tupleExpr, offset, limit);
		}

		// Process construct clause
		ASTConstruct constructNode = node.getConstruct();
		if (!constructNode.isWildcard()) {
			tupleExpr = (TupleExpr) constructNode.jjtAccept(this, tupleExpr);
		} else {
			// create construct clause from graph pattern.
			ConstructorBuilder cb = new ConstructorBuilder();

			// SPARQL does not allow distinct or reduced right now. Leaving
			// functionality in construct builder for
			// possible future use.
			try {
				tupleExpr = cb.buildConstructor(tupleExpr, false, false);
			} catch (MalformedQueryException e) {
				throw new VisitorException(e.getMessage());
			}
		}

		return tupleExpr;
	}

	@Override
	public TupleExpr visit(ASTConstruct node, Object data) throws VisitorException {
		TupleExpr result = (TupleExpr) data;

		// Collect construct triples
		graphPattern = new GraphPattern();
		super.visit(node, null);
		TupleExpr constructExpr = graphPattern.buildTupleExpr();

		// Retrieve TripleRefs from the head
		Map<String, Object> tripleRefs = TripleRefCollector.process(constructExpr);

		// Retrieve all StatementPatterns from the construct expression
		List<StatementPattern> statementPatterns = StatementPatternCollector.process(constructExpr);

		SameTermCollector collector = new SameTermCollector();
		constructExpr.visit(collector);
		if (!collector.getCollectedSameTerms().isEmpty()) {
			// sameTerm filters in construct (this can happen when there's a
			// cyclic path defined, see SES-1685 and SES-2104)

			// we remove the sameTerm filters by simply replacing all mapped
			// variable occurrences
			Set<SameTerm> sameTermConstraints = collector.getCollectedSameTerms();
			statementPatterns = replaceSameTermVars(statementPatterns, sameTermConstraints);
		}

		Set<Var> constructVars = getConstructVars(statementPatterns);

		VarCollector whereClauseVarCollector = new VarCollector();
		result.visit(whereClauseVarCollector);

		// Create BNodeGenerators for all anonymous variables
		// NB: preserve order for a deterministic output
		Map<Var, ExtensionElem> extElemMap = new LinkedHashMap<>();

		for (Var var : constructVars) {
			if (var.isAnonymous() && !extElemMap.containsKey(var)) {
				ValueExpr valueExpr;

				if (var.hasValue()) {
					valueExpr = new ValueConstant(var.getValue());
				} else if (tripleRefs.containsKey(var.getName())) {
					valueExpr = castToValueExpr(tripleRefs.get(var.getName()));
				} else {
					valueExpr = new BNodeGenerator();
				}
				extElemMap.put(var, new ExtensionElem(valueExpr, var.getName()));
			} else if (!whereClauseVarCollector.collectedVars.contains(var)) {
				// non-anon var in construct clause not present in where clause
				if (!extElemMap.containsKey(var)) {
					// assign non-anonymous vars not present in where clause as
					// extension elements. This is necessary to make external
					// binding
					// assingnment possible (see SES-996)
					extElemMap.put(var, new ExtensionElem(var, var.getName()));
				}
			}
		}

		if (!extElemMap.isEmpty()) {
			result = new Extension(result, extElemMap.values());
		}

		// Create a Projection for each StatementPattern in the constructor
		List<ProjectionElemList> projList = new ArrayList<>();

		for (StatementPattern sp : statementPatterns) {
			ProjectionElemList projElemList = new ProjectionElemList();

			projElemList.addElement(new ProjectionElem(sp.getSubjectVar().getName(), "subject"));
			projElemList.addElement(new ProjectionElem(sp.getPredicateVar().getName(), "predicate"));
			projElemList.addElement(new ProjectionElem(sp.getObjectVar().getName(), "object"));
			if (sp.getContextVar() != null) {
				projElemList.addElement(new ProjectionElem(sp.getContextVar().getName(), "context"));
			}

			projList.add(projElemList);
		}

		if (projList.size() == 1) {
			result = new Projection(result, projList.get(0));
		} else if (projList.size() > 1) {
			result = new MultiProjection(result, projList);
		} else {
			// Empty constructor
			result = new EmptySet();
		}

		return new Reduced(result);
	}

	/**
	 * Gets the set of variables that are relevant for the constructor. This method accumulates all subject, predicate
	 * and object variables from the supplied statement patterns, but ignores any context variables.
	 */
	private Set<Var> getConstructVars(Collection<StatementPattern> statementPatterns) {
		Set<Var> vars = new LinkedHashSet<>(statementPatterns.size() * 2);

		for (StatementPattern sp : statementPatterns) {
			vars.add(sp.getSubjectVar());
			vars.add(sp.getPredicateVar());
			vars.add(sp.getObjectVar());
		}

		return vars;
	}

	private List<StatementPattern> replaceSameTermVars(List<StatementPattern> statementPatterns,
			Set<SameTerm> sameTermConstraints) {
		if (sameTermConstraints != null) {
			for (SameTerm st : sameTermConstraints) {
				Var left = (Var) st.getLeftArg();
				Var right = (Var) st.getRightArg();
				for (StatementPattern sp : statementPatterns) {
					Var subj = sp.getSubjectVar();
					Var obj = sp.getObjectVar();

					if (subj.equals(left) || subj.equals(right)) {
						if (obj.equals(left) || obj.equals(right)) {
							sp.setObjectVar(subj);
						}
					}
				}
			}
		}
		return statementPatterns;
	}

	@Override
	public TupleExpr visit(ASTDescribeQuery node, Object data) throws VisitorException {
		TupleExpr tupleExpr = null;

		if (node.getWhereClause() != null) {
			// Start with building the graph pattern
			graphPattern = new GraphPattern();
			node.getWhereClause().jjtAccept(this, null);
			tupleExpr = graphPattern.buildTupleExpr();

			// Apply grouping
			Group group = null;
			ASTGroupClause groupNode = node.getGroupClause();
			if (groupNode != null) {
				tupleExpr = (TupleExpr) groupNode.jjtAccept(this, tupleExpr);
				group = (Group) tupleExpr;
			}

			if (node.getHavingClause() != null) {
				if (group == null) {
					// create implicit group
					group = new Group(tupleExpr);
				}
				// Apply HAVING group filter condition
				tupleExpr = processHavingClause(node.getHavingClause(), tupleExpr, group);
			}

			if (node.getOrderClause() != null) {
				if (group == null) {
					// create implicit group
					group = new Group(tupleExpr);
				}
				// Apply result ordering
				tupleExpr = processOrderClause(node.getOrderClause(), tupleExpr, group);
			}

			// Process limit and offset clauses
			ASTLimit limitNode = node.getLimit();
			long limit = -1;
			if (limitNode != null) {
				limit = (Long) limitNode.jjtAccept(this, null);
			}

			ASTOffset offsetNode = node.getOffset();
			long offset = -1;
			if (offsetNode != null) {
				offset = (Long) offsetNode.jjtAccept(this, null);
			}

			if (offset >= 1 || limit >= 0) {
				tupleExpr = new Slice(tupleExpr, offset, limit);
			}
		}

		// Process describe clause last
		return (TupleExpr) node.getDescribe().jjtAccept(this, tupleExpr);
	}

	@Override
	public TupleExpr visit(ASTDescribe node, Object data) throws VisitorException {

		TupleExpr tupleExpr = (TupleExpr) data;
		if (tupleExpr == null) {
			tupleExpr = new SingletonSet();
		}

		Extension e = new Extension();
		ProjectionElemList projectionElements = new ProjectionElemList();
		for (int i = 0; i < node.jjtGetNumChildren(); i++) {
			ValueExpr resource = (ValueExpr) node.jjtGetChild(i).jjtAccept(this, null);

			if (resource instanceof Var) {
				projectionElements.addElement(new ProjectionElem(((Var) resource).getName()));
			} else {
				String alias = "_describe_" + uniqueIdPrefix + uniqueIdSuffix.incrementAndGet();
				ExtensionElem elem = new ExtensionElem(resource, alias);
				e.addElement(elem);
				projectionElements.addElement(new ProjectionElem(alias));
			}
		}

		if (!e.getElements().isEmpty()) {
			e.setArg(tupleExpr);
			tupleExpr = e;
		}

		Projection p = new Projection(tupleExpr, projectionElements);
		return new DescribeOperator(p);

	}

	@Override
	public TupleExpr visit(ASTAskQuery node, Object data) throws VisitorException {
		graphPattern = new GraphPattern();

		super.visit(node, null);

		TupleExpr tupleExpr = graphPattern.buildTupleExpr();
		tupleExpr = new Slice(tupleExpr, 0, 1);

		// Apply grouping
		Group group = null;
		ASTGroupClause groupNode = node.getGroupClause();
		if (groupNode != null) {
			tupleExpr = (TupleExpr) groupNode.jjtAccept(this, tupleExpr);
			group = (Group) tupleExpr;
		}

		final ASTHavingClause havingClause = node.getHavingClause();
		if (havingClause != null) {
			if (group == null) {
				// create implicit group
				group = new Group(tupleExpr);
			}

			// Apply HAVING group filter condition
			tupleExpr = processHavingClause(havingClause, tupleExpr, group);
		}

		// process bindings clause
		final ASTBindingsClause bindingsClause = node.getBindingsClause();
		if (bindingsClause != null) {
			tupleExpr = new Join((BindingSetAssignment) bindingsClause.jjtAccept(this, null), tupleExpr);
		}

		final ASTOrderClause orderClause = node.getOrderClause();
		if (orderClause != null) {
			if (group == null) {
				// create implicit group
				group = new Group(tupleExpr);
			}
			// Apply result ordering
			tupleExpr = processOrderClause(node.getOrderClause(), tupleExpr, group);
		}

		return tupleExpr;
	}

	protected ValueExpr castToValueExpr(Object node) {
		if (node instanceof ValueExpr) {
			return (ValueExpr) node;
		}
		if (node instanceof TripleRef) {
			TripleRef t = (TripleRef) node;
			return new ValueExprTripleRef(t.getExprVar().getName(), t.getSubjectVar(), t.getPredicateVar(),
					t.getObjectVar());
		}
		throw new IllegalArgumentException("could not cast " + node.getClass().getName() + " to ValueExpr");
	}

	@Override
	public Group visit(ASTGroupClause node, Object data) throws VisitorException {
		TupleExpr tupleExpr = (TupleExpr) data;
		Group g = new Group(tupleExpr);
		int childCount = node.jjtGetNumChildren();

		List<String> groupBindingNames = new ArrayList<>();
		for (int i = 0; i < childCount; i++) {
			String name = (String) node.jjtGetChild(i).jjtAccept(this, g);
			groupBindingNames.add(name);
		}

		g.setGroupBindingNames(groupBindingNames);

		return g;
	}

	@Override
	public String visit(ASTGroupCondition node, Object data) throws VisitorException {
		Group group = (Group) data;
		TupleExpr arg = group.getArg();

		Extension extension;
		if (arg instanceof Extension) {
			extension = (Extension) arg;
		} else {
			extension = new Extension();
		}

		String name;
		ValueExpr ve = castToValueExpr(node.jjtGetChild(0).jjtAccept(this, data));

		boolean aliased = false;
		if (node.jjtGetNumChildren() > 1) {
			aliased = true;
			Var v = (Var) node.jjtGetChild(1).jjtAccept(this, data);
			name = v.getName();
		} else {
			if (ve instanceof Var) {
				name = ((Var) ve).getName();
			} else {
				aliased = true;
				Var v = createAnonVar();
				name = v.getName();
			}
		}

		if (aliased) {
			ExtensionElem elem = new ExtensionElem(ve, name);
			extension.addElement(elem);
		}

		if (extension.getElements().size() > 0 && !(arg instanceof Extension)) {
			extension.setArg(arg);
			group.setArg(extension);
		}

		return name;
	}

	@Override
	public List<OrderElem> visit(ASTOrderClause node, Object data) throws VisitorException {
		int childCount = node.jjtGetNumChildren();
		List<OrderElem> elements = new ArrayList<>(childCount);

		for (int i = 0; i < childCount; i++) {
			elements.add((OrderElem) node.jjtGetChild(i).jjtAccept(this, null));
		}

		return elements;
	}

	@Override
	public OrderElem visit(ASTOrderCondition node, Object data) throws VisitorException {
		ValueExpr valueExpr = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		return new OrderElem(valueExpr, node.isAscending());
	}

	@Override
	public Long visit(ASTLimit node, Object data) throws VisitorException {
		return node.getValue();
	}

	@Override
	public Long visit(ASTOffset node, Object data) throws VisitorException {
		return node.getValue();
	}

	@Override
	public TupleExpr visit(ASTGraphPatternGroup node, Object data) throws VisitorException {
		GraphPattern parentGP = graphPattern;
		graphPattern = new GraphPattern(parentGP);

		boolean optionalPatternInGroup = false;
		for (int i = 0; i < node.jjtGetNumChildren(); i++) {
			if (optionalPatternInGroup) {
				// building the LeftJoin and resetting the graphPattern.
				TupleExpr te = graphPattern.buildTupleExpr();
				graphPattern = new GraphPattern(parentGP);
				graphPattern.addRequiredTE(te);
				optionalPatternInGroup = false;
			}

			Node childNode = node.jjtGetChild(i);
			data = childNode.jjtAccept(this, data);

			if (childNode instanceof ASTOptionalGraphPattern) {
				optionalPatternInGroup = true;
			}
		}

		TupleExpr te = graphPattern.buildTupleExpr();
		if (node.isScopeChange()) {
			((VariableScopeChange) te).setVariableScopeChange(true);
		}
		parentGP.addRequiredTE(te);

		graphPattern = parentGP;

		return te;
	}

	@Override
	public Object visit(ASTServiceGraphPattern node, Object data) throws VisitorException {
		GraphPattern parentGP = graphPattern;

		ValueExpr serviceRef = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);

		graphPattern = new GraphPattern(parentGP);
		node.jjtGetChild(1).jjtAccept(this, null);
		TupleExpr serviceExpr = graphPattern.buildTupleExpr();

		if (serviceExpr instanceof SingletonSet) {
			return null; // do not add an empty service block
		}

		String serviceExpressionString = node.getPatternString();

		parentGP.addRequiredTE(new Service(mapValueExprToVar(serviceRef), serviceExpr, serviceExpressionString,
				node.getPrefixDeclarations(), node.getBaseURI(), node.isSilent()));

		graphPattern = parentGP;

		return null;
	}

	@Override
	public Object visit(ASTOptionalGraphPattern node, Object data) throws VisitorException {
		GraphPattern parentGP = graphPattern;
		graphPattern = new GraphPattern(parentGP);

		super.visit(node, null);

		// remove filter conditions from graph pattern for inclusion as conditions in the OptionalTE
		List<ValueExpr> optionalConstraints = graphPattern.removeAllConstraints();
		TupleExpr optional = graphPattern.buildTupleExpr();

		graphPattern = parentGP;
		graphPattern.addOptionalTE(optional, optionalConstraints);

		return null;
	}

	@Override
	public Object visit(ASTGraphGraphPattern node, Object data) throws VisitorException {
		Var oldContext = graphPattern.getContextVar();
		Scope oldScope = graphPattern.getStatementPatternScope();

		ValueExpr newContext = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);

		graphPattern.setContextVar(mapValueExprToVar(newContext));
		graphPattern.setStatementPatternScope(Scope.NAMED_CONTEXTS);

		node.jjtGetChild(1).jjtAccept(this, null);

		graphPattern.setContextVar(oldContext);
		graphPattern.setStatementPatternScope(oldScope);

		return null;
	}

	@Override
	public Object visit(ASTUnionGraphPattern node, Object data) throws VisitorException {
		GraphPattern parentGP = graphPattern;

		graphPattern = new GraphPattern(parentGP);
		node.jjtGetChild(0).jjtAccept(this, null);
		TupleExpr leftArg = graphPattern.buildTupleExpr();

		graphPattern = new GraphPattern(parentGP);
		node.jjtGetChild(1).jjtAccept(this, null);
		TupleExpr rightArg = graphPattern.buildTupleExpr();

		Union union = new Union(leftArg, rightArg);
		union.setVariableScopeChange(true);
		parentGP.addRequiredTE(union);
		graphPattern = parentGP;

		return null;
	}

	@Override
	public Object visit(ASTMinusGraphPattern node, Object data) throws VisitorException {
		GraphPattern parentGP = graphPattern;

		TupleExpr leftArg = graphPattern.buildTupleExpr();

		graphPattern = new GraphPattern(parentGP);
		node.jjtGetChild(0).jjtAccept(this, null);
		TupleExpr rightArg = graphPattern.buildTupleExpr();

		parentGP = new GraphPattern();
		parentGP.addRequiredTE(new Difference(leftArg, rightArg));
		graphPattern = parentGP;

		return null;
	}

	@Override
	public Object visit(ASTPropertyList propListNode, Object data) throws VisitorException {
		Var subjVar = mapValueExprToVar(data);
		ValueExpr predicate = (ValueExpr) propListNode.getVerb().jjtAccept(this, null);
		@SuppressWarnings("unchecked")
		List<ValueExpr> objectList = (List<ValueExpr>) propListNode.getObjectList().jjtAccept(this, null);

		Var predVar = mapValueExprToVar(predicate);

		for (ValueExpr object : objectList) {
			Var objVar = mapValueExprToVar(object);
			graphPattern.addRequiredSP(subjVar, predVar, objVar);
		}

		ASTPropertyList nextPropList = propListNode.getNextPropertyList();
		if (nextPropList != null) {
			nextPropList.jjtAccept(this, data);
		}

		return graphPattern.buildTupleExpr();
	}

	@Override
	public TupleExpr visit(ASTPathAlternative pathAltNode, Object data) throws VisitorException {
		// a path alternative consists of 1 or more PathSequence elements
		int altCount = pathAltNode.jjtGetNumChildren();

		if (altCount > 1) {
			Union union = new Union();
			Union currentUnion = union;
			for (int i = 0; i < altCount - 1; i++) {
				TupleExpr arg = (TupleExpr) pathAltNode.jjtGetChild(i).jjtAccept(this, data);
				currentUnion.setLeftArg(arg);
				if (i == altCount - 2) { // second-to-last item
					arg = (TupleExpr) pathAltNode.jjtGetChild(i + 1).jjtAccept(this, data);
					currentUnion.setRightArg(arg);
				} else {
					Union newUnion = new Union();
					currentUnion.setRightArg(newUnion);
					currentUnion = newUnion;
				}
			}

			// when using union to execute path expressions, the scope does not not change
			union.setVariableScopeChange(false);
			return union;
		}
		return (TupleExpr) pathAltNode.jjtGetChild(0).jjtAccept(this, data);
	}

	@Override
	public PropertySetElem visit(ASTPathOneInPropertySet node, Object data) throws VisitorException {

		PropertySetElem result = new PropertySetElem();
		result.setInverse(node.isInverse());
		ValueConstant predicate = (ValueConstant) node.jjtGetChild(0).jjtAccept(this, data);
		result.setPredicate(predicate);

		return result;
	}

	private ASTObjectList getObjectList(Node node) {
		if (node == null) {
			return null;
		}
		if (node instanceof ASTPropertyListPath) {
			return ((ASTPropertyListPath) node).getObjectList();
		} else {
			return getObjectList(node.jjtGetParent());
		}
	}

	@Override
	public TupleExpr visit(ASTPathSequence pathSeqNode, Object data) throws VisitorException {
		Var subjVar;
		PathSequenceContext pathSequenceContext;
		Var parentEndVar = null;
		if (data instanceof PathSequenceContext) {
			// nested path sequence
			pathSequenceContext = new PathSequenceContext((PathSequenceContext) data);
			subjVar = pathSequenceContext.startVar;
			parentEndVar = pathSequenceContext.endVar;
		} else {
			pathSequenceContext = new PathSequenceContext();
			subjVar = mapValueExprToVar(data);
		}

		List<ASTPathElt> pathElements = pathSeqNode.getPathElements();
		int pathLength = pathElements.size();

		GraphPattern pathSequencePattern = new GraphPattern(graphPattern);
		pathSequenceContext.scope = pathSequencePattern.getStatementPatternScope();
		pathSequenceContext.contextVar = pathSequencePattern.getContextVar();

		for (int i = 0; i < pathLength; i++) {
			ASTPathElt pathElement = pathElements.get(i);

			pathSequenceContext.startVar = i == 0 ? subjVar : mapValueExprToVar(pathSequenceContext.endVar);
			pathSequenceContext.endVar = createAnonVar();

			TupleExpr elementExpresion = (TupleExpr) pathElement.jjtAccept(this, pathSequenceContext);

			if (i == pathLength - 1) { // last item in sequence
				if (parentEndVar == null) { // not a nested sequence
					// replace last endVar occurrences with the actual (list of) object var(s)

					// We handle this here instead of higher up in the tree visitor because here we have
					// a reference to the "temporary" endVar that needs to be replaced.
					@SuppressWarnings("unchecked")
					List<ValueExpr> objectList = (List<ValueExpr>) getObjectList(pathSeqNode).jjtAccept(this, null);

					for (ValueExpr objectItem : objectList) {
						Var objectVar = mapValueExprToVar(objectItem);
						Var replacement = objectVar;
						if (objectVar.equals(subjVar)) { // corner case for cyclic expressions, see SES-1685
							replacement = createAnonVar();
						}
						TupleExpr copy = elementExpresion.clone();
						copy.visit(new VarReplacer(pathSequenceContext.endVar, replacement));
						if (!replacement.equals(objectVar)) {
							SameTerm condition = new SameTerm(objectVar, replacement);
							pathSequencePattern.addConstraint(condition);
						}
						pathSequencePattern.addRequiredTE(copy);
					}
				} else {
					// nested sequence, replace endVar with parent endVar
					Var replacement = parentEndVar;
					if (parentEndVar.equals(subjVar)) { // corner case for cyclic expressions, see SES-1685
						replacement = createAnonVar();
					}
					TupleExpr copy = elementExpresion.clone();
					copy.visit(new VarReplacer(pathSequenceContext.endVar, replacement));
					if (!replacement.equals(parentEndVar)) {
						SameTerm condition = new SameTerm(parentEndVar, replacement);
						pathSequencePattern.addConstraint(condition);
					}
					pathSequencePattern.addRequiredTE(copy);
				}
			} else {
				pathSequencePattern.addRequiredTE(elementExpresion);
			}
		}
		return pathSequencePattern.buildTupleExpr();
	}

	@Override
	public TupleExpr visit(ASTPathElt pathElement, Object data) throws VisitorException {
		final PathSequenceContext pathSequenceContext = (PathSequenceContext) data;

		final Var startVar = pathElement.isInverse() ? pathSequenceContext.endVar : pathSequenceContext.startVar;
		final Var endVar = pathElement.isInverse() ? pathSequenceContext.startVar : pathSequenceContext.endVar;

		TupleExpr pathElementExpression;
		if (pathElement.isNestedPath()) {
			// child is a new (nested) path alternative
			PathSequenceContext nestedContext = new PathSequenceContext(pathSequenceContext);
			nestedContext.startVar = startVar;
			nestedContext.endVar = endVar;

			pathElementExpression = (TupleExpr) pathElement.jjtGetChild(0).jjtAccept(this, nestedContext);
		} else if (pathElement.isNegatedPropertySet()) {
			List<PropertySetElem> psElems = new ArrayList<>();
			for (Node child : pathElement.jjtGetChildren()) {
				if (child instanceof ASTPathMod) {
					// skip the modifier
					continue;
				}
				psElems.add((PropertySetElem) child.jjtAccept(this, pathSequenceContext));
			}
			pathElementExpression = createTupleExprForNegatedPropertySets(psElems, pathSequenceContext);
		} else {
			final ValueExpr pred = (ValueExpr) pathElement.jjtGetChild(0).jjtAccept(this, pathSequenceContext);
			final Var predVar = mapValueExprToVar(pred);
			pathElementExpression = new StatementPattern(pathSequenceContext.scope, startVar, predVar, endVar,
					pathSequenceContext.contextVar);
		}

		final ASTPathMod pathMod = pathElement.getPathMod();
		if (pathMod != null) {
			long lowerBound = pathMod.getLowerBound();
			long upperBound = pathMod.getUpperBound();

			if (upperBound == Long.MIN_VALUE) {
				upperBound = lowerBound;
			} else if (lowerBound == Long.MIN_VALUE) {
				lowerBound = upperBound;
			}
			pathElementExpression = handlePathModifiers(pathSequenceContext.scope, startVar, pathElementExpression,
					endVar, pathSequenceContext.contextVar,
					lowerBound, upperBound);
		}

		return pathElementExpression;
	}

	private TupleExpr createTupleExprForNegatedPropertySets(List<PropertySetElem> nps,
			PathSequenceContext pathSequenceContext) {
		Var subjVar = pathSequenceContext.startVar;
		Var predVar = createAnonVar();
		Var endVar = pathSequenceContext.endVar;

		ValueExpr filterCondition = null;
		ValueExpr filterConditionInverse = null;

		// build (inverted) filter conditions for each negated path element.
		for (PropertySetElem elem : nps) {
			ValueConstant predicate = elem.getPredicate();

			if (elem.isInverse()) {
				Compare compare = new Compare(predVar, predicate, CompareOp.NE);
				if (filterConditionInverse == null) {
					filterConditionInverse = compare;
				} else {
					filterConditionInverse = new And(compare, filterConditionInverse);
				}
			} else {
				Compare compare = new Compare(predVar, predicate, CompareOp.NE);
				if (filterCondition == null) {
					filterCondition = compare;
				} else {
					filterCondition = new And(compare, filterCondition);
				}
			}
		}

		TupleExpr patternMatch = new StatementPattern(pathSequenceContext.scope, subjVar, predVar,
				endVar,
				pathSequenceContext.contextVar);

		TupleExpr patternMatchInverse = null;

		// build a inverse statement pattern if needed
		if (filterConditionInverse != null) {
			patternMatchInverse = new StatementPattern(pathSequenceContext.scope, endVar, predVar, subjVar,
					pathSequenceContext.contextVar);
		}

		TupleExpr completeMatch = null;

		if (filterCondition != null) {
			completeMatch = new Filter(patternMatch, filterCondition);
		}

		if (patternMatchInverse != null) {
			if (completeMatch == null) {
				completeMatch = new Filter(patternMatchInverse, filterConditionInverse);
			} else {
				completeMatch = new Union(new Filter(patternMatchInverse, filterConditionInverse), completeMatch);
			}
		}

		return completeMatch;
	}

	private TupleExpr handlePathModifiers(Scope scope, Var subjVar, TupleExpr te, Var endVar, Var contextVar,
			long lowerBound, long upperBound) {

		// * and + modifiers
		if (upperBound == Long.MAX_VALUE) {
			// upperbound is abitrary-length
			return new ArbitraryLengthPath(scope, subjVar.clone(), te, endVar.clone(), contextVar,
					lowerBound);
		}

		// ? modifier
		if (lowerBound == 0L && upperBound == 1L) {
			final Union zeroOne = new Union();
			zeroOne.setVariableScopeChange(false);
			zeroOne.setLeftArg(new ZeroLengthPath(scope, subjVar, endVar, contextVar));
			zeroOne.setRightArg(te);
			ProjectionElemList pelist = new ProjectionElemList();
			for (String name : zeroOne.getAssuredBindingNames()) {
				ProjectionElem pe = new ProjectionElem(name);
				pelist.addElement(pe);
			}

			return new Distinct(new Projection(zeroOne, pelist, false));
		}

		// nothing to modify
		return te;
	}

	protected class VarCollector extends AbstractQueryModelVisitor<VisitorException> {

		private final Set<Var> collectedVars = new HashSet<>();

		@Override
		public void meet(Var var) {
			collectedVars.add(var);
		}

		/**
		 * @return Returns the collectedVars.
		 */
		public Set<Var> getCollectedVars() {
			return collectedVars;
		}

	}

	protected class SameTermCollector extends AbstractQueryModelVisitor<VisitorException> {

		private final Set<SameTerm> collectedSameTerms = new HashSet<>();

		@Override
		public void meet(SameTerm st) {
			collectedSameTerms.add(st);
		}

		/**
		 * @return Returns the collected SameTerms.
		 */
		public Set<SameTerm> getCollectedSameTerms() {
			return collectedSameTerms;
		}

	}

	private static class VarReplacer extends AbstractQueryModelVisitor<VisitorException> {

		private final Var toBeReplaced;

		private final Var replacement;

		public VarReplacer(Var toBeReplaced, Var replacement) {
			this.toBeReplaced = toBeReplaced;
			this.replacement = replacement;
		}

		@Override
		public void meet(Var var) {
			if (toBeReplaced.equals(var)) {
				QueryModelNode parent = var.getParentNode();
				Var replacementVar = replacement.clone();
				parent.replaceChildNode(var, replacementVar);
				replacementVar.setParentNode(parent);
			}
		}

		@Override
		public void meet(ProjectionElem node) throws VisitorException {
			if (node.getSourceName().equals(toBeReplaced.getName())) {
				node.setSourceName(replacement.getName());
				node.setTargetName(replacement.getName());
			}
		}
	}

	@Override
	public Object visit(ASTPropertyListPath propListNode, Object data) throws VisitorException {
		Object verbPath = propListNode.getVerb().jjtAccept(this, data);

		if (verbPath instanceof Var) {

			@SuppressWarnings("unchecked")
			List<ValueExpr> objectList = (List<ValueExpr>) propListNode.getObjectList().jjtAccept(this, null);

			Var subjVar = mapValueExprToVar(data);

			Var predVar = mapValueExprToVar(verbPath);
			for (ValueExpr object : objectList) {
				Var objVar = mapValueExprToVar(object);
				graphPattern.addRequiredSP(subjVar, predVar, objVar);
			}
		} else if (verbPath instanceof TupleExpr) {
			graphPattern.addRequiredTE((TupleExpr) verbPath);
		}

		ASTPropertyListPath nextPropList = propListNode.getNextPropertyList();
		if (nextPropList != null) {
			nextPropList.jjtAccept(this, data);
		}

		return null;
	}

	@Override
	public List<ValueExpr> visit(ASTObjectList node, Object data) throws VisitorException {
		int childCount = node.jjtGetNumChildren();
		List<ValueExpr> result = new ArrayList<>(childCount);

		for (int i = 0; i < childCount; i++) {
			Object obj = node.jjtGetChild(i).jjtAccept(this, null);
			if (obj instanceof ValueExpr) {
				result.add((ValueExpr) obj);
			} else if (obj instanceof TripleRef) {
				result.add(((TripleRef) obj).getExprVar());
			}
		}

		return result;
	}

	@Override
	public Var visit(ASTBlankNodePropertyList node, Object data) throws VisitorException {
		Var bnodeVar = createAnonVar();
		super.visit(node, bnodeVar);
		return bnodeVar;
	}

	@Override
	public Var visit(ASTCollection node, Object data) throws VisitorException {
		Var rootListVar = createAnonVar();

		Var listVar = rootListVar;

		int childCount = node.jjtGetNumChildren();
		for (int i = 0; i < childCount; i++) {
			ValueExpr childValue = (ValueExpr) node.jjtGetChild(i).jjtAccept(this, null);

			Var childVar = mapValueExprToVar(childValue);
			graphPattern.addRequiredSP(listVar, TupleExprs.createConstVar(RDF.FIRST), childVar);

			Var nextListVar;
			if (i == childCount - 1) {
				nextListVar = TupleExprs.createConstVar(RDF.NIL);
			} else {
				nextListVar = createAnonVar();
			}

			graphPattern.addRequiredSP(listVar, TupleExprs.createConstVar(RDF.REST), nextListVar);
			listVar = nextListVar;
		}

		return rootListVar;
	}

	@Override
	public Object visit(ASTConstraint node, Object data) throws VisitorException {
		ValueExpr valueExpr = (ValueExpr) super.visit(node, null);
		graphPattern.addConstraint(valueExpr);

		return valueExpr;
	}

	@Override
	public Or visit(ASTOr node, Object data) throws VisitorException {
		ValueExpr leftArg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		ValueExpr rightArg = (ValueExpr) node.jjtGetChild(1).jjtAccept(this, null);
		return new Or(leftArg, rightArg);
	}

	@Override
	public Object visit(ASTAnd node, Object data) throws VisitorException {
		ValueExpr leftArg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		ValueExpr rightArg = (ValueExpr) node.jjtGetChild(1).jjtAccept(this, null);
		return new And(leftArg, rightArg);
	}

	@Override
	public Not visit(ASTNot node, Object data) throws VisitorException {
		ValueExpr arg = (ValueExpr) super.visit(node, null);
		return new Not(arg);
	}

	@Override
	public Coalesce visit(ASTCoalesce node, Object data) throws VisitorException {

		Coalesce coalesce = new Coalesce();
		int noOfArgs = node.jjtGetNumChildren();

		for (int i = 0; i < noOfArgs; i++) {
			ValueExpr arg = (ValueExpr) node.jjtGetChild(i).jjtAccept(this, data);
			coalesce.addArgument(arg);
		}

		return coalesce;
	}

	@Override
	public Compare visit(ASTCompare node, Object data) throws VisitorException {
		ValueExpr leftArg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		ValueExpr rightArg = (ValueExpr) node.jjtGetChild(1).jjtAccept(this, null);
		return new Compare(leftArg, rightArg, node.getOperator());
	}

	@Override
	public FunctionCall visit(ASTSubstr node, Object data) throws VisitorException {
		return createFunctionCall(FN.SUBSTRING.stringValue(), node, 2, 3);
	}

	@Override
	public FunctionCall visit(ASTConcat node, Object data) throws VisitorException {
		return createFunctionCall(FN.CONCAT.stringValue(), node, 1, Integer.MAX_VALUE);
	}

	@Override
	public FunctionCall visit(ASTAbs node, Object data) throws VisitorException {
		return createFunctionCall(FN.NUMERIC_ABS.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTCeil node, Object data) throws VisitorException {
		return createFunctionCall(FN.NUMERIC_CEIL.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTContains node, Object data) throws VisitorException {
		return createFunctionCall(FN.CONTAINS.stringValue(), node, 2, 2);
	}

	@Override
	public FunctionCall visit(ASTFloor node, Object data) throws VisitorException {
		return createFunctionCall(FN.NUMERIC_FLOOR.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTRound node, Object data) throws VisitorException {
		return createFunctionCall(FN.NUMERIC_ROUND.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTRand node, Object data) throws VisitorException {
		return createFunctionCall("RAND", node, 0, 0);
	}

	@Override
	public SameTerm visit(ASTSameTerm node, Object data) throws VisitorException {
		ValueExpr leftArg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		ValueExpr rightArg = (ValueExpr) node.jjtGetChild(1).jjtAccept(this, null);
		return new SameTerm(leftArg, rightArg);
	}

	@Override
	public Sample visit(ASTSample node, Object data) throws VisitorException {
		ValueExpr ve = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, data);

		return new Sample(ve, node.isDistinct());

	}

	@Override
	public MathExpr visit(ASTMath node, Object data) throws VisitorException {
		ValueExpr leftArg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		ValueExpr rightArg = (ValueExpr) node.jjtGetChild(1).jjtAccept(this, null);
		return new MathExpr(leftArg, rightArg, node.getOperator());
	}

	@Override
	public Object visit(ASTFunctionCall node, Object data) throws VisitorException {
		ValueConstant uriNode = (ValueConstant) node.jjtGetChild(0).jjtAccept(this, null);
		IRI functionURI = (IRI) uriNode.getValue();

		FunctionCall functionCall = new FunctionCall(functionURI.stringValue());

		for (int i = 1; i < node.jjtGetNumChildren(); i++) {
			Node argNode = node.jjtGetChild(i);
			functionCall.addArg(castToValueExpr(argNode.jjtAccept(this, null)));
		}

		return functionCall;
	}

	@Override
	public FunctionCall visit(ASTEncodeForURI node, Object data) throws VisitorException {
		return createFunctionCall(FN.ENCODE_FOR_URI.stringValue(), node, 1, 1);
	}

	@Override
	public Object visit(ASTStr node, Object data) throws VisitorException {
		ValueExpr arg = castToValueExpr(node.jjtGetChild(0).jjtAccept(this, null));
		return new Str(arg);
	}

	@Override
	public FunctionCall visit(ASTStrDt node, Object data) throws VisitorException {
		return createFunctionCall("STRDT", node, 2, 2);
	}

	@Override
	public FunctionCall visit(ASTStrStarts node, Object data) throws VisitorException {
		return createFunctionCall(FN.STARTS_WITH.stringValue(), node, 2, 2);
	}

	@Override
	public FunctionCall visit(ASTStrEnds node, Object data) throws VisitorException {
		return createFunctionCall(FN.ENDS_WITH.stringValue(), node, 2, 2);
	}

	@Override
	public FunctionCall visit(ASTStrLen node, Object data) throws VisitorException {
		return createFunctionCall(FN.STRING_LENGTH.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTStrAfter node, Object data) throws VisitorException {
		return createFunctionCall(FN.SUBSTRING_AFTER.stringValue(), node, 2, 2);
	}

	@Override
	public FunctionCall visit(ASTStrBefore node, Object data) throws VisitorException {
		return createFunctionCall(FN.SUBSTRING_BEFORE.stringValue(), node, 2, 2);
	}

	@Override
	public FunctionCall visit(ASTUpperCase node, Object data) throws VisitorException {
		return createFunctionCall(FN.UPPER_CASE.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTLowerCase node, Object data) throws VisitorException {
		return createFunctionCall(FN.LOWER_CASE.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTStrLang node, Object data) throws VisitorException {
		return createFunctionCall("STRLANG", node, 2, 2);
	}

	@Override
	public FunctionCall visit(ASTNow node, Object data) throws VisitorException {
		return createFunctionCall("NOW", node, 0, 0);
	}

	@Override
	public FunctionCall visit(ASTYear node, Object data) throws VisitorException {
		return createFunctionCall(FN.YEAR_FROM_DATETIME.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTMonth node, Object data) throws VisitorException {
		return createFunctionCall(FN.MONTH_FROM_DATETIME.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTDay node, Object data) throws VisitorException {
		return createFunctionCall(FN.DAY_FROM_DATETIME.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTHours node, Object data) throws VisitorException {
		return createFunctionCall(FN.HOURS_FROM_DATETIME.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTMinutes node, Object data) throws VisitorException {
		return createFunctionCall(FN.MINUTES_FROM_DATETIME.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTSeconds node, Object data) throws VisitorException {
		return createFunctionCall(FN.SECONDS_FROM_DATETIME.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTTimezone node, Object data) throws VisitorException {
		return createFunctionCall(FN.TIMEZONE_FROM_DATETIME.stringValue(), node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTTz node, Object data) throws VisitorException {
		return createFunctionCall("TZ", node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTMD5 node, Object data) throws VisitorException {
		return createFunctionCall("MD5", node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTSHA1 node, Object data) throws VisitorException {
		return createFunctionCall("SHA1", node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTSHA224 node, Object data) throws VisitorException {
		throw new VisitorException("hash function SHA-224 is currently not supported");
	}

	@Override
	public FunctionCall visit(ASTSHA256 node, Object data) throws VisitorException {
		return createFunctionCall("SHA256", node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTSHA384 node, Object data) throws VisitorException {
		return createFunctionCall("SHA384", node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTSHA512 node, Object data) throws VisitorException {
		return createFunctionCall("SHA512", node, 1, 1);
	}

	@Override
	public FunctionCall visit(ASTUUID node, Object data) throws VisitorException {
		return createFunctionCall("UUID", node, 0, 0);
	}

	@Override
	public FunctionCall visit(ASTSTRUUID node, Object data) throws VisitorException {
		return createFunctionCall("STRUUID", node, 0, 0);
	}

	@Override
	public IRIFunction visit(ASTIRIFunc node, Object data) throws VisitorException {
		ValueExpr expr = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		IRIFunction fn = new IRIFunction(expr);
		fn.setBaseURI(node.getBaseURI());
		return fn;
	}

	@Override
	public Lang visit(ASTLang node, Object data) throws VisitorException {
		ValueExpr arg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		return new Lang(arg);
	}

	@Override
	public Datatype visit(ASTDatatype node, Object data) throws VisitorException {
		ValueExpr arg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		return new Datatype(arg);
	}

	@Override
	public Object visit(ASTLangMatches node, Object data) throws VisitorException {
		ValueExpr leftArg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		ValueExpr rightArg = (ValueExpr) node.jjtGetChild(1).jjtAccept(this, null);
		return new LangMatches(leftArg, rightArg);
	}

	@Override
	public BindingSetAssignment visit(ASTInlineData node, Object data) throws VisitorException {

		BindingSetAssignment bsa = new BindingSetAssignment();

		List<ASTVar> varNodes = node.jjtGetChildren(ASTVar.class);
		List<Var> vars = new ArrayList<>(varNodes.size());

		// preserve order in query
		Set<String> bindingNames = new LinkedHashSet<>(varNodes.size());
		for (ASTVar varNode : varNodes) {
			Var var = (Var) varNode.jjtAccept(this, data);
			vars.add(var);
			bindingNames.add(var.getName());
		}

		bsa.setBindingNames(bindingNames);

		List<ASTBindingSet> bindingNodes = node.jjtGetChildren(ASTBindingSet.class);
		List<BindingSet> bindingSets = new ArrayList<>(bindingNodes.size());

		for (ASTBindingSet bindingNode : bindingNodes) {
			BindingSet bindingSet = (BindingSet) bindingNode.jjtAccept(this, vars);
			bindingSets.add(bindingSet);
		}

		bsa.setBindingSets(bindingSets);

		graphPattern.addRequiredTE(bsa);
		return bsa;
	}

	@Override
	public BindingSetAssignment visit(ASTBindingsClause node, Object data) throws VisitorException {
		BindingSetAssignment bsa = new BindingSetAssignment();

		List<ASTVar> varNodes = node.jjtGetChildren(ASTVar.class);
		List<Var> vars = new ArrayList<>(varNodes.size());

		// preserve order in query
		Set<String> bindingNames = new LinkedHashSet<>(varNodes.size());
		for (ASTVar varNode : varNodes) {
			Var var = (Var) varNode.jjtAccept(this, data);
			vars.add(var);
			bindingNames.add(var.getName());
		}

		bsa.setBindingNames(bindingNames);

		List<ASTBindingSet> bindingNodes = node.jjtGetChildren(ASTBindingSet.class);
		List<BindingSet> bindingSets = new ArrayList<>(bindingNodes.size());

		for (ASTBindingSet bindingNode : bindingNodes) {
			BindingSet bindingSet = (BindingSet) bindingNode.jjtAccept(this, vars);
			bindingSets.add(bindingSet);
		}

		bsa.setBindingSets(bindingSets);

		return bsa;
	}

	@Override
	public BindingSet visit(ASTBindingSet node, Object data) throws VisitorException {
		@SuppressWarnings("unchecked")
		List<Var> vars = (List<Var>) data;

		List<String> names = new ArrayList<>(vars.size());

		for (Var var : vars) {
			names.add(var.getName());
		}

		int numberOfBindingValues = node.jjtGetNumChildren();

		if (numberOfBindingValues != vars.size()) {
			throw new VisitorException("number of values in bindingset does not match variables in BINDINGS clause");
		}

		Value[] values = new Value[numberOfBindingValues];

		for (int i = 0; i < numberOfBindingValues; i++) {
			ValueExpr ve = (ValueExpr) node.jjtGetChild(i).jjtAccept(this, null);
			if (ve != null) {
				Value v = getValueForExpr(ve);
				values[i] = v;
			}
		}

		return new ListBindingSet(names, values);
	}

	@Override
	public ValueExpr visit(ASTBindingValue node, Object data) throws VisitorException {
		if (node.jjtGetNumChildren() > 0) {
			return (ValueExpr) node.jjtGetChild(0).jjtAccept(this, data);
		} else {
			return null;
		}
	}

	@Override
	public ValueExpr visit(ASTBound node, Object data) throws VisitorException {
		Var var = (Var) node.getArg().jjtAccept(this, null);
		return new Bound(var);
	}

	@Override
	public IsURI visit(ASTIsIRI node, Object data) throws VisitorException {
		ValueExpr arg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		return new IsURI(arg);
	}

	@Override
	public IsBNode visit(ASTIsBlank node, Object data) throws VisitorException {
		ValueExpr arg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		return new IsBNode(arg);
	}

	@Override
	public IsLiteral visit(ASTIsLiteral node, Object data) throws VisitorException {
		ValueExpr arg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		return new IsLiteral(arg);
	}

	@Override
	public IsNumeric visit(ASTIsNumeric node, Object data) throws VisitorException {
		ValueExpr arg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		return new IsNumeric(arg);
	}

	@Override
	public Object visit(ASTBNodeFunc node, Object data) throws VisitorException {

		BNodeGenerator generator = new BNodeGenerator();

		if (node.jjtGetNumChildren() > 0) {
			ValueExpr nodeIdExpr = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
			generator.setNodeIdExpr(nodeIdExpr);
		}

		return generator;
	}

	@Override
	public Object visit(ASTRegexExpression node, Object data) throws VisitorException {
		ValueExpr arg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		ValueExpr pattern = (ValueExpr) node.jjtGetChild(1).jjtAccept(this, null);
		ValueExpr flags = null;
		if (node.jjtGetNumChildren() > 2) {
			flags = (ValueExpr) node.jjtGetChild(2).jjtAccept(this, null);
		}
		return new Regex(arg, pattern, flags);
	}

	@Override
	public FunctionCall visit(ASTReplace node, Object data) throws VisitorException {
		return createFunctionCall(FN.REPLACE.stringValue(), node, 3, 4);
	}

	@Override
	public Exists visit(ASTExistsFunc node, Object data) throws VisitorException {
		GraphPattern parentGP = graphPattern;
		graphPattern = new GraphPattern(parentGP);

		Exists e = new Exists();
		node.jjtGetChild(0).jjtAccept(this, e);

		TupleExpr te = graphPattern.buildTupleExpr();

		e.setSubQuery(te);

		graphPattern = parentGP;

		return e;
	}

	@Override
	public Not visit(ASTNotExistsFunc node, Object data) throws VisitorException {

		GraphPattern parentGP = graphPattern;
		graphPattern = new GraphPattern(parentGP);

		Exists e = new Exists();
		node.jjtGetChild(0).jjtAccept(this, e);

		TupleExpr te = graphPattern.buildTupleExpr();

		e.setSubQuery(te);

		graphPattern = parentGP;

		return new Not(e);
	}

	@Override
	public If visit(ASTIf node, Object data) throws VisitorException {
		If result;

		if (node.jjtGetNumChildren() < 3) {
			throw new VisitorException("IF construction missing required number of arguments");
		}

		ValueExpr condition = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
		ValueExpr resultExpr = (ValueExpr) node.jjtGetChild(1).jjtAccept(this, null);
		ValueExpr alternative = (ValueExpr) node.jjtGetChild(2).jjtAccept(this, null);

		result = new If(condition, resultExpr, alternative);

		return result;
	}

	@Override
	public ValueExpr visit(ASTInfix node, Object data) throws VisitorException {
		ValueExpr leftArg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, data);
		ValueExpr rightArg = (ValueExpr) node.jjtGetChild(1).jjtAccept(this, leftArg);

		return rightArg;
	}

	@Override
	public ValueExpr visit(ASTIn node, Object data) throws VisitorException {
		ValueExpr result;
		ValueExpr leftArg = (ValueExpr) data;
		int listItemCount = node.jjtGetNumChildren();

		if (listItemCount == 0) {
			result = new ValueConstant(BooleanLiteral.FALSE);
		} else if (listItemCount == 1) {
			ValueExpr arg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);
			result = new Compare(leftArg, arg, CompareOp.EQ);
		} else {
			ListMemberOperator listMemberOperator = new ListMemberOperator();
			listMemberOperator.addArgument(leftArg);

			for (int i = 0; i < listItemCount; i++) {
				ValueExpr arg = (ValueExpr) node.jjtGetChild(i).jjtAccept(this, null);
				listMemberOperator.addArgument(arg);
			}
			result = listMemberOperator;
		}

		return result;
	}

	@Override
	public ValueExpr visit(ASTNotIn node, Object data) throws VisitorException {
		ValueExpr result;
		ValueExpr leftArg = (ValueExpr) data;

		int listItemCount = node.jjtGetNumChildren();

		if (listItemCount == 0) {
			result = new ValueConstant(BooleanLiteral.TRUE);
		} else if (listItemCount == 1) {
			ValueExpr arg = (ValueExpr) node.jjtGetChild(0).jjtAccept(this, null);

			result = new Compare(leftArg, arg, CompareOp.NE);
		} else {
			// create a set of conjunctive comparisons to represent the NOT IN
			// operator: X NOT IN (a, b, c) -> X != a && X != b && X != c.
			And and = new And();
			And currentAnd = and;
			for (int i = 0; i < listItemCount - 1; i++) {
				ValueExpr arg = (ValueExpr) node.jjtGetChild(i).jjtAccept(this, null);

				currentAnd.setLeftArg(new Compare(leftArg, arg, CompareOp.NE));

				if (i == listItemCount - 2) { // second-to-last item
					arg = (ValueExpr) node.jjtGetChild(i + 1).jjtAccept(this, null);
					currentAnd.setRightArg(new Compare(leftArg, arg, CompareOp.NE));
				} else {
					And newAnd = new And();
					currentAnd.setRightArg(newAnd);
					currentAnd = newAnd;
				}
			}
			result = and;
		}

		return result;
	}

	@Override
	public Var visit(ASTVar node, Object data) throws VisitorException {
		Var var = new Var(node.getName());
		var.setAnonymous(node.isAnonymous());
		return var;
	}

	@Override
	public ValueConstant visit(ASTIRI node, Object data) throws VisitorException {
		IRI uri;
		try {
			uri = valueFactory.createIRI(node.getValue());
		} catch (IllegalArgumentException e) {
			// invalid URI
			throw new VisitorException(e.getMessage());
		}

		return new ValueConstant(uri);
	}

	@Override
	public Object visit(ASTQName node, Object data) throws VisitorException {
		throw new VisitorException("QNames must be resolved before building the query model");
	}

	@Override
	public Object visit(ASTBind node, Object data) throws VisitorException {
		// bind expression
		Object child0 = node.jjtGetChild(0).jjtAccept(this, data);
		ValueExpr ve = child0 instanceof ValueExpr ? (ValueExpr) child0
				: (child0 instanceof TripleRef) ? ((TripleRef) child0).getExprVar() : null;
		if (ve == null) {
			throw new IllegalArgumentException("Unexpected expressin on bind");
		}

		// name to bind the expression outcome to
		Node aliasNode = node.jjtGetChild(1);
		String alias = ((ASTVar) aliasNode).getName();

		Extension extension = new Extension();
		extension.addElement(new ExtensionElem(ve, alias));

		TupleExpr result;
		TupleExpr arg = graphPattern.buildTupleExpr();

		// check if alias is not previously used.
		if (arg.getBindingNames().contains(alias)) {
			// SES-2314 we need to doublecheck that the reused varname is not
			// just
			// for an anonymous var or a constant.
			VarCollector collector = new VarCollector();
			arg.visit(collector);
			for (Var v : collector.getCollectedVars()) {
				if (alias.equals(v.getName())) {
					if (!v.isConstant() && !v.isAnonymous()) {
						throw new VisitorException(String.format("BIND clause alias '%s' was previously used", alias));
					}
					break;
				}
			}
		}

		if (arg instanceof Filter) {
			result = arg;
			// we need to push down the extension so that filters can operate on
			// the BIND expression.
			while (((Filter) arg).getArg() instanceof Filter) {
				arg = ((Filter) arg).getArg();
			}

			extension.setArg(((Filter) arg).getArg());
			((Filter) arg).setArg(extension);
		} else {
			extension.setArg(arg);
			result = extension;
		}

		GraphPattern replacementGP = new GraphPattern(graphPattern);
		replacementGP.addRequiredTE(result);

		graphPattern = replacementGP;

		return result;
	}

	@Override
	public Object visit(ASTBlankNode node, Object data) throws VisitorException {
		throw new VisitorException("Blank nodes must be replaced with variables before building the query model");
	}

	@Override
	public ValueConstant visit(ASTRDFLiteral node, Object data) throws VisitorException {
		String label = (String) node.getLabel().jjtAccept(this, null);
		String lang = node.getLang();
		ASTIRI datatypeNode = node.getDatatype();

		Literal literal;
		if (datatypeNode != null) {
			IRI datatype;
			try {
				datatype = valueFactory.createIRI(datatypeNode.getValue());
			} catch (IllegalArgumentException e) {
				// invalid URI
				throw new VisitorException(e.getMessage());
			}
			literal = valueFactory.createLiteral(label, datatype);
		} else if (lang != null) {
			literal = valueFactory.createLiteral(label, lang);
		} else {
			literal = valueFactory.createLiteral(label);
		}

		return new ValueConstant(literal);
	}

	@Override
	public ValueConstant visit(ASTNumericLiteral node, Object data) throws VisitorException {
		Literal literal = valueFactory.createLiteral(node.getValue(), node.getDatatype());
		return new ValueConstant(literal);
	}

	@Override
	public ValueConstant visit(ASTTrue node, Object data) throws VisitorException {
		return new ValueConstant(valueFactory.createLiteral(true));
	}

	@Override
	public ValueConstant visit(ASTFalse node, Object data) throws VisitorException {
		return new ValueConstant(valueFactory.createLiteral(false));
	}

	@Override
	public String visit(ASTString node, Object data) throws VisitorException {
		return node.getValue();
	}

	@Override
	public Object visit(ASTCount node, Object data) throws VisitorException {
		ValueExpr ve = null;
		if (node.jjtGetNumChildren() > 0) {
			ve = castToValueExpr(node.jjtGetChild(0).jjtAccept(this, data));
		}
		return new Count(ve, node.isDistinct());
	}

	@Override
	public Object visit(ASTGroupConcat node, Object data) throws VisitorException {
		ValueExpr ve = castToValueExpr(node.jjtGetChild(0).jjtAccept(this, data));

		GroupConcat gc = new GroupConcat(ve, node.isDistinct());

		if (node.jjtGetNumChildren() > 1) {
			ValueExpr separator = (ValueExpr) node.jjtGetChild(1).jjtAccept(this, data);
			gc.setSeparator(separator);
		}

		return gc;
	}

	@Override
	public Object visit(ASTMax node, Object data) throws VisitorException {
		ValueExpr ve = castToValueExpr(node.jjtGetChild(0).jjtAccept(this, data));

		return new Max(ve, node.isDistinct());
	}

	@Override
	public Object visit(ASTMin node, Object data) throws VisitorException {
		ValueExpr ve = castToValueExpr(node.jjtGetChild(0).jjtAccept(this, data));

		return new Min(ve, node.isDistinct());
	}

	@Override
	public Object visit(ASTSum node, Object data) throws VisitorException {
		ValueExpr ve = castToValueExpr(node.jjtGetChild(0).jjtAccept(this, data));

		return new Sum(ve, node.isDistinct());
	}

	@Override
	public Object visit(ASTAvg node, Object data) throws VisitorException {
		ValueExpr ve = castToValueExpr(node.jjtGetChild(0).jjtAccept(this, data));

		return new Avg(ve, node.isDistinct());
	}

	static class AggregateCollector extends AbstractQueryModelVisitor<VisitorException> {

		private final Collection<AggregateOperator> operators = new ArrayList<>();

		public Collection<AggregateOperator> getOperators() {
			return operators;
		}

		@Override
		public void meet(Avg node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(Count node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(GroupConcat node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(Max node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(Min node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(Sample node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(Sum node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		private void meetAggregate(AggregateOperator node) {
			operators.add(node);
		}

	}

	static class AggregateOperatorReplacer extends AbstractQueryModelVisitor<VisitorException> {

		private final Var replacement;

		private final AggregateOperator operator;

		public AggregateOperatorReplacer(AggregateOperator operator, Var replacement) {
			this.operator = operator;
			this.replacement = replacement;
		}

		@Override
		public void meet(Avg node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(Count node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(GroupConcat node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(Max node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(Min node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(Sample node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		@Override
		public void meet(Sum node) throws VisitorException {
			super.meet(node);
			meetAggregate(node);
		}

		private void meetAggregate(AggregateOperator node) {
			if (node.equals(operator)) {
				node.getParentNode().replaceChildNode(node, replacement);
			}
		}
	}

	@Override
	public TupleExpr visit(ASTTripleRef node, Object data) throws VisitorException {
		TripleRef ret = new TripleRef();
		ret.setSubjectVar(mapValueExprToVar(node.getSubj().jjtAccept(this, ret)));
		ret.setPredicateVar(mapValueExprToVar(node.getPred().jjtAccept(this, ret)));
		ret.setObjectVar(mapValueExprToVar(node.getObj().jjtAccept(this, ret)));
		ret.setExprVar(createAnonVar());
		graphPattern.addRequiredTE(ret);

		return ret;
	}

	@Override
	public Object visit(ASTTriplesSameSubjectPath node, Object data) throws VisitorException {
		return super.visit(node, data);
	}

	@Override
	public ValueConstant visit(ASTConstTripleRef node, Object data) throws VisitorException {
		Triple triple;
		Resource subject = (Resource) ((ValueConstant) node.getSubj().jjtAccept(this, data)).getValue();
		IRI predicate = (IRI) ((ValueConstant) node.getPred().jjtAccept(this, data)).getValue();
		Value object = ((ValueConstant) node.getObj().jjtAccept(this, data)).getValue();
		try {
			triple = valueFactory.createTriple(subject, predicate, object);
		} catch (IllegalArgumentException e) {
			// invalid URI
			throw new VisitorException(e.getMessage());
		}

		return new ValueConstant(triple);
	}

	/**
	 * Internal class for keeping track of contextual information relevant for path sequence processing: current scope,
	 * context, start and end variable of the path expression. Passed through to visitor methods via the
	 * <code>data</code> input parameter.
	 *
	 * @author Jeen Broekstra
	 */
	private static class PathSequenceContext {

		public Scope scope;
		public Var contextVar;
		public Var startVar;
		public Var endVar;

		/**
		 * Create a new {@link PathSequenceContext} that is a copy of the supplied <code>pathSequenceContext</code>.
		 *
		 * @param pathSequenceContext the {@link PathSequenceContext} to copy.
		 */
		public PathSequenceContext(PathSequenceContext pathSequenceContext) {
			this.scope = pathSequenceContext.scope;
			this.contextVar = pathSequenceContext.contextVar;
			this.startVar = pathSequenceContext.startVar;
			this.endVar = pathSequenceContext.endVar;
		}

		public PathSequenceContext() {
		}
	}
}
