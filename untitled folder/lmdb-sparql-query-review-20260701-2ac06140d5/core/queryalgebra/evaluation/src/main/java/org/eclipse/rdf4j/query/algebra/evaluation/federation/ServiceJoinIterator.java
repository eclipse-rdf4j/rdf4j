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
package org.eclipse.rdf4j.query.algebra.evaluation.federation;

import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;
import org.eclipse.rdf4j.repository.sparql.federation.JoinExecutorBase;

/**
 * Iterator for efficient SERVICE evaluation (vectored). SERVICE is the right handside argument of this join.
 *
 * @author Andreas Schwarte
 */
public class ServiceJoinIterator extends JoinExecutorBase<BindingSet> {

	protected Service service;

	protected EvaluationStrategy strategy;

	/**
	 * Construct a service join iteration to use vectored evaluation. The constructor automatically starts evaluation.
	 *
	 * @param leftIter
	 * @param service
	 * @param bindings
	 * @param strategy
	 * @throws QueryEvaluationException
	 */
	public ServiceJoinIterator(CloseableIteration<BindingSet> leftIter, Service service,
			BindingSet bindings, EvaluationStrategy strategy) throws QueryEvaluationException {
		super(leftIter, service, bindings);
		this.service = service;
		this.strategy = strategy;
		run();
	}

	@Override
	protected void handleBindings() throws Exception {
		boolean runtimeTelemetryEnabled = isRuntimeTelemetryEnabled(service);
		boolean fallbackEvaluation = false;
		try {
			Var serviceRef = service.getServiceRef();
			fallbackEvaluation = !serviceRef.hasValue();

			String serviceUri;
			if (serviceRef.hasValue()) {
				serviceUri = serviceRef.getValue().stringValue();
			} else {
				// case 2: the service ref is not defined beforehand
				// => use a fallback to the naive evaluation.
				// exceptions occurring here must NOT be silenced!
				while (!isClosed() && leftIter.hasNext()) {
					BindingSet leftBindings = leftIter.next();
					if (runtimeTelemetryEnabled) {
						incrementLongMetric(service, TelemetryMetricNames.REMOTE_EVALUATE_REQUEST_COUNT_ACTUAL);
					}
					CloseableIteration<BindingSet> result = strategy.evaluate(service, leftBindings);
					addResult(result);
				}
				return;
			}

			// use vectored evaluation
			if (runtimeTelemetryEnabled) {
				incrementLongMetric(service, TelemetryMetricNames.REMOTE_REQUEST_COUNT_ACTUAL);
				incrementLongMetric(service, TelemetryMetricNames.REMOTE_EVALUATE_REQUEST_COUNT_ACTUAL);
				addLongMetric(service, TelemetryMetricNames.REMOTE_BYTES_SENT_ACTUAL,
						estimateUtf8Bytes(service.getServiceExpressionString()));
			}
			FederatedService fs = strategy.getService(serviceUri);
			long started = runtimeTelemetryEnabled ? System.nanoTime() : 0L;
			try {
				CloseableIteration<BindingSet> result = fs.evaluate(service, leftIter, service.getBaseURI());
				if (runtimeTelemetryEnabled) {
					addResult(trackResponseBytes(service, result));
				} else {
					addResult(result);
				}
			} finally {
				if (runtimeTelemetryEnabled) {
					recordRequestLatency(service, started);
				}
			}
		} catch (Exception e) {
			if (runtimeTelemetryEnabled && !fallbackEvaluation) {
				incrementLongMetric(service, TelemetryMetricNames.REMOTE_ERROR_COUNT_ACTUAL);
				if (isTimeoutException(e)) {
					incrementLongMetric(service, TelemetryMetricNames.REMOTE_TIMEOUT_COUNT_ACTUAL);
				}
			}
			throw e;
		}
	}

	private static void incrementLongMetric(Service service, String metricName) {
		addLongMetric(service, metricName, 1L);
	}

	private static void addLongMetric(Service service, String metricName, long delta) {
		if (!isRuntimeTelemetryEnabled(service) || delta <= 0) {
			return;
		}
		service.setLongMetricActual(metricName, Math.max(0L, service.getLongMetricActual(metricName)) + delta);
	}

	private static void recordRequestLatency(Service service, long startedNanos) {
		long latencyNanos = Math.max(0L, System.nanoTime() - startedNanos);
		addLongMetric(service, TelemetryMetricNames.REMOTE_LATENCY_TOTAL_NANOS_ACTUAL, latencyNanos);
		updateLatencyQuantileEstimate(service, TelemetryMetricNames.REMOTE_LATENCY_P50_NANOS_ACTUAL, 0.50,
				latencyNanos);
		updateLatencyQuantileEstimate(service, TelemetryMetricNames.REMOTE_LATENCY_P95_NANOS_ACTUAL, 0.95,
				latencyNanos);
	}

	private static void updateLatencyQuantileEstimate(Service service, String metricName, double quantile,
			long sampleNanos) {
		if (sampleNanos <= 0L) {
			return;
		}

		double currentEstimate = service.getDoubleMetricActual(metricName);
		if (currentEstimate < 0D) {
			service.setDoubleMetricActual(metricName, sampleNanos);
			return;
		}

		long requestCount = Math.max(1L, service.getLongMetricActual(TelemetryMetricNames.REMOTE_REQUEST_COUNT_ACTUAL));
		double alpha = 1D / Math.min(2_000D, requestCount);
		double indicator = sampleNanos <= currentEstimate ? 1D : 0D;
		double step = Math.max(1D, Math.abs(sampleNanos - currentEstimate));
		double updated = currentEstimate + alpha * (quantile - indicator) * step;
		service.setDoubleMetricActual(metricName, Math.max(0D, updated));
	}

	private static CloseableIteration<BindingSet> trackResponseBytes(Service service,
			CloseableIteration<BindingSet> delegate) {
		return new ConvertingIteration<BindingSet, BindingSet>(delegate) {
			@Override
			protected BindingSet convert(BindingSet sourceObject) {
				addLongMetric(service, TelemetryMetricNames.REMOTE_BYTES_RECEIVED_ACTUAL,
						estimateUtf8Bytes(sourceObject == null ? null : sourceObject.toString()));
				return sourceObject;
			}
		};
	}

	private static long estimateRequestBytes(Service service, BindingSet bindingSet) {
		return estimateUtf8Bytes(service.getServiceExpressionString())
				+ estimateUtf8Bytes(bindingSet == null ? null : bindingSet.toString());
	}

	private static long estimateUtf8Bytes(String value) {
		if (value == null || value.isEmpty()) {
			return 0L;
		}
		return value.getBytes(StandardCharsets.UTF_8).length;
	}

	private static boolean isRuntimeTelemetryEnabled(Service service) {
		return service != null && service.isRuntimeTelemetryEnabled();
	}

	private static boolean isTimeoutException(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			String simpleName = current.getClass().getSimpleName();
			if (simpleName.contains("Timeout")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
