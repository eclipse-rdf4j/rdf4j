/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks {@link QueryModelNode#getParentNode()} references that have become inconsistent with the actual algebra tree
 * structure due to optimization operations. Used during testing.
 *
 * @author Jeen Broekstra
 * @author Håvard Ottestad
 */
@InternalUseOnly
public class ParentReferenceChecker implements QueryOptimizer {

	private static final Logger logger = LoggerFactory.getLogger(ParentReferenceChecker.class);

	public static boolean skip = false;

	private final QueryOptimizer previousOptimizerInPipeline;

	public ParentReferenceChecker(QueryOptimizer previousOptimizerInPipeline) {
		this.previousOptimizerInPipeline = previousOptimizerInPipeline;
	}

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		if (skip) {
			return;
		}

		verifySerializable(tupleExpr);
		tupleExpr.visit(new ParentCheckingVisitor(ParentReferenceChecker.this.previousOptimizerInPipeline));
	}

	private void verifySerializable(QueryModelNode tupleExpr) {

		byte[] bytes = objectToBytes(tupleExpr);
		QueryModelNode parsed = (QueryModelNode) bytesToObject(bytes);
//		byte[] bytesAfterSecondSerialization = objectToBytes(parsed);
//		assert Arrays.equals(bytes, bytesAfterSecondSerialization);
		assert tupleExpr.equals(parsed);
	}

	private byte[] objectToBytes(Serializable object) {
		try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
			try (var objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
				objectOutputStream.writeObject(object);
			}
			return byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Object bytesToObject(byte[] str) {
		try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(str)) {
			try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
				return objectInputStream.readObject();
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private final static class ParentCheckingVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		private final ArrayDeque<QueryModelNode> ancestors = new ArrayDeque<>();
		private final String previousOptimizer;

		public ParentCheckingVisitor(QueryOptimizer previousOptimizerInPipeline) {
			if (previousOptimizerInPipeline != null) {
				this.previousOptimizer = previousOptimizerInPipeline.getClass().getSimpleName();
			} else
				this.previousOptimizer = "query parsing";
		}

		@Override
		protected void meetNode(QueryModelNode node) throws RuntimeException {
			QueryModelNode expectedParent = ancestors.peekLast();
			if (node.getParentNode() != expectedParent) {
				String message = "After " + previousOptimizer + " there was an unexpected parent for node \n" + node
						+ "\nwith parent: \n" + node.getParentNode() + "\nexpected: \n" + expectedParent;
				assert node.getParentNode() == expectedParent : message;
			}

			ancestors.addLast(node);
			super.meetNode(node);
			ancestors.pollLast();
		}
	}
}
