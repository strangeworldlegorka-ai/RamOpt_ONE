package net.ramopt.mixin;

import net.minecraft.nbt.StringTag;
import net.ramopt.RamOpt;
import net.ramopt.config.RamOptConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Interns the backing String of every {@link StringTag} at construction time.
 *
 * <p>Why this target specifically: {@code StringTag.valueOf(String)} is the
 * single canonical entry point vanilla uses to create string tags (item NBT,
 * entity custom names, scoreboard data, command storage, etc.), so hooking it
 * once covers effectively all NBT string duplication without needing dozens
 * of injection points. It is not a hot per-frame render call, it has nothing
 * to do with chunk/entity ticking, and no optimizer in the Sodium/Lithium/
 * Starlight/Veil family touches it - which is exactly why it was chosen.</p>
 *
 * <p><b>Mapping note:</b> the target descriptor below matches the current
 * (1.21.1, official mappings via NeoForm) signature of
 * {@code StringTag(String)}. If your toolchain resolves a different
 * constructor signature, open {@code StringTag} in your IDE's mapped sources
 * and adjust the {@code target} string to match - Mixin will fail loudly at
 * startup with a clear "could not find target" error if this ever drifts,
 * it will not fail silently.</p>
 */
@Mixin(StringTag.class)
public abstract class CompoundTagInternMixin {

    @ModifyArg(
            method = "valueOf",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/StringTag;<init>(Ljava/lang/String;)V"
            )
    )
    private static String ramopt$internOnConstruct(String value) {
        if (!RamOptConfig.ENABLE_STRING_INTERNING.get()) {
            return value;
        }
        return RamOpt.STRING_INTERNER.intern(value);
    }
}
