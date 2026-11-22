package studio.lastlight;

import java.awt.geom.Rectangle2D;

/** 按钮的点击区域与动作标识。渲染器只描述布局，不执行游戏动作。 */
public record UiButton(String id, String label, Rectangle2D.Double bounds, boolean enabled, boolean primary) {
    public boolean contains(double x, double y) { return enabled && bounds.contains(x,y); }
}
