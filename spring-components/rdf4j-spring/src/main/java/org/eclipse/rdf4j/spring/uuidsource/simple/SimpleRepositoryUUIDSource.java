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

package org.eclipse.rdf4j.spring.uuidsource.simple;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.eclipse.rdf4j.spring.support.UUIDSource;
import org.eclipse.rdf4j.spring.util.QueryResultUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class SimpleRepositoryUUIDSource implements UUIDSource {

	@Autowired
	RDF4JTemplate rdf4JTemplate;

	@Override
	public IRI nextUUID() {
		return rdf4JTemplate
				.tupleQuery("SELECT (UUID() as ?id) WHERE {}")
				.evaluateAndConvert()
				.toSingleton(b -> QueryResultUtils.getIRI(b, "id"));
	}
}
