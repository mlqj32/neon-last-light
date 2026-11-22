package studio.lastlight;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import static studio.lastlight.GameModel.*;

/** 不依赖第三方测试库；从终端或 IDEA 直接运行本类即可验证游戏规则。 */
public final class GameTests {
    private static int checks;
    private static void check(boolean ok,String message) {checks++;if(!ok)throw new AssertionError(message);}
    private static void close(double expected,double actual,String message) {check(Math.abs(expected-actual)<.0001,message+": "+actual);}
    private static GameModel game() {GameModel m=new GameModel(new Profile());m.newRun(Loadout.STRIKER,12345);return m;}
    public static void main(String[] args) throws Exception {
        movement();collision();abilities();expansion();pause();progression();storage();checkpoints();randomness();simulation();renderScreens();
        System.out.println("测试通过："+checks+" 条断言。界面预览：target/preview/");
    }
    private static void movement() {
        GameModel a=game(),b=game();Input straight=new Input(),diagonal=new Input();straight.press(KeyEvent.VK_D);diagonal.press(KeyEvent.VK_D);diagonal.press(KeyEvent.VK_S);
        double ax=a.player.x,ay=a.player.y,bx=b.player.x,by=b.player.y;
        a.update(.05,straight);b.update(.05,diagonal);
        close(distance(a.player.x,a.player.y,ax,ay),distance(b.player.x,b.player.y,bx,by),"Diagonal movement has normalized speed");
        for(int i=0;i<300;i++)a.update(.05,straight);
        check(a.player.x<=RIGHT-15&&a.player.x>=LEFT+15,"Player cannot escape the arena");
        check(a.bullets.size()<=500&&a.enemies.size()<=100&&a.particles.size()<=350,"Entity limits remain bounded");
    }
    private static void collision() {
        close(0,segmentDistance(80,40,0,40,100,40),"Swept collision crosses a target");
        close(20,segmentDistance(120,40,0,40,100,40),"Swept collision clamps to segment end");
        GameModel m=game();m.spawnTimer=100;
        Enemy a=new Enemy(1,EnemyType.SEEKER,500,300,1),b=new Enemy(2,EnemyType.SEEKER,530,300,1);a.warning=b.warning=0;a.speed=b.speed=0;m.enemies.add(a);m.enemies.add(b);
        m.bullets.add(new Bullet(475,300,1500,0,8,false,1));double hp=a.hp;m.update(.05,new Input());
        close(hp-8,a.hp,"A high-speed round hits the first enemy");close(hp-8,b.hp,"A piercing round hits a second enemy");
        m.update(.01,new Input());close(hp-8,b.hp,"Same round never hits an enemy twice");
        m.player.invincible=0;double before=m.player.hp;m.hurt(9);m.hurt(9);close(before-9,m.player.hp,"Damage grants temporary invulnerability");
    }
    private static void abilities() {
        GameModel m=game();m.spawnTimer=100;Input input=new Input();input.press(KeyEvent.VK_SPACE);double x=m.player.x;input.mouseX=x+300;input.mouseY=m.player.y;
        m.update(.05,input);check(m.player.x>x+30&&m.player.invincible>0&&m.player.dashTimer>0,"Dash moves quickly and grants invulnerability");
        double timer=m.player.dashTimer;input.release(KeyEvent.VK_SPACE);input.press(KeyEvent.VK_SPACE);m.update(.05,input);check(m.player.dashTimer<timer,"Dash cannot bypass its cooldown");
        Enemy e=new Enemy(1,EnemyType.BRUTE,m.player.x+100,m.player.y,4);e.warning=0;e.speed=0;m.enemies.add(e);double hp=e.hp;
        m.bullets.add(new Bullet(m.player.x+90,m.player.y,0,0,10,true,0));input.press(KeyEvent.VK_E);m.update(.01,input);
        close(hp-m.player.pulseDamage,e.hp,"Pulse damages nearby enemies");check(m.bullets.isEmpty()&&m.player.pulseTimer>0,"Pulse clears nearby hostile bullets");
    }
    private static void expansion() {
        check(FINAL_WAVE==20&&isBossWave(5)&&isBossWave(10)&&isBossWave(15)&&isBossWave(20),"Campaign expands to twenty sectors with four boss waves");
        check(Loadout.values().length==6,"Six playable ships are available");
        check(Upgrade.values().length>=17,"Upgrade pool expands beyond the original ten");
        check(EnemyType.values().length==8,"Enemy roster includes three new species and the boss");
        GameModel m=game();m.player.shieldMax=1;m.player.shield=1;m.player.invincible=0;double hp=m.player.hp;
        m.hurt(10);check(m.player.hp==hp&&m.player.shield==0,"Shield absorbs one hit without damaging hull");
        m.player.droneCount=1;m.player.droneTimer=0;m.spawnTimer=100;Enemy target=new Enemy(1,EnemyType.SEEKER,500,300,1);target.warning=0;m.enemies.add(target);
        m.update(.05,new Input());check(m.bullets.stream().anyMatch(b->!b.hostile&&b.homing>0),"Orbiting drone fires a homing round");
        GameModel u=game();u.choices.clear();u.choices.add(Upgrade.SHIELD);u.screen=Screen.UPGRADE;u.chooseUpgrade(0);check(u.player.shieldMax>=1&&u.player.shield>0,"Shield upgrade raises shield capacity");
    }
    private static void pause() {
        GameModel m=game();double time=m.elapsed,x=m.player.x;Input i=new Input();i.press(KeyEvent.VK_D);m.screen=Screen.PAUSED;m.update(1,i);
        close(time,m.elapsed,"Paused clock stays frozen");close(x,m.player.x,"Paused player stays frozen");
    }
    private static void progression() {
        GameModel m=game();Input input=new Input();
        for(int wave=1;wave<=FINAL_WAVE;wave++) {
            check(m.wave==wave,"Wave sequence stays continuous");m.enemies.clear();m.spawned=m.quota;m.bossSpawned=isBossWave(wave);m.update(.016,input);
            if(wave<FINAL_WAVE) {check(m.screen==Screen.UPGRADE&&m.choices.size()==3,"Each sector grants three distinct upgrade choices");check(new HashSet<>(m.choices).size()==3,"Upgrade choices are unique");m.chooseUpgrade(0);}
        }
        check(m.screen==Screen.RESULT&&m.won&&m.completedWaves==FINAL_WAVE,"Final sector reaches the victory ending");
        check(m.profile.victories==1&&m.profile.runs==1&&m.reward>0,"Run rewards settle exactly once");
        int credits=m.profile.credits;m.update(.05,input);check(credits==m.profile.credits,"Result cannot settle twice");
        GameModel failure=game();failure.player.hp=0;failure.update(.01,input);check(failure.screen==Screen.RESULT&&!failure.won,"Death reaches the defeat ending");
        Profile p=new Profile();p.credits=40;check(p.purchase(0)&&p.credits==0&&p.hullLevel==1,"Shop deducts credits and adds a level");check(!p.purchase(0)&&p.hullLevel==1,"Unaffordable purchase does not mutate progress");p.hullLevel=5;p.credits=10000;check(!p.purchase(0),"Maximum upgrade level is enforced");
    }
    private static void storage() throws Exception {
        Path folder=Files.createTempDirectory(Path.of("target"),"save-test-");SaveStore s=new SaveStore(folder);Profile p=new Profile();p.credits=123;p.hullLevel=3;p.settings.volume=.6;p.lastSettledRun="finished-run";
        s.saveProfile(p);Profile q=s.loadProfile();check(q.credits==123&&q.hullLevel==3&&q.settings.volume==.6&&q.lastSettledRun.equals("finished-run"),"Profile survives disk roundtrip");
        p.credits=321;s.saveProfile(p);Files.writeString(folder.resolve("profile.properties"),"broken");check(s.loadProfile().credits==123,"Malformed profile falls back to the previous backup");
        Properties c=game().checkpoint();s.saveCheckpoint(c);check(c.getProperty("seed").equals(s.loadCheckpoint().getProperty("seed")),"Checkpoint survives disk roundtrip");s.clearCheckpoint();check(s.loadCheckpoint().isEmpty(),"Completed checkpoint and backup are cleared");
        check(SaveStore.number(new Properties(),"missing",5,0,10)==5,"Missing values have safe defaults");
    }
    private static void checkpoints() {
        GameModel m=game();m.upgrades.put(Upgrade.POWER,2);m.upgrades.put(Upgrade.MULTI,1);m.upgrades.put(Upgrade.REGEN,1);m.startWave(5);m.player.hp=65;m.score=1200;m.salvage=18;
        Properties p=m.checkpoint();GameModel resumed=new GameModel(m.profile);check(resumed.resume(p),"Valid checkpoint resumes");check(resumed.wave==5&&resumed.score==1200&&resumed.salvage==18,"Checkpoint restores sector and earned score");
        close(18*Math.pow(1.22,2),resumed.player.damage,"Checkpoint rebuilds stacked damage upgrades");check(resumed.player.projectiles==2&&resumed.player.regen==.9,"Checkpoint rebuilds passive upgrades");close(65,resumed.player.hp,"Checkpoint restores hull");
        p.setProperty("hp","NaN");check(!new GameModel(new Profile()).resume(p),"Non-finite checkpoint health is rejected");p.setProperty("hp","65");m.profile.lastSettledRun=m.runId;check(!new GameModel(m.profile).resume(p),"Already-settled run cannot replay its rewards");
        p.setProperty("loadout","BAD");check(!new GameModel(new Profile()).resume(p),"Invalid enum in checkpoint is rejected");
    }
    private static void randomness() {
        GameModel a=game(),b=game();b.profile.settings.reducedMotion=true;
        for(int i=0;i<200;i++) {a.update(.016,new Input());b.update(.016,new Input());}
        check(!a.enemies.isEmpty()&&a.enemies.size()==b.enemies.size(),"Reduced motion does not change spawn count");
        for(int i=0;i<a.enemies.size();i++){Enemy x=a.enemies.get(i),y=b.enemies.get(i);check(x.type==y.type,"Seed reproduces enemy types");close(x.x,y.x,"Seed reproduces enemy positions");}
    }
    private static void simulation() {
        // 长时间真实战斗检查数值与实体上限，三个角色均使用移动、开火、闪避和脉冲。
        for(Loadout frame:Loadout.values()) {
            Profile profile=new Profile();profile.settings.difficulty=Settings.Difficulty.RELAXED;
            GameModel m=new GameModel(profile);m.newRun(frame,321);Input input=new Input();
            int steps=0;
            while(m.screen!=Screen.RESULT && steps++<120000) {
                if(m.screen==Screen.UPGRADE) {m.chooseUpgrade(0);continue;}
                Enemy nearest=m.enemies.stream().filter(e->e.warning<=0).min(Comparator.comparingDouble(e->distance(e.x,e.y,m.player.x,m.player.y))).orElse(null);
                input.clear();input.firing=nearest!=null;if(nearest!=null){input.mouseX=nearest.x;input.mouseY=nearest.y;}
                double t=m.elapsed;double targetX=640+Math.cos(t*.48)*410,targetY=425+Math.sin(t*.48)*183;
                if(targetX-m.player.x>8)input.press(KeyEvent.VK_D);if(targetX-m.player.x<-8)input.press(KeyEvent.VK_A);
                if(targetY-m.player.y>8)input.press(KeyEvent.VK_S);if(targetY-m.player.y<-8)input.press(KeyEvent.VK_W);
                if(nearest!=null&&distance(nearest.x,nearest.y,m.player.x,m.player.y)<110)input.press(KeyEvent.VK_E);
                if(nearest!=null&&distance(nearest.x,nearest.y,m.player.x,m.player.y)<60)input.press(KeyEvent.VK_SPACE);
                m.update(1.0/60,input);m.sounds.clear();input.endFrame();
                if(!Double.isFinite(m.player.x)||!Double.isFinite(m.player.hp))throw new AssertionError("Non-finite combat state");
                if(m.enemies.size()>100||m.bullets.size()>500)throw new AssertionError("Entity cap exceeded");
            }
            check(m.screen==Screen.RESULT,"Autonomous combat reaches a terminal result for "+frame);
            check(m.kills>5,"Combat kills enemies through projectile collision for "+frame);
            System.out.println("战斗模拟："+frame.label+" / 守住 "+m.completedWaves+" 区 / "+(m.won?"胜利":"失败")+" / 击败 "+m.kills+" 名敌人 / "+(int)m.elapsed+" 秒");
        }
    }
    private static void renderScreens() throws Exception {
        Path folder=Path.of("target","preview");Files.createDirectories(folder);
        GameRenderer renderer=new GameRenderer();GameModel m=game();Input input=new Input();input.mouseX=900;input.mouseY=370;
        Screen[] screens={Screen.HOME,Screen.PLAYING,Screen.UPGRADE,Screen.WORKSHOP,Screen.SETTINGS,Screen.HELP,Screen.PAUSED,Screen.RESULT,Screen.CONFIRM_NEW};
        BufferedImage sheet=new BufferedImage(1280,((screens.length+1)/2)*400,BufferedImage.TYPE_INT_RGB);Graphics2D montage=sheet.createGraphics();
        for(int index=0;index<screens.length;index++) {
            m.screen=screens[index];m.choices.clear();m.choices.addAll(java.util.List.of(Upgrade.POWER,Upgrade.MULTI,Upgrade.SHIELD));
            if(m.screen==Screen.PLAYING) {m.wave=10;m.modifier=WaveModifier.ELITE;m.bannerTimer=0;m.spawned=8;m.quota=8;m.enemies.clear();for(int i=0;i<21;i++){Enemy e=new Enemy(i+1,EnemyType.values()[i%7],170+i%6*174,228+i/6*152,10);e.warning=0;if(i%6==0)e.makeElite();m.enemies.add(e);}Enemy boss=new Enemy(50,EnemyType.BOSS,890,274,10);boss.warning=0;m.enemies.add(boss);m.player.x=603;m.player.y=428;m.player.hp=72;m.player.shieldMax=2;m.player.shield=2;m.player.overdrive=100;m.player.combo=17;m.player.droneCount=2;m.score=18725;m.salvage=48;for(int i=0;i<14;i++)m.bullets.add(new Bullet(330+i*57,358+i%3*55,50,0,12,i%3==0,0));}
            if(m.screen==Screen.RESULT){m.won=true;m.completedWaves=FINAL_WAVE;m.kills=473;m.score=42650;m.reward=621;m.perfectWaves=7;m.maxCombo=42;}
            BufferedImage image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
            var buttons=renderer.render(g,m,input,3,true,-1,"");g.dispose();check(!buttons.isEmpty(),"Screen has interactive controls: "+m.screen);
            if(m.screen==Screen.PAUSED||m.screen==Screen.UPGRADE)check(buttons.stream().noneMatch(b->b.id().equals("pause")),"Overlay blocks underlying HUD controls");
            for(UiButton b:buttons){check(b.bounds().x>=0&&b.bounds().getMaxX()<=WIDTH&&b.bounds().y>=0&&b.bounds().getMaxY()<=HEIGHT,"Button stays inside viewport: "+b.id());}
            ImageIO.write(image,"png",folder.resolve(m.screen.name().toLowerCase()+".png").toFile());
            montage.drawImage(image,(index%2)*640,(index/2)*400,640,400,null);
        }
        montage.dispose();ImageIO.write(sheet,"png",folder.resolve("contact-sheet.png").toFile());
    }
}
