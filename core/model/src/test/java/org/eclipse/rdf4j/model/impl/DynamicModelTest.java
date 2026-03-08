/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.junit.jupiter.api.Test;

public class DynamicModelTest {

	@Test
	public void testAddNamespace() {
		SimpleNamespace ns = new SimpleNamespace("ex", "example:namespace");

		Model model = new DynamicModelFactory().createEmptyModel();
		model.setNamespace(ns);

		Model filtered = model.filter(null, null, null);

		assertThat(filtered.getNamespace(ns.getPrefix()).get())
				.isEqualTo(ns);
	}

	@Test
	public void testAddNamespacePrefixAndName() {
		String prefix = "ex";
		String name = "example:namespace";

		Model model = new DynamicModelFactory().createEmptyModel();
		model.setNamespace(prefix, name);

		Model filtered = model.filter(null, null, null);

		assertThat(filtered.getNamespace(prefix).isPresent())
				.isTrue();
		assertThat(filtered.getNamespace(prefix).get().getName())
				.isEqualTo(name);
	}

	@Test
	void statementsStorageForDynamicModelUsesMap() throws Exception {
		DynamicModel model = new DynamicModel(new LinkedHashModelFactory());
		Field statementsField = DynamicModel.class.getDeclaredField("statements");
		statementsField.setAccessible(true);
		Object statements = statementsField.get(model);

		assertThat(statements).isInstanceOf(Map.class);
	}

	@Test
	public void testRemoveNamespace() {
		Namespace ns = new SimpleNamespace("ex", "example:namespace");

		Model model = new DynamicModelFactory().createEmptyModel();
		model.setNamespace(ns);

		Model filtered = model.filter(null, null, null);

		filtered.removeNamespace(ns.getPrefix());

		assertThat(filtered.getNamespace(ns.getPrefix()).isPresent())
				.isFalse();
		assertThat(model.getNamespace(ns.getPrefix()).isPresent())
				.isFalse();
	}

	@Test
	void containsWildcardDoesNotForceUpgrade() {
		DynamicModel model = new DynamicModel(new LinkedHashModelFactory());
		ValueFactory valueFactory = SimpleValueFactory.getInstance();

		assertThat(model.getUpgradedModel()).isNull();
		assertThat(model.contains(null, null, null)).isFalse();
		assertThat(model.getUpgradedModel()).isNull();

		model.add(valueFactory.createIRI("urn:subject"), valueFactory.createIRI("urn:predicate"),
				valueFactory.createLiteral("object"));

		assertThat(model.contains(null, null, null)).isTrue();
		assertThat(model.getUpgradedModel()).isNull();
	}

	@Test
	void getStatementsWithoutContextDoesNotForceUpgradeAndChecksAllSeenContexts() {
		DynamicModel model = new DynamicModel(new LinkedHashModelFactory());
		ValueFactory valueFactory = SimpleValueFactory.getInstance();
		Resource subject = valueFactory.createIRI("urn:subject");
		IRI predicate = valueFactory.createIRI("urn:predicate");
		Resource context1 = valueFactory.createIRI("urn:context:1");
		Resource context2 = valueFactory.createIRI("urn:context:2");

		model.add(subject, predicate, valueFactory.createLiteral("object"), context1);
		model.add(subject, predicate, valueFactory.createLiteral("object"), context2);

		assertThat(model.getUpgradedModel()).isNull();

		Iterable<Statement> statements = model.getStatements(subject, predicate, valueFactory.createLiteral("object"));

		assertThat(model.getUpgradedModel()).isNull();
		assertThat(statements)
				.containsExactlyInAnyOrder(
						valueFactory.createStatement(subject, predicate, valueFactory.createLiteral("object"),
								context1),
						valueFactory.createStatement(subject, predicate, valueFactory.createLiteral("object"),
								context2));
	}

	@Test
	void removeTermIterationRemovesAllStatementsForSubjectWithoutUpgrade() throws Exception {
		DynamicModel model = new DynamicModel(new LinkedHashModelFactory());
		ValueFactory valueFactory = SimpleValueFactory.getInstance();
		Resource subject1 = valueFactory.createIRI("urn:subject:1");
		Resource subject2 = valueFactory.createIRI("urn:subject:2");
		IRI predicate1 = valueFactory.createIRI("urn:predicate:1");
		IRI predicate2 = valueFactory.createIRI("urn:predicate:2");
		Resource context1 = valueFactory.createIRI("urn:context:1");
		Resource context2 = valueFactory.createIRI("urn:context:2");

		model.add(subject1, predicate1, valueFactory.createLiteral("value:1"), context1);
		model.add(subject1, predicate2, valueFactory.createLiteral("value:2"), context2);
		model.add(subject2, predicate1, valueFactory.createLiteral("value:1"), context1);

		Iterator<Statement> iterator = model.iterator();
		Statement first = null;
		while (iterator.hasNext()) {
			Statement next = iterator.next();
			if (next.getSubject().equals(subject1)) {
				first = next;
				break;
			}
		}

		assertThat(first).isNotNull();

		Method removeTermIteration = DynamicModel.class.getMethod("removeTermIteration", Iterator.class, Resource.class,
				IRI.class, org.eclipse.rdf4j.model.Value.class, Resource[].class);
		removeTermIteration.invoke(model, iterator, subject1, null, null, (Object) new Resource[0]);

		assertThat(model.contains(subject1, predicate1, valueFactory.createLiteral("value:1"), context1)).isFalse();
		assertThat(model.contains(subject1, predicate2, valueFactory.createLiteral("value:2"), context2)).isFalse();
		assertThat(model.contains(subject2, predicate1, valueFactory.createLiteral("value:1"), context1)).isTrue();
		assertThat(model.getUpgradedModel()).isNull();
	}

}
