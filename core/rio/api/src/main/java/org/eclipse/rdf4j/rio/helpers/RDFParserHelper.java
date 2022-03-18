/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.LiteralUtilException;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.DatatypeHandler;
import org.eclipse.rdf4j.rio.LanguageHandler;
import org.eclipse.rdf4j.rio.ParseErrorListener;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Helper methods that may be used by {@link RDFParser} implementations.
 * <p>
 * This class contains reference implementations of the workflows for {@link ParseErrorListener},
 * {@link RDFParseException}, {@link ParserConfig}, {@link DatatypeHandler} and {@link LanguageHandler} related methods
 *
 * @author Peter Ansell
 */
public class RDFParserHelper {

	/**
	 * Create a literal using the given parameters, including iterative verification and normalization by any
	 * {@link DatatypeHandler} or {@link LanguageHandler} implementations that are found in the {@link ParserConfig}.
	 *
	 * @param label        The value for {@link Literal#getLabel()}, which may be iteratively normalized.
	 * @param lang         If this is not null, and the datatype is either not null, or is equal to
	 *                     {@link RDF#LANGSTRING}, then a language literal will be created.
	 * @param datatype     If datatype is not null, and the datatype is not equal to {@link RDF#LANGSTRING} with a
	 *                     non-null lang, then a datatype literal will be created.
	 * @param parserConfig The source of parser settings, including the desired list of {@link DatatypeHandler} and
	 *                     {@link LanguageHandler}s to use for verification and normalization of datatype and language
	 *                     literals respectively.
	 * @param errListener  The {@link ParseErrorListener} to use for signalling errors. This will be called if a setting
	 *                     is enabled by setting it to true in the {@link ParserConfig}, after which the error may
	 *                     trigger an {@link RDFParseException} if the setting is not present in
	 *                     {@link ParserConfig#getNonFatalErrors()}.
	 * @param valueFactory The {@link ValueFactory} to use for creating new {@link Literal}s using this method.
	 * @return A {@link Literal} created based on the given parameters.
	 * @throws RDFParseException If there was an error during the process that could not be recovered from, based on
	 *                           settings in the given parser config.
	 */
	public static final Literal createLiteral(String label, String lang, IRI datatype, ParserConfig parserConfig,
			ParseErrorListener errListener, ValueFactory valueFactory) throws RDFParseException {
		return createLiteral(label, lang, datatype, parserConfig, errListener, valueFactory, -1, -1);
	}

