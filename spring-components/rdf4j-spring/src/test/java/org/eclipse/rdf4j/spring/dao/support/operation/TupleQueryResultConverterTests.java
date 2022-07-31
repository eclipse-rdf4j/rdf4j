/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.support.operation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.spring.RDF4JSpringTestBase;
import org.eclipse.rdf4j.spring.dao.exception.IncorrectResultSetSizeException;
import org.eclipse.rdf4j.spring.domain.model.EX;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.eclipse.rdf4j.spring.util.QueryResultUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TupleQueryResultConverterTests extends RDF4JSpringTestBase {

	@Autowired
	private RDF4JTemplate rdf4JTemplate;

	private TupleQueryResultConverter resultConverter;

	@Test
	public void testConsumeResultMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		converter.consumeResult(tqr -> {
			Assertions.assertTrue(tqr.hasNext());
			Assertions.assertEquals(1, tqr.getBindingNames().size());
			Assertions.assertEquals(2, tqr.stream().count());
		});
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testConsumeResultSingle() {
		TupleQueryResultConverter converter = forSingle();
		converter.consumeResult(tqr -> {
			Assertions.assertTrue(tqr.hasNext());
			Assertions.assertEquals(1, tqr.getBindingNames().size());
			Assertions.assertEquals(1, tqr.stream().count());
		});
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testConsumeResultNothing() {
		TupleQueryResultConverter converter = forNothing();
		converter.consumeResult(tqr -> {
			Assertions.assertFalse(tqr.hasNext());
			Assertions.assertEquals(1, tqr.getBindingNames().size());
			Assertions.assertEquals(0, tqr.stream().count());
		});
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testApplyToResultMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		Set<IRI> artists = converter.applyToResult(res -> res.stream()
				.map(b -> QueryResultUtils.getIRI(b, "artist"))
				.collect(Collectors.toSet()));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertTrue(artists.contains(EX.VanGogh));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testApplyToResultSingle() {
		TupleQueryResultConverter converter = forSingle();
		Set<IRI> artists = converter.applyToResult(res -> res.stream()
				.map(b -> QueryResultUtils.getIRI(b, "artist"))
				.collect(Collectors.toSet()));
		Assertions.assertEquals(1, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testApplyToResultNothing() {
		TupleQueryResultConverter converter = forNothing();
		Set<IRI> artists = converter.applyToResult(res -> res.stream()
				.map(b -> QueryResultUtils.getIRI(b, "artist"))
				.collect(Collectors.toSet()));
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToStreamMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		Set<BindingSet> bindingSets = converter.toStream().collect(Collectors.toSet());
		Assertions.assertEquals(2, bindingSets.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToStreamSingle() {
		TupleQueryResultConverter converter = forSingle();
		Set<BindingSet> bindingSets = converter.toStream().collect(Collectors.toSet());
		Assertions.assertEquals(1, bindingSets.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToStreamNothing() {
		TupleQueryResultConverter converter = forNothing();
		Set<BindingSet> bindingSets = converter.toStream().collect(Collectors.toSet());
		Assertions.assertTrue(bindingSets.isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void toStream1Multiple() {
		TupleQueryResultConverter converter = forMultiple();
		Set<IRI> artists = converter.toStream(bs -> QueryResultUtils.getIRI(bs, "artist")).collect(Collectors.toSet());
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertTrue(artists.contains(EX.VanGogh));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void toStream1Single() {
		TupleQueryResultConverter converter = forSingle();
		Set<IRI> artists = converter.toStream(bs -> QueryResultUtils.getIRI(bs, "artist")).collect(Collectors.toSet());
		Assertions.assertEquals(1, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void toStream1Nothing() {
		TupleQueryResultConverter converter = forNothing();
		Set<IRI> artists = converter.toStream(bs -> QueryResultUtils.getIRI(bs, "artist")).collect(Collectors.toSet());
		Assertions.assertTrue(artists.isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void toStream2Multiple() {
		TupleQueryResultConverter converter = forMultiple();
		Set<String> artists = converter
				.toStream(
						bs -> QueryResultUtils.getIRI(bs, "artist"),
						Object::toString)
				.collect(Collectors.toSet());
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso.toString()));
		Assertions.assertTrue(artists.contains(EX.VanGogh.toString()));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void toStream2Single() {
		TupleQueryResultConverter converter = forSingle();
		Set<String> artists = converter
				.toStream(
						bs -> QueryResultUtils.getIRI(bs, "artist"),
						Object::toString)
				.collect(Collectors.toSet());
		Assertions.assertEquals(1, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso.toString()));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void toStream2Nothing() {
		TupleQueryResultConverter converter = forNothing();
		Set<String> artists = converter
				.toStream(
						bs -> QueryResultUtils.getIRI(bs, "artist"),
						Object::toString)
				.collect(Collectors.toSet());
		Assertions.assertTrue(artists.isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybeOfWholeResultMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		Set<IRI> artists = converter.toSingletonMaybeOfWholeResult(tqr -> {
			if (!tqr.hasNext()) {
				return null;
			}
			return tqr.stream()
					.map(bs -> QueryResultUtils.getIRI(bs, "artist"))
					.collect(Collectors.toSet());
		});
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertTrue(artists.contains(EX.VanGogh));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybeOfWholeResultSingle() {
		TupleQueryResultConverter converter = forSingle();
		Set<IRI> artists = converter.toSingletonMaybeOfWholeResult(tqr -> {
			if (!tqr.hasNext()) {
				return null;
			}
			return tqr.stream()
					.map(bs -> QueryResultUtils.getIRI(bs, "artist"))
					.collect(Collectors.toSet());
		});
		Assertions.assertEquals(1, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybeOfWholeResultNothing() {
		TupleQueryResultConverter converter = forNothing();
		Set<IRI> artists = converter.toSingletonMaybeOfWholeResult(tqr -> {
			if (!tqr.hasNext()) {
				return null;
			}
			return tqr.stream()
					.map(bs -> QueryResultUtils.getIRI(bs, "artist"))
					.collect(Collectors.toSet());
		});
		Assertions.assertNull(artists);
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptionalOfWholeResultMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		Optional<Set<IRI>> artists = converter.toSingletonOptionalOfWholeResult(tqr -> {
			if (!tqr.hasNext()) {
				return null;
			}
			return tqr.stream()
					.map(bs -> QueryResultUtils.getIRI(bs, "artist"))
					.collect(Collectors.toSet());
		});
		Assertions.assertTrue(artists.isPresent());
		Assertions.assertEquals(2, artists.get().size());
		Assertions.assertTrue(artists.get().contains(EX.Picasso));
		Assertions.assertTrue(artists.get().contains(EX.VanGogh));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptionalOfWholeResultSingle() {
		TupleQueryResultConverter converter = forSingle();
		Optional<Set<IRI>> artists = converter.toSingletonOptionalOfWholeResult(tqr -> {
			if (!tqr.hasNext()) {
				return null;
			}
			return tqr.stream()
					.map(bs -> QueryResultUtils.getIRI(bs, "artist"))
					.collect(Collectors.toSet());
		});
		Assertions.assertEquals(1, artists.get().size());
		Assertions.assertTrue(artists.get().contains(EX.Picasso));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptionalOfWholeResultNothing() {
		TupleQueryResultConverter converter = forNothing();
		Optional<Set<IRI>> artists = converter.toSingletonOptionalOfWholeResult(tqr -> {
			if (!tqr.hasNext()) {
				return null;
			}
			return tqr.stream()
					.map(bs -> QueryResultUtils.getIRI(bs, "artist"))
					.collect(Collectors.toSet());
		});
		Assertions.assertTrue(artists.isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOfWholeResultMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		Set<IRI> artists = converter.toSingletonOfWholeResult(tqr -> {
			if (!tqr.hasNext()) {
				return null;
			}
			return tqr.stream()
					.map(bs -> QueryResultUtils.getIRI(bs, "artist"))
					.collect(Collectors.toSet());
		});
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertTrue(artists.contains(EX.VanGogh));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOfWholeResultSingle() {
		TupleQueryResultConverter converter = forSingle();
		Set<IRI> artists = converter.toSingletonOfWholeResult(tqr -> {
			if (!tqr.hasNext()) {
				return null;
			}
			return tqr.stream()
					.map(bs -> QueryResultUtils.getIRI(bs, "artist"))
					.collect(Collectors.toSet());
		});
		Assertions.assertEquals(1, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOfWholeResultNothing() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertThrows(IncorrectResultSetSizeException.class, () -> converter.toSingletonOfWholeResult(tqr -> {
			if (!tqr.hasNext()) {
				return null;
			}
			return tqr.stream()
					.map(bs -> QueryResultUtils.getIRI(bs, "artist"))
					.collect(Collectors.toSet());
		})
		);
	}

	@Test
	public void testToSingletonMaybeMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertThrows(
				IncorrectResultSetSizeException.class,
				() -> converter.toSingletonMaybe(bs -> QueryResultUtils.getIRI(bs, "artist")));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybeMultipleToNull() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertNull(converter.toSingletonMaybe(bs -> null));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybeSingle() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertEquals(EX.Picasso, converter.toSingletonMaybe(bs -> QueryResultUtils.getIRI(bs, "artist")));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybeSingleToNull() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertNull(converter.toSingletonMaybe(bs -> null));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybeNothing() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertNull(converter.toSingletonMaybe(bs -> QueryResultUtils.getIRI(bs, "artist")));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybeNothingToNull() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertNull(converter.toSingletonMaybe(bs -> null));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptionalMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertThrows(
				IncorrectResultSetSizeException.class,
				() -> converter.toSingletonOptional(bs -> QueryResultUtils.getIRI(bs, "artist")));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptionalMultipleToNull() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertTrue(converter.toSingletonOptional(bs -> null).isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptionalSingle() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertEquals(EX.Picasso,
				converter.toSingletonOptional(bs -> QueryResultUtils.getIRI(bs, "artist")).get());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptionalSingleToNull() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertTrue(converter.toSingletonOptional(bs -> null).isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptionalNothing() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertTrue(converter.toSingletonOptional(
				bs -> QueryResultUtils.getIRI(bs, "artist"))
				.isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptionalNothingToNull() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertTrue(converter.toSingletonOptional(bs -> null).isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertThrows(
				IncorrectResultSetSizeException.class,
				() -> converter.toSingleton(bs -> QueryResultUtils.getIRI(bs, "artist")));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMultipleToNull() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertThrows(IncorrectResultSetSizeException.class,
				() -> converter.toSingleton(bs -> null));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonSingle() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertEquals(EX.Picasso, converter.toSingleton(bs -> QueryResultUtils.getIRI(bs, "artist")));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonSingleToNull() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertThrows(
				IncorrectResultSetSizeException.class,
				() -> converter.toSingleton(bs -> null));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonNothing() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertThrows(IncorrectResultSetSizeException.class,
				() -> converter.toSingleton(bs -> QueryResultUtils.getIRI(bs, "artist")));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonNothingToNull() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertThrows(IncorrectResultSetSizeException.class,
				() -> converter.toSingleton(bs -> null));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybe2Multiple() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertThrows(
				IncorrectResultSetSizeException.class,
				() -> converter.toSingletonMaybe(
						bs -> QueryResultUtils.getIRI(bs, "artist"),
						IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybe2MultipleToNull() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertNull(converter.toSingletonMaybe(bs -> null, IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybe2Single() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertEquals(EX.Picasso.toString(), converter.toSingletonMaybe(
				bs -> QueryResultUtils.getIRI(bs, "artist"),
				IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybe2SingleToNull() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertNull(converter.toSingletonMaybe(bs -> null, IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybe2Nothing() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertNull(converter.toSingletonMaybe(
				bs -> QueryResultUtils.getIRI(bs, "artist"), IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonMaybe2NothingToNull() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertNull(converter.toSingletonMaybe(bs -> null, IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptional2Multiple() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertThrows(
				IncorrectResultSetSizeException.class,
				() -> converter.toSingletonOptional(
						bs -> QueryResultUtils.getIRI(bs, "artist"),
						IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptional2MultipleToNull() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertTrue(converter.toSingletonOptional(bs -> null, IRI::toString).isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptional2Single() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertEquals(EX.Picasso.toString(), converter.toSingletonOptional(
				bs -> QueryResultUtils.getIRI(bs, "artist"),
				IRI::toString).get());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptional2SingleToNull() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertTrue(converter.toSingletonOptional(bs -> null,
				IRI::toString).isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptional2Nothing() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertTrue(converter.toSingletonOptional(
				bs -> QueryResultUtils.getIRI(bs, "artist"),
				IRI::toString)
				.isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingletonOptional2NothingToNull() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertTrue(converter.toSingletonOptional(bs -> null,
				IRI::toString).isEmpty());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingleton2Multiple() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertThrows(
				IncorrectResultSetSizeException.class,
				() -> converter.toSingleton(
						bs -> QueryResultUtils.getIRI(bs, "artist"),
						IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingleton2MultipleToNull() {
		TupleQueryResultConverter converter = forMultiple();
		Assertions.assertThrows(IncorrectResultSetSizeException.class,
				() -> converter.toSingleton(
						bs -> null,
						IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingleton2Single() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertEquals(EX.Picasso.toString(), converter.toSingleton(
				bs -> QueryResultUtils.getIRI(bs, "artist"),
				IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingleton2SingleToNull() {
		TupleQueryResultConverter converter = forSingle();
		Assertions.assertThrows(
				IncorrectResultSetSizeException.class,
				() -> converter.toSingleton(bs -> null, IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingleton2Nothing() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertThrows(IncorrectResultSetSizeException.class,
				() -> converter.toSingleton(
						bs -> QueryResultUtils.getIRI(bs, "artist"),
						IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSingleton2NothingToNull() {
		TupleQueryResultConverter converter = forNothing();
		Assertions.assertThrows(IncorrectResultSetSizeException.class,
				() -> converter.toSingleton(bs -> null, IRI::toString));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToListMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		List<IRI> artists = converter.toList(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.containsAll(Set.of(EX.Picasso, EX.VanGogh)));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToListMultipleToNull() {
		TupleQueryResultConverter converter = forMultiple();
		List<IRI> artists = converter.toList(bs -> null);
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToListSingle() {
		TupleQueryResultConverter converter = forSingle();
		List<IRI> artists = converter.toList(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(1, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToListNothing() {
		TupleQueryResultConverter converter = forNothing();
		List<IRI> artists = converter.toList(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToList2Multiple() {
		TupleQueryResultConverter converter = forMultiple();
		List<String> artists = converter.toList(bs -> QueryResultUtils.getIRI(bs, "artist"), IRI::toString);
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.containsAll(Set.of(EX.Picasso.toString(), EX.VanGogh.toString())));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToList2MultipleToNull() {
		TupleQueryResultConverter converter = forMultiple();
		List<String> artists = converter.toList(bs -> null, IRI::toString);
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToList2MultiplePostprocessToNull() {
		TupleQueryResultConverter converter = forMultiple();
		List<String> artists = converter.toList(bs -> QueryResultUtils.getIRI(bs, "artist"), x -> null);
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToList2Single() {
		TupleQueryResultConverter converter = forSingle();
		List<String> artists = converter.toList(bs -> QueryResultUtils.getIRI(bs, "artist"), IRI::toString);
		Assertions.assertEquals(1, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso.toString()));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToList2Nothing() {
		TupleQueryResultConverter converter = forNothing();
		List<String> artists = converter.toList(bs -> QueryResultUtils.getIRI(bs, "artist"), IRI::toString);
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSetMultiple() {
		TupleQueryResultConverter converter = forMultiple();
		Set<IRI> artists = converter.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.containsAll(Set.of(EX.Picasso, EX.VanGogh)));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSetMultipleToNull() {
		TupleQueryResultConverter converter = forMultiple();
		Set<IRI> artists = converter.toSet(bs -> null);
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSetSingle() {
		TupleQueryResultConverter converter = forSingle();
		Set<IRI> artists = converter.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(1, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSetNothing() {
		TupleQueryResultConverter converter = forNothing();
		Set<IRI> artists = converter.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSet2Multiple() {
		TupleQueryResultConverter converter = forMultiple();
		Set<String> artists = converter.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"), IRI::toString);
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.containsAll(Set.of(EX.Picasso.toString(), EX.VanGogh.toString())));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSet2MultipleToNull() {
		TupleQueryResultConverter converter = forMultiple();
		Set<String> artists = converter.toSet(bs -> null, IRI::toString);
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSet2MultiplePostprocessToNull() {
		TupleQueryResultConverter converter = forMultiple();
		Set<String> artists = converter.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"), x -> null);
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSet2Single() {
		TupleQueryResultConverter converter = forSingle();
		Set<String> artists = converter.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"), IRI::toString);
		Assertions.assertEquals(1, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso.toString()));
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToSet2Nothing() {
		TupleQueryResultConverter converter = forNothing();
		Set<String> artists = converter.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"), IRI::toString);
		Assertions.assertEquals(0, artists.size());
		Assertions.assertThrows(NullPointerException.class, converter::toStream);
	}

	@Test
	public void testToMapOfSet() {
		Map<IRI, Set<IRI>> paintings = rdf4JTemplate.tupleQueryFromResource(
				getClass(),
				"classpath:sparql/get-paintings-of-artist2.rq")
				.evaluateAndConvert()
				.toMapOfSet(bs -> QueryResultUtils.getIRI(bs, "artist"),
						bs -> QueryResultUtils.getIRI(bs, "painting"));
		Assertions.assertEquals(2, paintings.keySet().size());
		Assertions.assertNotNull(paintings.get(EX.Picasso));
		Assertions.assertNotNull(paintings.get(EX.VanGogh));
		Assertions.assertEquals(1, paintings.get(EX.Picasso).size());
		Assertions.assertEquals(3, paintings.get(EX.VanGogh).size());
	}

	/**
	 * TODO
	 *
	 *
	 * @Test public void testToMap() {
	 *
	 *       }
	 *
	 *       public <K, V> Map<K, List<V>> toMapOfList( Function<BindingSet, K> keyMapper, Function<BindingSet, V>
	 *       valueMapper) { return resultConverter.toMapOfList(keyMapper, valueMapper); }
	 *
	 *       public <T, K, V> Map<K, V> toMap(BindingSetMapper<T> mapper, Function<T, K> keyMapper, Function<T, V>
	 *       valueMapper) { return resultConverter.toMap(mapper, keyMapper, valueMapper); }
	 *
	 *       public <K, V> Map<K, V> toMap( Function<BindingSet, Map.Entry<K, V>> entryMapper) { return
	 *       resultConverter.toMap(entryMapper); }
	 *
	 *       public <T, K, V> Map<K, Set<V>> toMapOfSet( BindingSetMapper<T> mapper, Function<T, K> keyMapper,
	 *       Function<T, V> valueMapper) { return resultConverter.toMapOfSet(mapper, keyMapper, valueMapper); }
	 *
	 *       public <T, K, V> Map<K, List<V>> toMapOfList( BindingSetMapper<T> mapper, Function<T, K> keyMapper,
	 *       Function<T, V> valueMapper) { return resultConverter.toMapOfList(mapper, keyMapper, valueMapper); }
	 *
	 *       public Stream<BindingSet> getBindingStream( TupleQueryResult result) { return
	 *       resultConverter.getBindingStream(result); }
	 *
	 */

	private TupleQueryResultConverter forMultiple() {
		return rdf4JTemplate.tupleQueryFromResource(
				getClass(),
				"classpath:sparql/get-artists.rq").evaluateAndConvert();

	}

	private TupleQueryResultConverter forSingle() {
		return rdf4JTemplate.tupleQueryFromResource(
				getClass(),
				"classpath:sparql/get-artists.rq")
				.withBinding("artist", EX.Picasso)
				.evaluateAndConvert();

	}

	private TupleQueryResultConverter forNothing() {
		return rdf4JTemplate.tupleQueryFromResource(
				getClass(),
				"classpath:sparql/get-artists.rq")
				.withBinding("artist", EX.of("Vermeer"))
				.evaluateAndConvert();

	}

}
