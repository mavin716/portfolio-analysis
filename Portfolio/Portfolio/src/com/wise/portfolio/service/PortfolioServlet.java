package com.wise.portfolio.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.wise.portfolio.PortfolioApp;

public class PortfolioServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
		//super.doGet(req, resp);
		PortfolioApp app = new PortfolioApp();
		File pdfFile = app.run();
		
        response.setHeader("Content-Type", "application/pdf");
        
        try (ServletOutputStream out = response.getOutputStream()) {
            Path path = pdfFile.toPath();
            Files.copy(path, out);
            out.flush();
        } catch (IOException e) {
            // handle exception
        }

	}

}
