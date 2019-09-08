/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package com.fluidops.fedx.monitoring;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.exception.FedXRuntimeException;
import com.fluidops.fedx.monitoring.MonitoringImpl.MonitoringInformation;
import com.fluidops.fedx.monitoring.jmx.FederationStatus;


public class MonitoringUtil
{

	public static void printMonitoringInformation() {
		
		MonitoringService ms = getMonitoringService();

		System.out.println("### Request monitoring: ");
		for (MonitoringInformation m : ms.getAllMonitoringInformation()) {
			System.out.println("\t" + m.toString());
		}
	}
	
	
	public static MonitoringService getMonitoringService() throws FedXRuntimeException {
		Monitoring m = FederationManager.getMonitoringService();
		if (m instanceof MonitoringService)
			return (MonitoringService)m;
		throw new FedXRuntimeException("Monitoring is currently disabled for this system.");
	}
	
	
	/**
	 * Flag is set to true to once initialized
	 */
	private static boolean JMX_initialized = false;
	
	/**
	 * Initialize JMX monitoring using the systems MBeanServer. JMX
	 * is only initialized if it has not been initialized before.
	 * 
	 * @throws Exception
	 */
	public static void initializeJMXMonitoring() throws Exception
	{
		if (JMX_initialized)
			return;
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName monitoring = new ObjectName("com.fluidops.fedx:type=FederationStatus");
		mbs.registerMBean(new FederationStatus(), monitoring);
		JMX_initialized = true;
	}
}
