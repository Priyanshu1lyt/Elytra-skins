package com.notheartyt.elytraskins;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ElytraSkinsScreen extends Screen {
    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {}

    private static final int PW = 440, PH = 262;
    private static final int WHITE = 0xFFFFFFFF, GRAY = 0xFF9A9AB0, GREEN = 0xFF55FF55,
            CYAN = 0xFF00E5FF, MAGENTA = 0xFFFF2BD6, YELLOW = 0xFFFFEE55;
    private static final int BG = 0xFF0B0B14, PANEL = 0xFF12121F, CARD = 0xFF1B1B2E, CARD_SEL = 0xFF2A1F4D;

    private final List<Hit> hits = new ArrayList<>();
    private final long openedAt = System.currentTimeMillis();
    private final List<Integer> list = new ArrayList<>();

    private int filter = 0; // 0 all, 1 favorites, 2 recent, 3+ = category
    private String query = "";
    private boolean dirty = true;
    private int scroll = 0, maxScroll = 0;
    private int preview;
    private String status = "Pick a skin, then press APPLY.";
    private int statusColor = GRAY;

    public ElytraSkinsScreen() {
        super(Text.literal("Elytra Skins"));
        int sel = ElytraSkinManager.cfg.selected;
        preview = sel >= 0 ? sel : 0;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void rebuild() {
        list.clear();
        SkinConfig c = ElytraSkinManager.cfg;
        String q = query.toLowerCase(Locale.ROOT);
        List<Integer> src = new ArrayList<>();
        if (filter == 1) src.addAll(c.favorites);
        else if (filter == 2) src.addAll(c.recent);
        else for (int i = 0; i < SkinLibrary.COUNT; i++) src.add(i);
        for (int id : src) {
            if (id < 0 || id >= SkinLibrary.COUNT) continue;
            if (filter >= 3 && SkinLibrary.category(id) != filter - 3) continue;
            if (!q.isEmpty() && !SkinLibrary.name(id).toLowerCase(Locale.ROOT).contains(q)) continue;
            list.add(id);
        }
        scroll = 0;
    }

    private void setStatus(String s, int color) {
        status = s;
        statusColor = color;
    }

    // ------------------------------------------------------------ helpers

    private void addHit(int x1, int y1, int x2, int y2, Runnable r) {
        hits.add(new Hit(x1, y1, x2, y2, r));
    }

    private void drawCentered(DrawContext ctx, String s, int x, int y, int w, int color) {
        ctx.drawText(textRenderer, s, x + (w - textRenderer.getWidth(s)) / 2, y, color, true);
    }

    private static int lighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 28);
        int g = Math.min(255, ((c >> 8) & 0xFF) + 28);
        int b = Math.min(255, (c & 0xFF) + 28);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int shade(int c, double k) {
        int r = (int) (((c >> 16) & 0xFF) * k);
        int g = (int) (((c >> 8) & 0xFF) * k);
        int b = (int) ((c & 0xFF) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label, int bg, int accent, int mx, int my, Runnable r) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        ctx.fill(x, y, x + w, y + h, accent);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? lighten(bg) : bg);
        drawCentered(ctx, label, x, y + (h - 8) / 2, w, WHITE);
        addHit(x, y, x + w, y + h, r);
    }

    private void drawSwatch(DrawContext ctx, int id, int x, int y) {
        int[] pix = SkinLibrary.pixels(id);
        for (int cy = 0; cy < 6; cy++) {
            for (int cx = 0; cx < 12; cx++) {
                int tx = cx * 5 + 2;
                int ty = cy * 5 + 1;
                ctx.fill(x + cx * 3, y + cy * 3, x + cx * 3 + 3, y + cy * 3 + 3, pix[ty * SkinGenerator.W + tx]);
            }
        }
    }

    /** Fake-3D spinning Elytra: wings are sampled from the skin texture and squashed by cos(angle). */
    private void drawPreview(DrawContext ctx, int x, int y, int w, int h, int id, long now) {
        int[] pix = SkinLibrary.pixels(id);
        double a = (now % 5000L) / 5000.0 * Math.PI * 2;
        double c = Math.cos(a);
        double absC = Math.max(0.12, Math.abs(c));
        double dark = c < 0 ? 0.65 : 1.0;
        int cx = x + w / 2;
        for (int ry = 0; ry < h - 2; ry += 2) {
            double f = ry / (double) h;
            int inner = 5;
            int outer = (int) (6 + 54 * Math.min(1.0, f * 3.5) * (1 - 0.55 * f));
            if (outer <= inner) continue;
            for (int rx = inner; rx < outer; rx += 2) {
                double uu = (rx - inner) / (double) (outer - inner);
                int tx = Math.min(SkinGenerator.W - 1, (int) (2 + uu * 58));
                int ty = Math.min(SkinGenerator.H - 1, (int) (1 + f * 29));
                int col = shade(pix[ty * SkinGenerator.W + tx], dark);
                int sx = (int) Math.round(rx * absC);
                int pw = Math.max(1, (int) Math.ceil(2 * absC));
                ctx.fill(cx + sx, y + ry, cx + sx + pw, y + ry + 2, col);
                ctx.fill(cx - sx - pw, y + ry, cx - sx, y + ry + 2, col);
            }
        }
        ctx.fill(cx - 2, y, cx + 2, y + h - 14, 0xFF222233);
    }

    // ------------------------------------------------------------ rendering

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        hits.clear();
        if (dirty) {
            rebuild();
            dirty = false;
        }
        SkinConfig cfg = ElytraSkinManager.cfg;
        long now = System.currentTimeMillis();
        double a = Math.min(1.0, (now - openedAt) / 280.0);
        double e = 1 - Math.pow(1 - a, 3);
        int yOff = (int) ((1 - e) * 14);

        ctx.fill(0, 0, width, height, ((int) (0xA0 * e)) << 24);

        int px = (width - PW) / 2;
        int py = (height - PH) / 2 + yOff;
        int neon = SkinGenerator.hsv((now % 6000L) / 6000.0, 0.8, 1.0);
        ctx.fill(px - 2, py - 2, px + PW + 2, py + PH + 2, neon);
        ctx.fill(px, py, px + PW, py + PH, BG);
        ctx.fill(px + 2, py + 2, px + PW - 2, py + PH - 2, PANEL);

        drawCentered(ctx, "ELYTRA SKINS", px, py + 7, 90, CYAN);
        String counter = "Collection: " + SkinLibrary.COUNT + "   Favorites: " + cfg.favorites.size()
                + "   Applied: " + (cfg.selected >= 0 ? SkinLibrary.name(cfg.selected) : "Default");
        ctx.drawText(textRenderer, counter, px + PW - 8 - textRenderer.getWidth(counter), py + 7, GRAY, true);

        // search bar
        int sx = px + 98, sy = py + 20, sw = 202, sh = 16;
        ctx.fill(sx, sy, sx + sw, sy + sh, CYAN);
        ctx.fill(sx + 1, sy + 1, sx + sw - 1, sy + sh - 1, CARD);
        boolean blink = (now / 500) % 2 == 0;
        if (query.isEmpty()) {
            ctx.drawText(textRenderer, "Search skins... (just type)", sx + 5, sy + 4, 0xFF5A5A75, true);
        } else {
            ctx.drawText(textRenderer, query + (blink ? "_" : ""), sx + 5, sy + 4, WHITE, true);
        }

        // categories
        int cx0 = px + 8, cy0 = py + 20;
        List<String> labels = new ArrayList<>();
        labels.add("All Skins (" + SkinLibrary.COUNT + ")");
        labels.add("Favorites (" + cfg.favorites.size() + ")");
        labels.add("Recent (" + cfg.recent.size() + ")");
        for (String c : SkinLibrary.CATEGORIES) labels.add(c + " (" + SkinLibrary.PER + ")");
        for (int i = 0; i < labels.size(); i++) {
            final int idx = i;
            int ry = cy0 + i * 11;
            boolean sel = filter == i;
            boolean hover = mx >= cx0 && mx < cx0 + 84 && my >= ry && my < ry + 11;
            ctx.fill(cx0, ry, cx0 + 84, ry + 10, sel ? CARD_SEL : (hover ? 0xFF1F1F36 : PANEL));
            if (sel) ctx.fill(cx0, ry, cx0 + 2, ry + 10, MAGENTA);
            ctx.drawText(textRenderer, labels.get(i), cx0 + 5, ry + 1, sel ? WHITE : GRAY, false);
            addHit(cx0, ry, cx0 + 84, ry + 11, () -> {
                filter = idx;
                dirty = true;
            });
        }

        // list
        int vx = px + 98, vy = py + 40, vw = 202, vh = 184, rh = 23;
        maxScroll = Math.max(0, list.size() * rh - vh);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        ctx.fill(vx - 1, vy - 1, vx + vw + 1, vy + vh + 1, 0xFF2A2A44);
        ctx.fill(vx, vy, vx + vw, vy + vh, BG);
        ctx.enableScissor(vx, vy, vx + vw, vy + vh);
        if (list.isEmpty()) {
            drawCentered(ctx, filter == 1 ? "No favorites yet." : (filter == 2 ? "Nothing used yet." : "No skins found."), vx, vy + 8, vw, GRAY);
        }
        for (int i = 0; i < list.size(); i++) {
            int id = list.get(i);
            int ry = vy + i * rh - scroll;
            if (ry + rh < vy || ry > vy + vh) continue;
            boolean sel = id == preview;
            boolean hover = mx >= vx && mx < vx + vw && my >= Math.max(ry, vy) && my < Math.min(ry + rh, vy + vh);
            ctx.fill(vx + 1, ry + 1, vx + vw - 1, ry + rh - 1, sel ? CARD_SEL : (hover ? 0xFF222240 : CARD));
            if (sel) ctx.fill(vx + 1, ry + 1, vx + 3, ry + rh - 1, CYAN);
            drawSwatch(ctx, id, vx + 7, ry + 3);
            ctx.drawText(textRenderer, SkinLibrary.name(id), vx + 48, ry + 3, WHITE, true);
            ctx.drawText(textRenderer, SkinLibrary.categoryName(id), vx + 48, ry + 13, GRAY, false);
            if (cfg.favorites.contains(id)) ctx.drawText(textRenderer, "\u2665", vx + vw - 22, ry + 7, MAGENTA, true);
            if (cfg.selected == id) ctx.drawText(textRenderer, "\u2713", vx + vw - 12, ry + 7, GREEN, true);
            int hy1 = Math.max(ry, vy), hy2 = Math.min(ry + rh, vy + vh);
            if (hy1 < hy2) {
                addHit(vx, hy1, vx + vw, hy2, () -> {
                    preview = id;
                    setStatus("Previewing " + SkinLibrary.name(id), GRAY);
                });
            }
        }
        ctx.disableScissor();
        if (maxScroll > 0) {
            int total = list.size() * rh;
            int barH = Math.max(12, vh * vh / total);
            int barY = vy + (int) ((long) (vh - barH) * scroll / maxScroll);
            ctx.fill(vx + vw - 3, barY, vx + vw - 1, barY + barH, MAGENTA);
        }
        ctx.drawText(textRenderer, list.size() + " skin" + (list.size() == 1 ? "" : "s") + " shown", vx, vy + vh + 6, GRAY, false);

        // preview panel
        int rx = px + 306, rw = 126;
        ctx.fill(rx - 1, py + 20, rx + rw + 1, py + 106, MAGENTA);
        ctx.fill(rx, py + 21, rx + rw, py + 105, 0xFF0A0A12);
        drawPreview(ctx, rx, py + 23, rw, 80, preview, now);
        drawCentered(ctx, SkinLibrary.name(preview), rx, py + 110, rw, WHITE);
        drawCentered(ctx, "Category: " + SkinLibrary.categoryName(preview), rx, py + 121, rw, GRAY);
        if (cfg.selected == preview) drawCentered(ctx, "Currently applied", rx, py + 131, rw, GREEN);

        boolean fav = cfg.favorites.contains(preview);
        button(ctx, rx, py + 144, rw, 16, "APPLY SKIN", 0xFF1B5E3A, GREEN, mx, my, () -> {
            ElytraSkinManager.apply(preview);
            dirty = filter == 2;
            setStatus("Applied: " + SkinLibrary.name(preview), GREEN);
        });
        button(ctx, rx, py + 162, rw, 16, "REMOVE SKIN", 0xFF5E1B1B, 0xFFFF5555, mx, my, () -> {
            ElytraSkinManager.remove();
            setStatus("Default Elytra restored.", YELLOW);
        });
        button(ctx, rx, py + 180, rw, 16, fav ? "\u2665 UNFAVORITE" : "\u2665 FAVORITE", 0xFF4A1B4A, MAGENTA, mx, my, () -> {
            Integer boxed = preview;
            if (cfg.favorites.contains(boxed)) cfg.favorites.remove(boxed);
            else cfg.favorites.add(boxed);
            cfg.save();
            dirty = filter == 1;
        });
        button(ctx, rx, py + 198, rw, 16, "RANDOM SKIN", 0xFF1B3A5E, CYAN, mx, my, () -> {
            int id = ElytraSkinManager.randomSkin();
            preview = id;
            ElytraSkinManager.apply(id);
            dirty = filter == 2;
            setStatus("Random: " + SkinLibrary.name(id), CYAN);
        });
        button(ctx, rx, py + 216, rw, 16, "FAV RANDOMIZER: " + (cfg.randomizer ? "ON" : "OFF"),
                cfg.randomizer ? 0xFF1B5E3A : 0xFF2A2A3A, cfg.randomizer ? GREEN : GRAY, mx, my, () -> {
                    cfg.randomizer = !cfg.randomizer;
                    cfg.save();
                    setStatus(cfg.randomizer ? "Random favorite on every Elytra equip." : "Randomizer off.", GRAY);
                });

        ctx.drawText(textRenderer, status, px + 8, py + PH - 14, statusColor, true);
    }

    // ------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (click.x() >= h.x1() && click.x() < h.x2() && click.y() >= h.y1() && click.y() < h.y2()) {
                    h.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) (verticalAmount * 20);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int code = input.getKeycode();
        if (code == GLFW.GLFW_KEY_BACKSPACE) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                dirty = true;
            }
            return true;
        }
        if (code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z) {
            query += (char) ('a' + (code - GLFW.GLFW_KEY_A));
            dirty = true;
            return true;
        }
        if (code >= GLFW.GLFW_KEY_0 && code <= GLFW.GLFW_KEY_9) {
            query += (char) ('0' + (code - GLFW.GLFW_KEY_0));
            dirty = true;
            return true;
        }
        if (code == GLFW.GLFW_KEY_SPACE && !query.isEmpty()) {
            query += ' ';
            dirty = true;
            return true;
        }
        return super.keyPressed(input);
    }
}
