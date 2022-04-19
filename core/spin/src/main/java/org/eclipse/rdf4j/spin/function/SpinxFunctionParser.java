/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SPIN;
import org.eclipse.rdf4j.model.vocabulary.SPINX;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.TripleSources;
import org.eclipse.rdf4j.spin.Argument;
import org.eclipse.rdf4j.spin.SpinParser;

public class SpinxFunctionParser implements FunctionParser {

	private final SpinParser parser;

	private final ScriptEngineManager scriptManager;

	public SpinxFunctionParser(SpinParser parser) {
		this.parser = parser;
		this.scriptManager = new ScriptEngineManager();
	}

	@Override
	public Function parse(IRI funcUri, TripleSource store) throws RDF4JException {
		Value codeValue = TripleSources.singleValue(funcUri, SPINX.JAVA_SCRIPT_CODE_PROPERTY, store);
		String code = (codeValue instanceof Literal) ? ((Literal) codeValue).getLabel() : null;
		Value fileValue = TripleSources.singleValue(funcUri, SPINX.JAVA_SCRIPT_FILE_PROPERTY, store);
		String file = (fileValue instanceof Literal) ? ((Literal) fileValue).getLabel() : null;
		if (code == null && file == null) {
			return null;
		}

		if (code == null) {
			code = funcUri.getLocalName();
		}

		ScriptEngine engine = scriptManager.getEngineByName("javascript");

		if (engine == null) {
			throw new UnsupportedOperationException("No javascript engine available!");
		}

		try {
			if (file != null) {
				String ns = funcUri.getNamespace();
				try (Reader reader = new InputStreamReader(
						new URL(new URL(ns.substring(0, ns.length() - 1)), file).openStream())) {
					engine.eval(reader);
				} catch (IOException e) {
					throw new QueryEvaluationException(e);
				}
			}
		} catch (ScriptException e) {
			throw new QueryEvaluationException(e);
		}

		Value returnValue = TripleSources.singleValue(funcUri, SPIN.RETURN_TYPE_PROPERTY, store);

		Map<IRI, Argument> templateArgs = parser.parseArguments(funcUri, store);

		SpinxFunction func = new SpinxFunction(funcUri.stringValue());
		func.setScriptEngine(engine);
		func.setScript(code);
		func.setReturnType((returnValue instanceof IRI) ? (IRI) returnValue : null);
		List<IRI> orderedArgs = SpinParser.orderArguments(templateArgs.keySet());
		for (IRI IRI : orderedArgs) {
			Argument arg = templateArgs.get(IRI);
			func.addArgument(arg);
		}

		return func;
	}
}
