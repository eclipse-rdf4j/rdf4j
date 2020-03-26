/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

/**
 * HDT Triples section.
 * 
 * This part contains two levels of bitmaps and arrays.
 * 
 * Typically (in SPO order), the Y-level corresponds to the predicates and the Z-level to the objects.
 * 
 * The X (subjects) is implicit since triples are already ordered by X, and bitmap Y is used to notify when there is a
 * new X: X will be used until a 1 is found in bitmap Y, which indicates that the next triple uses X+1.
 * 
 * The array contains numeric references to the predicates and objects in the HDT dictionaries, starting with the
 * entries in the "shared" {@link org.eclipse.rdf4j.rio.hdt.HDTDictionary Dictionary} These positions are counted
 * starting with 1, not 0.
 * 
 * E.g. Y-bitmap <code>0 1 1</code> and Y-array <code>1 2 3</code> results in <code>S1-P1 S1-P2 S2-P3</code>
 * 
 * 
 * Structure:
 * 
 * <pre>
 * +----------+----------+---------+---------+
 * | Bitmap Y | Bitmap Z | Array Y | Array Z |
 * +----------+----------+---------+---------+
 * </pre>
 * 
 * @see http://www.rdfhdt.org/hdt-internals/#triples
 * 
 * @author Bart Hanssens
 */
class HDTTriplesSectionBitmap extends HDTTriplesSection {
	private HDTBitmap bitmapY;
	private HDTBitmap bitmapZ;
	private HDTArray arrY;
	private HDTArray arrZ;

	private int sizeY = 0;
	private int sizeZ = 0;

	private Iterator<int[]> iter;

	@Override
	protected Iterator<int[]> getIterator() {
		return new Iterator() {
			private int posX = 1;
			private int posY = 0;
			private int posZ = 0;

			@Override
			public boolean hasNext() {
				// we only need to check if we've reach the end of the "lowest" level
				return posZ < sizeZ;
			}

			@Override
			public int[] next() {
				int z = arrZ.get(posZ);
				int y = arrY.get(posY);
				int x = posX;

				if (bitmapZ.get(posZ) == 1 && posZ < sizeZ) {
					// move to next Y position (predicate) when there is no Z (predicate) left
					if (bitmapY.get(posY) == 1 && posY < sizeY) {
						// move to next X position (subject) when there is no Y (predicate) left
						posX++;
					}
					posY++;
				}
				posZ++;
				return new int[] { x, y, z };
			}
		};
	}

	@Override
	protected void setIterator(Iterator<int[]> iter) {
		this.iter = iter;
	}

	@Override
	protected void parse(InputStream is) throws IOException {
		parse(is, HDTTriples.Order.SPO);
	}

	@Override
	protected void write(OutputStream os) throws IOException {
		write(os, HDTTriples.Order.SPO);
	}

	@Override
	protected void parse(InputStream is, HDTTriples.Order order) throws IOException {
		bitmapY = new HDTBitmap();
		bitmapY.parse(is);
		sizeY = bitmapY.size();

		bitmapZ = new HDTBitmap();
		bitmapZ.parse(is);
		sizeZ = bitmapZ.size();

		arrY = HDTArrayFactory.parse(is);
		arrY.parse(is);

		arrZ = HDTArrayFactory.parse(is);
		arrZ.parse(is);
	}

	@Override
	protected void write(OutputStream os, HDTTriples.Order order) throws IOException {
		// Z will simply be the number of triples, 
		// also use Z as upper value for Y and resize later
		sizeY = size();
		sizeZ = size();

		bitmapY = new HDTBitmap();
		bitmapY.size(sizeY);

		bitmapZ = new HDTBitmap();
		bitmapZ.size(sizeZ);

		// Log64 cannot be resized easily, so create a temp array
		arrY = HDTArrayFactory.create(HDTArray.Type.LOG64);
		int[] arrYtmp = new int[sizeY];
		
		arrZ = HDTArrayFactory.create(HDTArray.Type.LOG64);
		arrZ.size(size());

		int[] triple = iter.next();
		arrYtmp[0] = triple[1]; // predicate
		arrZ.set(0, triple[2]); // object
	
		// iterate over triple references to calculate the size of Y (predicate bitmap / array)
		for(int i = 1; i < sizeZ; i++) {
			triple = iter.next();
			arrZ.set(i, triple[2]);
		}

		bitmapY.write(os);
		bitmapZ.write(os);

		arrY.write(os);
		arrZ.write(os);
	}
}
