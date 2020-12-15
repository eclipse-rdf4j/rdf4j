package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.UUID;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

public class StatementMatcher {

	private final String subjectName;
	private final Resource subjectValue;

	private final String predicateName;
	private final IRI predicateValue;

	private final String objectName;
	private final Value objectValue;

	public StatementMatcher(String subjectName, Resource subjectValue, String predicateName, IRI predicateValue,
			String objectName, Value objectValue) {
		this.subjectName = subjectName;
		this.subjectValue = subjectValue;
		this.predicateName = predicateName;
		this.predicateValue = predicateValue;
		this.objectName = objectName;
		this.objectValue = objectValue;
	}

	public StatementMatcher(Variable subject, Variable predicate, Variable object) {
		this.subjectName = subject == null ? null : subject.name;
		this.subjectValue = subject == null ? null : (Resource) subject.value;
		this.predicateName = predicate == null ? null : predicate.name;
		this.predicateValue = predicate == null ? null : (IRI) predicate.value;
		this.objectName = object == null ? null : object.name;
		this.objectValue = object == null ? null : object.value;
	}

	public String getSubjectName() {
		return subjectName;
	}

	public Resource getSubjectValue() {
		return subjectValue;
	}

	public boolean subjectIsWildcard() {
		return subjectName == null && subjectValue == null;
	}

	public String getPredicateName() {
		return predicateName;
	}

	public IRI getPredicateValue() {
		return predicateValue;
	}

	public boolean predicateIsWildcard() {
		return predicateName == null && predicateValue == null;
	}

	public String getObjectName() {
		return objectName;
	}

	public Value getObjectValue() {
		return objectValue;
	}

	public boolean objectIsWildcard() {
		return objectName == null && objectValue == null;
	}

	public static class Variable {
		String name;
		Value value;

		public Variable(String name, Value value) {
			this.name = name;
			this.value = value;
		}

		public Variable(String name) {
			this.name = name;
		}

		public Variable(Value value) {
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public Value getValue() {
			return value;
		}

		public boolean isWildcard() {
			return name == null && value == null;
		}

		public static Variable getRandomInstance() {
			return new Variable(UUID.randomUUID().toString().replace("-", ""));
		}
	}

}
