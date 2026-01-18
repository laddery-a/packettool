package com.ladder.packettool.ui.editors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class PlayerMoveEditor implements PacketEditor {

    private enum Mode { FULL, POSITION, LOOK, ON_GROUND }

    private static final List<Mode> MODES = List.of(Mode.values());

    private Mode mode = Mode.FULL;

    private double x, y, z;
    private float yaw, pitch;
    private boolean onGround = true;
    private boolean horizontalCollision = false;

    // NEW: makes rotation/position visible instantly
    private boolean applyLocally = false;

    private TextFieldWidget xField, yField, zField, yawField, pitchField;

    @Override
    public String displayName() {
        return "Player Move (Editable)";
    }

    @Override
    public void initWidgets(Consumer<ClickableWidget> add, int x0, int y0, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        // Seed from current player state
        if (client.player != null) {
            x = client.player.getX();
            y = client.player.getY();
            z = client.player.getZ();
            yaw = client.player.getYaw();
            pitch = client.player.getPitch();
            onGround = client.player.isOnGround();
            horizontalCollision = client.player.horizontalCollision;
        }

        int rowH = 22;
        int gap = 6;
        int wHalf = (width - gap) / 2;

        // Mode selector
        CyclingButtonWidget<Mode> modeBtn =
                CyclingButtonWidget.<Mode>builder(m -> Text.literal(m.name()))
                        .values(MODES)
                        .initially(mode)
                        .build(x0, y0, width, 20, Text.literal("Move Packet Type"), (b, v) -> mode = v);
        add.accept(modeBtn);

        int yPos = y0 + 30;

        // Row 1: x / y
        xField = field(tr, add, x0, yPos, wHalf, "x", fmt(x));
        yField = field(tr, add, x0 + wHalf + gap, yPos, wHalf, "y", fmt(y));
        yPos += rowH;

        // Row 2: z / yaw
        zField = field(tr, add, x0, yPos, wHalf, "z", fmt(z));
        yawField = field(tr, add, x0 + wHalf + gap, yPos, wHalf, "yaw", fmt(yaw));
        yPos += rowH;

        // Row 3: pitch / onGround checkbox
        pitchField = field(tr, add, x0, yPos, wHalf, "pitch", fmt(pitch));

        CheckboxWidget onGroundBox =
                CheckboxWidget.builder(Text.literal("onGround"), tr)
                        .pos(x0 + wHalf + gap, yPos)
                        .checked(onGround)
                        .callback((box, value) -> onGround = value)
                        .build();
        add.accept(onGroundBox);
        yPos += rowH;

        // Row 4: horizontalCollision checkbox
        CheckboxWidget horizCollBox =
                CheckboxWidget.builder(Text.literal("horizontalCollision"), tr)
                        .pos(x0, yPos)
                        .checked(horizontalCollision)
                        .callback((box, value) -> horizontalCollision = value)
                        .build();
        add.accept(horizCollBox);
        yPos += rowH;

        // Row 5: apply locally checkbox
        CheckboxWidget applyLocalBox =
                CheckboxWidget.builder(Text.literal("apply locally (camera/pos)"), tr)
                        .pos(x0, yPos)
                        .checked(applyLocally)
                        .callback((box, value) -> applyLocally = value)
                        .build();
        add.accept(applyLocalBox);
        yPos += rowH + 6;

        // Buttons row
        ButtonWidget usePos = ButtonWidget.builder(Text.literal("Use current pos"), b -> {
                    if (client.player == null) return;
                    xField.setText(fmt(client.player.getX()));
                    yField.setText(fmt(client.player.getY()));
                    zField.setText(fmt(client.player.getZ()));
                })
                .position(x0, yPos)
                .size(wHalf, 20)
                .build();
        add.accept(usePos);

        ButtonWidget useRot = ButtonWidget.builder(Text.literal("Use current rot"), b -> {
                    if (client.player == null) return;
                    yawField.setText(fmt(client.player.getYaw()));
                    pitchField.setText(fmt(client.player.getPitch()));
                })
                .position(x0 + wHalf + gap, yPos)
                .size(wHalf, 20)
                .build();
        add.accept(useRot);
    }

    @Override
    public Packet<?> buildPacket() {
        x = parseDouble(xField.getText(), x);
        y = parseDouble(yField.getText(), y);
        z = parseDouble(zField.getText(), z);
        yaw = (float) parseDouble(yawField.getText(), yaw);
        pitch = (float) parseDouble(pitchField.getText(), pitch);

        // Make changes visible instantly (optional)
        if (applyLocally) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                // Rotation is what you care about visually
                client.player.setYaw(yaw);
                client.player.setPitch(pitch);

                // Only apply position locally if this packet includes position
                if (mode == Mode.FULL || mode == Mode.POSITION) {
                    client.player.setPosition(x, y, z);
                }
            }
        }

        // 1.21.4: movement packets include horizontalCollision too
        return switch (mode) {
            case FULL -> new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, onGround, horizontalCollision);
            case POSITION -> new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, horizontalCollision);
            case LOOK -> new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround, horizontalCollision);
            case ON_GROUND -> new PlayerMoveC2SPacket.OnGroundOnly(onGround, horizontalCollision);
        };
    }

    private static TextFieldWidget field(TextRenderer tr, Consumer<ClickableWidget> add,
                                         int x, int y, int w, String label, String value) {
        TextFieldWidget tf = new TextFieldWidget(tr, x, y, w, 20, Text.literal(label));
        tf.setText(value);
        tf.setMaxLength(64);
        add.accept(tf);
        return tf;
    }

    private static String fmt(double v) { return String.format(Locale.ROOT, "%.5f", v); }
    private static String fmt(float v)  { return String.format(Locale.ROOT, "%.3f", v); }

    private static double parseDouble(String s, double fallback) {
        try {
            return Double.parseDouble(s.trim().replace(",", "."));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
