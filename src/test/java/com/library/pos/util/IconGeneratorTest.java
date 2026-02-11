package com.library.pos.util;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class IconGeneratorTest {

    @Test
    public void generateIcon() {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background (Rounded Square) - Deep Blue
        g2d.setColor(new Color(15, 23, 42));
        g2d.fillRoundRect(0, 0, size, size, 120, 120);

        // Border - Slate Gray
        g2d.setColor(new Color(51, 65, 85));
        g2d.setStroke(new BasicStroke(16));
        g2d.drawRoundRect(8, 8, size - 16, size - 16, 120, 120);

        // Stylized Book Icon
        g2d.setColor(new Color(16, 185, 129)); // Emerald Green

        // Book Spine
        int spineWidth = 40;
        int centerX = size / 2;
        int bookY = 120;
        int bookHeight = 280;
        int bookWidth = 360;

        // Left Page
        int leftX = centerX - bookWidth / 2;
        g2d.fillRoundRect(leftX, bookY, bookWidth / 2 - 5, bookHeight, 20, 20);

        // Right Page
        g2d.fillRoundRect(centerX + 5, bookY, bookWidth / 2 - 5, bookHeight, 20, 20);

        // Text "LMS"
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 100));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "LMS";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = bookY + bookHeight + 90;
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
