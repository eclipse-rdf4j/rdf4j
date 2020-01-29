/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryContextInitializer;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunctionRegistry;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.fc.AbstractForwardChainingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.eclipse.rdf4j.spin.SpinParser;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The SpinSail is currently deprecated. If you are using SPIN to validate your data then it is recommended to move to
 * SHACL with the ShaclSail. Currently, the SHACL W3C Recommendation only supports validation, and has no equivalent to
 * SPIN's inference features.
 * </p>
 * <p>
 * Deprecating the SpinSail has been discussed at https://github.com/eclipse/rdf4j/issues/1262 and can be summarized
 * with there being no developers actively supporting it and that SPIN in itself is no longer recommended by
 * TopQuadrant. Do not expect the SpinSail to scale in any way and expect simple delete operations to take seconds to
 * complete. There are still a number of open issues in GitHub connected to the SpinSail, none of these are likely to
 * get fixed.
 * </p>
 *
 * @since 3.1.0 2019
 */
@Deprecated
public class SpinSail extends AbstractForwardChainingInferencer {

	private FunctionRegistry functionRegistry = FunctionRegistry.getInstance();

	private TupleFunctionRegistry tupleFunctionRegistry = TupleFunctionRegistry.getInstance();

	private FederatedServiceResolver serviceResolver = new SPARQLServiceResolver();

	private SpinParser parser = new SpinParser();

	private TupleFunctionEvaluationMode evaluationMode = TupleFunctionEvaluationMode.SERVICE;

	private List<QueryContextInitializer> queryContextInitializers = new ArrayList<>();

	private boolean axiomClosureNeeded = true;

	private boolean validateConstraints = true;

	volatile private boolean initializing;

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

	public void setEvaluationMode(TupleFunctionEvaluationMode mode) {
		this.evaluationMode = mode;
	}

	public TupleFunctionEvaluationMode getEvaluationMode() {
		return evaluationMode;
	}

	public void setAxiomClosureNeeded(boolean axiomClosureNeeded) {
		this.axiomClosureNeeded = axiomClosureNeeded;
	}

	/**
	 * Indicates if the SPIN Sail should itself load the full deductive closure of the SPIN axioms. Typically, this will
	 * be {@code false} if the underlying Sail stack already supports RDFS inferencing, {@code true} if not.
	 *
	 * @return {@code true} if the SpinSail needs to load the full axiom closure, {@code false} otherwise.
	 */
	public boolean isAxiomClosureNeeded() {
		return this.axiomClosureNeeded;
	}

	public void addQueryContextInitializer(QueryContextInitializer initializer) {
		this.queryContextInitializers.add(initializer);
	}

	protected List<QueryContextInitializer> getQueryContextInitializers() {
		return this.queryContextInitializers;
	}

	public SpinParser getSpinParser() {
		return parser;
	}

	public void setSpinParser(SpinParser parser) {
		this.parser = parser;
	}

	@Override
	public SpinSailConnection getConnection()
			throws SailException {
		InferencerConnection con = (InferencerConnection) super.getConnection();
		return new SpinSailConnection(this, con);
	}

	private final static IRI spinrdf_sp = SimpleValueFactory.getInstance().createIRI("http://spinrdf.org/sp");

	@Override
	synchronized public void initialize()
			throws SailException {
		super.initialize();

		initializing = true;
		try {
			registerParsers();
			loadAxioms();
		} finally {
			initializing = false;
		}
	}

	private void registerParsers() {
		SpinFunctionInterpreter.registerSpinParsingFunctions(parser, functionRegistry);
		SpinMagicPropertyInterpreter.registerSpinParsingTupleFunctions(parser, tupleFunctionRegistry);
	}

	private void loadAxioms() {
		try (SpinSailConnection con = getConnection()) {
			con.begin(IsolationLevels.NONE);
			boolean b = con.hasStatement(spinrdf_sp, RDF.TYPE, OWL.ONTOLOGY, true);
			if (!b) {
				con.addAxiomStatements();
			}
			con.commit();
		}
	}

	public boolean isInitializing() {
		return initializing;
	}

	/**
	 * <p>
	 * Disable or enable SPIN constraint validation. This can be very useful in order to improve performance
	 * </p>
	 *
	 * <p>
	 * Default true (constraint validation enabled).
	 * </p>
	 *
	 * @param validateConstraints (true if enabled)
	 */
	public void setValidateConstraints(boolean validateConstraints) {
		this.validateConstraints = validateConstraints;
	}

	/**
	 * Check is SPIN constraint validation is enabled.
	 *
	 * @return true if enabled
	 */
	public boolean isValidateConstraints() {
		return validateConstraints;
	}
}
