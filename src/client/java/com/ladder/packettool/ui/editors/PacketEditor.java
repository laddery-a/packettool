package com.ladder.packettool.ui.editors;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.network.packet.Packet;

import java.util.function.Consumer;

public interface PacketEditor {

    /** Name shown in the packet selector */
    String displayName();

    /**
     * Add editor-specific widgets.
     *
     * @param adder function to add widgets to the screen
     * @param x left position
     * @param y top position
     * @param width widget width
     */
    void initWidgets(Consumer<ClickableWidget> adder, int x, int y, int width);

    /** Build the packet to send */
    Packet<?> buildPacket();
}
