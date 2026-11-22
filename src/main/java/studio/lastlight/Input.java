package studio.lastlight;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/** 按住与单次触发分别记录，避免按键自动重复影响闪避和暂停。 */
public final class Input {
    private final Set<Integer> held = new HashSet<>(), pressed = new HashSet<>();
    public double mouseX = 900, mouseY = 400;
    public boolean firing;
    public void press(int key) { if (held.add(key)) pressed.add(key); }
    public void release(int key) { held.remove(key); }
    public boolean held(int key) { return held.contains(key); }
    public boolean take(int key) { return pressed.remove(key); }
    public double horizontal() { return (held(KeyEvent.VK_D) || held(KeyEvent.VK_RIGHT) ? 1 : 0) - (held(KeyEvent.VK_A) || held(KeyEvent.VK_LEFT) ? 1 : 0); }
    public double vertical() { return (held(KeyEvent.VK_S) || held(KeyEvent.VK_DOWN) ? 1 : 0) - (held(KeyEvent.VK_W) || held(KeyEvent.VK_UP) ? 1 : 0); }
    public void endFrame() { pressed.clear(); }
    public void clear() { held.clear(); pressed.clear(); firing = false; }
}
