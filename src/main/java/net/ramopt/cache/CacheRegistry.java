package net.ramopt.cache;

import net.ramopt.util.SoftCache;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global registry of {@link SoftCache} instances that get swept on a timer by
 * {@code TickHandler}. Registration is opt-in and additive only - RamOpt never
 * discovers or reflects into another mod's caches on its own, which is what
 * keeps this compatible with Veil and friends instead of fighting them.
 */
public final class CacheRegistry {

    private static final List<SoftCache<?, ?>> REGISTERED = new CopyOnWriteArrayList<>();

    private CacheRegistry() {
    }

    public static void register(SoftCache<?, ?> cache) {
        REGISTERED.add(cache);
    }

    public static int sweepAll() {
        int total = 0;
        for (SoftCache<?, ?> cache : REGISTERED) {
            total += cache.sweep();
        }
        return total;
    }

    public static List<SoftCache<?, ?>> all() {
        return List.copyOf(REGISTERED);
    }
}
