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

import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.*;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
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

	public static PropertyPathBuilder of(Iri predicate) {
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
		return then(p(predicate));
	}

	public PropertyPathBuilder then(PropertyPath path) {
		Objects.requireNonNull(head);
		head = pSeq(head, path);
		return this;
	}

	public PropertyPathBuilder then(Consumer<EmptyPropertyPathBuilder> subtreeBuilder) {
		return withSubtree(subtreeBuilder, Expressions::pSeq);
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
		return or(p(predicate));
	}

	public PropertyPathBuilder or(PropertyPath path) {
		Objects.requireNonNull(head);
		head = pAlt(head, path);
		return this;
	}

	public PropertyPathBuilder or(Consumer<EmptyPropertyPathBuilder> subtreeBuilder) {
		return withSubtree(subtreeBuilder, Expressions::pAlt);
	}

	public PropertyPathBuilder zeroOrMore() {
		Objects.requireNonNull(head);
		head = pZeroOrMore(head);
		return this;
	}

	public PropertyPathBuilder oneOrMore() {
		Objects.requireNonNull(head);
		head = pOneOrMore(head);
		return this;
	}

	public PropertyPathBuilder zeroOrOne() {
		Objects.requireNonNull(head);
		head = pZeroOrOne(head);
		return this;
	}

	public PropertyPathBuilder group() {
		Objects.requireNonNull(head);
		head = pGroup(head);
		return this;
	}
}
