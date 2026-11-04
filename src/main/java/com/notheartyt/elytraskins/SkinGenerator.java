package com.notheartyt.elytraskins;

import java.util.Random;

/**
 * Generates one 64x32 Elytra texture per (category, index). Every skin has its own pattern,
 * parameters, palette order and category overlay, all derived from a fixed seed (deterministic).
 */
public final class SkinGenerator {
    public static final int W = 64;
    public static final int H = 32;
    public static final String[] PATTERNS = {
        "Stripes", "Checker", "Gradient", "Rings", "Clouds", "Cells", "Waves",
        "Scales", "Plasma", "Chevron", "Spiral", "Starfield", "Storm"
    };

    private static final int[][] PAL = {
        /* Anime     */ {0xFF6FAE, 0xFFD1E8, 0x7AD7FF, 0xFFF3A0, 0x7A4DFF},
        /* Dragon    */ {0x1F7A3A, 0x8B1A1A, 0xE0A030, 0x0E3B20, 0xB03030},
        /* Demon     */ {0x3A0008, 0xB00020, 0x1A0010, 0xFF3030, 0x601040},
        /* Angel     */ {0xFFFFFF, 0xFFF2B0, 0xDDEEFF, 0xFFD86B, 0xC8E0FF},
        /* Fire      */ {0xFF2000, 0xFF8A00, 0xFFD000, 0x7A0A00, 0xFFF0A0},
        /* Ice       */ {0xBFEFFF, 0x5FC8FF, 0xFFFFFF, 0x2A7FD0, 0x9AD8F0},
        /* Galaxy    */ {0x0B0B2E, 0x5A2FA0, 0xC040FF, 0x1A4FD0, 0xFFA0E0},
        /* Nether    */ {0x5A0F0F, 0xA02020, 0xFF6A00, 0x3A0A18, 0xE0C040},
        /* Ender     */ {0x0A0A0A, 0x7A2FD0, 0xC080FF, 0x1F0F3A, 0x00E0C0},
        /* RGB       */ {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF},
        /* Neon      */ {0x00FFC8, 0xFF00E6, 0xFFFF00, 0x00A2FF, 0x0A0A14},
        /* Crystal   */ {0xA0F0FF, 0xE0A0FF, 0xFFFFFF, 0x70D0C0, 0xB0B8FF},
        /* Nature    */ {0x2E8B3A, 0x7BC74D, 0x8B5A2B, 0xE8E06A, 0x1F5A2A},
        /* Gaming    */ {0x2020FF, 0xFF3030, 0x30FF30, 0xFFFF30, 0x202020},
        /* Legendary */ {0xFFD700, 0xFF8C00, 0x8A2BE2, 0xFFFFFF, 0x2A0A4A},
        /* Special   */ {0xFF4DA6, 0x4DFFEA, 0xFFE14D, 0x7A4DFF, 0xFFFFFF}
    };

    private SkinGenerator() {}

