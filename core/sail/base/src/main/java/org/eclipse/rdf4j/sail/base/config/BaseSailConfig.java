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

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.eclipse.rdf4j.sail.base.config.BaseSailSchema.EVALUATION_STRATEGY_FACTORY;
import static org.eclipse.rdf4j.sail.base.config.BaseSailSchema.NAMESPACE;

import java.util.Optional;

import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

public abstract class BaseSailConfig extends AbstractSailImplConfig {

	private String evalStratFactoryClassName;

	private QueryEvaluationMode defaultQueryEvaluationMode;

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
			var factory = (EvaluationStrategyFactory) Thread.currentThread()
					.getContextClassLoader()
					.loadClass(evalStratFactoryClassName)
					.newInstance();
			return factory;
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
					literal(evalStratFactoryClassName));
		}
		getDefaultQueryEvaluationMode().ifPresent(mode -> {
			graph.setNamespace("sb", NAMESPACE);
			graph.add(implNode, BaseSailSchema.DEFAULT_QUERY_EVALUATION_MODE,
					literal(mode.getValue()));
		});

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		super.parse(graph, implNode);

		try {
			Models.objectLiteral(graph.getStatements(implNode, BaseSailSchema.DEFAULT_QUERY_EVALUATION_MODE, null))
					.ifPresent(qem -> setDefaultQueryEvaluationMode(
							QueryEvaluationMode.valueOf(qem.stringValue())));

			Models.objectLiteral(graph.getStatements(implNode, EVALUATION_STRATEGY_FACTORY, null))
					.ifPresent(factoryClassName -> {
						setEvaluationStrategyFactoryClassName(factoryClassName.stringValue());
					});
		} catch (IllegalArgumentException | ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}

	/**
	 * @return the defaultQueryEvaluationMode
	 */
	public Optional<QueryEvaluationMode> getDefaultQueryEvaluationMode() {
		return Optional.ofNullable(defaultQueryEvaluationMode);
	}

	/**
	 * @param defaultQueryEvaluationMode the defaultQueryEvaluationMode to set
	 */
	public void setDefaultQueryEvaluationMode(QueryEvaluationMode defaultQueryEvaluationMode) {
		this.defaultQueryEvaluationMode = defaultQueryEvaluationMode;
	}
}
