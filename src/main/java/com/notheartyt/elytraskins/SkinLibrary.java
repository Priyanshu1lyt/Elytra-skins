package com.notheartyt.elytraskins;

import java.util.LinkedHashMap;
import java.util.Map;

/** 16 categories x 13 skins = 208 skins. Pixels are generated on demand and cached (small LRU, low RAM). */
public final class SkinLibrary {
    public static final String[] CATEGORIES = {
        "Anime", "Dragon", "Demon", "Angel", "Fire", "Ice", "Galaxy", "Nether",
        "Ender", "RGB", "Neon", "Crystal", "Nature", "Gaming", "Legendary", "Special Edition"
    };
    public static final int PER = SkinGenerator.PATTERNS.length;
    public static final int COUNT = CATEGORIES.length * PER;

    private static final Map<Integer, int[]> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, int[]> eldest) {
            return size() > 96;
        }
    };

    private SkinLibrary() {}

    public static int category(int id) { return id / PER; }

    public static String categoryName(int id) { return CATEGORIES[id / PER]; }

    public static String name(int id) {
        return CATEGORIES[id / PER] + " " + SkinGenerator.PATTERNS[id % PER];
    }

    public static int[] pixels(int id) {
        int[] p = CACHE.get(id);
        if (p == null) {
            p = SkinGenerator.generate(id / PER, id % PER);
            CACHE.put(id, p);
        }
        return p;
    }
}
