/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.monitoring;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.monitoring.MonitoringImpl.MonitoringInformation;
import org.eclipse.rdf4j.federated.monitoring.jmx.FederationStatus;

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
		if (m instanceof MonitoringService)
			return (MonitoringService) m;
		throw new FedXRuntimeException("Monitoring is currently disabled for this system.");
	}

	/**
	 * Flag is set to true to once initialized
	 */
	private static boolean JMX_initialized = false;

	/**
	 * Initialize JMX monitoring using the systems MBeanServer. JMX is only initialized if it has not been initialized
	 * before.
	 * 
	 * @throws Exception
	 */
	public static void initializeJMXMonitoring(FederationContext federationContext) throws Exception {
		if (JMX_initialized)
			return;
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName monitoring = new ObjectName("org.eclipse.rdf4j.federated:type=FederationStatus");
		mbs.registerMBean(new FederationStatus(federationContext), monitoring);
		JMX_initialized = true;
	}
}
