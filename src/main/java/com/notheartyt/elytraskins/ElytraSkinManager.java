package com.notheartyt.elytraskins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Client-only. Swaps the texture registered under the vanilla Elytra texture id with the chosen skin.
 * Removing a skin destroys that texture so vanilla reloads the original on demand.
 */
public final class ElytraSkinManager {
    public static final Identifier VANILLA = Identifier.of("minecraft", "textures/entity/equipment/wings/elytra.png");

    public static SkinConfig cfg = new SkinConfig();
    private static NativeImageBackedTexture tex;
    private static boolean hadElytra;
    private static final Random RNG = new Random();

    private ElytraSkinManager() {}

    public static void init() {
        cfg = SkinConfig.load();
    }

    public static void apply(int id) {
        if (id < 0 || id >= SkinLibrary.COUNT) return;
        cfg.selected = id;
        cfg.recent.remove(Integer.valueOf(id));
        cfg.recent.add(0, id);
        while (cfg.recent.size() > 12) cfg.recent.remove(cfg.recent.size() - 1);
        cfg.save();
        install(id);
    }

    public static void remove() {
        cfg.selected = -1;
        cfg.save();
        uninstall();
    }

    public static int randomSkin() {
        return RNG.nextInt(SkinLibrary.COUNT);
    }

    private static NativeImageBackedTexture makeTexture(NativeImage img) throws Exception {
        for (Constructor<?> c : NativeImageBackedTexture.class.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 2 && p[1] == NativeImage.class) {
                Object name = p[0] == String.class ? (Object) "elytraskins" : (Object) (Supplier<String>) () -> "elytraskins";
                return (NativeImageBackedTexture) c.newInstance(name, img);
            }
        }
        throw new IllegalStateException("No compatible NativeImageBackedTexture constructor");
    }

    private static void install(int id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            int[] pix = SkinLibrary.pixels(id);
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, SkinGenerator.W, SkinGenerator.H, false);
            for (int y = 0; y < SkinGenerator.H; y++) {
                for (int x = 0; x < SkinGenerator.W; x++) {
                    img.setColorArgb(x, y, pix[y * SkinGenerator.W + x]);
                }
            }
            NativeImageBackedTexture t = makeTexture(img);
            mc.getTextureManager().registerTexture(VANILLA, t);
            tex = t;
        } catch (Exception e) {
            tex = null;
        }
    }

    private static void uninstall() {
        tex = null;
        MinecraftClient.getInstance().getTextureManager().destroyTexture(VANILLA);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) {
            hadElytra = false;
            return;
        }
        boolean has = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        if (has && !hadElytra && cfg.randomizer && !cfg.favorites.isEmpty()) {
            int pick = cfg.favorites.get(RNG.nextInt(cfg.favorites.size()));
            apply(pick);
        }
        hadElytra = has;

        if (cfg.selected >= 0) {
            Object current = mc.getTextureManager().getTexture(VANILLA);
            if (tex == null || current != tex) install(cfg.selected);
        }
    }
}
