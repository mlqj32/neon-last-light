package studio.lastlight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/** 窗口事件适配层：输入、菜单动作、60 FPS 更新，以及存档/音频的协调。 */
public final class GamePanel extends JPanel implements AutoCloseable {
    private static final long serialVersionUID=1L;
    private final SaveStore store;
    private final GameModel model;
    private final Input input=new Input();
    private final GameRenderer renderer=new GameRenderer();
    private final AudioEngine audio;
    private final Timer timer;
    private List<UiButton> buttons=List.of();
    private Properties checkpoint;
    private int focused=-1;
    private long previous=System.nanoTime();
    private double visualTime, noticeTime, saveRetryTime;
    private String notice="";
    private boolean profileDirty, closed;
    private final Runnable exit;

    public GamePanel(SaveStore store,Runnable exit) {
        this.store=store;this.exit=exit;this.model=new GameModel(store.loadProfile());checkpoint=store.loadCheckpoint();
        audio=new AudioEngine(model.profile.settings);audio.start();
        setPreferredSize(new Dimension(GameModel.WIDTH,GameModel.HEIGHT));setBackground(GameRenderer.BG);setFocusable(true);setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                boolean fresh=!input.held(e.getKeyCode());input.press(e.getKeyCode());if(fresh) keyboard(e);input.press(e.getKeyCode());
            }
            @Override public void keyReleased(KeyEvent e) {input.release(e.getKeyCode());}
        });
        MouseAdapter mouse=new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {point(e);focused=-1;}
            @Override public void mouseDragged(MouseEvent e) {point(e);}
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();point(e);
                if(e.getButton()!=MouseEvent.BUTTON1)return;
                // 暂停按钮在战斗中也参与点击判定，不能被开火分支吞掉。
                for(UiButton b:buttons) if(b.contains(input.mouseX,input.mouseY)) {action(b.id());return;}
                if(model.screen==GameModel.Screen.PLAYING)input.firing=true;
            }
            @Override public void mouseReleased(MouseEvent e) {if(e.getButton()==MouseEvent.BUTTON1)input.firing=false;}
            @Override public void mouseExited(MouseEvent e) {input.firing=false;}
        };
        addMouseListener(mouse);addMouseMotionListener(mouse);
        timer=new Timer(16,e->tick());timer.start();
    }
    private void point(MouseEvent e) {
        double scale=Math.min(getWidth()/(double)GameModel.WIDTH,getHeight()/(double)GameModel.HEIGHT);
        input.mouseX=(e.getX()-(getWidth()-GameModel.WIDTH*scale)/2)/scale;
        input.mouseY=(e.getY()-(getHeight()-GameModel.HEIGHT*scale)/2)/scale;
    }
    private void keyboard(KeyEvent e) {
        int code=e.getKeyCode();
        if(code==KeyEvent.VK_ESCAPE || code==KeyEvent.VK_P) {
            switch(model.screen) {
                case PLAYING -> action("pause");case PAUSED -> action("resume");
                case HELP,SETTINGS,WORKSHOP -> action("back");case CONFIRM_NEW -> action("cancel");
                default -> { }
            }
        } else if(code==KeyEvent.VK_TAB) {
            if(!buttons.isEmpty()) {
                int step=e.isShiftDown()?-1:1;
                for(int i=0;i<buttons.size();i++) {focused=Math.floorMod(focused+step,buttons.size());if(buttons.get(focused).enabled())break;}
            }
        } else if(code==KeyEvent.VK_ENTER && focused>=0 && focused<buttons.size()) {
            UiButton b=buttons.get(focused);if(b.enabled())action(b.id());
        } else if(code>=KeyEvent.VK_1 && code<=KeyEvent.VK_6) {
            int index=code-KeyEvent.VK_1;
            if(model.screen==GameModel.Screen.UPGRADE && index<3)action("upgrade"+index);
            else if(model.screen==GameModel.Screen.HOME)action("loadout"+index);
        }
    }
    private void tick() {
        long now=System.nanoTime();double dt=Math.min(.05,(now-previous)/1_000_000_000.0);previous=now;visualTime+=dt;
        model.update(dt,input);input.endFrame();
        audio.combat(model.screen==GameModel.Screen.PLAYING);
        for(AudioEngine.Cue cue:model.sounds)audio.play(cue);model.sounds.clear();
        noticeTime=Math.max(0,noticeTime-dt);if(noticeTime==0)notice="";
        saveRetryTime=Math.max(0,saveRetryTime-dt);if(saveRetryTime==0)persist();
        repaint();
    }
    private boolean canContinue() {
        return "1".equals(checkpoint.getProperty("version"))&&!checkpoint.getProperty("runId", "").isBlank()
                && !checkpoint.getProperty("runId").equals(model.profile.lastSettledRun);
    }
    void action(String id) {
        audio.play(AudioEngine.Cue.CLICK);focused=-1;input.clear();
        switch(id) {
            case "start" -> {if(canContinue())model.screen=GameModel.Screen.CONFIRM_NEW;else deploy();}
            case "confirmStart","retry" -> deploy();
            case "cancel","home" -> model.screen=GameModel.Screen.HOME;
            case "continue" -> {if(!model.resume(checkpoint))message("检查点无法读取，请开始新的游戏。");}
            case "pause" -> {if(model.screen==GameModel.Screen.PLAYING)model.screen=GameModel.Screen.PAUSED;}
            case "resume" -> {if(model.screen==GameModel.Screen.PAUSED)model.screen=GameModel.Screen.PLAYING;}
            case "workshop" -> {model.returnScreen=GameModel.Screen.HOME;model.screen=GameModel.Screen.WORKSHOP;}
            case "settings","help" -> {model.returnScreen=model.screen;model.screen=id.equals("settings")?GameModel.Screen.SETTINGS:GameModel.Screen.HELP;}
            case "back" -> model.screen=model.returnScreen;
            case "difficulty" -> {var values=Settings.Difficulty.values();model.profile.settings.difficulty=values[(model.profile.settings.difficulty.ordinal()+1)%values.length];profileDirty=true;}
            case "sound" -> {model.profile.settings.sound=!model.profile.settings.sound;profileDirty=true;}
            case "music" -> {model.profile.settings.music=!model.profile.settings.music;profileDirty=true;}
            case "motion" -> {model.profile.settings.reducedMotion=!model.profile.settings.reducedMotion;profileDirty=true;}
            case "contrast" -> {model.profile.settings.highContrast=!model.profile.settings.highContrast;profileDirty=true;}
            case "volumeUp","volumeDown" -> {model.profile.settings.volume=Math.clamp(model.profile.settings.volume+(id.equals("volumeUp")?.1:-.1),0,1);profileDirty=true;}
            case "exit" -> exit.run();
            default -> {
                if(id.startsWith("loadout"))model.loadout=GameModel.Loadout.values()[Integer.parseInt(id.substring(7))];
                else if(id.startsWith("upgrade"))model.chooseUpgrade(Integer.parseInt(id.substring(7)));
                else if(id.startsWith("buy")&&model.profile.purchase(Integer.parseInt(id.substring(3))))profileDirty=true;
            }
        }
        buttons=List.of();persist();repaint();
    }
    private void deploy() {
        // 新飞行在成功写入检查点后才替换旧档；不先删除用户唯一的存档。
        model.newRun(model.loadout,System.nanoTime());
    }
    private void persist() {
        try {
            if(model.checkpointReady) {Properties next=model.checkpoint();store.saveCheckpoint(next);checkpoint=next;model.checkpointReady=false;}
            if(model.resultReady) {store.saveProfile(model.profile);store.clearCheckpoint();checkpoint=new Properties();model.resultReady=false;profileDirty=false;}
            else if(profileDirty) {store.saveProfile(model.profile);profileDirty=false;}
        } catch(IOException e) {message("保存失败，进度暂存于内存。请检查存档目录的写入权限。");saveRetryTime=5;}
    }
    private void message(String value) {notice=value;noticeTime=8;}
    public void pauseForFocusLoss() {if(model.screen==GameModel.Screen.PLAYING)model.screen=GameModel.Screen.PAUSED;input.clear();}
    GameModel modelForTesting() {return model;}
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
        double scale=Math.min(getWidth()/(double)GameModel.WIDTH,getHeight()/(double)GameModel.HEIGHT);
        g.translate((getWidth()-GameModel.WIDTH*scale)/2,(getHeight()-GameModel.HEIGHT*scale)/2);g.scale(scale,scale);
        buttons=renderer.render(g,model,input,visualTime,canContinue(),focused,notice);g.dispose();
    }
    @Override public void close() {if(closed)return;closed=true;timer.stop();persist();audio.close();}
}
