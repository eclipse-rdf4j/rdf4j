/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function.spif;

import org.eclipse.rdf4j.model.vocabulary.SPIF;

public class LowerTitleCase extends AbstractStringReplacer {

	public LowerTitleCase() {
		super(SPIF.LOWER_TITLE_CASE_FUNCTION.toString());
	}

	@Override
	protected String transform(String s) {
		StringBuilder buf = new StringBuilder(s.length());
		char prev = '\0';
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (prev == ' ') {
				buf.append(Character.toUpperCase(ch));
			}
			else {
				buf.append(ch);
			}
			prev = ch;
		}
		return buf.toString();
	}
}
