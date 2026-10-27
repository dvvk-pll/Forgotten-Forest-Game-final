package com.dsarpg;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import com.dsarpg.ai.AdaptiveCoach.AnswerResult;
import com.dsarpg.ai.AdaptiveCoach.QuestionPlan;
import com.dsarpg.ai.Intervention;
import com.dsarpg.ai.PlayerState;
import com.dsarpg.ui.Palette;
import com.dsarpg.ui.UiKit;
import com.dsarpg.ui.UiRect;

/**
 * Draws the battle screen and remembers where its buttons are so the mouse
 * can use them.
 *
 * Layout adapts to the player's text size: the content is measured first and
 * the text scale is reduced slightly if it would not fit on screen, so
 * nothing is ever cut off.
 */
final class BattleView {

    private final List<UiRect> optionRects = new ArrayList<>();
    private UiRect hintRect;
    private UiRect continueRect;
    private UiRect potionRect;
    private UiRect retryRect;
    private double mouseX = -1;
    private double mouseY = -1;

    // --------------------------------------------------
    // HIT TESTING
    // --------------------------------------------------

    void setHover(double mx, double my) {
        mouseX = mx;
        mouseY = my;
    }

    int optionAt(double mx, double my) {
        for (int i = 0; i < optionRects.size(); i++) {
            if (optionRects.get(i).contains(mx, my)) {
                return i;
            }
        }

        return -1;
    }

    boolean isOverHint(double mx, double my) {
        return hintRect != null && hintRect.contains(mx, my);
    }

    boolean isOverContinue(double mx, double my) {
        return continueRect != null && continueRect.contains(mx, my);
    }

    boolean isOverPotion(double mx, double my) {
        return potionRect != null && potionRect.contains(mx, my);
    }

    boolean isOverAction(double mx, double my, boolean answered) {
        if (answered) {
            return isOverContinue(mx, my) || isOverPotion(mx, my);
        }

        return isOverHint(mx, my) || isOverPotion(mx, my);
    }

    boolean isOverGameOverButton(double mx, double my) {
        return retryRect != null && retryRect.contains(mx, my);
    }

    private boolean hover(UiRect r) {
        return r != null && r.contains(mouseX, mouseY);
    }

    // --------------------------------------------------
    // BATTLE SCREEN
    // --------------------------------------------------

    void render(GraphicsContext gc, double w, double h, BattleManager b) {
        Palette p = Palette.get();

        gc.setFill(p.dim);
        gc.fillRect(0, 0, w, h);

        double scale = UiKit.scale();
        double limit = h - 24;

        // Measure with smaller and smaller text until everything fits.
        for (int i = 0; i < 8; i++) {
            double bottom = layout(gc, w, h, b, p, scale, false);

            if (bottom <= limit) {
                break;
            }

            scale *= 0.93;
        }

        layout(gc, w, h, b, p, scale, true);
    }

