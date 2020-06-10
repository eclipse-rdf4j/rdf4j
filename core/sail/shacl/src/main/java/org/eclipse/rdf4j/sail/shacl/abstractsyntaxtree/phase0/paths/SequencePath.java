package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.PlaneNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.HelperTool;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.tempPlanNodes.TupleValidationPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

public class SequencePath extends Path {

	private final List<Path> sequence;

	public SequencePath(Resource id, RepositoryConnection connection) {
		super(id);
		sequence = HelperTool.toList(connection, id, Resource.class)
				.stream()
				.map(p -> Path.buildPath(connection, p))
				.collect(Collectors.toList());

	}

	@Override
	public String toString() {
		return "SequencePath{ " + Arrays.toString(sequence.toArray()) + " }";
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		sequence.forEach(p -> p.toModel(p.getId(), model, exported));

		List<Resource> values = sequence.stream().map(Path::getId).collect(Collectors.toList());

		HelperTool.listToRdf(values, id, model);
	}

	@Override
	public TupleValidationPlanNode getAdded(ConnectionsGroup connectionsGroup, PlaneNodeWrapper planeNodeWrapper) {
		return null;
	}

}
