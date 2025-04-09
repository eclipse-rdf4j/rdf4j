package org.eclipse.rdf4j.query.resultio.sparqlxslx;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;

/**
 * Render a SPARQL result set into an ooxml file.
 *
 * @author Jerven Bolleman
 */
public class SPARQLResultsXLSXWriter implements TupleQueryResultWriter {

	private OutputStream out;
	private XSSFWorkbook wb;
	private XSSFSheet nice;
	private XSSFSheet raw;
	private int rawRowIndex;
	private int niceRowIndex;
	private final Map<String, Integer> columnIndexes = new HashMap<>();
	private XSSFCellStyle headerCellStyle;
	private XSSFCellStyle iriCellStyle;
	private XSSFCellStyle anyiriCellStyle;
	private final Map<String, String> prefixes = new HashMap<>();

	public SPARQLResultsXLSXWriter(OutputStream out) {
		this.out = out;
		wb = new XSSFWorkbookFactory().create();
		nice = wb.createSheet("nice");
		raw = wb.createSheet("raw");
		headerCellStyle = wb.createCellStyle();
		IndexedColorMap colorMap = wb.getStylesSource().getIndexedColors();
		XSSFColor lightGray = new XSSFColor(Color.LIGHT_GRAY, colorMap);
		XSSFColor blue = new XSSFColor(Color.BLUE, colorMap);
		XSSFColor magenta = new XSSFColor(Color.MAGENTA, colorMap);
		headerCellStyle.setFillForegroundColor(lightGray);
		headerCellStyle.setAlignment(HorizontalAlignment.RIGHT);
		headerCellStyle.setBorderBottom(BorderStyle.MEDIUM);
		headerCellStyle.setBorderLeft(BorderStyle.MEDIUM);
		headerCellStyle.setBorderRight(BorderStyle.MEDIUM);
		headerCellStyle.setBorderTop(BorderStyle.MEDIUM);

		iriCellStyle = wb.createCellStyle();
		Font hlinkFont = wb.createFont();
		hlinkFont.setUnderline(Font.U_SINGLE);
		hlinkFont.setColor(blue.getIndex());
		iriCellStyle.setFont(hlinkFont);

		anyiriCellStyle = wb.createCellStyle();
		Font anyhlinkFont = wb.createFont();
		anyhlinkFont.setUnderline(Font.U_SINGLE);
		anyhlinkFont.setColor(magenta.getIndex());
		anyiriCellStyle.setFont(hlinkFont);

	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		raw.createRow(0).createCell(0).setCellValue(value);
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		POIXMLProperties properties = wb.getProperties();
		properties.getCustomProperties().addProperty("links", linkUrls.stream().collect(Collectors.joining(", ")));
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		XSSFRow rawRow = raw.createRow(rawRowIndex++);
		XSSFRow niceRow = nice.createRow(niceRowIndex++);
		int columnIndex = 0;
		for (String bindingName : bindingNames) {
			columnIndexes.put(bindingName, columnIndex);
			XSSFCell rawHeader = rawRow.createCell(columnIndex);
			rawHeader.setCellValue(bindingName);
			rawHeader.setCellStyle(headerCellStyle);
			XSSFCell niceHeader = niceRow.createCell(columnIndex);
			niceHeader.setCellValue(bindingName);
			niceHeader.setCellStyle(headerCellStyle);

			columnIndex++;
		}
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		try {
			for (int c = 0; c < columnIndexes.size(); c++) {
				nice.autoSizeColumn(c);
				raw.autoSizeColumn(c);
			}
			wb.write(out);
			wb.close();
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		XSSFRow rawRow = raw.createRow(rawRowIndex++);
		XSSFRow niceRow = nice.createRow(niceRowIndex++);
		for (Binding b : bindingSet) {
			int ci = columnIndexes.get(b.getName());
			rawRow.createCell(ci).setCellValue(b.getValue().stringValue());
			XSSFCell nc = niceRow.createCell(ci);
			Value v = b.getValue();
			if (v.isLiteral()) {
				handleLiteral(nc, v);
			} else if (v instanceof IRI) {
				handleIri(nc, v);
			} else if (v.isBNode()) {
				handleIri(nc, v);
			} else if (v instanceof Triple) {
				handleTriple(nc, v);
			}
		}
	}

	private void handleLiteral(XSSFCell nc, Value v) {
		Literal l = (Literal) v;
		CoreDatatype cd = l.getCoreDatatype();
		if (cd != null) {
			if (cd.isXSDDatatype()) {
				handeXSDDatatype(nc, l);
			} else if (cd.isGEODatatype()) {
				handeGeoDatatype(nc, l);
			} else if (cd.isRDFDatatype()) {
				handeRDFDatatype(nc, l);
			}
		} else if (l.getLanguage().isPresent()) {
			handleLanguageString(nc, l);
		} else {
			nc.setCellValue(v.stringValue());
		}
	}

	private void handeRDFDatatype(XSSFCell nc, Literal l) {
		defaultFormat(nc, l);
	}

	private void handeGeoDatatype(XSSFCell nc, Literal l) {
		defaultFormat(nc, l);
	}

	private void handeXSDDatatype(XSSFCell nc, Literal l) {
		XSD as = l.getCoreDatatype().asXSDDatatypeOrNull();
		if (as == null) {
			nc.setCellValue(l.stringValue());
		} else {
			switch (as) {
			case ANYURI: {
				handleIri(nc, SimpleValueFactory.getInstance().createIRI(l.stringValue()));
				nc.setCellStyle(anyiriCellStyle);
				break;
			}
			case BOOLEAN: {
				nc.setCellValue(l.booleanValue());
				break;
			}
			case BYTE: {
				nc.setCellValue(l.byteValue());
				break;
			}
			case DATE: {
				nc.setCellValue(l.calendarValue().toGregorianCalendar());
				break;
			}
			case DATETIME: {
				nc.setCellValue(l.calendarValue().toGregorianCalendar());
				break;
			}
			case DATETIMESTAMP: {
				nc.setCellValue(l.calendarValue().toGregorianCalendar());
				break;
			}
			case DAYTIMEDURATION: {
				formatAsDate(nc, l);
				break;
			}
			case DECIMAL: {
				BigDecimal dv = l.decimalValue();
				nc.setCellValue(dv.toPlainString());
				nc.setCellType(CellType.NUMERIC);
				break;
			}
			case DOUBLE: {
				nc.setCellValue(l.doubleValue());
				nc.setCellType(CellType.NUMERIC);
				break;
			}

			case FLOAT: {
				nc.setCellValue(l.floatValue());
				nc.setCellType(CellType.NUMERIC);
				break;
			}
			case GDAY:
				formatAsDate(nc, l);
				break;
			case GMONTH:
				formatAsDate(nc, l);
				break;
			case GYEAR:
				formatAsDate(nc, l);
				break;
			case GMONTHDAY:
				formatAsDate(nc, l);
				break;
			case GYEARMONTH:
				formatAsDate(nc, l);
				break;
			case DURATION:
				formatAsDate(nc, l);
				break;
			case INT:
				nc.setCellValue(l.intValue());
				nc.setCellType(CellType.NUMERIC);
				break;
			case INTEGER:
				nc.setCellValue(l.integerValue().doubleValue());
				nc.setCellType(CellType.NUMERIC);
				break;
			case LONG: {
				nc.setCellValue(l.longValue());
				nc.setCellType(CellType.NUMERIC);
				break;
			}
			case NEGATIVE_INTEGER:
				nc.setCellValue(l.integerValue().doubleValue());
				nc.setCellType(CellType.NUMERIC);
				break;
			case NON_NEGATIVE_INTEGER:
				nc.setCellValue(l.integerValue().doubleValue());
				nc.setCellType(CellType.NUMERIC);
				break;
			case NON_POSITIVE_INTEGER:
				nc.setCellValue(l.integerValue().doubleValue());
				nc.setCellType(CellType.NUMERIC);
				break;
			case SHORT:
				nc.setCellValue(l.shortValue());
				nc.setCellType(CellType.NUMERIC);
				break;
			case UNSIGNED_BYTE:
			case UNSIGNED_INT:
			case UNSIGNED_LONG:
			case UNSIGNED_SHORT:
				nc.setCellValue(l.longValue());
				nc.setCellType(CellType.NUMERIC);
				break;
			case YEARMONTHDURATION:
				formatAsDate(nc, l);
				break;
			default:
				defaultFormat(nc, l);
				break;
			}
		}
	}

	private void defaultFormat(XSSFCell nc, Literal l) {
		nc.setCellValue(l.stringValue());

	}

	private void formatAsDate(XSSFCell nc, Literal l) {
		nc.setCellValue(l.stringValue());

	}

	private void handleTriple(XSSFCell nc, Value v) {
		Triple t = (Triple) v;
		XSSFRichTextString r = new XSSFRichTextString();
		r.setString(
				t.getSubject().stringValue() + " " + formatIri(t.getPredicate()) + " " + t.getObject().stringValue());
		nc.setCellValue(r);
	}

	private void handleIri(XSSFCell nc, Value v) {
		IRI i = (IRI) v;
		XSSFHyperlink link = wb.getCreationHelper().createHyperlink(HyperlinkType.URL);
		link.setAddress(v.stringValue());
		nc.setHyperlink(link);

		String ns = formatIri(i);
		nc.setCellValue(ns);
	}

	private String formatIri(IRI i) {
		String ns;
		String localName = i.getLocalName();
		String namespace = i.getNamespace();
		if (prefixes.containsKey(namespace)) {
			return prefixes.get(namespace) + ":" + i.stringValue().substring(namespace.length());
		} else if (localName == null || localName.isEmpty()) {
			ns = i.stringValue();
		} else {
			ns = localName;
		}
		return ns;
	}

	private void handleLanguageString(XSSFCell nc, Literal l) {
		// TODO indicate language maybe with a comment? or a color
		nc.setCellValue(l.stringValue());
	}

	@Override
	public QueryResultFormat getQueryResultFormat() {
		return TupleQueryResultFormat.XSLX;
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws QueryResultHandlerException {
		prefixes.put(uri, prefix);
	}

	@Override
	public void startDocument() throws QueryResultHandlerException {

	}

	@Override
	public void setWriterConfig(WriterConfig config) {
		// TODO Auto-generated method stub

	}

	@Override
	public WriterConfig getWriterConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		return Collections.emptyList();
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.XSLX;
	}

	@Override
	public void handleStylesheet(String stylesheetUrl) throws QueryResultHandlerException {
		// TODO Auto-generated method stub

	}

	@Override
	public void startHeader() throws QueryResultHandlerException {
		// TODO Auto-generated method stub

	}

	@Override
	public void endHeader() throws QueryResultHandlerException {
		// TODO Auto-generated method stub

	}

}
