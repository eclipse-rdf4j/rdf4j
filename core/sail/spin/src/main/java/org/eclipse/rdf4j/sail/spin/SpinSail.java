/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import java.util.Set;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverImpl;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.AbstractForwardChainingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.eclipse.rdf4j.spin.SpinParser;

public class SpinSail extends AbstractForwardChainingInferencer {

	public enum EvaluationMode {
		/**
		 * Uses the base SAIL along with an embedded SERVICE to perform query evaluation. The SERVICE is used
		 * to evaluate extended query algebra nodes such as {@link TupleFunction}s. (Default).
		 */
		SERVICE,
		/**
		 * Assumes the base SAIL supports an extended query algebra (e.g. {@link TupleFunction}s) and use it
		 * to perform all query evaluation.
		 */
		NATIVE,
		/**
		 * Treats the base SAIL as a simple triple source and all the query evaluation is performed by this
		 * SAIL.
		 */
		TRIPLE_SOURCE
	}

	private FunctionRegistry functionRegistry = FunctionRegistry.getInstance();

	private TupleFunctionRegistry tupleFunctionRegistry = TupleFunctionRegistry.getInstance();

	private FederatedServiceResolver serviceResolver = new FederatedServiceResolverImpl();

	private SpinParser parser = new SpinParser();

	private EvaluationMode evaluationMode = EvaluationMode.SERVICE;

	private boolean axiomClosureNeeded = true;

	public SpinSail() {
		super.setFederatedServiceResolver(serviceResolver);
	}

	public SpinSail(NotifyingSail baseSail) {
		super(baseSail);
		if (baseSail instanceof ForwardChainingRDFSInferencer) {
			this.setAxiomClosureNeeded(false);
		}
		super.setFederatedServiceResolver(serviceResolver);
	}

	@Override
	public void setBaseSail(Sail baseSail) {
		super.setBaseSail(baseSail);
		if (baseSail instanceof ForwardChainingRDFSInferencer) {
			this.setAxiomClosureNeeded(false);
		}
	}

	public FunctionRegistry getFunctionRegistry() {
		return functionRegistry;
	}

	public void setFunctionRegistry(FunctionRegistry registry) {
		this.functionRegistry = registry;
	}

	public TupleFunctionRegistry getTupleFunctionRegistry() {
		return tupleFunctionRegistry;
	}

	public void setTupleFunctionRegistry(TupleFunctionRegistry registry) {
		this.tupleFunctionRegistry = registry;
	}

	public FederatedServiceResolver getFederatedServiceResolver() {
		return serviceResolver;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		serviceResolver = resolver;
		super.setFederatedServiceResolver(resolver);
	}

	public void setEvaluationMode(EvaluationMode mode) {
		this.evaluationMode = mode;
	}

	public EvaluationMode getEvaluationMode() {
		return evaluationMode;
	}

	public void setAxiomClosureNeeded(boolean axiomClosureNeeded) {
		this.axiomClosureNeeded = axiomClosureNeeded;
	}

	/**
	 * Indicates if the SPIN Sail should itself load the full deductive closure of the SPIN axioms. Typically,
	 * this will be {@code false} if the underlying Sail stack already supports RDFS inferencing, {@code true}
	 * if not.
	 * 
	 * @return {@code true} if the SpinSail needs to load the full axiom closure, {@code false} otherwise.
	 */
	public boolean isAxiomClosureNeeded() {
		return this.axiomClosureNeeded;
	}

	public SpinParser getSpinParser() {
		return parser;
	}

	public void setSpinParser(SpinParser parser) {
		this.parser = parser;
	}

	@Override
	public SpinSailConnection getConnection()
		throws SailException
	{
		InferencerConnection con = (InferencerConnection)super.getConnection();
		return new SpinSailConnection(this, con);
	}

	@Override
	public void initialize()
		throws SailException
	{
		super.initialize();

		SpinFunctionInterpreter.registerSpinParsingFunctions(parser, functionRegistry);
		SpinMagicPropertyInterpreter.registerSpinParsingTupleFunctions(parser, tupleFunctionRegistry);

		SpinSailConnection con = getConnection();
		try {
			con.begin();
			Set<Statement> stmts = Iterations.asSet(con.getStatements(
					getValueFactory().createIRI("http://spinrdf.org/sp"), RDF.TYPE, OWL.ONTOLOGY, true));
			if (stmts.isEmpty()) {
				con.addAxiomStatements();
			}
			con.commit();
		}
		finally {
			con.close();
		}
	}
}
