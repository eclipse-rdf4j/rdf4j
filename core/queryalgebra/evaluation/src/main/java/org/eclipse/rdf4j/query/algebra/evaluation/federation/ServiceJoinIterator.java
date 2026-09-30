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
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.LongConsumer;

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
	private static final String DEFERRED_TELEMETRY_METADATA = "org.eclipse.rdf4j.query.algebra.evaluation.impl.deferredServiceTelemetry";
	private static final int REQUEST = 0;
	private static final int EVALUATE = 3;
	private static final int ERROR = 4;
	private static final int TIMEOUT = 5;
	private static final int BYTES_SENT = 6;
	private static final int BYTES_RECEIVED = 7;

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
		Object[] deferredTelemetry = deferredTelemetry(service);
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
						addLongMetric(service, deferredTelemetry, EVALUATE,
								TelemetryMetricNames.REMOTE_EVALUATE_REQUEST_COUNT_ACTUAL, 1L);
					}
					CloseableIteration<BindingSet> result = strategy.evaluate(service, leftBindings);
					addResult(result);
				}
				return;
			}

			// use vectored evaluation
			if (runtimeTelemetryEnabled) {
				addLongMetric(service, deferredTelemetry, REQUEST,
						TelemetryMetricNames.REMOTE_REQUEST_COUNT_ACTUAL, 1L);
				addLongMetric(service, deferredTelemetry, EVALUATE,
						TelemetryMetricNames.REMOTE_EVALUATE_REQUEST_COUNT_ACTUAL, 1L);
				addLongMetric(service, deferredTelemetry, BYTES_SENT,
						TelemetryMetricNames.REMOTE_BYTES_SENT_ACTUAL,
						estimateUtf8Bytes(service.getServiceExpressionString()));
			}
			FederatedService fs = strategy.getService(serviceUri);
			long started = runtimeTelemetryEnabled ? System.nanoTime() : 0L;
			try {
				CloseableIteration<BindingSet> result = fs.evaluate(service, leftIter, service.getBaseURI());
				if (runtimeTelemetryEnabled) {
					addResult(trackResponseBytes(service, deferredTelemetry, result));
				} else {
					addResult(result);
				}
			} finally {
				if (runtimeTelemetryEnabled) {
					recordRequestLatency(service, deferredTelemetry, started);
				}
			}
		} catch (Exception e) {
			if (runtimeTelemetryEnabled && !fallbackEvaluation) {
				addLongMetric(service, deferredTelemetry, ERROR, TelemetryMetricNames.REMOTE_ERROR_COUNT_ACTUAL, 1L);
				if (isTimeoutException(e)) {
					addLongMetric(service, deferredTelemetry, TIMEOUT,
							TelemetryMetricNames.REMOTE_TIMEOUT_COUNT_ACTUAL, 1L);
				}
			}
			throw e;
		}
	}

	private static void addLongMetric(Service service, String metricName, long delta) {
		if (!isRuntimeTelemetryEnabled(service) || delta <= 0) {
			return;
		}
		service.setLongMetricActual(metricName, Math.max(0L, service.getLongMetricActual(metricName)) + delta);
	}

	private static void addLongMetric(Service service, Object[] deferredTelemetry, int counter, String metricName,
			long delta) {
		if (deferredTelemetry != null && delta > 0L) {
			AtomicLongArray counters = (AtomicLongArray) deferredTelemetry[0];
			while (true) {
				long current = counters.get(counter);
				long next = current > Long.MAX_VALUE - delta ? Long.MAX_VALUE : current + delta;
				if (counters.compareAndSet(counter, current, next)) {
					return;
				}
			}
		}
		addLongMetric(service, metricName, delta);
	}

	private static void recordRequestLatency(Service service, Object[] deferredTelemetry, long startedNanos) {
		long latencyNanos = Math.max(0L, System.nanoTime() - startedNanos);
		if (deferredTelemetry != null) {
			((LongConsumer) deferredTelemetry[1]).accept(latencyNanos);
			return;
		}
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

	private static CloseableIteration<BindingSet> trackResponseBytes(Service service, Object[] deferredTelemetry,
			CloseableIteration<BindingSet> delegate) {
		return new ConvertingIteration<BindingSet, BindingSet>(delegate) {
			@Override
			protected BindingSet convert(BindingSet sourceObject) {
				addLongMetric(service, deferredTelemetry, BYTES_RECEIVED,
						TelemetryMetricNames.REMOTE_BYTES_RECEIVED_ACTUAL,
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

	private static Object[] deferredTelemetry(Service service) {
		Object state = service == null ? null : service.getQueryModelMetadata(DEFERRED_TELEMETRY_METADATA);
		if (!(state instanceof Object[] values) || values.length < 2
				|| !(values[0] instanceof AtomicLongArray) || !(values[1] instanceof LongConsumer)) {
			return null;
		}
		return values;
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
