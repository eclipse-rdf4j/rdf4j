package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Cache;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.NodeShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Shape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationTuple;

public class RSXTargetShape extends Target {

	private final Shape targetShape;

	public RSXTargetShape(Resource targetShape, RepositoryConnection connection, ShaclSail shaclSail) {

		ShaclProperties p = new ShaclProperties(targetShape, connection);

		if (p.getType() == SHACL.NODE_SHAPE) {
			this.targetShape = NodeShape.getInstance(p, connection, new Cache(), false, shaclSail);
		} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
			this.targetShape = PropertyShape.getInstance(p, connection, new Cache(), shaclSail);
		} else {
			throw new IllegalStateException("Unknown shape type for " + p.getId());
		}

		this.targetShape.setTargetChain(new TargetChain());

	}

	@Override
	public IRI getPredicate() {
		return RSX.targetShape;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		targetShape.toModel(subject, getPredicate(), model, exported);
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup, scope, connectionsGroup.getAddedStatements());
	}

	private PlanNode getAddedRemovedInner(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope,
			SailConnection connection) {

		List<StatementPattern> collect = getStatementPatterns(null, new Var("temp1"),
				connectionsGroup.getRdfsSubClassOfReasoner()).collect(Collectors.toList());

		String query = getTargetQueryFragment(null, new Var("temp1"), connectionsGroup.getRdfsSubClassOfReasoner());

		List<Var> vars = Arrays.asList(new Var("temp1"), new Var("someVarName"));

		return new TargetChainRetriever(
				connectionsGroup,
				collect,
				null,
				query,
				vars,
				scope
		);

	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {

		throw new UnsupportedOperationException(this.getClass().getSimpleName());

	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		String query = getTargetQueryFragment(null, new Var("temp1"), connectionsGroup.getRdfsSubClassOfReasoner());

		// TODO: this is a slow way to solve this problem! We should use bulk operations.
		return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), parent, query, new Var("temp1"),
				ValidationTuple::getActiveTarget)
						.getTrueNode(UnBufferedPlanNode.class);
	}

	@Override
	public Stream<StatementPattern> getStatementPatterns(Var subject, Var object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		return this.targetShape.getStatementPatterns_rsx_targetShape(subject, object,
				rdfsSubClassOfReasoner, null);
	}

	@Override
	public String getTargetQueryFragment(Var subject, Var object, RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		return this.targetShape.buildSparqlValidNodes_rsx_targetShape(subject, object, rdfsSubClassOfReasoner, null);

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RSXTargetShape that = (RSXTargetShape) o;
		return targetShape.equals(that.targetShape);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetShape);
	}
}
