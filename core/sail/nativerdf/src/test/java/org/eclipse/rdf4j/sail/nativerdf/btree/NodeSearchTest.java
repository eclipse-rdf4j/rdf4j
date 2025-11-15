/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NodeSearchTest {

	@TempDir
	File tempDir;

	private BTree tree;

	@BeforeEach
	void setUp() throws Exception {
		tree = new BTree(tempDir, "node-search", 85, 1);
	}

	@AfterEach
	void tearDown() throws Exception {
		if (tree != null) {
			tree.delete();
		}
	}

	@Test
	void exactMatchesAndInsertionPoints() {
		Node node = new Node(1, tree);
		appendValue(node, 10);
		appendValue(node, 20);
		appendValue(node, 30);
		appendValue(node, 40);

		assertEquals(0, node.search(bytes(10)));
		assertEquals(3, node.search(bytes(40)));
		assertEquals(-1, node.search(bytes(5)));
		assertEquals(-3, node.search(bytes(25)));
		assertEquals(-5, node.search(bytes(50)));
	}

	private static void appendValue(Node node, int value) {
		node.insertValueNodeIDPair(node.getValueCount(), bytes(value), 0);
	}

	private static byte[] bytes(int value) {
		return new byte[] { (byte) value };
	}
}
