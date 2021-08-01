package org.eclipse.rdf4j.spring.operationlog.log;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;

public class PseudoOperation {
	private String operation;
	private int valuesHash;

	private PseudoOperation(String operation, int valuesHash) {
		this.operation = operation;
		this.valuesHash = valuesHash;
	}

	public static PseudoOperation forGetSatements(Object... args) {
		return forMethodNameAndArgs("getStatements", args);
	}

	public static PseudoOperation forAdd(Object... args) {
		String argsString = getArgsString(args);
		return forMethodNameAndArgs("add", args);
	}

	public static PseudoOperation forRemove(Object... args) {
		return forMethodNameAndArgs("remove", args);
	}

	public static PseudoOperation forClear(Object... args) {
		String argsString = getArgsString(args);
		return forMethodNameAndArgs("clear", args);
	}

	public static PseudoOperation forHasStatement(Object... args) {
		return forMethodNameAndArgs("hasStatement", args);
	}

	public static PseudoOperation forMethodNameAndArgs(String methodName, Object... args) {
		String argsString = getArgsString(args);
		return new PseudoOperation(
				"RepositoryConnection." + methodName + "(" + argsString + ")",
				Arrays.hashCode(args));
	}

	public static PseudoOperation forSize(Object... args) {
		return forMethodNameAndArgs("size", args);
	}

	public String getOperation() {
		return operation;
	}

	public int getValuesHash() {
		return valuesHash;
	}

	private static String getArgsString(Object[] args) {
		if (args == null || args.length == 0) {
			return "";
		}
		return Arrays.stream(args)
				.map(o -> o == null ? "[null]" : o.getClass().getSimpleName())
				.collect(joining(", "));
	}
}
