/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder;

import static org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPaths.p;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPaths.pAlt;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPaths.pGroup;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPaths.pOneOrMore;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPaths.pSeq;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPaths.pZeroOrMore;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPaths.pZeroOrOne;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.GroupedPath;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.InversePath;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PropertyPath;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class PropertyPathBuilder {
	private PropertyPath head;

	private PropertyPathBuilder() {
		this.head = null;
	}

	PropertyPathBuilder(Iri predicate) {
		this.head = p(predicate);
	}

	PropertyPathBuilder(IRI predicate) {
		this(iri(predicate));
	}

	public static PropertyPathBuilder of(Iri predicate) {
		return new PropertyPathBuilder(predicate);
	}

	public static PropertyPathBuilder of(IRI predicate) {
		return new PropertyPathBuilder(predicate);
	}

	/**
	 * Build the path.
	 *
	 * @return
	 */
	public PropertyPath build() {
		return head;
	}

	/**
	 * Invert whatever comes next (i.e. append <code>^</code>.
	 */
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

	/**
	 * Append <code>`/` predicate</code> to the path.
	 */
	public PropertyPathBuilder then(Iri predicate) {
		return then(p(predicate));
	}

	/**
	 * Append <code>`/` path</code> to the path.
	 */
	public PropertyPathBuilder then(IRI predicate) {
		return then(iri(predicate));
	}

	/**
	 * Append <code>`/` path</code> to the path.
	 */
	public PropertyPathBuilder then(PropertyPath path) {
		Objects.requireNonNull(head);
		head = pSeq(head, path);
		return this;
	}

	/**
	 * Append <code>`/`</code> and the product of the <code>subtreeBuilder</code> to the path.
	 */
	public PropertyPathBuilder then(Consumer<EmptyPropertyPathBuilder> subtreeBuilder) {
		return withSubtree(subtreeBuilder, PropertyPaths::pSeq);
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

	/**
	 * Append <code>`|` predicate</code> to the path.
	 */
	public PropertyPathBuilder or(Iri predicate) {
		return or(p(predicate));
	}

	/**
	 * Append <code>`|` path</code> to the path.
	 */
	public PropertyPathBuilder or(IRI predicate) {
		return or(iri(predicate));
	}

	/**
	 * Append <code>`|` path</code> to the path.
	 */
	public PropertyPathBuilder or(PropertyPath path) {
		Objects.requireNonNull(head);
		head = pAlt(head, path);
		return this;
	}

	/**
	 * Append <code>`|`</code> and the product of the <code>subtreeBuilder</code> to the path.
	 */
	public PropertyPathBuilder or(Consumer<EmptyPropertyPathBuilder> subtreeBuilder) {
		return withSubtree(subtreeBuilder, PropertyPaths::pAlt);
	}

	/**
	 * Append <code>`*`</code> to the path.
	 */
	public PropertyPathBuilder zeroOrMore() {
		Objects.requireNonNull(head);
		head = pZeroOrMore(head);
		return this;
	}

	/**
	 * Append <code>`+`</code> to the path.
	 */

	public PropertyPathBuilder oneOrMore() {
		Objects.requireNonNull(head);
		head = pOneOrMore(head);
		return this;
	}

	/**
	 * Append <code>`?`</code> to the path.
	 */
	public PropertyPathBuilder zeroOrOne() {
		Objects.requireNonNull(head);
		head = pZeroOrOne(head);
		return this;
	}

	/**
	 * Enclose the path with <code>`(` and `)`</code>.
	 */
	public PropertyPathBuilder group() {
		Objects.requireNonNull(head);
		head = pGroup(head);
		return this;
	}
}
