package com.dsarpg.scenery;

/**
 * Visual identity of each region. This class only holds data (hex colours
 * and style choices) so it can be used and tested without JavaFX; the
 * {@link SceneryRenderer} turns it into pictures.
 */
public enum SceneryTheme {

    FOREST(1, "THE FORGOTTEN FOREST", "Whispering trees and a corrupted guardian",
            "Forest Guardian", "Corrupted Treant",
            new String[] {"#567D46", "#6F9654", "#41683A"},      // ground
            new String[] {"#B99A67", "#C9AD7D", "#92744F"},      // path
            Obstacle.TREE, Liquid.POND, "#3E86B5", "#7DBBE0",
            Ambient.FIREFLIES, "#FFE9A8", 0.06, "#6FA05A"),

    RUINS(2, "ANCIENT RUINS", "Sun-baked stone and forgotten puzzles",
            "Ruins Archivist", "Ancient Golem",
            new String[] {"#C9AE7A", "#DCC592", "#A58C5D"},
            new String[] {"#9C8F7A", "#B7AB95", "#7A6F5C"},
            Obstacle.PILLAR, Liquid.OASIS, "#2FA4A0", "#7FD8CF",
            Ambient.DUST, "#E8B24A", 0.10, "#D9A441"),

    FROZEN_PEAKS(3, "FROZEN PEAKS", "Howling wind and glittering ice",
            "Frost Sage", "Frozen Peak Warden",
            new String[] {"#DCE8F2", "#F4FAFF", "#B7CBDC"},
            new String[] {"#9FB6C8", "#BFD3E2", "#7F98AD"},
            Obstacle.PINE, Liquid.ICE, "#6F9CC0", "#CFE8F7",
            Ambient.SNOW, "#BFE0FF", 0.10, "#8EC5EC"),

    CRYSTAL_CAVERNS(4, "CRYSTAL CAVERNS", "Glowing crystals deep underground",
            "Crystal Seer", "Crystal Behemoth",
            new String[] {"#3A3752", "#4A4766", "#2B2940"},
            new String[] {"#5E5A85", "#7C78AA", "#46426A"},
            Obstacle.CRYSTAL, Liquid.GLOW_POOL, "#2FC5B4", "#9FF5E8",
            Ambient.SPORES, "#7B2CBF", 0.14, "#9B6DFF"),

    CITADEL(5, "THE FINAL CITADEL", "A fortress of shadow and fire",
            "Citadel Sentinel", "Citadel Overlord",
            new String[] {"#4A4448", "#5C5559", "#383337"},
            new String[] {"#7A4F52", "#9A6567", "#5C3A3D"},
            Obstacle.TOWER, Liquid.LAVA, "#D8481E", "#FFB347",
            Ambient.EMBERS, "#8B1E1E", 0.14, "#E8573C");

    /** Kind of blocking obstacle drawn for tile type 1. */
    public enum Obstacle { TREE, PILLAR, PINE, CRYSTAL, TOWER }

    /** Kind of liquid drawn for tile type 3 (also blocking). */
    public enum Liquid { POND, OASIS, ICE, GLOW_POOL, LAVA }

    /** Floating particles that give each region its atmosphere. */
    public enum Ambient { FIREFLIES, DUST, SNOW, SPORES, EMBERS }

    private final int level;
    private final String title;
    private final String tagline;
    private final String guardianName;
    private final String bossName;
    private final String[] ground;
    private final String[] path;
    private final Obstacle obstacle;
    private final Liquid liquid;
    private final String liquidDeep;
    private final String liquidLight;
    private final Ambient ambient;
    private final String tint;
    private final double tintAlpha;
    private final String accent;

    SceneryTheme(int level, String title, String tagline,
                 String guardianName, String bossName,
                 String[] ground, String[] path,
                 Obstacle obstacle, Liquid liquid,
                 String liquidDeep, String liquidLight,
                 Ambient ambient, String tint, double tintAlpha,
                 String accent) {
        this.level = level;
        this.title = title;
        this.tagline = tagline;
        this.guardianName = guardianName;
        this.bossName = bossName;
        this.ground = ground;
        this.path = path;
        this.obstacle = obstacle;
        this.liquid = liquid;
        this.liquidDeep = liquidDeep;
        this.liquidLight = liquidLight;
        this.ambient = ambient;
        this.tint = tint;
        this.tintAlpha = tintAlpha;
        this.accent = accent;
    }

    public static SceneryTheme forLevel(int level) {
        for (SceneryTheme theme : values()) {
            if (theme.level == level) {
                return theme;
            }
        }

        return FOREST;
    }

    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public String getTagline() { return tagline; }
    public String getGuardianName() { return guardianName; }
    public String getBossName() { return bossName; }
    public String getGroundBase() { return ground[0]; }
    public String getGroundLight() { return ground[1]; }
    public String getGroundDark() { return ground[2]; }
    public String getPathBase() { return path[0]; }
    public String getPathLight() { return path[1]; }
    public String getPathDark() { return path[2]; }
    public Obstacle getObstacle() { return obstacle; }
    public Liquid getLiquid() { return liquid; }
    public String getLiquidDeep() { return liquidDeep; }
    public String getLiquidLight() { return liquidLight; }
    public Ambient getAmbient() { return ambient; }
    public String getTint() { return tint; }
    public double getTintAlpha() { return tintAlpha; }
    public String getAccent() { return accent; }
}
