/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
