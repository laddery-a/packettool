package com.ladder.packettool;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;

public final class NetSend {
    private NetSend() {}

    public static void send(Packet<?> packet) {
        if (packet == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        var handler = client.getNetworkHandler();
        if (handler == null) return;

        handler.sendPacket(packet);
    }
}
