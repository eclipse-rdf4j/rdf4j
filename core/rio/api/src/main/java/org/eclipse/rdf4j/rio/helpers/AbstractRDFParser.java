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

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ParseErrorListener;
import org.eclipse.rdf4j.rio.ParseLocationListener;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Base class for {@link RDFParser}s offering common functionality for RDF parsers.
 *
 * @author Arjohn Kampman
 */
public abstract class AbstractRDFParser implements RDFParser {

	// static UUID as prefix together with a thread safe incrementing long ensures a unique identifier.
	private final static String uniqueIdPrefix = UUID.randomUUID().toString().replace("-", "");
	private final static AtomicLong uniqueIdSuffix = new AtomicLong();

	private final MessageDigest md5;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The RDFHandler that will handle the parsed RDF.
	 */
	protected RDFHandler rdfHandler;

	/**
	 * An optional ParseErrorListener to report parse errors to.
	 */
	private ParseErrorListener errListener;

	/**
	 * An optional ParseLocationListener to report parse progress in the form of line- and column numbers to.
	 */
	private ParseLocationListener locationListener;

	/**
	 * The ValueFactory to use for creating RDF model objects.
	 */
	protected ValueFactory valueFactory;

	private ValueFactory originalValueFactory;

	/**
	 * The base URI for resolving relative URIs.
	 */
	private ParsedIRI baseURI;

	/**
	 * The base URI for skolemizing IRIs.
	 */
	private String skolemOrigin;
	private ParsedIRI parsedSkolemOrigin;

	/**
	 * Enables a consistent global mapping of blank node identifiers without using a map, but concatenating this as a
	 * prefix for the blank node identifiers supplied by the parser.
	 */
	private String nextBNodePrefix;

	/**
	 * Mapping from namespace prefixes to namespace names.
	 */
	private final Map<String, String> namespaceTable;

	/**
	 * A collection of configuration options for this parser.
	 */
	private ParserConfig parserConfig;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFParserBase that will use a {@link SimpleValueFactory} to create RDF model objects.
	 */
	protected AbstractRDFParser() {
		this(SimpleValueFactory.getInstance());
	}

	/**
	 * Creates a new RDFParserBase that will use the supplied ValueFactory to create RDF model objects.
	 *
	 * @param valueFactory A ValueFactory.
	 */
	protected AbstractRDFParser(ValueFactory valueFactory) {
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		namespaceTable = new HashMap<>(16);
		nextBNodePrefix = createUniqueBNodePrefix();
		setValueFactory(valueFactory);
		setParserConfig(new ParserConfig());
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public RDFParser setValueFactory(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
		this.originalValueFactory = valueFactory;
		return this;
	}

	@Override
	public RDFParser setRDFHandler(RDFHandler handler) {
		rdfHandler = handler;
		return this;
	}

	public RDFHandler getRDFHandler() {
		return rdfHandler;
	}

	@Override
	public RDFParser setParseErrorListener(ParseErrorListener el) {
		errListener = el;
		return this;
	}

	public ParseErrorListener getParseErrorListener() {
		return errListener;
	}

	@Override
	public RDFParser setParseLocationListener(ParseLocationListener el) {
		locationListener = el;
		return this;
	}

	public ParseLocationListener getParseLocationListener() {
		return locationListener;
	}

	@Override
	public RDFParser setParserConfig(ParserConfig config) {
		this.parserConfig = config;
		initializeNamespaceTableFromConfiguration();
		return this;
	}

	@Override
	public ParserConfig getParserConfig() {
		return this.parserConfig;
	}

	/*
	 * Default implementation, specific parsers are encouraged to override this method as necessary.
	 */
	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Collection<RioSetting<?>> result = new HashSet<>();

		// Supported in RDFParserHelper.createLiteral
		result.add(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);
		result.add(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		result.add(BasicParserSettings.NORMALIZE_DATATYPE_VALUES);
		result.add(BasicParserSettings.DATATYPE_HANDLERS);

		// Supported in RDFParserHelper.createLiteral
		result.add(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES);
		result.add(BasicParserSettings.VERIFY_LANGUAGE_TAGS);
		result.add(BasicParserSettings.NORMALIZE_LANGUAGE_TAGS);
		result.add(BasicParserSettings.LANGUAGE_HANDLERS);

		// Supported in RDFParserBase.resolveURI
		result.add(BasicParserSettings.VERIFY_RELATIVE_URIS);

		// Supported in createURI
		result.add(BasicParserSettings.VERIFY_URI_SYNTAX);

		// Supported in RDFParserBase.createBNode(String)
		result.add(BasicParserSettings.PRESERVE_BNODE_IDS);

		result.add(BasicParserSettings.NAMESPACES);

		result.add(BasicParserSettings.SKOLEMIZE_ORIGIN);

		return result;
	}

	@Override
	public <T> RDFParser set(RioSetting<T> setting, T value) {
		getParserConfig().set(setting, value);
		return this;
	}

	@Override
	public void setPreserveBNodeIDs(boolean preserveBNodeIDs) {
		this.parserConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, preserveBNodeIDs);
	}

