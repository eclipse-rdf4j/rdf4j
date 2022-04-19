/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.endpoint;

/**
 * Classify endpoints into remote or local ones.
 *
 * @author Andreas Schwarte
 */
public enum EndpointClassification {
	Local,
	Remote
}
