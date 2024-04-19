/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.rio.RioSetting;

import com.github.jsonldjava.core.DocumentLoader;

import no.hasmac.jsonld.document.Document;

/**
 * Settings that can be passed to JSONLD Parsers and Writers.
 *
 * @author Peter Ansell
 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
 */
public class JSONLDSettings {

	/**
	 * If set to true, the JSON-LD processor replaces arrays with just one element with that element during compaction.
	 * If set to false, all arrays will remain arrays even if they have just one element.
	 * <p>
	 * Defaults to true.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.compact_arrays}.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
	 *
	 */
	public static final RioSetting<Boolean> COMPACT_ARRAYS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.jsonld.compact_arrays", "Compact arrays", Boolean.TRUE);

	/**
	 * If specified, it is used to retrieve remote documents and contexts; otherwise the processor's built-in loader is
	 * used.
	 *
	 */
	public static final RioSetting<DocumentLoader> DOCUMENT_LOADER = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.jsonld.document_loader", "Document loader", null);

	/**
	 * The JSON-LD context to use when expanding JSON-LD
	 *
	 * @see <a href=
	 *      "https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-expandcontext">https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-expandcontext</a>.
	 */
	public static final RioSetting<Document> EXPAND_CONTEXT = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.jsonld.expand_context",
			"A no.hasmac.jsonld.document.Document that contains the expanded context as specified in https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-expandcontext",
			null);

	public static final RioSetting<Document> FRAME = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.jsonld.frame_document",
			"A no.hasmac.jsonld.document.Document that contains the frame used for framing as specified in https://www.w3.org/TR/json-ld11-framing/",
			null);;

	/**
	 * The JSON-LD processor will throw an exception if a warning is encountered during processing.
	 *
	 */
	public static final RioSetting<Boolean> EXCEPTION_ON_WARNING = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.jsonld.exception_on_warning",
			"Throw an exception when logging a warning.",
			Boolean.FALSE);

	/**
	 * If set to true, the JSON-LD processor is allowed to optimize the output of the
	 * <a href= "http://json-ld.org/spec/latest/json-ld-api/#compaction-algorithm" >Compaction algorithm</a> to produce
	 * even compacter representations.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.optimize}.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
	 *
	 */
	public static final RioSetting<Boolean> OPTIMIZE = new BooleanRioSetting("org.eclipse.rdf4j.rio.jsonld.optimize",
			"Optimize output", Boolean.FALSE);

	/**
	 * If set to true, the JSON-LD processor may emit blank nodes for triple predicates, otherwise they will be omitted.
	 * <p>
	 * Note: the use of blank node identifiers to label properties is obsolete, and may be removed in a future version
	 * of JSON-LD,
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.produce_generalized_rdf}.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
	 *
	 */
	public static final RioSetting<Boolean> PRODUCE_GENERALIZED_RDF = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.jsonld.produce_generalized_rdf", "Produce generalized RDF", Boolean.FALSE);

	/**
	 * If set to true, the JSON-LD processor will try to convert typed values to JSON native types instead of using the
	 * expanded object form when converting from RDF. xsd:boolean values will be converted to true or false. xsd:integer
	 * and xsd:double values will be converted to JSON numbers.
	 * <p>
	 * Defaults to false for RDF compatibility.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.use_native_types}.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
	 *
	 */
	public static final RioSetting<Boolean> USE_NATIVE_TYPES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.jsonld.use_native_types", "Use Native JSON Types", Boolean.FALSE);

	/**
	 * If set to true, the JSON-LD processor will use the expanded rdf:type IRI as the property instead of @type when
	 * converting from RDF.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.use_rdf_type}.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
	 *
	 */
	public static final RioSetting<Boolean> USE_RDF_TYPE = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.jsonld.use_rdf_type", "Use RDF Type", Boolean.FALSE);

	/**
	 * The {@link JSONLDMode} that the writer will use to reorganise the JSONLD document after it is created.
	 * <p>
	 * Defaults to {@link JSONLDMode#EXPAND} to provide maximum RDF compatibility.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#features">JSONLD Features</a>
	 *
	 */
	public static final RioSetting<JSONLDMode> JSONLD_MODE = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.jsonld_mode", "JSONLD Mode", JSONLDMode.EXPAND);

	/**
	 * If set to true, the JSON-LD processor will try to represent the JSON-LD object in a hierarchical view.
	 * <p>
	 * Default to false
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.hierarchical_view}.
	 *
	 */
	public static final RioSetting<Boolean> HIERARCHICAL_VIEW = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.jsonld.hierarchical_view", "Hierarchical representation of the JSON", Boolean.FALSE);

	/**
	 * Whitelist of remote/local resources that the JSON-LD parser can retrieve. Set of URIs as strings.
	 * <p>
	 * Default:
	 * {@code Set.of("http://www.w3.org/ns/anno.jsonld", "http://www.w3.org/ns/activitystreams.jsonld", "http://www.w3.org/ns/ldp.jsonld", "http://www.w3.org/ns/oa.jsonld", "http://www.w3.org/ns/hydra/context.jsonld", "http://schema.org/", "https://w3id.org/security/v1", "https://w3c.github.io/json-ld-rc/context.jsonld", "https://www.w3.org/2018/credentials/v1", "https://health-lifesci.schema.org/", "https://auto.schema.org/", "https://bib.schema.org/", "http://xmlns.com/foaf/spec/index.jsonld", "https://pending.schema.org/", "https://schema.org/", "https://schema.org/docs/jsonldcontext.jsonld", "https://schema.org/version/latest/schemaorg-current-https.jsonld", "https://schema.org/version/latest/schemaorg-all-http.jsonld", "https://schema.org/version/latest/schemaorg-all-https.jsonld", "https://schema.org/version/latest/schemaorg-current-http.jsonld", "https://schema.org/version/latest/schemaorg-all.jsonld", "https://schema.org/version/latest/schemaorg-current.jsonld", "https://project-open-data.cio.gov/v1.1/schema/catalog.jsonld", "https://geojson.org/geojson-ld/geojson-context.jsonld", "https://www.w3.org/2019/wot/td/v1");
	 *
	 */
	public static final RioSetting<Set<String>> WHITELIST = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.jsonld_whitelist",
			"Whitelist of remote/local resources that the JSON-LD parser can retrieve. Set of URIs as strings.",
			Set.of(
					"http://www.w3.org/ns/anno.jsonld",
					"http://www.w3.org/ns/activitystreams.jsonld",
					"http://www.w3.org/ns/ldp.jsonld",
					"http://www.w3.org/ns/oa.jsonld",
					"http://www.w3.org/ns/hydra/context.jsonld",
					"http://schema.org/",
					"https://w3id.org/security/v1",
					"https://w3c.github.io/json-ld-rc/context.jsonld",
					"https://www.w3.org/2018/credentials/v1",
					"https://health-lifesci.schema.org/",
					"https://auto.schema.org/",
					"https://bib.schema.org/",
					"http://xmlns.com/foaf/spec/index.jsonld",
					"https://pending.schema.org/",
					"https://schema.org/",
					"https://schema.org/docs/jsonldcontext.jsonld",
					"https://schema.org/version/latest/schemaorg-current-https.jsonld",
					"https://schema.org/version/latest/schemaorg-all-http.jsonld",
					"https://schema.org/version/latest/schemaorg-all-https.jsonld",
					"https://schema.org/version/latest/schemaorg-current-http.jsonld",
					"https://schema.org/version/latest/schemaorg-all.jsonld",
					"https://schema.org/version/latest/schemaorg-current.jsonld",
					"https://project-open-data.cio.gov/v1.1/schema/catalog.jsonld",
					"https://geojson.org/geojson-ld/geojson-context.jsonld",
					"https://www.w3.org/2019/wot/td/v1"
			));

	/**
	 * Secure mode only allows loading remote/local resources (ex. context from url) that are whitelisted.
	 * <p>
	 * Default: true
	 */
	public static final RioSetting<Boolean> SECURE_MODE = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.jsonld_secure_mode",
			"Secure mode only allows loading remote/local resources (ex. context from url) that are whitelisted.",
			Boolean.TRUE);

	/**
	 * The document loader cache is enabled by default. All loaded documents, such as remote contexts, are cached for 1
	 * hour, or until the cache is full. The cache holds up to 1000 documents. The cache is shared between all
	 * JSONLDParsers. The cache can be disabled by setting this value to false.
	 * <p>
	 * Default: true
	 */
	public static final RioSetting<Boolean> DOCUMENT_LOADER_CACHE = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.jsonld_document_loader_cache",
			"The document loader cache is enabled by default. All loaded documents, such as remote contexts, are cached for 1 hour, or until the cache is full. The cache holds up to 1000 documents. The cache is shared between all JSONLDParsers. The cache can be disabled by setting this value to false.",
			Boolean.TRUE);

	/**
	 * Private default constructor.
	 */
	private JSONLDSettings() {
	}

}
