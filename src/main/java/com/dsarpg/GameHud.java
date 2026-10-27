package com.dsarpg;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import com.dsarpg.ai.AdaptiveCoach;
import com.dsarpg.ai.PlayerState;
import com.dsarpg.scenery.MapGenerator;
import com.dsarpg.ui.Palette;
import com.dsarpg.ui.UiKit;
import com.dsarpg.ui.UiRect;
import com.dsarpg.usability.UserSettings;

/**
 * Heads-up display, toasts and the Help / Insights overlays.
 *
 * Usability principles applied:
 *  - visibility of system status (health, XP, objective, minimap, toasts)
 *  - recognition rather than recall (controls always visible, key hints
 *    appear next to the thing they act on)
 *  - transparency of the AI (the Insights screen explains what the coach
 *    believes and why)
 */
final class GameHud {

    private static final class Toast {
        final String text;
        double life;

        Toast(String text, double life) {
            this.text = text;
            this.life = life;
        }
    }

    private final List<Toast> toasts = new ArrayList<>();
    private UiRect closeRect;

    void toast(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // Do not stack identical messages.
        for (Toast t : toasts) {
            if (t.text.equals(text)) {
                t.life = 3.2;
                return;
            }
        }

        toasts.add(new Toast(text, 3.6));

        while (toasts.size() > 4) {
            toasts.remove(0);
        }
    }

    void update(double dt) {
        for (Toast t : toasts) {
            t.life -= dt;
        }

        toasts.removeIf(t -> t.life <= 0);
    }

    boolean isOverClose(double mx, double my) {
        return closeRect != null && closeRect.contains(mx, my);
    }

    // ------------------------------------------------------------------
    // EXPLORATION HUD
    // ------------------------------------------------------------------

    void render(GraphicsContext gc, GameWorld world, double w, double h) {
        Palette p = Palette.get();
        double s = UiKit.scale();
        BattleManager b = world.battle();

        // ---- Status panel (top left)
        double px = 12;
        double py = 12;
        double pw = Math.max(250, 240 * s);
        double rowH = 24 * s;
        double ph = 14 + rowH * 3.5 + 8;

        UiKit.panel(gc, px, py, pw, ph, 12, p.panel, p.border, 1.5);

        UiKit.text(gc, world.theme().getTitle(), px + 12, py + 22 * s, p.accent, 14, true);

        double heartSize = 15 * s;

        for (int i = 0; i < BattleManager.MAX_HEALTH; i++) {
            UiKit.heart(gc, px + 20 + i * (heartSize + 5), py + rowH + 20 * s, heartSize,
                    i < b.getPlayerHealth(), p.bad, p.text);
        }

        UiKit.text(gc, "HP " + b.getPlayerHealth() + "/" + BattleManager.MAX_HEALTH,
                px + 20 + BattleManager.MAX_HEALTH * (heartSize + 5) + 4,
                py + rowH + 24 * s, p.text, 13, true);

        double barW = pw - 24;
        double barY = py + rowH * 2 + 14 * s;
        UiKit.bar(gc, px + 12, barY, barW, 10 * s,
                b.getXpIntoLevel() / (double) BattleManager.XP_PER_LEVEL,
                p.info, p.barBackground, p.border);
        UiKit.text(gc, "Level " + b.getPlayerLevel() + "   " + b.getXpIntoLevel() + "/"
                + BattleManager.XP_PER_LEVEL + " XP", px + 12, barY + 26 * s, p.text, 12, true);

        UiKit.flask(gc, px + pw - 86 * s, barY + 22 * s, 16 * s, p.good, p.text);
        UiKit.text(gc, "x" + b.getHealingPotions(), px + pw - 70 * s, barY + 26 * s, p.text, 12, true);

        // ---- Objective (under the status panel)
        double oy = py + ph + 8;
        String objective = world.objectiveText();
        List<String> oLines = UiKit.wrap(objective, pw - 40, 13, true);
        int oShown = Math.min(2, oLines.size());
        double oh = oShown * 18 * s + 14;

        UiKit.panel(gc, px, oy, pw, oh, 10, p.panel, p.accent, 1.5);
        UiKit.star(gc, px + 16, oy + oh / 2.0, 7 * s, true, p.accent);
        UiKit.paragraph(gc, objective, px + 30, oy + 18 * s, pw - 42, p.text, 13, true, 18 * s, 2);

        // ---- Minimap (top right)
        if (UserSettings.get().isShowMinimap()) {
            renderMinimap(gc, world, w, p);
        }

        // ---- Direction arrow to the objective
        renderObjectiveArrow(gc, world, w, h, p);

        // ---- Interaction prompt next to the thing the player can use
        renderPrompt(gc, world, p);

        // ---- Dialogue
        String dialogue = world.dialogue();
        double bottomReserved = 12;

        // ---- Controls bar (bottom)
        String controls = "Move: WASD / Arrows     E: Interact     P: Potion     M: Insights     "
                + "H: Help     F5: Save     Esc: Pause";
        List<String> cLines = UiKit.wrap(controls, w - 48, 12, false);
        int cShown = Math.min(2, cLines.size());
        double ch = cShown * 17 * s + 12;
        double cy = h - ch - 10;

        UiKit.panel(gc, 12, cy, w - 24, ch, 10, Color.color(
                p.panel.getRed(), p.panel.getGreen(), p.panel.getBlue(), 0.80), null, 0);
        UiKit.paragraph(gc, controls, 24, cy + 17 * s, w - 48, p.textDim, 12, false, 17 * s, 2);
        bottomReserved = ch + 20;

        if (dialogue != null && !dialogue.isEmpty()) {
            double dw = Math.min(w - 120, 760);
            List<String> dl = UiKit.wrap(dialogue, dw - 32, 15, false);
            int shown = Math.min(6, dl.size());
            double dh = shown * 21 * s + 40 * s;
            double dx = (w - dw) / 2.0;
            double dy = h - bottomReserved - dh - 4;

            UiKit.panel(gc, dx, dy, dw, dh, 14, p.panel, p.accent, 2);
            UiKit.paragraph(gc, dialogue, dx + 16, dy + 26 * s, dw - 32, p.text, 15, false, 21 * s, 6);
            UiKit.textRight(gc, "Space: close", dx + dw - 14, dy + dh - 10, p.textDim, 11, false);
        }

    }

