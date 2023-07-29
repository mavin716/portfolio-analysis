package com.wise.portfolio.pdf;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.itextpdf.io.font.FontConstants;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;

public class FooterHandler implements IEventHandler {
	
	public FooterHandler() {
		super();
		try {
			font = PdfFontFactory.createFont(FontConstants.HELVETICA_BOLD);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy hh:mm a");
    public  PdfFont font = null;

    protected String footer;
    protected Rectangle rectangle = new Rectangle(500, 803, 30, 30);
    
	PdfFormXObject template = new PdfFormXObject(rectangle);


    @Override
    public void handleEvent(Event event) {
    	
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfPage page = docEvent.getPage();
        PdfCanvas canvas = new PdfCanvas(page);
 //       float height = page.getPageSize().getHeight();
 //       float width = page.getPageSize().getWidth();
		canvas.setFontAndSize(font, 12f);
        canvas.beginText();
        canvas.moveText(50, 10);
        canvas.showText(String.format("Page %d ", docEvent.getDocument().getPageNumber(page)));
        canvas.moveText(1000, 0);
        canvas.showText(LocalDateTime.now().format(TIME_FORMATTER));
        canvas.endText();
        canvas.stroke();
        //canvas.addXObject(template, 20, 20);
        canvas.release();
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }
}
