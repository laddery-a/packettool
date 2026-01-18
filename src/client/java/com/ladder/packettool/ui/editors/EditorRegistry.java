package com.ladder.packettool.ui.editors;

import java.util.List;

public final class EditorRegistry {
    public static List<PacketEditor> all() {
        return List.of(
                new HandSwingEditor(),
                new InteractItemEditor(),
                new PlayerMoveEditor(),
                new InteractBlockEditor(),
                new BlockActionEditor(),
                new SneakEditor()
        );
    }
}
