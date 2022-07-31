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

package org.eclipse.rdf4j.spring.support;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Interface for making different approaches of obtaining new UUIDs pluggable into the {@link RDF4JTemplate
 * Rdf4JTemplate}. The {@link org.eclipse.rdf4j.spring.RDF4JConfig Rdf4JConfig}.
 *
 * <p>
 * For more information, see {@link org.eclipse.rdf4j.spring.uuidsource}.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 *
 */
public interface UUIDSource {
	IRI nextUUID();

	default IRI toURNUUID(String uuid) {
		return SimpleValueFactory.getInstance().createIRI("urn:uuid:", uuid);
	}
}
