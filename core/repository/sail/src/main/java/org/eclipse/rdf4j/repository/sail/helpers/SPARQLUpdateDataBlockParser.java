/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.helpers;

import org.eclipse.rdf4j.model.ValueFactory;

/**
 *
 * @deprecated since 3.2.2. Use {@link org.eclipse.rdf4j.query.parser.sparql.SPARQLUpdateDataBlockParser} instead.
 *
 */
@Deprecated
public class SPARQLUpdateDataBlockParser extends org.eclipse.rdf4j.query.parser.sparql.SPARQLUpdateDataBlockParser {

	@Deprecated
	public SPARQLUpdateDataBlockParser() {
		super();
	}

	@Deprecated
	public SPARQLUpdateDataBlockParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

}
