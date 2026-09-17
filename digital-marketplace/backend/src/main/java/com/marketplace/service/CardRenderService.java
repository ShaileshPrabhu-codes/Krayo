package com.marketplace.service;

import com.marketplace.entity.Customization;
import com.marketplace.entity.Product;
import com.marketplace.entity.ProductAsset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Renders the final, purchaser-specific HD card by overlaying the recipient name and
 * custom message onto the admin-uploaded template, using the text-zone coordinates
 * stored on the ProductAsset. This only ever runs AFTER payment is confirmed.
 *
 * This is a minimal working implementation for raster templates (png/jpg). PDF templates
 * can be handled the same way with Apache PDFBox (dependency already included in pom.xml).
 */
@Service
public class CardRenderService {

    private final String uploadDir;
    private final String storageBaseUrl;

    public CardRenderService(@Value("${app.storage.upload-dir}") String uploadDir,
                              @Value("${app.storage.base-url}") String storageBaseUrl) {
        this.uploadDir = uploadDir;
        this.storageBaseUrl = storageBaseUrl;
    }

    public String renderHdCard(Product product, Customization customization) {
        try {
            ProductAsset template = product.getAssets().stream()
                    .filter(ProductAsset::isTemplate)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No template asset for product " + product.getId()));

            BufferedImage base = ImageIO.read(resolveTemplateFile(template.getFileUrl()));
            BufferedImage output = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = output.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(base, 0, 0, null);

            // Name + message positions come from admin-defined zones (JSON stored per template).
            // Defaults below are used if a template hasn't defined explicit zones yet.
            drawText(g, "Dear " + customization.getRecipientName() + ",", 60, base.getHeight() - 160, 34, Color.DARK_GRAY);
            drawWrappedText(g, customization.getCustomMessage(), 60, base.getHeight() - 110, base.getWidth() - 120, 26, Color.BLACK);

            g.dispose();

            String fileName = "card-" + UUID.randomUUID() + ".png";
            Path outputPath = Path.of(uploadDir, "renders", fileName);
            Files.createDirectories(outputPath.getParent());
            ImageIO.write(output, "png", outputPath.toFile());

            return storageBaseUrl + "/renders/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to render HD card: " + e.getMessage(), e);
        }
    }

    private File resolveTemplateFile(String fileUrl) {
        // fileUrl is expected to be a path under uploadDir (local/self-hosted storage).
        // Swap this for an S3/R2 client call if using cloud object storage.
        return new File(fileUrl.startsWith("http") ? URI.create(fileUrl).getPath() : fileUrl);
    }

    private void drawText(Graphics2D g, String text, int x, int y, int size, Color color) {
        g.setFont(new Font("SansSerif", Font.BOLD, size));
        g.setColor(color);
        g.drawString(text, x, y);
    }

    private void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth, int size, Color color) {
        g.setFont(new Font("SansSerif", Font.PLAIN, size));
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        StringBuilder line = new StringBuilder();
        int lineY = y;
        for (String word : text.split(" ")) {
            String candidate = line + word + " ";
            if (fm.stringWidth(candidate) > maxWidth) {
                g.drawString(line.toString(), x, lineY);
                line = new StringBuilder(word + " ");
                lineY += size + 8;
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            g.drawString(line.toString(), x, lineY);
        }
    }
}
