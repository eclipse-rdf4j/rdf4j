/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.ir;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Textual IR node for a property-list triple, supporting semicolon and comma short-hand.
 */
public class IrPropertyList extends IrNode {
	public static final class Item {
		private final Var predicate;
		private final List<Var> objects = new ArrayList<>();

		public Item(Var predicate) {
			this.predicate = predicate;
		}

		public Var getPredicate() {
			return predicate;
		}

		public List<Var> getObjects() {
			return objects;
		}
	}

	private final Var subject;
	private final List<Item> items = new ArrayList<>();

	public IrPropertyList(Var subject) {
		this.subject = subject;
	}

	public Var getSubject() {
		return subject;
	}

	public List<Item> getItems() {
		return items;
	}

	public void addItem(Item it) {
		if (it != null) {
			items.add(it);
		}
	}

	@Override
	public void print(IrPrinter p) {
		String subj = p.renderTermWithOverrides(subject);
		List<String> parts = new ArrayList<>();
		for (Item it : items) {
			String pred = p.renderPredicateForTriple(it.getPredicate());
			List<String> objs = new ArrayList<>();
			for (Var ov : it.getObjects()) {
				objs.add(p.renderTermWithOverrides(ov));
			}
			String objTxt = objs.size() <= 1 ? (objs.isEmpty() ? "?_" : objs.get(0)) : String.join(", ", objs);
			parts.add(pred + " " + objTxt);
		}
		p.line(subj + " " + String.join(" ; ", parts) + " .");
	}
}
