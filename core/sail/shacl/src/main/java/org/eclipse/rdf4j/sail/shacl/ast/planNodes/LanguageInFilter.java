/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class LanguageInFilter extends FilterPlanNode {

	private static final Logger logger = LoggerFactory.getLogger(LanguageInFilter.class);

	private final List<String> languageRanges;
	private final Set<String> lowerCaseLanguageIn;

	public LanguageInFilter(PlanNode parent, Set<String> lowerCaseLanguageIn, List<String> languageRanges,
			ConnectionsGroup connectionsGroup) {
		super(parent, connectionsGroup);
		this.lowerCaseLanguageIn = lowerCaseLanguageIn;
		this.languageRanges = languageRanges;
	}

	@Override
	boolean checkTuple(Reference t) {
		if (!(t.get().getValue().isLiteral())) {
			logger.debug("Tuple rejected because it's not a literal. Tuple: {}", t);
			return false;
		}

		Optional<String> language = ((Literal) t.get().getValue()).getLanguage();
		if (language.isEmpty()) {
			logger.debug("Tuple rejected because it does not have a language tag. Tuple: {}", t);
			return false;
		}

		// early matching
		boolean languageMatches = language.map(String::toLowerCase).filter(lowerCaseLanguageIn::contains).isPresent();
		if (languageMatches) {
			logger.trace(
					"Tuple accepted because its language tag (toLowerCase()) is in the language set. Actual language: {}, Language set: {}, Tuple: {}",
					language.get(), lowerCaseLanguageIn, t);
			return true;
		}

		// test according to BCP47
		String langTag = language.get();

		for (String languageRange : languageRanges) {
			if (Literals.langMatches(langTag, languageRange)) {
				logger.trace(
						"Tuple accepted because its language tag matches the language range (BCP47). Actual language: {}, Language range: {}, Tuple: {}",
						langTag, languageRange, t);
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		LanguageInFilter that = (LanguageInFilter) o;
		return languageRanges.equals(that.languageRanges) && lowerCaseLanguageIn.equals(that.lowerCaseLanguageIn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), languageRanges, lowerCaseLanguageIn);
	}
}
