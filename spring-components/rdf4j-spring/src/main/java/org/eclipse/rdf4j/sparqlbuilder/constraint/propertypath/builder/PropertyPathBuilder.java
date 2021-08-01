package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder;

import static org.eclipse.rdf4j.sparqlbuilder.constraint.ExtendedExpressions.*;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.GroupedPath;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.InversePath;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PropertyPath;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

public class PropertyPathBuilder {
	private PropertyPath head;

	private PropertyPathBuilder() {
		this.head = null;
	}

	PropertyPathBuilder(Iri predicate) {
		this.head = P(predicate);
	}

	PropertyPathBuilder of(Iri predicate) {
		return new PropertyPathBuilder(predicate);
	}

	public PropertyPath build() {
		return head;
	}

	public PropertyPathBuilder inv() {
		Objects.requireNonNull(head);
		head = new InversePath(groupIfNotGrouped(head));
		return this;
	}

	private PropertyPath groupIfNotGrouped(PropertyPath path) {
		if (head instanceof GroupedPath) {
			return path;
		}
		return new GroupedPath(path);
	}

	public PropertyPathBuilder then(Iri predicate) {
		return then(P(predicate));
	}

	public PropertyPathBuilder then(PropertyPath path) {
		Objects.requireNonNull(head);
		head = SEQ(head, path);
		return this;
	}

	public PropertyPathBuilder then(Consumer<EmptyPropertyPathBuilder> subtreeBuilder) {
		return withSubtree(subtreeBuilder, (left, right) -> SEQ(left, right));
	}

	private PropertyPathBuilder withSubtree(
			Consumer<EmptyPropertyPathBuilder> subtreeBuilder,
			BiFunction<PropertyPath, PropertyPath, PropertyPath> assembler) {
		Objects.requireNonNull(head);
		EmptyPropertyPathBuilder b = new EmptyPropertyPathBuilder();
		subtreeBuilder.accept(b);
		head = assembler.apply(head, b.build());
		return this;
	}

	public PropertyPathBuilder or(Iri predicate) {
		return or(P(predicate));
	}

	public PropertyPathBuilder or(PropertyPath path) {
		Objects.requireNonNull(head);
		head = ALT(head, path);
		return this;
	}

	public PropertyPathBuilder or(Consumer<EmptyPropertyPathBuilder> subtreeBuilder) {
		return withSubtree(subtreeBuilder, (left, right) -> ALT(left, right));
	}

	public PropertyPathBuilder zeroOrMore() {
		Objects.requireNonNull(head);
		head = ZOM(head);
		return this;
	}

	public PropertyPathBuilder oneOrMore() {
		Objects.requireNonNull(head);
		head = OOM(head);
		return this;
	}

	public PropertyPathBuilder zeroOrOne() {
		Objects.requireNonNull(head);
		head = ZOO(head);
		return this;
	}

	public PropertyPathBuilder group() {
		Objects.requireNonNull(head);
		head = GROUP(head);
		return this;
	}
}
