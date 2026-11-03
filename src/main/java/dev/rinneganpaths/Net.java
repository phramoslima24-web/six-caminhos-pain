package dev.rinneganpaths;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class Net {
    private static final String VERSION = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RinneganPaths.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private Net() {}

    public static void register() {
        CH.registerMessage(0, UsePathPacket.class, UsePathPacket::encode, UsePathPacket::decode, UsePathPacket::handle);
    }

    public static final class UsePathPacket {
        private final int index;

        public UsePathPacket(int index) { this.index = index; }

        public static void encode(UsePathPacket m, FriendlyByteBuf b) { b.writeVarInt(m.index); }

        public static UsePathPacket decode(FriendlyByteBuf b) { return new UsePathPacket(b.readVarInt()); }

        public static void handle(UsePathPacket m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> {
                ServerPlayer p = ctx.getSender();
                if (p != null) {
                    if (m.index == 99) Abilities.summonSixPaths(p);
                    else Abilities.use(p, m.index);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
