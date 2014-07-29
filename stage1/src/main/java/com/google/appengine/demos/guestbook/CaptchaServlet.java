package com.google.appengine.demos.guestbook;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;

import java.util.Random;
import javax.imageio.ImageIO;
import javax.servlet.*;
import javax.servlet.http.*;

// inspired by: http://zetcode.com/tutorials/jeetutorials/captcha/

public class CaptchaServlet extends HttpServlet {


  protected void processRequest(HttpServletRequest request,
                                HttpServletResponse response)
                 throws ServletException, IOException {

    int width = 150;
    int height = 50;

    char data[][] = {
        { 'a', 'n', 'd', 'r', 'o', 'i', 'd' },
        { 'c', 'l', 'o', 'u', 'd' },
        { 'g', 'o', 'o', 'g', 'l', 'e' },
        { 'd', 'o', 'c', 'k', 'e', 'r' },
        { 'g', 'o', 'l', 'a', 'n', 'g' },
        { 'p', 'y', 't', 'h', 'o', 'n' },
        { 'c', 'o', 'm', 'p', 'u', 't', 'e' },
        { 'm', 'a', 'p', 's', 'a', 'p', 'i' }
    };


    BufferedImage bufferedImage = new BufferedImage(width, height,
                  BufferedImage.TYPE_INT_RGB);

    Graphics2D g2d = bufferedImage.createGraphics();

    Font font = new Font("Georgia", Font.BOLD, 18);
    g2d.setFont(font);

    RenderingHints rh = new RenderingHints(
           RenderingHints.KEY_ANTIALIASING,
           RenderingHints.VALUE_ANTIALIAS_ON);

    rh.put(RenderingHints.KEY_RENDERING,
           RenderingHints.VALUE_RENDER_QUALITY);

    g2d.setRenderingHints(rh);

    GradientPaint gp = new GradientPaint(0, 0,
    Color.red, 0, height/2, Color.black, true);

    g2d.setPaint(gp);
    g2d.fillRect(0, 0, width, height);

    g2d.setColor(new Color(255, 153, 0));

    Random r = new Random();
    int index = Math.abs(r.nextInt()) % 5;

    String captcha = String.copyValueOf(data[index]);
    request.getSession().setAttribute("captcha", captcha );

    int x = 0;
    int y = 0;

    for (int i=0; i<data[index].length; i++) {
        x += 10 + (Math.abs(r.nextInt()) % 15);
        y = 20 + Math.abs(r.nextInt()) % 20;
        g2d.drawChars(data[index], i, 1, x, y);
    }

    g2d.dispose();

    response.setContentType("image/png");
    OutputStream os = response.getOutputStream();
    ImageIO.write(bufferedImage, "png", os);
    os.close();
  }


  protected void doGet(HttpServletRequest request,
                       HttpServletResponse response)
                           throws ServletException, IOException {
      processRequest(request, response);
  }

}

