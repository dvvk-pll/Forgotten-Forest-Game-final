package com.dsarpg.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import com.dsarpg.usability.UserSettings;

/**
 * Drawing helpers shared by the HUD, battle screen and overlays.
 *
 * All text goes through {@link #font(double, boolean)}, which applies the
 * player's text-size setting, so every label scales together.
 */
public final class UiKit {

    private static final Map<String, Font> FONTS = new HashMap<>();
    private static final Map<String, Double> WIDTHS = new HashMap<>();
    private static final Map<String, List<String>> WRAPS = new HashMap<>();

    private UiKit() {
    }

    // --------------------------------------------------
    // FONTS AND MEASURING
    // --------------------------------------------------

    public static double scale() {
        return UserSettings.get().getTextScale();
    }

    public static Font font(double size, boolean bold) {
        return fontScaled(size, bold, scale());
    }

    public static Font fontScaled(double size, boolean bold, double scale) {
        double px = Math.round(size * scale * 10.0) / 10.0;
        String key = px + (bold ? "b" : "n");
        Font font = FONTS.get(key);

        if (font == null) {
            font = Font.font("Arial",
                    bold ? FontWeight.BOLD : FontWeight.NORMAL, px);
            FONTS.put(key, font);
        }

        return font;
    }

    public static double width(String s, double size, boolean bold, double scale) {
        if (s == null || s.isEmpty()) {
            return 0;
        }

        Font font = fontScaled(size, bold, scale);
        String key = font.getSize() + (bold ? "b|" : "n|") + s;
        Double cachedWidth = WIDTHS.get(key);

        if (cachedWidth == null) {
            if (WIDTHS.size() > 4000) {
                WIDTHS.clear();
            }

            Text t = new Text(s);
            t.setFont(font);
            cachedWidth = t.getLayoutBounds().getWidth();
            WIDTHS.put(key, cachedWidth);
        }

        return cachedWidth;
    }

    public static double width(String s, double size, boolean bold) {
        return width(s, size, bold, scale());
    }

    /** Splits text into lines that fit a pixel width (cached). */
    public static List<String> wrap(String text, double maxWidth,
                                    double size, boolean bold, double scale) {

        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        String key = maxWidth + "|" + size + "|" + bold + "|" + scale + "|" + text;
        List<String> cachedLines = WRAPS.get(key);

        if (cachedLines != null) {
            return cachedLines;
        }

        if (WRAPS.size() > 600) {
            WRAPS.clear();
        }

        List<String> lines = new ArrayList<>();

        for (String paragraph : text.replace("\r", "").split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder line = new StringBuilder();

            for (String word : paragraph.split("\\s+")) {
                String candidate = line.length() == 0 ? word : line + " " + word;

                if (width(candidate, size, bold, scale) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }

                if (line.length() > 0) {
                    lines.add(line.toString());
                    line.setLength(0);
                }

                // A single very long word: break it by characters.
                if (width(word, size, bold, scale) > maxWidth) {
                    StringBuilder part = new StringBuilder();

                    for (int i = 0; i < word.length(); i++) {
                        String test = part.toString() + word.charAt(i);

                        if (width(test, size, bold, scale) > maxWidth
                                && part.length() > 0) {
                            lines.add(part.toString());
                            part.setLength(0);
                        }

                        part.append(word.charAt(i));
                    }

                    line.append(part);
                } else {
                    line.append(word);
                }
            }

            if (line.length() > 0) {
                lines.add(line.toString());
            }
        }

        WRAPS.put(key, lines);
        return lines;
    }

    public static List<String> wrap(String text, double maxWidth,
                                    double size, boolean bold) {
        return wrap(text, maxWidth, size, bold, scale());
    }

    // --------------------------------------------------
    // TEXT
    // --------------------------------------------------

    /** Draws one line; y is the text baseline. */
    public static void text(GraphicsContext gc, String s, double x, double y,
                            Color color, double size, boolean bold) {
        textScaled(gc, s, x, y, color, size, bold, scale());
    }

