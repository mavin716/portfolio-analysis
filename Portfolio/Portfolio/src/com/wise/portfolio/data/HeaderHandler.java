package com.wise.portfolio.data;


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

public class HeaderHandler implements IEventHandler {
	
	public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy hh:mm a");

    protected String heading;
    protected Rectangle rectangle = new Rectangle(550, 803, 30, 30);
    
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
 //       canvas.moveText(34, 803);
        canvas.moveText(20, 20);
        canvas.showText(heading);
//        canvas.moveText(450, 0);
        canvas.moveText(600, 0);
        canvas.showText(String.format("Page %d ", docEvent.getDocument().getPageNumber(page)));
        canvas.moveText(400, 0);
        canvas.showText(LocalDateTime.now().format(TIME_FORMATTER));
        canvas.endText();
        canvas.stroke();
        canvas.addXObject(template, 20, 20);
        canvas.release();
    }

    public void setHeader(String heading) {
        this.heading = heading;
    }
}
