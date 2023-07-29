package com.wise.portfolio.pdf;


import java.io.IOException;
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

public class HeaderHandler implements IEventHandler {
	
	public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy hh:mm a");

    protected String heading;
    protected Rectangle rectangle = new Rectangle(0, 0, 30, 30);
    
	PdfFormXObject template = new PdfFormXObject(rectangle);


    @Override
    public void handleEvent(Event event) {
    	
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfPage page = docEvent.getPage();
        PdfFont font = null;
		try {
			font = PdfFontFactory.createFont(FontConstants.HELVETICA_BOLD);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        PdfCanvas canvas = new PdfCanvas(page);
 		canvas.setFontAndSize(font, 12f);
        canvas.beginText();
         canvas.moveText(20, 850);
        canvas.showText(heading);
        canvas.endText();
        canvas.stroke();
        canvas.addXObject(template, 10, 10);
        canvas.release();
    }

    public void setHeader(String heading) {
        this.heading = heading;
    }
}
