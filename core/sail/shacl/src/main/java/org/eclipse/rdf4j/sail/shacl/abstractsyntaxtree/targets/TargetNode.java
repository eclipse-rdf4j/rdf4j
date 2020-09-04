package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.SetFilterNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValuesBackedNode;

public class TargetNode extends Target {
	private final TreeSet<Value> targetNodes;

	public TargetNode(TreeSet<Value> targetNodes) {
		this.targetNodes = targetNodes;
		assert !this.targetNodes.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_NODE;
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope) {

		return new ValuesBackedNode(targetNodes, scope);
	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		StringBuilder sb = new StringBuilder();
		sb.append("VALUES ( ").append(subjectVariable).append(" ) {\n");

		targetNodes.stream()
				.map(targetNode -> {
					if (targetNode instanceof Resource) {
						return "<" + targetNode + ">";
					}
					if (targetNode instanceof Literal) {
						IRI datatype = ((Literal) targetNode).getDatatype();
						if (datatype == null) {
							return "\"" + targetNode.stringValue() + "\"";
						}
						if (((Literal) targetNode).getLanguage().isPresent()) {
							return "\"" + targetNode.stringValue() + "\"@" + ((Literal) targetNode).getLanguage().get();
						}
						return "\"" + targetNode.stringValue() + "\"^^<" + datatype.stringValue() + ">";
					}

					throw new IllegalStateException(targetNode.getClass().getSimpleName());

				})
				.forEach(targetNode -> sb.append("( ").append(targetNode).append(" )\n"));

		sb.append("}\n");

		return sb.toString();
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		return new SetFilterNode(targetNodes, parent, 0, true);
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		targetNodes.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}

	@Override
	public Stream<StatementPattern> getStatementPatterns(Var subject, Var object) {
		assert (subject == null);
		throw new ShaclUnsupportedException();
	}

	@Override
	public String getTargetQueryFragment(Var subject, Var object) {
		assert subject == null;

		StringBuilder sb = new StringBuilder();
		sb.append("VALUES ( ?").append(object.getName()).append(" ) {\n");

		targetNodes.stream()
				.map(targetNode -> {
					if (targetNode instanceof Resource) {
						return "<" + targetNode + ">";
					}
					if (targetNode instanceof Literal) {
						IRI datatype = ((Literal) targetNode).getDatatype();
						if (datatype == null) {
							return "\"" + targetNode.stringValue() + "\"";
						}
						if (((Literal) targetNode).getLanguage().isPresent()) {
							return "\"" + targetNode.stringValue() + "\"@" + ((Literal) targetNode).getLanguage().get();
						}
						return "\"" + targetNode.stringValue() + "\"^^<" + datatype.stringValue() + ">";
					}

					throw new IllegalStateException(targetNode.getClass().getSimpleName());

				})
				.forEach(targetNode -> sb.append("( ").append(targetNode).append(" )\n"));

		sb.append("}\n");

		return sb.toString();
	}
}
