package studio.lastlight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** IDEA 运行入口：右键本类，选择 Run 'Main.main()'。 */
public final class Main {
    private Main() { }
    public static void main(String[] args) {
        if(GraphicsEnvironment.isHeadless()) {System.err.println("游戏需要桌面显示环境。无需窗口的测试可使用 build.ps1 -Test 运行。");return;}
        SwingUtilities.invokeLater(()->{
            JFrame frame=new JFrame("霓虹：最后的光");
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            GamePanel[] holder=new GamePanel[1];
            Runnable close=()->{holder[0].close();frame.dispose();};
            GamePanel panel=new GamePanel(new SaveStore(dataDirectory()),close);holder[0]=panel;
            frame.setContentPane(panel);frame.pack();frame.setMinimumSize(new Dimension(960,660));frame.setLocationRelativeTo(null);
            frame.setIconImage(GameIcon.image());
            frame.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){close.run();}});
            frame.addWindowFocusListener(new WindowAdapter(){@Override public void windowLostFocus(WindowEvent e){panel.pauseForFocusLoss();}});
            frame.setVisible(true);panel.requestFocusInWindow();
            if(args.length>0 && args[0].equals("--smoke")) {Timer timer=new Timer(1500,e->{panel.action("start");panel.action("pause");panel.action("resume");close.run();System.out.println("窗口启动、菜单、暂停、恢复与退出测试通过。");});timer.setRepeats(false);timer.start();}
        });
    }
    public static Path dataDirectory() {
        String override=System.getProperty("lastlight.data");if(override!=null&&!override.isBlank())return Path.of(override).toAbsolutePath();
        try {
            Path source=Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path directory=Files.isDirectory(source)?source:source.getParent();
            if(directory.getFileName().toString().equals("classes"))directory=directory.getParent();
            if(directory.getFileName().toString().equals("target")||directory.getFileName().toString().equals("app"))directory=directory.getParent();
            return directory.resolve("data");
        }catch(Exception e){return Path.of("data").toAbsolutePath();}
    }
}
