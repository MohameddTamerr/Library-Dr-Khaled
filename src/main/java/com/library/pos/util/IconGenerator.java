package com.library.pos.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class IconGenerator {
    public static void main(String[] args) {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background (Rounded Square)
        g2d.setColor(new Color(15, 23, 42)); // #0f172a (Deep Blue)
        g2d.fillRoundRect(0, 0, size, size, 120, 120);

        // Border
        g2d.setColor(new Color(51, 65, 85)); // #334155 (Slate Gray)
        g2d.setStroke(new BasicStroke(20));
        g2d.drawRoundRect(10, 10, size - 20, size - 20, 120, 120);

        // Book Symbol (Simple Shapes)
        g2d.setColor(new Color(16, 185, 129)); // #10b981 (Emerald Green)
        // Left Page
        int margin = 100;
        int bookWidth = size - (2 * margin);
        int bookHeight = size - (2 * margin);

        // Draw open book
        g2d.fillRect(margin, margin + 50, bookWidth / 2 - 10, bookHeight - 100);
        g2d.fillRect(size / 2 + 10, margin + 50, bookWidth / 2 - 10, bookHeight - 100);

        // Text "POS"
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 120));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "POS";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(text, x, y);

        g2d.dispose();

        try {
            File outputFile = new File("src/main/resources/images/app_icon.png");
            outputFile.getParentFile().mkdirs();
            ImageIO.write(image, "png", outputFile);
            System.out.println("Icon generated successfully: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
