package net.ramopt.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * All subsystems are individually toggleable and tunable so server owners can
 * dial aggressiveness up or down without recompiling. Defaults are tuned to be
 * safe on a modded survival server with ~20-50 players.
 */
public final class RamOptConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_STRING_INTERNING;
    public static final ModConfigSpec.IntValue INTERN_TABLE_MAX_ENTRIES;

    public static final ModConfigSpec.BooleanValue ENABLE_SOFT_CACHE_EVICTION;
    public static final ModConfigSpec.IntValue CACHE_SWEEP_INTERVAL_TICKS;

    public static final ModConfigSpec.BooleanValue ENABLE_BLOCKPOS_POOLING;
    public static final ModConfigSpec.IntValue BLOCKPOS_POOL_SIZE_PER_THREAD;

    public static final ModConfigSpec.BooleanValue ENABLE_INACTIVE_ENTITY_COMPACTION;
    public static final ModConfigSpec.IntValue ENTITY_INACTIVE_THRESHOLD_TICKS;
    public static final ModConfigSpec.IntValue ENTITY_COMPACTION_SWEEP_INTERVAL_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("NBT / data string interning").push("stringInterning");
        ENABLE_STRING_INTERNING = builder
                .comment("Intern repeated NBT string values (item NBT, custom names, etc.) so identical",
                        "strings share one instance instead of being duplicated across every stack/entity.")
                .define("enabled", true);
        INTERN_TABLE_MAX_ENTRIES = builder
                .comment("Upper bound on the intern table before it starts evicting the least-recently-used entries.")
                .defineInRange("maxEntries", 250_000, 1_000, 5_000_000);
        builder.pop();

        builder.comment("Soft-reference cache eviction for mod-added caches (textures, shaders, baked data, etc.)")
                .push("softCacheEviction");
        ENABLE_SOFT_CACHE_EVICTION = builder
                .comment("Periodically sweep registered caches and let the JVM reclaim entries under memory pressure",
                        "instead of holding them forever.")
                .define("enabled", true);
        CACHE_SWEEP_INTERVAL_TICKS = builder
                .comment("How often (in server ticks) to run a sweep. 20 ticks = 1 second.")
                .defineInRange("sweepIntervalTicks", 600, 20, 72000);
        builder.pop();

        builder.comment("Thread-local pooling of mutable position objects used in hot loops")
                .push("blockPosPooling");
        ENABLE_BLOCKPOS_POOLING = builder
                .comment("Reuse a small pool of MutableBlockPos instances per thread instead of allocating new ones",
                        "in tight loops (chunk scans, area queries, etc.).")
                .define("enabled", true);
        BLOCKPOS_POOL_SIZE_PER_THREAD = builder
                .defineInRange("poolSizePerThread", 64, 8, 1024);
        builder.pop();

        builder.comment("Compaction of synced data for entities far from / not tracked by any player")
                .push("inactiveEntityCompaction");
        ENABLE_INACTIVE_ENTITY_COMPACTION = builder
                .comment("Shrink the retained footprint of entities that no player has had tracked for a while.",
                        "This never touches entities that are currently tracked - only idle ones sitting in",
                        "loaded chunks (e.g. distant villagers, farm animals in unloaded-but-ticking areas).")
                .define("enabled", true);
        ENTITY_INACTIVE_THRESHOLD_TICKS = builder
                .comment("How many ticks an entity must go untracked by any player before it's eligible.")
                .defineInRange("inactiveThresholdTicks", 6000, 200, 1_000_000);
        ENTITY_COMPACTION_SWEEP_INTERVAL_TICKS = builder
                .defineInRange("sweepIntervalTicks", 1200, 20, 72000);
        builder.pop();

        SPEC = builder.build();
    }

    private RamOptConfig() {
    }
}