	/**
	 * Create a literal using the given parameters, including iterative verification and normalization by any
	 * {@link DatatypeHandler} or {@link LanguageHandler} implementations that are found in the {@link ParserConfig}.
	 *
	 * @param label        The value for {@link Literal#getLabel()}, which may be iteratively normalized.
	 * @param lang         If this is not null, and the datatype is either not null, or is equal to
	 *                     {@link RDF#LANGSTRING}, then a language literal will be created.
	 * @param datatype     If datatype is not null, and the datatype is not equal to {@link RDF#LANGSTRING} with a
	 *                     non-null lang, then a datatype literal will be created.
	 * @param parserConfig The source of parser settings, including the desired list of {@link DatatypeHandler} and
	 *                     {@link LanguageHandler}s to use for verification and normalization of datatype and language
	 *                     literals respectively.
	 * @param errListener  The {@link ParseErrorListener} to use for signalling errors. This will be called if a setting
	 *                     is enabled by setting it to true in the {@link ParserConfig}, after which the error may
	 *                     trigger an {@link RDFParseException} if the setting is not present in
	 *                     {@link ParserConfig#getNonFatalErrors()}.
	 * @param valueFactory The {@link ValueFactory} to use for creating new {@link Literal}s using this method.
	 * @param lineNo       Optional line number, should default to setting this as -1 if not known. Used for
	 *                     {@link ParseErrorListener#error(String, long, long)} and for
	 *                     {@link RDFParseException#RDFParseException(String, long, long)}.
	 * @param columnNo     Optional column number, should default to setting this as -1 if not known. Used for
	 *                     {@link ParseErrorListener#error(String, long, long)} and for
	 *                     {@link RDFParseException#RDFParseException(String, long, long)}.
	 * @return A {@link Literal} created based on the given parameters.
	 * @throws RDFParseException If there was an error during the process that could not be recovered from, based on
	 *                           settings in the given parser config.
	 */
	public static Literal createLiteral(String label, String lang, IRI datatype, ParserConfig parserConfig,
			ParseErrorListener errListener, ValueFactory valueFactory, long lineNo, long columnNo)
			throws RDFParseException {
		if (label == null) {
			throw new NullPointerException("Cannot create a literal using a null label");
		}

		Literal result = null;
		String workingLabel = label;
		Optional<String> workingLang = Optional.ofNullable(lang);
		IRI workingDatatype = datatype;

		// In RDF-1.1 we must do lang check first as language literals will all
		// have datatype RDF.LANGSTRING, but only language literals would have a
		// non-null lang
		if (workingLang.isPresent() && (workingDatatype == null || RDF.LANGSTRING.equals(workingDatatype))) {
			boolean recognisedLanguage = false;
			for (LanguageHandler nextHandler : parserConfig.get(BasicParserSettings.LANGUAGE_HANDLERS)) {
				if (nextHandler.isRecognizedLanguage(workingLang.get())) {
					recognisedLanguage = true;
					if (parserConfig.get(BasicParserSettings.VERIFY_LANGUAGE_TAGS)) {
						try {
							if (!nextHandler.verifyLanguage(workingLabel, workingLang.get())) {
								reportError("'" + lang + "' is not a valid language tag ", lineNo, columnNo,
										BasicParserSettings.VERIFY_LANGUAGE_TAGS, parserConfig, errListener);
							}
						} catch (LiteralUtilException e) {
							reportError("'" + label
									+ " could not be verified by a language handler that recognised it. language was "
									+ lang, lineNo, columnNo, BasicParserSettings.VERIFY_LANGUAGE_TAGS, parserConfig,
									errListener);
						}
					}
					if (parserConfig.get(BasicParserSettings.NORMALIZE_LANGUAGE_TAGS)) {
						try {
							result = nextHandler.normalizeLanguage(workingLabel, workingLang.get(), valueFactory);
							workingLabel = result.getLabel();
							workingLang = result.getLanguage();
							workingDatatype = result.getDatatype();
						} catch (LiteralUtilException e) {
							reportError(
									"'" + label + "' did not have a valid value for language " + lang + ": "
											+ e.getMessage() + " and could not be normalised",
									lineNo, columnNo, BasicParserSettings.NORMALIZE_LANGUAGE_TAGS, parserConfig,
									errListener);
						}
					}
				}
			}
			if (!recognisedLanguage) {
				reportError("'" + label
						+ "' was not recognised as a language literal, and could not be verified, with language "
						+ lang, lineNo, columnNo, BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, parserConfig,
						errListener);
			}
		} else if (workingDatatype != null) {
			boolean recognisedDatatype = false;
			for (DatatypeHandler nextHandler : parserConfig.get(BasicParserSettings.DATATYPE_HANDLERS)) {
				if (nextHandler.isRecognizedDatatype(workingDatatype)) {
					recognisedDatatype = true;
					if (parserConfig.get(BasicParserSettings.VERIFY_DATATYPE_VALUES)) {
						try {
							if (!nextHandler.verifyDatatype(workingLabel, workingDatatype)) {
								reportError("'" + label + "' is not a valid value for datatype " + datatype, lineNo,
										columnNo, BasicParserSettings.VERIFY_DATATYPE_VALUES, parserConfig,
										errListener);
							}
						} catch (LiteralUtilException e) {
							reportError("'" + label
									+ " could not be verified by a datatype handler that recognised it. datatype was "
									+ datatype, lineNo, columnNo, BasicParserSettings.VERIFY_DATATYPE_VALUES,
									parserConfig, errListener);
						}
					}
					if (parserConfig.get(BasicParserSettings.NORMALIZE_DATATYPE_VALUES)) {
						try {
							result = nextHandler.normalizeDatatype(workingLabel, workingDatatype, valueFactory);
							workingLabel = result.getLabel();
							workingLang = result.getLanguage();
							workingDatatype = result.getDatatype();
						} catch (LiteralUtilException e) {
							reportError(
									"'" + label + "' is not a valid value for datatype " + datatype + ": "
											+ e.getMessage() + " and could not be normalised",
									lineNo, columnNo, BasicParserSettings.NORMALIZE_DATATYPE_VALUES, parserConfig,
									errListener);
						}
					}
				}
			}
			if (!recognisedDatatype) {
				reportError("'" + label + "' was not recognised, and could not be verified, with datatype " + datatype,
						lineNo, columnNo, BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, parserConfig, errListener);
			}

		}

		if (result == null) {
			try {
				// Removes datatype for langString datatype with no language tag when VERIFY_DATATYPE_VALUES is False.
				if ((workingDatatype == null || RDF.LANGSTRING.equals(workingDatatype))
						&& (!workingLang.isPresent() || workingLang.get().isEmpty())
						&& !parserConfig.get(BasicParserSettings.VERIFY_DATATYPE_VALUES)) {
					workingLang = Optional.ofNullable(null);
					workingDatatype = null;
				}
				// Backup for unnormalised language literal creation
				if (workingLang.isPresent() && (workingDatatype == null || RDF.LANGSTRING.equals(workingDatatype))) {
					result = valueFactory.createLiteral(workingLabel, workingLang.get().intern());
				}
				// Backup for unnormalised datatype literal creation
				else if (workingDatatype != null) {
					CoreDatatype coreDatatype = CoreDatatype.from(workingDatatype);

					result = valueFactory.createLiteral(workingLabel,
							coreDatatype != CoreDatatype.NONE ? coreDatatype.getIri() : workingDatatype, coreDatatype);

				} else {
					result = valueFactory.createLiteral(workingLabel, CoreDatatype.XSD.STRING);
				}
			} catch (Exception e) {
				reportFatalError(e, lineNo, columnNo, errListener);
			}
		}

		return result;
	}

