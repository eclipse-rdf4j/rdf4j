/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.tsv;

import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.Reader;

/**
 * This reader respects the TSV semantics of RDF4J and does absolutely no processing except for splitting the line on
 * horizontal tabulator characters.
 */
public class SPARQLResultsTSVReader extends CSVReader {
	public SPARQLResultsTSVReader(Reader reader) {
		super(reader);
	}

	@Override
	public String[] readNext() throws IOException {
		String line = getNextLine();
		return line == null ? null : validateResult(line.split("\t", -1));
	}
}
