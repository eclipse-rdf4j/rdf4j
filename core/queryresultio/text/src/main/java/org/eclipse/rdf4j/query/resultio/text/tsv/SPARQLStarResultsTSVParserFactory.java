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
package org.eclipse.rdf4j.query.resultio.text.tsv;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserFactory;

/**
 * {@link TupleQueryResultParserFactory} for creating instances of {@link SPARQLStarResultsTSVParser}.
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsTSVParserFactory implements TupleQueryResultParserFactory {
	/**
	 * Returns {@link TupleQueryResultFormat#TSV_STAR}.
	 */
	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.TSV_STAR;
	}

	/**
	 * Returns a new instance of {@link SPARQLStarResultsTSVParser}.
	 */
	@Override
	public TupleQueryResultParser getParser() {
		return new SPARQLStarResultsTSVParser();
	}
}
