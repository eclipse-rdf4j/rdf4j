/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.rdf4j.model.Path.alt;
import static org.eclipse.rdf4j.model.Path.format;
import static org.eclipse.rdf4j.model.Path.inverse;
import static org.eclipse.rdf4j.model.Path.multiple;
import static org.eclipse.rdf4j.model.Path.not;
import static org.eclipse.rdf4j.model.Path.optional;
import static org.eclipse.rdf4j.model.Path.repeatable;
import static org.eclipse.rdf4j.model.Path.seq;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Path.Alt;
import org.eclipse.rdf4j.model.Path.Hop;
import org.eclipse.rdf4j.model.Path.Not;
import org.eclipse.rdf4j.model.Path.Seq;
import org.eclipse.rdf4j.model.base.AbstractIRI;
import org.junit.Test;

public final class PathTest {

	private static final IRI x = iri("x");
	private static final IRI y = iri("y");

	private static AbstractIRI iri(String name) {
		return new AbstractIRI() {

			@Override
			public String getNamespace() {
				return "test";
			}

			@Override
			public String getLocalName() {
				return name;
			}

		};
	}

	@Test
	public void testFormatNesteds() {
		assertThat(format(seq(inverse(optional(alt(x, seq(x, y)))), multiple(not(x))))).isEqualTo(format(
				"(^(%1$s|(%1$s/%2$s))?/!(%1$s)*)", format(x), format(y)
		));
	}

	public static final class Hops {

		private final Path path = x;

		private static IRI getIRI(Path path) {
			return path.accept(new Path.Visitor<IRI>() {

				@Override
				protected IRI visit(Hop hop) {
					return hop.getIRI();
				}

				@Override
				protected IRI visit(Seq seq) {
					return fail("unexpected seq path");
				}

				@Override
				protected IRI visit(Alt alt) {
					return fail("unexpected alt path");
				}

				@Override
				protected IRI visit(Not not) {
					return fail("unexpected not path");
				}

			});
		}

		@Test
		public void testCreation() {

			assertThat(getIRI(path)).isEqualTo(x);

			assertThat(path.isInverse()).isFalse();
			assertThat(path.isOptional()).isFalse();
			assertThat(path.isRepeatable()).isFalse();

		}

		@Test
		public void testInverse() {
			assertThat(inverse(path).isInverse()).isTrue();
			assertThat(inverse(inverse(path)).isInverse()).isTrue();
			assertThat(getIRI(inverse(path))).isEqualTo(x);
		}

		@Test
		public void testOptional() {
			assertThat(optional(path).isOptional()).isTrue();
			assertThat(optional(optional(path)).isOptional()).isTrue();
			assertThat(getIRI(optional(path))).isEqualTo(x);
		}

		@Test
		public void testRepeatable() {
			assertThat(repeatable(path).isRepeatable()).isTrue();
			assertThat(repeatable(repeatable(path)).isRepeatable()).isTrue();
			assertThat(getIRI(repeatable(path))).isEqualTo(x);
		}

		@Test
		public void testFormat() {
			assertThat(format(path)).isEqualTo(format("%s", format(x)));
			assertThat(format(inverse(path))).isEqualTo(format("^%s", format(x)));
			assertThat(format(optional(path))).isEqualTo(format("%s?", format(x)));
			assertThat(format(repeatable(path))).isEqualTo(format("%s+", format(x)));
			assertThat(format(multiple(path))).isEqualTo(format("%s*", format(x)));
		}

	}

	public static final class Seqs {

		private final Path path = seq(x, y);

		private static List<Path> getSeq(Path path) {
			return path.accept(new Path.Visitor<List<Path>>() {

				@Override
				protected List<Path> visit(Hop hop) {
					return fail("unexpected step path");
				}

				@Override
				protected List<Path> visit(Seq seq) {
					return seq.getPaths();
				}

				@Override
				protected List<Path> visit(Alt alt) {
					return fail("unexpected alt path");
				}

				@Override
				protected List<Path> visit(Not not) {
					return fail("unexpected not path");
				}

			});
		}

		@Test
		public void testCreation() {

			assertThat(getSeq(path)).containsExactly(x, y);

			assertThat(path.isInverse()).isFalse();
			assertThat(path.isOptional()).isFalse();
			assertThat(path.isRepeatable()).isFalse();

		}

		@Test
		public void testInverse() {
			assertThat(inverse(path).isInverse()).isTrue();
			assertThat(inverse(inverse(path)).isInverse()).isTrue();
		}

		@Test
		public void testOptional() {
			assertThat(optional(path).isOptional()).isTrue();
			assertThat(optional(optional(path)).isOptional()).isTrue();
		}

		@Test
		public void testRepeatable() {
			assertThat(repeatable(path).isRepeatable()).isTrue();
			assertThat(repeatable(repeatable(path)).isRepeatable()).isTrue();
		}

