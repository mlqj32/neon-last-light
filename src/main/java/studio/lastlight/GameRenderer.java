package studio.lastlight;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import static studio.lastlight.GameModel.*;

/** 所有美术由 Java2D 绘制。使用统一的虚拟分辨率，窗口缩放不改变碰撞坐标。 */
public final class GameRenderer {
    public static final Color BG=new Color(0x0a101a), PANEL=new Color(0x111d2a), TEXT=new Color(0xe6edf3), MUTED=new Color(0x91a3b7), LINE=new Color(0x263749), CYAN=new Color(0x7bffe3), GOLD=new Color(0xffc585), RED=new Color(0xff6988), BLUE=new Color(0x74c8ff), VIOLET=new Color(0xb79cff), LIME=new Color(0xaafc80);
    private Graphics2D g;
    private GameModel model;
    private Input input;
    private double time;
    private int focused;
    private final List<UiButton> buttons=new ArrayList<>();
    public List<UiButton> render(Graphics2D graphics, GameModel model, Input input, double time, boolean canContinue, int focused, String notice) {
        this.g=(Graphics2D)graphics.create();this.model=model;this.input=input;this.time=model.profile.settings.reducedMotion?0:time;this.focused=focused;buttons.clear();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        background();
        switch(model.screen) {
            case HOME -> home(canContinue);
            case PLAYING -> {arena();hud();}
            case PAUSED -> {arena();hud();buttons.clear();veil();pause();}
            case UPGRADE -> {arena();hud();buttons.clear();veil();upgrade();}
            case WORKSHOP -> workshop();
            case SETTINGS -> settings();
            case HELP -> help();
            case RESULT -> result();
            case CONFIRM_NEW -> {home(canContinue);buttons.clear();veil();confirm();}
        }
        if(!notice.isBlank()) {box(260,750,760,35,PANEL);center(notice,640,773,13,GOLD,Font.PLAIN);}
        g.dispose();return List.copyOf(buttons);
    }
    private void background() {
        g.setPaint(new GradientPaint(0,0,BG,1280,800,new Color(0x102737)));g.fillRect(0,0,WIDTH,HEIGHT);
        g.setColor(new Color(0x142a37));g.setStroke(new BasicStroke(1));
        for(int x=0;x<WIDTH;x+=40) g.drawLine(x,0,x,HEIGHT);
        for(int y=0;y<HEIGHT;y+=40) g.drawLine(0,y,WIDTH,y);
        for(int i=0;i<68;i++) {double x=(i*197.37)%WIDTH,y=(i*i*73.19)%HEIGHT;g.setColor(alpha(CYAN,.12+(i%5)*.035));g.fill(new Ellipse2D.Double(x,y,2,2));}
        g.setColor(alpha(BG,.3));g.fillRect(0,0,WIDTH,HEIGHT);
    }
    private void page(String section,String subtitle) {
        text("最后的光  /  前线行动",52,48,14,CYAN,Font.BOLD);
        text(section,52,116,42,TEXT,Font.BOLD);text(subtitle,54,146,16,MUTED,Font.PLAIN);
        line(52,176,1228,176,LINE);
        footer();
    }
    private void footer() {
        line(52,744,1228,744,LINE);text("霓虹：最后的光",52,772,14,MUTED,Font.BOLD);
        text("离线单机  /  版本 2.0.0",1010,772,14,MUTED,Font.PLAIN);
    }
    private void home(boolean canContinue) {
        text("轨道防御部队",64,67,15,CYAN,Font.BOLD);
        text("霓虹",58,191,108,TEXT,Font.BOLD);
        text("最后的光",64,245,44,CYAN,Font.BOLD);
        text("一名飞行员，二十道防线。",67,290,19,TEXT,Font.PLAIN);
        text("抵御敌群，强化飞船，击败四名首领。",67,321,17,MUTED,Font.PLAIN);
        button("start","出击  /  开始新游戏",66,371,490,59,true,true);
        button("continue",canContinue?"继续游戏  /  从波次存档恢复":"继续游戏  /  暂无存档",66,442,490,48,canContinue,false);
        button("workshop","改装工坊",66,503,238,46,true,false);
        button("help","飞行手册",318,503,238,46,true,false);
        button("settings","设置",66,562,238,46,true,false);
        button("exit","退出游戏",318,562,238,46,true,false);
        text("最高得分",67,659,14,MUTED,Font.BOLD);text(String.format("%,d",model.profile.bestScore),67,690,26,TEXT,Font.BOLD);
        text("最多守住区域",263,659,14,MUTED,Font.BOLD);text(model.profile.bestWave+" / "+FINAL_WAVE,263,690,26,TEXT,Font.BOLD);
        text("胜利次数",468,659,14,MUTED,Font.BOLD);text(Integer.toString(model.profile.victories),468,690,26,GOLD,Font.BOLD);
        double cx=953,cy=286;
        for(int i=0;i<4;i++) {g.setColor(alpha(CYAN,.13-i*.02));g.setStroke(new BasicStroke(1));g.draw(new Ellipse2D.Double(cx-92-i*24,cy-92-i*24,184+i*48,184+i*48));}
        g.setColor(alpha(CYAN,.2));g.draw(new Arc2D.Double(cx-158,cy-158,316,316,time*12,105,Arc2D.OPEN));
        ship(cx,cy,-Math.PI/2,3.8,CYAN,false);
        text("选择你的机体",690,452,15,CYAN,Font.BOLD);
        for(int i=0;i<Loadout.values().length;i++) {
            Loadout l=Loadout.values()[i];int col=i%3,row=i/3,x=689+col*176,y=467+row*112;
            boolean selected=model.loadout==l;
            box(x,y,162,104,selected?new Color(0x183738):PANEL);
            g.setColor(selected?CYAN:LINE);g.drawRoundRect(x,y,162,104,12,12);
            text(l.label,x+14,y+24,18,selected?CYAN:TEXT,Font.BOLD);
            text(l.role,x+14,y+45,12,MUTED,Font.BOLD);
            text("生命 "+(int)l.hp+"  /  速度 "+(int)l.speed,x+14,y+64,10,MUTED,Font.PLAIN);
            button("loadout"+i,selected?"已选择":"选择",x+12,y+72,138,23,true,selected);
        }
        text("难度  /  "+model.profile.settings.difficulty.label,691,704,14,MUTED,Font.BOLD);
        text("WASD 移动 · 鼠标射击 · 空格闪避 · E 脉冲 · R 超载",691,726,13,MUTED,Font.PLAIN);
        footer();
    }
    private void arena() {
        int x=(int)LEFT,y=(int)TOP,w=(int)(RIGHT-LEFT),h=(int)(BOTTOM-TOP);
        box(x,y,w,h,new Color(0x0b1722));
        g.setColor(new Color(model.profile.settings.highContrast?0x335362:0x17303e));g.setStroke(new BasicStroke(1));
        for(int gx=x+24;gx<x+w;gx+=48) g.drawLine(gx,y,gx,y+h);
        for(int gy=y+24;gy<y+h;gy+=48) g.drawLine(x,gy,x+w,gy);
        g.setColor(alpha(CYAN,.28));g.drawRoundRect(x,y,w,h,14,14);
        for(int i=0;i<4;i++) {int ox=i%2==0?x:x+w,oy=i<2?y:y+h;g.setStroke(new BasicStroke(3));g.setColor(CYAN);g.drawLine(ox,oy,ox+(i%2==0?20:-20),oy);g.drawLine(ox,oy,ox,oy+(i<2?20:-20));}
        Shape previousClip=g.getClip();g.clip(new Rectangle2D.Double(LEFT,TOP,RIGHT-LEFT,BOTTOM-TOP));
        AffineTransform original=g.getTransform();
        if(model.shake>0&&!model.profile.settings.reducedMotion) g.translate(Math.sin(time*83)*model.shake,Math.cos(time*71)*model.shake);
        for(Drop d:model.drops) {
            Color c=switch(d.type) { case SALVAGE -> GOLD; case REPAIR -> LIME; case SHIELD -> BLUE; case OVERDRIVE -> VIOLET; };
            double bob=Math.sin(time*4+d.x)*2;
            g.setColor(alpha(c,.13));g.fill(new Ellipse2D.Double(d.x-13,d.y-13+bob,26,26));
            if(d.type==DropType.REPAIR) {g.setColor(c);g.fill(new Rectangle2D.Double(d.x-2,d.y-6+bob,4,12));g.fill(new Rectangle2D.Double(d.x-6,d.y-2+bob,12,4));}
            else if(d.type==DropType.SHIELD) {g.setColor(c);g.setStroke(new BasicStroke(2));g.draw(new Ellipse2D.Double(d.x-7,d.y-7+bob,14,14));g.draw(new Ellipse2D.Double(d.x-3,d.y-3+bob,6,6));}
            else if(d.type==DropType.OVERDRIVE) {polygon(d.x,d.y+bob,8,5,-Math.PI/2,c,true);}
            else polygon(d.x,d.y+bob,7,4,Math.PI/4,c,true);
        }
        for(Enemy e:model.enemies) enemy(e);
        for(Bullet b:model.bullets) {
            Color c=b.hostile?RED:b.homing>0?VIOLET:CYAN;g.setColor(alpha(c,.18));g.fill(new Ellipse2D.Double(b.x-9,b.y-9,18,18));
            g.setStroke(new BasicStroke(b.hostile?4:3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(c);
            double speed=Math.max(1,Math.hypot(b.vx,b.vy));g.draw(new Line2D.Double(b.x-b.vx/speed*10,b.y-b.vy/speed*10,b.x,b.y));
        }
        for(Particle p:model.particles) {g.setColor(alpha(new Color(p.color),p.life/p.maxLife));g.fill(new Ellipse2D.Double(p.x-2,p.y-2,4,4));}
        Player p=model.player;
        double aim=Math.atan2(input.mouseY-p.y,input.mouseX-p.x);
        if(p.overdriveTimer>0) {
            g.setColor(alpha(GOLD,.18));g.fill(new Ellipse2D.Double(p.x-34,p.y-34,68,68));
            g.setColor(alpha(GOLD,.65));g.setStroke(new BasicStroke(2));g.draw(new Arc2D.Double(p.x-31,p.y-31,62,62,time*220,250,Arc2D.OPEN));
        }
        if(p.shield>0) {
            g.setColor(alpha(BLUE,.42));g.setStroke(new BasicStroke(2));
            for(int i=0;i<p.shield;i++) g.draw(new Ellipse2D.Double(p.x-27-i*4,p.y-27-i*4,54+i*8,54+i*8));
        }
        for(int i=0;i<p.droneCount;i++) {
            double a=p.droneAngle+i*Math.PI*2/Math.max(1,p.droneCount),dx=p.x+Math.cos(a)*46,dy=p.y+Math.sin(a)*46;
            g.setColor(alpha(VIOLET,.18));g.fill(new Ellipse2D.Double(dx-10,dy-10,20,20));
            g.setColor(VIOLET);g.setStroke(new BasicStroke(2));g.draw(new Line2D.Double(dx-5,dy+5,dx+5,dy-5));g.draw(new Line2D.Double(dx-5,dy-5,dx+5,dy+5));
        }
        if(p.invincible>0) {g.setColor(alpha(CYAN,.5));g.setStroke(new BasicStroke(1.5f));g.draw(new Ellipse2D.Double(p.x-24,p.y-24,48,48));}
        ship(p.x,p.y,aim,1,p.invincible>0&&((int)(p.invincible*15)%2==0)?TEXT:CYAN,p.dashDuration>0);
        if(model.pulseRing>0) {g.setColor(alpha(CYAN,.45));g.setStroke(new BasicStroke(3));double r=245-model.pulseRing;g.draw(new Ellipse2D.Double(p.x-r,p.y-r,r*2,r*2));}
        g.setTransform(original);g.setClip(previousClip);
        if(model.hitFlash>0) {g.setColor(alpha(RED,model.hitFlash*.35));g.fillRect(x,y,w,h);}
        if(input.mouseX>=LEFT && input.mouseX<=RIGHT && input.mouseY>=TOP && input.mouseY<=BOTTOM) {
            double mx=input.mouseX,my=input.mouseY;g.setColor(alpha(TEXT,.7));g.setStroke(new BasicStroke(1));
            g.draw(new Ellipse2D.Double(mx-8,my-8,16,16));g.draw(new Line2D.Double(mx-12,my,mx+12,my));g.draw(new Line2D.Double(mx,my-12,mx,my+12));
        }
        if(model.bannerTimer>0) {box(392,350,496,78,alpha(BG,.92));center(model.banner,640,395,22,CYAN,Font.BOLD);}
    }
    private void enemy(Enemy e) {
        Color c=e.flash>0?TEXT:e.type==EnemyType.BOSS?GOLD:e.type==EnemyType.SNIPER?VIOLET:e.type==EnemyType.SENTINEL?BLUE:e.type==EnemyType.HIVE?LIME:e.type==EnemyType.WEAVER?CYAN:RED;
        if(e.warning>0) {
            double r=e.radius+10+Math.sin(time*10)*4;g.setColor(alpha(c,.5));g.setStroke(new BasicStroke(2));
            g.draw(new Ellipse2D.Double(e.x-r,e.y-r,r*2,r*2));text("!",e.x-4,e.y+5,16,c,Font.BOLD);return;
        }
        g.setColor(alpha(c,.1));g.fill(new Ellipse2D.Double(e.x-e.radius*1.6,e.y-e.radius*1.6,e.radius*3.2,e.radius*3.2));
        if(e.elite) {g.setColor(alpha(GOLD,.8));g.setStroke(new BasicStroke(2));g.draw(new Ellipse2D.Double(e.x-e.radius-6,e.y-e.radius-6,e.radius*2+12,e.radius*2+12));}
        switch(e.type) {
            case SEEKER -> polygon(e.x,e.y,e.radius,4,Math.PI/4,c,true);
            case HUNTER -> polygon(e.x,e.y,e.radius,3,Math.atan2(model.player.y-e.y,model.player.x-e.x),c,true);
            case BRUTE -> {polygon(e.x,e.y,e.radius,6,e.age*.1,c,true);polygon(e.x,e.y,e.radius*.6,6,e.age*.1,BG,true);}
            case SNIPER -> {polygon(e.x,e.y,e.radius,4,e.age*.5,c,false);polygon(e.x,e.y,5,4,e.age*.5,c,true);}
            case WEAVER -> {polygon(e.x,e.y,e.radius,5,e.age*.8,c,false);polygon(e.x,e.y,e.radius*.45,5,-e.age*.8,c,true);}
            case SENTINEL -> {
                polygon(e.x,e.y,e.radius,6,e.age*.15,c,true);polygon(e.x,e.y,e.radius*.55,6,e.age*.15,BG,true);
                if(e.shield>0) {g.setColor(alpha(BLUE,.32+.12*e.shield/Math.max(1,e.shieldMax)));g.setStroke(new BasicStroke(2));g.draw(new Ellipse2D.Double(e.x-e.radius-5,e.y-e.radius-5,(e.radius+5)*2,(e.radius+5)*2));for(int i=0;i<e.shieldMax;i++){g.setColor(i<e.shield?BLUE:LINE);g.fill(new Ellipse2D.Double(e.x-e.shieldMax*5+i*10,e.y+e.radius+9,6,6));}}
            }
            case HIVE -> {g.setColor(alpha(LIME,.22));g.fill(new Ellipse2D.Double(e.x-e.radius-5,e.y-e.radius-5,(e.radius+5)*2,(e.radius+5)*2));polygon(e.x,e.y,e.radius,6,e.age*.35,c,true);polygon(e.x,e.y,e.radius*.42,6,-e.age*.5,BG,true);}
            case BOSS -> {polygon(e.x,e.y,e.radius,8,-e.age*.3,c,true);polygon(e.x,e.y,e.radius*.77,8,e.age*.25,BG,true);polygon(e.x,e.y,e.radius*.38,4,e.age*.5,c,true);}
        }
        if(e.hp<e.maxHp && e.type!=EnemyType.BOSS) bar(e.x-e.radius,e.y-e.radius-10,e.radius*2,3,e.hp/e.maxHp,e.elite?GOLD:c);
        if(e.shieldMax>0 && e.shield>0 && e.type!=EnemyType.BOSS) bar(e.x-e.radius,e.y-e.radius-16,e.radius*2,3,e.shield/(double)e.shieldMax,BLUE);
    }
    private void ship(double x,double y,double angle,double scale,Color color,boolean dash) {
        AffineTransform old=g.getTransform();g.translate(x,y);g.rotate(angle);g.scale(scale,scale);
        g.setColor(alpha(color,.12));g.fillOval(-28,-28,56,56);
        Path2D shape=new Path2D.Double();shape.moveTo(20,0);shape.lineTo(-12,-12);shape.lineTo(-5,0);shape.lineTo(-12,12);shape.closePath();
        g.setColor(BG);g.fill(shape);g.setColor(color);g.setStroke(new BasicStroke(2));g.draw(shape);
        g.setColor(TEXT);g.fill(new Ellipse2D.Double(0,-3,6,6));
        g.setColor(alpha(color,.6));Path2D flame=new Path2D.Double();flame.moveTo(-10,-4);flame.lineTo(dash?-37:-20-3*Math.sin(time*12),0);flame.lineTo(-10,4);g.fill(flame);
        g.setTransform(old);
    }
    private void hud() {
        text("霓虹 / 最后的光",58,46,15,CYAN,Font.BOLD);
        text("区域 "+String.format("%02d",model.wave)+" / "+FINAL_WAVE,58,90,27,TEXT,Font.BOLD);
        text("装甲生命",338,46,13,MUTED,Font.BOLD);text((int)Math.ceil(model.player.hp)+" / "+(int)model.player.maxHp,338,70,18,TEXT,Font.BOLD);
        bar(338,84,230,9,model.player.hp/model.player.maxHp,model.player.hp/model.player.maxHp<.3?RED:CYAN);
        if(model.player.shieldMax>0) {
            text("护盾",580,46,13,MUTED,Font.BOLD);
            for(int i=0;i<model.player.shieldMax;i++) {g.setColor(i<model.player.shield?BLUE:LINE);g.fill(new Ellipse2D.Double(580+i*18,56,12,12));}
        }
        text("得分",646,46,13,MUTED,Font.BOLD);text(String.format("%,d",model.score),646,80,25,TEXT,Font.BOLD);
        text("回收物",819,46,13,MUTED,Font.BOLD);text(Integer.toString(model.salvage),819,80,25,GOLD,Font.BOLD);
        int seconds=(int)model.elapsed;text(String.format("%02d:%02d",seconds/60,seconds%60),990,77,23,MUTED,Font.PLAIN);
        button("pause","暂停",1110,42,111,44,true,false);
        text("场上敌人 "+model.enemies.size()+"  /  已出现 "+Math.min(model.spawned,model.quota)+" / "+model.quota+(model.player.combo>1?"   连击 x"+model.player.combo:""),58,123,13,MUTED,Font.PLAIN);
        text(model.modifier.label+" · "+model.modifier.description,338,123,12,model.modifier==WaveModifier.STANDARD?MUTED:GOLD,Font.PLAIN);
        model.enemies.stream().filter(e->e.type==EnemyType.BOSS).findFirst().ifPresent(e->{
            center(bossName(model.wave),830,111,11,GOLD,Font.BOLD);bar(649,120,364,5,e.hp/e.maxHp,GOLD);
        });
        ability(58,718,"空格  /  闪避",model.player.dashTimer,model.player.dashCooldown,CYAN);
        ability(339,718,"E  /  脉冲",model.player.pulseTimer,model.player.pulseCooldown,GOLD);
        text("R  /  超载",620,735,14,model.player.overdrive>=100?VIOLET:MUTED,Font.BOLD);
        text(model.player.overdriveTimer>0?String.format("%.1f秒",model.player.overdriveTimer):model.player.overdrive>=100?"就绪":(int)model.player.overdrive+"%",794,735,14,model.player.overdrive>=100?VIOLET:MUTED,Font.BOLD);
        bar(620,745,240,4,model.player.overdrive/100,VIOLET);
        text("WASD 或方向键移动 · 鼠标左键射击",620,770,14,MUTED,Font.PLAIN);
        text(model.loadout.label+"  /  "+model.runDifficulty.label,1030,770,14,MUTED,Font.BOLD);
        text("保持移动。闪避期间无敌，脉冲可以清除附近的敌方子弹；击杀积累超载。",58,790,13,MUTED,Font.PLAIN);
    }
    private void ability(int x,int y,String name,double remaining,double maximum,Color c) {
        text(name,x,y+17,14,c,Font.BOLD);text(remaining<=0?"就绪":String.format("%.1f秒",remaining),x+174,y+17,14,remaining<=0?c:MUTED,Font.BOLD);
        bar(x,y+27,240,4,1-remaining/maximum,c);
    }
    private void veil() {g.setColor(alpha(BG,.88));g.fillRect(0,0,WIDTH,HEIGHT);}
    private void pause() {
        center("行动待命",640,217,15,CYAN,Font.BOLD);center("已暂停",640,271,44,TEXT,Font.BOLD);
        center("从存档继续时，会从当前波次的开头重新开始。",640,311,15,MUTED,Font.PLAIN);
        button("resume","继续战斗",440,351,400,52,true,true);
        button("settings","设置",440,417,400,48,true,false);
        button("help","飞行手册",440,477,400,48,true,false);
        button("home","返回基地  /  保留波次存档",440,537,400,48,true,false);
        center("退出键或 P 键：继续战斗",640,640,14,MUTED,Font.PLAIN);
    }
    private void upgrade() {
        center("第 "+String.format("%02d",model.wave)+" 区已守住",640,183,15,CYAN,Font.BOLD);
        center("强化你的优势",640,241,43,TEXT,Font.BOLD);
        center("选择一项强化。本波结束已恢复 18 点生命。",640,276,16,MUTED,Font.PLAIN);
        for(int i=0;i<model.choices.size();i++) {
            Upgrade u=model.choices.get(i);int x=140+i*340;
            box(x,319,320,302,PANEL);text("0"+(i+1)+"  /  前线强化",x+24,357,14,CYAN,Font.BOLD);
            upgradeIcon(x+58,407,u);
            text(u.title,x+24,462,26,TEXT,Font.BOLD);wrap(u.benefit,x+24,493,275,15,CYAN);
            wrap(u.description,x+24,536,273,14,MUTED);
            button("upgrade"+i,"选择  ["+(i+1)+"]",x+24,567,272,39,true,true);
        }
        center("强化可以叠加。不同的搭配，将带来不同的作战方式。",640,677,16,MUTED,Font.PLAIN);
    }
    private void upgradeIcon(int x,int y,Upgrade u) {
        int sides=switch(u) { case POWER,RAPID -> 4; case HULL,SHIELD -> 6; case SPEED,DASH -> 3; case MULTI,ORBIT -> 5; case PIERCE,HOMING -> 8; default -> 7; };
        Color c=switch(u) { case CRIT,CHAIN -> VIOLET; case SHIELD,HOMING -> BLUE; case LIFESTEAL,REGEN -> LIME; default -> CYAN; };
        polygon(x,y,25,sides,time*.15,c,false);polygon(x,y,10,4,Math.PI/4,c,true);
    }
    private void workshop() {
        page("改装工坊", "守住区域赚取信用点。永久升级会在每次新游戏中生效。");
        text("可用信用点",54,229,14,MUTED,Font.BOLD);text(String.format("%,d",model.profile.credits),54,269,34,GOLD,Font.BOLD);
        String[] names={"加固装甲","矢量反应堆","武器校准"};String[] desc={"每级增加 8 点生命上限","每级缩短 5% 闪避冷却时间","每级提高 5% 基础伤害"};
        int[] levels={model.profile.hullLevel,model.profile.reactorLevel,model.profile.weaponLevel};
        for(int i=0;i<3;i++) {
            int x=54+i*395;box(x,308,370,320,PANEL);upgradeIcon(x+49,361,Upgrade.values()[i]);
            text(names[i],x+25,426,25,TEXT,Font.BOLD);text(desc[i],x+25,457,15,MUTED,Font.PLAIN);
            for(int j=0;j<5;j++) {g.setColor(j<levels[i]?CYAN:LINE);g.fillRoundRect(x+25+j*59,483,48,8,4,4);}
            text("等级 "+levels[i]+" / 5",x+25,530,15,CYAN,Font.BOLD);
            int cost=model.profile.cost(levels[i]);boolean available=levels[i]<5&&model.profile.credits>=cost;
            button("buy"+i,levels[i]==5?"已达最高等级":"升级  /  "+cost+" 信用点",x+25,557,320,45,available,true);
        }
        button("back","返回基地",54,670,230,44,true,false);
        text("六种机体均可受益，永久升级不会过期。",331,698,15,MUTED,Font.PLAIN);
    }
    private void settings() {
        Settings s=model.profile.settings;
        page("游戏设置", "设置保存在本地。调整难度会在下一次新游戏中生效。");
        String[] labels={"难度", "音效", "背景音乐", "总音量", "减少动态效果", "高对比度网格"};
        String[] values={s.difficulty.label, s.sound?"开启":"关闭", s.music?"开启":"关闭", (int)Math.round(s.volume*100)+"%", s.reducedMotion?"开启":"关闭", s.highContrast?"开启":"关闭"};
        String[] ids={"difficulty","sound","music","volume","motion","contrast"};
        for(int i=0;i<labels.length;i++) {
            int y=205+i*71;box(54,y,760,59,PANEL);text(labels[i],75,y+36,13,MUTED,Font.BOLD);
            if(i==3) {button("volumeDown","-",540,y+9,55,41,true,false);center(values[i],650,y+36,16,TEXT,Font.BOLD);button("volumeUp","+",704,y+9,82,41,true,false);}
            else button(ids[i],values[i],540,y+9,246,41,true,false);
        }
        text("视觉辅助",881,231,15,CYAN,Font.BOLD);wrap("减少动态效果会关闭屏幕震动、粒子和背景动画。高对比度设置会增强战场网格的可见度。",881,268,317,16,MUTED);
        text("本地存档",881,410,15,CYAN,Font.BOLD);wrap("无需账号，不采集数据，不发送网络请求。进度保存在项目或应用旁的本地存档文件夹中。",881,447,317,16,MUTED);
        button("back","返回",54,670,230,44,true,false);
    }
    private void help() {
        page("飞行手册", "守住全部二十个区域，击败四名首领，让最后的光重新亮起。");
        String[][] controls={{"WASD / 方向键","移动飞船"},{"鼠标","瞄准武器"},{"鼠标左键 / J","按住连续射击"},{"空格","闪避，短时间无敌"},{"E / Q","脉冲，伤害敌人并清除附近子弹"},{"R","超载：击杀充能，满槽后短时间爆发"},{"退出键 / P","暂停或继续"},{"1–3 / 1–6","选择强化 / 选择机体"},{"制表键 / 回车","切换菜单按钮或确认"}};
        text("飞行操作",54,218,14,CYAN,Font.BOLD);
        for(int i=0;i<controls.length;i++) {int y=248+i*44;line(54,y+15,624,y+15,LINE);text(controls[i][0],54,y,14,TEXT,Font.BOLD);text(controls[i][1],270,y,13,MUTED,Font.PLAIN);}
        text("识别敌人 / 留意形状",704,218,14,CYAN,Font.BOLD);
        String[] names={"追踪者 / 菱形","猎手 / 三角形","重甲兵 / 六边形","狙击手 / 旋转方形","编织者 / 五角星","哨戒机 / 蓝盾六边形","巢母 / 绿色核心","精英 / 金色外环"};
        String[] tips={"持续追踪你的位置。","向你先前的位置快速冲刺。","行动缓慢、装甲厚重，近距离威胁很大。","保持距离，发射红色子弹。","横向游走并发射三向弹幕。","携带可再生的蓝色护盾，先破盾。","持续放出小型追踪者，优先击杀。","生命和速度更高，掉落更多回收物。"};
        for(int i=0;i<names.length;i++) {int y=248+i*39;text(names[i],704,y,14,i>=5?BLUE:RED,Font.BOLD);text(tips[i],704,y+17,12,MUTED,Font.PLAIN);}
        text("四名首领：棱镜 / 零 / 弥撒 / 终末守望者",704,590,14,GOLD,Font.BOLD);
        wrap("绿色十字恢复 20 生命，蓝色圆环补充护盾，紫色星补充超载，金色菱形是回收物。红色圆环提示敌人即将出现。进入吸附范围会自动拾取。",704,616,494,13,MUTED);
        button("back","明白了",54,670,230,44,true,true);
        text("存档记录波次开头的状态。返回基地或关闭窗口后，可继续该波次。",324,698,14,MUTED,Font.PLAIN);
    }
    private void result() {
        text("飞行记录 / "+model.loadout.label,65,69,15,CYAN,Font.BOLD);
        text(model.won?"光芒":"信号",61,180,82,TEXT,Font.BOLD);
        text(model.won?"归来":"消失",65,257,72,model.won?CYAN:RED,Font.BOLD);
        text(model.won?"二十道防线已守住，终末守望者已被击败。":"舰队记得你的奋战。改装飞船，再次出击。",67,306,18,MUTED,Font.PLAIN);
        String[] labels={"得分","守住区域","击败敌人","获得信用点","完美区域","最高连击"};
        String[] values={String.format("%,d",model.score),model.completedWaves+" / "+FINAL_WAVE,Integer.toString(model.kills),"+"+model.reward,Integer.toString(model.perfectWaves),"x"+model.maxCombo};
        for(int i=0;i<6;i++) {int x=66+(i%3)*282,y=355+(i/3)*92;box(x,y,262,76,PANEL);text(labels[i],x+19,y+25,11,MUTED,Font.BOLD);text(values[i],x+19,y+59,25,i==3?GOLD:TEXT,Font.BOLD);}
        button("retry","再次出击",66,615,262,52,true,true);button("home","返回基地",348,615,262,52,true,false);
        double x=963,y=404;for(int i=0;i<5;i++) {double r=90+i*31;g.setColor(alpha(model.won?CYAN:RED,.16-i*.02));g.setStroke(new BasicStroke(2));g.draw(new Ellipse2D.Double(x-r,y-r,2*r,2*r));}
        ship(x,y,-Math.PI/2,4,model.won?CYAN:RED,false);
        center(model.won?"任务完成":"通讯中断",963,672,15,model.won?CYAN:RED,Font.BOLD);footer();
    }
    private void confirm() {
        box(382,230,516,333,PANEL);center("替换当前波次存档？",640,298,32,TEXT,Font.BOLD);
        center("开始新游戏会替换尚未完成的飞行存档。",640,340,16,MUTED,Font.PLAIN);
        button("confirmStart","开始新游戏",429,395,422,50,true,true);button("cancel","保留现有存档",429,462,422,44,true,false);
    }
    private void button(String id,String label,int x,int y,int width,int height,boolean enabled,boolean primary) {
        UiButton b=new UiButton(id,label,new Rectangle2D.Double(x,y,width,height),enabled,primary);
        boolean hovered=b.contains(input.mouseX,input.mouseY), focus=buttons.size()==focused;buttons.add(b);
        Color fill=enabled?(primary?(hovered?new Color(0xb1ffe9):CYAN):(hovered?new Color(0x223948):PANEL)):new Color(0x13202b);
        box(x,y,width,height,fill);
        if(!primary) {g.setStroke(new BasicStroke(1));g.setColor(enabled?(hovered?CYAN:LINE):LINE);g.drawRoundRect(x,y,width,height,10,10);}
        if(focus&&enabled) {g.setStroke(new BasicStroke(2));g.setColor(GOLD);g.drawRoundRect(x-4,y-4,width+8,height+8,13,13);}
        center(label,x+width/2.0,y+height/2.0+5,height<30?11:16,enabled?(primary?BG:TEXT):MUTED,Font.BOLD);
    }
    private void box(double x,double y,double w,double h,Color c) {g.setColor(c);g.fill(new RoundRectangle2D.Double(x,y,w,h,12,12));}
    private void bar(double x,double y,double w,double h,double fraction,Color c) {box(x,y,w,h,LINE);if(fraction>0)box(x,y,w*Math.clamp(fraction,0,1),h,c);}
    private void line(int x,int y,int a,int b,Color c) {g.setStroke(new BasicStroke(1));g.setColor(c);g.drawLine(x,y,a,b);}
    private void polygon(double x,double y,double radius,int sides,double angle,Color c,boolean fill) {
        Path2D p=new Path2D.Double();for(int i=0;i<sides;i++) {double a=angle+i*Math.PI*2/sides;if(i==0)p.moveTo(x+Math.cos(a)*radius,y+Math.sin(a)*radius);else p.lineTo(x+Math.cos(a)*radius,y+Math.sin(a)*radius);}p.closePath();g.setColor(c);g.setStroke(new BasicStroke(2));if(fill)g.fill(p);else g.draw(p);
    }
    private void text(String value,double x,double y,int size,Color c,int style) {g.setFont(new Font("Microsoft YaHei",style,size));g.setColor(c);g.drawString(value,(float)x,(float)y);}
    private void center(String value,double x,double y,int size,Color c,int style) {g.setFont(new Font("Microsoft YaHei",style,size));g.setColor(c);g.drawString(value,(float)(x-g.getFontMetrics().stringWidth(value)/2.0),(float)y);}
    private void wrap(String value,int x,int y,int width,int size,Color c) {
        g.setFont(new Font("Microsoft YaHei",Font.PLAIN,size));String line="";
        // 中文段落没有空格，按完整 Unicode 字符的实际绘制宽度换行。
        for(int offset=0;offset<value.length();) {int point=value.codePointAt(offset);String word=new String(Character.toChars(point));offset+=Character.charCount(point);String next=line+word;if(g.getFontMetrics().stringWidth(next)>width&&!line.isEmpty()) {text(line,x,y,size,c,Font.PLAIN);y+=size+8;line=word.stripLeading();}else line=next;}
        if(!line.isEmpty())text(line,x,y,size,c,Font.PLAIN);
    }
    private static Color alpha(Color c,double alpha) {return new Color(c.getRed(),c.getGreen(),c.getBlue(),(int)(Math.clamp(alpha,0,1)*255));}
}
