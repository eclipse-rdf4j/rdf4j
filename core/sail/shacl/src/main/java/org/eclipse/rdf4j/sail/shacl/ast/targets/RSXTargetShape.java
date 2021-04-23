package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RSX;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ast.Cache;
import org.eclipse.rdf4j.sail.shacl.ast.NodeShape;
import org.eclipse.rdf4j.sail.shacl.ast.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;

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
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		targetShape.toModel(subject, getPredicate(), model, cycleDetection);
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup, scope, connectionsGroup.getAddedStatements());
	}

	private PlanNode getAddedRemovedInner(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope,
			SailConnection connection) {

		StatementMatcher.Variable object = new StatementMatcher.Variable("temp1");

		SparqlFragment sparqlFragment = this.targetShape.buildSparqlValidNodes_rsx_targetShape(null, object,
				connectionsGroup.getRdfsSubClassOfReasoner(), null);

		List<StatementMatcher> statementMatchers = sparqlFragment.getStatementMatchers();

		String query = sparqlFragment.getFragment();

		List<StatementMatcher.Variable> vars = Collections.singletonList(object);

		return new Unique(new TargetChainRetriever(
				connectionsGroup,
				statementMatchers,
				statementMatchers,
				query,
				vars,
				scope
		), false);

	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {

		throw new UnsupportedOperationException(this.getClass().getSimpleName());

	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		String query = getTargetQueryFragment(null, new StatementMatcher.Variable("temp1"),
				connectionsGroup.getRdfsSubClassOfReasoner());

		// TODO: this is a slow way to solve this problem! We should use bulk operations.
		return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), parent, query,
				new StatementMatcher.Variable("temp1"),
				ValidationTuple::getActiveTarget)
						.getTrueNode(UnBufferedPlanNode.class);
	}

	@Override
	public Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		return this.targetShape.buildSparqlValidNodes_rsx_targetShape(subject, object, rdfsSubClassOfReasoner, null)
				.getStatementMatchers()
				.stream();
	}

	@Override
	public String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		return this.targetShape.buildSparqlValidNodes_rsx_targetShape(subject, object, rdfsSubClassOfReasoner, null)
				.getFragment();

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
