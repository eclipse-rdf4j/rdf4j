/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.Literal;

/**
 * @author Håvard Ottestad
 */
public class LanguageInFilter extends FilterPlanNode {

	private final Set<String> languageIn;

	public LanguageInFilter(PlanNode parent, Set<String> languageIn) {
		super(parent);
		this.languageIn = languageIn;
	}

	@Override
	boolean checkTuple(ValidationTuple t) {
		if (!(t.getValue() instanceof Literal)) {
			return false;
		}

		Optional<String> language = ((Literal) t.getValue()).getLanguage();
		return language.filter(languageIn::contains).isPresent();

	}

	@Override
	public String toString() {
		return "LanguageInFilter{" + "languageIn=" + Arrays.toString(languageIn.toArray()) + '}';
	}
}
