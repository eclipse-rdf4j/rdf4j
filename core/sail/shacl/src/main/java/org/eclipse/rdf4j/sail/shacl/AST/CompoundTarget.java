/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors. All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Distribution License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

// Keeping this around because we may use it in the future to optimize simple use of filter shapes

/*
 * import java.util.Arrays; import java.util.HashSet; import java.util.Objects;
 *
 * import org.eclipse.rdf4j.model.IRI; import org.eclipse.rdf4j.model.Resource; import
 * org.eclipse.rdf4j.model.Statement; import org.eclipse.rdf4j.model.impl.SimpleValueFactory; import
 * org.eclipse.rdf4j.repository.sail.SailRepositoryConnection; import org.eclipse.rdf4j.sail.SailConnection; import
 * org.eclipse.rdf4j.sail.shacl.ConnectionsGroup; import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner; import
 * org.eclipse.rdf4j.sail.shacl.ShaclSail; import org.eclipse.rdf4j.sail.shacl.Stats; import
 * org.eclipse.rdf4j.sail.shacl.planNodes.ExternalTypeFilterNode; import
 * org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode; import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
 * import org.eclipse.rdf4j.sail.shacl.planNodes.Sort; import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple; import
 * org.eclipse.rdf4j.sail.shacl.planNodes.Unique; import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;
 *
 * public class CompoundTarget extends NodeShape {
 *
 * private IRI targetPredicate; private IRI targetObject;
 *
 * CompoundTarget(Resource id, ShaclSail shaclSail, SailRepositoryConnection connection, boolean deactivated, Resource
 * compoundTarget) { super(id, shaclSail, connection, deactivated);
 *
 * SimpleValueFactory vf = SimpleValueFactory.getInstance();
 *
 * IRI targetPredicate = vf.createIRI("http://rdf4j.org/schema/rdf4j-shacl#", "targetPredicate"); IRI targetObject =
 * vf.createIRI("http://rdf4j.org/schema/rdf4j-shacl#", "targetObject");
 *
 * for (Statement statement : connection.getStatements(compoundTarget, null, null)) { if
 * (statement.getPredicate().equals(targetObject)) { this.targetObject = (IRI) statement.getObject(); } if
 * (statement.getPredicate().equals(targetPredicate)) { this.targetPredicate = (IRI) statement.getObject(); } }
 *
 * }
 *
 * @Override public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans, PlanNodeProvider
 * overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
 *
 * assert !negateSubPlans : "There are no subplans!"; assert !negateThisPlan;
 *
 * PlanNode planNode = connectionsGroup .getCachedNodeFor(new Sort(new
 * UnorderedSelect(connectionsGroup.getBaseConnection(), null, targetPredicate, targetObject,
 * UnorderedSelect.OutputPattern.SubjectPredicateObject)));
 *
 * return new Unique(new TrimTuple(planNode, 0, 1));
 *
 * }
 *
 * @Override public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup, PlaneNodeWrapper
 * planeNodeWrapper) { PlanNode planNode = connectionsGroup .getCachedNodeFor(new Sort(new
 * UnorderedSelect(connectionsGroup.getAddedStatements(), null, targetPredicate, targetObject,
 * UnorderedSelect.OutputPattern.SubjectPredicateObject)));
 *
 * return new Unique(new TrimTuple(planNode, 0, 1));
 *
 * }
 *
 * @Override public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup, PlaneNodeWrapper
 * planeNodeWrapper) { PlanNode planNode = connectionsGroup .getCachedNodeFor(new Sort(new
 * UnorderedSelect(connectionsGroup.getRemovedStatements(), null, targetPredicate, targetObject,
 * UnorderedSelect.OutputPattern.SubjectPredicateObject)));
 *
 * return new Unique(new TrimTuple(planNode, 0, 1)); }
 *
 * @Override public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements, Stats
 * stats) { return addedStatements.hasStatement(null, targetPredicate, targetObject, false); }
 *
 * @Override public String getQuery(String subjectVariable, String objectVariable, RdfsSubClassOfReasoner
 * rdfsSubClassOfReasoner) {
 *
 * return " BIND(<" + targetPredicate + "> as ?b1) \n " + "BIND(<" + targetObject + "> as " + objectVariable + ") \n " +
 * subjectVariable + " ?b1 " + objectVariable + ".  \n";
 *
 * }
 *
 * @Override public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) { return new
 * ExternalTypeFilterNode(connectionsGroup.getBaseConnection(), targetPredicate, new
 * HashSet<>(Arrays.asList(targetObject)), parent, 0, true); }
 *
 * @Override public boolean equals(Object o) { if (this == o) { return true; } if (o == null || getClass() !=
 * o.getClass()) { return false; } if (!super.equals(o)) { return false; } CompoundTarget that = (CompoundTarget) o;
 * return Objects.equals(targetPredicate, that.targetPredicate) && Objects.equals(targetObject, that.targetObject); }
 *
 * @Override public int hashCode() { return Objects.hash(super.hashCode(), targetPredicate, targetObject); }
 *
 * @Override public String toString() { return "CompoundTarget{" + "targetPredicate=" + targetPredicate +
 * ", targetObject=" + targetObject + ", id=" + id + '}'; } }
 */
