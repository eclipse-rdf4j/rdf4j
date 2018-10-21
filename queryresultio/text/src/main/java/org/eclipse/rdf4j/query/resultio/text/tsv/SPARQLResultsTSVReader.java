package org.eclipse.rdf4j.query.resultio.text.tsv;

import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.Reader;

/**
 * This reader respects the TSV semantics of RDF4J and does absolutely no
 * processing except for splitting the line on horizontal tabulator characters.
 */
public class SPARQLResultsTSVReader extends CSVReader {
    public SPARQLResultsTSVReader(Reader reader) {
        super(reader);
    }

    @Override
    public String[] readNext() throws IOException {
        String line = getNextLine();
        return line == null ? null : validateResult(line.split("\t", -1));
    }
}