    /**
     * Lays out (and optionally draws) the battle panel.
     * Returns the y coordinate at which the content ends.
     */
    private double layout(GraphicsContext gc, double w, double h,
                          BattleManager b, Palette p, double s, boolean draw) {

        Enemy enemy = b.getEnemy();
        QuestionPlan plan = b.getPlan();
        String[] options = plan.question.getOptions();
        boolean answered = b.isAnswered();

        double px = 24;
        double py = 10;
        double pw = w - 48;
        double x0 = px + 24;
        double innerW = pw - 48;
        double rightX = x0 + innerW;

        if (draw) {
            optionRects.clear();
            hintRect = null;
            continueRect = null;
            potionRect = null;
            UiKit.panel(gc, px, py, pw, h - 20, 18, p.panel, p.border, 2);
        }

        double y = py + 16;

        // ---- Header: enemy name + HP pips (left), AI state + hearts (right)
        if (draw) {
            UiKit.textScaled(gc, "DSA BATTLE", x0, y + 10 * s, p.textDim, 11, true, s);
            UiKit.textScaled(gc, enemy.getName(), x0, y + 32 * s, p.accent, 22, true, s);

            double pipX = x0;
            double pipY = y + 40 * s;
            double pipW = 22 * s;
            double pipH = 9 * s;

            for (int i = 0; i < enemy.getMaxHealth(); i++) {
                boolean alive = i < enemy.getHealth();
                gc.setFill(alive ? p.bad : p.barBackground);
                gc.fillRoundRect(pipX + i * (pipW + 3), pipY, pipW, pipH, 4, 4);
            }

            UiKit.textScaled(gc, "Enemy HP " + enemy.getHealth() + "/" + enemy.getMaxHealth(),
                    pipX + enemy.getMaxHealth() * (pipW + 3) + 8, pipY + pipH, p.textDim, 12, false, s);

            drawFocusMeter(gc, b, p, rightX, y, s);
            drawHearts(gc, b, p, rightX, y + 40 * s, s);
        }

        y += 60 * s;

        // ---- Chips: topic and difficulty
        if (draw) {
            double cx = x0;
            double chipSize = 13 * s;
            cx += UiKit.chip(gc, plan.question.getTopic(), cx, y, 13 * s, p.panelSoft, p.info, p.info) + 8;

            double chipW = UiKit.width("Difficulty", 13, true, s) + 13 * s * 1.2 + 70 * s;
            double chipH = chipSize * 1.7;
            UiKit.panel(gc, cx, y, chipW, chipH, chipH, p.panelSoft, p.accent, 1);
            UiKit.textScaled(gc, "Difficulty", cx + chipSize * 0.6, y + chipH * 0.7, p.accent, 13, true, s);

            double starX = cx + chipSize * 0.6 + UiKit.width("Difficulty", 13, true, s) + 14 * s;

            for (int i = 0; i < 3; i++) {
                UiKit.star(gc, starX + i * 18 * s, y + chipH / 2.0, 7 * s,
                        i < plan.question.getDifficulty(), p.accent);
            }
        }

        y += 13 * s * 1.7 + 8;

        // ---- Coach banner
        String message = plan.coachMessage == null ? "" : plan.coachMessage;

        if (!message.isEmpty()) {
            String label = plan.intervention == Intervention.NONE ? "AI" : plan.intervention.getLabel();
            double labelW = UiKit.width(label, 12, true, s) + 18;
            List<String> lines = UiKit.wrap(message, innerW - labelW - 36, 14, false, s);
            int shown = Math.min(2, lines.size());
            double bannerH = Math.max(30 * s, shown * 18 * s + 12);

            if (draw) {
                boolean helped = plan.intervention != Intervention.NONE;
                UiKit.panel(gc, x0, y, innerW, bannerH, 10, p.panelSoft,
                        helped ? p.accent : p.border, helped ? 2 : 1);

                double badgeH = 20 * s;
                UiKit.panel(gc, x0 + 8, y + (bannerH - badgeH) / 2.0, labelW, badgeH, badgeH,
                        helped ? p.accent : p.info, null, 0);
                UiKit.textScaled(gc, label, x0 + 8 + 9, y + (bannerH - badgeH) / 2.0 + badgeH * 0.72,
                        p.buttonText, 12, true, s);

                UiKit.paragraphScaled(gc, message, x0 + labelW + 24, y + 13 * s + 4,
                        innerW - labelW - 36, p.text, 14, false, 18 * s, 2, s);
            }

            y += bannerH + 10;
        }

        // ---- Question
        List<String> qLines = UiKit.wrap(plan.question.getText(), innerW, 19, true, s);
        int qShown = Math.min(4, qLines.size());

        if (draw) {
            UiKit.paragraphScaled(gc, plan.question.getText(), x0, y + 19 * s, innerW,
                    p.text, 19, true, 25 * s, 4, s);
        }

        y += qShown * 25 * s + 12;

        // ---- Options
        for (int i = 0; i < options.length; i++) {
            List<String> oLines = UiKit.wrap(options[i], innerW - 110, 16, false, s);
            int oShown = Math.min(2, oLines.size());
            double oh = Math.max(42 * s, oShown * 21 * s + 16);

            UiRect rect = new UiRect(x0, y, innerW, oh);

            if (draw) {
                optionRects.add(rect);
                drawOption(gc, b, p, plan, i, options[i], rect, oShown, s);
            }

            y += oh + 8;
        }

        // ---- Bottom area: hint / feedback and buttons
        double buttonH = 38 * s;

        if (!answered) {
            if (b.isHintShown()) {
                String hint = (b.isHintByCoach() ? "Hint (opened by AI Coach): " : "Hint: ")
                        + (plan.question.getHint() == null ? "" : plan.question.getHint());
                List<String> hLines = UiKit.wrap(hint, innerW - 24, 15, false, s);
                int hShown = Math.min(2, hLines.size());
                double hh = hShown * 20 * s + 14;

                if (draw) {
                    UiKit.panel(gc, x0, y, innerW, hh, 10, p.panelSoft, p.accent, 1.5);
                    UiKit.paragraphScaled(gc, hint, x0 + 12, y + 20 * s, innerW - 24,
                            p.accent, 15, false, 20 * s, 2, s);
                }

                y += hh + 8;
            }

            if (draw) {
                String hintLabel = b.isHintShown() ? "Hint shown" : "Hint (H)";
                hintRect = new UiRect(x0, y, 140 * s, buttonH);
                drawButton(gc, p, hintRect, hintLabel, !b.isHintShown(), s);

                drawPotionButton(gc, b, p, x0 + 152 * s, y, buttonH, s);

                UiKit.textRight(gc, "Click an answer or press 1-4", rightX,
                        y + buttonH * 0.62, p.textDim, 13 * s, false);
            }

            y += buttonH;
        } else {
            AnswerResult result = b.getResult();

            String explanation = plan.question.getExplanation() == null
                    ? "" : plan.question.getExplanation();
            String headline = b.isLastAnswerCorrect() ? "Correct!" : "Not quite.";
            String effect;

            if (b.isLastAnswerCorrect()) {
                effect = "You dealt 1 damage and earned " + b.getXpGained() + " XP"
                        + (result != null && result.xpMultiplier > 1 ? " (double XP bonus)." : ".");
            } else if (!b.getFeedbackNote().isEmpty()) {
                effect = b.getFeedbackNote();
            } else {
                effect = "You lost 1 health.";
            }

            List<String> eLines = UiKit.wrap(explanation, innerW - 24, 15, false, s);
            int eShown = Math.min(3, eLines.size());
            double fh = 26 * s + eShown * 20 * s + 26 * s + 10;

            if (draw) {
                Color tone = b.isLastAnswerCorrect() ? p.good : p.bad;
                UiKit.panel(gc, x0, y, innerW, fh, 10, p.panelSoft, tone, 2);

                double iconCx = x0 + 22;
                double iconCy = y + 18 * s;

                if (b.isLastAnswerCorrect()) {
                    UiKit.checkMark(gc, iconCx, iconCy, 18 * s, tone);
                } else {
                    UiKit.crossMark(gc, iconCx, iconCy, 18 * s, tone);
                }

                UiKit.textScaled(gc, headline + "  " + effect, x0 + 44, y + 23 * s, tone, 15, true, s);
                UiKit.paragraphScaled(gc, explanation, x0 + 12, y + 26 * s + 20 * s, innerW - 24,
                        p.text, 15, false, 20 * s, 3, s);

                if (result != null) {
                    int before = (int) Math.round(result.masteryBefore * 100);
                    int after = (int) Math.round(result.masteryAfter * 100);
                    String line = String.format(Locale.ROOT, "%s mastery: %d%% → %d%%",
                            plan.question.getTopic(), before, after);
                    UiKit.textScaled(gc, line, x0 + 12, y + fh - 10 * s, p.info, 13, true, s);
                }
            }

            y += fh + 8;

            if (draw) {
                continueRect = new UiRect(rightX - 190 * s, y, 190 * s, buttonH);
                drawButton(gc, p, continueRect,
                        b.getEnemy() != null && b.getEnemy().isAlive() ? "Next question (Enter)" : "Finish (Enter)",
                        true, s);

                drawPotionButton(gc, b, p, x0, y, buttonH, s);
            }

            y += buttonH;
        }

        return y + 14;
    }

