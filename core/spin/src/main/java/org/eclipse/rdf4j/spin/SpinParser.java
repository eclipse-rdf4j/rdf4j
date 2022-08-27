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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.AFN;
import org.eclipse.rdf4j.model.vocabulary.FN;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.model.vocabulary.SPL;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Avg;
import org.eclipse.rdf4j.query.algebra.BNodeGenerator;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Clear;
import org.eclipse.rdf4j.query.algebra.Coalesce;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Create;
import org.eclipse.rdf4j.query.algebra.Datatype;
import org.eclipse.rdf4j.query.algebra.DeleteData;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Distinct;
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
import org.eclipse.rdf4j.query.algebra.InsertData;
import org.eclipse.rdf4j.query.algebra.IsBNode;
import org.eclipse.rdf4j.query.algebra.IsLiteral;
import org.eclipse.rdf4j.query.algebra.IsNumeric;
import org.eclipse.rdf4j.query.algebra.IsURI;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Lang;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Load;
import org.eclipse.rdf4j.query.algebra.LocalName;
import org.eclipse.rdf4j.query.algebra.MathExpr;
import org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import org.eclipse.rdf4j.query.algebra.Max;
import org.eclipse.rdf4j.query.algebra.Min;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.Sample;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.query.algebra.Sum;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.util.TripleSources;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedDescribeQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.SPARQLQueryRenderer;
import org.eclipse.rdf4j.spin.function.FunctionParser;
import org.eclipse.rdf4j.spin.function.KnownFunctionParser;
import org.eclipse.rdf4j.spin.function.KnownTupleFunctionParser;
import org.eclipse.rdf4j.spin.function.SpinFunctionParser;
import org.eclipse.rdf4j.spin.function.SpinTupleFunctionAsFunctionParser;
import org.eclipse.rdf4j.spin.function.SpinTupleFunctionParser;
import org.eclipse.rdf4j.spin.function.SpinxFunctionParser;
import org.eclipse.rdf4j.spin.function.TupleFunctionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;

public class SpinParser {

	private static final Logger logger = LoggerFactory.getLogger(SpinParser.class);

	private static final Set<IRI> QUERY_TYPES = Sets.newHashSet(SP.SELECT_CLASS, SP.CONSTRUCT_CLASS, SP.ASK_CLASS,
			SP.DESCRIBE_CLASS);

	private static final Set<IRI> UPDATE_TYPES = Sets.newHashSet(SP.MODIFY_CLASS, SP.DELETE_WHERE_CLASS,
			SP.INSERT_DATA_CLASS, SP.DELETE_DATA_CLASS, SP.LOAD_CLASS, SP.CLEAR_CLASS, SP.CREATE_CLASS, SP.DROP_CLASS);

	private static final Set<IRI> COMMAND_TYPES = Sets.union(QUERY_TYPES, UPDATE_TYPES);

	private static final Set<IRI> NON_TEMPLATES = Sets.newHashSet(RDFS.RESOURCE, SP.SYSTEM_CLASS, SP.COMMAND_CLASS,
			SP.QUERY_CLASS, SP.UPDATE_CLASS, SPIN.MODULES_CLASS, SPIN.TEMPLATES_CLASS, SPIN.ASK_TEMPLATES_CLASS,
			SPIN.SELECT_TEMPLATES_CLASS, SPIN.CONSTRUCT_TEMPLATES_CLASS, SPIN.UPDATE_TEMPLATES_CLASS, SPIN.RULE_CLASS);

	private static final Set<IRI> TEMPLATE_TYPES = Sets.newHashSet(SPIN.ASK_TEMPLATE_CLASS, SPIN.SELECT_TEMPLATE_CLASS,
			SPIN.CONSTRUCT_TEMPLATE_CLASS, SPIN.UPDATE_TEMPLATE_CLASS);

	public enum Input {
		TEXT_FIRST(true, true),
		TEXT_ONLY(true, false),
		RDF_FIRST(false, true),
		RDF_ONLY(false, false);

		final boolean textFirst;

		final boolean canFallback;

		Input(boolean textFirst, boolean canFallback) {
			this.textFirst = textFirst;
			this.canFallback = canFallback;
		}
	}

	private final Input input;

	private final Function<IRI, String> wellKnownVars;

	private final Function<IRI, String> wellKnownFunctions;

	private List<FunctionParser> functionParsers;

	private List<TupleFunctionParser> tupleFunctionParsers;

	private boolean strictFunctionChecking = true;

	private final Cache<IRI, Template> templateCache = CacheBuilder.newBuilder().maximumSize(100).build();

	private final Cache<IRI, Map<IRI, Argument>> argumentCache = CacheBuilder.newBuilder().maximumSize(100).build();

	public SpinParser() {
		this(Input.TEXT_FIRST);
	}

	public SpinParser(Input input) {
		this(input, SpinWellKnownVars.INSTANCE::getName, SpinWellKnownFunctions.INSTANCE::getName);
	}

	public SpinParser(Input input, Function<IRI, String> wellKnownVarsMapper,
			Function<IRI, String> wellKnownFuncMapper) {
		this.input = input;
		this.wellKnownVars = wellKnownVarsMapper;
		this.wellKnownFunctions = wellKnownFuncMapper;
		this.functionParsers = Arrays.<FunctionParser>asList(
				new KnownFunctionParser(FunctionRegistry.getInstance(), wellKnownFunctions),
				new SpinTupleFunctionAsFunctionParser(this), new SpinFunctionParser(this),
				new SpinxFunctionParser(this));
		this.tupleFunctionParsers = Arrays.<TupleFunctionParser>asList(
				new KnownTupleFunctionParser(TupleFunctionRegistry.getInstance()), new SpinTupleFunctionParser(this));
	}

	public List<FunctionParser> getFunctionParsers() {
		return functionParsers;
	}

	public void setFunctionParsers(List<FunctionParser> functionParsers) {
		this.functionParsers = functionParsers;
	}

	public List<TupleFunctionParser> getTupleFunctionParsers() {
		return tupleFunctionParsers;
	}

	public void setTupleFunctionParsers(List<TupleFunctionParser> tupleFunctionParsers) {
		this.tupleFunctionParsers = tupleFunctionParsers;
	}

	public boolean isStrictFunctionChecking() {
		return strictFunctionChecking;
	}

	public void setStrictFunctionChecking(boolean strictFunctionChecking) {
		this.strictFunctionChecking = strictFunctionChecking;
	}

	public Map<IRI, RuleProperty> parseRuleProperties(TripleSource store) throws RDF4JException {
		Map<IRI, RuleProperty> rules = new HashMap<>();
		try (CloseableIteration<IRI, QueryEvaluationException> rulePropIter = TripleSources
				.getSubjectURIs(RDFS.SUBPROPERTYOF, SPIN.RULE_PROPERTY, store)) {

			while (rulePropIter.hasNext()) {
				IRI ruleProp = rulePropIter.next();
				RuleProperty ruleProperty = new RuleProperty(ruleProp);

				List<IRI> nextRules = getNextRules(ruleProp, store);
				ruleProperty.setNextRules(nextRules);

				int maxIterCount = getMaxIterationCount(ruleProp, store);
				ruleProperty.setMaxIterationCount(maxIterCount);

				rules.put(ruleProp, ruleProperty);
			}
		}
		return rules;
	}

	private List<IRI> getNextRules(Resource ruleProp, TripleSource store) throws RDF4JException {
		return Iterations.asList((TripleSources.getObjectURIs(ruleProp,
				SPIN.NEXT_RULE_PROPERTY_PROPERTY, store)));
	}

	private int getMaxIterationCount(Resource ruleProp, TripleSource store) throws RDF4JException {
		Value v = TripleSources.singleValue(ruleProp, SPIN.RULE_PROPERTY_MAX_ITERATION_COUNT_PROPERTY, store);
		if (v == null) {
			return -1;
		} else if (v instanceof Literal) {
			try {
				return ((Literal) v).intValue();
			} catch (NumberFormatException e) {
				throw new MalformedSpinException("Value for " + SPIN.RULE_PROPERTY_MAX_ITERATION_COUNT_PROPERTY
						+ " must be of datatype " + XSD.INTEGER + ": " + ruleProp);
			}
		} else {
			throw new MalformedSpinException(
					"Non-literal value for " + SPIN.RULE_PROPERTY_MAX_ITERATION_COUNT_PROPERTY + ": " + ruleProp);
		}
	}

	public boolean isThisUnbound(Resource subj, TripleSource store) throws RDF4JException {
		return TripleSources.booleanValue(subj, SPIN.THIS_UNBOUND_PROPERTY, store);
	}

