/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring.support;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.MultiIRI;

/**
 * Interface for making different approaches of obtaining new UUIDs pluggable into the {@link RDF4JTemplate
 * Rdf4JTemplate}. The {@link org.eclipse.rdf4j.spring.RDF4JConfig Rdf4JConfig}.
 *
 * <p>
 * For more information, see {@link org.eclipse.rdf4j.spring.uuidsource}.
 */
public interface UUIDSource {
	IRI nextUUID();

	default IRI toURNUUID(String uuid) {
		return new MultiIRI("urn:uuid:", uuid);
	}
}
