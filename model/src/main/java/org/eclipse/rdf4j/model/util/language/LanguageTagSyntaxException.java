/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
/*
 * LanguageTagSyntaxException.java
 *
 * Created on July 25, 2001, 9:32 AM
 */

package org.eclipse.rdf4j.model.util.language;

import org.eclipse.rdf4j.model.util.Literals;

/**
 * A LanguageTag did not conform to RFC3066. This exception is for the syntactic rules of RFC3066 section 2.1.
 * 
 * @author jjc
 * @deprecated Use {@link Literals#normalizeLanguageTag(String) instead}
 */
@Deprecated
public class LanguageTagSyntaxException extends java.lang.Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5425207434895448094L;

	/**
	 * Constructs an <code>LanguageTagSyntaxException</code> with the specified detail message.
	 * 
	 * @param msg the detail message.
	 */
	LanguageTagSyntaxException(String msg) {
		super(msg);
	}
}
