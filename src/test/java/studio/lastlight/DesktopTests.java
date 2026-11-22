package studio.lastlight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import static studio.lastlight.GameModel.Screen;

/** 原生 Swing 控件事件测试。只使用隔离测试目录，不写入玩家的正式存档。 */
public final class DesktopTests {
    private static int checks;
    private static void check(boolean value,String message) {checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        Path folder=Files.createTempDirectory(Path.of("target"),"desktop-test-");
        SwingUtilities.invokeAndWait(()->{
            JFrame frame=new JFrame("Last Light - input verification");GamePanel panel=new GamePanel(new SaveStore(folder),()->{});
            try {
                frame.setContentPane(panel);frame.pack();frame.setVisible(true);GameModel model=panel.modelForTesting();
                draw(panel);click(panel,100,396);check(model.screen==Screen.PLAYING,"Mouse starts a run");
                draw(panel);click(panel,1160,60);check(model.screen==Screen.PAUSED,"Mouse pause button works during combat");
                panel.action("resume");check(model.screen==Screen.PLAYING,"Resume returns to combat");
                key(panel,KeyEvent.VK_P,true);check(model.screen==Screen.PAUSED,"P pauses once");key(panel,KeyEvent.VK_P,true);check(model.screen==Screen.PAUSED,"Held P does not repeatedly toggle pause");
                key(panel,KeyEvent.VK_P,false);key(panel,KeyEvent.VK_P,true);check(model.screen==Screen.PLAYING,"Fresh P press resumes");key(panel,KeyEvent.VK_P,false);
                panel.pauseForFocusLoss();check(model.screen==Screen.PAUSED,"Focus loss pauses combat");
                panel.action("settings");check(model.screen==Screen.SETTINGS,"Paused settings opens");panel.action("difficulty");panel.action("back");check(model.screen==Screen.PAUSED,"Settings returns to pause without starting simulation");
                panel.action("home");check(model.screen==Screen.HOME,"Return to base works");
                panel.setSize(960,660);draw(panel);click(panel,100,467);check(model.screen==Screen.PLAYING,"Scaled mouse restores checkpoint");
                check(model.wave==1&&model.runDifficulty==Settings.Difficulty.STANDARD,"Continue preserves wave and original difficulty");
                panel.action("home");draw(panel);click(panel,100,396);check(model.screen==Screen.CONFIRM_NEW,"Existing checkpoint triggers overwrite confirmation");panel.action("cancel");check(model.screen==Screen.HOME,"Cancel preserves checkpoint");
                panel.action("confirmStart");check(model.runDifficulty==Settings.Difficulty.VETERAN,"New run applies changed difficulty");
            } finally {panel.close();frame.dispose();}
        });
        System.out.println("窗口测试通过："+checks+" 条输入、缩放、失焦与菜单导航断言。");
    }
    private static void draw(GamePanel panel) {BufferedImage image=new BufferedImage(panel.getWidth(),panel.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();}
    private static void click(GamePanel panel,double x,double y) {
        double s=Math.min(panel.getWidth()/1280.0,panel.getHeight()/800.0);int px=(int)((panel.getWidth()-1280*s)/2+x*s),py=(int)((panel.getHeight()-800*s)/2+y*s);
        MouseEvent e=new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,px,py,1,false,MouseEvent.BUTTON1);
        for(MouseListener listener:panel.getMouseListeners())listener.mousePressed(e);
    }
    private static void key(GamePanel panel,int code,boolean press) {
        KeyEvent e=new KeyEvent(panel,press?KeyEvent.KEY_PRESSED:KeyEvent.KEY_RELEASED,System.currentTimeMillis(),0,code,KeyEvent.CHAR_UNDEFINED);
        for(KeyListener listener:panel.getKeyListeners())if(press)listener.keyPressed(e);else listener.keyReleased(e);
    }
}
