package dev.rinneganpaths;

import net.mcreator.narutoto.network.NarutotoModVariables;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

public class PathEvents {
    static final double GAKI_UPKEEP = 4;            // chakra per 5 ticks
    static final double GAKI_GAIN_PER_DAMAGE = 8;
    static final double GAKI_PROJECTILE_GAIN = 15;
    static final double GAKI_DRAIN_ATTACKER = 40;
    static final double ROD_DRAIN = 40;             // chakra per 10 ticks on players

    private static final List<String> NPC_EXCLUDE = List.of(
            "dummy", "sphere", "chibaku", "king_of_hell", "clone", "kongo", "block", "bunshin", "shadow");

    // Pain bodies only attack the target selected by their controller and never attack allies.
    @SubscribeEvent
    public void onPainTargetChange(LivingChangeTargetEvent ev) {
        LivingEntity body = ev.getEntity();
        if (!body.getTags().contains("rp_pain_body") || ev.getNewTarget() == null) return;
        CompoundTag data = body.getPersistentData();
        if (!data.hasUUID("rp_owner") || body.level().getServer() == null) return;
        ServerPlayer owner = body.level().getServer().getPlayerList().getPlayer(data.getUUID("rp_owner"));
        if (owner == null || !owner.isAlive()) { ev.setCanceled(true); return; }
        UUID defendTarget = data.hasUUID("rp_defend_target") ? data.getUUID("rp_defend_target") : null;
        boolean defending = defendTarget != null && defendTarget.equals(ev.getNewTarget().getUUID());
        LivingEntity commanded = Abilities.findTarget(owner, 28);
        if (ev.getNewTarget() == owner || Abilities.ally(owner, ev.getNewTarget())
                || (!defending && ev.getNewTarget() != commanded)) {
            ev.setCanceled(true);
        }
    }