	public boolean preserveBNodeIDs() {
		return this.parserConfig.get(BasicParserSettings.PRESERVE_BNODE_IDS);
	}

	/**
	 * Parses the supplied URI-string and sets it as the base URI for resolving relative URIs.
	 */
	protected void setBaseURI(String uriSpec) {
		// Store base URI
		if (this.baseURI == null || !this.baseURI.toString().equals(uriSpec)) {
			this.baseURI = ParsedIRI.create(uriSpec);
		}
	}

	/**
	 * Sets the base URI for resolving relative URIs.
	 */
	protected void setBaseURI(ParsedIRI baseURI) {
		setBaseURI(baseURI.toString());
	}

	/**
	 * Associates the specified prefix to the specified namespace.
	 */
	protected void setNamespace(String prefix, String namespace) {
		namespaceTable.put(prefix, namespace);
	}

	/**
	 * Gets the namespace that is associated with the specified prefix or throws an {@link RDFParseException}.
	 *
	 * @throws RDFParseException if no namespace is associated with this prefix
	 */
	protected String getNamespace(String prefix) throws RDFParseException {
		if (namespaceTable.containsKey(prefix)) {
			return namespaceTable.get(prefix);
		}
		String msg = "Namespace prefix '" + prefix + "' used but not defined";

		if ("".equals(prefix)) {
			msg = "Default namespace used but not defined";
		}

		reportFatalError(msg);
		throw new RDFParseException(msg);
	}

	/**
	 * Clears any information that has been collected while parsing. This method must be called by subclasses when
	 * finishing the parse process.
	 */
	protected void clear() {
		baseURI = null;
		nextBNodePrefix = createUniqueBNodePrefix();
		namespaceTable.clear();
		// Don't use the setter setValueFactory() as it will update originalValueFactory too
		if (getParserConfig().get(BasicParserSettings.PROCESS_ENCODED_RDF_STAR)) {
			valueFactory = new RDFStarDecodingValueFactory(originalValueFactory);
		} else {
			valueFactory = originalValueFactory;
		}

		initializeNamespaceTableFromConfiguration();
	}

	protected void initializeNamespaceTableFromConfiguration() {
		for (Namespace aNS : getParserConfig().get(BasicParserSettings.NAMESPACES)) {
			namespaceTable.put(aNS.getPrefix(), aNS.getName());
		}
	}

	/**
	 * Clears the map that keeps track of blank nodes that have been parsed. Normally, this map is clear when the
	 * document has been parsed completely, but subclasses can clear the map at other moments too, for example when a
	 * bnode scope ends.
	 *
	 * @deprecated Map is no longer used, call {@link #clear()} instead.
	 */
	@Deprecated
	protected void clearBNodeIDMap() {
		clear();
	}

	/**
	 * Resolves a URI-string against the base URI and creates a {@link IRI} object for it.
	 */
	protected IRI resolveURI(String uriSpec) throws RDFParseException {
		if (uriSpec.indexOf(':') < 0) {
			// Resolve relative URIs against base URI
			if (baseURI == null) {
				reportFatalError("Unable to resolve URIs, no base URI has been set");
			}

			if (getParserConfig().get(BasicParserSettings.VERIFY_RELATIVE_URIS)) {
				if (uriSpec.length() > 0 && !uriSpec.startsWith("#") && baseURI.isOpaque()) {
					reportError("Relative URI '" + uriSpec + "' cannot be resolved using the opaque base URI '"
							+ baseURI + "'", BasicParserSettings.VERIFY_RELATIVE_URIS);
				}
			}

			return createURI(baseURI.resolve(uriSpec));
		} else {
			// URI is not relative
			return createURI(uriSpec);
		}
	}

