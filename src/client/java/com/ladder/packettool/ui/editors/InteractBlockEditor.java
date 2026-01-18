package com.ladder.packettool.ui.editors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class InteractBlockEditor implements PacketEditor {

    private static final List<Hand> HANDS = List.of(Hand.MAIN_HAND, Hand.OFF_HAND);
    private static final List<Direction> DIRS = List.of(
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN
    );

    private Hand hand = Hand.MAIN_HAND;
    private boolean useCrosshair = true;

    private Direction side = Direction.UP;
    private boolean insideBlock = false;

    private TextFieldWidget xField, yField, zField;
    private TextFieldWidget hitXField, hitYField, hitZField;

    @Override
    public String displayName() {
        return "Interact Block (Use/Place)";
    }

    @Override
    public void initWidgets(Consumer<ClickableWidget> add, int x0, int y0, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        int rowH = 22;
        int gap = 6;
        int wHalf = (width - gap) / 2;

        // Hand selector
        CyclingButtonWidget<Hand> handBtn =
                CyclingButtonWidget.<Hand>builder(h -> Text.literal(h == Hand.MAIN_HAND ? "MAIN_HAND" : "OFF_HAND"))
                        .values(HANDS)
                        .initially(hand)
                        .build(x0, y0, width, 20, Text.literal("Hand"), (b, v) -> hand = v);
        add.accept(handBtn);

        int y = y0 + 30;

        // Source selector (crosshair/manual)
        CyclingButtonWidget<Boolean> sourceBtn =
                CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "Crosshair" : "Manual"))
                        .values(List.of(Boolean.TRUE, Boolean.FALSE))
                        .initially(useCrosshair)
                        .build(x0, y, width, 20, Text.literal("Target Source"), (b, v) -> useCrosshair = v);
        add.accept(sourceBtn);

        y += rowH + 6;

        // Side selector
        CyclingButtonWidget<Direction> sideBtn =
                CyclingButtonWidget.<Direction>builder(d -> Text.literal(d.asString().toUpperCase(Locale.ROOT)))
                        .values(DIRS)
                        .initially(side)
                        .build(x0, y, width, 20, Text.literal("Side"), (b, v) -> side = v);
        add.accept(sideBtn);

        y += rowH + 6;

        // insideBlock checkbox
        CheckboxWidget insideBox =
                CheckboxWidget.builder(Text.literal("insideBlock"), tr)
                        .pos(x0, y)
                        .checked(insideBlock)
                        .callback((box, value) -> insideBlock = value)
                        .build();
        add.accept(insideBox);

        y += rowH + 6;

        // Seed from crosshair if available
        Resolved r = resolveFromCrosshair(client);
        BlockPos seedPos = r.pos != null ? r.pos : (client.player != null ? client.player.getBlockPos() : BlockPos.ORIGIN);
        Vec3d seedHit = r.hitVec != null ? r.hitVec : Vec3d.ofCenter(seedPos);
        Direction seedSide = r.side != null ? r.side : side;

        // BlockPos fields
        xField = field(tr, add, x0, y, wHalf, "x", Integer.toString(seedPos.getX()));
        yField = field(tr, add, x0 + wHalf + gap, y, wHalf, "y", Integer.toString(seedPos.getY()));
        y += rowH;

        zField = field(tr, add, x0, y, wHalf, "z", Integer.toString(seedPos.getZ()));

        ButtonWidget useHitBtn = ButtonWidget.builder(Text.literal("Use crosshair target"), b -> {
                    Resolved rr = resolveFromCrosshair(client);
                    if (rr.pos == null || rr.hitVec == null || rr.side == null) return;

                    xField.setText(Integer.toString(rr.pos.getX()));
                    yField.setText(Integer.toString(rr.pos.getY()));
                    zField.setText(Integer.toString(rr.pos.getZ()));

                    hitXField.setText(fmt(rr.hitVec.x));
                    hitYField.setText(fmt(rr.hitVec.y));
                    hitZField.setText(fmt(rr.hitVec.z));

                    side = rr.side;
                })
                .position(x0 + wHalf + gap, y)
                .size(wHalf, 20)
                .build();
        add.accept(useHitBtn);

        y += rowH + 6;

        // hitVec fields (Vec3d)
        hitXField = field(tr, add, x0, y, wHalf, "hitX", fmt(seedHit.x));
        hitYField = field(tr, add, x0 + wHalf + gap, y, wHalf, "hitY", fmt(seedHit.y));
        y += rowH;

        hitZField = field(tr, add, x0, y, wHalf, "hitZ", fmt(seedHit.z));

        ButtonWidget showBtn = ButtonWidget.builder(Text.literal("Show resolved target"), b -> {
                    if (client.player == null) return;
                    BlockHitResult bhr = buildHitResult(client);
                    client.player.sendMessage(Text.literal(
                            "pos=" + bhr.getBlockPos().getX() + " " + bhr.getBlockPos().getY() + " " + bhr.getBlockPos().getZ() +
                                    " side=" + bhr.getSide().asString() +
                                    " hit=" + fmt(bhr.getPos().x) + "," + fmt(bhr.getPos().y) + "," + fmt(bhr.getPos().z) +
                                    " inside=" + bhr.isInsideBlock() +
                                    " source=" + (useCrosshair ? "crosshair" : "manual")
                    ), false);
                })
                .position(x0 + wHalf + gap, y)
                .size(wHalf, 20)
                .build();
        add.accept(showBtn);
    }

    @Override
    public Packet<?> buildPacket() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;

        BlockHitResult bhr = buildHitResult(client);

        // Most versions include a "sequence" int for block interaction; 0 is fine for testing.
        return new PlayerInteractBlockC2SPacket(hand, bhr, 0);
    }

    private BlockHitResult buildHitResult(MinecraftClient client) {
        // If using crosshair and it is a real block hit, use it directly
        if (useCrosshair) {
            HitResult hit = client.crosshairTarget;
            if (hit instanceof BlockHitResult bhr) {
                return bhr;
            }
        }

        // Manual
        int bx = parseInt(xField.getText(), 0);
        int by = parseInt(yField.getText(), 0);
        int bz = parseInt(zField.getText(), 0);

        double hx = parseDouble(hitXField.getText(), bx + 0.5);
        double hy = parseDouble(hitYField.getText(), by + 0.5);
        double hz = parseDouble(hitZField.getText(), bz + 0.5);

        BlockPos pos = new BlockPos(bx, by, bz);
        Vec3d hitVec = new Vec3d(hx, hy, hz);

        return new BlockHitResult(hitVec, side, pos, insideBlock);
    }

    private static Resolved resolveFromCrosshair(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult bhr) {
            return new Resolved(bhr.getBlockPos(), bhr.getSide(), bhr.getPos());
        }
        return new Resolved(null, null, null);
    }

    private record Resolved(BlockPos pos, Direction side, Vec3d hitVec) {}

    private static TextFieldWidget field(TextRenderer tr, Consumer<ClickableWidget> add,
                                         int x, int y, int w, String label, String value) {
        TextFieldWidget tf = new TextFieldWidget(tr, x, y, w, 20, Text.literal(label));
        tf.setText(value);
        tf.setMaxLength(64);
        add.accept(tf);
        return tf;
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.5f", v);
    }

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception ignored) { return fallback; }
    }

    private static double parseDouble(String s, double fallback) {
        try { return Double.parseDouble(s.trim().replace(",", ".")); } catch (Exception ignored) { return fallback; }
    }
}

