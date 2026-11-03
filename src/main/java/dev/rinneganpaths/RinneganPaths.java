package dev.rinneganpaths;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RinneganPaths.MODID)
public class RinneganPaths {
    public static final String MODID = "rinnegan_paths";

    public RinneganPaths() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(new PathEvents());
    }

    private void setup(FMLCommonSetupEvent e) {
        Net.register();
    }
}
