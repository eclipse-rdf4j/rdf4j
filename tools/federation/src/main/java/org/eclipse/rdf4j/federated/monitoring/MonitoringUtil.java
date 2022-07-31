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

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.monitoring.MonitoringImpl.MonitoringInformation;

public class MonitoringUtil {

	public static void printMonitoringInformation(FederationContext federationContext) {

		MonitoringService ms = getMonitoringService(federationContext);

		System.out.println("### Request monitoring: ");
		for (MonitoringInformation m : ms.getAllMonitoringInformation()) {
			System.out.println("\t" + m.toString());
		}
	}

	private static MonitoringService getMonitoringService(FederationContext federationContext)
			throws FedXRuntimeException {
		Monitoring m = federationContext.getMonitoringService();
		if (m instanceof MonitoringService) {
			return (MonitoringService) m;
		}
		throw new FedXRuntimeException("Monitoring is currently disabled for this system.");
	}

}
