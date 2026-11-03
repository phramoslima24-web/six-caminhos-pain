package dev.rinneganpaths;

import net.mcreator.narutoto.network.NarutotoModVariables;
import net.mcreator.narutoto.init.NarutotoModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class Abilities {
    public static final String[] IDS = {"tendo", "shurado", "ningendo", "chikushodo", "gakido", "jigokudo", "black_rods", "rinne_tensei"};

    // ---- Tunables ----------------------------------------------------------
    static final double[] COST = {450, 500, 300, 400, 0, 500, 300, 3000};
    static final long[] COOLDOWN = {240, 300, 400, 900, 40, 600, 500, 12000};   // ticks
    static final double GAKI_MIN_CHAKRA = 50;
    /** The base mod's King of Hell entity runs its own damage procedure; keep off unless you've tested it. */
    static final boolean SPAWN_KING_OF_HELL_VISUAL = false;
    static final double RINNE_RADIUS = 64;
    static final long RINNE_WINDOW = 12000;                            // 10 min
    static final long SUMMON_LIFETIME = 900;                           // 45 s
    static final long PAIN_LIFETIME = 12000;                             // 10 min
    // -----------------------------------------------------------------------

    private Abilities() {}

    public static void use(ServerPlayer p, int idx) {
        if (idx < 0 || idx >= IDS.length) return;
        NarutotoModVariables.PlayerVariables v = Chakra.vars(p);
        if (v == null) return;
        if (!Chakra.rinneganActive(p)) { msg(p, "text.rinnegan_paths.need_rinnegan"); return; }

        long now = p.level().getGameTime();
        String cdKey = "rp_cd_" + IDS[idx];
        long until = p.getPersistentData().getLong(cdKey);
        if (now < until) { msg(p, "text.rinnegan_paths.cooldown", (until - now + 19) / 20); return; }

        if (idx == 4) {
            if (toggleGaki(p, v)) p.getPersistentData().putLong(cdKey, now + COOLDOWN[idx]);
            return;
        }

        double cost = COST[idx];
        if (idx == 7 && v.chakra_maximum > 0) cost = Math.min(cost, v.chakra_maximum * 0.9);
        if (v.chakra < cost) { msg(p, "text.rinnegan_paths.not_enough_chakra", (int) cost); return; }

        boolean ok = switch (idx) {
            case 0 -> tendo(p);
            case 1 -> shurado(p);
            case 2 -> ningendo(p);
            case 3 -> chikushodo(p);
            case 5 -> jigokudo(p);
            case 6 -> blackRods(p);
            case 7 -> rinneTensei(p);
            default -> false;
        };
        if (ok) {
            Chakra.add(p, -cost);
            p.getPersistentData().putLong(cdKey, now + COOLDOWN[idx]);
        }
    }

    // ---- Tendō: Shinra Tensei / Banshō Ten'in -----------------------------
    private static boolean tendo(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        if (p.isShiftKeyDown()) {
            LivingEntity target = findTarget(p, 22);
            if (target == null) { msg(p, "text.rinnegan_paths.no_target"); return false; }
            Vec3 pull = p.position().add(0, 1.0, 0).subtract(target.position()).normalize();
            target.setDeltaMovement(pull.scale(1.65).add(0, 0.22, 0));
            target.hurtMarked = true;
            lvl.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY(0.6), target.getZ(), 28, 0.45, 0.6, 0.45, 0.5);
            lvl.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.7f);
            msg(p, "text.rinnegan_paths.bansho_tenin");
            return true;
        }
        int hit = 0;
        for (LivingEntity target : lvl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(10), e -> validTarget(p, e))) {
            Vec3 away = target.position().subtract(p.position()).normalize();
            double strength = Math.max(0.8, 2.8 - target.distanceTo(p) * 0.12);
            target.setDeltaMovement(away.scale(strength).add(0, 0.55, 0));
            target.hurtMarked = true;
            target.hurt(p.damageSources().indirectMagic(p, p), 8.0F);
            lvl.sendParticles(ParticleTypes.POOF, target.getX(), target.getY(0.5), target.getZ(), 16, 0.4, 0.5, 0.4, 0.08);
            hit++;
        }
        lvl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY(1.0), p.getZ(), 1, 0, 0, 0, 0);
        lvl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.2f);
        msg(p, "text.rinnegan_paths.shinra_tensei", hit);
        return true;
    }

    // ---- Shuradō: mechanical chakra barrage -------------------------------
    private static boolean shurado(ServerPlayer p) {
        LivingEntity target = findTarget(p, 24);
        if (target == null) { msg(p, "text.rinnegan_paths.no_target"); return false; }
        ServerLevel lvl = p.serverLevel();
        Vec3 from = p.getEyePosition();
        Vec3 to = target.getEyePosition();
        beam(lvl, from, to);
        target.hurt(p.damageSources().indirectMagic(p, p), 18.0F);
        target.setSecondsOnFire(4);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        lvl.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(0.8), target.getZ(), 12, 0.5, 0.6, 0.5, 0.08);
        lvl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.8), target.getZ(), 24, 0.4, 0.6, 0.4, 0.25);
        lvl.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.6f);
        return true;
    }

    /** Invokes or dismisses the six Pain bodies. Yarico remains the controller. */
    public static void summonSixPaths(ServerPlayer p) {
        if (!Chakra.rinneganActive(p)) { msg(p, "text.rinnegan_paths.need_rinnegan"); return; }
        ServerLevel lvl = p.serverLevel();
        List<Mob> existing = lvl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48),
                e -> e.getTags().contains("rp_pain_body") && e.getPersistentData().hasUUID("rp_owner")
                        && e.getPersistentData().getUUID("rp_owner").equals(p.getUUID()));
        if (!existing.isEmpty()) {
            existing.forEach(e -> { lvl.sendParticles(ParticleTypes.POOF, e.getX(), e.getY(0.8), e.getZ(), 18, 0.3, 0.5, 0.3, 0.04); e.discard(); });
            msg(p, "text.rinnegan_paths.pain_dismissed");
            return;
        }
        NarutotoModVariables.PlayerVariables vars = Chakra.vars(p);
        double cost = 1200;
        if (vars == null || vars.chakra < cost) { msg(p, "text.rinnegan_paths.not_enough_chakra", (int) cost); return; }
        String[] names = {"Tendō", "Shuradō", "Ningendō", "Chikushōdō", "Gakidō", "Jigokudō"};
        LivingEntity target = findTarget(p, 28);
        for (int i = 0; i < names.length; i++) {
            // Reuse the actual GeckoLib Pain/Jigokudo entities and textures shipped by Narutoto.
            Mob mob = i == 5
                    ? NarutotoModEntities.JIGOKUDO_NPC.get().create(lvl)
                    : NarutotoModEntities.PAIN_NPC.get().create(lvl);
            if (mob == null) continue;
            double angle = p.getYRot() * Math.PI / 180.0 + (Math.PI * 2.0 * i / 6.0);
            double x = p.getX() + Math.cos(angle) * 3.2;
            double z = p.getZ() + Math.sin(angle) * 3.2;
            mob.moveTo(x, p.getY(), z, p.getYRot(), 0);
            mob.setCustomName(Component.literal("§5" + names[i]));
            mob.setCustomNameVisible(true);
            mob.setPersistenceRequired();
            AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) { hp.setBaseValue(100.0); mob.setHealth(100.0F); }
            AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) damage.setBaseValue(12.0);
            AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.32);
            markSummon(mob, p, PAIN_LIFETIME);
            mob.addTag("rp_pain_body");
            mob.goalSelector.addGoal(0, new PathFollowOwnerGoal(mob, p));
            if (target != null) mob.setTarget(target);
            lvl.addFreshEntity(mob);
            lvl.sendParticles(ParticleTypes.PORTAL, x, p.getY() + 1.0, z, 26, 0.35, 0.8, 0.35, 0.5);
        }
        Chakra.add(p, -cost);
        lvl.playSound(null, p.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0f, 0.55f);
        msg(p, "text.rinnegan_paths.pain_summoned");
    }

    // ---- Ningendo: rip the soul -------------------------------------------
    private static boolean ningendo(ServerPlayer p) {
        LivingEntity t = findTarget(p, 14);
        if (t == null) { msg(p, "text.rinnegan_paths.no_target"); return false; }
        ServerLevel lvl = p.serverLevel();
        t.hurt(p.damageSources().indirectMagic(p, p), 24.0F);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        NarutotoModVariables.PlayerVariables tv = Chakra.vars(t);
        if (tv != null) {
            double take = Math.min(tv.chakra, 400);
            Chakra.add(t, -take);
            Chakra.add(p, take * 0.5);
        }
        lvl.sendParticles(ParticleTypes.SOUL, t.getX(), t.getY(1.0), t.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        lvl.playSound(null, t.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1f, 0.8f);
        return true;
    }

    // ---- Chikushodo: summon beasts ----------------------------------------
    private static boolean chikushodo(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        LivingEntity t = findTarget(p, 24);
        long life = SUMMON_LIFETIME;
        for (int i = 0; i < 3; i++) {
            double ang = p.getYRot() * Math.PI / 180.0 + i * (2 * Math.PI / 3);
            double x = p.getX() + Math.cos(ang) * 2.5, z = p.getZ() + Math.sin(ang) * 2.5;
            if (i < 2) {
                Wolf w = EntityType.WOLF.create(lvl);
                if (w == null) continue;
                w.moveTo(x, p.getY(), z, p.getYRot(), 0);
                w.tame(p);
                w.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
                w.setHealth(40);
                w.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
                markSummon(w, p, life);
                lvl.addFreshEntity(w);
                if (t != null) w.setTarget(t);
            } else {
                IronGolem g = EntityType.IRON_GOLEM.create(lvl);
                if (g == null) continue;
                g.moveTo(x, p.getY(), z, p.getYRot(), 0);
                g.setPlayerCreated(true);
                markSummon(g, p, life);
                lvl.addFreshEntity(g);
                if (t != null) g.setTarget(t);
            }
            lvl.sendParticles(ParticleTypes.POOF, x, p.getY() + 0.5, z, 20, 0.4, 0.4, 0.4, 0.05);
        }
        lvl.playSound(null, p.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1f, 0.7f);
        return true;
    }

    // ---- Gakido: chakra-absorbing barrier (toggle) ------------------------
    private static boolean toggleGaki(ServerPlayer p, NarutotoModVariables.PlayerVariables v) {
        boolean on = !p.getPersistentData().getBoolean("rp_gaki");
        if (on && v.chakra < GAKI_MIN_CHAKRA) {
            msg(p, "text.rinnegan_paths.not_enough_chakra", (int) GAKI_MIN_CHAKRA);
            return false;
        }
        p.getPersistentData().putBoolean("rp_gaki", on);
        msg(p, on ? "text.rinnegan_paths.gaki_on" : "text.rinnegan_paths.gaki_off");
        return true;
    }

    // ---- Jigokudo: judgment / restore -------------------------------------
    private static boolean jigokudo(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        if (SPAWN_KING_OF_HELL_VISUAL) spawnKingOfHell(p);

        if (p.isShiftKeyDown()) {
            int n = 0;
            AABB box = p.getBoundingBox().inflate(12);
            for (LivingEntity e : lvl.getEntitiesOfClass(LivingEntity.class, box, x -> x.isAlive() && ally(p, x))) {
                e.setHealth(e.getMaxHealth());
                List<net.minecraft.world.effect.MobEffect> bad = e.getActiveEffects().stream()
                        .map(MobEffectInstance::getEffect)
                        .filter(m -> m.getCategory() == MobEffectCategory.HARMFUL).toList();
                bad.forEach(e::removeEffect);
                Chakra.add(e, 300);
                lvl.sendParticles(ParticleTypes.HEART, e.getX(), e.getY(1.0), e.getZ(), 6, 0.4, 0.5, 0.4, 0.02);
                n++;
            }
            msg(p, "text.rinnegan_paths.restored", n);
            return true;
        }

        LivingEntity t = findTarget(p, 16);
        if (t == null) { msg(p, "text.rinnegan_paths.no_target"); return false; }
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 5));
        NarutotoModVariables.PlayerVariables tv = Chakra.vars(t);
        String chakra = tv != null ? String.valueOf((int) tv.chakra) : "?";
        p.sendSystemMessage(Component.translatable("text.rinnegan_paths.interrogate",
                t.getDisplayName(), (int) t.getHealth(), chakra));
        return true;
    }

    private static void spawnKingOfHell(ServerPlayer p) {
        ResourceLocation id = new ResourceLocation("narutoto", "king_of_hell");
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(id)) return;
        ServerLevel lvl = p.serverLevel();
        Entity k = ForgeRegistries.ENTITY_TYPES.getValue(id).create(lvl);
        if (k == null) return;
        Vec3 look = p.getLookAngle();
        k.moveTo(p.getX() + look.x * 3, p.getY(), p.getZ() + look.z * 3, p.getYRot() + 180, 0);
        markSummon(k, p, 100);
        lvl.addFreshEntity(k);
    }

    // ---- Chakra Receivers: Black Rods -------------------------------------
    private static boolean blackRods(ServerPlayer p) {
        LivingEntity main = findTarget(p, 24);
        if (main == null) { msg(p, "text.rinnegan_paths.no_target"); return false; }
        ServerLevel lvl = p.serverLevel();
        long now = lvl.getGameTime();
        List<LivingEntity> victims = new ArrayList<>();
        victims.add(main);
        lvl.getEntitiesOfClass(LivingEntity.class, main.getBoundingBox().inflate(10),
                        e -> e != main && validTarget(p, e))
                .stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(main)))
                .limit(2).forEach(victims::add);
        for (LivingEntity v : victims) {
            v.getPersistentData().putLong("rp_rod_until", now + 160);
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 9));
            v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            beam(lvl, p.getEyePosition().add(0, -0.3, 0), v.getEyePosition());
        }
        lvl.playSound(null, main.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.6f, 0.5f);
        return true;
    }

    // ---- Rinne Tensei: revive recent deaths --------------------------------
    private static boolean rinneTensei(ServerPlayer p) {
        MinecraftServer srv = p.getServer();
        if (srv == null) return false;
        DeathLedger ledger = DeathLedger.get(srv);
        ServerLevel here = p.serverLevel();
        ledger.prune(here.getGameTime(), RINNE_WINDOW);
        String hereDim = here.dimension().location().toString();
        int players = 0, npcs = 0, stacks = 0;

        Iterator<Tag> it = ledger.raw().iterator();
        while (it.hasNext()) {
            CompoundTag t = (CompoundTag) it.next();
            if (!hereDim.equals(t.getString("dim"))) continue;
            Vec3 pos = new Vec3(t.getDouble("x"), t.getDouble("y"), t.getDouble("z"));
            if (pos.distanceTo(p.position()) > RINNE_RADIUS) continue;

            if ("player".equals(t.getString("k"))) {
                ServerPlayer tp = srv.getPlayerList().getPlayer(t.getUUID("uuid"));
                if (tp == null || tp == p || !tp.isAlive()) continue; // must have respawned already
                tp.teleportTo(here, pos.x, pos.y, pos.z, tp.getYRot(), tp.getXRot());
                tp.removeAllEffects();
                tp.setHealth(tp.getMaxHealth());
                tp.getFoodData().setFoodLevel(20);
                stacks += recoverItems(here, tp, pos, t.getList("items", 10));
                here.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y + 1, pos.z, 40, 0.5, 0.8, 0.5, 0.3);
                players++;
                it.remove();
            } else if ("npc".equals(t.getString("k"))) {
                Optional<EntityType<?>> type = EntityType.byString(t.getString("type"));
                if (type.isEmpty()) { it.remove(); continue; }
                Entity ent = type.get().create(here);
                if (ent == null) { it.remove(); continue; }
                CompoundTag nbt = t.getCompound("nbt").copy();
                nbt.remove("UUID");
                nbt.putShort("DeathTime", (short) 0);
                nbt.putShort("HurtTime", (short) 0);
                ent.load(nbt);
                ent.moveTo(pos.x, pos.y, pos.z, ent.getYRot(), ent.getXRot());
                if (ent instanceof LivingEntity le) le.setHealth(le.getMaxHealth());
                here.addFreshEntity(ent);
                here.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y + 1, pos.z, 40, 0.5, 0.8, 0.5, 0.3);
                npcs++;
                it.remove();
            }
        }
        ledger.setDirty();

        if (players + npcs == 0) { msg(p, "text.rinnegan_paths.rinne_none"); return false; }

        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3600, 2));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 3600, 1));
        p.getFoodData().setFoodLevel(6);
        here.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 0.6f);
        p.sendSystemMessage(Component.translatable("text.rinnegan_paths.rinne_done", players, npcs, stacks));
        return true;
    }

    /** Pulls back the death drops still lying near the death spot (picked-up / despawned items are gone). */
    private static int recoverItems(ServerLevel lvl, ServerPlayer owner, Vec3 pos, ListTag saved) {
        List<ItemStack> want = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) want.add(ItemStack.of(saved.getCompound(i)));
        int given = 0;
        for (ItemEntity ie : lvl.getEntitiesOfClass(ItemEntity.class, new AABB(pos, pos).inflate(8))) {
            ItemStack es = ie.getItem();
            for (ItemStack w : want) {
                if (w.isEmpty() || es.isEmpty() || !ItemStack.isSameItemSameTags(w, es)) continue;
                int take = Math.min(w.getCount(), es.getCount());
                ItemStack give = es.copy();
                give.setCount(take);
                w.shrink(take);
                es.shrink(take);
                if (es.isEmpty()) ie.discard(); else ie.setItem(es);
                if (!owner.getInventory().add(give)) owner.drop(give, false);
                given++;
            }
        }
        return given;
    }

    private static final class PathFollowOwnerGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final Mob body;
        private final ServerPlayer owner;
        PathFollowOwnerGoal(Mob body, ServerPlayer owner) { this.body = body; this.owner = owner; this.setFlags(java.util.EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() { return owner.isAlive() && body.getTarget() == null && body.distanceToSqr(owner) > 16.0; }
        @Override public boolean canContinueToUse() { return canUse() && body.distanceToSqr(owner) > 6.25; }
        @Override public void tick() { body.getNavigation().moveTo(owner, 1.15); }
    }

    // ---- Helpers -----------------------------------------------------------
    static void markSummon(Entity e, ServerPlayer owner, long life) {
        e.addTag("rp_summon");
        CompoundTag d = e.getPersistentData();
        d.putLong("rp_expire", owner.level().getGameTime() + life);
        d.putUUID("rp_owner", owner.getUUID());
    }

    static boolean ally(Entity self, Entity o) {
        if (self == o || self.isAlliedTo(o)) return true;
        if (o instanceof TamableAnimal ta && ta.getOwner() == self) return true;
        CompoundTag d = o.getPersistentData();
        if (d.hasUUID("rp_owner") && d.getUUID("rp_owner").equals(self.getUUID())) return true;
        NarutotoModVariables.PlayerVariables a = Chakra.vars(self), b = Chakra.vars(o);
        if (a != null && b != null && a.team != null && !a.team.isEmpty()
                && !a.team.equalsIgnoreCase("none") && a.team.equals(b.team)) return true;
        return false;
    }

    static boolean validTarget(ServerPlayer p, Entity e) {
        return e instanceof LivingEntity le && le != p && le.isAlive()
                && !(le instanceof Player pl && (pl.isSpectator() || pl.isCreative()))
                && le.getType() != EntityType.ARMOR_STAND && !ally(p, le);
    }

    static LivingEntity findTarget(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), end = eye.add(look.scale(range));
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(p.level(), p, eye, end, box, e -> validTarget(p, e));
        return r != null && r.getEntity() instanceof LivingEntity le ? le : null;
    }

    static void beam(ServerLevel lvl, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        int n = (int) (d.length() * 2);
        for (int i = 0; i < n; i++) {
            Vec3 q = a.add(d.scale(i / (double) n));
            lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    static void msg(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args), true);
    }
}
