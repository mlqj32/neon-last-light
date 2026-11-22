package studio.lastlight;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

/** 原创程序化图标；同一图形用于窗口和 Windows 便携版。 */
public final class GameIcon {
    private GameIcon() { }
    public static BufferedImage image() {
        BufferedImage image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(GameRenderer.BG);g.fillRoundRect(0,0,64,64,16,16);
        g.setColor(new Color(0x244d51));g.setStroke(new BasicStroke(1.5f));g.drawOval(7,7,50,50);
        g.setColor(GameRenderer.CYAN);g.fillPolygon(new int[]{51,15,25,15},new int[]{32,14,32,50},4);
        g.setColor(GameRenderer.BG);g.fillPolygon(new int[]{43,21,29,21},new int[]{32,23,32,41},4);
        g.setColor(GameRenderer.TEXT);g.fillOval(30,29,6,6);g.dispose();return image;
    }
    public static void main(String[] args) throws IOException {
        if(args.length!=1)throw new IllegalArgumentException("请指定图标输出路径。");
        ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image(),"png",out);byte[] png=out.toByteArray();
        ByteBuffer ico=ByteBuffer.allocate(22+png.length).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short)0).putShort((short)1).putShort((short)1);
        ico.put((byte)64).put((byte)64).put((byte)0).put((byte)0).putShort((short)1).putShort((short)32).putInt(png.length).putInt(22).put(png);
        Path path=Path.of(args[0]);Files.createDirectories(path.toAbsolutePath().getParent());Files.write(path,ico.array());
    }
}
