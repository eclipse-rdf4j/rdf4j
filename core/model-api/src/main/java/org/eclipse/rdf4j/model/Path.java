/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Variable length property path.
 *
 * <p>
 * Represents a directed variable length property path between two {@linkplain Value values} in an RDF graph.
 * </p>
 *
 * @author Alessandro Bollini
 * @see <a href="https://www.w3.org/TR/sparql11-query/#propertypaths">SPARQL 1.1 Query Language – 9.1 Property Path
 *      Syntax</a>
 * @since 3.7.0
 */
public interface Path extends Link {

	/**
	 * Creates a sequence path.
	 *
	 * @param paths the paths to be included in the sequence path
	 *
	 * @return a new sequence path including the given {@code paths}
	 *
	 * @throws NullPointerException if {@code paths} is null or contains null elements
	 */
	public static Path seq(Path... paths) {

		if (paths == null || Arrays.stream(paths).anyMatch(Objects::isNull)) {
			throw new NullPointerException("null paths");
		}

		if (paths.length == 0) {
			throw new IllegalArgumentException("empty path list");
		}

		return seq(asList(paths));
	}

	/**
	 * Creates a sequence path.
	 *
	 * @param paths the paths to be included in the sequence path
	 *
	 * @return a new sequence path including the given {@code paths}
	 *
	 * @throws NullPointerException if {@code paths} is null or contains null elements
	 */
	public static Path seq(Collection<? extends Path> paths) {

		if (paths == null || paths.stream().anyMatch(Objects::isNull)) {
			throw new NullPointerException("null paths");
		}

		if (paths.isEmpty()) {
			throw new IllegalArgumentException("empty path list");
		}

		return paths.size() == 1 ? paths.iterator().next()
				: new PathDefault.Seq(
						unmodifiableList(new ArrayList<>(paths)), false, false, false
				);
	}

	/**
	 * Creates an alternative path.
	 *
	 * @param paths the paths to be included in the alternative path
	 *
	 * @return a new alternative path including the given {@code paths}
	 *
	 * @throws NullPointerException if {@code paths} is null or contains null elements
	 */
	public static Path alt(Path... paths) {

		if (paths == null || Arrays.stream(paths).anyMatch(Objects::isNull)) {
			throw new NullPointerException("null paths");
		}

		if (paths.length == 0) {
			throw new IllegalArgumentException("empty path set");
		}

		return alt(asList(paths));
	}

	/**
	 * Creates an alternative path.
	 *
	 * @param paths the paths to be included in the alternative path
	 *
	 * @return a new alternative path including the given {@code paths}
	 *
	 * @throws NullPointerException if {@code paths} is null or contains null elements
	 */
	public static Path alt(Collection<? extends Path> paths) {

		if (paths == null || paths.stream().anyMatch(Objects::isNull)) {
			throw new NullPointerException("null paths");
		}

		if (paths.isEmpty()) {
			throw new IllegalArgumentException("empty path set");
		}

		return paths.size() == 1 ? paths.iterator().next()
				: new PathDefault.Alt(
						unmodifiableSet(new LinkedHashSet<>(paths)), false, false, false
				);
	}

	/**
	 * Creates a negated property set.
	 *
	 * @param steps the predicate path steps to be excluded from the negated property set
	 *
	 * @return a new negated property set excluding the given {@code steps}
	 *
	 * @throws NullPointerException if {@code steps} is null or contains null elements
	 */
	public static Path not(Step... steps) {

		if (steps == null || Arrays.stream(steps).anyMatch(Objects::isNull)) {
			throw new NullPointerException("null steps");
		}

		if (steps.length == 0) {
			throw new IllegalArgumentException("empty path set");
		}

		return not(asList(steps));
	}

	/**
	 * Creates a negated property set.
	 *
	 * @param steps the predicate paths to be excluded from the negated property set
	 *
	 * @return a new negated property set excluding the given {@code steps}
	 *
	 * @throws NullPointerException     if {@code steps} is null or contains null elements
	 * @throws IllegalArgumentException if {@code steps} contains either {@linkplain #isOptional() optional} or
	 *                                  {@linkplain #isRepeatable() repeatable} steps
	 */
	public static Path not(Collection<? extends Step> steps) {

		if (steps == null || steps.stream().anyMatch(Objects::isNull)) {
			throw new NullPointerException("null paths");
		}

		if (steps.isEmpty()) {
			throw new IllegalArgumentException("empty path set");
		}

		return new PathDefault.Not(
				unmodifiableSet(new LinkedHashSet<>(steps)), false, false, false
		);
	}

