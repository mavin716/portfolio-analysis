package com.wise.portfolio.data;


import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;

public class HeaderHandler implements IEventHandler {
    protected String heading;
    protected Rectangle rectangle = new Rectangle(550, 803, 30, 30);
    
	PdfFormXObject template = new PdfFormXObject(rectangle);


    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfPage page = docEvent.getPage();
        int pageNum = docEvent.getDocument().getPageNumber(page);
        PdfCanvas canvas = new PdfCanvas(page);
        canvas.beginText();
        canvas.moveText(34, 803);
        canvas.showText(heading);
        canvas.moveText(450, 0);
        canvas.showText(String.format("Page %d of", pageNum));
        canvas.endText();
        canvas.stroke();
        canvas.addXObject(template, 0, 0);
        canvas.release();
    }

    public void setHeader(String heading) {
        this.heading = heading;
    }
}
