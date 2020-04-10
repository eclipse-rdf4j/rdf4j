package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets.Target;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets.TargetClass;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets.TargetNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets.TargetObjectsOf;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.targets.TargetSubjectsOf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract public class Shape implements Identifiable {
	Resource id;

	List<Target> target = new ArrayList<>();

	boolean deactivated;
	List<Literal> message;
	Severity severity;

	public Shape() {
	}

	public void populate(ShaclProperties properties, SailRepositoryConnection connection, Cache cache) {
		this.deactivated = properties.isDeactivated();
		this.message = properties.getMessage();
		this.id = properties.getId();

		if (!properties.getTargetClass().isEmpty()) {
			target.add(new TargetClass(properties.getTargetClass()));
		}
		if (!properties.getTargetNode().isEmpty()) {
			target.add(new TargetNode(properties.getTargetNode()));
		}
		if (!properties.getTargetObjectsOf().isEmpty()) {
			target.add(new TargetObjectsOf(properties.getTargetObjectsOf()));
		}
		if (!properties.getTargetSubjectsOf().isEmpty()) {
			target.add(new TargetSubjectsOf(properties.getTargetSubjectsOf()));
		}

	}

	@Override
	public Resource getId() {
		return id;
	}

	public static class Factory {

		public static List<Shape> getShapes(SailRepositoryConnection connection, ShaclSail sail) {

			Cache cache = new Cache();

			Set<Resource> resources = getTargetableShapes(connection);

			List<Shape> collect = resources.stream()
					.map(r -> new ShaclProperties(r, connection))
					.map(p -> {
						if (p.getType() == SHACL.NODE_SHAPE) {
							return NodeShape.getInstance(p, connection, cache);
						} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
							return PropertyShape.getInstance(p, connection, cache);
						}
						return null;
					})
					.collect(Collectors.toList());

			System.out.println();

			return collect;
		}

		private static Set<Resource> getTargetableShapes(SailRepositoryConnection connection) {
			Set<Resource> collect;
			try (Stream<Statement> TARGET_NODE = connection.getStatements(null, SHACL.TARGET_NODE, null, true)
					.stream()) {
				try (Stream<Statement> TARGET_CLASS = connection.getStatements(null, SHACL.TARGET_CLASS, null, true)
						.stream()) {
					try (Stream<Statement> TARGET_SUBJECTS_OF = connection
							.getStatements(null, SHACL.TARGET_SUBJECTS_OF, null, true)
							.stream()) {
						try (Stream<Statement> TARGET_OBJECTS_OF = connection
								.getStatements(null, SHACL.TARGET_OBJECTS_OF, null, true)
								.stream()) {

							collect = Stream
									.of(TARGET_CLASS, TARGET_NODE, TARGET_OBJECTS_OF, TARGET_SUBJECTS_OF)
									.reduce(Stream::concat)
									.get()
									.map(Statement::getSubject)
									.collect(Collectors.toSet());
						}

					}
				}
			}
			return collect;
		}
	}
}
