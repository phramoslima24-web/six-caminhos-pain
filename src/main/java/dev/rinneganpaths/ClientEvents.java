package dev.rinneganpaths;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    static final KeyMapping USE = new KeyMapping("key.rinnegan_paths.use", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.rinnegan_paths");
    static final KeyMapping SUMMON = new KeyMapping("key.rinnegan_paths.summon", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.rinnegan_paths");
    static final KeyMapping CYCLE = new KeyMapping("key.rinnegan_paths.cycle", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.rinnegan_paths");
    static int selected = 0;

    @Mod.EventBusSubscriber(modid = RinneganPaths.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(USE);
            e.register(CYCLE);
            e.register(SUMMON);
        }
    }

    @Mod.EventBusSubscriber(modid = RinneganPaths.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            boolean active = mc.player != null && mc.screen == null;
            while (CYCLE.consumeClick()) {
                if (!active) continue;
                selected = (selected + 1) % Abilities.IDS.length;
                mc.player.displayClientMessage(
                        Component.translatable("ability.rinnegan_paths." + Abilities.IDS[selected]), true);
            }
            while (USE.consumeClick()) {
                if (active) Net.CH.sendToServer(new Net.UsePathPacket(selected));
            }
            while (SUMMON.consumeClick()) {
                if (active) Net.CH.sendToServer(new Net.UsePathPacket(99));
            }
        }
    }
}
