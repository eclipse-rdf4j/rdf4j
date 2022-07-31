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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.http.protocol.transaction.operations.AddStatementOperation;
import org.eclipse.rdf4j.http.protocol.transaction.operations.TransactionOperation;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

/**
 * @author jeen
 */
public class TransactionReaderTest {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	private static final IRI bob = vf.createIRI("http://example.org/bob");

	private static final IRI alice = vf.createIRI("http://example.org/alice");

	private static final IRI knows = vf.createIRI("http://example.org/knows");

	private static final char ux0005 = 0x0005;

	private static final Literal controlCharText = vf.createLiteral("foobar." + ux0005 + " foo.");

	private static final IRI context1 = vf.createIRI("http://example.org/context1");

	private static final IRI context2 = vf.createIRI("http://example.org/context2");

	@Test
	public void testRoundtrip() throws Exception {

		AddStatementOperation operation = new AddStatementOperation(bob, knows, alice, context1, context2);

		List<TransactionOperation> txn = new ArrayList<>();
		txn.add(operation);

		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TransactionWriter w = new TransactionWriter();
		w.serialize(txn, out);

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TransactionReader r = new TransactionReader();
		Collection<TransactionOperation> parsedTxn = r.parse(in);

		assertNotNull(parsedTxn);

		for (TransactionOperation op : parsedTxn) {
			assertTrue(op instanceof AddStatementOperation);
			AddStatementOperation addOp = (AddStatementOperation) op;

			Resource[] contexts = addOp.getContexts();

			assertEquals(2, contexts.length);
			assertTrue(contexts[0].equals(context1) || contexts[1].equals(context1));
			assertTrue(contexts[0].equals(context2) || contexts[1].equals(context2));
		}

	}

	/**
	 * reproduces GH-3048
	 *
	 * @throws Exception
	 */
	@Test
	public void testRoundtripRDFStar() throws Exception {

		AddStatementOperation rdfStarOperation = new AddStatementOperation(alice, knows,
				vf.createTriple(bob, knows, alice), context1);

		List<TransactionOperation> txn = new ArrayList<>();
		txn.add(rdfStarOperation);

		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TransactionWriter w = new TransactionWriter();
		w.serialize(txn, out);

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TransactionReader r = new TransactionReader();
		Collection<TransactionOperation> parsedTxn = r.parse(in);

		assertNotNull(parsedTxn);

		for (TransactionOperation op : parsedTxn) {
			assertTrue(op instanceof AddStatementOperation);
			AddStatementOperation addOp = (AddStatementOperation) op;
			assertEquals(rdfStarOperation, op);
			Resource[] contexts = addOp.getContexts();

			assertEquals(1, contexts.length);
			assertTrue(contexts[0].equals(context1) || contexts[1].equals(context1));
		}

	}

	@Test
	public void testControlCharHandling() throws Exception {
		AddStatementOperation operation = new AddStatementOperation(bob, knows, controlCharText);

		List<TransactionOperation> txn = new ArrayList<>();
		txn.add(operation);

		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TransactionWriter w = new TransactionWriter();
		w.serialize(txn, out);

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		TransactionReader r = new TransactionReader();
		Collection<TransactionOperation> parsedTxn = r.parse(in);

		assertNotNull(parsedTxn);

		for (TransactionOperation op : parsedTxn) {
			assertTrue(op instanceof AddStatementOperation);
			AddStatementOperation addOp = (AddStatementOperation) op;
			assertTrue(addOp.getObject().equals(controlCharText));
		}

	}
}