    /** Toasts are drawn last so they stay visible above every overlay. */
    void renderToasts(GraphicsContext gc, double w) {
        renderToasts(gc, w, Palette.get(), UiKit.scale());
    }

    private void renderMinimap(GraphicsContext gc, GameWorld world, double w, Palette p) {
        double mw = 150;
        double mh = 100;
        double mx = w - mw - 12;
        double my = 12;
        double sx = mw / (MapGenerator.COLS * MapGenerator.TILE);
        double sy = mh / (MapGenerator.ROWS * MapGenerator.TILE);

        UiKit.panel(gc, mx - 4, my - 4, mw + 8, mh + 8 + 18, 10, p.panel, p.border, 1.5);

        gc.setImageSmoothing(false);
        gc.drawImage(world.minimap(), 0, 0, MapGenerator.COLS, MapGenerator.ROWS, mx, my, mw, mh);
        gc.setImageSmoothing(true);

        for (TreasureChest c : world.chests()) {
            if (!c.isOpened()) {
                gc.setFill(Color.GOLD);
                gc.fillRect(mx + c.getX() * sx - 1.5, my + c.getY() * sy - 1.5, 4, 4);
            }
        }

        ForestGuardian g = world.guardian();
        gc.setFill(p.info);
        gc.fillOval(mx + g.getX() * sx - 2, my + g.getY() * sy - 2, 5, 5);

        for (Enemy e : world.enemies()) {
            gc.setFill(p.bad);

            if (e.isBoss()) {
                gc.fillRect(mx + e.getX() * sx - 3, my + e.getY() * sy - 3, 7, 7);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1);
                gc.strokeRect(mx + e.getX() * sx - 3, my + e.getY() * sy - 3, 7, 7);
            } else {
                gc.fillOval(mx + e.getX() * sx - 2, my + e.getY() * sy - 2, 5, 5);
            }
        }

        // View rectangle and player.
        gc.setStroke(Color.color(1, 1, 1, 0.8));
        gc.setLineWidth(1);
        gc.strokeRect(mx + world.camX() * sx, my + world.camY() * sy,
                GameWorld.VIEW_WIDTH * sx, GameWorld.VIEW_HEIGHT * sy);