	/**
	 * Creates an inverse path.
	 *
	 * @param path the path to be inverted
	 *
	 * @return a new inverse path derived from {@code path}
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public static Path inverse(Path path) {

		if (path == null) {
			throw new NullPointerException("null path");
		}

		return path.accept(new Visitor<Path>() {

			@Override
			protected Path visit(Hop hop) {
				return new PathDefault.Hop(hop.getIRI(), true, hop.isOptional(), hop.isRepeatable());
			}

			@Override
			protected Path visit(Seq seq) {
				return new PathDefault.Seq(seq.getPaths(), true, seq.isOptional(), seq.isRepeatable());
			}

			@Override
			protected Path visit(Alt alt) {
				return new PathDefault.Alt(alt.getPaths(), true, alt.isOptional(), alt.isRepeatable());
			}

			@Override
			protected Path visit(Not not) {
				return new PathDefault.Not(not.getSteps(), true, not.isOptional(), not.isRepeatable());
			}

		});
	}

	/**
	 * Creates an optional path.
	 *
	 * @param path the path to be made optional
	 *
	 * @return a new optional path derived from {@code path}
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public static Path optional(Path path) {

		if (path == null) {
			throw new NullPointerException("null path");
		}

		return path.accept(new Visitor<Path>() {

			@Override
			protected Path visit(Hop hop) {
				return new PathDefault.Hop(hop.getIRI(), hop.isInverse(), true, hop.isRepeatable());
			}

			@Override
			protected Path visit(Seq seq) {
				return new PathDefault.Seq(seq.getPaths(), seq.isInverse(), true, seq.isRepeatable());
			}

			@Override
			protected Path visit(Alt alt) {
				return new PathDefault.Alt(alt.getPaths(), alt.isInverse(), true, alt.isRepeatable());
			}

			@Override
			protected Path visit(Not not) {
				return new PathDefault.Not(not.getSteps(), not.isInverse(), true, not.isRepeatable());
			}

		});
	}

	/**
	 * Creates a repeatable path.
	 *
	 * @param path the path to be made repeatable
	 *
	 * @return a new repeatable path derived from {@code path}
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public static Path repeatable(Path path) {

		if (path == null) {
			throw new NullPointerException("null path");
		}

		return path.accept(new Visitor<Path>() {

			@Override
			protected Path visit(Hop hop) {
				return new PathDefault.Hop(hop.getIRI(), hop.isInverse(), hop.isOptional(), true);
			}

			@Override
			protected Path visit(Seq seq) {
				return new PathDefault.Seq(seq.getPaths(), seq.isInverse(), seq.isOptional(), true);
			}

			@Override
			protected Path visit(Alt alt) {
				return new PathDefault.Alt(alt.getPaths(), alt.isInverse(), alt.isOptional(), true);
			}

			@Override
			protected Path visit(Not not) {
				return new PathDefault.Not(not.getSteps(), not.isInverse(), not.isOptional(), true);
			}

		});
	}

	/**
	 * Creates a multiple ({@linkplain #optional(Path) optional}/{@linkplain #repeatable(Path) repeatable}) path.
	 *
	 * @param path the path to be made multiple
	 *
	 * @return a new multiple path derived from {@code path}
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public static Path multiple(Path path) {

		if (path == null) {
			throw new NullPointerException("null path");
		}

		return path.accept(new Visitor<Path>() {

			@Override
			protected Path visit(Hop hop) {
				return new PathDefault.Hop(hop.getIRI(), hop.isInverse(), true, true);
			}

			@Override
			protected Path visit(Seq seq) {
				return new PathDefault.Seq(seq.getPaths(), seq.isInverse(), true, true);
			}

			@Override
			protected Path visit(Alt alt) {
				return new PathDefault.Alt(alt.getPaths(), alt.isInverse(), true, true);
			}

			@Override
			protected Path visit(Not not) {
				return new PathDefault.Not(not.getSteps(), not.isInverse(), true, true);
			}

		});
	}

	/**
	 * Converts a path to a textual representation.
	 *
	 * @param path the path to be formatted
	 *
	 * @return a fully parenthesized SPARQL expression representing {@code path} with full IRIs
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public static String format(Path path) {

		if (path == null) {
			throw new NullPointerException("null path");
		}

		return path.accept(new PathFormatter()).toString();
	}

	/**
	 * Checks if this path is optional.
	 *
	 * @return {@code true}, if this path is optional; {@code false}, otherwise
	 */
	public boolean isOptional();

