package com.zmer.testmod.client;

import com.zmer.testmod.ExampleMod;
import com.zmer.testmod.energy.ExoskeletonEnergyClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Handles all visual effects when the Tech Collar is equipped:
 * - Hides the vanilla crosshair
 * - Replaces the vanilla hotbar with a high-tech version
 * - Replaces vanilla health / food bars with tech-styled ones
 * - Keeps first-person arms visible
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public class CollarEffectsHandler {

    // ── Colors ───────────────────────────────────────────────
    private static final int COL_BG           = 0xAA060610;
    private static final int COL_BORDER       = 0xCC00DDDD;
    private static final int COL_BORDER_DIM   = 0x66008888;
    private static final int COL_SELECT       = 0xCC00FFFF;
    private static final int COL_SELECT_GLOW  = 0x3300FFFF;
    private static final int COL_SLOT_BG      = 0x88080818;
    private static final int COL_CYAN         = 0xDD00FFFF;
    private static final int COL_CYAN_DIM     = 0x6600CCCC;
    private static final int COL_CYAN_FAINT   = 0x33009999;
    private static final int COL_MAGENTA      = 0xCCFF00FF;
    private static final int COL_MAGENTA_DIM  = 0x66CC00CC;
    private static final int COL_RED          = 0xCCFF3344;
    private static final int COL_GREEN        = 0xCC44FF44;
    private static final int COL_WHITE        = 0xAAFFFFFF;
    private static final int COL_DARK_BG      = 0x88000011;

    // ── Hotbar dimensions ────────────────────────────────────
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_GAP = 2;
    private static final int HOTBAR_PADDING = 4;

    /** Cached collar-equipped flag, refreshed each overlay frame. */
    private static boolean collarEquippedCache = false;
    private static long lastCacheTime = 0;

    // ═══════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════

    /** Whether the local player currently has a Tech Collar equipped. */
    public static boolean isCollarEquipped() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        long now = System.currentTimeMillis();
        if (now - lastCacheTime > 250) {
            lastCacheTime = now;
            collarEquippedCache = CuriosApi.getCuriosInventory(player).resolve()
                    .flatMap(inv -> inv.findFirstCurio(ExampleMod.TECH_COLLAR.get()))
                    .isPresent();
        }
        return collarEquippedCache;
    }

    // ═══════════════════════════════════════════════════════════
    // EVENT: Keep first-person arms visible
    // ═══════════════════════════════════════════════════════════

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        // Intentionally not canceling hand rendering:
        // users want to keep first-person arms visible while wearing the suit/collar.
    }

    // ═══════════════════════════════════════════════════════════
    // EVENT: Cancel vanilla overlays we replace
    // ═══════════════════════════════════════════════════════════

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPreOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!isCollarEquipped()) return;

        var overlay = event.getOverlay();

        // Cancel crosshair (replaced by CollarHudOverlay reticle)
        if (overlay == VanillaGuiOverlay.CROSSHAIR.type()) {
            event.setCanceled(true);
            return;
        }

        // Cancel hotbar — render tech version in its place
        if (overlay == VanillaGuiOverlay.HOTBAR.type()) {
            event.setCanceled(true);
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player != null) {
                GuiGraphics g = event.getGuiGraphics();
                int w = mc.getWindow().getGuiScaledWidth();
                int h = mc.getWindow().getGuiScaledHeight();
                renderTechHotbar(g, w, h, player);
            }
            return;
        }

        // Cancel exp bar and item name for cleaner look
        if (overlay == VanillaGuiOverlay.EXPERIENCE_BAR.type()
         || overlay == VanillaGuiOverlay.ITEM_NAME.type()) {
            event.setCanceled(true);
        }

        // Hide vanilla health bar (replaced by tech HUD side indicators)
        if (overlay == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            event.setCanceled(true);
        }

        // Hide vanilla armor bar (replaced by tech HUD)
        if (overlay == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
            event.setCanceled(true);
        }

        // Hide vanilla food bar when wearing exoskeleton (energy replaces it)
        if (overlay == VanillaGuiOverlay.FOOD_LEVEL.type()
            && ExoskeletonEnergyClientHandler.isLocalPlayerWearingExo()) {
            event.setCanceled(true);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TECH HOTBAR RENDERING
    // ═══════════════════════════════════════════════════════════

    private static void renderTechHotbar(GuiGraphics g, int screenW, int screenH, Player player) {
        Inventory inv = player.getInventory();
        int selected = inv.selected;
        long tick = System.currentTimeMillis();

        int totalSlots = 9;
        int slotTotal = SLOT_SIZE + SLOT_GAP;
        int hotbarW = totalSlots * slotTotal - SLOT_GAP + HOTBAR_PADDING * 2;
        int hotbarH = SLOT_SIZE + HOTBAR_PADDING * 2;
        int x0 = (screenW - hotbarW) / 2;
        int y0 = screenH - hotbarH - 4;

        // ── Outer glow / selection highlight behind ──────────
        if (selected >= 0 && selected < 9) {
            int selX = x0 + HOTBAR_PADDING + selected * slotTotal - 2;
            int selY = y0 + HOTBAR_PADDING - 2;
            g.fill(selX - 1, selY - 1, selX + SLOT_SIZE + 3, selY + SLOT_SIZE + 3, COL_SELECT_GLOW);
        }

        // ── Background panel ─────────────────────────────────
        g.fill(x0, y0, x0 + hotbarW, y0 + hotbarH, COL_BG);

        // Top border with animated scanner
        g.fill(x0, y0, x0 + hotbarW, y0 + 1, COL_BORDER_DIM);
        int scanPos = (int)((tick / 8) % hotbarW);
        int scanLen = 30;
        int sStart = x0 + Math.max(0, scanPos - scanLen);
        int sEnd = x0 + Math.min(hotbarW, scanPos);
        if (sEnd > sStart) {
            g.fill(sStart, y0, sEnd, y0 + 1, COL_BORDER);
        }

        // Bottom border
        g.fill(x0, y0 + hotbarH - 1, x0 + hotbarW, y0 + hotbarH, COL_BORDER_DIM);
        // Side borders
        g.fill(x0, y0, x0 + 1, y0 + hotbarH, COL_BORDER_DIM);
        g.fill(x0 + hotbarW - 1, y0, x0 + hotbarW, y0 + hotbarH, COL_BORDER_DIM);

        // ── Slot backgrounds + selection ─────────────────────
        for (int i = 0; i < totalSlots; i++) {
            int slotX = x0 + HOTBAR_PADDING + i * slotTotal;
            int slotY = y0 + HOTBAR_PADDING;

            if (i == selected) {
                g.fill(slotX - 1, slotY - 1, slotX + SLOT_SIZE + 1, slotY + SLOT_SIZE + 1, COL_SELECT);
                g.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, COL_SLOT_BG);
            } else {
                g.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, COL_SLOT_BG);
                g.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + 1, COL_BORDER_DIM);
                g.fill(slotX, slotY + SLOT_SIZE - 1, slotX + SLOT_SIZE, slotY + SLOT_SIZE, COL_BORDER_DIM);
                g.fill(slotX, slotY, slotX + 1, slotY + SLOT_SIZE, COL_BORDER_DIM);
                g.fill(slotX + SLOT_SIZE - 1, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, COL_BORDER_DIM);
            }

            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                int itemX = slotX + (SLOT_SIZE - 16) / 2;
                int itemY = slotY + (SLOT_SIZE - 16) / 2;
                g.renderItem(stack, itemX, itemY);
                g.renderItemDecorations(Minecraft.getInstance().font, stack, itemX, itemY);
            }
        }

        // ── Left/right decorative brackets ───────────────────
        g.fill(x0 - 6, y0 + 2, x0 - 4, y0 + hotbarH - 2, COL_MAGENTA);
        g.fill(x0 - 6, y0 + 2, x0 - 2, y0 + 4, COL_MAGENTA);
        g.fill(x0 - 6, y0 + hotbarH - 4, x0 - 2, y0 + hotbarH - 2, COL_MAGENTA);

        g.fill(x0 + hotbarW + 4, y0 + 2, x0 + hotbarW + 6, y0 + hotbarH - 2, COL_MAGENTA);
        g.fill(x0 + hotbarW + 2, y0 + 2, x0 + hotbarW + 6, y0 + 4, COL_MAGENTA);
        g.fill(x0 + hotbarW + 2, y0 + hotbarH - 4, x0 + hotbarW + 6, y0 + hotbarH - 2, COL_MAGENTA);

        // ── Offhand slot on far left ─────────────────────────
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            int ohX = x0 - SLOT_SIZE - 12;
            int ohY = y0 + HOTBAR_PADDING;
            g.fill(ohX - 1, ohY - 1, ohX + SLOT_SIZE + 1, ohY + SLOT_SIZE + 1, COL_BORDER_DIM);
            g.fill(ohX, ohY, ohX + SLOT_SIZE, ohY + SLOT_SIZE, COL_SLOT_BG);
            g.renderItem(offhand, ohX + 2, ohY + 2);
            g.renderItemDecorations(Minecraft.getInstance().font, offhand, ohX + 2, ohY + 2);
        }
    }
}
