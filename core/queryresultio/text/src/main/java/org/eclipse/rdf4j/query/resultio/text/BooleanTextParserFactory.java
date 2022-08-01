/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text;

import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParser;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultParserFactory;

/**
 * A {@link BooleanQueryResultParserFactory} for plain text boolean result parsers.
 *
 * @author Arjohn Kampman
 */
public class BooleanTextParserFactory implements BooleanQueryResultParserFactory {

	/**
	 * Returns {@link BooleanQueryResultFormat#TEXT}.
	 */
	@Override
	public BooleanQueryResultFormat getBooleanQueryResultFormat() {
		return BooleanQueryResultFormat.TEXT;
	}

	/**
	 * Returns a new instance of BooleanTextParser.
	 */
	@Override
	public BooleanQueryResultParser getParser() {
		return new BooleanTextParser();
	}
}
