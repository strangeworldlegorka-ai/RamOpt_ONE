package net.ramopt.util;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * Thread-local pool of {@link BlockPos.MutableBlockPos}. Intended for use in
 * *our own* helper code (e.g. area scans this mod performs) rather than as a
 * blanket mixin into vanilla methods - mutating vanilla's pooling behavior
 * via mixin is exactly the kind of thing that starts fights with Lithium,
 * which already optimizes several vanilla scan loops itself. Using this pool
 * only inside RamOpt-owned code keeps it strictly additive.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * BlockPosPool.borrow(pos -> {
 *     pos.set(x, y, z);
 *     // use pos, do not let it escape the lambda
 * });
 * }</pre>
 */
public final class BlockPosPool {

    private static final ThreadLocal<ArrayDeque<BlockPos.MutableBlockPos>> POOL =
            ThreadLocal.withInitial(ArrayDeque::new);

    private static volatile int maxPerThread = 64;

    private BlockPosPool() {
    }

    public static void configure(int maxPerThread) {
        BlockPosPool.maxPerThread = Math.max(8, maxPerThread);
    }

    /**
     * Borrow a pooled mutable pos, run {@code action} with it, then return it
     * to the pool. The instance must not be retained past the lambda.
     */
    public static void borrow(Consumer<BlockPos.MutableBlockPos> action) {
        ArrayDeque<BlockPos.MutableBlockPos> deque = POOL.get();
        BlockPos.MutableBlockPos pos = deque.pollFirst();
        if (pos == null) {
            pos = new BlockPos.MutableBlockPos();
        }
        try {
            action.accept(pos);
        } finally {
            if (deque.size() < maxPerThread) {
                deque.offerFirst(pos);
            }
        }
    }

    /** Current size of the calling thread's pool, mostly useful for diagnostics/tests. */
    public static int currentThreadPoolSize() {
        return POOL.get().size();
    }
}