	/**
	 * Checks if this path is repeatable.
	 *
	 * @return {@code true}, if this path is repeatable; {@code false}, otherwise
	 */
	public boolean isRepeatable();

	/**
	 * Accepts a path visitor.
	 *
	 * @param visitor a path visitor
	 * @param <V>     the type of the result returned by {@code visitor}
	 *
	 * @return the (possibly null) value computed by {@code visitor} according to the type of this path
	 *
	 * @throws NullPointerException if {@code visitor} is null
	 */
	public <V> V accept(Visitor<V> visitor);

	/**
	 * Predicate path.
	 *
	 * <p>
	 * Represents a path composed by a single predicate IRI.
	 * </p>
	 *
	 * @implNote {@link #equals(Object)}/{@link #hashCode()} contracts conflicts with those specified by {@link IRI},
	 *           which extends this interface: in order to preserve equality consistency, plain direct predicate paths
	 *           must be represented only with IRIs; concrete classes implementing this interface must ensure that no
	 *           instance is created with coincident {@code false}
	 *           {@link #isInverse()}/{@link #isOptional()}/{@link #isRepeatable()} values.
	 */
	public static interface Hop extends Path {

		/**
		 * Retrieves the IRI of this predicate path.
		 *
		 * @return the IRI of this path
		 */
		public IRI getIRI();

		/**
		 * Checks if this predicate path is equal to a reference object.
		 *
		 * @param object the reference object
		 *
		 * @return {@code true}, if the reference object is an instance of {@code Step} and the {@link #getIRI()},
		 *         {@link #isInverse()}, {@link #isOptional()} and {@link #isRepeatable()} values ot this path and of
		 *         the reference object are equal to each other; {@code false}, otherwise
		 *
		 * @implNote In order to ensure interoperability of concrete classes implementing this interface, the
		 *           {@code equals(Object)} method must be implemented exactly as described in this specs.
		 */
		public boolean equals(Object object);

		/**
		 * Computes the hash code of this predicate path.
		 *
		 * @return a hash code for this path computed as:<br>
		 *
		 *         {@link #getIRI()}{@code .hashCode()}<br>
		 *         {@code ^Boolean.hashCode(}{@link #isInverse()}{@code )}<br>
		 *         {@code ^Boolean.hashCode(}{@link #isOptional()}{@code )}<br>
		 *         {@code ^Boolean.hashCode(}{@link #isRepeatable()}{@code )}
		 *
		 * @implNote In order to ensure interoperability of concrete classes implementing this interface, the
		 *           {@code hashCode()} method must be implemented exactly as described in this specs.
		 */
		public int hashCode();

	}

	/**
	 * Sequence path.
	 *
	 * <p>
	 * Represents a path composed by a list of sequential paths.
	 * </p>
	 */
	public static interface Seq extends Path {

		/**
		 * Retrieves the paths of this sequence path.
		 *
		 * @return the paths of this sequence path
		 */
		public List<Path> getPaths();

		/**
		 * Checks if this sequence path is equal to a reference object.
		 *
		 * @param object the reference object
		 *
		 * @return {@code true}, if the reference object is an instance of {@code Seq} and the {@link #getPaths()},
		 *         {@link #isInverse()}, {@link #isOptional()} and {@link #isRepeatable()} values ot this path and of
		 *         the reference object are equal to each other; {@code false}, otherwise
		 *
		 * @implNote In order to ensure interoperability of concrete classes implementing this interface, the
		 *           {@code equals(Object)} method must be implemented exactly as described in this specs.
		 */
		public boolean equals(Object object);

		/**
		 * Computes the hash code of this sequence path.
		 *
		 * @return a hash code for this path computed as:
		 *
		 *         {@link #getPaths()}{@code .hashCode()} {@code ^Boolean.hashCode(}{@link #isInverse()}{@code )}
		 *         {@code ^Boolean.hashCode(}{@link #isOptional()}{@code )}
		 *         {@code ^Boolean.hashCode(}{@link #isRepeatable()}{@code )}
		 *
		 * @implNote In order to ensure interoperability of concrete classes implementing this interface, the
		 *           {@code hashCode()} method must be implemented exactly as described in this specs.
		 */
		public int hashCode();

	}

	/**
	 * Alternative path.
	 *
	 * <p>
	 * Represents a path composed by a set of alternative paths.
	 * </p>
	 */
	public static interface Alt extends Path {

