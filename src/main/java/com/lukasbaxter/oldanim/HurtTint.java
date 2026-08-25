package com.lukasbaxter.oldanim;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Holds the red damage tint on for longer than vanilla does.
 *
 * <p>Vanilla drives the tint straight off {@code hurtTime}, which is 10 ticks
 * and is the same 10 ticks 1.7 used -- so a longer tint is a preference rather
 * than a restoration. What makes 26.2 <em>feel</em> different is the attack
 * cooldown: you land fewer hits, so the flash fires less often even though each
 * one is the same length. Stretching each flash is the only part of that a
 * client can do anything about.
 *
 * <p>Entities are held weakly and only while a tint is running, so this cannot
 * keep a despawned entity alive or grow without bound.
 */
public final class HurtTint {

    private static final Map<LivingEntity, Long> LAST_HURT = new WeakHashMap<>();

    private HurtTint() {
    }

    /** True if this entity should still be drawn red. */
    public static boolean isTinted(LivingEntity entity, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        long now = minecraft.level.getGameTime();

        if (entity.hurtTime > 0) {
            LAST_HURT.put(entity, now);
            return true;
        }

        Long last = LAST_HURT.get(entity);
        if (last == null) {
            return false;
        }
        if (now - last < ticks) {
            return true;
        }
        LAST_HURT.remove(entity);
        return false;
    }
}
