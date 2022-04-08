/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.eclipse.rdf4j.rio.DatatypeHandler;
import org.eclipse.rdf4j.rio.DatatypeHandlerRegistry;
import org.eclipse.rdf4j.rio.LanguageHandler;
import org.eclipse.rdf4j.rio.LanguageHandlerRegistry;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RioSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class encapsulating the basic parser settings that most parsers may support.
 *
 * @author Peter Ansell
 */
public class BasicParserSettings {

	private final static Logger log = LoggerFactory.getLogger(BasicParserSettings.class);

	/**
	 * Boolean setting for parser to determine whether values for recognised datatypes are to be verified.
	 * <p>
	 * Verification is performed using registered DatatypeHandlers.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.verify_datatype_values}.
	 */
	public static final RioSetting<Boolean> VERIFY_DATATYPE_VALUES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.verify_datatype_values", "Verify recognised datatype values", Boolean.FALSE);

	/**
	 * Boolean setting for parser to determine whether to fail parsing if datatypes are not recognised.
	 * <p>
	 * Datatypes are recognised based on matching one of the registered {@link DatatypeHandler}s.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.fail_on_unknown_datatypes}.
	 */
	public static final RioSetting<Boolean> FAIL_ON_UNKNOWN_DATATYPES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.fail_on_unknown_datatypes", "Fail on unknown datatypes", Boolean.FALSE);

	/**
	 * Boolean setting for parser to determine whether recognised datatypes need to have their values be normalized.
	 * <p>
	 * Normalization is performed using registered DatatypeHandlers.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.normalize_datatype_values}.
	 */
	public static final RioSetting<Boolean> NORMALIZE_DATATYPE_VALUES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.normalize_datatype_values", "Normalize recognised datatype values", Boolean.FALSE);

	/**
	 * Setting used to specify which {@link DatatypeHandler} implementations are to be used for a given parser
	 * configuration.
	 * <p>
	 * Defaults to an XMLSchema DatatypeHandler implementation based on {@link DatatypeHandler#XMLSCHEMA} and an RDF
	 * DatatypeHandler implementation based on {@link DatatypeHandler#RDFDATATYPES}.
	 */
	public static final RioSetting<List<DatatypeHandler>> DATATYPE_HANDLERS;

	/**
	 * Boolean setting for parser to determine whether to fail parsing if languages are not recognized.
	 * <p>
	 * Languages are recognized based on matching one of the registered {@link LanguageHandler}s.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.fail_on_unknown_languages}.
	 */
	public static final RioSetting<Boolean> FAIL_ON_UNKNOWN_LANGUAGES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.fail_on_unknown_languages", "Fail on unknown languages", Boolean.FALSE);

	/**
	 * Boolean setting for parser to determine whether languages are to be verified based on a given set of definitions
	 * for valid languages.
	 * <p>
	 * Verification is performed using registered {@link LanguageHandler}s.
	 * <p>
	 * Defaults to true.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.verify_language_tags}.
	 */
	public static final RioSetting<Boolean> VERIFY_LANGUAGE_TAGS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.verify_language_tags", "Verify language tags", Boolean.TRUE);

	/**
	 * Boolean setting for parser to determine whether languages need to be normalized.
	 * <p>
	 * Normalization is performed using registered {@link LanguageHandler}s.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.normalize_language_tags}.
	 */
	public static final RioSetting<Boolean> NORMALIZE_LANGUAGE_TAGS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.normalize_language_tags", "Normalize recognised language tags", Boolean.FALSE);

	/**
	 * Setting used to specify which {@link LanguageHandler} implementations are to be used for a given parser
	 * configuration.
	 * <p>
	 * Defaults to an BCP47 LanguageHandler implementation based on {@link LanguageHandler#BCP47}.
	 */
	public static final RioSetting<List<LanguageHandler>> LANGUAGE_HANDLERS;

	/**
	 * Boolean setting for parser to determine whether relative URIs are verified.
	 * <p>
	 * Defaults to true..
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.verify_relative_uris}.
	 */
	public static final RioSetting<Boolean> VERIFY_RELATIVE_URIS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.verify_relative_uris", "Verify relative URIs", Boolean.TRUE);

	/**
	 * Boolean setting for parser to determine if URIs should be verified to contain only legal characters.
	 * <p>
	 * Defaults to {@code true}. If set to {@code false}, the parser will report syntactically illegal URIs to the
	 * {@link RDFHandler}.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.verify_uri_syntax}.
	 */
	public static final RioSetting<Boolean> VERIFY_URI_SYNTAX = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.verify_uri_syntax", "Verify URI syntax", Boolean.TRUE);

	/**
	 * Boolean setting for parser to determine whether parser should attempt to preserve identifiers for blank nodes. If
	 * the blank node did not have an identifier in the document a new identifier will be generated for it.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.preserve_bnode_ids}.
	 */
	public static final RioSetting<Boolean> PRESERVE_BNODE_IDS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.preserve_bnode_ids", "Preserve blank node identifiers", Boolean.FALSE);

	/**
	 * Scheme and authority of new mint Skolem IRIs that should replace Blank Nodes. For example a value of
	 * "http://example.com" might cause a blank node to be replaced with an IRI of
	 * "http://example.com/.well-known/genid/d26a2d0e98334696f4ad70a677abc1f6"
	 * <p>
	 * Defaults to null (disabled).
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.skolem_origin}.
	 */
	public static final RioSetting<String> SKOLEMIZE_ORIGIN = new StringRioSetting(
			"org.eclipse.rdf4j.rio.skolem_origin",
			"Replace blank nodes with well known genid IRIs using this scheme and authority", null);

