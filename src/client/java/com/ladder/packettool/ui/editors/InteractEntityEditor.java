package com.ladder.packettool.ui.editors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.List;
import java.util.function.Consumer;

public final class InteractEntityEditor implements PacketEditor {

    private enum Action { ATTACK, INTERACT }

    private static final List<Action> ACTIONS = List.of(Action.values());
    private static final List<Hand> HANDS = List.of(Hand.MAIN_HAND, Hand.OFF_HAND);

    private Action action = Action.ATTACK;
    private Hand hand = Hand.MAIN_HAND;

    @Override
    public String displayName() {
        return "Entity: Attack/Interact (Crosshair)";
    }

    @Override
    public void initWidgets(Consumer<ClickableWidget> add, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        int rowH = 22;
        int gap = 6;
        int wHalf = (width - gap) / 2;

        // Action selector
        CyclingButtonWidget<Action> actionBtn =
                CyclingButtonWidget.<Action>builder(a -> Text.literal(a.name()))
                        .values(ACTIONS)
                        .initially(action)
                        .build(x, y, width, 20, Text.literal("Action"), (b, v) -> action = v);
        add.accept(actionBtn);
        y += rowH + 6;

        // Hand selector
        CyclingButtonWidget<Hand> handBtn =
                CyclingButtonWidget.<Hand>builder(h -> Text.literal(h == Hand.MAIN_HAND ? "MAIN_HAND" : "OFF_HAND"))
                        .values(HANDS)
                        .initially(hand)
                        .build(x, y, width, 20, Text.literal("Hand"), (b, v) -> hand = v);
        add.accept(handBtn);
        y += rowH + 6;

        // Helpful hint button (no packets, just chat feedback)
        ButtonWidget hint = ButtonWidget.builder(Text.literal("Show target in chat"), b -> {
                    var hit = client.crosshairTarget;
                    if (client.player == null) return;

                    if (hit instanceof EntityHitResult ehr) {
                        client.player.sendMessage(Text.literal("Target: " + ehr.getEntity().getType().toString()
                                + " id=" + ehr.getEntity().getId()), false);
                    } else {
                        client.player.sendMessage(Text.literal("No entity targeted."), false);
                    }
                })
                .position(x, y)
                .size(width, 20)
                .build();
        add.accept(hint);
    }

    @Override
    public Packet<?> buildPacket() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult ehr)) {
            client.player.sendMessage(Text.literal("No entity targeted."), true);
            return null;
        }

        var entity = ehr.getEntity();
        boolean sneaking = client.player.isSneaking();

        // NOTE:
        // - Attack does NOT use a hand parameter; interact does.
        // - This packet actually triggers real server-side actions (unlike HandSwing).
        return switch (action) {
            case ATTACK -> PlayerInteractEntityC2SPacket.attack(entity, sneaking);
            case INTERACT -> PlayerInteractEntityC2SPacket.interact(entity, sneaking, hand);
        };
    }
}
