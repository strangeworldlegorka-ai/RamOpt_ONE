package net.ramopt.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bounded, LRU-evicting string intern table.
 *
 * <p>Vanilla {@link String#intern()} uses the JVM's global string pool, which is
 * fine but (a) is shared with every other string in the process and can grow
 * unbounded, and (b) lives partially off-heap depending on JVM flags, which
 * makes it a poor fit for a table we specifically want to cap and monitor.
 * This is a small, self-contained alternative scoped to just the strings this
 * mod chooses to intern (NBT string tags, entity custom names, etc.).</p>
 *
 * <p>Thread-safe via a single lock; NBT read/write is not hot enough
 * (relative to per-tick work) for this to be a bottleneck, and a lock is far
 * simpler to reason about than a lock-free structure for an LRU map.</p>
 */
public final class StringInterner {

    private final int maxEntries;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, String> table;

    private long hits = 0;
    private long misses = 0;

    public StringInterner(int maxEntries) {
        this.maxEntries = maxEntries;
        // access-order LinkedHashMap gives us LRU eviction for free via removeEldestEntry
        this.table = new LinkedHashMap<>(1024, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > StringInterner.this.maxEntries;
            }
        };
    }

    /**
     * Returns a shared instance for {@code value} if one is already tracked,
     * otherwise stores and returns {@code value} itself. Never returns null,
     * never allocates a new String beyond what the caller already passed in.
     */
    public String intern(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        // Skip pointlessly long strings - the point is deduping short, highly
        // repeated values (names, enum-ish identifiers), not caching essays.
        if (value.length() > 256) {
            return value;
        }

        lock.lock();
        try {
            String existing = table.get(value);
            if (existing != null) {
                hits++;
                return existing;
            }
            misses++;
            table.put(value, value);
            return value;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return table.size();
        } finally {
            lock.unlock();
        }
    }

    public long hits() {
        return hits;
    }

    public long misses() {
        return misses;
    }

    public void clear() {
        lock.lock();
        try {
            table.clear();
        } finally {
            lock.unlock();
        }
    }
}
