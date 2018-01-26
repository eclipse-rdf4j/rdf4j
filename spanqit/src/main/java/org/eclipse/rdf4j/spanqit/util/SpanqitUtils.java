package org.eclipse.rdf4j.spanqit.util;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.eclipse.rdf4j.spanqit.core.QueryElement;

@SuppressWarnings("javadoc")
public class SpanqitUtils {
	private static final String PAD = " ";

	public static <O> Optional<O> getOrCreateAndModifyOptional(Optional<O> optional, Supplier<O> getter, UnaryOperator<O> operator) {
		return Optional.of(operator.apply(optional.orElseGet(getter)));
	}
	
	public static void appendAndNewlineIfPresent(Optional<? extends QueryElement> elementOptional, StringBuilder builder) {
		appendQueryElementIfPresent(elementOptional, builder, null, "\n");
	}
	
	public static void appendQueryElementIfPresent(Optional<? extends QueryElement> queryElementOptional, StringBuilder builder, String prefix, String suffix) {
		appendStringIfPresent(queryElementOptional.map(QueryElement::getQueryString), builder, prefix, suffix);
	}
	
	public static void appendStringIfPresent(Optional<String> stringOptional, StringBuilder builder, String prefix, String suffix) {
		Optional<String> preOpt = Optional.ofNullable(prefix);
		Optional<String> sufOpt = Optional.ofNullable(suffix);
		
		stringOptional.ifPresent(string -> {
			preOpt.ifPresent(p -> builder.append(p));
			builder.append(string);
			sufOpt.ifPresent(s -> builder.append(s));
		});
	}
	
	public static String getBracedString(String contents) {
		return getEnclosedString("{", "}", contents);
	}

	public static String getBracketedString(String contents) {
		return getEnclosedString("[", "]", contents);
	}
	
	public static String getParenthesizedString(String contents) {
		return getEnclosedString("(", ")", contents);
	}

	public static String getQuotedString(String contents) {
		return getEnclosedString("\"", "\"", contents, false);
	}

	/**
	 * For string literals that contain single- or double-quotes
	 * 
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynLiterals">
	 * RDF Literal Syntax</a>
	 * @param contents
	 * @return a "long quoted" string
	 */
	public static String getLongQuotedString(String contents) {
		return getEnclosedString("'''", "'''", contents, false);
	}
	
	private static String getEnclosedString(String open, String close,
			String contents) {
		return getEnclosedString(open, close, contents, true);
	}

	private static String getEnclosedString(String open, String close,
			String contents, boolean pad) {
		StringBuilder es = new StringBuilder();

		es.append(open);
		if (contents != null && !contents.isEmpty()) {
			es.append(contents);
			if (pad) {
				es.insert(open.length(), PAD).append(PAD);
			}
		} else {
			es.append(PAD);
		}
		es.append(close);

		return es.toString();
	}
}