package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.api.ValuePair;
import org.eclipse.rdf4j.collection.factory.impl.DefaultValuePair;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LmdbCollectionFactoryTest {
	private static final int NUMBER_TO_TEST = 32;
	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();
	private File dataDir;
	private LmdbStore sail;

	/*---------*
	 * Methods *
	 *---------*/

	@Before
	public void setup() throws SailException {
		try {
			dataDir = tempDir.newFolder();
			sail = new LmdbStore(dataDir, new LmdbStoreConfig("spoc,posc"));
			sail.setIterationCacheSyncThreshold(NUMBER_TO_TEST / 4);
			sail.init();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	@After
	public void shutDown() throws SailException {
		sail.shutDown();
	}

	@Test
	public void testValueSet() {
		try (CollectionFactory collectionFactory = sail.getCollectionFactory()) {
			Set<Value> vs = collectionFactory.createValueSet();
			int expectedSize = NUMBER_TO_TEST;
			for (int i = 0; i < expectedSize; i++) {
				assertTrue(vs.add(sail.getValueFactory().createLiteral(i)));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {
				assertTrue(vs.contains(sail.getValueFactory().createLiteral(i)));
			}
		}
	}

	@Test
	public void testSetOfBindingSet() {
		try (CollectionFactory collectionFactory = sail.getCollectionFactory()) {
			Set<BindingSet> vs = collectionFactory.createSetOfBindingSets(QueryBindingSet::new);
			Set<BindingSet> ebs = new HashSet<>();
			int expectedSize = NUMBER_TO_TEST;
			for (int i = 0; i < expectedSize; i++) {
				QueryBindingSet qbs = new QueryBindingSet();
				qbs.addBinding("a", sail.getValueFactory().createLiteral(i));
				qbs.addBinding("b", sail.getValueFactory().createLiteral(i * 2));
				assertTrue(vs.add(qbs));
				assertTrue(ebs.add(qbs));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {
				QueryBindingSet qbs = new QueryBindingSet();
				qbs.addBinding("a", sail.getValueFactory().createLiteral(i));
				assertFalse(vs.contains(qbs));
				qbs.addBinding("b", sail.getValueFactory().createLiteral(i * 2));
				assertTrue(vs.contains(qbs));
			}
			for (BindingSet bs : vs) {
				assertTrue(ebs.contains(bs));
			}
		}
	}

	@Test
	public void testSetOfBindingSetMix() {
		try (CollectionFactory collectionFactory = sail.getCollectionFactory()) {
			Set<BindingSet> vs = collectionFactory.createSetOfBindingSets(QueryBindingSet::new);
			Set<BindingSet> ebs = new HashSet<>();
			int expectedSize = NUMBER_TO_TEST;
			for (int i = 0; i < expectedSize; i++) {
				QueryBindingSet qbs = new QueryBindingSet();
				if (i % 2 == 0) {
					qbs.addBinding("a", sail.getValueFactory().createLiteral(i));
					qbs.addBinding("b", sail.getValueFactory().createLiteral(i * 2));
				} else {
					qbs.addBinding("a", SimpleValueFactory.getInstance().createLiteral(i));
					qbs.addBinding("b", SimpleValueFactory.getInstance().createLiteral(i * 2));
				}
				assertTrue(vs.add(qbs));
				assertTrue(ebs.add(qbs));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {
				QueryBindingSet qbs = new QueryBindingSet();
				qbs.addBinding("a", sail.getValueFactory().createLiteral(i));
				assertFalse(vs.contains(qbs));
				qbs.addBinding("b", sail.getValueFactory().createLiteral(i * 2));
				assertTrue(vs.contains(qbs));
			}
			for (BindingSet bs : vs) {
				assertTrue(ebs.contains(bs));
			}
		}
	}

	@Test
	public void testSetOfBindingSetNot() throws IOException {
		try (CollectionFactory collectionFactory = sail.getCollectionFactory()) {
			Set<BindingSet> vs = collectionFactory.createSetOfBindingSets(QueryBindingSet::new);
			Set<BindingSet> ebs = new HashSet<>();
			int expectedSize = NUMBER_TO_TEST;
			ValueStore valueFactory = (ValueStore) sail.getValueFactory();
			for (int i = 0; i < expectedSize; i++) {
				QueryBindingSet qbs = new QueryBindingSet();
				qbs.addBinding("a", SimpleValueFactory.getInstance().createLiteral(i));
				qbs.addBinding("b", SimpleValueFactory.getInstance().createLiteral(String.valueOf(i * 2)));
				assertTrue(vs.add(qbs));
				assertTrue(ebs.add(qbs));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {
				QueryBindingSet qbs = new QueryBindingSet();
				valueFactory.startTransaction();
				Literal createLiteral = valueFactory.createLiteral(i);
				long id = valueFactory.storeValue(createLiteral);
				valueFactory.commit();
				qbs.addBinding("a", valueFactory.getLazyValue(id));
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
		try (CollectionFactory collectionFactory = sail.getCollectionFactory()) {
			Set<ValuePair> vs = collectionFactory.createValuePairSet();
			Set<ValuePair> ebs = new HashSet<>();
			int expectedSize = NUMBER_TO_TEST;
			ValueStore valueFactory = (ValueStore) sail.getValueFactory();
			for (int i = 0; i < expectedSize; i++) {
				Literal start = valueFactory.createLiteral(i);
				Literal end = valueFactory.createLiteral(i * 2);
				valueFactory.startTransaction();
				long startId = valueFactory.storeValue(start);
				long endId = valueFactory.storeValue(end);
				valueFactory.commit();
				ValuePair vp = new DefaultValuePair(valueFactory.getLazyValue(startId),
						valueFactory.getLazyValue(endId));
				assertTrue(vs.add(vp));
				assertTrue(ebs.add(vp));
			}
			assertEquals(expectedSize, vs.size());
			for (int i = 0; i < expectedSize; i++) {

				Literal start = valueFactory.createLiteral(i);
				Literal end = valueFactory.createLiteral(i * 2);
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
		try (CollectionFactory collectionFactory = sail.getCollectionFactory()) {
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
				Literal start = sail.getValueFactory().createLiteral(i);
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
