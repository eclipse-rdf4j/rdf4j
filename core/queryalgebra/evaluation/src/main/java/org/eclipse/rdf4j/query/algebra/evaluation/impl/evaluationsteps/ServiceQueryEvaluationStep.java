/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.SingletonIteration;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;
import org.eclipse.rdf4j.query.impl.MapBindingSet;

public final class ServiceQueryEvaluationStep implements QueryEvaluationStep {
	private final Service service;
	private final Var serviceRef;
	private final FederatedServiceResolver serviceResolver;

	public ServiceQueryEvaluationStep(Service service, Var serviceRef, FederatedServiceResolver serviceResolver) {
		this.service = service;
		this.serviceRef = serviceRef;
		this.serviceResolver = serviceResolver;
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		boolean runtimeTelemetryEnabled = isRuntimeTelemetryEnabled(service);
		String serviceUri;
		if (serviceRef.hasValue()) {
			serviceUri = serviceRef.getValue().stringValue();
		} else {
			if (bindings != null && bindings.getValue(serviceRef.getName()) != null) {
				serviceUri = bindings.getBinding(serviceRef.getName()).getValue().stringValue();
			} else {
				throw new QueryEvaluationException("SERVICE variables must be bound at evaluation time.");
			}
		}

		long started = runtimeTelemetryEnabled ? System.nanoTime() : 0L;
		if (runtimeTelemetryEnabled) {
			incrementLongMetric(service, TelemetryMetricNames.REMOTE_REQUEST_COUNT_ACTUAL);
		}
		try {
			FederatedService fs = serviceResolver.getService(serviceUri);

			// create a copy of the free variables, and remove those for which
			// bindings are available (we can set them as constraints!)
			Set<String> freeVars = new HashSet<>(service.getServiceVars());
			freeVars.removeAll(bindings.getBindingNames());

			// Get bindings from values pre-bound into variables.
			MapBindingSet allBindings = new MapBindingSet();
			for (Binding binding : bindings) {
				allBindings.setBinding(binding.getName(), binding.getValue());
			}

			Set<Var> boundVars = getBoundVariables(service);
			for (Var boundVar : boundVars) {
				freeVars.remove(boundVar.getName());
				allBindings.setBinding(boundVar.getName(), boundVar.getValue());
			}
			bindings = allBindings;
			if (runtimeTelemetryEnabled) {
				addLongMetric(service, TelemetryMetricNames.REMOTE_BYTES_SENT_ACTUAL,
						estimateRequestBytes(service, bindings, freeVars));
			}

			String baseUri = service.getBaseURI();

			// special case: no free variables => perform ASK query
			if (freeVars.isEmpty()) {
				if (runtimeTelemetryEnabled) {
					incrementLongMetric(service, TelemetryMetricNames.REMOTE_ASK_REQUEST_COUNT_ACTUAL);
				}
				boolean exists = fs.ask(service, bindings, baseUri);
				if (runtimeTelemetryEnabled) {
					addLongMetric(service, TelemetryMetricNames.REMOTE_BYTES_RECEIVED_ACTUAL, exists ? 4 : 5);
				}

				// check if triples are available (with inserted bindings)
				if (exists) {
					return new SingletonIteration<>(bindings);
				} else {
					return EMPTY_ITERATION;
				}

			}

			// otherwise: perform a SELECT query
			if (runtimeTelemetryEnabled) {
				incrementLongMetric(service, TelemetryMetricNames.REMOTE_SELECT_REQUEST_COUNT_ACTUAL);
			}
			CloseableIteration<BindingSet> results = fs.select(service, freeVars, bindings, baseUri);
			if (!runtimeTelemetryEnabled) {
				return results;
			}
			return trackResponseBytes(service, results);

		} catch (RuntimeException e) {
			if (runtimeTelemetryEnabled) {
				incrementLongMetric(service, TelemetryMetricNames.REMOTE_ERROR_COUNT_ACTUAL);
				if (isTimeoutException(e)) {
					incrementLongMetric(service, TelemetryMetricNames.REMOTE_TIMEOUT_COUNT_ACTUAL);
				}
			}
			// suppress exceptions if silent
			if (service.isSilent()) {
				return new SingletonIteration<>(bindings);
			} else {
				throw e;
			}
		} finally {
			if (runtimeTelemetryEnabled) {
				recordRequestLatency(service, started);
			}
		}
	}

	private Set<Var> getBoundVariables(Service service) {
		BoundVarVisitor visitor = new BoundVarVisitor();
		visitor.meet(service);
		return visitor.boundVars;
	}

	private static class BoundVarVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		final Set<Var> boundVars = new HashSet<>();

		private BoundVarVisitor() {
			super(true);
		}

		@Override
		public void meet(Var var) {
			if (var.hasValue()) {
				boundVars.add(var);
			}
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
						estimateBindingSetBytes(sourceObject));
				return sourceObject;
			}
		};
	}

	private static long estimateRequestBytes(Service service, BindingSet bindings, Set<String> freeVars) {
		long bytes = estimateUtf8Bytes(service.getServiceExpressionString());
		bytes += estimateUtf8Bytes(bindings == null ? null : bindings.toString());
		bytes += estimateUtf8Bytes(freeVars == null || freeVars.isEmpty() ? null : freeVars.toString());
		return bytes;
	}

	private static long estimateBindingSetBytes(BindingSet bindingSet) {
		return estimateUtf8Bytes(bindingSet == null ? null : bindingSet.toString());
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
