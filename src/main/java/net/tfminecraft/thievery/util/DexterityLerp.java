package net.tfminecraft.thievery.util;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class DexterityLerp {

    private static NavigableMap<Integer, Double> map = defaultMap();

    private DexterityLerp() {}

    public static void load(Map<?, ?> fromConfig) {
        TreeMap<Integer, Double> parsed = new TreeMap<>();
        if (fromConfig != null) {
            for (Map.Entry<?, ?> entry : fromConfig.entrySet()) {
                try {
                    int dex = Integer.parseInt(entry.getKey().toString().trim());
                    double value = ((Number) entry.getValue()).doubleValue();
                    parsed.put(dex, value);
                } catch (NumberFormatException | ClassCastException ignored) {
                }
            }
        }
        map = parsed.isEmpty() ? defaultMap() : parsed;
    }

    public static double getValue(int dexterity) {
        if (map.isEmpty()) {
            return 1.0;
        }
        Map.Entry<Integer, Double> floor = map.floorEntry(dexterity);
        Map.Entry<Integer, Double> ceiling = map.ceilingEntry(dexterity);
        if (floor == null) {
            return ceiling.getValue();
        }
        if (ceiling == null) {
            return floor.getValue();
        }
        if (floor.getKey().equals(ceiling.getKey())) {
            return floor.getValue();
        }
        double t = (dexterity - floor.getKey()) / (double) (ceiling.getKey() - floor.getKey());
        return floor.getValue() + t * (ceiling.getValue() - floor.getValue());
    }

    private static NavigableMap<Integer, Double> defaultMap() {
        NavigableMap<Integer, Double> defaults = new TreeMap<>();
        defaults.put(0, 1.0);
        defaults.put(40, 2.0);
        return defaults;
    }
}
