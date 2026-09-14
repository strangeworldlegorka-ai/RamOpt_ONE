package net.ramopt;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.ramopt.config.RamOptConfig;
import net.ramopt.util.BlockPosPool;
import net.ramopt.util.StringInterner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RAM Optimizer - aggressive, narrowly-scoped memory optimizations for
 * NeoForge 1.21.1, designed to be a drop-in alongside Sodium, Lithium,
 * Starlight and Veil.
 *
 * <p>Design rule followed throughout this mod: every optimization either (a)
 * hooks an event on the NeoForge event bus, which is inherently additive and
 * cannot conflict with another mod's listener, or (b) mixes into a single,
 * narrowly-scoped vanilla method that none of the mods above touch (see the
 * javadoc on {@link net.ramopt.mixin.CompoundTagInternMixin} for the
 * reasoning on the one mixin this mod ships). Nothing here injects into
 * chunk generation, the light engine, entity tick scheduling, or the render
 * pipeline - that is deliberate, not an oversight.</p>
 */
@Mod(RamOpt.MOD_ID)
public final class RamOpt {

    public static final String MOD_ID = "ramopt";
    public static final Logger LOGGER = LoggerFactory.getLogger("RamOpt");

    /** Shared intern table used by {@link net.ramopt.mixin.CompoundTagInternMixin}. */
    public static final StringInterner STRING_INTERNER =
            new StringInterner(250_000);

    public RamOpt(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, RamOptConfig.SPEC);

        modEventBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("[RamOpt] initializing - string interning, soft cache eviction, "
                + "BlockPos pooling and inactive-entity side-cache compaction are all "
                + "independently toggleable in the config.");
    }

    private void onCommonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        BlockPosPool.configure(RamOptConfig.BLOCKPOS_POOL_SIZE_PER_THREAD.get());

        // Re-apply pool size if the config gets reloaded at runtime.
        event.enqueueWork(() ->
                LOGGER.info("[RamOpt] ready. intern table cap: {} entries, cache sweep every {} ticks.",
                        RamOptConfig.INTERN_TABLE_MAX_ENTRIES.get(),
                        RamOptConfig.CACHE_SWEEP_INTERVAL_TICKS.get()));
    }
}
