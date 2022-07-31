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
package org.eclipse.rdf4j.sail.base.config;

import static org.eclipse.rdf4j.sail.base.config.BaseSailSchema.EVALUATION_STRATEGY_FACTORY;
import static org.eclipse.rdf4j.sail.base.config.BaseSailSchema.NAMESPACE;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

public abstract class BaseSailConfig extends AbstractSailImplConfig {

	private String evalStratFactoryClassName;

	protected BaseSailConfig(String type) {
		super(type);
	}

	public String getEvaluationStrategyFactoryClassName() {
		return evalStratFactoryClassName;
	}

	public void setEvaluationStrategyFactoryClassName(String className) {
		this.evalStratFactoryClassName = className;
	}

	public EvaluationStrategyFactory getEvaluationStrategyFactory() throws SailConfigException {
		if (evalStratFactoryClassName == null) {
			return null;
		}

		try {
			return (EvaluationStrategyFactory) Thread.currentThread()
					.getContextClassLoader()
					.loadClass(evalStratFactoryClassName)
					.newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new SailConfigException(e);
		}
	}

	@Override
	public Resource export(Model graph) {
		Resource implNode = super.export(graph);

		if (evalStratFactoryClassName != null) {
			graph.setNamespace("sb", NAMESPACE);
			graph.add(implNode, EVALUATION_STRATEGY_FACTORY,
					SimpleValueFactory.getInstance().createLiteral(evalStratFactoryClassName));
		}

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		super.parse(graph, implNode);

		try {

			Models.objectLiteral(graph.getStatements(implNode, EVALUATION_STRATEGY_FACTORY, null))
					.ifPresent(factoryClassName -> {
						setEvaluationStrategyFactoryClassName(factoryClassName.stringValue());
					});
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
