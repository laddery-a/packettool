package com.ladder.packettool.ui.editors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class BlockActionEditor implements PacketEditor {

    private static final List<PlayerActionC2SPacket.Action> ACTIONS = List.of(
            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
            PlayerActionC2SPacket.Action.DROP_ITEM,
            PlayerActionC2SPacket.Action.DROP_ALL_ITEMS,
            PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND
    );

    private static final List<Direction> DIRECTIONS = List.of(
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN
    );

    private PlayerActionC2SPacket.Action action = PlayerActionC2SPacket.Action.START_DESTROY_BLOCK;
    private Direction direction = Direction.UP;

    private boolean useCrosshair = true;

    private TextFieldWidget xField, yField, zField;

    @Override
    public String displayName() {
        return "Block Actions (PlayerActionC2S)";
    }

    @Override
    public void initWidgets(Consumer<ClickableWidget> add, int x0, int y0, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        int rowH = 22;
        int gap = 6;
        int wHalf = (width - gap) / 2;

        // Action
        CyclingButtonWidget<PlayerActionC2SPacket.Action> actionBtn =
                CyclingButtonWidget.<PlayerActionC2SPacket.Action>builder(a -> Text.literal(a.name()))
                        .values(ACTIONS)
                        .initially(action)
                        .build(x0, y0, width, 20, Text.literal("Action"), (b, v) -> action = v);
        add.accept(actionBtn);

        int y = y0 + 30;

        // Direction
        CyclingButtonWidget<Direction> dirBtn =
                CyclingButtonWidget.<Direction>builder(d -> Text.literal(d.asString().toUpperCase(Locale.ROOT)))
                        .values(DIRECTIONS)
                        .initially(direction)
                        .build(x0, y, width, 20, Text.literal("Direction"), (b, v) -> direction = v);
        add.accept(dirBtn);

        y += rowH + 6;

        // Use crosshair toggle
        CyclingButtonWidget<Boolean> sourceBtn =
                CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "Crosshair" : "Manual"))
                        .values(List.of(Boolean.TRUE, Boolean.FALSE))
                        .initially(useCrosshair)
                        .build(x0, y, width, 20, Text.literal("Target Source"), (b, v) -> useCrosshair = v);
        add.accept(sourceBtn);

        y += rowH + 6;

        // Manual BlockPos fields (always visible; ignored if crosshair is enabled)
        BlockPos seed = seedPosFromCrosshair(client);

        xField = field(tr, add, x0, y, wHalf, "x", Integer.toString(seed.getX()));
        yField = field(tr, add, x0 + wHalf + gap, y, wHalf, "y", Integer.toString(seed.getY()));
        y += rowH;

        zField = field(tr, add, x0, y, wHalf, "z", Integer.toString(seed.getZ()));

        ButtonWidget useHit = ButtonWidget.builder(Text.literal("Use crosshair pos"), b -> {
                    BlockPos p = seedPosFromCrosshair(client);
                    xField.setText(Integer.toString(p.getX()));
                    yField.setText(Integer.toString(p.getY()));
                    zField.setText(Integer.toString(p.getZ()));
                })
                .position(x0 + wHalf + gap, y)
                .size(wHalf, 20)
                .build();
        add.accept(useHit);

        y += rowH + 6;

        ButtonWidget showTarget = ButtonWidget.builder(Text.literal("Show resolved target"), b -> {
                    if (client.player == null) return;
                    ResolvedTarget t = resolveTarget(client);
                    client.player.sendMessage(Text.literal(
                            "Target=" + t.pos.getX() + " " + t.pos.getY() + " " + t.pos.getZ() +
                                    " dir=" + t.dir.asString() + " source=" + (t.fromCrosshair ? "crosshair" : "manual")
                    ), false);
                })
                .position(x0, y)
                .size(width, 20)
                .build();
        add.accept(showTarget);
    }

    @Override
    public Packet<?> buildPacket() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;

        ResolvedTarget t = resolveTarget(client);

        // Important: Some actions ignore pos/dir server-side, but the packet still requires them.
        return new PlayerActionC2SPacket(action, t.pos, t.dir, 0);
        // The trailing "0" is the sequence; vanilla uses it for digging ack logic.
        // For testing it is fine; if you want accurate sequencing, we can hook into the client's sequence manager next.
    }

    private ResolvedTarget resolveTarget(MinecraftClient client) {
        if (useCrosshair) {
            HitResult hit = client.crosshairTarget;
            if (hit instanceof BlockHitResult bhr) {
                return new ResolvedTarget(bhr.getBlockPos(), bhr.getSide(), true);
            }
            // Fallback: player position, UP
            BlockPos fallback = client.player != null ? client.player.getBlockPos() : BlockPos.ORIGIN;
            return new ResolvedTarget(fallback, Direction.UP, true);
        }

        int bx = parseInt(xField.getText(), 0);
        int by = parseInt(yField.getText(), 0);
        int bz = parseInt(zField.getText(), 0);
        return new ResolvedTarget(new BlockPos(bx, by, bz), direction, false);
    }

    private static BlockPos seedPosFromCrosshair(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult bhr) return bhr.getBlockPos();
        if (client.player != null) return client.player.getBlockPos();
        return BlockPos.ORIGIN;
    }

    private record ResolvedTarget(BlockPos pos, Direction dir, boolean fromCrosshair) {}

    private static TextFieldWidget field(TextRenderer tr, Consumer<ClickableWidget> add,
                                         int x, int y, int w, String label, String value) {
        TextFieldWidget tf = new TextFieldWidget(tr, x, y, w, 20, Text.literal(label));
        tf.setText(value);
        tf.setMaxLength(32);
        add.accept(tf);
        return tf;
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
