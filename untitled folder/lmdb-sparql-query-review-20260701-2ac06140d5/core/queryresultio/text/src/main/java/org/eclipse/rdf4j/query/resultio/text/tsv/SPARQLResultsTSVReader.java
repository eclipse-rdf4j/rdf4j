/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.tsv;

import java.io.IOException;
import java.io.Reader;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

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
		if (line == null) {
			return null;
		}
		String[] fields = line.split("\t", -1);
		try {
			validateResult(fields, linesRead);
		} catch (CsvValidationException ex) {
			throw new IOException(ex);
		}
		return fields;
	}
}