	/**
	 * Creates a {@link IRI} object for the specified URI-string.
	 */
	protected IRI createURI(String uri) throws RDFParseException {
		if (getParserConfig().get(BasicParserSettings.VERIFY_URI_SYNTAX)) {
			try {
				new ParsedIRI(uri);
			} catch (URISyntaxException e) {
				reportError(e.getMessage(), BasicParserSettings.VERIFY_URI_SYNTAX);
				return null;
			}
		}
		try {
			return valueFactory.createIRI(uri);
		} catch (Exception e) {
			reportFatalError(e);
			return null; // required by compiler
		}
	}

	/**
	 * Creates a new {@link BNode} or Skolem {@link IRI} object.
	 *
	 * @return blank node or skolem IRI
	 */
	protected Resource createNode() throws RDFParseException {
		ParsedIRI skolem = getCachedSkolemOrigin();
		try {
			if (preserveBNodeIDs() || skolem == null) {
				return valueFactory.createBNode();
			} else {
				String nodeId = valueFactory.createBNode().getID();
				String path = "/.well-known/genid/" + nextBNodePrefix + nodeId;
				String iri = skolem.resolve(path);
				return valueFactory.createIRI(iri);
			}
		} catch (Exception e) {
			reportFatalError(e);
			return null; // required by compiler
		}
	}

	/**
	 * Creates a {@link BNode} or Skolem {@link IRI} object for the specified identifier.
	 *
	 * @param nodeID node identifier
	 * @return blank node or skolem IRI
	 */
	protected Resource createNode(String nodeID) throws RDFParseException {
		// If we are preserving blank node ids then we do not prefix them to
		// make them globally unique
		if (preserveBNodeIDs()) {
			return valueFactory.createBNode(nodeID);
		} else {
			// Prefix the node ID with a unique UUID prefix to reduce
			// cross-document clashes
			// This is consistent as long as nextBNodePrefix is not modified
			// between parser runs

			String toAppend = nodeID;
			if (nodeID.length() > 32) {
				// we only hash the node ID if it is longer than the hash string
				// itself would be.
				byte[] chars = nodeID.getBytes(StandardCharsets.UTF_8);

				// we use an MD5 hash rather than the node ID itself to get a
				// fixed-length generated id, rather than
				// an ever-growing one (see SES-2171)
				toAppend = (new HexBinaryAdapter()).marshal(md5.digest(chars));
			}

			ParsedIRI skolem = getCachedSkolemOrigin();
			if (skolem == null) {
				return valueFactory.createBNode("genid-" + nextBNodePrefix + toAppend);
			} else {
				String path = "/.well-known/genid/" + nextBNodePrefix + toAppend;
				String iri = skolem.resolve(path);
				return valueFactory.createIRI(iri);
			}
		}
	}

	/**
	 * Creates a new {@link BNode} object.
	 */
	@Deprecated
	protected BNode createBNode() throws RDFParseException {
		try {
			return valueFactory.createBNode();
		} catch (Exception e) {
			reportFatalError(e);
			return null; // required by compiler
		}
	}

	/**
	 * Creates a {@link BNode} object for the specified identifier.
	 */
	@Deprecated
	protected BNode createBNode(String nodeID) throws RDFParseException {
		// If we are preserving blank node ids then we do not prefix them to
		// make them globally unique
		if (preserveBNodeIDs()) {
			return valueFactory.createBNode(nodeID);
		} else {
			// Prefix the node ID with a unique UUID prefix to reduce
			// cross-document clashes
			// This is consistent as long as nextBNodePrefix is not modified
			// between parser runs

			String toAppend = nodeID;
			if (nodeID.length() > 32) {
				// we only hash the node ID if it is longer than the hash string
				// itself would be.
				byte[] chars = nodeID.getBytes(StandardCharsets.UTF_8);

				// we use an MD5 hash rather than the node ID itself to get a
				// fixed-length generated id, rather than
				// an ever-growing one (see SES-2171)
				toAppend = (new HexBinaryAdapter()).marshal(md5.digest(chars));
			}

			return valueFactory.createBNode("genid-" + nextBNodePrefix + toAppend);

		}
	}

