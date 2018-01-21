package org.eclipse.rdf4j.spanqit.graphpattern;

import java.util.Arrays;
import java.util.function.Consumer;

import org.eclipse.rdf4j.spanqit.constraint.Expression;

/**
 * A SPARQL Graph Pattern that is not a triple pattern.
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GraphPattern">
 *      SPARQL Graph Patterns</a>
 */
public class GraphPatternNotTriple implements GraphPattern {
	private GraphPattern pattern;

	GraphPatternNotTriple() {
		this(new GroupGraphPattern());
	}

	GraphPatternNotTriple(GraphPattern other) {
		this.pattern = extractPattern(other);
	}

	/**
	 * Convert this graph pattern into a group graph pattern, combining this
	 * graph pattern with the given patterns: <br>
	 * 
	 * <pre>
	 * {
	 *   thisPattern .
	 *   pattern1 .
	 *   pattern2 .
	 *   ...
	 *   patternN
	 * }
	 * </pre>
	 * 
	 * @param patterns
	 *            the patterns to add
	 * @return this
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GroupPatterns">SPARQL
	 *      Group Graph Pattern</a>
	 */
	public GraphPatternNotTriple and(GraphPattern... patterns) {
		if (patterns != null && patterns.length > 0) {
			GroupGraphPattern groupPattern = new GroupGraphPattern(pattern);
			extractAndAddPatterns(groupPattern::and, patterns);

			pattern = groupPattern;
		}
		
		return this;
	}

	/**
	 * Convert this graph pattern into an alternative graph pattern, combining
	 * this graph pattern with the given patterns: <br>
	 * 
	 * <pre>
	 * {
	 *   { thisPattern } UNION
	 *   { pattern1 } UNION
	 *   { pattern2 } UNION
	 *   ...
	 *   { patternN }
	 * }
	 * </pre>
	 * 
	 * @param patterns
	 *            the patterns to add
	 * @return this
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#alternatives">SPARQL
	 *      Alternative Graph Pattern</a>
	 */
	public GraphPatternNotTriple union(GraphPattern... patterns) {
		AlternativeGraphPattern alternativePattern = new AlternativeGraphPattern(pattern);
		extractAndAddPatterns(alternativePattern::union, patterns);

		pattern = alternativePattern;

		return this;
	}

	/**
	 * Convert this graph pattern into an optional group graph pattern: <br>
	 * 
	 * <pre>
	 * OPTIONAL {thisPattern}
	 * </pre>
	 * 
	 * @return this
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#optionals">
	 *      SPARQL Optional Graph Patterns</a>
	 */
	public GraphPatternNotTriple optional() {
		return optional(true);
	}

	/**
	 * Specify if this graph pattern should be optional.
	 * 
	 * <p>
	 * NOTE: This converts this graph pattern into a group graph pattern.
	 * 
	 * @param isOptional
	 *            if this graph pattern should be optional or not
	 * @return this
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#optionals">
	 *      SPARQL Optional Graph Patterns</a>
	 */
	public GraphPatternNotTriple optional(boolean isOptional) {
		pattern = new GroupGraphPattern(pattern).optional(isOptional);

		return this;
	}

	/**
	 * Convert this graph pattern into a group graph pattern and add a filter: <br>
	 * 
	 * <pre>
	 * {
	 *   thisPattern
	 *   FILTER { constraint }
	 * }
	 * </pre>
	 * 
	 * @param constraint
	 *            the filter constraint
	 * @return this
	 * 
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#termConstraint">
	 *      SPARQL Filter</a>
	 */
	public GraphPatternNotTriple filter(Expression<?> constraint) {
		pattern = new GroupGraphPattern(pattern).filter(constraint);

		return this;
	}

	/**
	 * Create an <code>EXISTS{}</code> filter expression with the given graph
	 * patterns and add it to this graph pattern (converting this to a group
	 * graph pattern in the process): <br>
	 * 
	 * <pre>
	 * {
	 * 	thisPattern
	 * 	FILTER EXISTS { patterns }
	 * }
	 * </pre>
	 * 
	 * @param patterns
	 *            the patterns to pass as arguments to the <code>EXISTS</code>
	 *            expression
	 * 
	 * @return this
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#neg-pattern">
	 *      Filtering using Graph Pattern</a>
	 */
	public GraphPatternNotTriple filterExists(GraphPattern... patterns) {
		filterExists(true, patterns);

		return this;
	}

	/**
	 * Create a <code>NOT EXISTS{}</code> filter expression with the given graph
	 * patterns and add it to this graph pattern (converting this to a group
	 * graph pattern in the process): <br>
	 * 
	 * <pre>
	 * {
	 * 	thisPattern
	 * 	FILTER NOT EXISTS { patterns }
	 * }
	 * </pre>
	 * 
	 * @param patterns
	 *            the patterns to pass as arguments to the
	 *            <code>NOT EXISTS</code> expression
	 * 
	 * @return this
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#neg-pattern">
	 *      Filtering using Graph Pattern</a>
	 */
	public GraphPatternNotTriple filterNotExists(GraphPattern... patterns) {
		filterExists(false, patterns);

		return this;
	}

	/**
	 * Create a <code>MINUS</code> graph pattern with the given graph patterns
	 * and add it to this graph pattern (converting this to a group graph
	 * pattern in the process): <br>
	 * 
	 * <pre>
	 * {
	 * 	thisPattern
	 * 	MINUS { patterns }
	 * }
	 * </pre>
	 * 
	 * @param patterns
	 *            the patterns to construct the <code>MINUS</code> graph pattern
	 *            with
	 * @return this
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#neg-minus">
	 *      SPARQL MINUS Graph Pattern</a>
	 */
	public GraphPatternNotTriple minus(GraphPattern... patterns) {
		MinusGraphPattern minus = new MinusGraphPattern();
		extractAndAddPatterns(minus::and, patterns);
		
		pattern = new GroupGraphPattern(pattern).and(minus);

		return this;
	}

	/**
	 * Convert this graph pattern into a named group graph pattern: <br>
	 * 
	 * <pre>
	 * GRAPH graphName { thisPattern }
	 * </pre>
	 * 
	 * @param name
	 *            the name to specify
	 * @return this
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#queryDataset">
	 *      Specifying Datasets in SPARQL Queries</a>
	 */
	public GraphPatternNotTriple from(GraphName name) {
		pattern = new GroupGraphPattern(pattern).from(name);

		return this;
	}

	@Override
	public boolean isEmpty() {
		return pattern.isEmpty();
	}

	public boolean hasQualifier() {
        return (pattern instanceof GroupGraphPattern) && ((GroupGraphPattern) pattern).hasQualifier();
    }

	private void filterExists(boolean exists, GraphPattern... patterns) {
		FilterExistsGraphPattern filterExists = new FilterExistsGraphPattern().exists(exists);
		extractAndAddPatterns(filterExists::and, patterns);

		pattern = new GroupGraphPattern(pattern).and(filterExists);
	}

	private void extractAndAddPatterns(Consumer<? super GraphPattern> action, GraphPattern... patterns) {
		Arrays.stream(patterns).map(this::extractPattern).forEach(action);
	}
	
	private GraphPattern extractPattern(GraphPattern pattern) {
		if (pattern instanceof GraphPatternNotTriple) {
			return ((GraphPatternNotTriple) pattern).pattern;
		} else {
			return pattern;
		}
	}

	@Override
	public String getQueryString() {
		return pattern.getQueryString();
	}
}