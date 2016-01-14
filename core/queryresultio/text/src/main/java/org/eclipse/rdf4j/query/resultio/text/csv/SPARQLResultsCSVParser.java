/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.text.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.opencsv.CSVReader;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.resultio.AbstractTupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;

/**
 * SPARQL Results CSV format parser.
 * 
 * @author Jeen Broekstra
 */
public class SPARQLResultsCSVParser extends AbstractTupleQueryResultParser implements TupleQueryResultParser {

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.CSV;
	}

	@Override
	public void parse(InputStream in)
		throws IOException, QueryResultParseException, TupleQueryResultHandlerException
	{
		CSVReader reader = new CSVReader(new InputStreamReader(in, Charset.forName("UTF-8")));

		List<String> bindingNames = null;

		String[] nextLine;

		try {
			while ((nextLine = reader.readNext()) != null) {
				if (bindingNames == null) {
					// header is mandatory in SPARQL CSV
					bindingNames = Arrays.asList(nextLine);
					if (handler != null) {
						handler.startQueryResult(bindingNames);
					}
				}
				else {
					// process solution
					List<Value> values = new ArrayList<Value>();
					for (String valueString : nextLine) {
						Value v = null;
						if (valueString.startsWith("_:")) {
							v = valueFactory.createBNode(valueString.substring(2));
						}
						else if (!"".equals(valueString)) {
							if (valueString.matches("^[\\+\\-]?[\\d\\.].*")) {

								IRI datatype = null;

								if (XMLDatatypeUtil.isValidInteger(valueString)) {
									if (XMLDatatypeUtil.isValidNegativeInteger(valueString)) {
										datatype = XMLSchema.NEGATIVE_INTEGER;
									}
									else {
										datatype = XMLSchema.INTEGER;
									}
								}
								else if (XMLDatatypeUtil.isValidDecimal(valueString)) {
									datatype = XMLSchema.DECIMAL;
								}
								else if (XMLDatatypeUtil.isValidDouble(valueString)) {
									datatype = XMLSchema.DOUBLE;
								}

								if (datatype != null) {
									v = valueFactory.createLiteral(valueString, datatype);
								}
								else {
									v = valueFactory.createLiteral(valueString);
								}
							}
							else {
								try {
									v = valueFactory.createIRI(valueString);
								}
								catch (IllegalArgumentException e) {
									v = valueFactory.createLiteral(valueString);
								}
							}
						}
						values.add(v);
					}

					BindingSet bindingSet = new ListBindingSet(bindingNames,
							values.toArray(new Value[values.size()]));
					if (handler != null) {
						handler.handleSolution(bindingSet);
					}
				}
			}

			if (bindingNames != null && handler != null) {
				handler.endQueryResult();
			}
		}
		finally {
			reader.close();
		}
	}
}