	/**
	 * Boolean setting for parser to determine whether parser should preserve, truncate, drop, or otherwise manipulate
	 * statements that contain long literals. The maximum length of literals if this setting is set to truncate or drop
	 * is configured using {@link #LARGE_LITERALS_LIMIT}.
	 * <p>
	 * Defaults to {@link LargeLiteralHandling#PRESERVE}.
	 */
	public static final RioSetting<LargeLiteralHandling> LARGE_LITERALS_HANDLING = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.large_literals", "Large literals handling", LargeLiteralHandling.PRESERVE);

	/**
	 * If {@link #LARGE_LITERALS_HANDLING} is set to {@link LargeLiteralHandling#PRESERVE}, which it is by default, then
	 * the value of this setting is not used.
	 * <p>
	 * If {@link #LARGE_LITERALS_HANDLING} is set to {@link LargeLiteralHandling#DROP} , then the value of this setting
	 * corresponds to the maximum number of bytes for a literal before the statement it is a part of is dropped silently
	 * by the parser.
	 * <p>
	 * If {@link #LARGE_LITERALS_HANDLING} is set to {@link LargeLiteralHandling#TRUNCATE} , then the value of this
	 * setting corresponds to the maximum number of bytes for a literal before the value is truncated.
	 * <p>
	 * Defaults to 1048576 bytes, which is equivalent to 1 megabyte.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.large_literals_limit}.
	 */
	public static final RioSetting<Long> LARGE_LITERALS_LIMIT = new LongRioSetting(
			"org.eclipse.rdf4j.rio.large_literals_limit", "Size limit for large literals", 1048576L);

	/**
	 * <p>
	 * Setting to provide a collection of {@link Namespace} objects which will be used when parsing RDF as the basis for
	 * the default set of namespaces of the document.
	 * </p>
	 * <p>
	 * Namespaces specified within the RDF document being parsed will override these defaults
	 * </p>
	 * <p>
	 * Defaults to {@link Namespaces.DEFAULT_RDF4J} the RDFa 1.1 initial context + some additional prefixes.
	 * </p>
	 */
	public static final RioSetting<Set<Namespace>> NAMESPACES = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.namespaces", "Collection of default namespaces to use for parsing",
			Namespaces.DEFAULT_RDF4J);

	/**
	 * Boolean setting for parser to determine whether it should process RDF-star triples encoded as RDF-compatible
	 * special IRIs back to RDF-star values. These IRIs start with urn:rdf4j:triple: followed by the base64-encoding of
	 * the N-Triples serialization of the RDF-star triple value.
	 * <p>
	 * Parsers that support RDF-star natively will honour this setting too.
	 * <p>
	 * Defaults to true.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.process_encoded_rdf_star}.
	 */
	public static final RioSetting<Boolean> PROCESS_ENCODED_RDF_STAR = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.process_encoded_rdf_star",
			"Converts RDF-star triples encoded as RDF-compatible IRIs back to triple values", Boolean.TRUE);

	static {
		List<DatatypeHandler> defaultDatatypeHandlers = new ArrayList<>(5);
		try {
			DatatypeHandlerRegistry registry = DatatypeHandlerRegistry.getInstance();
			for (String nextDatatype : Arrays.asList(DatatypeHandler.XMLSCHEMA, DatatypeHandler.RDFDATATYPES,
					DatatypeHandler.DBPEDIA, DatatypeHandler.VIRTUOSOGEOMETRY, DatatypeHandler.GEOSPARQL)) {
				Optional<DatatypeHandler> nextDatatypeHandler = registry.get(nextDatatype);
				if (nextDatatypeHandler.isPresent()) {
					defaultDatatypeHandlers.add(nextDatatypeHandler.get());
				} else {
					log.warn("Could not find DatatypeHandler : {}", nextDatatype);
				}
			}
		} catch (Exception e) {
			// Ignore exceptions so that service loading failures do not cause
			// class initialization errors.
			log.warn("Found an error loading DatatypeHandler services", e);
		}

		DATATYPE_HANDLERS = new RioSettingImpl<>("org.eclipse.rdf4j.rio.datatype_handlers", "Datatype Handlers",
				Collections.unmodifiableList(defaultDatatypeHandlers));

		List<LanguageHandler> defaultLanguageHandlers = new ArrayList<>(1);
		try {
			LanguageHandlerRegistry registry = LanguageHandlerRegistry.getInstance();
			String nextLanguageTagScheme = LanguageHandler.BCP47;
			if (registry.has(nextLanguageTagScheme)) {
				Optional<LanguageHandler> nextLanguageHandler = registry.get(nextLanguageTagScheme);
				if (nextLanguageHandler.isPresent()) {
					defaultLanguageHandlers.add(nextLanguageHandler.get());
				} else {
					log.warn("Could not find LanguageHandler : {}", nextLanguageTagScheme);
				}
			}
		} catch (Exception e) {
			// Ignore exceptions so that service loading failures do not cause
			// class initialization errors.
			log.warn("Found an error loading LanguageHandler services", e);
		}

		LANGUAGE_HANDLERS = new RioSettingImpl<>("org.eclipse.rdf4j.rio.language_handlers", "Language Handlers",
				Collections.unmodifiableList(defaultLanguageHandlers));
	}

	/**
	 * Private default constructor.
	 */
	private BasicParserSettings() {
	}

}
