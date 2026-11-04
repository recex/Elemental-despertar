package com.elementalawakening.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Efeito visual 3D animado (forma geometrica brilhante). Quem desenha e o FxRenderer (so no cliente).
 * O eixo "Y local" da forma aponta na direcao dir.
 * Animacao (tudo calculado no cliente, sem custo de rede por tick):
 *  - delay: espera N ticks antes de aparecer (permite montar sequencias)
 *  - vel/acc: deslocamento por tick e aceleracao vertical (arcos de detritos)
 *  - orbit: gira em volta do ponto de origem
 *  - grow: curva de crescimento (POP, SLOW, SHRINK, BELL)
 * Para RING: height = espessura. Para DISC: height = raio interno. Para FUNNEL: radius = raio do topo.
 */
public class FxEntity extends Entity {
    // formas
    public static final int SPHERE = 0, PILLAR = 1, SPIKE = 2, BEAM = 3, RING = 4, DOME = 5, FUNNEL = 6, DISC = 7,
            BOX = 8, ROCK = 9;
    // estilos (podem ser somados com |)
    public static final int EXPAND = 1, RISE = 2, PULSE = 4, SPIN = 8, FLICKER = 16, SOLID = 32;
    // curvas de crescimento
    public static final int GROW_NONE = 0, POP = 1, SLOW = 2, SHRINK = 3, BELL = 4;

    private static final EntityDataAccessor<Integer> SHAPE = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_AGE = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GROW = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VX = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VY = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VZ = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AY = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPIN_RATE = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_R = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_W = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORBIT_PH = SynchedEntityData.defineId(FxEntity.class, EntityDataSerializers.FLOAT);

