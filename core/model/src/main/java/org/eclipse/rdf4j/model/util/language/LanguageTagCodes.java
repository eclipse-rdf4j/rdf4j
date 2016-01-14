/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
/*
 * LanguageTagCodes.java
 *
 * Created on July 25, 2001, 10:09 AM
 */

package org.eclipse.rdf4j.model.util.language;

/**
 * Informational values about language codes. Values to be OR-ed together.
 * 
 * @author jjc
 */
public interface LanguageTagCodes {

	/**
	 * The special tag <CODE>i-default</CODE>.
	 */
	public static final int LT_DEFAULT = 0x0100;

	/**
	 * A tag with non-standard extra subtags. Set for language tags with
	 * additional subtags over their IANA registration, or a third subtag for
	 * unregistered tags of the form ISO639Code-ISO3166Code.
	 */
	public static final int LT_EXTRA = 0x0080;

	/**
	 * A tag in the IANA registry.
	 */
	public static final int LT_IANA = 0x1024;

	/**
	 * An illegal tag. Some rule of RFC3066 failed, or the tag is not in IANA, or
	 * ISO639 or ISO3166.
	 */
	public static final int LT_ILLEGAL = 0x8000;

	/**
	 * The second subtag is from ISO3166 and identifies a country.
	 */
	public static final int LT_ISO3166 = 0x0010;

	/**
	 * The first subtag is from ISO639-1 or ISO639-2 and identifies a language,
	 */
	public static final int LT_ISO639 = 0x0001;

	/**
	 * A special ISO639-2 local use language tag. A three letter code
	 * 'q[a-t][a-z]'.
	 */
	public static final int LT_LOCAL_USE = 0x0800;

	/**
	 * The special ISO639-2 language tag <CODE>mul</CODE>. This indicates
	 * multiple languages.
	 */
	public static final int LT_MULTIPLE = 0x0400;

	/**
	 * An RFC3066 private use tag. A language tag of the form <CODE>x-????</CODE>.
	 */
	public static final int LT_PRIVATE_USE = 0x0002;

	/**
	 * The undetermined ISO639-2 language <CODE>und</CODE>.
	 */
	public static final int LT_UNDETERMINED = 0x0200;

	/**
	 * A language tag that is deprecated in the IANA registry.
	 */
	public static final int LT_IANA_DEPRECATED = 0x2000;

}
