package dev.rinneganpaths;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-persistent record of recent deaths used by Rinne Tensei. */
public class DeathLedger extends SavedData {
    private static final String NAME = "rinnegan_paths_ledger";
    private static final int MAX = 100;
    private final ListTag entries = new ListTag();

    public static DeathLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeathLedger::load, DeathLedger::new, NAME);
    }

    public static DeathLedger load(CompoundTag tag) {
        DeathLedger l = new DeathLedger();
        l.entries.addAll(tag.getList("e", 10));
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("e", entries);
        return tag;
    }

    public ListTag raw() { return entries; }

    public void add(CompoundTag entry) {
        entries.add(entry);
        while (entries.size() > MAX) entries.remove(0);
        setDirty();
    }

    public void prune(long now, long maxAge) {
        entries.removeIf(t -> now - ((CompoundTag) t).getLong("t") > maxAge);
        setDirty();
    }
}