    // ---- Gakido upkeep + projectile absorption ----------------------------
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || !(ev.player instanceof ServerPlayer p)) return;
        CompoundTag d = p.getPersistentData();
        if (!d.getBoolean("rp_gaki")) return;
        NarutotoModVariables.PlayerVariables v = Chakra.vars(p);
        if (v == null || !Chakra.rinneganActive(p) || v.chakra < GAKI_UPKEEP) {
            d.putBoolean("rp_gaki", false);
            Abilities.msg(p, "text.rinnegan_paths.gaki_off");
            return;
        }
        ServerLevel lvl = p.serverLevel();
        if (p.tickCount % 5 == 0) Chakra.add(p, -GAKI_UPKEEP);
        for (Projectile pr : lvl.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(3.0),
                x -> x.getOwner() != p)) {
            pr.discard();
            Chakra.add(p, GAKI_PROJECTILE_GAIN);
            lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pr.getX(), pr.getY(), pr.getZ(), 6, 0.1, 0.1, 0.1, 0.02);
        }
        if (p.tickCount % 4 == 0) {
            lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY(1.0), p.getZ(), 4, 1.2, 0.8, 1.2, 0.0);
        }
    }

    // ---- Gakido damage absorption -------------------------------------------
    @SubscribeEvent
    public void onAttack(LivingAttackEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        DamageSource s = ev.getSource();

        // The Six Paths automatically intercept attacks from any mob while they are summoned.
        // Damage is cancelled for the controller and every active Path is ordered to attack the aggressor.
        if (protectWithSixPaths(p, s)) {
            ev.setCanceled(true);
            return;
        }

        if (!p.getPersistentData().getBoolean("rp_gaki")) return;
        if (s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        ev.setCanceled(true);
        Chakra.add(p, ev.getAmount() * GAKI_GAIN_PER_DAMAGE);
        if (s.getEntity() instanceof Player atk && atk != p) Chakra.add(atk, -GAKI_DRAIN_ATTACKER);
    }

    private boolean protectWithSixPaths(ServerPlayer owner, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Projectile projectile && projectile.getOwner() != null) {
            attacker = projectile.getOwner();
        }
        if (!(attacker instanceof Mob hostile) || !hostile.isAlive()) return false;

        ServerLevel level = owner.serverLevel();
        List<Mob> bodies = level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(64),
                body -> body.getTags().contains("rp_pain_body")
                        && body.getPersistentData().hasUUID("rp_owner")
                        && body.getPersistentData().getUUID("rp_owner").equals(owner.getUUID())
                        && body.isAlive());
        if (bodies.isEmpty()) return false;

        for (Mob body : bodies) {
            body.getPersistentData().putUUID("rp_defend_target", hostile.getUUID());
            body.setTarget(hostile);
            body.getNavigation().moveTo(hostile, 1.25D);
            level.sendParticles(ParticleTypes.CRIT, body.getX(), body.getY(1.0), body.getZ(),
                    5, 0.25, 0.35, 0.25, 0.08);
        }
        return true;
    }

    // ---- Summon expiry + Black Rods hold -------------------------------------
    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent ev) {
        LivingEntity e = ev.getEntity();
        if (e.level().isClientSide) return;
        CompoundTag d = e.getPersistentData();
        long now = e.level().getGameTime();

        if (d.contains("rp_expire") && now >= d.getLong("rp_expire")) {
            if (e.level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.POOF, e.getX(), e.getY(0.5), e.getZ(), 15, 0.3, 0.3, 0.3, 0.03);
            e.discard();
            return;
        }

        if (d.contains("rp_rod_until")) {
            if (now >= d.getLong("rp_rod_until")) { d.remove("rp_rod_until"); return; }
            Vec3 m = e.getDeltaMovement();
            e.setDeltaMovement(0, Math.min(0, m.y), 0);
            if (e instanceof ServerPlayer) e.hurtMarked = true;
            if (e instanceof Mob mob) mob.getNavigation().stop();
            if (e instanceof Player && e.tickCount % 10 == 0) Chakra.add(e, -ROD_DRAIN);
        }
    }

    // ---- Rinne Tensei bookkeeping ---------------------------------------------
    @SubscribeEvent
    public void onDeath(LivingDeathEvent ev) {
        LivingEntity e = ev.getEntity();
        if (!(e.level() instanceof ServerLevel lvl) || e.getTags().contains("rp_summon")) return;
        DeathLedger ledger = DeathLedger.get(lvl.getServer());
        CompoundTag t = new CompoundTag();
        t.putString("dim", lvl.dimension().location().toString());
        t.putDouble("x", e.getX());
        t.putDouble("y", e.getY());
        t.putDouble("z", e.getZ());
        t.putLong("t", lvl.getGameTime());

        if (e instanceof ServerPlayer p) {
            t.putString("k", "player");
            t.putUUID("uuid", p.getUUID());
            t.putString("name", p.getGameProfile().getName());
            t.put("items", new ListTag());
            ledger.add(t);
        } else if (e instanceof Mob) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            if (id == null || !id.getNamespace().equals("narutoto")) return;
            for (String bad : NPC_EXCLUDE) if (id.getPath().contains(bad)) return;
            t.putString("k", "npc");
            t.putString("type", id.toString());
            t.put("nbt", e.saveWithoutId(new CompoundTag()));
            ledger.add(t);
        }
    }

    @SubscribeEvent
    public void onDrops(LivingDropsEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        DeathLedger ledger = DeathLedger.get(p.getServer());
        ListTag raw = ledger.raw();
        for (int i = raw.size() - 1; i >= 0; i--) {
            CompoundTag t = raw.getCompound(i);
            if ("player".equals(t.getString("k")) && t.hasUUID("uuid") && t.getUUID("uuid").equals(p.getUUID())) {
                ListTag items = new ListTag();
                for (ItemEntity ie : ev.getDrops()) items.add(ie.getItem().save(new CompoundTag()));
                t.put("items", items);
                ledger.setDirty();
                return;
            }
        }
    }
}