	/**
	 * Creates a {@link Literal} object with the supplied parameters.
	 */
	protected Literal createLiteral(String label, String lang, IRI datatype) throws RDFParseException {
		return RDFParserHelper.createLiteral(label, lang, datatype, getParserConfig(), getParseErrorListener(),
				valueFactory);
	}

	/**
	 * Creates a {@link Literal} object with the supplied parameters, using the lineNo and columnNo to enhance error
	 * messages or exceptions that may be generated during the creation of the literal.
	 *
	 * @see org.eclipse.rdf4j.rio.helpers.RDFParserHelper#createLiteral(String, String, IRI, ParserConfig,
	 *      ParseErrorListener, ValueFactory, long, long)
	 */
	protected Literal createLiteral(String label, String lang, IRI datatype, long lineNo, long columnNo)
			throws RDFParseException {
		return RDFParserHelper.createLiteral(label, lang, datatype, getParserConfig(), getParseErrorListener(),
				valueFactory, lineNo, columnNo);
	}

	/**
	 * Creates a new {@link Statement} object with the supplied components.
	 */
	protected Statement createStatement(Resource subj, IRI pred, Value obj) throws RDFParseException {
		try {
			return valueFactory.createStatement(subj, pred, obj);
		} catch (Exception e) {
			reportFatalError(e);
			return null; // required by compiler
		}
	}

	/**
	 * Creates a new {@link Statement} object with the supplied components.
	 */
	protected Statement createStatement(Resource subj, IRI pred, Value obj, Resource context) throws RDFParseException {
		try {
			return valueFactory.createStatement(subj, pred, obj, context);
		} catch (Exception e) {
			reportFatalError(e);
			return null; // required by compiler
		}
	}

	/**
	 * Reports the specified line- and column number to the registered {@link ParseLocationListener}, if any.
	 */
	protected void reportLocation(long lineNo, long columnNo) {
		if (locationListener != null) {
			locationListener.parseLocationUpdate(lineNo, columnNo);
		}
	}

	/**
	 * Reports a warning to the registered ParseErrorListener, if any. This method simply calls
	 * {@link #reportWarning(String, long, long)} supplying <var>-1</var> for the line- and column number.
	 */
	protected void reportWarning(String msg) {
		reportWarning(msg, -1, -1);
	}

	/**
	 * Reports a warning with associated line- and column number to the registered ParseErrorListener, if any.
	 */
	protected void reportWarning(String msg, long lineNo, long columnNo) {
		if (errListener != null) {
			errListener.warning(msg, lineNo, columnNo);
		}
	}

	/**
	 * Reports an error with associated line- and column number to the registered ParseErrorListener, if the given
	 * setting has been set to true.
	 * <p>
	 * This method also throws an {@link RDFParseException} when the given setting has been set to <var>true</var> and
	 * it is not a nonFatalError.
	 *
	 * @param msg             The message to use for {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param relevantSetting The boolean setting that will be checked to determine if this is an issue that we need to
	 *                        look at at all. If this setting is true, then the error listener will receive the error,
	 *                        and if {@link ParserConfig#isNonFatalError(RioSetting)} returns true an exception will be
	 *                        thrown.
	 * @throws RDFParseException If {@link ParserConfig#get(RioSetting)} returns true, and
	 *                           {@link ParserConfig#isNonFatalError(RioSetting)} returns true for the given setting.
	 */
	protected void reportError(String msg, RioSetting<Boolean> relevantSetting) throws RDFParseException {
		RDFParserHelper.reportError(msg, relevantSetting, getParserConfig(), getParseErrorListener());
	}

