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
		bitmapY.setSize(sizeY);

		bitmapZ = new HDTBitmap();
		bitmapZ.setSize(sizeZ);

		arrY = HDTArrayFactory.create(HDTArray.Type.LOG64);
		arrZ = HDTArrayFactory.create(HDTArray.Type.LOG64);

		fillYZ();

		bitmapY.write(os);
		bitmapZ.write(os);

		arrY.write(os);
		arrZ.write(os);
	}

	/**
	 * Fill Y and Z bitmaps and arrays
	 */
	private void fillYZ() {
		// Log64 cannot be resized easily, and require max value to be known, so create a temp array
		int[] arrYtmp = new int[sizeY];
		int[] arrZtmp = new int[sizeZ];

		int posY = 1;
		int posZ = 1;

		int[] triple = iter.next();
		int prevX = triple[0]; // subject
		int prevY = triple[1]; // predicate
		int prevZ = triple[2]; // object

		arrYtmp[0] = triple[1];
		arrZtmp[0] = triple[2];

		int maxY = triple[1];
		int maxZ = triple[2];

		// iterate over triple references
		for (posZ = 1; posZ < sizeZ; posZ++) {
			triple = iter.next();

			if (triple[1] > maxY) {
				maxY = triple[1];
			}
			if (triple[2] > maxZ) {
				maxZ = triple[2];
			}

			if (triple[0] != prevX) {
				// previous P/O were the last P/O for that subject
				bitmapY.set(posY - 1, 1);
				bitmapZ.set(posZ - 1, 1);
				prevX = triple[0];
				arrYtmp[posY++] = triple[1];
			} else {
				bitmapY.set(posY - 1, 0);
				if (triple[1] != prevY) {
					bitmapZ.set(posZ - 1, 1);
					arrYtmp[posY++] = triple[1];
					prevY = triple[1];
				} else {
					bitmapZ.set(posZ - 1, 0);
				}
			}
			arrZtmp[posZ] = triple[2];
		}

		// arrYtmp[posY] = triple[1];
		// last bit is always 1 (= last predicate/object)
		bitmapY.set(posY - 1, 1);
		bitmapZ.set(posZ - 1, 1);

		// now resize and fill the Y array
		arrY.setMaxValue(maxY);
		arrY.setSize(posY);
		bitmapY.setSize(posY);
		for (int i = 0; i < posY; i++) {
			arrY.set(i, arrYtmp[i]);
		}

		// now resize and fill the Z array
		arrZ.setMaxValue(maxZ);
		arrZ.setSize(posZ);
		bitmapZ.setSize(posZ);
		for (int i = 0; i < posZ; i++) {
			arrZ.set(i, arrZtmp[i]);
		}
	}
}
