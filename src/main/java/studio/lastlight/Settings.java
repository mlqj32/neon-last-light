package studio.lastlight;

/** 可保存的玩家偏好；不引用界面，方便测试与复用。 */
public final class Settings {
    public enum Difficulty {
        RELAXED("休闲", .72, .82), STANDARD("标准", 1, 1), VETERAN("挑战", 1.28, 1.15);
        public final String label;
        public final double damage, speed;
        Difficulty(String label, double damage, double speed) { this.label = label; this.damage = damage; this.speed = speed; }
    }
    public volatile Difficulty difficulty = Difficulty.STANDARD;
    public volatile double volume = .35;
    public volatile boolean sound = true, music = true, reducedMotion = false, highContrast = false;
}
