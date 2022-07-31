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
package org.eclipse.rdf4j.http.protocol.transaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;

import org.eclipse.rdf4j.common.xml.SimpleSAXParser;
import org.eclipse.rdf4j.http.protocol.transaction.operations.TransactionOperation;
import org.xml.sax.SAXException;

public class TransactionReader {

	/**
	 * parse the transaction from the serialization
	 *
	 * @throws SAXException If the SimpleSAXParser was unable to create an XMLReader or if the XML is faulty.
	 * @throws IOException  If IO problems during parsing.
	 */
	public Collection<TransactionOperation> parse(InputStream in) throws SAXException, IOException {
		SimpleSAXParser saxParser = new SimpleSAXParser();
		saxParser.setPreserveWhitespace(true);

		TransactionSAXParser handler = new TransactionSAXParser();
		saxParser.setListener(handler);
		saxParser.parse(in);
		return handler.getTxn();
	}

	/**
	 * parse the transaction from the serialization
	 *
	 * @throws SAXException If the SimpleSAXParser was unable to create an XMLReader or if the XML is faulty.
	 * @throws IOException  If IO problems during parsing.
	 */
	public Collection<TransactionOperation> parse(Reader in) throws SAXException, IOException {
		SimpleSAXParser saxParser = new SimpleSAXParser();
		TransactionSAXParser handler = new TransactionSAXParser();
		saxParser.setPreserveWhitespace(true);
		saxParser.setListener(handler);
		saxParser.parse(in);
		return handler.getTxn();
	}
}