	/**
	 * Reports an error with associated line- and column number to the registered ParseErrorListener, if the given
	 * setting has been set to true.
	 * <p>
	 * This method also throws an {@link RDFParseException} when the given setting has been set to <var>true</var> and
	 * it is not a nonFatalError.
	 *
	 * @param msg             The message to use for {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param lineNo          Optional line number, should default to setting this as -1 if not known. Used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param columnNo        Optional column number, should default to setting this as -1 if not known. Used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param relevantSetting The boolean setting that will be checked to determine if this is an issue that we need to
	 *                        look at at all. If this setting is true, then the error listener will receive the error,
	 *                        and if {@link ParserConfig#isNonFatalError(RioSetting)} returns true an exception will be
	 *                        thrown.
	 * @throws RDFParseException If {@link ParserConfig#get(RioSetting)} returns true, and
	 *                           {@link ParserConfig#isNonFatalError(RioSetting)} returns true for the given setting.
	 */
	protected void reportError(String msg, long lineNo, long columnNo, RioSetting<Boolean> relevantSetting)
			throws RDFParseException {
		RDFParserHelper.reportError(msg, lineNo, columnNo, relevantSetting, getParserConfig(), getParseErrorListener());
	}

	/**
	 * Reports an error with associated line- and column number to the registered ParseErrorListener, if the given
	 * setting has been set to true.
	 * <p>
	 * This method also throws an {@link RDFParseException} when the given setting has been set to <var>true</var> and
	 * it is not a nonFatalError.
	 *
	 * @param e               The exception whose message will be used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param relevantSetting The boolean setting that will be checked to determine if this is an issue that we need to
	 *                        look at at all. If this setting is true, then the error listener will receive the error,
	 *                        and if {@link ParserConfig#isNonFatalError(RioSetting)} returns true an exception will be
	 *                        thrown.
	 * @throws RDFParseException If {@link ParserConfig#get(RioSetting)} returns true, and
	 *                           {@link ParserConfig#isNonFatalError(RioSetting)} returns true for the given setting.
	 */
	protected void reportError(Exception e, RioSetting<Boolean> relevantSetting) throws RDFParseException {
		RDFParserHelper.reportError(e, -1, -1, relevantSetting, getParserConfig(), getParseErrorListener());
	}

	/**
	 * Reports an error with associated line- and column number to the registered ParseErrorListener, if the given
	 * setting has been set to true.
	 * <p>
	 * This method also throws an {@link RDFParseException} when the given setting has been set to <var>true</var> and
	 * it is not a nonFatalError.
	 *
	 * @param e               The exception whose message will be used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param lineNo          Optional line number, should default to setting this as -1 if not known. Used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param columnNo        Optional column number, should default to setting this as -1 if not known. Used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param relevantSetting The boolean setting that will be checked to determine if this is an issue that we need to
	 *                        look at at all. If this setting is true, then the error listener will receive the error,
	 *                        and if {@link ParserConfig#isNonFatalError(RioSetting)} returns true an exception will be
	 *                        thrown.
	 * @throws RDFParseException If {@link ParserConfig#get(RioSetting)} returns true, and
	 *                           {@link ParserConfig#isNonFatalError(RioSetting)} returns true for the given setting.
	 */
	protected void reportError(Exception e, long lineNo, long columnNo, RioSetting<Boolean> relevantSetting)
			throws RDFParseException {
		RDFParserHelper.reportError(e, lineNo, columnNo, relevantSetting, getParserConfig(), getParseErrorListener());
	}

	/**
	 * Reports an error with associated line- and column number to the registered ParseErrorListener, if the given
	 * setting has been set to true.
	 * <p>
	 * This method also throws an {@link RDFParseException} when the given setting has been set to <var>true</var> and
	 * it is not a nonFatalError.
	 *
	 * @param msg             The message to use for {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param e               The exception whose message will be used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param lineNo          Optional line number, should default to setting this as -1 if not known. Used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param columnNo        Optional column number, should default to setting this as -1 if not known. Used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)} .
	 * @param relevantSetting The boolean setting that will be checked to determine if this is an issue that we need to
	 *                        look at at all. If this setting is true, then the error listener will receive the error,
	 *                        and if {@link ParserConfig#isNonFatalError(RioSetting)} returns true an exception will be
	 *                        thrown.
	 * @throws RDFParseException If {@link ParserConfig#get(RioSetting)} returns true, and
	 *                           {@link ParserConfig#isNonFatalError(RioSetting)} returns true for the given setting.
	 */
	protected void reportError(String msg, Exception e, long lineNo, long columnNo, RioSetting<Boolean> relevantSetting)
			throws RDFParseException {
		RDFParserHelper.reportError(e, lineNo, columnNo, relevantSetting, getParserConfig(), getParseErrorListener());
	}

