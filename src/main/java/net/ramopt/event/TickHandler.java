package net.ramopt.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ramopt.RamOpt;
import net.ramopt.cache.CacheRegistry;
import net.ramopt.config.RamOptConfig;

/**
 * All periodic work is driven from vanilla's own server tick event rather
 * than a custom thread or scheduler, so we never fight anyone over thread
 * ownership and shut down cleanly with the server for free.
 */
@EventBusSubscriber(modid = RamOpt.MOD_ID)
public final class TickHandler {

    private static long tickCounter = 0;

    private TickHandler() {
    }

    @SubscribeEvent
    public static void onServerTickEnd(ServerTickEvent.Post event) {
        tickCounter++;

        if (RamOptConfig.ENABLE_SOFT_CACHE_EVICTION.get()
                && tickCounter % RamOptConfig.CACHE_SWEEP_INTERVAL_TICKS.get() == 0) {
            int removed = CacheRegistry.sweepAll();
            if (removed > 0) {
                RamOpt.LOGGER.debug("[RamOpt] cache sweep reclaimed {} stale entries", removed);
            }
        }

        if (RamOptConfig.ENABLE_INACTIVE_ENTITY_COMPACTION.get()
                && tickCounter % RamOptConfig.ENTITY_COMPACTION_SWEEP_INTERVAL_TICKS.get() == 0) {
            MinecraftServer server = event.getServer();
            net.ramopt.compaction.InactiveEntityCompactor.sweep(
                    server, RamOptConfig.ENTITY_INACTIVE_THRESHOLD_TICKS.get());
        }
    }
}
