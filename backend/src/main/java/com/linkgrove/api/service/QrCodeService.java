package com.linkgrove.api.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.common.BitMatrix;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;

@Service
public class QrCodeService {

    private final MeterRegistry meterRegistry;

    public QrCodeService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public byte[] generatePng(String content, int size, int margin) {
        try {
            BitMatrix matrix = encode(content, size, margin, ErrorCorrectionLevel.M);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, new MatrixToImageConfig());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR PNG", e);
        }
    }

    public String generateSvg(String content, int size, int margin) {
        try {
            BitMatrix matrix = encode(content, size, margin, ErrorCorrectionLevel.M);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            StringBuilder svg = new StringBuilder();
            svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
               .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ")
               .append(width).append(" ").append(height).append("\">");
            svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>");
            svg.append("<path d=\"");
            boolean first = true;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (matrix.get(x, y)) {
                        if (!first) svg.append(" ");
                        first = false;
                        svg.append("M").append(x).append(" ").append(y).append("h1v1h-1z");
                    }
                }
            }
            svg.append("\" fill=\"#000000\"/></svg>");
            return svg.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR SVG", e);
        }
    }

    @Cacheable(value = "qrPng", key = "'png:' + #content + ':' + #size + ':' + #margin + ':' + (#fgColorArgb==null? '':#fgColorArgb) + ':' + (#bgColorArgb==null?'':#bgColorArgb) + ':' + (#logoUrl==null?'':#logoUrl) + ':M'")
    public byte[] generatePng(String content, int size, int margin, Integer fgColorArgb, Integer bgColorArgb, String logoUrl) {
        return timePng(content, size, margin, fgColorArgb, bgColorArgb, logoUrl, ErrorCorrectionLevel.M,
                () -> doGeneratePng(content, size, margin, fgColorArgb, bgColorArgb, logoUrl, ErrorCorrectionLevel.M));
    }

    @Cacheable(value = "qrSvg", key = "'svg:' + #content + ':' + #size + ':' + #margin + ':' + (#fgColorArgb==null? '':#fgColorArgb) + ':' + (#bgColorArgb==null?'':#bgColorArgb) + ':M'")
    public String generateSvg(String content, int size, int margin, Integer fgColorArgb, Integer bgColorArgb) {
        return timeSvg(content, size, margin, fgColorArgb, bgColorArgb, ErrorCorrectionLevel.M,
                () -> doGenerateSvg(content, size, margin, fgColorArgb, bgColorArgb, ErrorCorrectionLevel.M));
    }

    // Overloads with explicit error correction level
    @Cacheable(value = "qrPng", key = "'png:' + #content + ':' + #size + ':' + #margin + ':' + (#fgColorArgb==null? '':#fgColorArgb) + ':' + (#bgColorArgb==null?'':#bgColorArgb) + ':' + (#logoUrl==null?'':#logoUrl) + ':' + (#ecc==null?'M':#ecc.name())")
    public byte[] generatePng(String content, int size, int margin, Integer fgColorArgb, Integer bgColorArgb, String logoUrl, ErrorCorrectionLevel ecc) {
        try {
            BitMatrix matrix = encode(content, size, margin, ecc == null ? ErrorCorrectionLevel.M : ecc);
            int fg = fgColorArgb != null ? fgColorArgb : MatrixToImageConfig.BLACK;
            int bg = bgColorArgb != null ? bgColorArgb : MatrixToImageConfig.WHITE;
            if (!hasSufficientContrast(fg, bg)) {
                fg = MatrixToImageConfig.BLACK;
                bg = MatrixToImageConfig.WHITE;
            }
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, new MatrixToImageConfig(fg, bg));

            if (isSafeLogoUrl(logoUrl)) {
                BufferedImage logo = fetchAndValidateLogo(logoUrl);
                image = overlayLogo(image, logo);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR PNG", e);
        }
    }

    @Cacheable(value = "qrSvg", key = "'svg:' + #content + ':' + #size + ':' + #margin + ':' + (#fgColorArgb==null? '':#fgColorArgb) + ':' + (#bgColorArgb==null?'':#bgColorArgb) + ':' + (#ecc==null?'M':#ecc.name())")
    public String generateSvg(String content, int size, int margin, Integer fgColorArgb, Integer bgColorArgb, ErrorCorrectionLevel ecc) {
        try {
            BitMatrix matrix = encode(content, size, margin, ecc == null ? ErrorCorrectionLevel.M : ecc);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            String fg = toHexColor(fgColorArgb, "#000000");
            String bg = toHexColor(bgColorArgb, "#FFFFFF");
            if (!hasSufficientContrast(parseHexToArgb(fg), parseHexToArgb(bg))) {
                fg = "#000000";
                bg = "#FFFFFF";
            }
            StringBuilder svg = new StringBuilder();
            svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
               .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ")
               .append(width).append(" ").append(height).append("\">");
            svg.append("<rect width=\"100%\" height=\"100%\" fill=\"").append(bg).append("\"/>");
            svg.append("<path d=\"");
            boolean first = true;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (matrix.get(x, y)) {
                        if (!first) svg.append(" ");
                        first = false;
                        svg.append("M").append(x).append(" ").append(y).append("h1v1h-1z");
                    }
                }
            }
            svg.append("\" fill=\"").append(fg).append("\"/></svg>");
            return svg.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR SVG", e);
        }
    }

    private byte[] timePng(String content, int size, int margin, Integer fgColorArgb, Integer bgColorArgb, String logoUrl, ErrorCorrectionLevel ecc, java.util.concurrent.Callable<byte[]> c) {
        Timer timer = Timer.builder("qr.generate.png")
                .description("QR PNG generation")
                .tag("ecc", ecc == null ? "M" : ecc.name())
                .tag("logo", (logoUrl == null || logoUrl.isBlank()) ? "none" : "present")
                .tag("size", String.valueOf(size))
                .register(meterRegistry);
        try {
            return timer.recordCallable(c);
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    private String timeSvg(String content, int size, int margin, Integer fgColorArgb, Integer bgColorArgb, ErrorCorrectionLevel ecc, java.util.concurrent.Callable<String> c) {
        Timer timer = Timer.builder("qr.generate.svg")
                .description("QR SVG generation")
                .tag("ecc", ecc == null ? "M" : ecc.name())
                .tag("size", String.valueOf(size))
                .register(meterRegistry);
        try {
            return timer.recordCallable(c);
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }

    // Shared generation helpers for instrumentation wrappers
    private byte[] doGeneratePng(String content, int size, int margin, Integer fgColorArgb, Integer bgColorArgb, String logoUrl, ErrorCorrectionLevel ecc) {
        try {
            BitMatrix matrix = encode(content, size, margin, ecc == null ? ErrorCorrectionLevel.M : ecc);
            int fg = fgColorArgb != null ? fgColorArgb : MatrixToImageConfig.BLACK;
            int bg = bgColorArgb != null ? bgColorArgb : MatrixToImageConfig.WHITE;
            if (!hasSufficientContrast(fg, bg)) {
                fg = MatrixToImageConfig.BLACK;
                bg = MatrixToImageConfig.WHITE;
            }
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, new MatrixToImageConfig(fg, bg));

            if (isSafeLogoUrl(logoUrl)) {
                BufferedImage logo = fetchAndValidateLogo(logoUrl);
                image = overlayLogo(image, logo);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR PNG", e);
        }
    }

    private String doGenerateSvg(String content, int size, int margin, Integer fgColorArgb, Integer bgColorArgb, ErrorCorrectionLevel ecc) {
        try {
            BitMatrix matrix = encode(content, size, margin, ecc == null ? ErrorCorrectionLevel.M : ecc);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            String fg = toHexColor(fgColorArgb, "#000000");
            String bg = toHexColor(bgColorArgb, "#FFFFFF");
            if (!hasSufficientContrast(parseHexToArgb(fg), parseHexToArgb(bg))) {
                fg = "#000000";
                bg = "#FFFFFF";
            }
            StringBuilder svg = new StringBuilder();
            svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
               .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ")
               .append(width).append(" ").append(height).append("\">");
            svg.append("<rect width=\"100%\" height=\"100%\" fill=\"").append(bg).append("\"/>");
            svg.append("<path d=\"");
            boolean first = true;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (matrix.get(x, y)) {
                        if (!first) svg.append(" ");
                        first = false;
                        svg.append("M").append(x).append(" ").append(y).append("h1v1h-1z");
                    }
                }
            }
            svg.append("\" fill=\"").append(fg).append("\"/></svg>");
            return svg.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR SVG", e);
        }
    }

    private BufferedImage overlayLogo(BufferedImage qr, BufferedImage logo) {
        int qrW = qr.getWidth();
        int qrH = qr.getHeight();
        // Scale logo to ~20% of QR size
        int maxLogoW = qrW / 5;
        int maxLogoH = qrH / 5;
        int logoW = logo.getWidth();
        int logoH = logo.getHeight();
        double scale = Math.min((double) maxLogoW / logoW, (double) maxLogoH / logoH);
        int drawW = Math.max(1, (int) Math.round(logoW * scale));
        int drawH = Math.max(1, (int) Math.round(logoH * scale));
        int x = (qrW - drawW) / 2;
        int y = (qrH - drawH) / 2;

        BufferedImage out = new BufferedImage(qrW, qrH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setComposite(AlphaComposite.SrcOver);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(qr, 0, 0, null);
            g.drawImage(logo, x, y, drawW, drawH, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private boolean isSafeLogoUrl(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase();
        if (u.length() < 8 || u.length() > 1024) return false;
        if (!(u.startsWith("https://") || u.startsWith("http://"))) return false;
        // Simple extension check to limit types
        if (!(u.endsWith(".png") || u.endsWith(".jpg") || u.endsWith(".jpeg"))) return false;
        return true;
    }

    private boolean hasSufficientContrast(int fgArgb, int bgArgb) {
        double l1 = relativeLuminance(fgArgb);
        double l2 = relativeLuminance(bgArgb);
        double ratio = (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
        return ratio >= 2.5; // heuristic threshold for QR scanners
    }

    private double relativeLuminance(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        double rs = srgbToLinear(r / 255.0);
        double gs = srgbToLinear(g / 255.0);
        double bs = srgbToLinear(b / 255.0);
        return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
    }

    private double srgbToLinear(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private int parseHexToArgb(String hex) {
        if (hex == null) return 0xFF000000;
        String s = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            int rgb = (int) Long.parseLong(s, 16);
            if (s.length() <= 6) return 0xFF000000 | rgb;
            return rgb;
        } catch (Exception e) {
            return 0xFF000000;
        }
    }

    private String toHexColor(Integer argb, String def) {
        if (argb == null) return def;
        int rgb = argb & 0x00FFFFFF;
        String hex = String.format("#%06X", rgb);
        return hex;
    }

    @SuppressWarnings("unused")
    private BitMatrix encode(String content, int size, int margin) throws Exception {
        return encode(content, size, margin, ErrorCorrectionLevel.M);
    }

    private BitMatrix encode(String content, int size, int margin, ErrorCorrectionLevel ecc) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, Math.max(0, margin));
        if (ecc != null) {
            hints.put(EncodeHintType.ERROR_CORRECTION, ecc);
        }
        return new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
    }

    private BufferedImage fetchAndValidateLogo(String logoUrl) {
        try {
            java.net.URI uri = java.net.URI.create(logoUrl);
            java.net.URLConnection conn = uri.toURL().openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            String ct = conn.getContentType();
            String lower = ct == null ? "" : ct.toLowerCase();
            if (!(lower.startsWith("image/png") || lower.startsWith("image/jpeg"))) {
                throw new IllegalArgumentException("Unsupported logo content-type");
            }
            long len = conn.getContentLengthLong();
            if (len > 5_000_000L) { // 5 MB
                throw new IllegalArgumentException("Logo too large");
            }
            try (java.io.InputStream in = conn.getInputStream()) {
                BufferedImage img = ImageIO.read(in);
                if (img == null) throw new IllegalArgumentException("Logo is not a valid image");
                return img;
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to fetch logo", e);
        }
    }
}