    public static void textScaled(GraphicsContext gc, String s, double x,
                                  double y, Color color, double size,
                                  boolean bold, double scale) {
        if (s == null || s.isEmpty()) {
            return;
        }

        gc.setFont(fontScaled(size, bold, scale));
        gc.setFill(color);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(s, x, y);
    }

    public static void textCentered(GraphicsContext gc, String s, double cx,
                                    double y, Color color, double size,
                                    boolean bold) {
        if (s == null || s.isEmpty()) {
            return;
        }

        gc.setFont(font(size, bold));
        gc.setFill(color);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(s, cx, y);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    public static void textRight(GraphicsContext gc, String s, double rightX,
                                 double y, Color color, double size,
                                 boolean bold) {
        if (s == null || s.isEmpty()) {
            return;
        }

        gc.setFont(font(size, bold));
        gc.setFill(color);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(s, rightX, y);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    /**
     * Draws wrapped text and returns the number of lines drawn. When the
     * text is longer than maxLines the last line ends with an ellipsis.
     */
    public static int paragraph(GraphicsContext gc, String text, double x,
                                double y, double maxWidth, Color color,
                                double size, boolean bold, double lineHeight,
                                int maxLines) {
        return paragraphScaled(gc, text, x, y, maxWidth, color, size, bold,
                lineHeight, maxLines, scale());
    }

    public static int paragraphScaled(GraphicsContext gc, String text,
                                      double x, double y, double maxWidth,
                                      Color color, double size, boolean bold,
                                      double lineHeight, int maxLines,
                                      double scale) {

        List<String> lines = wrap(text, maxWidth, size, bold, scale);
        int count = Math.min(lines.size(), Math.max(0, maxLines));

        for (int i = 0; i < count; i++) {
            String line = lines.get(i);

            if (lines.size() > maxLines && i == count - 1) {
                line = ellipsize(line, maxWidth, size, bold, scale);
            }

            textScaled(gc, line, x, y + i * lineHeight, color, size, bold, scale);
        }

        return count;
    }

    private static String ellipsize(String line, double maxWidth, double size,
                                    boolean bold, double scale) {
        String result = line;

        while (!result.isEmpty()
                && width(result + "...", size, bold, scale) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }

        return result + "...";
    }

    // --------------------------------------------------
    // SHAPES
    // --------------------------------------------------

    public static void panel(GraphicsContext gc, double x, double y, double w,
                             double h, double arc, Color fill, Color border,
                             double borderWidth) {
        gc.setFill(fill);
        gc.fillRoundRect(x, y, w, h, arc, arc);

        if (border != null && borderWidth > 0) {
            gc.setStroke(border);
            gc.setLineWidth(borderWidth);
            gc.strokeRoundRect(x, y, w, h, arc, arc);
        }
    }

    public static void bar(GraphicsContext gc, double x, double y, double w,
                           double h, double fraction, Color fill,
                           Color background, Color border) {
        double f = Math.max(0, Math.min(1, fraction));

        gc.setFill(background);
        gc.fillRoundRect(x, y, w, h, h, h);

        if (f > 0) {
            gc.setFill(fill);
            gc.fillRoundRect(x, y, Math.max(h, w * f), h, h, h);
        }

        if (border != null) {
            gc.setStroke(border);
            gc.setLineWidth(1);
            gc.strokeRoundRect(x, y, w, h, h, h);
        }
    }

    /** Heart icon (filled or outline). */
    public static void heart(GraphicsContext gc, double cx, double cy,
                             double size, boolean filled, Color color,
                             Color outline) {
        double r = size * 0.27;
        double[] xs = {cx - size * 0.52, cx + size * 0.52, cx};
        double[] ys = {cy - size * 0.05, cy - size * 0.05, cy + size * 0.52};

        if (filled) {
            gc.setFill(color);
            gc.fillOval(cx - size * 0.52, cy - size * 0.45, r * 2, r * 2);
            gc.fillOval(cx + size * 0.52 - r * 2, cy - size * 0.45, r * 2, r * 2);
            gc.fillPolygon(xs, ys, 3);
        } else {
            gc.setStroke(outline);
            gc.setLineWidth(1.5);
            gc.strokeOval(cx - size * 0.52, cy - size * 0.45, r * 2, r * 2);
            gc.strokeOval(cx + size * 0.52 - r * 2, cy - size * 0.45, r * 2, r * 2);
            gc.strokePolygon(xs, ys, 3);
        }
    }

    /** Five-pointed star centred on (cx, cy). */
    public static void star(GraphicsContext gc, double cx, double cy,
                            double radius, boolean filled, Color color) {
        double[] xs = new double[10];
        double[] ys = new double[10];

        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double r = i % 2 == 0 ? radius : radius * 0.45;
            xs[i] = cx + Math.cos(angle) * r;
            ys[i] = cy + Math.sin(angle) * r;
        }

        if (filled) {
            gc.setFill(color);
            gc.fillPolygon(xs, ys, 10);
        } else {
            gc.setStroke(color);
            gc.setLineWidth(1.5);
            gc.strokePolygon(xs, ys, 10);
        }
    }

    public static void checkMark(GraphicsContext gc, double cx, double cy,
                                 double size, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(Math.max(2, size * 0.16));
        gc.strokeLine(cx - size * 0.38, cy, cx - size * 0.1, cy + size * 0.3);
        gc.strokeLine(cx - size * 0.1, cy + size * 0.3, cx + size * 0.42, cy - size * 0.32);
    }

    public static void crossMark(GraphicsContext gc, double cx, double cy,
                                 double size, Color color) {
        gc.setStroke(color);
        gc.setLineWidth(Math.max(2, size * 0.16));
        gc.strokeLine(cx - size * 0.32, cy - size * 0.32, cx + size * 0.32, cy + size * 0.32);
        gc.strokeLine(cx - size * 0.32, cy + size * 0.32, cx + size * 0.32, cy - size * 0.32);
    }

    /** Potion flask icon. */
    public static void flask(GraphicsContext gc, double cx, double cy,
                             double size, Color liquid, Color outline) {
        double[] xs = {cx - size * 0.12, cx + size * 0.12, cx + size * 0.42, cx - size * 0.42};
        double[] ys = {cy - size * 0.25, cy - size * 0.25, cy + size * 0.42, cy + size * 0.42};

        gc.setFill(liquid);
        gc.fillPolygon(xs, ys, 4);
        gc.setStroke(outline);
        gc.setLineWidth(1.5);
        gc.strokePolygon(xs, ys, 4);
        gc.strokeLine(cx - size * 0.16, cy - size * 0.42, cx + size * 0.16, cy - size * 0.42);
    }

    /** Key-cap badge such as [E]; returns its width. */
    public static double keyCap(GraphicsContext gc, String key, double x,
                                double y, double size, Palette p) {
        double w = Math.max(size * 1.5, width(key, size, true) + size * 0.7);
        double h = size * 1.5;

        panel(gc, x, y, w, h, 6, p.panelSoft, p.border, 1.5);
        textCentered(gc, key, x + w / 2.0, y + h * 0.72, p.accent, size, true);
        return w;
    }

    /** Small rounded label; returns its width. */
    public static double chip(GraphicsContext gc, String label, double x,
                              double y, double size, Color fill, Color textColor,
                              Color border) {
        double w = width(label, size, true) + size * 1.2;
        double h = size * 1.7;

        panel(gc, x, y, w, h, h, fill, border, border == null ? 0 : 1);
        text(gc, label, x + size * 0.6, y + h * 0.7, textColor, size, true);
        return w;
    }

    /** Button; label centred. */
    public static void button(GraphicsContext gc, UiRect r, String label,
                              double size, boolean hover, boolean enabled,
                              Palette p) {
        Color fill = !enabled ? p.disabledFill : (hover ? p.buttonHover : p.buttonFill);
        Color txt = !enabled ? p.disabledText : p.buttonText;

        panel(gc, r.x, r.y, r.w, r.h, 10, fill,
                hover && enabled ? p.text : p.border, hover && enabled ? 3 : 1.5);

        gc.setFont(font(size, true));
        gc.setFill(txt);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(label, r.centerX(), r.y + r.h / 2.0 + size * scale() * 0.34);
        gc.setTextAlign(TextAlignment.LEFT);
    }
}
