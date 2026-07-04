/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.monitoring;

import org.eclipse.rdf4j.federated.FedXConfig;

public class MonitoringFactory {

	/**
	 * Create a new monitoring instance depending on {@link FedXConfig#isEnableMonitoring()}
	 *
	 * @return the {@link Monitoring} instance
	 */
	public static Monitoring createMonitoring(FedXConfig config) {

		if (config.isEnableMonitoring()) {
			return new MonitoringImpl(config);
		}
		return new NoopMonitoringImpl();
	}
}
