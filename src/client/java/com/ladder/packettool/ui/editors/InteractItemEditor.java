package com.ladder.packettool.ui.editors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class InteractItemEditor implements PacketEditor {
    private static final List<Hand> HANDS = Arrays.asList(Hand.values());
    private Hand hand = Hand.MAIN_HAND;

    @Override
    public String displayName() {
        return "Interact Item (Open Book)";
    }

    @Override
    public void initWidgets(Consumer<ClickableWidget> addWidget, int x, int y, int width) {
        CyclingButtonWidget<Hand> handBtn =
                CyclingButtonWidget.<Hand>builder(v -> Text.literal(v.toString()))
                        .values(HANDS)
                        .initially(hand)
                        .build(x, y, width, 20, Text.literal("Hand"), (btn, value) -> hand = value);

        addWidget.accept(handBtn);
    }

    @Override
    public Packet<?> buildPacket() {
        var player = MinecraftClient.getInstance().player;
        float yaw = player != null ? player.getYaw() : 0f;
        float pitch = player != null ? player.getPitch() : 0f;
        return new PlayerInteractItemC2SPacket(hand, 0, yaw, pitch);
    }
}
