/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Settings that can be passed to JSONLD Parsers and Writers.
 * 
 * @author Peter Ansell
 * @see <a
 *      href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD
 *      Data Structures</a>
 */
public class JSONLDSettings {

	/**
	 * If set to true, the JSON-LD processor replaces arrays with just one
	 * element with that element during compaction. If set to false, all arrays
	 * will remain arrays even if they have just one element.
	 * <p>
	 * Defaults to true.
	 * 
	 * @since 2.7.0
	 * @see <a
	 *      href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD
	 *      Data Structures</a>
	 */
	public static final RioSetting<Boolean> COMPACT_ARRAYS = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.jsonld.compactarrays", "Compact arrays", Boolean.TRUE);

	/**
	 * If set to true, the JSON-LD processor is allowed to optimize the output of
	 * the <a href=
	 * "http://json-ld.org/spec/latest/json-ld-api/#compaction-algorithm"
	 * >Compaction algorithm</a> to produce even compacter representations.
	 * <p>
	 * Defaults to false.
	 * 
	 * @since 2.7.0
	 * @see <a
	 *      href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD
	 *      Data Structures</a>
	 */
	public static final RioSetting<Boolean> OPTIMIZE = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.jsonld.optimize", "Optimize output", Boolean.FALSE);

	/**
	 * If set to true, the JSON-LD processor will try to convert typed values to
	 * JSON native types instead of using the expanded object form when
	 * converting from RDF. xsd:boolean values will be converted to true or
	 * false. xsd:integer and xsd:double values will be converted to JSON
	 * numbers.
	 * <p>
	 * Defaults to false for RDF compatibility.
	 * 
	 * @since 2.7.0
	 * @see <a
	 *      href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD
	 *      Data Structures</a>
	 */
	public static final RioSetting<Boolean> USE_NATIVE_TYPES = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.jsonld.usenativetypes", "Use Native JSON Types", Boolean.FALSE);

	/**
	 * If set to true, the JSON-LD processor will use the expanded rdf:type IRI
	 * as the property instead of @type when converting from RDF.
	 * <p>
	 * Defaults to false.
	 * 
	 * @since 2.7.0
	 * @see <a
	 *      href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD
	 *      Data Structures</a>
	 */
	public static final RioSetting<Boolean> USE_RDF_TYPE = new RioSettingImpl<Boolean>(
			"org.eclipse.rdf4j.rio.jsonld.userdftype", "Use RDF Type", Boolean.FALSE);

	/**
	 * The {@link JSONLDMode} that the writer will use to reorganise the JSONLD
	 * document after it is created.
	 * <p>
	 * Defaults to {@link JSONLDMode#EXPAND} to provide maximum RDF
	 * compatibility.
	 * 
	 * @since 2.7.0
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#features">JSONLD
	 *      Features</a>
	 */
	public static final RioSetting<JSONLDMode> JSONLD_MODE = new RioSettingImpl<JSONLDMode>(
			"org.eclipse.rdf4j.rio.jsonld.mode", "JSONLD Mode", JSONLDMode.EXPAND);

	/**
	 * Private default constructor.
	 */
	private JSONLDSettings() {
	}

}
