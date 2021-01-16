/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Literals;

/**
 * @author Håvard Ottestad
 */
public class LanguageInFilter extends FilterPlanNode {

	private final List<String> languageRanges;
	private final Set<String> lowerCaseLanguageIn;

	public LanguageInFilter(PlanNode parent, Set<String> lowerCaseLanguageIn, List<String> languageRanges) {
		super(parent);
		this.lowerCaseLanguageIn = lowerCaseLanguageIn;
		this.languageRanges = languageRanges;
	}

	@Override
	boolean checkTuple(ValidationTuple t) {
		if (!(t.getValue().isLiteral())) {
			return false;
		}

		Optional<String> language = ((Literal) t.getValue()).getLanguage();
		if (!language.isPresent()) {
			return false;
		}

		// early matching
		boolean languageMatches = language.map(String::toLowerCase).filter(lowerCaseLanguageIn::contains).isPresent();
		if (languageMatches) {
			return true;
		}

		// test according to BCP47
		String langTag = language.get();

		for (String languageRange : languageRanges) {
			if (Literals.langMatches(langTag, languageRange)) {
				return true;
			}
		}

		return false;

	}

	@Override
	public String toString() {
		return "LanguageInFilter{" +
				"languageRanges=" + Arrays.toString(languageRanges.toArray()) +
				", lowerCaseLanguageIn=" + Arrays.toString(lowerCaseLanguageIn.toArray()) +
				'}';
	}
}
