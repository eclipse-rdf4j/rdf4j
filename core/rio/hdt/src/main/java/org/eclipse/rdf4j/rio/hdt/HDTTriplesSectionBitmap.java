/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.IOException;
import java.io.InputStream;

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
 * @author Bart Hanssens
 */
class HDTTriplesSectionBitmap extends HDTTriplesSection {
	private HDTBitmap bitmapY;
	private HDTBitmap bitmapZ;
	private HDTArray arrY;
	private HDTArray arrZ;

	private int sizeY = 0;
	private int sizeZ = 0;

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

	@Override
	protected void parse(InputStream is) throws IOException {
		parse(is, HDTTriples.Order.SPO);
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
}
