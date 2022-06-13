package org.eclipse.rdf4j.collection.factory.mapdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.api.ValuePair;
import org.eclipse.rdf4j.collection.factory.impl.DefaultValuePair;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.junit.Test;

public class MapDBCollectionFactory {

	private static final int NUMBER_TO_TEST = 32;
	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testValueSet() {
		try (CollectionFactory collectionFactory = new MapDbCollectionFactory(1)) {
			Set<Value> vs = collectionFactory.createValueSet();
			int expectedSize = NUMBER_TO_TEST;
			for (int i = 0; i < expectedSize; i++) {
				assertTrue(vs.add(vf.createLiteral(i)));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {
				assertTrue(vs.contains(vf.createLiteral(i)));
			}
		}
	}

	@Test
	public void testSetOfBindingSet() {
		try (CollectionFactory collectionFactory = new MapDbCollectionFactory(1)) {
			Set<BindingSet> vs = collectionFactory.createSetOfBindingSets(MapBindingSet::new,
					(s) -> ((v, bs) -> bs.setBinding(s, v)));
			Set<BindingSet> ebs = new HashSet<>();
			int expectedSize = NUMBER_TO_TEST;
			for (int i = 0; i < expectedSize; i++) {
				MapBindingSet qbs = new MapBindingSet();
				qbs.addBinding("a", vf.createLiteral(i));
				qbs.addBinding("b", vf.createLiteral(i * 2));
				assertTrue(vs.add(qbs));
				assertTrue(ebs.add(qbs));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {
				MapBindingSet qbs = new MapBindingSet();
				qbs.addBinding("a", vf.createLiteral(i));
				assertFalse(vs.contains(qbs));
				qbs.addBinding("b", vf.createLiteral(i * 2));
				assertTrue(vs.contains(qbs));
			}
			for (BindingSet bs : vs) {
				assertTrue(ebs.contains(bs));
			}
		}
	}

	@Test
	public void testSetOfBindingSetMix() {
		try (CollectionFactory collectionFactory = new MapDbCollectionFactory(1)) {
			Set<BindingSet> vs = collectionFactory.createSetOfBindingSets(MapBindingSet::new,
					(s) -> ((v, bs) -> bs.setBinding(s, v)));
			Set<BindingSet> ebs = new HashSet<>();
			int expectedSize = NUMBER_TO_TEST;
			for (int i = 0; i < expectedSize; i++) {
				MapBindingSet qbs = new MapBindingSet();
				if (i % 2 == 0) {
					qbs.addBinding("a", vf.createLiteral(i));
					qbs.addBinding("b", vf.createLiteral(i * 2));
				} else {
					qbs.addBinding("a", SimpleValueFactory.getInstance().createLiteral(i));
					qbs.addBinding("b", SimpleValueFactory.getInstance().createLiteral(i * 2));
				}
				assertTrue(vs.add(qbs));
				assertTrue(ebs.add(qbs));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {
				MapBindingSet qbs = new MapBindingSet();
				qbs.addBinding("a", vf.createLiteral(i));
				assertFalse(vs.contains(qbs));
				qbs.addBinding("b", vf.createLiteral(i * 2));
				assertTrue(vs.contains(qbs));
			}
			for (BindingSet bs : vs) {
				assertTrue(ebs.contains(bs));
			}
		}
	}

	@Test
	public void testSetOfBindingSetNot() throws IOException {
		try (CollectionFactory collectionFactory = new MapDbCollectionFactory(1)) {
			Set<BindingSet> vs = collectionFactory.createSetOfBindingSets(MapBindingSet::new,
					(s) -> ((v, bs) -> bs.setBinding(s, v)));
			Set<BindingSet> ebs = new HashSet<>();
			int expectedSize = NUMBER_TO_TEST;
			for (int i = 0; i < expectedSize; i++) {
				MapBindingSet qbs = new MapBindingSet();
				qbs.addBinding("a", vf.createLiteral(i));
				qbs.addBinding("b", vf.createLiteral(String.valueOf(i * 2)));
				assertTrue(vs.add(qbs));
				assertTrue(ebs.add(qbs));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {
				MapBindingSet qbs = new MapBindingSet();
				qbs.addBinding("a", vf.createLiteral(i));
				assertFalse(vs.contains(qbs));
				qbs.addBinding("b", SimpleValueFactory.getInstance().createLiteral(String.valueOf(i * 2)));
				assertTrue(vs.contains(qbs));
			}
			for (BindingSet bs : vs) {
				assertTrue(ebs.contains(bs));
			}
		}
	}

	@Test
	public void testSetOfValuePair() throws IOException {
		try (CollectionFactory collectionFactory = new MapDbCollectionFactory(1)) {
			Set<ValuePair> vs = collectionFactory.createValuePairSet();
			Set<ValuePair> ebs = new HashSet<>();
			int expectedSize = NUMBER_TO_TEST;
			for (int i = 0; i < expectedSize; i++) {
				Literal start = vf.createLiteral(i);
				Literal end = vf.createLiteral(i * 2);
				ValuePair vp = new DefaultValuePair(start, end);
				assertTrue(vs.add(vp));
				assertTrue(ebs.add(vp));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {

				Literal start = vf.createLiteral(i);
				Literal end = vf.createLiteral(i * 2);
				ValuePair vp = new DefaultValuePair(start, end);
				assertTrue(vs.contains(vp));
			}
			for (ValuePair bs : vs) {
				assertTrue(ebs.contains(bs));
			}
		}
	}

	@Test
	public void testSetOfValuePairNot() {
		try (CollectionFactory collectionFactory = new MapDbCollectionFactory(1)) {
			Set<ValuePair> vs = collectionFactory.createValuePairSet();
			Set<ValuePair> ebs = new HashSet<>();
			int expectedSize = NUMBER_TO_TEST;
			for (int i = 0; i < expectedSize; i++) {

				ValuePair vp = new DefaultValuePair(SimpleValueFactory.getInstance().createLiteral(i),
						SimpleValueFactory.getInstance().createLiteral(String.valueOf(i * 2)));
				assertTrue(vs.add(vp));
				assertTrue(ebs.add(vp));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {
				Literal start = vf.createLiteral(i);
				Literal end = SimpleValueFactory.getInstance().createLiteral(String.valueOf(i * 2));
				ValuePair vp = new DefaultValuePair(start, end);
				assertTrue(vs.contains(vp));
			}
			for (ValuePair vp : vs) {
				assertTrue(ebs.contains(vp));
			}
		}
	}
}