        gc.setFill(Color.WHITE);
        gc.fillOval(mx + world.playerCx() * sx - 3, my + world.playerCy() * sy - 3, 7, 7);
        gc.setStroke(Color.BLACK);
        gc.strokeOval(mx + world.playerCx() * sx - 3, my + world.playerCy() * sy - 3, 7, 7);

        UiKit.text(gc, "Map", mx + 2, my + mh + 15, p.textDim, 11, true);
        UiKit.textRight(gc, "you = white", mx + mw - 2, my + mh + 15, p.textDim, 11, false);
    }

    private void renderObjectiveArrow(GraphicsContext gc, GameWorld world,
                                      double w, double h, Palette p) {
        double[] target = world.objectiveTarget();

        if (target == null) {
            return;
        }

        double sx = target[0] - world.camX();
        double sy = target[1] - world.camY();
        double margin = 46;

        // Only needed when the target is off screen.
        if (sx > margin && sx < w - margin && sy > margin && sy < h - margin) {
            return;
        }

        double cx = w / 2.0;
        double cy = h / 2.0;
        double dx = sx - cx;
        double dy = sy - cy;
        double len = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        dx /= len;
        dy /= len;

        // Intersect the ray with an inset rectangle.
        double tx = dx == 0 ? Double.MAX_VALUE : ((dx > 0 ? w - margin : margin) - cx) / dx;
        double ty = dy == 0 ? Double.MAX_VALUE : ((dy > 0 ? h - margin - 40 : margin + 60) - cy) / dy;
        double t = Math.min(tx, ty);
        double ax = cx + dx * t;
        double ay = cy + dy * t;

        double size = 14;
        double nx = -dy;
        double ny = dx;

        gc.setFill(Color.color(0, 0, 0, 0.55));
        gc.fillOval(ax - 22, ay - 22, 44, 44);
        gc.setFill(p.accent);
        gc.fillPolygon(
                new double[] {ax + dx * size, ax - dx * size * 0.6 + nx * size * 0.8,
                        ax - dx * size * 0.6 - nx * size * 0.8},
                new double[] {ay + dy * size, ay - dy * size * 0.6 + ny * size * 0.8,
                        ay - dy * size * 0.6 - ny * size * 0.8}, 3);
    }

    private void renderPrompt(GraphicsContext gc, GameWorld world, Palette p) {
        String prompt = world.promptText();

        if (prompt == null) {
            return;
        }

        double[] pos = world.promptPos();
        double size = 13;
        double textW = UiKit.width(prompt, size, true);
        double capW = Math.max(size * 1.5, UiKit.width("E", size, true) + size * 0.7);
        double total = capW + 8 + textW + 18;
        double left = pos[0] - world.camX() - total / 2.0;
        double top = pos[1] - world.camY() - 10;
        double height = size * 1.5 + 8;

        UiKit.panel(gc, left, top - 4, total, height, 10, p.panel, p.border, 1.5);
        UiKit.keyCap(gc, "E", left + 6, top, size, p);
        UiKit.text(gc, prompt, left + capW + 14, top + size * 1.5 * 0.72, p.text, size, true);
    }

    private void renderToasts(GraphicsContext gc, double w, Palette p, double s) {
        double y = 16;

        for (Toast t : toasts) {
            double alpha = Math.min(1.0, t.life / 0.6);
            double tw = UiKit.width(t.text, 14, true) + 40;
            double th = 32 * s;
            double x = (w - tw) / 2.0;

            gc.setGlobalAlpha(alpha);
            UiKit.panel(gc, x, y, tw, th, th, p.panel, p.accent, 2);
            UiKit.checkMark(gc, x + 18, y + th / 2.0, 12 * s, p.good);
            UiKit.text(gc, t.text, x + 32, y + th * 0.66, p.text, 14, true);
            gc.setGlobalAlpha(1.0);

            y += th + 6;
        }
    }

    // ------------------------------------------------------------------
    // HELP OVERLAY
    // ------------------------------------------------------------------

    void renderHelp(GraphicsContext gc, double w, double h) {
        Palette p = Palette.get();
        double s = UiKit.scale();

        gc.setFill(p.dim);
        gc.fillRect(0, 0, w, h);

        double pw = Math.min(w - 60, 820);
        double ph = h - 50;
        double px = (w - pw) / 2.0;
        double py = 25;

        UiKit.panel(gc, px, py, pw, ph, 18, p.panel, p.border, 2);
        UiKit.text(gc, "How to play", px + 28, py + 40, p.accent, 26, true);

        double y = py + 66;
        double col = px + 28;

        y = helpSection(gc, p, "Exploring", col, y, pw - 56, new String[][] {
                {"WASD", "Move (arrow keys also work)"},
                {"E", "Open chests and talk to the guardian"},
                {"M", "Open Learning Insights"},
                {"Esc", "Pause menu (save, settings, session report)"}});

        y = helpSection(gc, p, "Battles", col, y + 6, pw - 56, new String[][] {
                {"1-4", "Pick an answer, or just click it"},
                {"H", "Show a hint (it is always safe to use)"},
                {"P", "Drink a healing potion"},
                {"Enter", "Continue after reading the explanation"}});

        UiKit.text(gc, "Your AI Coach", col, y + 28 * s, p.info, 16, true);
        y += 28 * s;

        int lines = UiKit.paragraph(gc,
                "The game learns how you play. If you struggle it offers help (50/50, a shield, "
                + "a hint). When things are easy it gives harder questions and bonus XP. "
                + "Open Learning Insights to see what it knows and why it chose a question. "
                + "You can switch the AI Coach off in Settings.",
                col, y + 22 * s, pw - 56, p.text, 14, false, 20 * s, 6);
        y += 22 * s + lines * 20 * s + 14;

        UiKit.text(gc, "Comfort and accessibility", col, y + 20 * s, p.info, 16, true);
        UiKit.paragraph(gc,
                "Settings has text size, high contrast, a colour-blind friendly palette and reduced "
                + "motion. Everything can be done with the keyboard or the mouse.",
                col, y + 42 * s, pw - 56, p.text, 14, false, 20 * s, 3);

        closeRect = new UiRect(px + pw - 190, py + ph - 56, 160, 40);
        UiKit.button(gc, closeRect, "Close (H / Esc)", 14, false, true, p);
    }

    private double helpSection(GraphicsContext gc, Palette p, String title,
                               double x, double y, double width, String[][] rows) {
        double s = UiKit.scale();

        UiKit.text(gc, title, x, y + 20 * s, p.info, 16, true);
        double yy = y + 30 * s;

        for (String[] row : rows) {
            double cap = UiKit.keyCap(gc, row[0], x, yy, 13, p);
            UiKit.text(gc, row[1], x + Math.max(70 * s, cap + 14), yy + 19 * s * 0.95 + 1,
                    p.text, 14, false);
            yy += 27 * s;
        }

        return yy;
    }

    // ------------------------------------------------------------------
    // INSIGHTS OVERLAY
    // ------------------------------------------------------------------

    void renderInsights(GraphicsContext gc, GameWorld world, double w, double h) {
        Palette p = Palette.get();
        double s = UiKit.scale();
        BattleManager b = world.battle();
        AdaptiveCoach coach = b.getCoach();

        gc.setFill(p.dim);
        gc.fillRect(0, 0, w, h);

        double pw = Math.min(w - 40, 900);
        double ph = h - 40;
        double px = (w - pw) / 2.0;
        double py = 20;

        UiKit.panel(gc, px, py, pw, ph, 18, p.panel, p.border, 2);
        UiKit.text(gc, "Learning Insights", px + 28, py + 38, p.accent, 26, true);
        UiKit.text(gc, "What the game has learned about you in " + world.theme().getTitle().toLowerCase(Locale.ROOT)
                .replace("the ", ""), px + 28, py + 60 * s, p.textDim, 13, false);

        double leftW = (pw - 84) * 0.52;
        double x = px + 28;
        double y = py + 84 * s;

        // ---- Topic mastery
        UiKit.text(gc, "Topic mastery", x, y + 14, p.info, 16, true);
        y += 26 * s;

        List<AdaptiveCoach.TopicInsight> topics = coach.insights(
                b.getEngine().getLevelTopics(), b.getEngine());

        String weakest = null;
        double weakestValue = 2;

        for (AdaptiveCoach.TopicInsight t : topics) {
            int percent = (int) Math.round(t.mastery * 100);
            String word = t.mastery >= 0.8 ? "Strong" : (t.mastery >= 0.5 ? "Growing" : "Needs practice");

            UiKit.text(gc, t.topic, x, y + 14, p.text, 14, true);
            UiKit.textRight(gc, percent + "%  " + word, x + leftW, y + 14, p.text, 13, true);
            UiKit.bar(gc, x, y + 22 * s, leftW, 12 * s, t.mastery,
                    t.mastery >= 0.5 ? p.good : p.accent, p.barBackground, p.border);

            String detail = t.attempts == 0 ? "Not attempted yet"
                    : t.correct + " of " + t.attempts + " correct this session";
            UiKit.text(gc, detail, x, y + 48 * s, p.textDim, 12, false);

            if (t.mastery < weakestValue) {
                weakestValue = t.mastery;
                weakest = t.topic;
            }

            y += 62 * s;
        }

        double readiness = coach.readiness(b.getEngine().getLevelTopics());
        UiKit.text(gc, "Region readiness: " + Math.round(readiness * 100) + "%", x, y + 10, p.accent, 15, true);

        if (weakest != null) {
            UiKit.paragraph(gc, "Suggestion: practise " + weakest + " next.", x, y + 32 * s,
                    leftW, p.text, 13, false, 18 * s, 2);
        }

        // ---- AI Coach panel
        double rx = x + leftW + 28;
        double rw = pw - 56 - leftW - 28;
        double ry = py + 84 * s;

        UiKit.panel(gc, rx, ry, rw, ph - 84 * s - 76, 12, p.panelSoft, p.border, 1);
        double cx = rx + 14;
        double cy = ry + 28 * s;
        double textW = rw - 28;

        boolean on = coach.isAiEnabled();
        UiKit.text(gc, "AI Coach: " + (on ? "ON" : "OFF"), cx, cy, on ? p.info : p.textDim, 16, true);
        cy += 26 * s;

        if (!on) {
            int n = UiKit.paragraph(gc, "Questions use the original rule-based difficulty. "
                    + "Switch the AI Coach on in Settings to get personal adaptation.",
                    cx, cy, textW, p.text, 13, false, 18 * s, 4);
            cy += n * 18 * s + 10;
        } else {
            PlayerState state = coach.getState();
            UiKit.text(gc, "Right now: " + state.getLabel(), cx, cy, p.text, 14, true);
            cy += 20 * s;

            int n = UiKit.paragraph(gc, state.getDescription(), cx, cy, textW, p.textDim, 12, false, 17 * s, 3);
            cy += n * 17 * s + 8;

            UiKit.text(gc, "Focus", cx, cy + 10, p.text, 12, true);
            UiKit.bar(gc, cx + 60 * s, cy, textW - 60 * s, 11 * s, coach.getFlowScore(),
                    p.info, p.barBackground, p.border);
            cy += 24 * s;

            UiKit.text(gc, "Frustration " + Math.round(coach.getFrustration() * 100) + "%", cx, cy + 10, p.textDim, 12, false);
            UiKit.textRight(gc, "Boredom " + Math.round(coach.getBoredom() * 100) + "%", cx + textW, cy + 10, p.textDim, 12, false);
            cy += 26 * s;

            AdaptiveCoach.QuestionPlan plan = coach.getLastPlan();

            if (plan != null) {
                UiKit.text(gc, "Why this question?", cx, cy, p.info, 14, true);
                cy += 20 * s;
                int m = UiKit.paragraph(gc, plan.reason, cx, cy, textW, p.text, 12, false, 17 * s, 4);
                cy += m * 17 * s + 10;
            }

            String learned = coach.learnedPreference(state);
            UiKit.text(gc, "What helps you", cx, cy, p.info, 14, true);
            cy += 20 * s;
            UiKit.paragraph(gc, learned == null
                    ? "Still learning what works for you."
                    : "In this state, " + learned + " works best so far.",
                    cx, cy, textW, p.text, 12, false, 17 * s, 3);
        }

        // ---- Session line
        double sy = py + ph - 62;
        UiKit.text(gc, "This session: " + coach.getSessionCorrect() + " of "
                + coach.getSessionAnswers() + " answers correct", x, sy + 10, p.text, 13, true);

        closeRect = new UiRect(px + pw - 190, py + ph - 56, 160, 40);
        UiKit.button(gc, closeRect, "Close (M / Esc)", 14, false, true, p);
    }
}