	public ConstraintViolation parseConstraintViolation(Resource subj, TripleSource store) throws RDF4JException {
		Value labelValue = TripleSources.singleValue(subj, RDFS.LABEL, store);
		Value rootValue = TripleSources.singleValue(subj, SPIN.VIOLATION_ROOT_PROPERTY, store);
		Value pathValue = TripleSources.singleValue(subj, SPIN.VIOLATION_PATH_PROPERTY, store);
		Value valueValue = TripleSources.singleValue(subj, SPIN.VIOLATION_VALUE_PROPERTY, store);
		Value levelValue = TripleSources.singleValue(subj, SPIN.VIOLATION_LEVEL_PROPERTY, store);
		String label = (labelValue instanceof Literal) ? labelValue.stringValue() : null;
		String root = (rootValue instanceof Resource) ? rootValue.stringValue() : null;
		String path = (pathValue != null) ? pathValue.stringValue() : null;
		String value = (valueValue != null) ? valueValue.stringValue() : null;
		ConstraintViolationLevel level = ConstraintViolationLevel.ERROR;
		if (levelValue != null) {
			if (levelValue instanceof IRI) {
				level = ConstraintViolationLevel.valueOf((IRI) levelValue);
			}
			if (level == null) {
				throw new MalformedSpinException(
						"Invalid value " + levelValue + " for " + SPIN.VIOLATION_LEVEL_PROPERTY + ": " + subj);
			}
		}
		return new ConstraintViolation(label, root, path, value, level);
	}

	public ParsedOperation parse(Resource queryResource, TripleSource store) throws RDF4JException {
		return parse(queryResource, SP.COMMAND_CLASS, store);
	}

	public ParsedQuery parseQuery(Resource queryResource, TripleSource store) throws RDF4JException {
		return (ParsedQuery) parse(queryResource, SP.QUERY_CLASS, store);
	}

	public ParsedGraphQuery parseConstructQuery(Resource queryResource, TripleSource store) throws RDF4JException {
		return (ParsedGraphQuery) parse(queryResource, SP.CONSTRUCT_CLASS, store);
	}

	public ParsedTupleQuery parseSelectQuery(Resource queryResource, TripleSource store) throws RDF4JException {
		return (ParsedTupleQuery) parse(queryResource, SP.SELECT_CLASS, store);
	}

	public ParsedBooleanQuery parseAskQuery(Resource queryResource, TripleSource store) throws RDF4JException {
		return (ParsedBooleanQuery) parse(queryResource, SP.ASK_CLASS, store);
	}

	public ParsedDescribeQuery parseDescribeQuery(Resource queryResource, TripleSource store) throws RDF4JException {
		return (ParsedDescribeQuery) parse(queryResource, SP.DESCRIBE_CLASS, store);
	}

	public ParsedUpdate parseUpdate(Resource queryResource, TripleSource store) throws RDF4JException {
		return (ParsedUpdate) parse(queryResource, SP.UPDATE_CLASS, store);
	}

	protected ParsedOperation parse(Resource queryResource, IRI queryClass, TripleSource store) throws RDF4JException {
		Boolean isQueryElseTemplate = null;
		Set<IRI> possibleQueryTypes = new HashSet<>();
		Set<IRI> possibleTemplates = new HashSet<>();
		try (CloseableIteration<IRI, QueryEvaluationException> typeIter = TripleSources
				.getObjectURIs(queryResource, RDF.TYPE, store)) {
			while (typeIter.hasNext()) {
				IRI type = typeIter.next();
				if (isQueryElseTemplate == null && SPIN.TEMPLATES_CLASS.equals(type)) {
					isQueryElseTemplate = Boolean.FALSE;
				} else if ((isQueryElseTemplate == null || isQueryElseTemplate == Boolean.TRUE)
						&& COMMAND_TYPES.contains(type)) {
					isQueryElseTemplate = Boolean.TRUE;
					possibleQueryTypes.add(type);
				} else if ((isQueryElseTemplate == null || isQueryElseTemplate == Boolean.FALSE)
						&& !NON_TEMPLATES.contains(type)) {
					possibleTemplates.add(type);
				}
			}
		}

		ParsedOperation parsedOp;
		if (isQueryElseTemplate == null) {
			throw new MalformedSpinException(String.format("Missing RDF type: %s", queryResource));
		} else if (isQueryElseTemplate == Boolean.TRUE) {
			// command (query or update)
			if (possibleQueryTypes.size() > 1) {
				throw new MalformedSpinException(
						"Incompatible RDF types for command: " + queryResource + " has types " + possibleQueryTypes);
			}

			IRI queryType = possibleQueryTypes.iterator().next();

			if (input.textFirst) {
				parsedOp = parseText(queryResource, queryType, store);
				if (parsedOp == null && input.canFallback) {
					parsedOp = parseRDF(queryResource, queryType, store);
				}
			} else {
				parsedOp = parseRDF(queryResource, queryType, store);
				if (parsedOp == null && input.canFallback) {
					parsedOp = parseText(queryResource, queryType, store);
				}
			}

			if (parsedOp == null) {
				throw new MalformedSpinException(String.format("Command is not parsable: %s", queryResource));
			}
		} else {
			// template
			Set<IRI> abstractTemplates;
			if (possibleTemplates.size() > 1) {
				abstractTemplates = new HashSet<>();
				for (Iterator<IRI> iter = possibleTemplates.iterator(); iter.hasNext();) {
					IRI t = iter.next();
					boolean isAbstract = TripleSources.booleanValue(t, SPIN.ABSTRACT_PROPERTY, store);
					if (isAbstract) {
						abstractTemplates.add(t);
						iter.remove();
					}
				}
			} else {
				abstractTemplates = Collections.emptySet();
			}

			if (possibleTemplates.isEmpty()) {
				throw new MalformedSpinException(String.format("Template missing RDF type: %s", queryResource));
			}
			if (possibleTemplates.size() > 1) {
				throw new MalformedSpinException("Template has unexpected RDF types: " + queryResource
						+ " has non-abstract types " + possibleTemplates);
			}

			IRI templateResource = (IRI) possibleTemplates.iterator().next();
			Template tmpl = getTemplate(templateResource, queryClass, abstractTemplates, store);
			Map<IRI, Value> argValues = new HashMap<>(2 * tmpl.getArguments().size());
			for (Argument arg : tmpl.getArguments()) {
				IRI argPred = (IRI) arg.getPredicate();
				Value argValue = TripleSources.singleValue(queryResource, argPred, store);
				argValues.put(argPred, argValue);
			}
			parsedOp = tmpl.call(argValues);
		}

		return parsedOp;
	}

