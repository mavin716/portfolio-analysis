package com.wise.portfolio.service;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.jfree.chart.JFreeChart;

import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.orsonpdf.PDFDocument;
import com.orsonpdf.PDFGraphics2D;
import com.orsonpdf.Page;
import com.wise.portfolio.pdf.FooterHandler;
import com.wise.portfolio.pdf.HeaderHandler;

public class PortfolioReport  {

	private Document document;
	public Document getDocument() {
		return document;
	}

	private PdfDocument pdfDoc;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PortfolioReport(String filename) throws FileNotFoundException {

		PdfWriter writer = new PdfWriter(filename);
		PdfDocument pdfDoc = new PdfDocument(writer);
		
		document = new Document(pdfDoc, PageSize.LEDGER);
		document.setMargins(30f, 10f, 30f, 10f);

		// Document Title
		document.add(new Paragraph("Report run at " + LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yy"))
				+ " " + LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))).setFontSize(14)
				.setHorizontalAlignment(HorizontalAlignment.CENTER));
		HeaderHandler headerHandler = new HeaderHandler();
		pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, headerHandler);
		FooterHandler footerHandler = new FooterHandler();
		pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);

	}

	public void addTable(Table table) {
		document.add(table);
		document.add(new AreaBreak());
	}
	public  void addChart(JFreeChart lineChart) {
		
		// Draw the chart into a transition PDF
		PDFDocument doc = new PDFDocument();
		Rectangle bounds = new Rectangle(1200, 720);
		Page page = doc.createPage(bounds);
		PDFGraphics2D g2 = page.getGraphics2D();
		lineChart.draw(g2, bounds);

		// Add the graph PDF image into the document
		try {
			PdfReader reader = new PdfReader(new ByteArrayInputStream(doc.getPDFBytes()));
			PdfDocument chartDoc = new PdfDocument(reader);
			PdfFormXObject chart = chartDoc.getFirstPage().copyAsFormXObject(pdfDoc);
			chartDoc.close();
			Image chartImage = new Image(chart);
			document.add(chartImage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
