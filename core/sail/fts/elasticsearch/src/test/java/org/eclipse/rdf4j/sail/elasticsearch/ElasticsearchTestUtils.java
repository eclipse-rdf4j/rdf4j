/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import java.util.concurrent.Semaphore;

public class ElasticsearchTestUtils {

	/**
	 * ElasticSearch shares/exposes a number of services at the JVM level. This Semaphore ensures that only a
	 * single one of our tests is attempting to work with the ElasticSearch APIs at one time.
	 */
	public static Semaphore TEST_SEMAPHORE = new Semaphore(1);

}
