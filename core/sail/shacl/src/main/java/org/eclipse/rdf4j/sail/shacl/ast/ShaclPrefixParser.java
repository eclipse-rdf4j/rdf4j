/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

@InternalUseOnly
public class ShaclPrefixParser {

	private ShaclPrefixParser() {
	}

	public static Namespaces extractNamespaces(Resource id, ShapeSource shapeSource) {
		var shaclPrefixes = new Namespaces();

		try (Stream<Value> objects = shapeSource.getObjects(id, ShapeSource.Predicates.PREFIXES)) {
			objects.forEach(prefix -> {
				if (!(prefix instanceof Resource)) {
					throw new IllegalStateException("sh:prefixes must be an Resource for constraint component " + id);
				}
				shaclPrefixes.model.add(id, SHACL.PREFIXES, prefix);

				try (Stream<Value> declareObjects = shapeSource.getObjects(((Resource) prefix),
						ShapeSource.Predicates.DECLARE)) {
					declareObjects.forEach(declaration -> {
						if (!(declaration instanceof Resource)) {
							throw new IllegalStateException("sh:declare must be a Resource for " + prefix);
						}

						shaclPrefixes.model.add((Resource) prefix, SHACL.DECLARE, declaration);

						String namespacePrefix = null;
						String namespaceName = null;

						try (Stream<Value> prefixPropObjects = shapeSource.getObjects(((Resource) declaration),
								ShapeSource.Predicates.PREFIX_PROP)) {
							namespacePrefix = prefixPropObjects
									.map(literal -> {
										if (!(literal instanceof Literal)) {
											throw new IllegalStateException(
													"sh:prefix must be a Literal for " + declaration);
										}
										shaclPrefixes.model.add((Resource) declaration, SHACL.PREFIX_PROP, literal);
										return literal.stringValue();
									})
									.findFirst()
									.orElseThrow(() -> new IllegalStateException(
											"sh:prefix must have a value for " + declaration));
						}

						try (Stream<Value> namespacePropObjects = shapeSource.getObjects(((Resource) declaration),
								ShapeSource.Predicates.NAMESPACE_PROP)) {
							namespaceName = namespacePropObjects
									.map(literal -> {
										if (!(literal instanceof Literal)) {
											throw new IllegalStateException(
													"sh:namespace must be a Literal for " + declaration);
										}
										shaclPrefixes.model.add((Resource) declaration, SHACL.NAMESPACE_PROP, literal);
										return literal.stringValue();
									})
									.findFirst()
									.orElseThrow(() -> new IllegalStateException(
											"sh:namespace must have a value for " + declaration));
						}

						shaclPrefixes.namespaces.add(new SimpleNamespace(namespacePrefix, namespaceName));

					});
				}

			});
		}

		return shaclPrefixes;
	}

	public static String toSparqlPrefixes(Collection<Namespace> namespaces) {
		StringBuilder sb = new StringBuilder();
		namespaces.forEach(namespace -> {
			sb.append("PREFIX ");
			sb.append(namespace.getPrefix());
			sb.append(": <");
			sb.append(namespace.getName());
			sb.append("> \n");
		});
		sb.append("\n");
		return sb.toString();
	}

	public static final class Namespaces {
		private final Set<Namespace> namespaces = new HashSet<>();
		private final Model model = new DynamicModel(new LinkedHashModelFactory());

		private Namespaces() {
		}

		public Set<Namespace> getNamespaces() {
			return namespaces;
		}

		public Model getModel() {
			return model;
		}

	}

}
