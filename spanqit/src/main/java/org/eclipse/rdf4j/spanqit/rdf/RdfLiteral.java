package org.eclipse.rdf4j.spanqit.rdf;

import java.util.Optional;

import org.eclipse.rdf4j.spanqit.util.SpanqitUtils;

/**
 * Denotes an RDF literal
 * 
 * @param <T> the datatype of the literal
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2014/NOTE-rdf11-primer-20140225/#section-literal">
 *      RDF Literals</a>
 * @see <a
 * 		href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynLiterals">
 * 		RDF Literal Syntax</a>
 */
public abstract class RdfLiteral<T> implements RdfValue { 
	protected T value;
	
	private RdfLiteral(T value) {
		this.value = value;
	}

	@Override
	public String getQueryString() {
		return String.valueOf(value);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}
		
		if(!(obj instanceof RdfLiteral)) {
			return false;
		}

		RdfLiteral<?> other = (RdfLiteral<?>) obj;
		if(value == null) {
			return other.value == null;
		} else {
			return value.equals(other.value);
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());

		return result;
	}

	/**
	 * Represents an RDF string literal
	 */
	public static class StringLiteral extends RdfLiteral<String> {
		private static final String DATATYPE_SPECIFIER = "^^";
		private static final String LANG_TAG_SPECIFIER = "@";
		
		private Optional<Iri> dataType = Optional.empty();
		private Optional<String> languageTag = Optional.empty();
		
		StringLiteral(String stringValue) {
			super(stringValue);
		}
		
		StringLiteral(String stringValue, Iri dataType) {
			super(stringValue);
			this.dataType = Optional.ofNullable(dataType);
		}
		
		StringLiteral(String stringValue, String languageTag) {
			super(stringValue);
			this.languageTag = Optional.ofNullable(languageTag);
		}
				
		@Override
		public String getQueryString() {
			StringBuilder literal = new StringBuilder();
			
			if(value.contains("'") || value.contains("\"")) {
				literal.append(SpanqitUtils.getLongQuotedString(value));
			} else {
				literal.append(SpanqitUtils.getQuotedString(value));
			}
			
			SpanqitUtils.appendQueryElementIfPresent(dataType, literal, DATATYPE_SPECIFIER, null);
			SpanqitUtils.appendStringIfPresent(languageTag, literal, LANG_TAG_SPECIFIER, null);
			
			return literal.toString();
		}
	}
	
	/**
	 * Represents an RDF number literal
	 */
	public static class NumericLiteral extends RdfLiteral<Number> {
		NumericLiteral(Number numbervalue) {
			super(numbervalue);
		}
	}
	
	/**
	 * Represents an RDF boolean literal
	 */
	public static class BooleanLiteral extends RdfLiteral<Boolean> {
		BooleanLiteral(Boolean boolValue) {
			super(boolValue);
		}
	}
}