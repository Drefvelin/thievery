package net.tfminecraft.thievery.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StealGuiLayout {

    private final int logicalSlotCount;
    private final int guiSize;
    private final Map<Integer, Integer> logicalSlotToGuiSlot;
    private final Map<Integer, Integer> guiSlotToLogicalSlot;

    private StealGuiLayout(int logicalSlotCount, int guiSize,
            Map<Integer, Integer> logicalSlotToGuiSlot, Map<Integer, Integer> guiSlotToLogicalSlot) {
        this.logicalSlotCount = logicalSlotCount;
        this.guiSize = guiSize;
        this.logicalSlotToGuiSlot = logicalSlotToGuiSlot;
        this.guiSlotToLogicalSlot = guiSlotToLogicalSlot;
    }

    public static StealGuiLayout create(int logicalSlotCount) {
        int guiSize = resolveGuiSize(logicalSlotCount);
        Map<Integer, Integer> logicalToGui = buildRandomLayout(logicalSlotCount, guiSize);
        Map<Integer, Integer> guiToLogical = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : logicalToGui.entrySet()) {
            guiToLogical.put(entry.getValue(), entry.getKey());
        }
        return new StealGuiLayout(logicalSlotCount, guiSize,
                Collections.unmodifiableMap(logicalToGui), Collections.unmodifiableMap(guiToLogical));
    }

    public static int resolveGuiSize(int needed) {
        if (needed <= 9) {
            return 9;
        }
        if (needed <= 27) {
            return 27;
        }
        return 54;
    }

    public int getLogicalSlotCount() {
        return logicalSlotCount;
    }

    public int getGuiSize() {
        return guiSize;
    }

    public Map<Integer, Integer> getLogicalSlotToGuiSlot() {
        return logicalSlotToGuiSlot;
    }

    public int getGuiSlotForLogical(int logicalSlot) {
        Integer guiSlot = logicalSlotToGuiSlot.get(logicalSlot);
        return guiSlot != null ? guiSlot : -1;
    }

    public Integer getLogicalForGui(int guiSlot) {
        return guiSlotToLogicalSlot.get(guiSlot);
    }

    private static Map<Integer, Integer> buildRandomLayout(int logicalSlotCount, int guiSize) {
        Map<Integer, Integer> layout = new HashMap<>();
        List<Integer> guiSlots = new ArrayList<>();
        for (int i = 0; i < guiSize; i++) {
            guiSlots.add(i);
        }
        Collections.shuffle(guiSlots);

        for (int logicalSlot = 0; logicalSlot < logicalSlotCount; logicalSlot++) {
            if (guiSlots.isEmpty()) {
                break;
            }
            layout.put(logicalSlot, guiSlots.remove(0));
        }
        return layout;
    }
}
