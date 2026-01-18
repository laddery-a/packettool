package com.ladder.packettool.ui.editors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.text.Text;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Consumer;

public final class SneakEditor implements PacketEditor {

    private Mode mode = Mode.PRESS;

    private enum Mode {
        PRESS("PRESS_SHIFT_KEY"),
        RELEASE("RELEASE_SHIFT_KEY");

        final String mcEnumName;
        Mode(String mcEnumName) { this.mcEnumName = mcEnumName; }
        @Override public String toString() { return mcEnumName; }
    }

    @Override
    public String displayName() {
        return "Sneak (ClientCommand)";
    }

    @Override
    public void initWidgets(Consumer<ClickableWidget> add, int x, int y, int width) {
        CyclingButtonWidget<Mode> btn =
                CyclingButtonWidget.<Mode>builder(m -> Text.literal(m.mcEnumName))
                        .values(List.of(Mode.PRESS, Mode.RELEASE))
                        .initially(mode)
                        .build(x, y, width, 20, Text.literal("Mode"), (b, v) -> mode = v);

        add.accept(btn);
    }

    @Override
    public Packet<?> buildPacket() {
        try {
            // Resolve the MC enum constant by name (avoids compile errors if names differ)
            ClientCommandC2SPacket.Mode mcMode =
                    Enum.valueOf(ClientCommandC2SPacket.Mode.class, mode.mcEnumName);

            // Try common constructor shapes across mappings/versions
            Object player = MinecraftClient.getInstance().player;
            if (player == null) return null;

            // 1) (int entityId, Mode mode)
            Packet<?> p = tryCtor(mcMode, int.class, ClientCommandC2SPacket.Mode.class,
                    ((net.minecraft.client.network.ClientPlayerEntity) player).getId(), mcMode);
            if (p != null) return p;

            // 2) (ClientPlayerEntity player, Mode mode)
            p = tryCtor(mcMode, player.getClass(), ClientCommandC2SPacket.Mode.class, player, mcMode);
            if (p != null) return p;

            // 3) (Mode mode)
            p = tryCtor(mcMode, ClientCommandC2SPacket.Mode.class, mcMode);
            if (p != null) return p;

            // If none matched, give up cleanly
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Packet<?> tryCtor(ClientCommandC2SPacket.Mode mcMode, Class<?> p0, Class<?> p1, Object a0, Object a1) {
        try {
            Constructor<ClientCommandC2SPacket> c = ClientCommandC2SPacket.class.getDeclaredConstructor(p0, p1);
            c.setAccessible(true);
            return c.newInstance(a0, a1);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Packet<?> tryCtor(ClientCommandC2SPacket.Mode mcMode, Class<?> p0, Object a0) {
        try {
            Constructor<ClientCommandC2SPacket> c = ClientCommandC2SPacket.class.getDeclaredConstructor(p0);
            c.setAccessible(true);
            return c.newInstance(a0);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