    public FxEntity(EntityType<? extends FxEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Descricao de uma peca (monte com os metodos encadeados e termine com spawn(level)). */
    public static final class Spec {
        final int shape;
        Vec3 pos = Vec3.ZERO;
        Vec3 dir = null;
        int color = 0xFFFFFF, life = 20, style = 0, delay = 0, grow = 0;
        float radius = 1.0F, height = 1.0F, vx, vy, vz, ay, spin, orbitR, orbitW, orbitPh;

        public Spec(int shape) { this.shape = shape; }
        public Spec pos(Vec3 v) { this.pos = v; return this; }
        public Spec dir(Vec3 v) { this.dir = v; return this; }
        public Spec color(int c) { this.color = c; return this; }
        public Spec r(float v) { this.radius = v; return this; }
        public Spec h(float v) { this.height = v; return this; }
        public Spec life(int v) { this.life = v; return this; }
        public Spec style(int v) { this.style = v; return this; }
        public Spec delay(int v) { this.delay = v; return this; }
        public Spec grow(int v) { this.grow = v; return this; }
        public Spec vel(double x, double y, double z) { this.vx = (float) x; this.vy = (float) y; this.vz = (float) z; return this; }
        public Spec acc(float v) { this.ay = v; return this; }
        public Spec spin(float v) { this.spin = v; return this; }
        public Spec orbit(float r, float w, float phase) { this.orbitR = r; this.orbitW = w; this.orbitPh = phase; return this; }
        public FxEntity spawn(ServerLevel level) { return FxEntity.spawn(level, this); }
    }

    public static FxEntity spawn(ServerLevel level, Spec s) {
        FxEntity fx = new FxEntity(ModEntities.FX.get(), level);
        fx.setPos(s.pos.x, s.pos.y, s.pos.z);
        Vec3 d = s.dir == null || s.dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : s.dir.normalize();
        fx.entityData.set(SHAPE, s.shape);
        fx.entityData.set(COLOR, s.color);
        fx.entityData.set(RADIUS, s.radius);
        fx.entityData.set(HEIGHT, s.height);
        fx.entityData.set(MAX_AGE, s.delay + s.life);
        fx.entityData.set(STYLE, s.style);
        fx.entityData.set(DIR_X, (float) d.x);
        fx.entityData.set(DIR_Y, (float) d.y);
        fx.entityData.set(DIR_Z, (float) d.z);
        fx.entityData.set(DELAY, s.delay);
        fx.entityData.set(GROW, s.grow);
        fx.entityData.set(VX, s.vx);
        fx.entityData.set(VY, s.vy);
        fx.entityData.set(VZ, s.vz);
        fx.entityData.set(AY, s.ay);
        fx.entityData.set(SPIN_RATE, s.spin);
        fx.entityData.set(ORBIT_R, s.orbitR);
        fx.entityData.set(ORBIT_W, s.orbitW);
        fx.entityData.set(ORBIT_PH, s.orbitPh);
        level.addFreshEntity(fx);
        return fx;
    }

    /** Atalho antigo. Formas "de crescer" ganham a curva POP automaticamente. */
    public static FxEntity spawn(ServerLevel level, int shape, Vec3 pos, Vec3 dir, int color,
                                 float radius, float height, int life, int style) {
        Spec s = new Spec(shape).pos(pos).dir(dir).color(color).r(radius).h(height).life(life).style(style);
        if (shape == PILLAR || shape == SPIKE || shape == BOX || shape == BEAM || shape == DOME) {
            s.grow(POP);
        }
        return spawn(level, s);
    }

    /** Muda a direcao do eixo da forma (usado por projeteis em movimento). */
    public void setDir(Vec3 dir) {
        if (dir == null || dir.lengthSqr() < 1.0E-6) return;
        Vec3 d = dir.normalize();
        this.entityData.set(DIR_X, (float) d.x);
        this.entityData.set(DIR_Y, (float) d.y);
        this.entityData.set(DIR_Z, (float) d.z);
    }

    public int getShape() { return this.entityData.get(SHAPE); }
    public int getColor() { return this.entityData.get(COLOR); }
    public float getRadius() { return this.entityData.get(RADIUS); }
    public float getHeight() { return this.entityData.get(HEIGHT); }
    public int getMaxAge() { return this.entityData.get(MAX_AGE); }
    public int getStyle() { return this.entityData.get(STYLE); }
    public float getDirX() { return this.entityData.get(DIR_X); }
    public float getDirY() { return this.entityData.get(DIR_Y); }
    public float getDirZ() { return this.entityData.get(DIR_Z); }
    public int getDelay() { return this.entityData.get(DELAY); }
    public int getGrow() { return this.entityData.get(GROW); }
    public float getVx() { return this.entityData.get(VX); }
    public float getVy() { return this.entityData.get(VY); }
    public float getVz() { return this.entityData.get(VZ); }
    public float getAy() { return this.entityData.get(AY); }
    public float getSpinRate() { return this.entityData.get(SPIN_RATE); }
    public float getOrbitR() { return this.entityData.get(ORBIT_R); }
    public float getOrbitW() { return this.entityData.get(ORBIT_W); }
    public float getOrbitPh() { return this.entityData.get(ORBIT_PH); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SHAPE, SPHERE);
        this.entityData.define(COLOR, 0xFFFFFF);
        this.entityData.define(RADIUS, 1.0F);
        this.entityData.define(HEIGHT, 1.0F);
        this.entityData.define(MAX_AGE, 20);
        this.entityData.define(STYLE, 0);
        this.entityData.define(DIR_X, 0.0F);
        this.entityData.define(DIR_Y, 1.0F);
        this.entityData.define(DIR_Z, 0.0F);
        this.entityData.define(DELAY, 0);
        this.entityData.define(GROW, 0);
        this.entityData.define(VX, 0.0F);
        this.entityData.define(VY, 0.0F);
        this.entityData.define(VZ, 0.0F);
        this.entityData.define(AY, 0.0F);
        this.entityData.define(SPIN_RATE, 0.0F);
        this.entityData.define(ORBIT_R, 0.0F);
        this.entityData.define(ORBIT_W, 0.0F);
        this.entityData.define(ORBIT_PH, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount >= getMaxAge()) {
            this.discard();
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        float t = getMaxAge();
        float drift = (Math.abs(getVx()) + Math.abs(getVy()) + Math.abs(getVz())) * t
                + Math.abs(getAy()) * t * t * 0.5F + getOrbitR();
        float m = Math.max(getRadius(), getHeight()) + 1.0F + drift;
        return new AABB(getX() - m, getY() - m, getZ() - m, getX() + m, getY() + m, getZ() + m);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 128.0 * 128.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
