/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.benchmark.rio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.rdf4j.common.io.IOUtil;

/**
 * This enum holds locations for RDF files from the web. It allows downloading these into a a temporary location in
 * {@code {java.io.tmpdir}/rdf4j-benchmark-datasets/{filename} }. Please note
 *
 * @author Frens Jan Rumph
 */
public enum RDFTestDataset {

	/**
	 * <p>
	 * A dump of the <a href="http://wifo5-03.informatik.uni-mannheim.de/bizer/berlinsparqlbenchmark/">Berlin SPARQL
	 * Benchmark</a> made available by the <a href="https://project-hobbit.eu">HOBBIT project</a>.
	 * </p>
	 * <p>
	 * 25 GB extracted.
	 * </p>
	 */
	BSBM("bsbm.nt", "dataset.nt",
			"https://hobbitdata.informatik.uni-leipzig.de/benchmarks-data/datasets-dumps/bsbm-dump.zip"),
	/**
	 * <p>
	 * A dump of the <a href="http://owl.cs.manchester.ac.uk/publications/supporting-material/fishmark/">FishMark</a>
	 * benchmark made available by the <a href="https://project-hobbit.eu">HOBBIT project</a>.
	 * </p>
	 * <p>
	 * 1.8 GB extracted
	 * </p>
	 */
	FISHMARK("fishmark.nt", "fishmark-size1.nt",
			"https://hobbitdata.informatik.uni-leipzig.de/benchmarks-data/datasets-dumps/fishmark-dump.zip"),

	/**
	 * <p>
	 * A dump of the
	 * <a href="http://borneo.informatik.uni-freiburg.de/content/publikationen/papers/sp2b.pdf">SP2Bench</a> benchmark
	 * made available by the <a href="https://project-hobbit.eu">HOBBIT project</a>.
	 * </p>
	 * <p>
	 * 2.7 GB extracted
	 * </p>
	 */
	SP2BENCH("sp2b.n3", "sp2b.n3",
			"https://hobbitdata.informatik.uni-leipzig.de/benchmarks-data/datasets-dumps/sp2bench-dump.zip"),

	/**
	 * <p>
	 * A dump of the Semantic Web Dog Food dataset made available by the <a href="https://project-hobbit.eu">HOBBIT
	 * project</a>.
	 * </p>
	 * <p>
	 * 49 MB extracted
	 * </p>
	 */
	SWDF("swdf.nt", "SWDF.nt",
			"https://hobbitdata.informatik.uni-leipzig.de/benchmarks-data/datasets-dumps/swdf-dump.zip"),

	/**
	 * <p>
	 * A dump of the gene database of National Center for Biotechnology Information made available by the
	 * <a href="https://bio2rdf.org">Bio2RDF project</a>.
	 * </p>
	 * <p>
	 * 5.6 GB extracted
	 * </p>
	 */
	GENE2GO("gene2go.nq",
			"https://download.bio2rdf.org/files/release/3/ncbigene/gene2go.nq.gz"),

	/**
	 * <p>
	 * A dump of the <a href="http://www.lexvo.org/">Lexvo.org</a> data.
	 * </p>
	 * <p>
	 * 67 MB extracted
	 * </p>
	 */
	LEXVO("lexvo_latest.rdf",
			"http://www.lexvo.org/resources/lexvo_latest.rdf.gz"),

	/**
	 * <p>
	 * A Data Catalogue Vocabulary file from <a href="https://github.com/Fedict/dcat">Federal Public Service Policy and
	 * Support DG Digital Transformation</a>'s github repository.
	 * </p>
	 * <p>
	 * 139 MB extracted
	 * </p>
	 */
	DATAGOVBE("datagovbe.nt",
			"https://github.com/Fedict/dcat/raw/master/all/datagovbe.nt.gz");

	/**
	 * Name of the target file.
	 */
	private final String fileName;

	/**
	 * Name of the file to extract from the archive; {@code null} if the file is not part of a multi file archive (e.g.
	 * ZIP file).
	 */
	private final String archiveEntryName;

	/**
	 * The {@link URL} to download the file from.
	 */
	private final URL url;

	RDFTestDataset(String fileName, String url) {
		this(fileName, null, url);
	}

	RDFTestDataset(String fileName, String archiveEntryName, String url) {
		this.archiveEntryName = archiveEntryName;
		this.fileName = fileName;
		try {
			this.url = new URL(url);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Statically defiled URL " + url + " is unexpectedly malformed", e);
		}
	}

	/**
	 * Download the dataset file to {@code {java.io.tmpdir}/rdf4j-benchmark-datasets/{fileName}}.
	 *
	 * @return The {@link File} to which the dataset was downloaded and extracted.
	 */
	public File download() {
		File downloadDir = new File(System.getProperty("java.io.tmpdir"), "rdf4j-benchmark-datasets");

		File dataFile = new File(downloadDir, fileName);
		if (dataFile.exists()) {
			return dataFile;
		}

		try {
			Files.createDirectories(downloadDir.toPath());
			File downloadFile = new File(downloadDir, Paths.get(url.getPath()).getFileName().toString());

			if (!downloadFile.exists()) {
				downloadTo(downloadFile);
			}

			if (!downloadFile.equals(dataFile)) {
				extract(downloadFile, dataFile);
				downloadFile.delete();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return dataFile;
	}

	private void downloadTo(File downloadFile) throws IOException {
		System.out.println("Downloading " + url);
		try (InputStream is = new BufferedInputStream(url.openStream())) {
			IOUtil.transfer(is, downloadFile);
		}
	}

	private void extract(File downloadFile, File dataFile) throws IOException {
		String downloadFileName = downloadFile.getName();
		if (downloadFileName.endsWith(".zip")) {
			extractFromZip(downloadFile, dataFile);
		} else if (downloadFileName.endsWith(".gz")) {
			extractFromGzip(downloadFile, dataFile);
		}
	}

	private void extractFromZip(File downloadFile, File dataFile) throws IOException {
		System.out.println("Extracting " + archiveEntryName + " from " + downloadFile + " to " + dataFile.getName());
		try (ZipFile zf = new ZipFile(downloadFile)) {
			ZipEntry entry = zf.getEntry(archiveEntryName);

			try (InputStream in = zf.getInputStream(entry)) {
				IOUtil.writeStream(in, dataFile);
			}
		}
	}

	private void extractFromGzip(File downloadFile, File dataFile) throws IOException {
		System.out.println("Extracting " + downloadFile + " to " + dataFile.getName());
		GZIPInputStream in = new GZIPInputStream(new FileInputStream(downloadFile));
		IOUtil.writeStream(in, dataFile);
	}

}
