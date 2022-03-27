/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

/**
 * @author Håvard Ottestad
 */
public class LeftOuterJoinTest {
	/*
	 * @BeforeClass public static void beforeClass() { // GlobalValidationExecutionLogging.loggingEnabled = true;
	 *
	 * }
	 *
	 * @AfterClass public static void afterClass() { GlobalValidationExecutionLogging.loggingEnabled = false; }
	 *
	 * @Test public void testSimple() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a")); PlanNode right = new
	 * MockInputPlanNode(Arrays.asList("a", "b"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a", "b"));
	 *
	 * }
	 *
	 * @Test public void testSimple2() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2")); PlanNode right = new
	 * MockInputPlanNode(Arrays.asList("a1", "b"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1", "b"), Arrays.asList("a2"));
	 *
	 * }
	 *
	 * @Test public void testSimple3() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2")); PlanNode right = new
	 * MockInputPlanNode(Arrays.asList("a2", "b"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1"), Arrays.asList("a2", "b"));
	 *
	 * }
	 *
	 * @Test public void testSimple4() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2")); PlanNode right = new
	 * MockInputPlanNode(Arrays.asList());
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1"), Arrays.asList("a2"));
	 *
	 * }
	 *
	 * @Test public void testSimple5() {
	 *
	 * PlanNode left = new MockInputPlanNode(); PlanNode right = new MockInputPlanNode(Arrays.asList("a1"),
	 * Arrays.asList("a2"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples);
	 *
	 * }
	 *
	 * @Test public void testSimple6() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
	 * Arrays.asList("a4")); PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2",
	 * "b2"), Arrays.asList("a3", "b3"), Arrays.asList("a4", "b4"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a3", "b3"),
	 * Arrays.asList("a4", "b4"));
	 *
	 * }
	 *
	 * @Test public void testSimple7() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2")); PlanNode right = new
	 * MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"));
	 *
	 * }
	 *
	 * @Test public void testSimple8() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3")); PlanNode
	 * right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a3", "b3"));
	 *
	 * PlanNode leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a3", "b3"));
	 *
	 * }
	 *
	 * @Test public void testSimple9() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
	 * Arrays.asList("a4")); PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2",
	 * "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a3", "b3"), Arrays.asList("a4", "b4"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"),
	 * Arrays.asList("a3", "b3"), Arrays.asList("a4", "b4"));
	 *
	 * }
	 *
	 * @Test public void testSimple10() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
	 * Arrays.asList("a4")); PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a2",
	 * "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a4", "b4"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"),
	 * Arrays.asList("a3"), Arrays.asList("a4", "b4"));
	 *
	 * }
	 *
	 * @Test public void testSimple11() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
	 * Arrays.asList("a4")); PlanNode right = new MockInputPlanNode(Arrays.asList("a2", "b2"), Arrays.asList("a2",
	 * "b22"), Arrays.asList("a4", "b4"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a3"),
	 * Arrays.asList("a4", "b4"));
	 *
	 * }
	 *
	 * @Test public void testSimple12() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
	 * Arrays.asList("a4")); PlanNode right = new MockInputPlanNode(Arrays.asList("a2", "b2"), Arrays.asList("a2",
	 * "b22"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"), Arrays.asList("a3"),
	 * Arrays.asList("a4"));
	 *
	 * }
	 *
	 * @Test public void testSimple13() {
	 *
	 * PlanNode left = new MockInputPlanNode(Arrays.asList("a1"), Arrays.asList("a2"), Arrays.asList("a3"),
	 * Arrays.asList("a4")); PlanNode right = new MockInputPlanNode(Arrays.asList("a1", "b1"), Arrays.asList("a1",
	 * "b11"), Arrays.asList("a2", "b2"), Arrays.asList("a2", "b22"));
	 *
	 * LeftOuterJoin leftOuterJoin = new LeftOuterJoin(left, right);
	 *
	 * List<Tuple> tuples = new MockConsumePlanNode(leftOuterJoin).asList();
	 *
	 * tuples.forEach(System.out::println);
	 *
	 * verify(tuples, Arrays.asList("a1", "b1"), Arrays.asList("a1", "b11"), Arrays.asList("a2", "b2"),
	 * Arrays.asList("a2", "b22"), Arrays.asList("a3"), Arrays.asList("a4"));
	 *
	 * }
	 *
	 * public void verify(List<Tuple> actual, List<String>... expect) {
	 *
	 * Set<Tuple> collect = Arrays.stream(expect) .map(strings -> strings.stream()
	 * .map(SimpleValueFactory.getInstance()::createLiteral) .map(l -> (Value) l) .collect(Collectors.toList()))
	 * .map(Tuple::new) .collect(Collectors.toSet());
	 *
	 * Set<Tuple> actualSet = new HashSet<>(actual);
	 *
	 * assertTrue(collect.containsAll(actualSet)); assertTrue(actualSet.containsAll(collect));
	 *
	 * }
	 */
}
