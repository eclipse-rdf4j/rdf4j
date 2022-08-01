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

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * @author Håvard Ottestad
 */
public class OrderingTest {

	private final static ValueFactory vf = SimpleValueFactory.getInstance();

//	@Test
//	public void testSelect() {
//		MemoryStore repository = new MemoryStore();
//		repository.init();
//
//		try (SailConnection connection = repository.getConnection()) {
//			connection.begin(IsolationLevels.NONE);
//			connection.addStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(RDFS.SUBCLASSOF, RDF.TYPE, RDFS.RESOURCE);
//			connection.commit();
//
//			Select select = new Select(connection, "?a <" + RDF.TYPE + "> []", "*");
//			List<Tuple> tuples = new MockConsumePlanNode(select).asList();
//
//			String actual = Arrays.toString(tuples.toArray());
//
//			Collections.sort(tuples);
//
//			String expected = Arrays.toString(tuples.toArray());
//
//			assertEquals(expected, actual);
//		}
//	}
//
//	@Test
//	public void testSortPlanNode() {
//
//		MemoryStore sailRepository = new MemoryStore();
//		sailRepository.init();
//
//		try (SailConnection connection = sailRepository.getConnection()) {
//			connection.begin();
//			connection.addStatement(vf.createBNode("1"), RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(vf.createBNode("2"), RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(vf.createBNode("4"), RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(vf.createBNode("3"), RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(vf.createBNode("2"), RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(vf.createBNode("2"), RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(vf.createBNode("100"), RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(vf.createBNode("99"), RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(vf.createBNode("101"), RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(vf.createBNode("98"), RDF.TYPE, RDFS.RESOURCE);
//			connection.addStatement(vf.createBNode("102"), RDF.TYPE, RDFS.RESOURCE);
//			connection.commit();
//		}
//		try (SailConnection connection = sailRepository.getConnection()) {
//
//			Select select = new Select(connection, "?a a rdfs:Resource", "?a");
//			List<Tuple> sortedBySelect = new MockConsumePlanNode(select).asList();
//
//			Sort sort = new Sort(new Select(connection, "?a a rdfs:Resource", "?a"));
//			List<Tuple> sortedBySort = new MockConsumePlanNode(sort).asList();
//
//			Assert.assertEquals(sortedBySelect, sortedBySort);
//
//		}
//
//	}
//
//	@Test
//	public void testTargetNode() {
//		MemoryStore memoryStore = new MemoryStore();
//
//		IRI target1 = vf.createIRI("http://example.com/target1");
//		IRI target2 = vf.createIRI("http://example.com/target2");
//		IRI target3 = vf.createIRI("http://example.com/target3");
//		IRI target4 = vf.createIRI("http://example.com/target4");
//		IRI target5 = vf.createIRI("http://example.com/target5");
//
//		try (SailConnection connection = memoryStore.getConnection()) {
//			connection.begin(IsolationLevels.NONE);
//
//			connection.addStatement(target1, FOAF.KNOWS, target1);
//			connection.addStatement(target2, FOAF.KNOWS, target2);
//			connection.addStatement(target3, FOAF.KNOWS, target3);
//			connection.addStatement(target4, FOAF.KNOWS, target4);
//			connection.addStatement(target5, FOAF.KNOWS, target5);
//
//			connection.commit();
//
//			ShaclProperties shaclProperties = new ShaclProperties();
//
//			Set<Value> targetNode = shaclProperties.getTargetNode();
//			targetNode.add(target1);
//			targetNode.add(target5);
//			targetNode.add(target2);
//			targetNode.add(target4);
//			targetNode.add(target3);
//
//			Select select = new Select(connection, "?a ?b ?c", "?a", "?c");
//
//			ValuesBackedNode valuesBackedNode = new ValuesBackedNode(shaclProperties.getTargetNode());
//
//			PlanNode innerJoin = new InnerJoin(valuesBackedNode, select).getJoined(UnBufferedPlanNode.class);
//
//			List<Tuple> tuples = new MockConsumePlanNode(innerJoin).asList();
//
//			verify(tuples,
//					Arrays.asList(target1.toString(), target1.toString()),
//					Arrays.asList(target2.toString(), target2.toString()),
//					Arrays.asList(target3.toString(), target3.toString()),
//					Arrays.asList(target4.toString(), target4.toString()),
//					Arrays.asList(target5.toString(), target5.toString()));
//
//		}
//	}
//
//	@Test
//	public void testComparableTuplesDifferentLength() {
//		assertThat(new Tuple(RDF.FIRST)).isLessThan(new Tuple(RDF.FIRST, RDF.REST));
//	}
//
//	public void verify(List<Tuple> actual, List<String>... expect) {
//
//		List<Tuple> collect = Arrays.stream(expect)
//				.map(strings -> strings.stream()
//						.map(SimpleValueFactory.getInstance()::createLiteral)
//						.map(l -> (Value) l)
//						.collect(Collectors.toList()))
//				.map(Tuple::new)
//				.collect(Collectors.toList());
//
//		actual = actual.stream()
//				.map(tuple -> tuple.getLine()
//						.stream()
//						.map(Value::stringValue)
//						.map(SimpleValueFactory.getInstance()::createLiteral)
//						.map(l -> (Value) l)
//						.collect(Collectors.toList()))
//				.map(Tuple::new)
//				.collect(Collectors.toList());
//
//		assertEquals(collect, actual);
//
//	}
}
