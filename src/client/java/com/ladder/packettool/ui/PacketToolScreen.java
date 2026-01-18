package com.ladder.packettool.ui;

import com.ladder.packettool.NetSend;
import com.ladder.packettool.ui.editors.EditorRegistry;
import com.ladder.packettool.ui.editors.PacketEditor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public final class PacketToolScreen extends Screen {
    private final List<PacketEditor> editors = EditorRegistry.all();
    private PacketEditor active = editors.getFirst();

    public PacketToolScreen() {
        super(Text.literal("Packet Tool"));
    }

    @Override
    protected void init() {
        // Clear existing widgets so switching editors doesn't overlap
        this.clearChildren();

        int x = 20;
        int y = 20;
        int w = 260;

        // Packet selector
        CyclingButtonWidget<PacketEditor> selector =
                CyclingButtonWidget.<PacketEditor>builder(e -> Text.literal(e.displayName()))
                        .values(editors)
                        .initially(active)
                        .build(x, y, w, 20, Text.literal("Packet"), (b, v) -> {
                            active = v;
                            init(); // rebuild UI cleanly
                        });

        addDrawableChild(selector);

        // Editor-specific widgets
        Consumer<ClickableWidget> add = this::addDrawableChild;
        active.initWidgets(add, x, y + 30, w);

        // Send
        addDrawableChild(
                ButtonWidget.builder(Text.literal("Send"), b -> {
                            var pkt = active.buildPacket();
                            if (pkt != null) NetSend.send(pkt);
                        })
                        .position(x, this.height - 40)
                        .size(80, 20)
                        .build()
        );

        // Close
        addDrawableChild(
                ButtonWidget.builder(Text.literal("Close"), b -> {
                            if (this.client != null) this.client.setScreen(null);
                        })
                        .position(x + 90, this.height - 40)
                        .size(80, 20)
                        .build()
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
