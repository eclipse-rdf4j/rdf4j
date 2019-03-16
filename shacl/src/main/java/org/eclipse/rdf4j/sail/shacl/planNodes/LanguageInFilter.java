/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.model.Literal;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Håvard Ottestad
 */
public class LanguageInFilter extends FilterPlanNode {

	private final List<String> languageIn;

	public LanguageInFilter(PlanNode parent, List<String> languageIn) {
		super(parent);
		this.languageIn = languageIn;
	}

	@Override
	boolean checkTuple(Tuple t) {
		if (!(t.line.get(1) instanceof Literal))
			return false;

		Optional<String> language = ((Literal) t.line.get(1)).getLanguage();
		return language.filter(languageIn::contains).isPresent();

	}

	@Override
	public String toString() {
		return "LanguageInFilter{" + "languageIn=" + Arrays.toString(languageIn.toArray()) + '}';
	}
}
