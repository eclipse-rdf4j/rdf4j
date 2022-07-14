/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

/**
 * A container object for easy setting and passing of {@link RDFParser} configuration options.
 *
 * @author Jeen Broekstra
 * @author Peter Ansell
 */
public class ParserConfig extends RioConfig {

	/**
	 */
	private static final long serialVersionUID = 270L;

	private Set<RioSetting<?>> nonFatalErrors = new HashSet<>();

	/**
	 * Creates a ParserConfig object starting with default settings.
	 */
	public ParserConfig() {
		super();
	}

	/**
	 * This method indicates a list of optional errors that the parser should attempt to recover from.
	 * <p>
	 * If recovery is not possible, then the parser will still abort with an exception.
	 * <p>
	 * Calls to this method will override previous calls, including the backwards-compatibility settings setup in the
	 * deprecated constructor.
	 * <p>
	 * Non-Fatal errors that are detected MUST be reported to the error listener.
	 *
	 * @param nonFatalErrors The set of parser errors that are relevant to
	 * @return Either a copy of this config, if it is immutable, or this object, to allow chaining of method calls.
	 */
	public ParserConfig setNonFatalErrors(Set<RioSetting<?>> nonFatalErrors) {
		this.nonFatalErrors = new HashSet<>(nonFatalErrors);
		return this;
	}

	/**
	 * Add a non-fatal error to the set used by parsers to determine whether they should attempt to recover from a
	 * particular parsing error.
	 *
	 * @param nextNonFatalError A non-fatal error that a parser should attempt to recover from.
	 * @return Either a copy of this config, if it is immutable, or this object, to allow chaining of method calls.
	 */
	public ParserConfig addNonFatalError(RioSetting<?> nextNonFatalError) {
		this.nonFatalErrors.add(nextNonFatalError);
		return this;
	}

	/**
	 * This method is used by the parser to check whether they should throw an exception or attempt to recover from a
	 * non-fatal error.
	 * <p>
	 * If this method returns false, then the given non-fatal error will cause the parser to throw an exception.
	 * <p>
	 * If this method returns true, then the parser will do its best to recover from the error, potentially by dropping
	 * triples or creating triples that do not exactly match the source.
	 * <p>
	 * By default this method will always return false until {@link #setNonFatalErrors(Set)} is called to specify the
	 * set of errors that are non-fatal in the given context.
	 * <p>
	 * Non-Fatal errors that are detected MUST be reported to the error listener.
	 *
	 * @param errorToCheck
	 * @return True if the user has setup the parser configuration to indicate that this error is not necessarily fatal
	 *         and false if the user has not indicated that this error is fatal.
	 */
	public boolean isNonFatalError(RioSetting<?> errorToCheck) {
		return nonFatalErrors.contains(errorToCheck);
	}

	/**
	 * Get the current set of non-fatal errors.
	 *
	 * @return An unmodifiable set containing the current non-fatal errors.
	 */
	public Set<RioSetting<?>> getNonFatalErrors() {
		return Collections.unmodifiableSet(nonFatalErrors);
	}

	/**
	 * This method is preserved for backwards compatibility.
	 * <p>
	 * Code should be gradually migrated to use {@link BasicParserSettings#PRESERVE_BNODE_IDS}.
	 *
	 * @return Returns the {@link BasicParserSettings#PRESERVE_BNODE_IDS} setting.
	 */
	public boolean isPreserveBNodeIDs() {
		return this.get(BasicParserSettings.PRESERVE_BNODE_IDS);
	}

	@Override
	public ParserConfig useDefaults() {
		super.useDefaults();
		this.nonFatalErrors.clear();
		return this;
	}

	@Override
	public <T extends Object> ParserConfig set(RioSetting<T> setting, T value) {
		super.set(setting, value);
		return this;
	}
}