	private Template getTemplate(final IRI tmplUri, final IRI queryType, final Set<IRI> abstractTmpls,
			final TripleSource store) throws RDF4JException {
		try {
			return templateCache.get(tmplUri, () -> parseTemplateInternal(tmplUri, queryType, abstractTmpls, store));
		} catch (ExecutionException e) {
			if (e.getCause() instanceof RDF4JException) {
				throw (RDF4JException) e.getCause();
			} else if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	private Template parseTemplateInternal(IRI tmplUri, IRI queryType, Set<IRI> abstractTmpls, TripleSource store)
			throws RDF4JException {
		Set<IRI> possibleTmplTypes;
		try (Stream<IRI> stream = TripleSources.getObjectURIs(tmplUri, RDF.TYPE, store).stream()) {
			possibleTmplTypes = stream
					.filter(TEMPLATE_TYPES::contains)
					.collect(Collectors.toSet());
		}

		if (possibleTmplTypes.isEmpty()) {
			throw new MalformedSpinException(String.format("Template missing RDF type: %s", tmplUri));
		}
		if (possibleTmplTypes.size() > 1) {
			throw new MalformedSpinException(
					"Incompatible RDF types for template: " + tmplUri + " has types " + possibleTmplTypes);
		}

		IRI tmplType = possibleTmplTypes.iterator().next();

		Set<IRI> compatibleTmplTypes;
		if (SP.QUERY_CLASS.equals(queryType)) {
			compatibleTmplTypes = Sets.newHashSet(SPIN.ASK_TEMPLATE_CLASS, SPIN.SELECT_TEMPLATE_CLASS,
					SPIN.CONSTRUCT_TEMPLATE_CLASS);
		} else if (SP.UPDATE_CLASS.equals(queryType) || UPDATE_TYPES.contains(queryType)) {
			compatibleTmplTypes = Collections.singleton(SPIN.UPDATE_TEMPLATE_CLASS);
		} else if (SP.ASK_CLASS.equals(queryType)) {
			compatibleTmplTypes = Collections.singleton(SPIN.ASK_TEMPLATE_CLASS);
		} else if (SP.SELECT_CLASS.equals(queryType)) {
			compatibleTmplTypes = Collections.singleton(SPIN.SELECT_TEMPLATE_CLASS);
		} else if (SP.CONSTRUCT_CLASS.equals(queryType)) {
			compatibleTmplTypes = Collections.singleton(SPIN.CONSTRUCT_TEMPLATE_CLASS);
		} else {
			compatibleTmplTypes = TEMPLATE_TYPES;
		}
		if (!compatibleTmplTypes.contains(tmplType)) {
			throw new MalformedSpinException(
					"Template type " + tmplType + " is incompatible with command type " + queryType);
		}

		Template tmpl = new Template(tmplUri);

		Value body = TripleSources.singleValue(tmplUri, SPIN.BODY_PROPERTY, store);
		if (!(body instanceof Resource)) {
			throw new MalformedSpinException(String.format("Template body is not a resource: %s", body));
		}
		ParsedOperation op = parse((Resource) body, queryType, store);
		tmpl.setParsedOperation(op);

		Map<IRI, Argument> templateArgs = parseTemplateArguments(tmplUri, abstractTmpls, store);

		List<IRI> orderedArgs = orderArguments(templateArgs.keySet());
		for (IRI IRI : orderedArgs) {
			Argument arg = templateArgs.get(IRI);
			tmpl.addArgument(arg);
		}

		return tmpl;
	}

	private Map<IRI, Argument> parseTemplateArguments(IRI tmplUri, Set<IRI> abstractTmpls, TripleSource store)
			throws RDF4JException {
		Map<IRI, Argument> args = new HashMap<>();
		for (IRI abstractTmpl : abstractTmpls) {
			parseArguments(abstractTmpl, store, args);
		}
		parseArguments(tmplUri, store, args);
		return args;
	}

	public org.eclipse.rdf4j.query.algebra.evaluation.function.Function parseFunction(IRI funcUri, TripleSource store)
			throws RDF4JException {
		for (FunctionParser functionParser : functionParsers) {
			org.eclipse.rdf4j.query.algebra.evaluation.function.Function function = functionParser.parse(funcUri,
					store);
			if (function != null) {
				return function;
			}
		}
		logger.warn("No FunctionParser for function: {}", funcUri);
		throw new MalformedSpinException(String.format("No FunctionParser for function: %s", funcUri));
	}

	public TupleFunction parseMagicProperty(IRI propUri, TripleSource store) throws RDF4JException {
		for (TupleFunctionParser tupleFunctionParser : tupleFunctionParsers) {
			TupleFunction tupleFunction = tupleFunctionParser.parse(propUri, store);
			if (tupleFunction != null) {
				return tupleFunction;
			}
		}
		logger.warn("No TupleFunctionParser for magic property: {}", propUri);
		throw new MalformedSpinException(String.format("No TupleFunctionParser for magic property: %s", propUri));
	}

	public Map<IRI, Argument> parseArguments(final IRI moduleUri, final TripleSource store) throws RDF4JException {
		try {
			return argumentCache.get(moduleUri, () -> {
				Map<IRI, Argument> args = new HashMap<>();
				parseArguments(moduleUri, store, args);
				return Collections.unmodifiableMap(args);
			});
		} catch (ExecutionException e) {
			if (e.getCause() instanceof RDF4JException) {
				throw (RDF4JException) e.getCause();
			} else if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	private void parseArguments(IRI moduleUri, TripleSource store, Map<IRI, Argument> args) throws RDF4JException {
		try (CloseableIteration<Resource, QueryEvaluationException> argIter = TripleSources
				.getObjectResources(moduleUri, SPIN.CONSTRAINT_PROPERTY, store)) {

			while (argIter.hasNext()) {
				Resource possibleArg = argIter.next();
				Statement argTmpl = TripleSources.single(possibleArg, RDF.TYPE, SPL.ARGUMENT_TEMPLATE, store);
				if (argTmpl != null) {
					Value argPred = TripleSources.singleValue(possibleArg, SPL.PREDICATE_PROPERTY, store);
					Value valueType = TripleSources.singleValue(possibleArg, SPL.VALUE_TYPE_PROPERTY, store);
					boolean optional = TripleSources.booleanValue(possibleArg, SPL.OPTIONAL_PROPERTY, store);
					Value defaultValue = TripleSources.singleValue(possibleArg, SPL.DEFAULT_VALUE_PROPERTY, store);
					IRI argUri = (IRI) argPred;
					args.put(argUri, new Argument(argUri, (IRI) valueType, optional, defaultValue));
				}
			}

		}
	}

	private ParsedOperation parseText(Resource queryResource, IRI queryType, TripleSource store) throws RDF4JException {
		Value text = TripleSources.singleValue(queryResource, SP.TEXT_PROPERTY, store);
		if (text != null) {
			if (QUERY_TYPES.contains(queryType)) {
				return QueryParserUtil.parseQuery(QueryLanguage.SPARQL, text.stringValue(), null);
			} else if (UPDATE_TYPES.contains(queryType)) {
				return QueryParserUtil.parseUpdate(QueryLanguage.SPARQL, text.stringValue(), null);
			} else {
				throw new MalformedSpinException(String.format("Unrecognised command type: %s", queryType));
			}
		} else {
			return null;
		}
	}

	private ParsedOperation parseRDF(Resource queryResource, IRI queryType, TripleSource store) throws RDF4JException {
		if (SP.CONSTRUCT_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitConstruct(queryResource);
			TupleExpr tupleExpr = makeQueryRootIfNeeded(visitor.getTupleExpr());
			return new ParsedGraphQuery(tupleExpr);
		} else if (SP.SELECT_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitSelect(queryResource);
			return new ParsedTupleQuery(makeQueryRootIfNeeded(visitor.getTupleExpr()));
		} else if (SP.ASK_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitAsk(queryResource);
			return new ParsedBooleanQuery(makeQueryRootIfNeeded(visitor.getTupleExpr()));
		} else if (SP.DESCRIBE_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitDescribe(queryResource);
			return new ParsedDescribeQuery(makeQueryRootIfNeeded(visitor.getTupleExpr()));
		} else if (SP.MODIFY_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitModify(queryResource);
			ParsedUpdate parsedUpdate = new ParsedUpdate();
			parsedUpdate.addUpdateExpr(visitor.getUpdateExpr());
			return parsedUpdate;
		} else if (SP.DELETE_WHERE_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitDeleteWhere(queryResource);
			ParsedUpdate parsedUpdate = new ParsedUpdate();
			parsedUpdate.addUpdateExpr(visitor.getUpdateExpr());
			return parsedUpdate;
		} else if (SP.INSERT_DATA_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitInsertData(queryResource);
			ParsedUpdate parsedUpdate = new ParsedUpdate();
			parsedUpdate.addUpdateExpr(visitor.getUpdateExpr());
			return parsedUpdate;
		} else if (SP.DELETE_DATA_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitDeleteData(queryResource);
			ParsedUpdate parsedUpdate = new ParsedUpdate();
			parsedUpdate.addUpdateExpr(visitor.getUpdateExpr());
			return parsedUpdate;
		} else if (SP.LOAD_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitLoad(queryResource);
			ParsedUpdate parsedUpdate = new ParsedUpdate();
			parsedUpdate.addUpdateExpr(visitor.getUpdateExpr());
			return parsedUpdate;
		} else if (SP.CLEAR_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitClear(queryResource);
			ParsedUpdate parsedUpdate = new ParsedUpdate();
			parsedUpdate.addUpdateExpr(visitor.getUpdateExpr());
			return parsedUpdate;
		} else if (SP.CREATE_CLASS.equals(queryType)) {
			SpinVisitor visitor = new SpinVisitor(store);
			visitor.visitCreate(queryResource);
			ParsedUpdate parsedUpdate = new ParsedUpdate();
			parsedUpdate.addUpdateExpr(visitor.getUpdateExpr());
			return parsedUpdate;
		} else {
			throw new MalformedSpinException(String.format("Unrecognised command type: %s", queryType));
		}
	}

	private TupleExpr makeQueryRootIfNeeded(TupleExpr tupleExpr) {
		if (!(tupleExpr instanceof QueryRoot)) {
			return new QueryRoot(tupleExpr);
		} else {
			return tupleExpr;
		}
	}

	public ValueExpr parseExpression(Value expr, TripleSource store) throws RDF4JException {
		SpinVisitor visitor = new SpinVisitor(store);
		return visitor.visitExpression(expr);
	}

	/**
	 * Resets/clears any cached information about the given URIs.
	 *
	 * @param uris if none are specified all cached information is cleared.
	 */
	public void reset(IRI... uris) {
		if (uris != null && uris.length > 0) {
			Iterable<?> uriList = Arrays.asList(uris);
			templateCache.invalidateAll(uriList);
			argumentCache.invalidateAll(uriList);
		} else {
			templateCache.invalidateAll();
			argumentCache.invalidateAll();
		}
	}

	public static List<IRI> orderArguments(Set<IRI> args) {
		SortedSet<IRI> sortedArgs = new TreeSet<>(
				(IRI uri1, IRI uri2) -> uri1.getLocalName().compareTo(uri2.getLocalName()));
		sortedArgs.addAll(args);

		int numArgs = sortedArgs.size();
		List<IRI> orderedArgs = new ArrayList<>(numArgs);
		for (int i = 0; i < numArgs; i++) {
			IRI arg = toArgProperty(i);
			if (!sortedArgs.remove(arg)) {
				arg = sortedArgs.first();
				sortedArgs.remove(arg);
			}
			orderedArgs.add(arg);
		}
		return orderedArgs;
	}

	private static IRI toArgProperty(int i) {
		switch (i) {
		case 1:
			return SP.ARG1_PROPERTY;
		case 2:
			return SP.ARG2_PROPERTY;
		case 3:
			return SP.ARG3_PROPERTY;
		case 4:
			return SP.ARG4_PROPERTY;
		case 5:
			return SP.ARG5_PROPERTY;
		default:
			return SimpleValueFactory.getInstance().createIRI(SP.NAMESPACE, "arg" + i);
		}
	}

	private class SpinVisitor {

		final TripleSource store;

		TupleExpr tupleRoot;

		TupleExpr tupleNode;

		UpdateExpr updateRoot;

		Var namedGraph;

		Map<String, ProjectionElem> projElems;

		Group group;

		Map<Resource, String> vars = new HashMap<>();

		Collection<AggregateOperator> aggregates = new ArrayList<>();

		SpinVisitor(TripleSource store) {
			this.store = store;
		}

		public TupleExpr getTupleExpr() {
			return tupleRoot;
		}

		public UpdateExpr getUpdateExpr() {
			return updateRoot;
		}

		public void visitConstruct(Resource construct) throws RDF4JException {
			Value templates = TripleSources.singleValue(construct, SP.TEMPLATES_PROPERTY, store);
			if (!(templates instanceof Resource)) {
				throw new MalformedSpinException(String.format("Value of %s is not a resource", SP.TEMPLATES_PROPERTY));
			}

			projElems = new LinkedHashMap<>();
			UnaryTupleOperator projection = visitTemplates((Resource) templates);
			TupleExpr whereExpr = visitWhere(construct);
			projection.setArg(whereExpr);
			addSourceExpressions(projection, projElems.values());
		}

		public void visitDescribe(Resource describe) throws RDF4JException {
			Value resultNodes = TripleSources.singleValue(describe, SP.RESULT_NODES_PROPERTY, store);
			if (!(resultNodes instanceof Resource)) {
				throw new MalformedSpinException(
						String.format("Value of %s is not a resource", SP.RESULT_NODES_PROPERTY));
			}

			projElems = new LinkedHashMap<>();
			Projection projection = visitResultNodes((Resource) resultNodes);
			TupleExpr whereExpr = visitWhere(describe);
			projection.setArg(whereExpr);
			addSourceExpressions(projection, projElems.values());
		}

		public void visitSelect(Resource select) throws RDF4JException {
			Value resultVars = TripleSources.singleValue(select, SP.RESULT_VARIABLES_PROPERTY, store);
			if (!(resultVars instanceof Resource)) {
				throw new MalformedSpinException(
						String.format("Value of %s is not a resource", SP.RESULT_VARIABLES_PROPERTY));
			}

			Map<String, ProjectionElem> oldProjElems = projElems;
			projElems = new LinkedHashMap<>();
			Projection projection = visitResultVariables((Resource) resultVars, oldProjElems);
			TupleExpr whereExpr = visitWhere(select);
			projection.setArg(whereExpr);

			Value groupBy = TripleSources.singleValue(select, SP.GROUP_BY_PROPERTY, store);
			if (groupBy instanceof Resource) {
				visitGroupBy((Resource) groupBy);
			}
			if (group != null) {
				group.setArg(projection.getArg());
				projection.setArg(group);
			}

			Value having = TripleSources.singleValue(select, SP.HAVING_PROPERTY, store);
			if (having instanceof Resource) {
				TupleExpr havingExpr = visitHaving((Resource) having);
				projection.setArg(havingExpr);
			}

			addSourceExpressions(projection, projElems.values());
			projElems = oldProjElems;

			Value orderby = TripleSources.singleValue(select, SP.ORDER_BY_PROPERTY, store);
			if (orderby instanceof Resource) {
				Order order = visitOrderBy((Resource) orderby);
				order.setArg(projection.getArg());
				projection.setArg(order);
			}

			boolean distinct = TripleSources.booleanValue(select, SP.DISTINCT_PROPERTY, store);
			if (distinct) {
				tupleRoot = new Distinct(tupleRoot);
			}

			long offset = -1L;
			Value offsetValue = TripleSources.singleValue(select, SP.OFFSET_PROPERTY, store);
			if (offsetValue instanceof Literal) {
				offset = ((Literal) offsetValue).longValue();
			}
			long limit = -1L;
			Value limitValue = TripleSources.singleValue(select, SP.LIMIT_PROPERTY, store);
			if (limitValue instanceof Literal) {
				limit = ((Literal) limitValue).longValue();
			}
			if (offset > 0L || limit >= 0L) {
				Slice slice = new Slice(tupleRoot);
				if (offset > 0L) {
					slice.setOffset(offset);
				}
				if (limit >= 0L) {
					slice.setLimit(limit);
				}
				tupleRoot = slice;
			}
		}

		public void visitAsk(Resource ask) throws RDF4JException {
			TupleExpr whereExpr = visitWhere(ask);
			tupleRoot = new Slice(whereExpr, 0, 1);
		}

		private void addSourceExpressions(UnaryTupleOperator op, Collection<ProjectionElem> elems) {
			Extension ext = null;
			for (ProjectionElem projElem : elems) {
				ExtensionElem extElem = projElem.getSourceExpression();
				if (extElem != null) {
					if (ext == null) {
						ext = new Extension(op.getArg());
						op.setArg(ext);
					}
					ext.addElement(extElem);
				}
			}
		}

		private UnaryTupleOperator visitTemplates(Resource templates) throws RDF4JException {
			List<ProjectionElemList> projElemLists = new ArrayList<>();
			Iteration<Resource, QueryEvaluationException> iter = TripleSources.listResources(templates,
					store);
			while (iter.hasNext()) {
				Resource r = iter.next();
				ProjectionElemList projElems = visitTemplate(r);
				projElemLists.add(projElems);
			}

			UnaryTupleOperator expr;
			if (projElemLists.size() > 1) {
				MultiProjection proj = new MultiProjection();
				proj.setProjections(projElemLists);
				expr = proj;
			} else {
				Projection proj = new Projection();
				proj.setProjectionElemList(projElemLists.get(0));
				expr = proj;
			}

			Reduced reduced = new Reduced();
			reduced.setArg(expr);
			tupleRoot = reduced;
			return expr;
		}

		private ProjectionElemList visitTemplate(Resource r) throws RDF4JException {
			ProjectionElemList projElems = new ProjectionElemList();
			Value subj = TripleSources.singleValue(r, SP.SUBJECT_PROPERTY, store);
			projElems.addElement(createProjectionElem(subj, "subject", null));
			Value pred = TripleSources.singleValue(r, SP.PREDICATE_PROPERTY, store);
			projElems.addElement(createProjectionElem(pred, "predicate", null));
			Value obj = TripleSources.singleValue(r, SP.OBJECT_PROPERTY, store);
			projElems.addElement(createProjectionElem(obj, "object", null));
			return projElems;
		}

		private Projection visitResultNodes(Resource resultNodes) throws RDF4JException {
			ProjectionElemList projElemList = new ProjectionElemList();
			Iteration<Resource, QueryEvaluationException> iter = TripleSources.listResources(resultNodes,
					store);
			while (iter.hasNext()) {
				Resource r = iter.next();
				ProjectionElem projElem = visitResultNode(r);
				projElemList.addElement(projElem);
			}

			Projection proj = new Projection();
			proj.setProjectionElemList(projElemList);

			tupleRoot = new DescribeOperator(proj);
			return proj;
		}

		private ProjectionElem visitResultNode(Resource r) throws RDF4JException {
			return createProjectionElem(r, null, null);
		}

		private Projection visitResultVariables(Resource resultVars, Map<String, ProjectionElem> previousProjElems)
				throws RDF4JException {
			ProjectionElemList projElemList = new ProjectionElemList();
			Iteration<Resource, QueryEvaluationException> iter = TripleSources.listResources(resultVars,
					store);
			while (iter.hasNext()) {
				Resource r = iter.next();
				ProjectionElem projElem = visitResultVariable(r, previousProjElems);
				projElemList.addElement(projElem);
			}

			Projection proj = new Projection();
			proj.setProjectionElemList(projElemList);

			tupleRoot = proj;
			return proj;
		}

		private ProjectionElem visitResultVariable(Resource r, Map<String, ProjectionElem> previousProjElems)
				throws RDF4JException {
			return createProjectionElem(r, null, previousProjElems);
		}

		private void visitGroupBy(Resource groupby) throws RDF4JException {
			if (group == null) {
				group = new Group();
			}
			Iteration<Resource, QueryEvaluationException> iter = TripleSources.listResources(groupby, store);
			while (iter.hasNext()) {
				Resource r = iter.next();
				ValueExpr groupByExpr = visitExpression(r);
				if (!(groupByExpr instanceof Var)) {
					// TODO
					// have to create an intermediate Var/Extension for the
					// expression
					throw new UnsupportedOperationException("TODO!");
				}
				group.addGroupBindingName(((Var) groupByExpr).getName());
			}
		}

		private TupleExpr visitHaving(Resource having) throws RDF4JException {
			UnaryTupleOperator op = (UnaryTupleOperator) group.getParentNode();
			op.setArg(new Extension(group));
			Iteration<Resource, QueryEvaluationException> iter = TripleSources.listResources(having, store);
			while (iter.hasNext()) {
				Resource r = iter.next();
				ValueExpr havingExpr = visitExpression(r);
				Filter filter = new Filter(op.getArg(), havingExpr);
				op.setArg(filter);
				op = filter;
			}
			return op;
		}

		private Order visitOrderBy(Resource orderby) throws RDF4JException {
			Order order = new Order();
			Iteration<Resource, QueryEvaluationException> iter = TripleSources.listResources(orderby, store);
			while (iter.hasNext()) {
				Resource r = iter.next();
				OrderElem orderElem = visitOrderByCondition(r);
				order.addElement(orderElem);
			}
			return order;
		}

		private OrderElem visitOrderByCondition(Resource r) throws RDF4JException {
			Value expr = TripleSources.singleValue(r, SP.EXPRESSION_PROPERTY, store);
			ValueExpr valueExpr = visitExpression(expr);
			Statement descStmt = TripleSources.single(r, RDF.TYPE, SP.DESC_CLASS, store);
			boolean asc = (descStmt == null);
			return new OrderElem(valueExpr, asc);
		}

		private ProjectionElem createProjectionElem(Value v, String projName,
				Map<String, ProjectionElem> previousProjElems) throws RDF4JException {
			String varName;
			ValueExpr valueExpr;
			Collection<AggregateOperator> oldAggregates = aggregates;
			aggregates = Collections.emptyList();
			if (v instanceof Literal) {
				// literal
				if (projName == null) {
					throw new MalformedSpinException(String.format("Expected a projection var: %s", v));
				}
				varName = TupleExprs.getConstVarName(v);
				valueExpr = new ValueConstant(v);
			} else {
				varName = getVarName((Resource) v);
				if (varName != null) {
					// var
					Value expr = TripleSources.singleValue((Resource) v, SP.EXPRESSION_PROPERTY, store);
					if (expr != null) {
						// AS
						aggregates = new ArrayList<>();
						valueExpr = visitExpression(expr);
					} else {
						valueExpr = new Var(varName);
					}
				} else {
					// resource
					if (projName == null) {
						throw new MalformedSpinException(String.format("Expected a projection var: %s", v));
					}
					varName = TupleExprs.getConstVarName(v);
					valueExpr = new ValueConstant(v);
				}
			}

			ProjectionElem projElem = new ProjectionElem(varName, projName);
			if (!(valueExpr instanceof Var && ((Var) valueExpr).getName().equals(varName))) {
				projElem.setSourceExpression(new ExtensionElem(valueExpr, varName));
			}
			if (!aggregates.isEmpty()) {
				projElem.setAggregateOperatorInExpression(true);
				if (group == null) {
					group = new Group();
				}
				for (AggregateOperator op : aggregates) {
					group.addGroupElement(new GroupElem(projElem.getProjectionAlias().orElse(varName), op));
				}
			}
			aggregates = oldAggregates;
			if (projElems != null) {
				projElems.put(varName, projElem);
			}
			if (previousProjElems != null) {
				previousProjElems.remove(projName);
			}
			return projElem;
		}

		public void visitModify(Resource query) throws RDF4JException {
			Value with = TripleSources.singleValue(query, SP.WITH_PROPERTY, store);
			if (with != null) {
				namedGraph = TupleExprs.createConstVar(with);
			}

			SingletonSet stub = new SingletonSet();
			tupleRoot = new QueryRoot(stub);
			tupleNode = stub;
			TupleExpr deleteExpr;
			Value delete = TripleSources.singleValue(query, SP.DELETE_PATTERN_PROPERTY, store);
			if (delete != null) {
				visitDelete((Resource) delete);
				deleteExpr = tupleNode;
				deleteExpr.setParentNode(null);
			} else {
				deleteExpr = null;
			}

			tupleRoot = new QueryRoot(stub);
			tupleNode = stub;
			TupleExpr insertExpr;
			Value insert = TripleSources.singleValue(query, SP.INSERT_PATTERN_PROPERTY, store);
			if (insert != null) {
				visitInsert((Resource) insert);
				insertExpr = tupleNode;
				insertExpr.setParentNode(null);
			} else {
				insertExpr = null;
			}

			TupleExpr whereExpr;
			Value where = TripleSources.singleValue(query, SP.WHERE_PROPERTY, store);
			if (where != null) {
				whereExpr = visitGroupGraphPattern((Resource) where);
			} else {
				whereExpr = null;
			}

			updateRoot = new Modify(deleteExpr, insertExpr, whereExpr);
		}

		public void visitDeleteWhere(Resource query) throws RDF4JException {
			TupleExpr whereExpr = visitWhere(query);
			updateRoot = new Modify(whereExpr, null, whereExpr.clone());
		}

		public void visitInsertData(Resource query) throws RDF4JException {
			SingletonSet stub = new SingletonSet();
			tupleRoot = new QueryRoot(stub);
			tupleNode = stub;
			TupleExpr insertExpr;
			Value insert = TripleSources.singleValue(query, SP.DATA_PROPERTY, store);
			if (!(insert instanceof Resource)) {
				throw new MalformedSpinException(String.format("Value of %s is not a resource", SP.DATA_PROPERTY));
			}
			visitInsert((Resource) insert);
			insertExpr = tupleNode;
			insertExpr.setParentNode(null);

			DataVisitor visitor = new DataVisitor();
			insertExpr.visit(visitor);
			updateRoot = new InsertData(visitor.getData());
		}

		public void visitDeleteData(Resource query) throws RDF4JException {
			SingletonSet stub = new SingletonSet();
			tupleRoot = new QueryRoot(stub);
			tupleNode = stub;
			TupleExpr deleteExpr;
			Value delete = TripleSources.singleValue(query, SP.DATA_PROPERTY, store);
			if (!(delete instanceof Resource)) {
				throw new MalformedSpinException(String.format("Value of %s is not a resource", SP.DATA_PROPERTY));
			}
			visitDelete((Resource) delete);
			deleteExpr = tupleNode;
			deleteExpr.setParentNode(null);

			DataVisitor visitor = new DataVisitor();
			deleteExpr.visit(visitor);
			updateRoot = new DeleteData(visitor.getData());
		}

		public void visitLoad(Resource query) throws RDF4JException {
			Value document = TripleSources.singleValue(query, SP.DOCUMENT_PROPERTY, store);
			Value into = TripleSources.singleValue(query, SP.INTO_PROPERTY, store);
			Load load = new Load(new ValueConstant(document));
			load.setGraph(new ValueConstant(into));
			boolean isSilent = TripleSources.booleanValue(query, SP.SILENT_PROPERTY, store);
			load.setSilent(isSilent);
			updateRoot = load;
		}

		public void visitClear(Resource query) throws RDF4JException {
			Value graph = TripleSources.singleValue(query, SP.GRAPH_IRI_PROPERTY, store);
			Clear clear = new Clear(new ValueConstant(graph));
			boolean isSilent = TripleSources.booleanValue(query, SP.SILENT_PROPERTY, store);
			clear.setSilent(isSilent);
			updateRoot = clear;
		}

		public void visitCreate(Resource query) throws RDF4JException {
			Value graph = TripleSources.singleValue(query, SP.GRAPH_IRI_PROPERTY, store);
			Create create = new Create(new ValueConstant(graph));
			boolean isSilent = TripleSources.booleanValue(query, SP.SILENT_PROPERTY, store);
			create.setSilent(isSilent);
			updateRoot = create;
		}

		public TupleExpr visitWhere(Resource query) throws RDF4JException {
			Value where = TripleSources.singleValue(query, SP.WHERE_PROPERTY, store);
			if (!(where instanceof Resource)) {
				throw new MalformedSpinException(String.format("Value of %s is not a resource", SP.WHERE_PROPERTY));
			}
			return visitGroupGraphPattern((Resource) where);
		}

		public TupleExpr visitGroupGraphPattern(Resource group) throws RDF4JException {
			tupleNode = new SingletonSet();
			QueryRoot groupRoot = new QueryRoot(tupleNode);

			Map<Resource, Set<IRI>> patternTypes = new LinkedHashMap<>();
			Iteration<Resource, QueryEvaluationException> groupIter = TripleSources.listResources(group,
					store);
			while (groupIter.hasNext()) {
				Resource r = groupIter.next();
				patternTypes.put(r, Iterations.asSet(TripleSources.getObjectURIs(r, RDF.TYPE, store)));
			}

			// first process filters
			TupleExpr currentNode = tupleNode;
			SingletonSet nextNode = new SingletonSet();
			tupleNode = nextNode;
			for (Iterator<Map.Entry<Resource, Set<IRI>>> iter = patternTypes.entrySet().iterator(); iter.hasNext();) {
				Map.Entry<Resource, Set<IRI>> entry = iter.next();
				if (entry.getValue().contains(SP.FILTER_CLASS)) {
					visitFilter(entry.getKey());
					iter.remove();
				}
			}
			currentNode.replaceWith(tupleNode);
			tupleNode = nextNode;

			// then binds
			currentNode = tupleNode;
			nextNode = new SingletonSet();
			tupleNode = nextNode;
			for (Iterator<Map.Entry<Resource, Set<IRI>>> iter = patternTypes.entrySet().iterator(); iter.hasNext();) {
				Map.Entry<Resource, Set<IRI>> entry = iter.next();
				if (entry.getValue().contains(SP.BIND_CLASS)) {
					visitBind(entry.getKey());
					iter.remove();
				}
			}
			currentNode.replaceWith(tupleNode);
			tupleNode = nextNode;

			// then anything else
			for (Iterator<Map.Entry<Resource, Set<IRI>>> iter = patternTypes.entrySet().iterator(); iter.hasNext();) {
				Map.Entry<Resource, Set<IRI>> entry = iter.next();
				visitPattern(entry.getKey(), entry.getValue(), groupRoot.getArg());
			}

			TupleExpr groupExpr = groupRoot.getArg();
			groupExpr.setParentNode(null);
			return groupExpr;
		}

		private void visitInsert(Resource insert) throws RDF4JException {
			Iteration<Resource, QueryEvaluationException> groupIter = TripleSources.listResources(insert,
					store);
			while (groupIter.hasNext()) {
				Resource r = groupIter.next();
				Value type = TripleSources.singleValue(r, RDF.TYPE, store);
				visitPattern(r, (type != null) ? Collections.singleton((IRI) type) : Collections.<IRI>emptySet(), null);
			}
		}

		private void visitDelete(Resource delete) throws RDF4JException {
			Iteration<Resource, QueryEvaluationException> groupIter = TripleSources.listResources(delete,
					store);
			while (groupIter.hasNext()) {
				Resource r = groupIter.next();
				Value type = TripleSources.singleValue(r, RDF.TYPE, store);
				visitPattern(r, (type != null) ? Collections.singleton((IRI) type) : Collections.<IRI>emptySet(), null);
			}
		}

		private void visitPattern(Resource r, Set<IRI> types, TupleExpr currentGroupExpr) throws RDF4JException {
			TupleExpr currentNode = tupleNode;
			Value pred = TripleSources.singleValue(r, SP.PREDICATE_PROPERTY, store);
			if (pred != null) {
				// only triple patterns have sp:predicate
				Value subj = TripleSources.singleValue(r, SP.SUBJECT_PROPERTY, store);
				Value obj = TripleSources.singleValue(r, SP.OBJECT_PROPERTY, store);
				Scope stmtScope = (namedGraph != null) ? Scope.NAMED_CONTEXTS : Scope.DEFAULT_CONTEXTS;
				tupleNode = new StatementPattern(stmtScope, getVar(subj), getVar(pred), getVar(obj), namedGraph);
			} else {
				if (types.contains(SP.NAMED_GRAPH_CLASS)) {
					Var oldGraph = namedGraph;
					Value graphValue = TripleSources.singleValue(r, SP.GRAPH_NAME_NODE_PROPERTY, store);
					namedGraph = getVar(graphValue);
					Value elements = TripleSources.singleValue(r, SP.ELEMENTS_PROPERTY, store);
					if (!(elements instanceof Resource)) {
						throw new MalformedSpinException(
								String.format("Value of %s is not a resource", SP.ELEMENTS_PROPERTY));
					}
					tupleNode = visitGroupGraphPattern((Resource) elements);
					namedGraph = oldGraph;
				} else if (types.contains(SP.UNION_CLASS)) {
					Value elements = TripleSources.singleValue(r, SP.ELEMENTS_PROPERTY, store);
					if (!(elements instanceof Resource)) {
						throw new MalformedSpinException(
								String.format("Value of %s is not a resource", SP.ELEMENTS_PROPERTY));
					}

					Iteration<Resource, QueryEvaluationException> iter = TripleSources
							.listResources((Resource) elements, store);
					TupleExpr prev = null;
					while (iter.hasNext()) {
						Resource entry = iter.next();
						TupleExpr groupExpr = visitGroupGraphPattern(entry);
						if (prev != null) {
							groupExpr = new Union(prev, groupExpr);
							tupleNode = groupExpr;
						}
						prev = groupExpr;
					}
				} else if (types.contains(SP.OPTIONAL_CLASS)) {
					Value elements = TripleSources.singleValue(r, SP.ELEMENTS_PROPERTY, store);
					if (!(elements instanceof Resource)) {
						throw new MalformedSpinException(
								String.format("Value of %s is not a resource", SP.ELEMENTS_PROPERTY));
					}
					TupleExpr groupExpr = visitGroupGraphPattern((Resource) elements);
					LeftJoin leftJoin = new LeftJoin();
					currentGroupExpr.replaceWith(leftJoin);
					leftJoin.setLeftArg(currentGroupExpr);
					leftJoin.setRightArg(groupExpr);
					tupleNode = leftJoin;
					currentNode = null;
				} else if (types.contains(SP.MINUS_CLASS)) {
					Value elements = TripleSources.singleValue(r, SP.ELEMENTS_PROPERTY, store);
					if (!(elements instanceof Resource)) {
						throw new MalformedSpinException(
								String.format("Value of %s is not a resource", SP.ELEMENTS_PROPERTY));
					}
					TupleExpr groupExpr = visitGroupGraphPattern((Resource) elements);
					Difference difference = new Difference();
					currentGroupExpr.replaceWith(difference);
					difference.setLeftArg(currentGroupExpr);
					difference.setRightArg(groupExpr);
					tupleNode = difference;
					currentNode = null;
				} else if (types.contains(SP.SUB_QUERY_CLASS)) {
					Value q = TripleSources.singleValue(r, SP.QUERY_PROPERTY, store);
					TupleExpr oldRoot = tupleRoot;
					visitSelect((Resource) q);
					tupleNode = tupleRoot;
					tupleRoot = oldRoot;
				} else if (types.contains(SP.VALUES_CLASS)) {
					BindingSetAssignment bsa = new BindingSetAssignment();
					Set<String> varNames = new LinkedHashSet<>();
					Value varNameList = TripleSources.singleValue(r, SP.VAR_NAMES_PROPERTY, store);
					Iteration<Value, QueryEvaluationException> varNameIter = TripleSources
							.list((Resource) varNameList, store);
					while (varNameIter.hasNext()) {
						Value v = varNameIter.next();
						if (v instanceof Literal) {
							varNames.add(((Literal) v).getLabel());
						}
					}
					bsa.setBindingNames(varNames);
					List<BindingSet> bindingSets = new ArrayList<>();
					Value bindingsList = TripleSources.singleValue(r, SP.BINDINGS_PROPERTY, store);
					Iteration<Value, QueryEvaluationException> bindingsIter = TripleSources
							.list((Resource) bindingsList, store);
					while (bindingsIter.hasNext()) {
						Value valueList = bindingsIter.next();
						QueryBindingSet bs = new QueryBindingSet();
						Iterator<String> nameIter = varNames.iterator();
						Iteration<Value, QueryEvaluationException> valueIter = TripleSources
								.list((Resource) valueList, store);
						while (nameIter.hasNext() && valueIter.hasNext()) {
							String name = nameIter.next();
							Value value = valueIter.next();
							if (!SP.UNDEF.equals(value)) {
								bs.addBinding(name, value);
							}
						}
						bindingSets.add(bs);
					}
					bsa.setBindingSets(bindingSets);
					tupleNode = bsa;
				} else if (types.contains(RDF.LIST) || (TripleSources.singleValue(r, RDF.FIRST, store) != null)) {
					tupleNode = visitGroupGraphPattern(r);
				} else if (types.contains(SP.TRIPLE_PATH_CLASS)) {
					Value subj = TripleSources.singleValue(r, SP.SUBJECT_PROPERTY, store);
					Value obj = TripleSources.singleValue(r, SP.OBJECT_PROPERTY, store);
					Resource path = (Resource) TripleSources.singleValue(r, SP.PATH_PROPERTY, store);
					Set<IRI> pathTypes = Iterations.asSet(TripleSources.getObjectURIs(path, RDF.TYPE, store));
					if (pathTypes.contains(SP.MOD_PATH_CLASS)) {
						Resource subPath = (Resource) TripleSources.singleValue(path, SP.SUB_PATH_PROPERTY, store);
						Literal minPath = (Literal) TripleSources.singleValue(path, SP.MOD_MIN_PROPERTY, store);
						Literal maxPath = (Literal) TripleSources.singleValue(path, SP.MOD_MAX_PROPERTY, store);
						if (maxPath == null || maxPath.intValue() != -2) {
							throw new UnsupportedOperationException("Unsupported mod path");
						}
						Var subjVar = getVar(subj);
						Var objVar = getVar(obj);
						tupleNode = new ArbitraryLengthPath(subjVar,
								new StatementPattern(subjVar, getVar(subPath), objVar), objVar, minPath.longValue());
					} else {
						throw new UnsupportedOperationException(types.toString());
					}
				} else if (types.contains(SP.SERVICE_CLASS)) {
					Value serviceUri = TripleSources.singleValue(r, SP.SERVICE_URI_PROPERTY, store);

					Value elements = TripleSources.singleValue(r, SP.ELEMENTS_PROPERTY, store);
					if (!(elements instanceof Resource)) {
						throw new MalformedSpinException(
								String.format("Value of %s is not a resource", SP.ELEMENTS_PROPERTY));
					}
					TupleExpr groupExpr = visitGroupGraphPattern((Resource) elements);

					boolean isSilent = TripleSources.booleanValue(r, SP.SILENT_PROPERTY, store);
					String exprString;
					try {
						exprString = new SPARQLQueryRenderer().render(new ParsedTupleQuery(groupExpr));
						exprString = exprString.substring(exprString.indexOf('{') + 1, exprString.lastIndexOf('}'));
					} catch (Exception e) {
						throw new QueryEvaluationException(e);
					}
					Map<String, String> prefixDecls = new HashMap<>(8);
					prefixDecls.put(SP.PREFIX, SP.NAMESPACE);
					prefixDecls.put(SPIN.PREFIX, SPIN.NAMESPACE);
					prefixDecls.put(SPL.PREFIX, SPL.NAMESPACE);
					Service service = new Service(getVar(serviceUri), tupleNode, exprString, prefixDecls, null,
							isSilent);
					tupleNode = service;
				} else {
					throw new UnsupportedOperationException(types.toString());
				}
			}

			if (currentNode instanceof SingletonSet) {
				currentNode.replaceWith(tupleNode);
			} else if (currentNode != null) {
				Join join = new Join();
				currentNode.replaceWith(join);
				join.setLeftArg(currentNode);
				join.setRightArg(tupleNode);
				tupleNode = join;
			}
		}

		private void visitFilter(Resource r) throws RDF4JException {
			Value expr = TripleSources.singleValue(r, SP.EXPRESSION_PROPERTY, store);
			ValueExpr valueExpr = visitExpression(expr);
			tupleNode = new Filter(tupleNode, valueExpr);
		}

		private void visitBind(Resource r) throws RDF4JException {
			Value expr = TripleSources.singleValue(r, SP.EXPRESSION_PROPERTY, store);
			ValueExpr valueExpr = visitExpression(expr);
			Value varValue = TripleSources.singleValue(r, SP.VARIABLE_PROPERTY, store);
			if (!(varValue instanceof Resource)) {
				throw new MalformedSpinException(String.format("Value of %s is not a resource", SP.VARIABLE_PROPERTY));
			}
			String varName = getVarName((Resource) varValue);
			tupleNode = new Extension(tupleNode, new ExtensionElem(valueExpr, varName));
		}

		private ValueExpr visitExpression(Value v) throws RDF4JException {
			ValueExpr expr;
			if (v instanceof Literal) {
				expr = new ValueConstant(v);
			} else {
				Resource r = (Resource) v;
				String varName = getVarName(r);
				if (varName != null) {
					expr = createVar(varName);
				} else {
					Set<IRI> exprTypes = Iterations.asSet(TripleSources.getObjectURIs(r, RDF.TYPE, store));
					exprTypes.remove(RDF.PROPERTY);
					exprTypes.remove(RDFS.RESOURCE);
					exprTypes.remove(RDFS.CLASS);
					if (exprTypes.size() > 1) {
						if (exprTypes.remove(SPIN.FUNCTIONS_CLASS)) {
							exprTypes.remove(SPIN.MODULES_CLASS);
							if (exprTypes.size() > 1) {
								for (Iterator<IRI> iter = exprTypes.iterator(); iter.hasNext();) {
									IRI f = iter.next();
									Value abstractValue = TripleSources.singleValue(f, SPIN.ABSTRACT_PROPERTY, store);
									if (BooleanLiteral.TRUE.equals(abstractValue)) {
										iter.remove();
									}
								}
							}
							if (exprTypes.isEmpty()) {
								throw new MalformedSpinException(String.format("Function missing RDF type: %s", r));
							}
						} else if (exprTypes.remove(SP.AGGREGATION_CLASS)) {
							exprTypes.remove(SP.SYSTEM_CLASS);
							if (exprTypes.isEmpty()) {
								throw new MalformedSpinException(String.format("Aggregation missing RDF type: %s", r));
							}
						} else {
							exprTypes = Collections.emptySet();
						}
					}

					expr = null;
					if (exprTypes.size() == 1) {
						IRI func = exprTypes.iterator().next();
						expr = toValueExpr(r, func);
					}
					if (expr == null) {
						expr = new ValueConstant(v);
					}
				}
			}
			return expr;
		}

		private ValueExpr toValueExpr(Resource r, IRI func) throws RDF4JException {
			ValueExpr expr;
			CompareOp compareOp;
			MathOp mathOp;
			if ((compareOp = toCompareOp(func)) != null) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY, SP.ARG2_PROPERTY);
				if (args.size() != 2) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new Compare(args.get(0), args.get(1), compareOp);
			} else if ((mathOp = toMathOp(func)) != null) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY, SP.ARG2_PROPERTY);
				if (args.size() != 2) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new MathExpr(args.get(0), args.get(1), mathOp);
			} else if (SP.AND.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY, SP.ARG2_PROPERTY);
				if (args.size() != 2) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new And(args.get(0), args.get(1));
			} else if (SP.OR.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY, SP.ARG2_PROPERTY);
				if (args.size() != 2) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new Or(args.get(0), args.get(1));
			} else if (SP.NOT.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new Not(args.get(0));
			} else if (SP.COUNT_CLASS.equals(func)) {
				Value arg = TripleSources.singleValue(r, SP.EXPRESSION_PROPERTY, store);
				boolean distinct = TripleSources.booleanValue(r, SP.DISTINCT_PROPERTY, store);
				Count count = new Count(visitExpression(arg), distinct);
				aggregates.add(count);
				expr = count;
			} else if (SP.MAX_CLASS.equals(func)) {
				Value arg = TripleSources.singleValue(r, SP.EXPRESSION_PROPERTY, store);
				boolean distinct = TripleSources.booleanValue(r, SP.DISTINCT_PROPERTY, store);
				Max max = new Max(visitExpression(arg), distinct);
				aggregates.add(max);
				expr = max;
			} else if (SP.MIN_CLASS.equals(func)) {
				Value arg = TripleSources.singleValue(r, SP.EXPRESSION_PROPERTY, store);
				boolean distinct = TripleSources.booleanValue(r, SP.DISTINCT_PROPERTY, store);
				Min min = new Min(visitExpression(arg), distinct);
				aggregates.add(min);
				expr = min;
			} else if (SP.SUM_CLASS.equals(func)) {
				Value arg = TripleSources.singleValue(r, SP.EXPRESSION_PROPERTY, store);
				boolean distinct = TripleSources.booleanValue(r, SP.DISTINCT_PROPERTY, store);
				Sum sum = new Sum(visitExpression(arg), distinct);
				aggregates.add(sum);
				expr = sum;
			} else if (SP.AVG_CLASS.equals(func)) {
				Value arg = TripleSources.singleValue(r, SP.EXPRESSION_PROPERTY, store);
				boolean distinct = TripleSources.booleanValue(r, SP.DISTINCT_PROPERTY, store);
				Avg avg = new Avg(visitExpression(arg), distinct);
				aggregates.add(avg);
				expr = avg;
			} else if (SP.GROUP_CONCAT_CLASS.equals(func)) {
				Value arg = TripleSources.singleValue(r, SP.EXPRESSION_PROPERTY, store);
				boolean distinct = TripleSources.booleanValue(r, SP.DISTINCT_PROPERTY, store);
				GroupConcat groupConcat = new GroupConcat(visitExpression(arg), distinct);
				aggregates.add(groupConcat);
				expr = groupConcat;
			} else if (SP.SAMPLE_CLASS.equals(func)) {
				Value arg = TripleSources.singleValue(r, SP.EXPRESSION_PROPERTY, store);
				boolean distinct = TripleSources.booleanValue(r, SP.DISTINCT_PROPERTY, store);
				Sample sample = new Sample(visitExpression(arg), distinct);
				aggregates.add(sample);
				expr = sample;
			} else if (SP.EXISTS.equals(func)) {
				Value elements = TripleSources.singleValue(r, SP.ELEMENTS_PROPERTY, store);
				if (!(elements instanceof Resource)) {
					throw new MalformedSpinException(
							String.format("Value of %s is not a resource", SP.ELEMENTS_PROPERTY));
				}
				TupleExpr currentNode = tupleNode;
				TupleExpr groupExpr = visitGroupGraphPattern((Resource) elements);
				expr = new Exists(groupExpr);
				tupleNode = currentNode;
			} else if (SP.NOT_EXISTS.equals(func)) {
				Value elements = TripleSources.singleValue(r, SP.ELEMENTS_PROPERTY, store);
				if (!(elements instanceof Resource)) {
					throw new MalformedSpinException(
							String.format("Value of %s is not a resource", SP.ELEMENTS_PROPERTY));
				}
				TupleExpr currentNode = tupleNode;
				TupleExpr groupExpr = visitGroupGraphPattern((Resource) elements);
				expr = new Not(new Exists(groupExpr));
				tupleNode = currentNode;
			} else if (SP.BOUND.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new Bound((Var) args.get(0));
			} else if (SP.IF.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY, SP.ARG2_PROPERTY, SP.ARG3_PROPERTY);
				if (args.size() != 3) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new If(args.get(0), args.get(1), args.get(2));
			} else if (SP.COALESCE.equals(func)) {
				List<ValueExpr> args = getArgs(r, func);
				expr = new Coalesce(args);
			} else if (SP.IS_IRI.equals(func) || SP.IS_URI.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new IsURI(args.get(0));
			} else if (SP.IS_BLANK.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new IsBNode(args.get(0));
			} else if (SP.IS_LITERAL.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new IsLiteral(args.get(0));
			} else if (SP.IS_NUMERIC.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new IsNumeric(args.get(0));
			} else if (SP.STR.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new Str(args.get(0));
			} else if (SP.LANG.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new Lang(args.get(0));
			} else if (SP.DATATYPE.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new Datatype(args.get(0));
			} else if (SP.IRI.equals(func) || SP.URI.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new IRIFunction(args.get(0));
			} else if (SP.BNODE.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				ValueExpr arg = (args.size() == 1) ? args.get(0) : null;
				expr = new BNodeGenerator(arg);
			} else if (SP.REGEX.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY, SP.ARG2_PROPERTY, SP.ARG3_PROPERTY);
				if (args.size() < 2) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				ValueExpr flagsArg = (args.size() == 3) ? args.get(2) : null;
				expr = new Regex(args.get(0), args.get(1), flagsArg);
			} else if (AFN.LOCALNAME.equals(func)) {
				List<ValueExpr> args = getArgs(r, func, SP.ARG1_PROPERTY);
				if (args.size() != 1) {
					throw new MalformedSpinException(
							String.format("Invalid number of arguments for function: %s", func));
				}
				expr = new LocalName(args.get(0));
			} else {
				String funcName = wellKnownFunctions.apply(func);
				if (funcName == null) {
					// check if it is a SPIN function
					Statement funcTypeStmt = TripleSources.single(func, RDF.TYPE, SPIN.FUNCTION_CLASS, store);
					if (funcTypeStmt != null) {
						funcName = func.stringValue();
					}
				}
				// not enough information available to determine
				// if it is really a function or not
				// so by default we can either assume it is or it is not
				if (funcName == null && !strictFunctionChecking) {
					funcName = func.stringValue();
				}
				if (funcName != null) {
					List<ValueExpr> args = getArgs(r, func, (IRI[]) null);
					expr = new FunctionCall(funcName, args);
				} else {
					expr = null;
				}
			}
			return expr;
		}

		private CompareOp toCompareOp(IRI func) {
			if (SP.EQ.equals(func)) {
				return CompareOp.EQ;
			} else if (SP.NE.equals(func)) {
				return CompareOp.NE;
			} else if (SP.LT.equals(func)) {
				return CompareOp.LT;
			} else if (SP.LE.equals(func)) {
				return CompareOp.LE;
			} else if (SP.GE.equals(func)) {
				return CompareOp.GE;
			} else if (SP.GT.equals(func)) {
				return CompareOp.GT;
			} else {
				return null;
			}
		}

		private MathOp toMathOp(IRI func) {
			if (SP.ADD.equals(func)) {
				return MathOp.PLUS;
			} else if (SP.SUB.equals(func)) {
				return MathOp.MINUS;
			} else if (SP.MUL.equals(func)) {
				return MathOp.MULTIPLY;
			} else if (SP.DIVIDE.equals(func)) {
				return MathOp.DIVIDE;
			} else {
				return null;
			}
		}

		/**
		 * @param knownArgs empty for vararg, null for unknown.
		 */
		private List<ValueExpr> getArgs(Resource r, IRI func, IRI... knownArgs) throws RDF4JException {
			Collection<IRI> args;
			if (knownArgs != null) {
				args = Arrays.asList(knownArgs);
			} else {
				args = parseArguments(func, store).keySet();
			}
			Map<IRI, ValueExpr> argBindings = new HashMap<>();
			if (!args.isEmpty()) {
				for (IRI arg : args) {
					Value value = TripleSources.singleValue(r, arg, store);
					if (value != null) {
						ValueExpr argValue = visitExpression(value);
						argBindings.put(arg, argValue);
					}
				}
			} else {
				Value value;
				int i = 1;
				do {
					IRI arg = toArgProperty(i++);
					value = TripleSources.singleValue(r, arg, store);
					if (value != null) {
						ValueExpr argValue = visitExpression(value);
						argBindings.put(arg, argValue);
					}
				} while (value != null);
			}

			List<ValueExpr> argValues = new ArrayList<>(argBindings.size());
			List<IRI> orderedArgs = orderArguments(argBindings.keySet());
			for (IRI IRI : orderedArgs) {
				ValueExpr argExpr = argBindings.get(IRI);
				argValues.add(argExpr);
			}
			return argValues;
		}

		private String getVarName(Resource r) throws RDF4JException {
			// have we already seen it
			String varName = vars.get(r);
			// is it well-known
			if (varName == null && r instanceof IRI) {
				varName = wellKnownVars.apply((IRI) r);
				if (varName != null) {
					vars.put(r, varName);
				}
			}
			if (varName == null) {
				// check for a varName statement
				Value nameValue = TripleSources.singleValue(r, SP.VAR_NAME_PROPERTY, store);
				if (nameValue instanceof Literal) {
					varName = ((Literal) nameValue).getLabel();
					if (varName != null) {
						vars.put(r, varName);
					}
				} else if (nameValue != null) {
					throw new MalformedSpinException(
							String.format("Value of %s is not a literal", SP.VAR_NAME_PROPERTY));
				}
			}
			return varName;
		}

		private Var getVar(Value v) throws RDF4JException {
			Var var = null;
			if (v instanceof Resource) {
				String varName = getVarName((Resource) v);
				if (varName != null) {
					var = createVar(varName);
				}
			}

			if (var == null) {
				// it must be a constant then
				var = TupleExprs.createConstVar(v);
			}

			return var;
		}

		private Var createVar(String varName) {
			if (projElems != null) {
				ProjectionElem projElem = projElems.get(varName);
				if (projElem != null) {
					ExtensionElem extElem = projElem.getSourceExpression();
					if (extElem != null && extElem.getExpr() instanceof Var) {
						projElems.remove(varName);
					}
				}
			}
			return new Var(varName);
		}
	}

	private static class DataVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		final StringBuilder buf = new StringBuilder(1024);

		DataVisitor() {
			appendPrefix(RDF.PREFIX, RDF.NAMESPACE);
			appendPrefix(RDFS.PREFIX, RDFS.NAMESPACE);
			appendPrefix(RDF4J.PREFIX, RDF4J.NAMESPACE);
			appendPrefix(SESAME.PREFIX, SESAME.NAMESPACE);
			appendPrefix(OWL.PREFIX, OWL.NAMESPACE);
			appendPrefix(XSD.PREFIX, XSD.NAMESPACE);
			appendPrefix(FN.PREFIX, FN.NAMESPACE);
			buf.append(" ");
		}

		void appendPrefix(String prefix, String namespace) {
			buf.append("PREFIX ").append(prefix).append(": <").append(namespace).append("> \n");
		}

		String getData() {
			return buf.toString();
		}

		@Override
		public void meet(StatementPattern node) throws RuntimeException {
			if (node.getContextVar() != null) {
				buf.append("GRAPH <").append(node.getContextVar().getValue()).append("> { ");
			}
			buf.append("<")
					.append(node.getSubjectVar().getValue())
					.append("> <")
					.append(node.getPredicateVar().getValue())
					.append("> <")
					.append(node.getObjectVar().getValue())
					.append("> .");
			if (node.getContextVar() != null) {
				buf.append(" } ");
			}
		}
	}
}
