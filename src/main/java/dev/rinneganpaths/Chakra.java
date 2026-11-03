package dev.rinneganpaths;

import net.mcreator.narutoto.network.NarutotoModVariables;
import net.minecraft.world.entity.Entity;

/** Thin wrapper over the base mod's player capability (chakra, Rinnegan state). */
public final class Chakra {
    private Chakra() {}

    /** Returns the base mod's PlayerVariables, or null for non-players. */
    public static NarutotoModVariables.PlayerVariables vars(Entity e) {
        return e.getCapability(NarutotoModVariables.PLAYER_VARIABLES_CAPABILITY, null).resolve().orElse(null);
    }

    /** Base mod checks `if_sharigan == 4` (plus ninja > 0) before letting Bansho Tenin / Shinra Tensei cast. */
    public static boolean rinneganActive(Entity e) {
        NarutotoModVariables.PlayerVariables v = vars(e);
        return v != null && v.ninja > 0 && v.if_sharigan == 4.0;
    }

    public static void add(Entity e, double delta) {
        NarutotoModVariables.PlayerVariables v = vars(e);
        if (v == null) return;
        double nv = v.chakra + delta;
        if (delta > 0 && v.chakra_maximum > 0) nv = Math.max(v.chakra, Math.min(nv, v.chakra_maximum));
        v.chakra = Math.max(0, nv);
        v.syncPlayerVariables(e);
    }
}
