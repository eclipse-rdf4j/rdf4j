/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.monitoring.jmx;

import java.util.List;

public interface FederationStatusMBean {

	public List<String> getFederationMembersDescription();
	
	public int getIdleJoinWorkerThreads();
	
	public int getTotalJoinWorkerThreads();
	
	public int getIdleUnionWorkerThreads();
	
	public int getTotalUnionWorkerThreads();
	
	public int getNumberOfScheduledJoinTasks();
	
	public int getNumberOfScheduledUnionTasks();
}