		@Test
		public void testFormat() {
			assertThat(format(path)).isEqualTo(format("(%s/%s)", format(x), format(y)));
			assertThat(format(inverse(path))).isEqualTo(format("^(%s/%s)", format(x), format(y)));
			assertThat(format(optional(path))).isEqualTo(format("(%s/%s)?", format(x), format(y)));
			assertThat(format(repeatable(path))).isEqualTo(format("(%s/%s)+", format(x), format(y)));
			assertThat(format(multiple(path))).isEqualTo(format("(%s/%s)*", format(x), format(y)));
		}

		@Test
		public void testOptimizeSingleton() {
			assertThat(seq(x)).isEqualTo(x);
		}

		@Test
		public void testReportEmpty() {
			assertThatIllegalArgumentException().isThrownBy(Path::seq);
		}

	}

	public static final class Alts {

		private final Path path = alt(x, y);

		private static Set<Path> getAlt(Path path) {
			return path.accept(new Path.Visitor<Set<Path>>() {

				@Override
				protected Set<Path> visit(Hop hop) {
					return fail("unexpected step path");
				}

				@Override
				protected Set<Path> visit(Seq seq) {
					return fail("unexpected seq path");
				}

				@Override
				protected Set<Path> visit(Alt alt) {
					return alt.getPaths();
				}

				@Override
				protected Set<Path> visit(Not not) {
					return fail("unexpected not path");
				}

			});
		}

		@Test
		public void testCreation() {

			assertThat(getAlt(path)).containsExactly(x, y);

			assertThat(path.isInverse()).isFalse();
			assertThat(path.isOptional()).isFalse();
			assertThat(path.isRepeatable()).isFalse();

		}

		@Test
		public void testInverse() {
			assertThat(inverse(path).isInverse()).isTrue();
			assertThat(inverse(inverse(path)).isInverse()).isTrue();
		}

		@Test
		public void testOptional() {
			assertThat(optional(path).isOptional()).isTrue();
			assertThat(optional(optional(path)).isOptional()).isTrue();
		}

		@Test
		public void testRepeatable() {
			assertThat(repeatable(path).isRepeatable()).isTrue();
			assertThat(repeatable(repeatable(path)).isRepeatable()).isTrue();
		}

		@Test
		public void testFormatAlts() {
			assertThat(format(path)).isEqualTo(format("(%s|%s)", format(x), format(y)));
			assertThat(format(inverse(path))).isEqualTo(format("^(%s|%s)", format(x), format(y)));
			assertThat(format(optional(path))).isEqualTo(format("(%s|%s)?", format(x), format(y)));
			assertThat(format(repeatable(path))).isEqualTo(format("(%s|%s)+", format(x), format(y)));
			assertThat(format(multiple(path))).isEqualTo(format("(%s|%s)*", format(x), format(y)));
		}

		@Test
		public void testOptimizeSingleton() {
			assertThat(alt(x)).isEqualTo(x);
		}

		@Test
		public void testReportEmpty() {
			assertThatIllegalArgumentException().isThrownBy(Path::alt);
		}

	}

	public static final class Nots {

		private final Path path = not(x, y);

		private static Set<Step> getNot(Path path) {
			return path.accept(new Path.Visitor<Set<Step>>() {

				@Override
				protected Set<Step> visit(Path.Hop hop) {
					return fail("unexpected step path");
				}

				@Override
				protected Set<Step> visit(Seq seq) {
					return fail("unexpected seq path");
				}

				@Override
				protected Set<Step> visit(Alt alt) {
					return fail("unexpected alt path");
				}

				@Override
				protected Set<Step> visit(Not not) {
					return not.getSteps();
				}

			});
		}

		@Test
		public void testCreation() {

			assertThat(getNot(path)).containsExactly(x, y);

			assertThat(path.isInverse()).isFalse();
			assertThat(path.isOptional()).isFalse();
			assertThat(path.isRepeatable()).isFalse();

		}

		@Test
		public void testInverse() {
			assertThat(inverse(path).isInverse()).isTrue();
			assertThat(inverse(inverse(path)).isInverse()).isTrue();
		}

		@Test
		public void testOptional() {
			assertThat(optional(path).isOptional()).isTrue();
			assertThat(optional(optional(path)).isOptional()).isTrue();
		}

		@Test
		public void testRepeatable() {
			assertThat(repeatable(path).isRepeatable()).isTrue();
			assertThat(repeatable(repeatable(path)).isRepeatable()).isTrue();
		}

		@Test
		public void testFormatNots() {
			assertThat(format(path)).isEqualTo(format("!(%s|%s)", format(x), format(y)));
			assertThat(format(inverse(path))).isEqualTo(format("^!(%s|%s)", format(x), format(y)));
			assertThat(format(optional(path))).isEqualTo(format("!(%s|%s)?", format(x), format(y)));
			assertThat(format(repeatable(path))).isEqualTo(format("!(%s|%s)+", format(x), format(y)));
			assertThat(format(multiple(path))).isEqualTo(format("!(%s|%s)*", format(x), format(y)));
		}

		@Test
		public void testReportEmpty() {
			assertThatIllegalArgumentException().isThrownBy(Path::not);
		}

	}

}
