/* SPDX-License-Identifier: BSD-3-Clause */
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.valueoverlay.CompressedValueOverlay;
import org.eclipse.rdf4j.sail.lmdb.valueoverlay.OverlayCapacityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real native/RDF4J integration acceptance; requires the full module dependencies and native library. */
public class ValueStoreCompressedOverlayTest {
 @TempDir File directory;
 private CompressedValueOverlay.Options options() {
  return new CompressedValueOverlay.Options(32L << 20, 8192, 16, 64 << 10, 1 << 20, true, true);
 }
 @Test void exactPersistentBytesAndLexicalFormsSurviveWarmAndEviction() throws Exception {
  var factory=SimpleValueFactory.getInstance();
  var store=new ValueStore(new File(directory,"values"),new LmdbStoreConfig().setInlineLiterals(false));
  try {
   List<Value> values=new ArrayList<>();
   for(int i=0;i<256;i++) {
    values.add(factory.createIRI("https://example.org/items/"+i));
    values.add(factory.createLiteral("+000000"+i, XSD.INTEGER));
    values.add(factory.createLiteral("-00001234567890."+i+"000", XSD.DECIMAL));
    values.add(factory.createLiteral("2026-09-09T12:00:00."+i+"000+02:00",XSD.DATETIME));
    values.add(factory.createLiteral("e\u0301 and é; 漢字; 😀 "+i,"nb"));
    values.add(factory.createBNode("blank-"+i));
   }
   long[] ids=new long[values.size()];store.startTransaction(true);
   for(int i=0;i<ids.length;i++)ids[i]=store.storeValue(values.get(i));
   store.commit();byte[][] records=new byte[ids.length][];
   for(int i=0;i<ids.length;i++)records[i]=store.getData(ids[i]);
   var stats=store.warmCompressedValueOverlay(options());assertTrue(stats.records()>=ids.length);
   store.clearCaches();
   for(int i=0;i<ids.length;i++) {
    assertArrayEquals(records[i],store.getData(ids[i]));
    Value actual=store.getValue(ids[i]);assertEquals(values.get(i),actual);
    if(actual instanceof Literal literal)assertEquals(((Literal)values.get(i)).getLabel(),literal.getLabel());
    assertEquals(ids[i],store.getId(values.get(i)));
   }
  } finally {store.close();}
 }
 @Test void writesRollbackAndClearNeverLeaveAnOldPublicationUsable() throws Exception {
  var factory=SimpleValueFactory.getInstance();
  var store=new ValueStore(new File(directory,"mutable"),new LmdbStoreConfig());
  try {
   store.startTransaction(true);long id=store.storeValue(factory.createBNode("kept"));store.commit();
   store.warmCompressedValueOverlay(options());assertNotNull(store.compressedValueOverlayStats());
   store.startTransaction(true);assertNull(store.compressedValueOverlayStats());
   store.storeValue(factory.createBNode("rollback"));store.rollback();
   assertEquals("kept",store.getValue(id).stringValue());store.warmCompressedValueOverlay(options());
   store.clear();assertNull(store.compressedValueOverlayStats());
   store.warmCompressedValueOverlay(options());assertNotNull(store.compressedValueOverlayStats());
  } finally {store.close();}
 }
 @Test void refusalDoesNotReplaceExistingPublishedOverlay() throws Exception {
  var factory=SimpleValueFactory.getInstance();
  var store=new ValueStore(new File(directory,"capacity"),new LmdbStoreConfig());
  try {
   store.startTransaction(true);long id=store.storeValue(factory.createBNode("stable"));store.commit();
   var installed=store.warmCompressedValueOverlay(options());
   assertThrows(OverlayCapacityException.class,()->store.warmCompressedValueOverlay(
     new CompressedValueOverlay.Options(100,0,2,1024,1024,true,true)));
   assertSame(installed,store.compressedValueOverlayStats());assertEquals("stable",store.getValue(id).stringValue());
  } finally {store.close();}
 }
}
