package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.HelperTool;

public class InConstraintComponent extends AbstractConstraintComponent {

	private final Set<Value> in;

	public InConstraintComponent(RepositoryConnection connection, Resource in) {
		super(in);
		this.in = new HashSet<>(HelperTool.toList(connection, in, Value.class));
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.IN, getId());
		TreeSet<Value> values = new TreeSet<>(new ValueComparator());
		values.addAll(in);
		HelperTool.listToRdf(values, getId(), model);
	}

}