    private void drawPotionButton(GraphicsContext gc, BattleManager b, Palette p,
                                  double x, double y, double h, double s) {
        if (b.getHealingPotions() <= 0) {
            potionRect = null;
            return;
        }

        potionRect = new UiRect(x, y, 170 * s, h);
        drawButton(gc, p, potionRect, "Potion x" + b.getHealingPotions() + " (P)",
                b.canUsePotion(), s);
    }

    private void drawButton(GraphicsContext gc, Palette p, UiRect r, String label,
                            boolean enabled, double s) {
        boolean over = enabled && hover(r);
        Color fill = !enabled ? p.disabledFill : (over ? p.buttonHover : p.buttonFill);
        Color txt = !enabled ? p.disabledText : p.buttonText;

        UiKit.panel(gc, r.x, r.y, r.w, r.h, 10, fill, over ? p.text : p.border, over ? 3 : 1.5);

        gc.setFont(UiKit.fontScaled(14, true, s));
        gc.setFill(txt);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(label, r.centerX(), r.y + r.h / 2.0 + 14 * s * 0.34);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawOption(GraphicsContext gc, BattleManager b, Palette p,
                            QuestionPlan plan, int index, String text,
                            UiRect r, int shownLines, double s) {

        boolean answered = b.isAnswered();
        boolean removed = plan.isOptionRemoved(index);
        boolean correct = index == plan.question.getCorrectAnswer();
        boolean selected = index == b.getSelectedIndex();
        boolean over = !answered && !removed && hover(r);

        Color border = p.border;
        double borderWidth = 1;
        Color textColor = p.text;
        Color fill = p.optionFill;

        if (removed) {
            fill = p.barBackground;
            textColor = p.textDim;
        } else if (over) {
            fill = p.optionHover;
            border = p.accent;
            borderWidth = 3;
        }

        UiKit.panel(gc, r.x, r.y, r.w, r.h, 12, fill, border, borderWidth);

        if (answered) {
            if (correct) {
                gc.setFill(Color.color(p.good.getRed(), p.good.getGreen(), p.good.getBlue(), 0.28));
                gc.fillRoundRect(r.x, r.y, r.w, r.h, 12, 12);
                UiKit.panel(gc, r.x, r.y, r.w, r.h, 12, Color.TRANSPARENT, p.good, 3);
            } else if (selected) {
                gc.setFill(Color.color(p.bad.getRed(), p.bad.getGreen(), p.bad.getBlue(), 0.28));
                gc.fillRoundRect(r.x, r.y, r.w, r.h, 12, 12);
                UiKit.panel(gc, r.x, r.y, r.w, r.h, 12, Color.TRANSPARENT, p.bad, 3);
            } else {
                textColor = p.textDim;
            }
        }

        // Number badge.
        double badge = 26 * s;
        double bx = r.x + 12;
        double by = r.y + (r.h - badge) / 2.0;
        gc.setFill(removed ? p.disabledFill : p.accent);
        gc.fillOval(bx, by, badge, badge);
        UiKit.textCentered(gc, Integer.toString(index + 1), bx + badge / 2.0,
                by + badge * 0.7, p.buttonText, 15 * s / UiKit.scale(), true);

        UiKit.paragraphScaled(gc, text, r.x + 52, r.y + r.h / 2.0 - (shownLines - 1) * 10.5 * s + 5.5 * s,
                r.w - 110, textColor, 16, false, 21 * s, 2, s);

        // Icons and words (never colour alone).
        double iconX = r.x + r.w - 30;
        double iconY = r.y + r.h / 2.0;

        if (removed) {
            UiKit.textRight(gc, "removed", r.x + r.w - 14, iconY + 5, p.textDim, 12 * s, false);
        } else if (answered && correct) {
            UiKit.checkMark(gc, iconX, iconY, 20 * s, p.good);
        } else if (answered && selected) {
            UiKit.crossMark(gc, iconX, iconY, 20 * s, p.bad);
        }
    }

    private void drawFocusMeter(GraphicsContext gc, BattleManager b, Palette p,
                                double rightX, double y, double s) {

        boolean on = b.getCoach().isAiEnabled();
        PlayerState state = b.getCoach().getState();
        String text = on ? "AI Coach: " + state.getLabel() : "Standard mode";
        double size = 13;

        double w = UiKit.width(text, size, true, s) + 24;
        double x = rightX - w;

        UiKit.panel(gc, x, y, w, 22 * s, 22 * s, p.panelSoft, on ? p.info : p.border, 1.5);
        UiKit.textScaled(gc, text, x + 12, y + 16 * s, on ? p.info : p.textDim, size, true, s);

        if (on) {
            UiKit.bar(gc, x, y + 26 * s, w, 6 * s, b.getCoach().getFlowScore(),
                    p.info, p.barBackground, null);
        }
    }

    private void drawHearts(GraphicsContext gc, BattleManager b, Palette p,
                            double rightX, double y, double s) {

        double size = 18 * s;
        double gap = 6;
        double total = BattleManager.MAX_HEALTH * (size + gap);
        double labelW = UiKit.width("HP " + b.getPlayerHealth() + "/" + BattleManager.MAX_HEALTH,
                13, true, s);
        double startX = rightX - total - labelW - 6;

        for (int i = 0; i < BattleManager.MAX_HEALTH; i++) {
            UiKit.heart(gc, startX + i * (size + gap) + size / 2.0, y + size * 0.55, size,
                    i < b.getPlayerHealth(), p.bad, p.text);
        }

        UiKit.textScaled(gc, "HP " + b.getPlayerHealth() + "/" + BattleManager.MAX_HEALTH,
                rightX - labelW, y + size * 0.85, p.text, 13, true, s);
    }

    // --------------------------------------------------
    // GAME OVER
    // --------------------------------------------------

    void renderGameOver(GraphicsContext gc, double w, double h, BattleManager b) {
        Palette p = Palette.get();
        double s = UiKit.scale();

        gc.setFill(p.dim);
        gc.fillRect(0, 0, w, h);

        double pw = Math.min(w - 80, 640);
        double ph = 330 * Math.min(1.25, s);
        double px = (w - pw) / 2.0;
        double py = (h - ph) / 2.0;

        UiKit.panel(gc, px, py, pw, ph, 18, p.panel, p.bad, 3);

        UiKit.crossMark(gc, px + 44, py + 46, 34, p.bad);
        UiKit.text(gc, "You were defeated", px + 76, py + 56, p.bad, 26, true);

        String note = b.getCoach().isAiEnabled()
                ? "Everything you answered is remembered. The AI Coach will use it to "
                + "make the next attempt a better fit for you."
                : "Your answers are remembered. Try again and use hints when you are stuck.";

        int lines = UiKit.paragraph(gc, note, px + 32, py + 100, pw - 64, p.text, 16, false, 22, 4);

        UiKit.text(gc, "Tip: open the hint (H) before guessing. Press M to see which topics to review.",
                px + 32, py + 100 + lines * 22 + 16, p.textDim, 13, false);

        retryRect = new UiRect(px + pw / 2.0 - 130, py + ph - 70 * Math.min(1.25, s), 260,
                46 * Math.min(1.25, s));
        UiKit.button(gc, retryRect, "Try again (R)", 17, hover(retryRect), true, p);
    }
}