	/**
	 * Reports a fatal error to the registered ParseErrorListener, if any, and throws a <var>ParseException</var>
	 * afterwards. This method simply calls {@link #reportFatalError(String, long, long)} supplying <var>-1</var> for
	 * the line- and column number.
	 */
	protected void reportFatalError(String msg) throws RDFParseException {
		RDFParserHelper.reportFatalError(msg, getParseErrorListener());
	}

	/**
	 * Reports a fatal error with associated line- and column number to the registered ParseErrorListener, if any, and
	 * throws a <var>ParseException</var> afterwards.
	 */
	protected void reportFatalError(String msg, long lineNo, long columnNo) throws RDFParseException {
		RDFParserHelper.reportFatalError(msg, lineNo, columnNo, getParseErrorListener());
	}

	/**
	 * Reports a fatal error to the registered ParseErrorListener, if any, and throws a <var>ParseException</var>
	 * afterwards. An exception is made for the case where the supplied exception is a {@link RDFParseException}; in
	 * that case the supplied exception is not wrapped in another ParseException and the error message is not reported
	 * to the ParseErrorListener, assuming that it has already been reported when the original ParseException was
	 * thrown.
	 * <p>
	 * This method simply calls {@link #reportFatalError(Exception, long, long)} supplying <var>-1</var> for the line-
	 * and column number.
	 */
	protected void reportFatalError(Exception e) throws RDFParseException {
		RDFParserHelper.reportFatalError(e, getParseErrorListener());
	}

	/**
	 * Reports a fatal error with associated line- and column number to the registered ParseErrorListener, if any, and
	 * throws a <var>ParseException</var> wrapped the supplied exception afterwards. An exception is made for the case
	 * where the supplied exception is a {@link RDFParseException}; in that case the supplied exception is not wrapped
	 * in another ParseException and the error message is not reported to the ParseErrorListener, assuming that it has
	 * already been reported when the original ParseException was thrown.
	 */
	protected void reportFatalError(Exception e, long lineNo, long columnNo) throws RDFParseException {
		RDFParserHelper.reportFatalError(e, lineNo, columnNo, getParseErrorListener());
	}

	/**
	 * Reports a fatal error with associated line- and column number to the registered ParseErrorListener, if any, and
	 * throws a <var>ParseException</var> wrapped the supplied exception afterwards. An exception is made for the case
	 * where the supplied exception is a {@link RDFParseException}; in that case the supplied exception is not wrapped
	 * in another ParseException and the error message is not reported to the ParseErrorListener, assuming that it has
	 * already been reported when the original ParseException was thrown.
	 */
	protected void reportFatalError(String message, Exception e, long lineNo, long columnNo) throws RDFParseException {
		RDFParserHelper.reportFatalError(message, e, lineNo, columnNo, getParseErrorListener());
	}

	private String createUniqueBNodePrefix() {
		return uniqueIdPrefix + uniqueIdSuffix.incrementAndGet() + "-";
	}

	/**
	 * Parse skolem origin, if set
	 *
	 * @return skolem origin or null
	 */
	private ParsedIRI getCachedSkolemOrigin() {
		String origin = getParserConfig().get(BasicParserSettings.SKOLEMIZE_ORIGIN);

		if (origin == null || origin.length() == 0) {
			if (skolemOrigin != null) {
				skolemOrigin = null;
				parsedSkolemOrigin = null;
			}
			return null;
		}

		if (skolemOrigin != null && origin.equals(skolemOrigin)) {
			return parsedSkolemOrigin;
		}

		skolemOrigin = origin;
		parsedSkolemOrigin = ParsedIRI.create(origin);
		return parsedSkolemOrigin;
	}
}
