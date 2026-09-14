package net.ramopt.compaction;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.ramopt.RamOpt;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deliberately scoped-down implementation.
 *
 * <p>Reaching into vanilla's {@code SynchedEntityData} / brain memory
 * internals via mixin to "compact" them is exactly the kind of thing that
 * (a) breaks on every Minecraft version bump and (b) is the most likely
 * place to collide with Lithium, which already optimizes entity tracking and
 * brain ticking. Instead of pretending to shrink engine-owned structures we
 * don't control, this class only manages a side-table of *our own* optional
 * per-entity caches (registered by other RamOpt features via
 * {@link #registerSideCache}) and drops entries for entities that have gone
 * untracked long enough. If you're not registering anything into that table,
 * this class currently has nothing to compact and costs one cheap map
 * iteration per sweep.</p>
 */
public final class InactiveEntityCompactor {

    /** entity UUID -> last tick any player had this entity tracked. */
    private static final Map<UUID, Long> LAST_TRACKED_TICK = new ConcurrentHashMap<>();

    /** Optional per-feature side caches, keyed by entity UUID, cleared on inactivity. */
    private static final Map<UUID, Map<String, Object>> SIDE_CACHES = new ConcurrentHashMap<>();

    private InactiveEntityCompactor() {
    }

    public static void markTracked(UUID entityId, long currentTick) {
        LAST_TRACKED_TICK.put(entityId, currentTick);
    }

    public static void registerSideCache(UUID entityId, String key, Object value) {
        SIDE_CACHES.computeIfAbsent(entityId, id -> new ConcurrentHashMap<>()).put(key, value);
    }

    public static void sweep(MinecraftServer server, int inactiveThresholdTicks) {
        long now = server.getTickCount();
        int compacted = 0;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getEntities().getAll()) {
                UUID id = entity.getUUID();
                Long lastTracked = LAST_TRACKED_TICK.get(id);

                boolean currentlyTracked = !entity.isAlwaysTicking() && hasNearbyPlayer(level, entity);
                if (currentlyTracked) {
                    LAST_TRACKED_TICK.put(id, now);
                    continue;
                }

                long lastSeen = (lastTracked != null) ? lastTracked : now;
                if (now - lastSeen >= inactiveThresholdTicks) {
                    Map<String, Object> removed = SIDE_CACHES.remove(id);
                    if (removed != null && !removed.isEmpty()) {
                        compacted++;
                    }
                }
            }
        }

        // Drop bookkeeping for entities that no longer exist at all.
        LAST_TRACKED_TICK.keySet().removeIf(id -> SIDE_CACHES.get(id) == null && !SIDE_CACHES.containsKey(id));

        if (compacted > 0) {
            RamOpt.LOGGER.debug("[RamOpt] compacted side-caches for {} inactive entities", compacted);
        }
    }

    private static boolean hasNearbyPlayer(ServerLevel level, Entity entity) {
        return level.getNearestPlayer(entity, 128.0D) != null;
    }
}
