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
package org.eclipse.rdf4j.spin.function;

import java.util.ArrayList;
import java.util.List;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.spin.Argument;

import com.google.common.base.Joiner;

public class SpinxFunction implements TransientFunction {

	private final String uri;

	private final List<Argument> arguments = new ArrayList<>(4);

	private ScriptEngine scriptEngine;

	private CompiledScript compiledScript;

	private String script;

	private IRI returnType;

	public SpinxFunction(String uri) {
		this.uri = uri;
	}

	public void setScriptEngine(ScriptEngine engine) {
		this.scriptEngine = engine;
	}

	public ScriptEngine getScriptEngine() {
		return scriptEngine;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getScript() {
		return script;
	}

	public void setReturnType(IRI datatype) {
		this.returnType = datatype;
	}

	public IRI getReturnType() {
		return returnType;
	}

	public void addArgument(Argument arg) {
		arguments.add(arg);
	}

	public List<Argument> getArguments() {
		return arguments;
	}

	@Override
	public String toString() {
		return uri + "(" + Joiner.on(", ").join(arguments) + ")";
	}

	@Override
	public String getURI() {
		return uri;
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {

		Bindings bindings = scriptEngine.createBindings();
		for (int i = 0; i < args.length; i++) {
			Argument argument = arguments.get(i);
			Value arg = args[i];
			Object jsArg;
			if (arg instanceof Literal) {
				Literal argLiteral = (Literal) arg;
				if (XSD.INTEGER.equals(argLiteral.getDatatype())) {
					jsArg = argLiteral.intValue();
				} else if (XSD.DECIMAL.equals(argLiteral.getDatatype())) {
					jsArg = argLiteral.doubleValue();
				} else {
					jsArg = argLiteral.getLabel();
				}
			} else {
				jsArg = arg.stringValue();
			}
			bindings.put(argument.getPredicate().getLocalName(), jsArg);
		}

		Object result;
		try {
			if (compiledScript == null && scriptEngine instanceof Compilable) {
				compiledScript = ((Compilable) scriptEngine).compile(script);
			}
			if (compiledScript != null) {
				result = compiledScript.eval(bindings);
			} else {
				result = scriptEngine.eval(script, bindings);
			}
		} catch (ScriptException e) {
			throw new ValueExprEvaluationException(e);
		}

		ValueFactory vf = SimpleValueFactory.getInstance();
		return (returnType != null) ? vf.createLiteral(result.toString(), returnType) : vf.createIRI(result.toString());
	}

}
