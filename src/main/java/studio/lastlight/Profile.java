package studio.lastlight;

/** 局外进度与设置。单局属性独立存储，不把临时强化写回永久升级。 */
public final class Profile {
    public int credits, bestWave, bestScore, victories, runs, hullLevel, reactorLevel, weaponLevel;
    public final Settings settings = new Settings();
    public String lastSettledRun = "";
    public int cost(int level) { return 40 + level * 35; }
    public boolean purchase(int type) {
        int level = switch (type) { case 0 -> hullLevel; case 1 -> reactorLevel; default -> weaponLevel; };
        if (level >= 5 || credits < cost(level)) return false;
        credits -= cost(level);
        switch (type) { case 0 -> hullLevel++; case 1 -> reactorLevel++; default -> weaponLevel++; }
        return true;
    }
}