    public static int[] generate(int cat, int idx) {
        Random r = new Random(cat * 7919L + idx * 104729L + 12345L);
        int[] pal = PAL[cat].clone();
        for (int i = pal.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = pal[i];
            pal[i] = pal[j];
            pal[j] = t;
        }
        int pattern = idx % PATTERNS.length;
        long seed = r.nextInt(1 << 20);
        double fx = 0.10 + r.nextDouble() * 0.30;
        double fy = 0.10 + r.nextDouble() * 0.30;
        double ang = r.nextDouble() * Math.PI;
        double ph = r.nextDouble() * 50.0;
        int cell = 3 + r.nextInt(5);
        double cx = 12 + r.nextDouble() * 40;
        double cy = 6 + r.nextDouble() * 20;
        double[][] pts = new double[14][2];
        for (double[] p : pts) {
            p[0] = r.nextDouble() * W;
            p[1] = r.nextDouble() * H;
        }
        boolean sharp = pattern == 0 || pattern == 1 || pattern == 5 || pattern == 7 || pattern == 9;
        boolean rainbow = cat == 9;

        int[] out = new int[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double u = x / (double) W;
                double v = y / (double) H;
                double t;
                switch (pattern) {
                    case 0:
                        t = frac((x * Math.cos(ang) + y * Math.sin(ang)) * fx * 0.6 + ph);
                        break;
                    case 1:
                        t = (((x / cell) + (y / cell)) & 1) * 0.5 + hash(x / cell, y / cell, seed) * 0.5;
                        break;
                    case 2:
                        t = v + 0.18 * Math.sin(u * 6.0 + ph);
                        break;
                    case 3:
                        t = frac(Math.hypot(x - cx, (y - cy) * 2.0) * fx * 0.5 + ph);
                        break;
                    case 4:
                        t = fbm(x * 0.09 + ph, y * 0.14 + ph, seed);
                        break;
                    case 5: {
                        double d1 = 1e9, d2 = 1e9;
                        int id = 0;
                        for (int i = 0; i < pts.length; i++) {
                            double d = Math.hypot(x - pts[i][0], (y - pts[i][1]) * 1.6);
                            if (d < d1) {
                                d2 = d1;
                                d1 = d;
                                id = i;
                            } else if (d < d2) {
                                d2 = d;
                            }
                        }
                        t = id / (double) pts.length + ((d2 - d1) < 1.5 ? 0.5 : 0.0);
                        break;
                    }
                    case 6:
                        t = 0.5 + 0.5 * Math.sin(x * fx + Math.sin(y * fy * 1.5 + ph) * 2.0);
                        break;
                    case 7: {
                        int row = y / cell;
                        int offs = (row & 1) * (cell / 2);
                        int lx = (x + offs) % cell;
                        int ly = y % cell;
                        double d = Math.hypot(lx - cell / 2.0, ly - cell * 0.9) / cell;
                        t = d * 1.2 + hash((x + offs) / cell, row, seed) * 0.4;
                        break;
                    }
                    case 8:
                        t = 0.5 + (Math.sin(x * fx + ph) + Math.sin(y * fy + ph)
                                + Math.sin((x + y) * fx * 0.5) + Math.sin(Math.hypot(x - cx, y - cy) * fy)) / 8.0;
                        break;
                    case 9:
                        t = frac((Math.abs(x - 32) * fx + y * fy) * 0.7 + ph);
                        break;
                    case 10: {
                        double dx = x - cx;
                        double dy = (y - cy) * 2.0;
                        t = frac(Math.atan2(dy, dx) / (2 * Math.PI) + Math.hypot(dx, dy) * fx * 0.25 + ph);
                        break;
                    }
                    case 11:
                        t = 0.1 + 0.55 * fbm(x * 0.06 + ph, y * 0.1 + ph, seed);
                        break;
                    default:
                        t = fbm(x * 0.12 + ph, y * 0.2 + ph, seed) * 0.8 + 0.1 * v;
                        break;
                }

                int col = rainbow ? hsv(frac(t + ph * 0.1), 0.85, 1.0) : samplePal(pal, t, sharp);

                if (pattern == 11 && hash(x, y, seed + 5) > 0.965) col = mix(col, 0xFFFFFF, 0.9);
                if (pattern == 12) {
                    double n = noise(x * 0.18 + ph, y * 0.5, seed + 3);
                    if (Math.abs(n - 0.5) < 0.025) col = mix(col, 0xFFFFFF, 0.9);
                }

                col = overlay(cat, x, y, col, seed);
                double jit = 0.95 + 0.1 * hash(x * 3 + 1, y * 5 + 2, seed + 7);
                out[y * W + x] = scale(col, jit);
            }
        }
        return out;
    }

    private static int overlay(int cat, int x, int y, int col, long seed) {
        double h = hash(x, y, seed + 99);
        switch (cat) {
            case 0:
                if (h > 0.985) col = mix(col, 0xFFFFFF, 0.9);
                break;
            case 1:
                if (((x + ((y / 3) & 1) * 3) % 6 == 0) || y % 6 == 0) col = scale(col, 0.7);
                break;
            case 2: {
                double n = noise(x * 0.25, y * 0.25, seed);
                if (Math.abs(n - 0.5) < 0.03) col = mix(col, 0xFF3020, 0.85);
                break;
            }
            case 3:
                if (x % 5 == 0 && y % 2 == 0) col = mix(col, 0xFFFFFF, 0.35);
                if (h > 0.99) col = 0xFFFFFFFF;
                break;
            case 4: {
                double f = y / (double) H;
                col = mix(col, 0xFFD060, f * f * 0.5);
                if (h > 0.99) col = 0xFFFFFFA0;
                break;
            }
            case 5:
                if (h > 0.97) col = mix(col, 0xFFFFFF, 0.9);
                break;
            case 6:
            case 8:
            case 15:
                if (h > 0.975) col = mix(col, 0xFFFFFF, 0.95);
                break;
            case 7:
                if (h > 0.985) col = mix(col, 0xFFC040, 0.9);
                break;
            case 10:
                if (y % 4 == 0) col = mix(col, 0xFFFFFF, 0.35);
                if (x % 8 == 0) col = scale(col, 1.15);
                break;
            case 11:
                if ((x + y) % 9 == 0) col = mix(col, 0xFFFFFF, 0.5);
                break;
            case 12:
                if (h < 0.06) col = scale(col, 0.7);
                break;
            case 13:
                if (x % 4 == 0 || y % 4 == 0) col = scale(col, 0.62);
                break;
            case 14:
                if (x < 2 || y < 2 || x >= W - 2 || y >= H - 2) col = 0xFFFFD700;
                if (h > 0.98) col = 0xFFFFFFFF;
                break;
            default:
                break;
        }
        return col;
    }

    private static int samplePal(int[] pal, double t, boolean sharp) {
        int n = pal.length;
        double p = frac(t) * n;
        int i = (int) p;
        double f = p - i;
        int a = pal[i % n];
        int b = pal[(i + 1) % n];
        if (sharp) return 0xFF000000 | a;
        f = f * f * (3 - 2 * f);
        return mix(a, b, f);
    }

    static double frac(double d) {
        return d - Math.floor(d);
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    static int scale(int c, double k) {
        int r = Math.min(255, (int) (((c >> 16) & 255) * k));
        int g = Math.min(255, (int) (((c >> 8) & 255) * k));
        int b = Math.min(255, (int) ((c & 255) * k));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int hsv(double h, double s, double v) {
        h = frac(h) * 6.0;
        int i = (int) h;
        double f = h - i;
        double p = v * (1 - s);
        double q = v * (1 - s * f);
        double t = v * (1 - s * (1 - f));
        double r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    static double hash(int x, int y, long s) {
        long h = x * 374761393L + y * 668265263L + s * 1442695041L + 1013904223L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= (h >>> 16);
        return (h & 0xFFFFFFL) / (double) 0x1000000L;
    }

    static double noise(double x, double y, long s) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        double fx = x - xi;
        double fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hash(xi, yi, s);
        double b = hash(xi + 1, yi, s);
        double c = hash(xi, yi + 1, s);
        double d = hash(xi + 1, yi + 1, s);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    static double fbm(double x, double y, long s) {
        return noise(x, y, s) * 0.55 + noise(x * 2.1, y * 2.1, s + 1) * 0.3 + noise(x * 4.3, y * 4.3, s + 2) * 0.15;
    }
}
