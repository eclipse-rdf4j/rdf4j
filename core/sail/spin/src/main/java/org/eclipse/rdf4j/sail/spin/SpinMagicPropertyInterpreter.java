/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SP;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.model.vocabulary.SPL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.TupleFunctionCall;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.AbstractFederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.TupleFunctionFederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.util.TripleSources;
import org.eclipse.rdf4j.query.algebra.helpers.BGPCollector;
import org.eclipse.rdf4j.query.algebra.helpers.QueryModelVisitorBase;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.queryrender.sparql.SPARQLQueryRenderer;
import org.eclipse.rdf4j.spin.SpinParser;
import org.eclipse.rdf4j.spin.function.ConstructTupleFunction;
import org.eclipse.rdf4j.spin.function.InverseMagicProperty;
import org.eclipse.rdf4j.spin.function.SelectTupleFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpinMagicPropertyInterpreter implements QueryOptimizer {

	private static final Logger logger = LoggerFactory.getLogger(SpinMagicPropertyInterpreter.class);

	private static final String SPIN_SERVICE = "spin:/";

	private final TripleSource tripleSource;

	private final SpinParser parser;

	private final TupleFunctionRegistry tupleFunctionRegistry;

	private AbstractFederatedServiceResolver serviceResolver;

	private final IRI spinServiceUri;

	static void registerSpinParsingTupleFunctions(SpinParser parser,
			TupleFunctionRegistry tupleFunctionRegistry)
	{
		if (!tupleFunctionRegistry.has(SPIN.CONSTRUCT_PROPERTY.stringValue())) {
			tupleFunctionRegistry.add(new ConstructTupleFunction(parser));
		}
		if (!tupleFunctionRegistry.has(SPIN.SELECT_PROPERTY.stringValue())) {
			tupleFunctionRegistry.add(new SelectTupleFunction(parser));
		}
	}

	public SpinMagicPropertyInterpreter(SpinParser parser, TripleSource tripleSource,
			TupleFunctionRegistry tupleFunctionRegistry, AbstractFederatedServiceResolver serviceResolver)
	{
		this.parser = parser;
		this.tripleSource = tripleSource;
		this.tupleFunctionRegistry = tupleFunctionRegistry;
		this.serviceResolver = serviceResolver;
		this.spinServiceUri = tripleSource.getValueFactory().createIRI(SPIN_SERVICE);
	}

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		try {
			tupleExpr.visit(new PropertyScanner());
		}
		catch (RDF4JException e) {
			logger.warn("Failed to parse tuple function");
		}
	}

	private class PropertyScanner extends QueryModelVisitorBase<RDF4JException> {

		private void processGraphPattern(List<StatementPattern> sps)
			throws RDF4JException
		{
			Map<StatementPattern, TupleFunction> magicProperties = new LinkedHashMap<StatementPattern, TupleFunction>();
			Map<String, Map<IRI, List<StatementPattern>>> spIndex = new HashMap<String, Map<IRI, List<StatementPattern>>>();

			for (StatementPattern sp : sps) {
				IRI pred = (IRI)sp.getPredicateVar().getValue();
				if (pred != null) {
					TupleFunction func = tupleFunctionRegistry.get(pred.stringValue());
					if (func != null) {
						magicProperties.put(sp, func);
					}
					else {
						Statement magicPropStmt = TripleSources.single(pred, RDF.TYPE, SPIN.MAGIC_PROPERTY_CLASS,
								tripleSource);
						if (magicPropStmt != null) {
							func = parser.parseMagicProperty(pred, tripleSource);
							tupleFunctionRegistry.add(func);
							magicProperties.put(sp, func);
						}
						else {
							// normal statement
							String subj = sp.getSubjectVar().getName();
							Map<IRI, List<StatementPattern>> predMap = spIndex.get(subj);
							if (predMap == null) {
								predMap = new HashMap<IRI, List<StatementPattern>>(8);
								spIndex.put(subj, predMap);
							}
							List<StatementPattern> v = predMap.get(pred);
							if (v == null) {
								v = new ArrayList<StatementPattern>(1);
								predMap.put(pred, v);
							}
							v.add(sp);
						}
					}
				}
			}

			if (!magicProperties.isEmpty()) {
				for (Map.Entry<StatementPattern, TupleFunction> entry : magicProperties.entrySet()) {
					StatementPattern sp = entry.getKey();
					TupleFunction func = entry.getValue();
					Union union = new Union();
					sp.replaceWith(union);
					TupleExpr stmts = sp;

					List<? super Var> subjList = new ArrayList<ValueExpr>(4);
					TupleExpr subjNodes = addList(subjList, sp.getSubjectVar(), spIndex);
					if (subjNodes != null) {
						stmts = new Join(stmts, subjNodes);
					}
					else {
						subjList = Collections.<ValueExpr> singletonList(sp.getSubjectVar());
					}

					List<? super Var> objList = new ArrayList<ValueExpr>(4);
					TupleExpr objNodes = addList(objList, sp.getObjectVar(), spIndex);
					if (objNodes != null) {
						stmts = new Join(stmts, objNodes);
					}
					else {
						objList = Collections.<ValueExpr> singletonList(sp.getObjectVar());
					}
					union.setLeftArg(stmts);

					TupleFunctionCall funcCall = new TupleFunctionCall();
					funcCall.setURI(sp.getPredicateVar().getValue().stringValue());
					if (func instanceof InverseMagicProperty) {
						funcCall.setArgs((List<ValueExpr>)objList);
						funcCall.setResultVars((List<Var>)subjList);
					}
					else {
						funcCall.setArgs((List<ValueExpr>)subjList);
						funcCall.setResultVars((List<Var>)objList);
					}

					TupleExpr magicPropertyNode;
					if (serviceResolver != null) {
						// use SERVICE evaluation
						if (!serviceResolver.hasService(SPIN_SERVICE)) {
							serviceResolver.registerService(SPIN_SERVICE, new TupleFunctionFederatedService(
									tupleFunctionRegistry, tripleSource.getValueFactory()));
						}

						Var serviceRef = TupleExprs.createConstVar(spinServiceUri);
						String exprString;
						try {
							exprString = new SPARQLQueryRenderer().render(new ParsedTupleQuery(stmts));
							exprString = exprString.substring(exprString.indexOf('{') + 1,
									exprString.lastIndexOf('}'));
						}
						catch (Exception e) {
							throw new MalformedQueryException(e);
						}
						Map<String, String> prefixDecls = new HashMap<String, String>(8);
						prefixDecls.put(SP.PREFIX, SP.NAMESPACE);
						prefixDecls.put(SPIN.PREFIX, SPIN.NAMESPACE);
						prefixDecls.put(SPL.PREFIX, SPL.NAMESPACE);
						magicPropertyNode = new Service(serviceRef, funcCall, exprString, prefixDecls, null,
								false);
					}
					else {
						magicPropertyNode = funcCall;
					}

					union.setRightArg(magicPropertyNode);
				}
			}
		}

		private TupleExpr join(TupleExpr node, TupleExpr toMove) {
			toMove.replaceWith(new SingletonSet());
			if (node != null) {
				node = new Join(node, toMove);
			}
			else {
				node = toMove;
			}
			return node;
		}

		private TupleExpr addList(List<? super Var> list, Var subj,
				Map<String, Map<IRI, List<StatementPattern>>> spIndex)
		{
			TupleExpr node = null;
			do {
				Map<IRI, List<StatementPattern>> predMap = spIndex.get(subj.getName());
				if (predMap == null) {
					return null;
				}

				List<StatementPattern> firstStmts = predMap.get(RDF.FIRST);
				if (firstStmts == null) {
					return null;
				}
				if (firstStmts.size() != 1) {
					return null;
				}

				List<StatementPattern> restStmts = predMap.get(RDF.REST);
				if (restStmts == null) {
					return null;
				}
				if (restStmts.size() != 1) {
					return null;
				}

				StatementPattern firstStmt = firstStmts.get(0);
				list.add(firstStmt.getObjectVar());
				node = join(node, firstStmt);

				StatementPattern restStmt = restStmts.get(0);
				subj = restStmt.getObjectVar();
				node = join(node, restStmt);

				List<StatementPattern> typeStmts = predMap.get(RDF.TYPE);
				if (typeStmts != null) {
					for (StatementPattern sp : firstStmts) {
						Value type = sp.getObjectVar().getValue();
						if (RDFS.RESOURCE.equals(type) || RDF.LIST.equals(type)) {
							node = join(node, sp);
						}
					}
				}
			}
			while (!RDF.NIL.equals(subj.getValue()));
			return node;
		}

		@Override
		public void meet(Join node)
			throws RDF4JException
		{
			BGPCollector<RDF4JException> collector = new BGPCollector<RDF4JException>(this);
			node.visit(collector);
			processGraphPattern(collector.getStatementPatterns());
		}

		@Override
		public void meet(StatementPattern node)
			throws RDF4JException
		{
			processGraphPattern(Collections.singletonList(node));
		}
	}
}
