package org.eclipse.rdf4j.sail.shacl.ast;

import static org.eclipse.rdf4j.model.util.Values.bnode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;

public class HelperTool {

	public static void listToRdf(Collection<? extends Value> values, Resource head, Model model) {
		// The Turtle parser does not add "a rdf:List" statements when parsing the shorthand list format,
		// so we don't add rdf:List when writing it out either.

		Iterator<? extends Value> iter = values.iterator();
		while (iter.hasNext()) {
			Value value = iter.next();
			model.add(head, RDF.FIRST, value);

			if (iter.hasNext()) {
				Resource next = bnode();
				model.add(head, RDF.REST, next);
				head = next;
			} else {
				model.add(head, RDF.REST, RDF.NIL);
			}
		}
	}

	private static List<Value> toList(RepositoryConnection connection, Resource head) {
		List<Value> ret = new ArrayList<>();
		while (!head.equals(RDF.NIL)) {
			try (Stream<Statement> stream = connection.getStatements(head, RDF.FIRST, null).stream()) {
				Value value = stream.map(Statement::getObject).findAny().get();
				ret.add(value);
			}

			try (Stream<Statement> stream = connection.getStatements(head, RDF.REST, null).stream()) {
				head = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().get();
			}

		}

		return ret;

	}

	public static <T extends Value> List<T> toList(RepositoryConnection connection, Resource head, Class<T> type) {
		if (type == Value.class) {
			return (List<T>) toList(connection, head);
		}

		List<T> ret = new ArrayList<>();
		while (!head.equals(RDF.NIL)) {
			try (Stream<Statement> stream = connection.getStatements(head, RDF.FIRST, null).stream()) {
				Value value = stream.map(Statement::getObject).findAny().get();
				if (type.isInstance(value)) {
					ret.add(((T) value));
				} else {
					throw new IllegalStateException("RDF list should contain only type " + type.getSimpleName()
							+ ", but found " + value.getClass().getSimpleName());
				}
			}

			try (Stream<Statement> stream = connection.getStatements(head, RDF.REST, null).stream()) {
				head = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().get();
			}

		}

		return ret;

	}

	public static class SubjectScopedMapper implements Function<Statement, ValidationTuple> {

		private final ConstraintComponent.Scope scope;

		private SubjectScopedMapper(ConstraintComponent.Scope scope) {
			this.scope = scope;
		}

		static SubjectScopedMapper nodeShapeInstance = new SubjectScopedMapper(ConstraintComponent.Scope.nodeShape);
		static SubjectScopedMapper propertyShapeInstance = new SubjectScopedMapper(
				ConstraintComponent.Scope.propertyShape);
		static SubjectScopedMapper noneInstance = new SubjectScopedMapper(ConstraintComponent.Scope.none);

		@Override
		public ValidationTuple apply(Statement s) {
			return new ValidationTuple(s.getSubject(), scope, false);
		}

		@Override
		public int hashCode() {
			return 72706357 + scope.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this;
		}

		public static SubjectScopedMapper getFunction(ConstraintComponent.Scope scope) {
			switch (scope) {

			case none:
				return noneInstance;
			case nodeShape:
				return nodeShapeInstance;
			case propertyShape:
				return propertyShapeInstance;
			}

			throw new IllegalStateException("Unknown scope: " + scope);
		}

	}

	public static class ObjectScopedMapper implements Function<Statement, ValidationTuple> {

		private final ConstraintComponent.Scope scope;

		private ObjectScopedMapper(ConstraintComponent.Scope scope) {
			this.scope = scope;
		}

		static ObjectScopedMapper nodeShapeInstance = new ObjectScopedMapper(ConstraintComponent.Scope.nodeShape);
		static ObjectScopedMapper propertyShapeInstance = new ObjectScopedMapper(
				ConstraintComponent.Scope.propertyShape);
		static ObjectScopedMapper noneInstance = new ObjectScopedMapper(ConstraintComponent.Scope.none);

		@Override
		public ValidationTuple apply(Statement s) {
			return new ValidationTuple(s.getObject(), scope, false);
		}

		@Override
		public int hashCode() {
			return 25482634 + scope.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this;
		}

		public static ObjectScopedMapper getFunction(ConstraintComponent.Scope scope) {
			switch (scope) {

			case none:
				return noneInstance;
			case nodeShape:
				return nodeShapeInstance;
			case propertyShape:
				return propertyShapeInstance;
			}

			throw new IllegalStateException("Unknown scope: " + scope);
		}

	}

	public static class SubjectObjectPropertyShapeMapper implements Function<Statement, ValidationTuple> {

		private SubjectObjectPropertyShapeMapper() {
		}

		static SubjectObjectPropertyShapeMapper instance = new SubjectObjectPropertyShapeMapper();

		@Override
		public ValidationTuple apply(Statement s) {
			return new ValidationTuple(s.getSubject(), s.getObject(), ConstraintComponent.Scope.propertyShape, true);
		}

		@Override
		public int hashCode() {
			return 35972357;
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this;
		}

		public static SubjectObjectPropertyShapeMapper getFunction() {
			return instance;
		}

	}

}
