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
import static org.eclipse.rdf4j.sail.base.config.BaseSailSchema.DEFAULT_QUERY_EVALUATION_MODE;
import static org.eclipse.rdf4j.sail.base.config.BaseSailSchema.EVALUATION_STRATEGY_FACTORY;
import static org.eclipse.rdf4j.sail.base.config.BaseSailSchema.SLOW_QUERY_LOG_FILE;
import static org.eclipse.rdf4j.sail.base.config.BaseSailSchema.SLOW_QUERY_LOG_FIRST_RESULT_THRESHOLD_SECONDS;
import static org.eclipse.rdf4j.sail.base.config.BaseSailSchema.SLOW_QUERY_LOG_THRESHOLD_SECONDS;

import java.util.Optional;

import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Configurations;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

public abstract class BaseSailConfig extends AbstractSailImplConfig {

	private String evalStratFactoryClassName;

	private QueryEvaluationMode defaultQueryEvaluationMode;
	private long slowQueryLogThresholdSeconds;
	private long slowQueryLogFirstResultThresholdSeconds;
	private String slowQueryLogFile;

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
		if (Configurations.useLegacyConfig()) {
			return exportLegacy(graph);
		}

		Resource implNode = super.export(graph);

		if (evalStratFactoryClassName != null) {
			graph.add(implNode, CONFIG.Sail.evaluationStrategyFactory, literal(evalStratFactoryClassName));

		}
		getDefaultQueryEvaluationMode().ifPresent(mode -> {
			graph.add(implNode, CONFIG.Sail.defaultQueryEvaluationMode, literal(mode.getValue()));
		});
		if (slowQueryLogThresholdSeconds != 0) {
			graph.add(implNode, CONFIG.Sail.slowQueryLogThresholdSeconds, literal(slowQueryLogThresholdSeconds));
		}
		if (slowQueryLogFirstResultThresholdSeconds != 0) {
			graph.add(implNode, CONFIG.Sail.slowQueryLogFirstResultThresholdSeconds,
					literal(slowQueryLogFirstResultThresholdSeconds));
		}
		if (slowQueryLogFile != null && !slowQueryLogFile.isBlank()) {
			graph.add(implNode, CONFIG.Sail.slowQueryLogFile, literal(slowQueryLogFile));
		}

		return implNode;
	}

	private Resource exportLegacy(Model graph) {
		Resource implNode = super.export(graph);

		if (evalStratFactoryClassName != null) {
			graph.add(implNode, EVALUATION_STRATEGY_FACTORY, literal(evalStratFactoryClassName));

		}
		getDefaultQueryEvaluationMode().ifPresent(mode -> {
			graph.add(implNode, DEFAULT_QUERY_EVALUATION_MODE, literal(mode.getValue()));
		});
		if (slowQueryLogThresholdSeconds != 0) {
			graph.add(implNode, SLOW_QUERY_LOG_THRESHOLD_SECONDS, literal(slowQueryLogThresholdSeconds));
		}
		if (slowQueryLogFirstResultThresholdSeconds != 0) {
			graph.add(implNode, SLOW_QUERY_LOG_FIRST_RESULT_THRESHOLD_SECONDS,
					literal(slowQueryLogFirstResultThresholdSeconds));
		}
		if (slowQueryLogFile != null && !slowQueryLogFile.isBlank()) {
			graph.add(implNode, SLOW_QUERY_LOG_FILE, literal(slowQueryLogFile));
		}

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		super.parse(graph, implNode);

		try {
			Configurations.getLiteralValue(graph, implNode, CONFIG.Sail.defaultQueryEvaluationMode,
					DEFAULT_QUERY_EVALUATION_MODE)
					.ifPresent(qem -> setDefaultQueryEvaluationMode(
							QueryEvaluationMode.valueOf(qem.stringValue())));

			Configurations.getLiteralValue(graph, implNode, CONFIG.Sail.evaluationStrategyFactory,
					EVALUATION_STRATEGY_FACTORY)
					.ifPresent(factoryClassName -> {
						setEvaluationStrategyFactoryClassName(factoryClassName.stringValue());
					});
			Configurations.getLiteralValue(graph, implNode, CONFIG.Sail.slowQueryLogThresholdSeconds,
					SLOW_QUERY_LOG_THRESHOLD_SECONDS)
					.ifPresent(threshold -> setSlowQueryLogThresholdSeconds(threshold.longValue()));
			Configurations.getLiteralValue(graph, implNode, CONFIG.Sail.slowQueryLogFirstResultThresholdSeconds,
					SLOW_QUERY_LOG_FIRST_RESULT_THRESHOLD_SECONDS)
					.ifPresent(threshold -> setSlowQueryLogFirstResultThresholdSeconds(threshold.longValue()));
			Configurations.getLiteralValue(graph, implNode, CONFIG.Sail.slowQueryLogFile,
					SLOW_QUERY_LOG_FILE)
					.ifPresent(logFile -> setSlowQueryLogFile(logFile.stringValue()));
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

	public long getSlowQueryLogThresholdSeconds() {
		return slowQueryLogThresholdSeconds;
	}

	public void setSlowQueryLogThresholdSeconds(long slowQueryLogThresholdSeconds) {
		this.slowQueryLogThresholdSeconds = slowQueryLogThresholdSeconds;
	}

	public long getSlowQueryLogFirstResultThresholdSeconds() {
		return slowQueryLogFirstResultThresholdSeconds;
	}

	public void setSlowQueryLogFirstResultThresholdSeconds(long slowQueryLogFirstResultThresholdSeconds) {
		this.slowQueryLogFirstResultThresholdSeconds = slowQueryLogFirstResultThresholdSeconds;
	}

	public String getSlowQueryLogFile() {
		return slowQueryLogFile;
	}

	public void setSlowQueryLogFile(String slowQueryLogFile) {
		this.slowQueryLogFile = slowQueryLogFile;
	}
}
