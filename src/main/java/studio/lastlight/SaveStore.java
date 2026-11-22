package studio.lastlight;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Properties;

/** 存档先落临时文件，再替换正式文件；失败时保持旧档和备份。 */
public final class SaveStore {
    private final Path directory;
    public SaveStore(Path directory) { this.directory = directory.toAbsolutePath(); }
    public Path directory() { return directory; }
    public Profile loadProfile() {
        Properties p = read("profile.properties");
        Profile v = new Profile();
        v.credits = number(p, "credits", 0, 0, 1_000_000);
        v.bestWave = number(p, "bestWave", 0, 0, GameModel.FINAL_WAVE);
        v.bestScore = number(p, "bestScore", 0, 0, 100_000_000);
        v.victories = number(p, "victories", 0, 0, 1_000_000);
        v.runs = number(p, "runs", 0, 0, 1_000_000);
        v.lastSettledRun = p.getProperty("lastSettledRun", "");
        v.hullLevel = number(p, "hull", 0, 0, 5);
        v.reactorLevel = number(p, "reactor", 0, 0, 5);
        v.weaponLevel = number(p, "weapon", 0, 0, 5);
        v.settings.difficulty = Settings.Difficulty.values()[number(p, "difficulty", 1, 0, 2)];
        v.settings.volume = number(p, "volume", 35, 0, 100) / 100.0;
        v.settings.sound = bool(p, "sound", true); v.settings.music = bool(p, "music", true);
        v.settings.reducedMotion = bool(p, "reducedMotion", false); v.settings.highContrast = bool(p, "highContrast", false);
        return v;
    }
    public void saveProfile(Profile v) throws IOException {
        Properties p = new Properties();
        p.setProperty("version", "1");
        p.setProperty("lastSettledRun", v.lastSettledRun);
        put(p, "credits", v.credits); put(p, "bestWave", v.bestWave); put(p, "bestScore", v.bestScore);
        put(p, "victories", v.victories); put(p, "runs", v.runs); put(p, "hull", v.hullLevel);
        put(p, "reactor", v.reactorLevel); put(p, "weapon", v.weaponLevel);
        put(p, "difficulty", v.settings.difficulty.ordinal()); put(p, "volume", (int) Math.round(v.settings.volume * 100));
        p.setProperty("sound", Boolean.toString(v.settings.sound)); p.setProperty("music", Boolean.toString(v.settings.music));
        p.setProperty("reducedMotion", Boolean.toString(v.settings.reducedMotion)); p.setProperty("highContrast", Boolean.toString(v.settings.highContrast));
        write("profile.properties", p);
    }
    public void saveCheckpoint(Properties snapshot) throws IOException { write("checkpoint.properties", snapshot); }
    public Properties loadCheckpoint() { return read("checkpoint.properties"); }
    public void clearCheckpoint() throws IOException {
        Files.deleteIfExists(directory.resolve("checkpoint.properties"));
        Files.deleteIfExists(directory.resolve("checkpoint.properties.bak"));
    }
    private Properties read(String name) {
        for (String candidate : new String[]{name, name + ".bak"}) {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(directory.resolve(candidate))) {
                p.load(r);
                if ("1".equals(p.getProperty("version"))) return p;
            } catch (IOException | IllegalArgumentException ignored) { /* 缺档或坏档时尝试备份。 */ }
        }
        return new Properties();
    }
    private void write(String name, Properties p) throws IOException {
        Files.createDirectories(directory);
        Path file = directory.resolve(name), temp = directory.resolve(name + ".tmp"), backup = directory.resolve(name + ".bak");
        try (Writer out = Files.newBufferedWriter(temp)) { p.store(out, "Neon Last Light save v1"); }
        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) { channel.force(true); }
        if (Files.exists(file)) Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
    }
    public static int number(Properties p, String key, int fallback, int min, int max) {
        try { return Math.clamp(Integer.parseInt(p.getProperty(key)), min, max); }
        catch (Exception e) { return fallback; }
    }
    private static boolean bool(Properties p, String key, boolean fallback) { return Boolean.parseBoolean(p.getProperty(key, Boolean.toString(fallback))); }
    private static void put(Properties p, String key, int value) { p.setProperty(key, Integer.toString(value)); }
}
