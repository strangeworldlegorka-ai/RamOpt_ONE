package net.ramopt.util;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A cache whose values are wrapped in {@link SoftReference}s, so the JVM is
 * free to reclaim them under memory pressure before ever throwing an
 * OutOfMemoryError. Entries with a cleared reference are swept out on the
 * next {@link #sweep()} call (invoked periodically by
 * {@code net.ramopt.event.TickHandler}) rather than left as dead weight.
 *
 * <p>This exists because a lot of client-side caches (baked models, decoded
 * textures, shader program handles held by helper objects, etc.) are built
 * with plain {@link java.util.HashMap}s that never evict anything. Rendering
 * mods like Sodium/Veil manage their own internal caches fine on their own -
 * this class is for RamOpt's own use and for any mod that opts in by
 * wrapping its cache with it; it never reaches into another mod's memory.</p>
 */
public final class SoftCache<K, V> {

    private final Map<K, SoftReference<V>> backing = new ConcurrentHashMap<>();
    private final String name;

    public SoftCache(String name) {
        this.name = name;
    }

    public V get(K key, Function<K, V> loader) {
        SoftReference<V> ref = backing.get(key);
        V value = (ref != null) ? ref.get() : null;
        if (value != null) {
            return value;
        }
        value = loader.apply(key);
        backing.put(key, new SoftReference<>(value));
        return value;
    }

    public void put(K key, V value) {
        backing.put(key, new SoftReference<>(value));
    }

    public void invalidate(K key) {
        backing.remove(key);
    }

    /** Removes entries whose soft reference has already been cleared by the GC. */
    public int sweep() {
        int removed = 0;
        var it = backing.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getValue().get() == null) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public int size() {
        return backing.size();
    }

    public String name() {
        return name;
    }
}