	/**
	 * Reports an error with associated line- and column number to the registered ParseErrorListener, if the given
	 * setting has been set to true.
	 * <p>
	 * This method also throws an {@link RDFParseException} when the given setting has been set to <var>true</var> and
	 * it is not a nonFatalError.
	 *
	 * @param msg             The message to use for {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)}.
	 * @param relevantSetting The boolean setting that will be checked to determine if this is an issue that we need to
	 *                        look at at all. If this setting is true, then the error listener will receive the error,
	 *                        and if {@link ParserConfig#isNonFatalError(RioSetting)} returns true an exception will be
	 *                        thrown.
	 * @param parserConfig    The {@link ParserConfig} to use for determining if the error is first sent to the
	 *                        ParseErrorListener, and whether it is then also non-fatal to avoid throwing an
	 *                        {@link RDFParseException}.
	 * @param errListener     The {@link ParseErrorListener} that will be sent messages about errors that are enabled.
	 * @throws RDFParseException If {@link ParserConfig#get(RioSetting)} returns true, and
	 *                           {@link ParserConfig#isNonFatalError(RioSetting)} returns true for the given setting.
	 */
	public static void reportError(String msg, RioSetting<Boolean> relevantSetting, ParserConfig parserConfig,
			ParseErrorListener errListener) throws RDFParseException {
		reportError(msg, -1, -1, relevantSetting, parserConfig, errListener);
	}

	/**
	 * Reports an error with associated line- and column number to the registered ParseErrorListener, if the given
	 * setting has been set to true.
	 * <p>
	 * This method also throws an {@link RDFParseException} when the given setting has been set to <var>true</var> and
	 * it is not a nonFatalError.
	 *
	 * @param msg             The message to use for {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)}.
	 * @param lineNo          Optional line number, should default to setting this as -1 if not known. Used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)}.
	 * @param columnNo        Optional column number, should default to setting this as -1 if not known. Used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)}.
	 * @param relevantSetting The boolean setting that will be checked to determine if this is an issue that we need to
	 *                        look at at all. If this setting is true, then the error listener will receive the error,
	 *                        and if {@link ParserConfig#isNonFatalError(RioSetting)} returns true an exception will be
	 *                        thrown.
	 * @param parserConfig    The {@link ParserConfig} to use for determining if the error is first sent to the
	 *                        ParseErrorListener, and whether it is then also non-fatal to avoid throwing an
	 *                        {@link RDFParseException}.
	 * @param errListener     The {@link ParseErrorListener} that will be sent messages about errors that are enabled.
	 * @throws RDFParseException If {@link ParserConfig#get(RioSetting)} returns true, and
	 *                           {@link ParserConfig#isNonFatalError(RioSetting)} returns true for the given setting.
	 */
	public static void reportError(String msg, long lineNo, long columnNo, RioSetting<Boolean> relevantSetting,
			ParserConfig parserConfig, ParseErrorListener errListener) throws RDFParseException {
		if (parserConfig.get(relevantSetting)) {
			if (errListener != null) {
				errListener.error(msg, lineNo, columnNo);
			}

			if (!parserConfig.isNonFatalError(relevantSetting)) {
				throw new RDFParseException(msg, lineNo, columnNo);
			}
		}
	}

