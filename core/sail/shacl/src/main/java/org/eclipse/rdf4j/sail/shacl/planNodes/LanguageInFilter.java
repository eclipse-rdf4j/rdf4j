/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.rio.languages.BCP47LanguageHandler;

/**
 * @author Håvard Ottestad
 */
public class LanguageInFilter extends FilterPlanNode {

	BCP47LanguageHandler languageHandler = new BCP47LanguageHandler();

	private final Set<String> languageIn;

	public LanguageInFilter(PlanNode parent, Set<String> languageIn) {
		super(parent);
		this.languageIn = languageIn;
	}

	@Override
	boolean checkTuple(Tuple t) {
		if (!(t.getLine().get(1) instanceof Literal)) {
			return false;
		}

		Optional<String> language = ((Literal) t.getLine().get(1)).getLanguage();
		if (!language.isPresent()) {
			return false;
		}

		// early matching
		boolean languageMatches = language.filter(languageIn::contains).isPresent();
		if (languageMatches) {
			return true;
		}

		// test according to BCP47
		String langTag = language.get();

		return languageIn.stream().anyMatch(langRange -> checkLanguage(langTag, langRange));

	}

	@Override
	public String toString() {
		return "LanguageInFilter{" + "languageIn=" + Arrays.toString(languageIn.toArray()) + '}';
	}

	private static boolean checkLanguage(String langTag, String langRange) {

		List<Locale.LanguageRange> parse = Locale.LanguageRange.parse(langRange);
		List<String> strings = Locale.filterTags(parse, Collections.singletonList(langTag));
		return !strings.isEmpty();
	}
}
