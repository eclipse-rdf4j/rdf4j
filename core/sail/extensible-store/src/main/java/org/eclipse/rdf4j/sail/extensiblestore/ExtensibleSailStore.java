/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.DynamicStatistics;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.EvaluationStatisticsEnum;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.EvaluationStatisticsWrapper;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.ExtensibleEvaluationStatistics;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatementHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class ExtensibleSailStore implements SailStore {

	private static final Logger logger = LoggerFactory.getLogger(ExtensibleSailStore.class);
	public static final int EVALUATION_STATISTICS_STALENESS_CHECK_INTERVAL = 1000 * 60;

	private final ExtensibleSailSource sailSource;
	private final ExtensibleSailSource sailSourceInferred;
	private final EvaluationStatisticsEnum evaluationStatisticsEnum;
	private ExtensibleEvaluationStatistics evaluationStatistics;
	private Thread evaluationStatisticsMaintainerThread;
	private final DataStructureInterface dataStructure;
	private volatile boolean closed;

	public ExtensibleSailStore(DataStructureInterface dataStructure,
			NamespaceStoreInterface namespaceStore, EvaluationStatisticsEnum evaluationStatisticsEnum,
			ExtensibleStatementHelper extensibleStatementHelper) {

		this.evaluationStatisticsEnum = evaluationStatisticsEnum;
		this.evaluationStatistics = evaluationStatisticsEnum.getInstance(this);

		if (evaluationStatistics instanceof DynamicStatistics) {
			dataStructure = new EvaluationStatisticsWrapper(dataStructure, (DynamicStatistics) evaluationStatistics);
			startEvaluationStatisticsMaintainerThread();
		}

		this.dataStructure = dataStructure;
		sailSource = new ExtensibleSailSource(dataStructure, namespaceStore, false, extensibleStatementHelper);
		sailSourceInferred = new ExtensibleSailSource(dataStructure, namespaceStore, true, extensibleStatementHelper);

	}

	synchronized private void startEvaluationStatisticsMaintainerThread() {
		if (!closed) {
			return;
		}
		evaluationStatisticsMaintainerThread = new Thread(new EvaluationStatisticsThread());
		evaluationStatisticsMaintainerThread.setDaemon(true);
		evaluationStatisticsMaintainerThread.start();
	}

	@Override
	synchronized public void close() throws SailException {
		closed = true;
		if (evaluationStatisticsMaintainerThread != null) {
			evaluationStatisticsMaintainerThread.interrupt();
		}
		sailSource.close();
		sailSourceInferred.close();
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return evaluationStatistics;
	}

	@Override
	public SailSource getExplicitSailSource() {
		return sailSource;
	}

	@Override
	public SailSource getInferredSailSource() {
		return sailSourceInferred;
	}

	public void init() {
		sailSource.init();
		sailSourceInferred.init();
	}

	private void startRecalculateStatistics() {

		logger.info("Recalculating stats: started");
		DynamicStatistics instance = (DynamicStatistics) evaluationStatisticsEnum.getInstance(this);

		addToStats(instance, dataStructure.getStatements(null, null, null, false));
		addToStats(instance, dataStructure.getStatements(null, null, null, true));

		((EvaluationStatisticsWrapper) dataStructure).setEvaluationStatistics(instance);

		evaluationStatistics = (ExtensibleEvaluationStatistics) instance;
		logger.info("Recalculating stats: complete");

	}

	private void addToStats(DynamicStatistics instance,
			CloseableIteration<? extends ExtensibleStatement, SailException> statements) {

		long estimatedSize = dataStructure.getEstimatedSize();

		long counter = 0;
		while (statements.hasNext()) {
			ExtensibleStatement next = statements.next();
			instance.add(next);

			if (Thread.interrupted() || closed) {
				return;
			}

			if (++counter % 100000 == 0) {
				logger.info("Recalculating stats: {}%", Math.round(100.0 / estimatedSize * counter));
			}
		}

	}

	class EvaluationStatisticsThread implements Runnable {

		@Override
		public void run() {

			try {
				try {
					Thread.sleep(EVALUATION_STATISTICS_STALENESS_CHECK_INTERVAL);
				} catch (InterruptedException e) {
					return;
				}

				if (closed) {
					return;
				}

				long estimatedSize = dataStructure.getEstimatedSize();

				if (estimatedSize > 1000) {
					double staleness = ((DynamicStatistics) evaluationStatistics).staleness(estimatedSize);

					if (staleness > 0.2) {
						long formattedStaleness = Math.round(staleness * 100);
						logger.info("Evaluation statistics is stale ({}%) and needs to be recalculated",
								formattedStaleness);
						startRecalculateStatistics();
					}

				}

			} catch (Exception e) {
				if (!(closed || Thread.interrupted())) {
					throw new RuntimeException(e);
				}

			} finally {
				if (!Thread.interrupted()) {
					startEvaluationStatisticsMaintainerThread();
				}
			}

		}
	}

}
