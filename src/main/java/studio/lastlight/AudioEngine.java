package studio.lastlight;

import javax.sound.sampled.*;
import java.util.concurrent.ArrayBlockingQueue;

/** 使用正弦波合成背景音乐与短音效，没有外部音频素材或第三方依赖。 */
public final class AudioEngine implements AutoCloseable {
    public enum Cue { SHOT, HIT, DASH, PULSE, PICKUP, CLEAR, CLICK, LOSS, WIN }
    private final ArrayBlockingQueue<Cue> cues = new ArrayBlockingQueue<>(32);
    private final Settings settings;
    private volatile boolean running = true, combat;
    private Thread worker;
    private SourceDataLine line;
    public AudioEngine(Settings settings) { this.settings = settings; }
    public void start() {
        worker = new Thread(this::mix, "last-light-audio"); worker.setDaemon(true); worker.start();
    }
    public void play(Cue cue) { if (settings.sound && settings.volume > 0) cues.offer(cue); }
    public void combat(boolean value) { combat = value; }
    private void mix() {
        try {
            AudioFormat format = new AudioFormat(22050, 16, 1, true, false);
            line = AudioSystem.getSourceDataLine(format); line.open(format, 4096); line.start();
            byte[] buffer = new byte[1024];
            long sample = 0, cueStart = 0; Cue active = null;
            double[] notes = {110, 164.81, 146.83, 196, 110, 220, 164.81, 146.83};
            while (running) {
                Cue cue = cues.poll(); if (cue != null) { active = cue; cueStart = sample; }
                for (int i = 0; i < buffer.length / 2; i++, sample++) {
                    double time = sample / 22050.0, value = 0;
                    if (settings.music) {
                        double beat = combat ? .25 : .5;
                        double note = notes[(int)(time / beat) % notes.length];
                        double envelope = Math.exp(-(time % beat) * 9);
                        value = .1 * Math.sin(2 * Math.PI * note * time) * envelope
                                + .035 * Math.sin(2 * Math.PI * 55 * time);
                    }
                    double elapsed = (sample - cueStart) / 22050.0;
                    if (active != null && settings.sound && elapsed < .22) {
                        double frequency = switch (active) {
                            case SHOT -> 650 - elapsed * 1700; case HIT, LOSS -> 100;
                            case DASH -> 280 + elapsed * 1700; case PULSE -> 65 + elapsed * 600;
                            case PICKUP -> 960; case CLEAR, WIN -> elapsed < .07 ? 523 : elapsed < .14 ? 659 : 784;
                            case CLICK -> 440;
                        };
                        value += .28 * Math.sin(2 * Math.PI * frequency * elapsed) * Math.exp(-elapsed * 20);
                    }
                    short pcm = (short) (Math.clamp(value * settings.volume, -1, 1) * 32767);
                    buffer[i * 2] = (byte) pcm; buffer[i * 2 + 1] = (byte) (pcm >> 8);
                }
                line.write(buffer, 0, buffer.length);
            }
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            // 无音频设备时保持游戏可玩。
        } finally { if (line != null) { line.stop(); line.close(); } }
    }
    @Override public void close() { running = false; if (worker != null) worker.interrupt(); }
}