		/**
		 * Retrieves the paths of this alternative path.
		 *
		 * @return the paths of this path
		 */
		public Set<Path> getPaths();

		/**
		 * Checks if this alternative path is equal to a reference object.
		 *
		 * @param object the reference object
		 *
		 * @return {@code true}, if the reference object is an instance of {@code Alt} and the {@link #getPaths()},
		 *         {@link #isInverse()}, {@link #isOptional()} and {@link #isRepeatable()} values ot this path and of
		 *         the reference object are equal to each other; {@code false}, otherwise
		 *
		 * @implNote In order to ensure interoperability of concrete classes implementing this interface, the
		 *           {@code equals(Object)} method must be implemented exactly as described in this specs.
		 */
		public boolean equals(Object object);

		/**
		 * Computes the hash code of this alternative path.
		 *
		 * @return a hash code for this path computed as:
		 *
		 *         {@link #getPaths()}{@code .hashCode()} {@code ^Boolean.hashCode(}{@link #isInverse()}{@code )}
		 *         {@code ^Boolean.hashCode(}{@link #isOptional()}{@code )}
		 *         {@code ^Boolean.hashCode(}{@link #isRepeatable()}{@code )}
		 *
		 * @implNote In order to ensure interoperability of concrete classes implementing this interface, the
		 *           {@code hashCode()} method must be implemented exactly as described in this specs.
		 */
		public int hashCode();

	}

	/**
	 * Negated property set.
	 *
	 * <p>
	 * Represents a path composed by a set of excluded {@linkplain Step predicate path steps}.
	 * </p>
	 */
	public static interface Not extends Path {

		/**
		 * Retrieves the predicate path steps of this negated property set.
		 *
		 * @return the predicate path stepss of this negated property set
		 */
		public Set<Step> getSteps();

		/**
		 * Checks if this negated property set is equal to a reference object.
		 *
		 * @param object the reference object
		 *
		 * @return {@code true}, if the reference object is an instance of {@code Not} and the {@link #getSteps()},
		 *         {@link #isInverse()}, {@link #isOptional()} and {@link #isRepeatable()} values ot this property set
		 *         and of the reference object are equal to each other; {@code false}, otherwise
		 *
		 * @implNote In order to ensure interoperability of concrete classes implementing this interface, the
		 *           {@code equals(Object)} method must be implemented exactly as described in this specs.
		 */
		public boolean equals(Object object);

		/**
		 * Computes the hash code of this negated property set.
		 *
		 * @return a hash code for this property set computed as:
		 *
		 *         {@link #getSteps()}{@code .hashCode()} {@code ^Boolean.hashCode(}{@link #isInverse()}{@code )}
		 *         {@code ^Boolean.hashCode(}{@link #isOptional()}{@code )}
		 *         {@code ^Boolean.hashCode(}{@link #isRepeatable()}{@code )}
		 *
		 * @implNote In order to ensure interoperability of concrete classes implementing this interface, the
		 *           {@code hashCode()} method must be implemented exactly as described in this specs.
		 */
		public int hashCode();

	}

	/**
	 * Path visitor.
	 *
	 * <p>
	 * Visits paths returning a value computed according to their type.
	 * </p>
	 *
	 * @param <V> the type of the result returned by the visitor
	 */
	public abstract static class Visitor<V> {

		/**
		 * Visits a predicate path.
		 *
		 * @param hop the predicate path to be visited
		 *
		 * @return a value computed on the basis of the visited {@code step} path
		 *
		 * @throws NullPointerException if {@code step} is null
		 */
		protected abstract V visit(Hop hop);

		/**
		 * Visits a sequence path.
		 *
		 * @param seq the sequence path to be visited
		 *
		 * @return a value computed on the basis of the visited {@code seq} path
		 *
		 * @throws NullPointerException if {@code seq} is null
		 */
		protected abstract V visit(Seq seq);

		/**
		 * Visits an alternative path.
		 *
		 * @param alt the alternative path to be visited
		 *
		 * @return a value computed on the basis of the visited {@code alt} path
		 *
		 * @throws NullPointerException if {@code alt} is null
		 */
		protected abstract V visit(Alt alt);

		/**
		 * Visits a negated property set.
		 *
		 * @param not the egated property set to be visited
		 *
		 * @return a value computed on the basis of the visited {@code not} property set
		 *
		 * @throws NullPointerException if {@code not} is null
		 */
		protected abstract V visit(Not not);

	}

}