	/**
	 * Reports an error with associated line- and column number to the registered ParseErrorListener, if the given
	 * setting has been set to true.
	 * <p>
	 * This method also throws an {@link RDFParseException} when the given setting has been set to <var>true</var> and
	 * it is not a nonFatalError.
	 *
	 * @param e               The exception whose message to use for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)}.
	 * @param lineNo          Optional line number, should default to setting this as -1 if not known. Used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)}.
	 * @param columnNo        Optional column number, should default to setting this as -1 if not known. Used for
	 *                        {@link ParseErrorListener#error(String, long, long)} and for
	 *                        {@link RDFParseException#RDFParseException(String, long, long)}.
	 * @param relevantSetting The boolean setting that will be checked to determine if this is an issue that we need to
	 *                        look at at all. If this setting is true, then the error listener will receive the error,
	 *                        and if {@link ParserConfig#isNonFatalError(RioSetting)} returns true an exception will be
	 *                        thrown.
	 * @param parserConfig    The {@link ParserConfig} to use for determining if the error is first sent to the
	 *                        ParseErrorListener, and whether it is then also non-fatal to avoid throwing an
	 *                        {@link RDFParseException}.
	 * @param errListener     The {@link ParseErrorListener} that will be sent messages about errors that are enabled.
	 * @throws RDFParseException If {@link ParserConfig#get(RioSetting)} returns true, and
	 *                           {@link ParserConfig#isNonFatalError(RioSetting)} returns true for the given setting.
	 */
	public static void reportError(Exception e, long lineNo, long columnNo, RioSetting<Boolean> relevantSetting,
			ParserConfig parserConfig, ParseErrorListener errListener) throws RDFParseException {
		if (parserConfig.get(relevantSetting)) {
			if (errListener != null) {
				errListener.error(e.getMessage(), lineNo, columnNo);
			}

			if (!parserConfig.isNonFatalError(relevantSetting)) {
				if (e instanceof RDFParseException) {
					throw (RDFParseException) e;
				} else {
					throw new RDFParseException(e, lineNo, columnNo);
				}
			}
		}
	}

	/**
	 * Reports a fatal error to the registered ParseErrorListener, if any, and throws a <var>ParseException</var>
	 * afterwards. This method simply calls {@link #reportFatalError(String, long, long, ParseErrorListener)} supplying
	 * <var>-1</var> for the line- and column number.
	 */
	public static void reportFatalError(String msg, ParseErrorListener errListener) throws RDFParseException {
		reportFatalError(msg, -1, -1, errListener);
	}

	/**
	 * Reports a fatal error with associated line- and column number to the registered ParseErrorListener, if any, and
	 * throws a <var>ParseException</var> afterwards.
	 */
	public static void reportFatalError(String msg, long lineNo, long columnNo, ParseErrorListener errListener)
			throws RDFParseException {
		if (errListener != null) {
			errListener.fatalError(msg, lineNo, columnNo);
		}

		throw new RDFParseException(msg, lineNo, columnNo);
	}

	/**
	 * Reports a fatal error to the registered ParseErrorListener, if any, and throws a <var>ParseException</var>
	 * afterwards. An exception is made for the case where the supplied exception is a {@link RDFParseException}; in
	 * that case the supplied exception is not wrapped in another ParseException and the error message is not reported
	 * to the ParseErrorListener, assuming that it has already been reported when the original ParseException was
	 * thrown.
	 * <p>
	 * This method simply calls {@link #reportFatalError(Exception, long, long, ParseErrorListener)} supplying
	 * <var>-1</var> for the line- and column number.
	 */
	public static void reportFatalError(Exception e, ParseErrorListener errListener) throws RDFParseException {
		reportFatalError(e, -1, -1, errListener);
	}

	/**
	 * Reports a fatal error with associated line- and column number to the registered ParseErrorListener, if any, and
	 * throws a <var>ParseException</var> wrapped the supplied exception afterwards. An exception is made for the case
	 * where the supplied exception is a {@link RDFParseException}; in that case the supplied exception is not wrapped
	 * in another ParseException and the error message is not reported to the ParseErrorListener, assuming that it has
	 * already been reported when the original ParseException was thrown.
	 */
	public static void reportFatalError(Exception e, long lineNo, long columnNo, ParseErrorListener errListener)
			throws RDFParseException {
		if (e instanceof RDFParseException) {
			throw (RDFParseException) e;
		} else {
			if (errListener != null) {
				errListener.fatalError(e.getMessage(), lineNo, columnNo);
			}

			throw new RDFParseException(e, lineNo, columnNo);
		}
	}

	/**
	 * Reports a fatal error with associated line- and column number to the registered ParseErrorListener, if any, and
	 * throws a <var>ParseException</var> wrapped the supplied exception afterwards. An exception is made for the case
	 * where the supplied exception is a {@link RDFParseException}; in that case the supplied exception is not wrapped
	 * in another ParseException and the error message is not reported to the ParseErrorListener, assuming that it has
	 * already been reported when the original ParseException was thrown.
	 */
	public static void reportFatalError(String message, Exception e, long lineNo, long columnNo,
			ParseErrorListener errListener) throws RDFParseException {
		if (e instanceof RDFParseException) {
			throw (RDFParseException) e;
		} else {
			if (errListener != null) {
				errListener.fatalError(message, lineNo, columnNo);
			}

			throw new RDFParseException(message, e, lineNo, columnNo);
		}
	}

	/**
	 * Protected constructor to prevent direct instantiation.
	 */
	protected RDFParserHelper() {
	}
}
