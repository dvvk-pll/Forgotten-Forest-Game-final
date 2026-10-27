package com.dsarpg;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import com.dsarpg.usability.UsabilityLogger;
import com.dsarpg.usability.UserSettings;

/**
 * Plain-Java checks for settings persistence and usability metrics.
 * Run: java -cp target/classes:target/test-classes com.dsarpg.UsabilityTest
 */
public class UsabilityTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean ok, String msg) {
        if (ok) { passed++; System.out.println("  PASS  " + msg); }
        else { failed++; System.out.println("  FAIL  " + msg); }
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("dsarpg-test").toFile();

        System.out.println("Settings");
        File file = new File(dir, "settings.properties");
        UserSettings s = new UserSettings(file);
        check(s.getTextSize() == UserSettings.TextSize.MEDIUM && s.isAiCoach()
                && !s.isHighContrast(), "defaults are sensible");

        s.setTextSize(UserSettings.TextSize.LARGE);
        s.setHighContrast(true);
        s.setColorblindSafe(true);
        s.setReduceMotion(true);
        s.setAiCoach(false);
        s.setTutorialSeen(true);
        s.setLastUsername("devika");
        s.save();

        UserSettings again = new UserSettings(file);
        check(again.getTextSize() == UserSettings.TextSize.LARGE
                && again.isHighContrast() && again.isColorblindSafe()
                && again.isReduceMotion() && !again.isAiCoach()
                && again.isTutorialSeen()
                && again.getLastUsername().equals("devika"),
                "settings survive a restart");

        Files.writeString(file.toPath(), "textSize=GIGANTIC\nhighContrast=maybe\n");
        UserSettings damaged = new UserSettings(file);
        check(damaged.getTextSize() == UserSettings.TextSize.MEDIUM,
                "an invalid value falls back to the default");

        System.out.println("Usability metrics");
        UsabilityLogger log = new UsabilityLogger("tester, \"quoted\"", true, new File(dir, "usability"));
        log.battleStarted();
        log.battleStarted();
        log.battleWon();
        log.answer("Arrays", 1, true, 8000, false, "NONE", "FLOW", 0.7, true);
        log.answer("Arrays", 2, false, 12000, true, "SHIELD", "FRUSTRATED", 0.5, false);
        log.hintUsed();
        log.answer("Stacks", 3, true, 10000, false, "NONE", "FLOW", 0.6, true);
        log.setSatisfaction(4);

        check(Math.abs(log.taskCompletionRate() - 0.5) < 1e-9, "task completion rate = 50%");
        check(Math.abs(log.errorRate() - 1.0 / 3) < 1e-9, "error rate = 1/3");
        check(Math.abs(log.averageResponseSeconds() - 10.0) < 1e-9, "average response 10 s");
        check(Math.abs(log.mouseShare() - 2.0 / 3) < 1e-9, "mouse share = 2/3");
        check(log.report().contains("Satisfaction: 4 / 5"), "report shows satisfaction");

        log.flush();
        log.flush();
        File sessions = new File(dir, "usability/sessions.csv");
        File events = new File(dir, "usability/events.csv");
        List<String> sessionLines = Files.readAllLines(sessions.toPath());
        List<String> eventLines = Files.readAllLines(events.toPath());
        check(sessionLines.size() == 2, "one session row plus header (flush is idempotent)");
        check(eventLines.size() == 4, "three event rows plus header");
        check(sessionLines.get(1).contains("\"tester, \"\"quoted\"\"\""),
                "CSV values are escaped correctly");

        UsabilityLogger empty = new UsabilityLogger("nobody", false, new File(dir, "empty"));
        empty.flush();
        check(!new File(dir, "empty").exists(), "an empty session writes nothing");

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }
}
