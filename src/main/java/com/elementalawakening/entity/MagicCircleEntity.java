package com.elementalawakening.entity;

import com.elementalawakening.element.Element;
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

/** Circulo magico decorativo deitado no chao. Quem desenha e o MagicCircleRenderer (so no cliente). */
public class MagicCircleEntity extends Entity {
    private static final EntityDataAccessor<Integer> ELEMENT =
            SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(MagicCircleEntity.class, EntityDataSerializers.INT);

    public MagicCircleEntity(EntityType<? extends MagicCircleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** size = metade da largura em blocos, lifetime em ticks. O simbolo e a cor vem do elemento. */
    public static void spawn(ServerLevel level, Vec3 pos, Element element, float size, int lifetime) {
        spawn(level, pos, element, element.color, size, lifetime);
    }

    /** Igual ao outro, mas com cor propria (ex.: branco no Interdiction). */
    public static void spawn(ServerLevel level, Vec3 pos, Element element, int rgb, float size, int lifetime) {
        MagicCircleEntity circle = new MagicCircleEntity(ModEntities.MAGIC_CIRCLE.get(), level);
        circle.setPos(pos.x, pos.y + 0.03, pos.z);
        circle.entityData.set(ELEMENT, element.ordinal());
        circle.entityData.set(COLOR, rgb);
        circle.entityData.set(SIZE, size);
        circle.entityData.set(MAX_AGE, lifetime);
        level.addFreshEntity(circle);
    }

    /** Circulo em pe (como o relogio do Tempo): yawDeg gira o plano em volta do eixo vertical. */
    public static void spawnVertical(ServerLevel level, Vec3 pos, Element element, int rgb, float size,
                                     int lifetime, float yawDeg) {
        MagicCircleEntity circle = new MagicCircleEntity(ModEntities.MAGIC_CIRCLE.get(), level);
        circle.setPos(pos.x, pos.y, pos.z);
        circle.setXRot(90.0F);
        circle.setYRot(yawDeg);
        circle.entityData.set(ELEMENT, element.ordinal());
        circle.entityData.set(COLOR, rgb);
        circle.entityData.set(SIZE, size);
        circle.entityData.set(MAX_AGE, lifetime);
        level.addFreshEntity(circle);
    }

    public int getElementId() {
        return this.entityData.get(ELEMENT);
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    public float getSize() {
        return this.entityData.get(SIZE);
    }

    public int getMaxAge() {
        return this.entityData.get(MAX_AGE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ELEMENT, 0);
        this.entityData.define(COLOR, 0xFFFFFF);
        this.entityData.define(SIZE, 2.0F);
        this.entityData.define(MAX_AGE, 40);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount >= getMaxAge()) {
            this.discard();
        }
    }

    // O circulo e grande, mas a caixa da entidade e minuscula: sem isso ele some ao sair de vista/longe.
    @Override
    public AABB getBoundingBoxForCulling() {
        float s = getSize();
        return new AABB(getX() - s, getY() - 0.5, getZ() - s, getX() + s, getY() + 1.0, getZ() + s);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < 128.0 * 128.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // nao salva no mundo (e temporario)
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // nao salva no mundo (e temporario)
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
