# What changed

**Sceneries** - five regions (Forest, Ruins, Frozen Peaks, Crystal Caverns, Citadel): generated maps (always reachable), tile art, ambient particles, themed enemies/bosses/guardians (`scenery/`).

**Adaptive gamification (AI/ML)** - `ai/`: Bayesian Knowledge Tracing per topic, online logistic-regression success predictor (pre-trained on simulated learners), engagement-state estimator, and a Thompson-sampling bandit that picks supports (50/50, shield, hint, bonus XP). The original rule-based `AdaptiveEngine` is kept as the baseline and as one input to the coach. "Learning Insights" (M) explains each decision.

**Usability** - clickable answers, HUD with hearts/XP/minimap/objective arrow, toasts, help overlay (H/F1), Esc closes overlays, autosave, settings (text size, high contrast, colour-blind palette, reduce motion, minimap, AI Coach on/off), locked-level explanations, recommended level, satisfaction prompt + session report. Usability CSVs and AI profiles go to `~/.dsarpg/`; the MySQL schema is unchanged.

# Tests
Plain `main` programs in `src/test/java/com/dsarpg` (`AiSelfTest`, `SceneryTest`, `UsabilityTest`). Compile the non-JavaFX packages with javac and run `java -cp <out> com.dsarpg.AiSelfTest`.
