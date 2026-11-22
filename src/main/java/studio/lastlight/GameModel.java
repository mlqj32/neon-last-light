package studio.lastlight;

import java.awt.event.KeyEvent;
import java.util.*;

/** 游戏规则与界面分离。所有更新在 Swing 事件线程执行，不启动第二个逻辑线程。 */
public final class GameModel {
    public static final int WIDTH = 1280, HEIGHT = 800, FINAL_WAVE = 20, BOSS_INTERVAL = 5;
    public static final double LEFT = 58, RIGHT = 1222, TOP = 145, BOTTOM = 699;
    public enum Screen { HOME, PLAYING, PAUSED, UPGRADE, WORKSHOP, SETTINGS, HELP, RESULT, CONFIRM_NEW }
    public enum Loadout {
        STRIKER("突击者", "均衡可靠", "精准的全能机体，配备速射脉冲步枪。", 100, 252, 18, .21),
        BULWARK("壁垒", "重装火力", "加固装甲，速度较慢，但耐久和单发伤害更高。", 150, 208, 26, .29),
        WRAITH("幻影", "高速突袭", "轻型截击机，机动与射速出色，但装甲较弱。", 80, 298, 13, .14),
        LANCER("长枪", "贯穿狙击", "相位长枪发射高伤弹丸，自带两层穿透，射速偏慢。", 90, 232, 34, .38),
        NOVA("新星", "脉冲专精", "脉冲电容过载，范围伤害更高、冷却更短，擅长清场。", 95, 246, 15, .22),
        AEGIS("神盾", "护盾支援", "开局携带一层相位护盾，装甲厚实，适合稳扎稳打。", 120, 218, 16, .25);
        public final String label, role, description;
        public final double hp, speed, damage, interval;
        Loadout(String label, String role, String description, double hp, double speed, double damage, double interval) {
            this.label = label; this.role = role; this.description = description;
            this.hp = hp; this.speed = speed; this.damage = damage; this.interval = interval;
        }
    }
    public enum Upgrade {
        POWER("武器过载", "弹丸伤害提高 22%", "每一发命中，都更具威力。", 5),
        RAPID("快速循环", "射速提高约 18%", "压缩发射间隔，持续压制敌群。", 5),
        HULL("装甲强化", "生命上限增加 25，回复 25 生命", "在敌群包围中争取生存空间。", 5),
        SPEED("加力推进", "移动速度提高 12%", "保持机动，别让敌群封锁退路。", 4),
        MULTI("分裂发射", "每次射击额外发射 1 枚弹丸", "用更宽的火力覆盖敌方阵线。", 2),
        PIERCE("相位弹丸", "每枚弹丸额外穿透 1 个敌人", "让一条直线上的敌人承受打击。", 3),
        REGEN("纳米修复", "每秒回复 0.9 生命", "在战斗间隙持续修复装甲。", 4),
        MAGNET("牵引力场", "拾取范围增加 65", "无需改变航线，也能回收物资。", 3),
        DASH("矢量驱动", "闪避冷却缩短 18%", "更频繁地穿越危险，摆脱围困。", 4),
        PULSE("脉冲电容", "脉冲伤害增加 35，冷却缩短 12%", "关键时刻，用更强的冲击清场。", 4),
        CRIT("弱点扫描", "暴击率提高 10%，暴击造成 1.8 倍伤害", "瞄准结构弱点，让每发子弹更致命。", 5),
        CHAIN("链式电弧", "命中时电弧跳跃到附近敌人，可叠加", "敌群越密集，电弧越难停下。", 3),
        HOMING("智能弹道", "弹丸获得轻微追踪能力", "即使瞄准稍有偏差，弹丸也会修正航线。", 2),
        SHIELD("相位护盾", "护盾上限增加 1 层，每 14 秒补充 1 层", "用一层护盾换一次重新调整的机会。", 3),
        LIFESTEAL("吸血纳米", "每次击杀回复 1.2 生命", "把敌人的残骸转化为修复材料。", 3),
        ORBIT("护卫无人机", "增加 1 架环绕无人机，自动射击", "让无人机替你照顾侧翼的敌人。", 3),
        THORNS("反应装甲", "受击时释放电爆，伤害附近敌人", "被近身不等于任人宰割。", 2);
        public final String title, benefit, description; public final int cap;
        Upgrade(String title, String benefit, String description, int cap) { this.title=title; this.benefit=benefit; this.description=description; this.cap=cap; }
    }
    public enum EnemyType { SEEKER, HUNTER, BRUTE, SNIPER, WEAVER, SENTINEL, HIVE, BOSS }
    public enum DropType { SALVAGE, REPAIR, SHIELD, OVERDRIVE }
    public enum WaveModifier {
        STANDARD("标准战区", "没有额外修正"),
        SWARM("蜂群警报", "敌群数量增加，出现更密集"),
        ELITE("精英猎杀", "精英敌人出现率大幅提高"),
        ION_STORM("离子风暴", "敌方弹速提高，回收物价值增加");
        public final String label, description;
        WaveModifier(String label, String description) { this.label=label; this.description=description; }
    }
    public static final class Player {
        public double x = WIDTH / 2.0, y = 425, hp, maxHp, speed, damage, interval;
        public double shootTimer, dashTimer, dashDuration, dashX, dashY, invincible, pulseTimer;
        public int projectiles = 1, pierce;
        public double regen, pickupRadius = 78, dashCooldown = 2.6, pulseCooldown = 16, pulseDamage = 65;
        public int shield, shieldMax;
        public double shieldTimer;
        public double overdrive;
        public double overdriveTimer;
        public int combo;
        public double comboTimer;
        public int droneCount;
        public double droneAngle, droneTimer;
        public double critChance, critMultiplier = 1.8, lifesteal, thorns;
        public int chain, homing;
    }
    public static final class Enemy {
        public long id;
        public EnemyType type;
        public double x, y, hp, maxHp, radius, speed, shotTimer, warning = .8, age, flash, charge, chargeX, chargeY;
        public int bossPhase;
        public boolean elite;
        public int shield, shieldMax;
        public double shieldTimer, spawnTimer;
        public Enemy(long id, EnemyType type, double x, double y, int wave) {
            this.id=id; this.type=type; this.x=x; this.y=y;
            radius = switch(type) { case SEEKER -> 12; case HUNTER -> 10; case BRUTE -> 20; case SNIPER -> 14; case WEAVER -> 13; case SENTINEL -> 18; case HIVE -> 16; case BOSS -> 38; };
            maxHp = switch(type) { case SEEKER -> 20 + wave * 3; case HUNTER -> 16 + wave * 2; case BRUTE -> 65 + wave * 6; case SNIPER -> 30 + wave * 3; case WEAVER -> 34 + wave * 4; case SENTINEL -> 80 + wave * 7; case HIVE -> 55 + wave * 5; case BOSS -> 240 + wave * 75; };
            hp=maxHp;
            speed = switch(type) { case SEEKER -> 79 + wave * 3; case HUNTER -> 126 + wave * 3; case BRUTE -> 50; case SNIPER -> 65; case WEAVER -> 95; case SENTINEL -> 48; case HIVE -> 55; case BOSS -> 60; };
            shotTimer = 1.5; spawnTimer = 4.5;
            if(type==EnemyType.SENTINEL) { shieldMax = 3 + Math.min(3, wave / 5); shield = shieldMax; shieldTimer = 4.5; }
        }
        public void makeElite() {
            if(elite) return;
            elite=true; maxHp*=1.7; hp=maxHp; speed*=1.08; radius*=1.12;
            if(shieldMax>0) { shieldMax=Math.min(6,(int)Math.ceil(shieldMax*1.3)); shield=shieldMax; }
        }
    }
    public static final class Bullet {
        public double x,y,previousX,previousY,vx,vy,damage,life = 2.2;
        public boolean hostile;
        public int pierce;
        public double homing;
        public final Set<Long> hit = new HashSet<>();
        Bullet(double x,double y,double vx,double vy,double damage,boolean hostile,int pierce) {
            this.x=this.previousX=x;this.y=this.previousY=y;this.vx=vx;this.vy=vy;this.damage=damage;this.hostile=hostile;this.pierce=pierce;
        }
    }
    public static final class Particle {
        public double x,y,vx,vy,life,maxLife;
        public int color;
        Particle(double x,double y,double vx,double vy,double life,int color) { this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=this.maxLife=life;this.color=color; }
    }
    public static final class Drop {
        public double x,y,age; public DropType type;
        Drop(double x,double y,DropType type) { this.x=x;this.y=y;this.type=type; }
    }
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Bullet> bullets = new ArrayList<>();
    public final List<Particle> particles = new ArrayList<>();
    public final List<Drop> drops = new ArrayList<>();
    public final List<AudioEngine.Cue> sounds = new ArrayList<>();
    public final EnumMap<Upgrade,Integer> upgrades = new EnumMap<>(Upgrade.class);
    public final List<Upgrade> choices = new ArrayList<>();
    public final Profile profile;
    public Player player = new Player();
    public Screen screen = Screen.HOME, returnScreen = Screen.HOME;
    public Loadout loadout = Loadout.STRIKER;
    public Settings.Difficulty runDifficulty = Settings.Difficulty.STANDARD;
    public WaveModifier modifier = WaveModifier.STANDARD;
    public int wave, spawned, quota, kills, score, salvage, waveKills, completedWaves, reward;
    public int perfectWaves, maxCombo, bossesKilled, eliteKills;
    public boolean won, checkpointReady, resultReady, bossSpawned;
    public double elapsed, waveTime, spawnTimer, bannerTimer, hitFlash, pulseRing, shake;
    public String banner = "", runId = "";
    public long seed;
    private long nextId;
    private Random random = new Random();
    public GameModel(Profile profile) { this.profile=profile; }

