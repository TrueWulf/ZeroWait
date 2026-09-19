package dev.truewulf.zerowait;

import net.minecraft.client.Minecraft;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?}

import java.util.LinkedHashMap;
import java.util.Map;

public final class BootTimings {
    private static final long START = System.nanoTime();
    private static final Map<String, Long> MARKS = new LinkedHashMap<>();
    private static volatile long totalMillis = -1L;
    private static boolean reported;

    private BootTimings() {
    }

    public static synchronized void mark(String phase) {
        if (!reported) {
            MARKS.put(phase, elapsedMillis());
        }
    }

    public static synchronized void report() {
        if (reported) {
            return;
        }
        reported = true;
        totalMillis = elapsedMillis();
        long previous = 0L;
        for (Map.Entry<String, Long> entry : MARKS.entrySet()) {
            long value = entry.getValue();
            ZeroWait.LOGGER.info("Boot {}ms (+{}ms): {}", value, value - previous, entry.getKey());
            previous = value;
        }
        ZeroWait.LOGGER.info("Total boot time: {}.{}s", totalMillis / 1000L, String.format("%03d", totalMillis % 1000L));
    }

    public static synchronized void onMenuShown() {
        mark("menu shown");
        report();
    }

    public static String titleScreenLine() {
        long total = totalMillis;
        if (total < 0L) {
            return null;
        }
        return "ZeroWait: launched in " + (total / 1000L) + "." + String.format("%02d", (total % 1000L) / 10L) + "s";
    }

//? if <26.1 {
    public static void drawBootLine(GuiGraphics graphics) {
        if (!ZeroWaitConfig.get().showBootTimeOverlay) {
            return;
        }
        String line = titleScreenLine();
        if (line == null) {
            return;
        }
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, line, 4, graphics.guiHeight() - 30, 0xFFFFFFFF, true);
    }
//?}

    private static long elapsedMillis() {
        return (System.nanoTime() - START) / 1_000_000L;
    }
}