    public void newRun(Loadout selected, long seed) {
        loadout=selected; this.seed=seed; runId=UUID.randomUUID().toString();
        runDifficulty=profile.settings.difficulty;
        kills=score=salvage=completedWaves=reward=0; elapsed=0; nextId=0; won=false; resultReady=false;
        perfectWaves=maxCombo=bossesKilled=eliteKills=0;
        upgrades.clear(); player=new Player(); configurePlayer(); player.hp=player.maxHp;
        startWave(1);
    }
    private void configurePlayer() {
        player.maxHp=loadout.hp + profile.hullLevel * 8;
        player.speed=loadout.speed;
        player.damage=loadout.damage * (1 + profile.weaponLevel * .05);
        player.interval=loadout.interval;
        player.dashCooldown=2.6 * Math.pow(.95,profile.reactorLevel);
        player.pulseCooldown=16; player.pulseDamage=65; player.projectiles=1; player.pierce=0; player.regen=0; player.pickupRadius=78;
        player.shield=player.shieldMax=0; player.shieldTimer=0; player.overdrive=0; player.overdriveTimer=0;
        player.combo=0; player.comboTimer=0; player.droneCount=0; player.droneAngle=0; player.droneTimer=0;
        player.critChance=0; player.critMultiplier=1.8; player.lifesteal=0; player.thorns=0; player.chain=0; player.homing=0;
        switch(loadout) {
            case LANCER -> player.pierce += 2;
            case NOVA -> { player.pulseDamage += 30; player.pulseCooldown *= .72; }
            case AEGIS -> { player.shieldMax = 1; player.shield = 1; player.maxHp += 20; }
            default -> { }
        }
        for (var entry : upgrades.entrySet()) for(int i=0;i<entry.getValue();i++) applyStats(entry.getKey());
        player.shield=player.shieldMax;
    }
    public void startWave(int value) {
        wave=value; spawned=waveKills=0; waveTime=0; spawnTimer=1.8; bossSpawned=false;
        modifier = modifierFor(wave);
        quota = 6 + wave * 2 + (wave >= 10 ? 3 : 0) + (wave >= 15 ? 3 : 0)
                + (modifier==WaveModifier.SWARM ? 5 : 0) - (modifier==WaveModifier.ELITE ? 2 : 0);
        random=new Random(seed ^ (wave * 0x9E3779B97F4A7C15L));
        enemies.clear();bullets.clear();drops.clear();particles.clear();
        player.x=WIDTH/2.0;player.y=425;player.invincible=1.3;player.dashDuration=0;player.dashTimer=0;player.pulseTimer=0;player.shootTimer=0;
        player.combo=0; player.comboTimer=0; player.overdriveTimer=0; player.shield=player.shieldMax;
        damageTakenWave=0;
        screen=Screen.PLAYING;
        banner=isBossWave(wave) ? bossName(wave) + "即将抵达" : "第 " + String.format("%02d",wave) + " 区 · " + modifier.label;
        bannerTimer=2.7; checkpointReady=true;
    }
    public int damageTakenWave;
    public static boolean isBossWave(int wave) { return wave>0 && wave % BOSS_INTERVAL == 0; }
    public static int bossTier(int wave) { return Math.max(1, wave / BOSS_INTERVAL); }
    private static WaveModifier modifierFor(int wave) {
        if(isBossWave(wave)) return WaveModifier.STANDARD;
        return switch(wave % 5) { case 2 -> WaveModifier.SWARM; case 3 -> WaveModifier.ELITE; case 4 -> WaveModifier.ION_STORM; default -> WaveModifier.STANDARD; };
    }
    public static String bossName(int wave) {
        return switch(wave) { case 5 -> "哨卫·棱镜"; case 10 -> "先知·零"; case 15 -> "织网者·弥撒"; default -> "终末守望者"; };
    }
    public void update(double dt, Input input) {
        dt=Math.clamp(dt,0,.05);
        if (screen!=Screen.PLAYING) return;
        elapsed+=dt;waveTime+=dt;bannerTimer=Math.max(0,bannerTimer-dt);
        hitFlash=Math.max(0,hitFlash-dt*2);shake=Math.max(0,shake-dt*30);pulseRing=Math.max(0,pulseRing-dt*420);
        player.invincible=Math.max(0,player.invincible-dt);player.dashTimer=Math.max(0,player.dashTimer-dt);
        player.pulseTimer=Math.max(0,player.pulseTimer-dt);player.shootTimer=Math.max(0,player.shootTimer-dt);
        player.overdriveTimer=Math.max(0,player.overdriveTimer-dt);
        if(player.combo>0) { player.comboTimer-=dt; if(player.comboTimer<=0) player.combo=0; }
        if(player.shieldMax>0 && player.shield<player.shieldMax) {
            player.shieldTimer-=dt;
            if(player.shieldTimer<=0) { player.shield++; player.shieldTimer=14; }
        }
        player.hp=Math.min(player.maxHp,player.hp+player.regen*dt);
        double dx=input.horizontal(), dy=input.vertical(), length=Math.hypot(dx,dy);
        if(length>0) { dx/=length;dy/=length; }
        if(input.take(KeyEvent.VK_SPACE) && player.dashTimer<=0) {
            if(length==0) { double a=Math.atan2(input.mouseY-player.y,input.mouseX-player.x);dx=Math.cos(a);dy=Math.sin(a); }
            player.dashX=dx;player.dashY=dy;player.dashDuration=.18;player.invincible=Math.max(player.invincible,.3);
            player.dashTimer=player.dashCooldown; sound(AudioEngine.Cue.DASH);
            burst(player.x,player.y,0x74f8e6,18);
        }
        if(player.dashDuration>0) { player.dashDuration-=dt;dx=player.dashX*3.5;dy=player.dashY*3.5; }
        double moveBoost=player.overdriveTimer>0?1.12:1;
        player.x=Math.clamp(player.x+dx*player.speed*moveBoost*dt,LEFT+15,RIGHT-15);
        player.y=Math.clamp(player.y+dy*player.speed*moveBoost*dt,TOP+15,BOTTOM-15);
        if(input.take(KeyEvent.VK_R) && player.overdrive>=100 && player.overdriveTimer<=0) {
            player.overdrive=0; player.overdriveTimer=6; sound(AudioEngine.Cue.PULSE); burst(player.x,player.y,0xffd166,30);
        }
        if((input.take(KeyEvent.VK_E) || input.take(KeyEvent.VK_Q)) && player.pulseTimer<=0) {
            player.pulseTimer=player.pulseCooldown;pulseRing=245; sound(AudioEngine.Cue.PULSE);
            for(Enemy e:enemies) if(e.warning<=0 && distance(player.x,player.y,e.x,e.y)<245+e.radius) {
                damageEnemy(e,player.pulseDamage,false);
            }
            bullets.removeIf(b->b.hostile && distance(b.x,b.y,player.x,player.y)<245);
            if(!profile.settings.reducedMotion) shake=7;
        }
        if((input.firing || input.held(KeyEvent.VK_J)) && player.shootTimer<=0) {
            player.shootTimer=player.interval * (player.overdriveTimer>0?.58:1);
            double angle=Math.atan2(input.mouseY-player.y,input.mouseX-player.x);
            for(int i=0;i<player.projectiles;i++) {
                double a=angle+(i-(player.projectiles-1)/2.0)*.13;
                firePlayerBullet(player.x+Math.cos(a)*20,player.y+Math.sin(a)*20,a,720,player.damage,player.pierce,player.homing);
            }
            sound(AudioEngine.Cue.SHOT);
        }
        updateDrones(dt);
        spawnTimer-=dt;
        if(spawnTimer<=0 && spawned<quota) {
            int batch=Math.min(wave>=12?4:wave>=6?3:2,quota-spawned);
            for(int i=0;i<batch;i++) spawn(selectEnemy());
            spawnTimer=Math.max(.55,1.65-wave*.055) * (modifier==WaveModifier.SWARM?.78:modifier==WaveModifier.ION_STORM?1.08:1);
        }
        if(isBossWave(wave) && !bossSpawned && spawned>=quota) spawnBoss();
        updateEnemies(dt);
        updateBullets(dt);
        removeDead();
        updateDrops(dt);
        updateParticles(dt);
        if(player.hp<=0) { finish(false);return; }
        if(spawned>=quota && (!isBossWave(wave) || bossSpawned) && enemies.isEmpty()) clearWave();
    }
    private EnemyType selectEnemy() {
        int n=random.nextInt(100);
        if(wave>=4 && n<8) return EnemyType.HIVE;
        if(wave>=3 && n<22) return EnemyType.SENTINEL;
        if(wave>=2 && n<40) return EnemyType.WEAVER;
        if(wave>=3 && n<52) return EnemyType.SNIPER;
        if(wave>=2 && n<68) return EnemyType.BRUTE;
        if(n<85) return EnemyType.HUNTER;
        return EnemyType.SEEKER;
    }
    private void spawn(EnemyType type) {
        if(enemies.size()>=100) return;
        double x,y;
        if(random.nextBoolean()) { x=random.nextBoolean()?LEFT+26:RIGHT-26;y=TOP+25+random.nextDouble()*(BOTTOM-TOP-50); }
        else { x=LEFT+25+random.nextDouble()*(RIGHT-LEFT-50);y=random.nextBoolean()?TOP+26:BOTTOM-26; }
        Enemy e=new Enemy(++nextId,type,x,y,wave);
        double eliteChance=.015+wave*.006+(modifier==WaveModifier.ELITE?.28:0);
        if(random.nextDouble()<Math.min(.5,eliteChance)) e.makeElite();
        enemies.add(e);spawned++;
    }
    private void spawnBoss() {
        Enemy boss=new Enemy(++nextId,EnemyType.BOSS,WIDTH/2.0,TOP+75,wave);
        boss.warning=1.6;boss.bossPhase=0;bossSpawned=true;enemies.add(boss);banner=bossName(wave);bannerTimer=2.7;
    }
    private void updateEnemies(double dt) {
        List<Enemy> additions=new ArrayList<>();
        for(Enemy e:enemies) {
            e.warning-=dt;e.flash=Math.max(0,e.flash-dt);e.age+=dt;
            if(e.shieldMax>0 && e.shield<e.shieldMax) {
                e.shieldTimer-=dt;
                if(e.shieldTimer<=0) { e.shield++; e.shieldTimer=5; }
            }
            if(e.warning>0 || e.hp<=0) continue;
            double dx=player.x-e.x,dy=player.y-e.y,dist=Math.max(1,Math.hypot(dx,dy));dx/=dist;dy/=dist;
            double velocity=e.speed*runDifficulty.speed;
            if(e.type==EnemyType.SNIPER) {
                double approach=dist>275?1:dist<175?-1:0;
                e.x+=(dx*approach+dy*.4)*velocity*dt;e.y+=(dy*approach-dx*.4)*velocity*dt;
                e.shotTimer-=dt;
                if(e.shotTimer<=0) { enemyShot(e.x,e.y,dx,dy,245,10);e.shotTimer=2.1; }
            } else if(e.type==EnemyType.WEAVER) {
                double approach=dist>300?1:dist<210?-1:0,strafe=Math.sin(e.age*2.4)*.8;
                e.x+=(dx*approach-dy*strafe)*velocity*dt;e.y+=(dy*approach+dx*strafe)*velocity*dt;
                e.shotTimer-=dt;
                if(e.shotTimer<=0) {
                    double a=Math.atan2(dy,dx);
                    for(int i=-1;i<=1;i++) enemyShot(e.x,e.y,Math.cos(a+i*.16),Math.sin(a+i*.16),215,8);
                    e.shotTimer=2.5;
                }
            } else if(e.type==EnemyType.SENTINEL) {
                if(dist>230) { e.x+=dx*velocity*dt;e.y+=dy*velocity*dt; }
                e.shotTimer-=dt;
                if(e.shotTimer<=0) { enemyShot(e.x,e.y,dx,dy,225,15);e.shotTimer=2.6; }
            } else if(e.type==EnemyType.HIVE) {
                double approach=dist>360?1:dist<250?-1:0;
                e.x+=dx*approach*velocity*dt;e.y+=dy*approach*velocity*dt;
                e.spawnTimer-=dt;
                if(e.spawnTimer<=0 && enemies.size()+additions.size()<100) {
                    for(int i=0;i<2;i++) {
                        Enemy child=new Enemy(++nextId,EnemyType.SEEKER,e.x+Math.cos(i*Math.PI)*28,e.y+Math.sin(i*Math.PI)*28,wave);
                        child.warning=.35;child.speed*=1.12;additions.add(child);
                    }
                    e.spawnTimer=5.5;
                }
            } else if(e.type==EnemyType.BOSS) {
                e.x+=(dx*velocity+Math.sin(e.age*.8)*22)*dt;
                e.y+=(dy*velocity+Math.cos(e.age*.6)*18)*dt;
                e.shotTimer-=dt;
                if(e.shotTimer<=0) { bossAttack(e,dx,dy); }
                int phase=e.hp<e.maxHp*.33?2:e.hp<e.maxHp*.66?1:0;
                if(phase>e.bossPhase) { e.bossPhase=phase; bossSummon(e,additions); }
            } else {
                if(e.type==EnemyType.HUNTER) {
                    e.shotTimer-=dt;
                    if(e.shotTimer<0 && e.charge<=0) { e.charge=.6;e.chargeX=dx;e.chargeY=dy;e.shotTimer=2.8; }
                    if(e.charge>0) { e.charge-=dt;dx=e.chargeX;dy=e.chargeY;velocity*=1.65; }
                }
                e.x+=dx*velocity*dt;e.y+=dy*velocity*dt;
            }
            e.x=Math.clamp(e.x,LEFT+e.radius,RIGHT-e.radius);e.y=Math.clamp(e.y,TOP+e.radius,BOTTOM-e.radius);
            if(distance(player.x,player.y,e.x,e.y)<e.radius+13) {
                hurt(collisionDamage(e)*runDifficulty.damage);
                e.x-=dx*18;e.y-=dy*18;
            }
        }
        for(Enemy e:additions) if(enemies.size()<100) enemies.add(e);
    }
    private static double collisionDamage(Enemy e) {
        return switch(e.type) { case BOSS -> 24; case BRUTE -> 19; case SENTINEL -> 16; case HIVE -> 12; case WEAVER -> 11; case SNIPER, SEEKER, HUNTER -> 10; };
    }
    private void bossAttack(Enemy e,double dx,double dy) {
        int tier=bossTier(wave);
        int count=switch(tier) { case 1 -> 10; case 2 -> 14; case 3 -> 18; default -> 22; };
        double offset=e.age*(.25+tier*.06);
        for(int i=0;i<count;i++) {
            double a=offset+i*Math.PI*2/count;
            enemyShot(e.x,e.y,Math.cos(a),Math.sin(a),170+tier*12,10+tier*2);
        }
        int aimed=Math.min(4,tier);
        double base=Math.atan2(dy,dx);
        for(int i=0;i<aimed;i++) enemyShot(e.x,e.y,Math.cos(base+i*.12-.06*(aimed-1)),Math.sin(base+i*.12-.06*(aimed-1)),245+tier*15,12+tier*2);
        e.shotTimer=Math.max(.8,2.5-tier*.32)*(e.hp<e.maxHp*.4?.8:1);
    }
    private void bossSummon(Enemy e,List<Enemy> additions) {
        int tier=bossTier(wave);
        EnemyType[] types=switch(tier) { case 1 -> new EnemyType[]{EnemyType.HUNTER,EnemyType.HUNTER,EnemyType.HUNTER}; case 2 -> new EnemyType[]{EnemyType.WEAVER,EnemyType.WEAVER,EnemyType.SEEKER,EnemyType.SEEKER}; case 3 -> new EnemyType[]{EnemyType.SENTINEL,EnemyType.SENTINEL,EnemyType.HUNTER}; default -> new EnemyType[]{EnemyType.HUNTER,EnemyType.HUNTER,EnemyType.SENTINEL,EnemyType.HIVE}; };
        for(int i=0;i<types.length;i++) {
            double a=i*Math.PI*2/types.length;
            Enemy child=new Enemy(++nextId,types[i],e.x+Math.cos(a)*78,e.y+Math.sin(a)*78,wave);
            child.warning=.5;
            if(tier>=4) child.makeElite();
            additions.add(child);
        }
    }
    private void enemyShot(double x,double y,double dx,double dy,double speed,double damage) {
        double storm=modifier==WaveModifier.ION_STORM?1.2:1;
        addBullet(new Bullet(x,y,dx*speed*storm,dy*speed*storm,damage*runDifficulty.damage,true,0));
    }
    private void firePlayerBullet(double x,double y,double angle,double speed,double damage,int pierce,int homing) {
        Bullet b=new Bullet(x,y,Math.cos(angle)*speed,Math.sin(angle)*speed,damage,false,pierce);
        b.homing=homing;addBullet(b);
    }
    private void updateDrones(double dt) {
        if(player.droneCount<=0) return;
        player.droneAngle+=dt*1.6;player.droneTimer-=dt;
        if(player.droneTimer>0) return;
        Enemy target=nearestEnemy(player.x,player.y,520,null);
        if(target==null) { player.droneTimer=.2;return; }
        for(int i=0;i<player.droneCount;i++) {
            double a=player.droneAngle+i*Math.PI*2/player.droneCount;
            double x=player.x+Math.cos(a)*46,y=player.y+Math.sin(a)*46;
            double angle=Math.atan2(target.y-y,target.x-x);
            firePlayerBullet(x,y,angle,660,player.damage*.5,0,1);
        }
        player.droneTimer=.55;
    }
    private Enemy nearestEnemy(double x,double y,double maxDistance,Set<Long> ignore) {
        Enemy best=null;double score=maxDistance;
        for(Enemy e:enemies) {
            if(e.hp<=0||e.warning>0||(ignore!=null&&ignore.contains(e.id))) continue;
            double d=distance(x,y,e.x,e.y);
            if(d<score) {score=d;best=e;}
        }
        return best;
    }
    private void addBullet(Bullet b) { if(bullets.size()<500) bullets.add(b); }
    private void updateBullets(double dt) {
        for(Bullet b:bullets) {
            b.previousX=b.x;b.previousY=b.y;
            if(!b.hostile && b.homing>0) {
                Enemy target=nearestEnemy(b.x,b.y,460,b.hit);
                if(target!=null) {
                    double speed=Math.hypot(b.vx,b.vy),desired=Math.atan2(target.y-b.y,target.x-b.x);
                    double blend=Math.min(1,b.homing*1.1*dt);
                    b.vx+=(Math.cos(desired)*speed-b.vx)*blend;b.vy+=(Math.sin(desired)*speed-b.vy)*blend;
                }
            }
            b.x+=b.vx*dt;b.y+=b.vy*dt;b.life-=dt;
            if(b.hostile) {
                if(segmentDistance(player.x,player.y,b.previousX,b.previousY,b.x,b.y)<17) { hurt(b.damage);b.life=0; }
            } else {
                for(Enemy e:enemies) {
                    if(b.life<=0) break;
                    if(e.hp<=0 || e.warning>0 || b.hit.contains(e.id)) continue;
                    if(segmentDistance(e.x,e.y,b.previousX,b.previousY,b.x,b.y)<e.radius+4) {
                        b.hit.add(e.id);
                        if(e.shield>0) {
                            e.shield--;e.shieldTimer=5;e.flash=.1;burst(b.x,b.y,0x74c8ff,7);
                            b.life=0;continue;
                        }
                        boolean crit=player.critChance>0 && random.nextDouble()<player.critChance;
                        double damage=b.damage*(crit?player.critMultiplier:1);
                        damageEnemy(e,damage,crit);
                        if(player.chain>0 && random.nextDouble()<Math.min(.6,.18*player.chain)) chainLightning(e,damage*.45,player.chain);
                        burst(b.x,b.y,crit?0xffe08a:0xffbd68,crit?8:4);
                        if(b.pierce--<=0) b.life=0;
                    }
                }
            }
            if(b.x<LEFT-25 || b.x>RIGHT+25 || b.y<TOP-25 || b.y>BOTTOM+25) b.life=0;
        }
        bullets.removeIf(b->b.life<=0);
    }
    private void damageEnemy(Enemy e,double damage,boolean crit) {
        if(e.shield>0) { e.shield--;e.shieldTimer=5;e.flash=.1;burst(e.x,e.y,0x74c8ff,6);return; }
        e.hp-=damage;e.flash=crit?.16:.09;
    }
    private void chainLightning(Enemy source,double damage,int jumps) {
        Set<Long> hit=new HashSet<>();hit.add(source.id);Enemy current=source;
        for(int i=0;i<jumps;i++) {
            Enemy next=nearestEnemy(current.x,current.y,165,hit);
            if(next==null) break;
            next.hp-=damage*(1-i*.12);next.flash=.12;hit.add(next.id);
            burst(next.x,next.y,0x9f7bff,6);
            current=next;
        }
    }
    public void hurt(double damage) {
        if(player.invincible>0) return;
        if(player.shield>0) {
            player.shield--;player.shieldTimer=14;player.invincible=.35;hitFlash=.15;
            burst(player.x,player.y,0x74c8ff,16);sound(AudioEngine.Cue.HIT);return;
        }
        player.hp=Math.max(0,player.hp-damage);damageTakenWave+=damage;player.combo=0;player.comboTimer=0;
        player.invincible=.72;hitFlash=.3;
        if(player.thorns>0) {
            for(Enemy e:enemies) if(e.hp>0 && distance(player.x,player.y,e.x,e.y)<150) {
                e.hp-=player.thorns;e.flash=.12;
            }
            burst(player.x,player.y,0x9f7bff,18);
        }
        if(!profile.settings.reducedMotion) shake=5;
        burst(player.x,player.y,0xff607c,15);sound(AudioEngine.Cue.HIT);
    }
    private void removeDead() {
        Iterator<Enemy> iterator=enemies.iterator();
        while(iterator.hasNext()) {
            Enemy e=iterator.next();if(e.hp>0) continue;
            iterator.remove();kills++;waveKills++;
            player.combo++;player.comboTimer=3;maxCombo=Math.max(maxCombo,player.combo);
            double multiplier=comboMultiplier();
            int base=e.type==EnemyType.BOSS?1500:e.elite?180:e.type==EnemyType.BRUTE?90:e.type==EnemyType.SENTINEL?120:e.type==EnemyType.HIVE?100:50;
            score+=(int)Math.round(base*multiplier);
            player.overdrive=Math.min(100,player.overdrive+(e.type==EnemyType.BOSS?25:5));
            player.hp=Math.min(player.maxHp,player.hp+player.lifesteal);
            burst(e.x,e.y,e.type==EnemyType.BOSS?0xffbf72:e.elite?0xffe08a:0xff6685,e.type==EnemyType.BOSS?55:e.elite?24:14);
            if(drops.size()<150) {
                double roll=random.nextDouble();
                DropType type=roll<.16?DropType.REPAIR:roll<.24?DropType.SHIELD:roll<.31?DropType.OVERDRIVE:DropType.SALVAGE;
                if(e.elite&&roll>.55) type=DropType.SALVAGE;
                drops.add(new Drop(e.x,e.y,type));
            }
            if(e.elite) eliteKills++;
            if(e.type==EnemyType.BOSS) {
                bossesKilled++;player.hp=Math.min(player.maxHp,player.hp+40);salvage+=15;player.overdrive=Math.min(100,player.overdrive+25);
                for(int i=0;i<3;i++) drops.add(new Drop(e.x+(i-1)*24,e.y,DropType.values()[1+i%3]));
            }
        }
    }
    private double comboMultiplier() { return 1+Math.min(2.0,player.combo*.03); }
    private void updateDrops(double dt) {
        Iterator<Drop> it=drops.iterator();
        while(it.hasNext()) {
            Drop d=it.next();d.age+=dt;double dist=distance(d.x,d.y,player.x,player.y);
            if(dist<player.pickupRadius) { double step=Math.min(dist,350*dt);d.x+=(player.x-d.x)/Math.max(1,dist)*step;d.y+=(player.y-d.y)/Math.max(1,dist)*step; }
            if(dist<19) {
                if(d.type==DropType.REPAIR) player.hp=Math.min(player.maxHp,player.hp+20);
                else if(d.type==DropType.SHIELD) { player.shieldMax=Math.max(1,player.shieldMax);player.shield=Math.min(player.shieldMax,player.shield+1);player.shieldTimer=14; }
                else if(d.type==DropType.OVERDRIVE) player.overdrive=Math.min(100,player.overdrive+35);
                else { salvage++;score+=15; }
                sound(AudioEngine.Cue.PICKUP);it.remove();
            }
            else if(d.age>25) it.remove();
        }
    }
    private void updateParticles(double dt) {
        for(Particle p:particles) {p.x+=p.vx*dt;p.y+=p.vy*dt;p.vx*=Math.exp(-3*dt);p.vy*=Math.exp(-3*dt);p.life-=dt;}
        particles.removeIf(p->p.life<=0);
    }
    private void burst(double x,double y,int color,int count) {
        if(profile.settings.reducedMotion) return;
        // 视觉随机单独生成，避免关闭特效改变关卡与掉落的随机序列。
        for(int i=0;i<count && particles.size()<350;i++) {double a=i*2.39996+elapsed;double v=45+(i%7)*20;particles.add(new Particle(x,y,Math.cos(a)*v,Math.sin(a)*v,.3+(i%5)*.07,color));}
    }
    private void clearWave() {
        completedWaves=wave;
        boolean perfect=damageTakenWave<=0;
        if(perfect) { perfectWaves++; score+=600; salvage+=5; }
        score+=300+wave*50;
        salvage+=drops.stream().filter(d->d.type==DropType.SALVAGE).count();
        player.hp=Math.min(player.maxHp,player.hp+18);
        sound(AudioEngine.Cue.CLEAR);
        if(wave==FINAL_WAVE) {finish(true);return;}
        List<Upgrade> available=new ArrayList<>();
        for(Upgrade u:Upgrade.values()) if(upgrades.getOrDefault(u,0)<u.cap) available.add(u);
        Collections.shuffle(available,random);choices.clear();choices.addAll(available.subList(0,Math.min(3,available.size())));
        screen=Screen.UPGRADE;
    }
    public void chooseUpgrade(int index) {
        if(screen!=Screen.UPGRADE || index<0 || index>=choices.size()) return;
        Upgrade u=choices.get(index);upgrades.merge(u,1,Integer::sum);applyStats(u);
        if(u==Upgrade.HULL) player.hp=Math.min(player.maxHp,player.hp+25);
        if(u==Upgrade.SHIELD) player.shield=player.shieldMax;
        choices.clear();startWave(wave+1);
    }
    private void applyStats(Upgrade u) {
        switch(u) {
            case POWER -> player.damage*=1.22; case RAPID -> player.interval*=.85;
            case HULL -> player.maxHp+=25; case SPEED -> player.speed*=1.12;
            case MULTI -> player.projectiles++; case PIERCE -> player.pierce++; case REGEN -> player.regen+=.9;
            case MAGNET -> player.pickupRadius+=65; case DASH -> player.dashCooldown*=.82;
            case PULSE -> { player.pulseDamage+=35;player.pulseCooldown*=.88; }
            case CRIT -> player.critChance+=.10; case CHAIN -> player.chain++; case HOMING -> player.homing++;
            case SHIELD -> player.shieldMax++; case LIFESTEAL -> player.lifesteal+=1.2;
            case ORBIT -> player.droneCount++; case THORNS -> player.thorns+=18;
        }
    }
    private void finish(boolean victory) {
        won=victory;screen=Screen.RESULT;resultReady=true;
        reward=completedWaves*14+kills/2+salvage/3+perfectWaves*15+bossesKilled*10+(victory?200:0);
        if(!runId.equals(profile.lastSettledRun)) {
            profile.credits+=reward;profile.bestScore=Math.max(profile.bestScore,score);
            profile.bestWave=Math.max(profile.bestWave,completedWaves);profile.runs++;
            if(victory) profile.victories++;profile.lastSettledRun=runId;
        }
        sound(victory?AudioEngine.Cue.WIN:AudioEngine.Cue.LOSS);
    }
    public Properties checkpoint() {
        Properties p=new Properties();p.setProperty("version","1");p.setProperty("runId",runId);p.setProperty("seed",Long.toString(seed));
        p.setProperty("wave",Integer.toString(wave));p.setProperty("loadout",loadout.name());p.setProperty("difficulty",runDifficulty.name());
        p.setProperty("hp",Double.toString(player.hp));p.setProperty("elapsed",Double.toString(elapsed));
        p.setProperty("kills",Integer.toString(kills));p.setProperty("score",Integer.toString(score));p.setProperty("salvage",Integer.toString(salvage));
        p.setProperty("perfectWaves",Integer.toString(perfectWaves));p.setProperty("maxCombo",Integer.toString(maxCombo));
        p.setProperty("bossesKilled",Integer.toString(bossesKilled));p.setProperty("eliteKills",Integer.toString(eliteKills));
        p.setProperty("overdrive",Double.toString(player.overdrive));
        for(var entry:upgrades.entrySet()) p.setProperty("upgrade."+entry.getKey().name(),entry.getValue().toString());
        return p;
    }
    public boolean resume(Properties p) {
        if(!"1".equals(p.getProperty("version")) || p.getProperty("runId", "").isBlank() || p.getProperty("runId").equals(profile.lastSettledRun)) return false;
        try {
            long savedSeed=Long.parseLong(p.getProperty("seed"));
            Loadout selected=Loadout.valueOf(p.getProperty("loadout"));Settings.Difficulty difficulty=Settings.Difficulty.valueOf(p.getProperty("difficulty"));
            int savedWave=SaveStore.number(p,"wave",1,1,FINAL_WAVE);
            double hp=Double.parseDouble(p.getProperty("hp")), time=Double.parseDouble(p.getProperty("elapsed"));
            if(!Double.isFinite(hp) || hp<=0 || !Double.isFinite(time) || time<0) return false;
            seed=savedSeed;loadout=selected;runDifficulty=difficulty;runId=p.getProperty("runId");
            upgrades.clear();for(Upgrade u:Upgrade.values()) {int count=SaveStore.number(p,"upgrade."+u.name(),0,0,u.cap);if(count>0) upgrades.put(u,count);}
            player=new Player();configurePlayer();player.hp=Math.min(player.maxHp,hp);
            kills=SaveStore.number(p,"kills",0,0,100_000);score=SaveStore.number(p,"score",0,0,10_000_000);
            salvage=SaveStore.number(p,"salvage",0,0,100_000);elapsed=time;completedWaves=savedWave-1;
            perfectWaves=SaveStore.number(p,"perfectWaves",0,0,FINAL_WAVE);maxCombo=SaveStore.number(p,"maxCombo",0,0,100_000);
            bossesKilled=SaveStore.number(p,"bossesKilled",0,0,FINAL_WAVE);eliteKills=SaveStore.number(p,"eliteKills",0,0,100_000);
            reward=0;won=false;resultReady=false;nextId=0;startWave(savedWave);
            player.overdrive=Math.clamp(Double.parseDouble(p.getProperty("overdrive","0")),0,100);
            return true;
        } catch(IllegalArgumentException | NullPointerException e) { return false; }
    }
    private void sound(AudioEngine.Cue cue) { sounds.add(cue); }
    public static double distance(double x,double y,double a,double b) { return Math.hypot(x-a,y-b); }
    public static double segmentDistance(double px,double py,double ax,double ay,double bx,double by) {
        double dx=bx-ax,dy=by-ay,squared=dx*dx+dy*dy;
        if(squared==0) return distance(px,py,ax,ay);
        double t=Math.clamp(((px-ax)*dx+(py-ay)*dy)/squared,0,1);
        return distance(px,py,ax+t*dx,ay+t*dy);
    }
}
