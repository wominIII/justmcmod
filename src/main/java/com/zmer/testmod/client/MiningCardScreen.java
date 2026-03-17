package com.zmer.testmod.client;

import com.zmer.testmod.NetworkHandler;
import com.zmer.testmod.network.InsertMiningCardPacket;
import com.zmer.testmod.network.RemoveMiningCardPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/**
 * Simple GUI for the master to confirm inserting/removing a mining card
 * into/from a target player's mechanical gloves.
 */
public class MiningCardScreen extends Screen {

    private final UUID targetPlayerUUID;
    private final boolean removeMode;  // true = remove card, false = insert card

    public MiningCardScreen(UUID targetPlayerUUID, boolean removeMode) {
        super(Component.translatable(removeMode ? 
            "screen.zmer_test_mod.mining_card.remove_title" : 
            "screen.zmer_test_mod.mining_card.title"));
        this.targetPlayerUUID = targetPlayerUUID;
        this.removeMode = removeMode;
    }

    public static void openFor(UUID targetPlayerUUID, boolean removeMode) {
        Minecraft.getInstance().tell(() -> {
            Minecraft.getInstance().setScreen(new MiningCardScreen(targetPlayerUUID, removeMode));
        });
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (removeMode) {
            // "Remove Mining Card" button
            this.addRenderableWidget(Button.builder(
                Component.translatable("screen.zmer_test_mod.mining_card.remove"),
                btn -> {
                    // Send packet to server to remove the card
                    NetworkHandler.CHANNEL.sendToServer(new RemoveMiningCardPacket(targetPlayerUUID));
                    this.onClose();
                }
            ).bounds(centerX - 80, centerY - 10, 160, 20).build());
        } else {
            // "Insert Mining Card" button
            this.addRenderableWidget(Button.builder(
                Component.translatable("screen.zmer_test_mod.mining_card.insert"),
                btn -> {
                    // Send packet to server to insert the card
                    NetworkHandler.CHANNEL.sendToServer(new InsertMiningCardPacket(targetPlayerUUID));
                    this.onClose();
                }
            ).bounds(centerX - 80, centerY - 10, 160, 20).build());
        }

        // "Cancel" button
        this.addRenderableWidget(Button.builder(
            Component.translatable("screen.zmer_test_mod.mining_card.cancel"),
            btn -> this.onClose()
        ).bounds(centerX - 80, centerY + 20, 160, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // Title
        graphics.drawCenteredString(this.font,
            Component.translatable(removeMode ? 
                "screen.zmer_test_mod.mining_card.remove_title" : 
                "screen.zmer_test_mod.mining_card.title"),
            this.width / 2, this.height / 2 - 40, removeMode ? 0xFFFF5555 : 0xFF55FF55);

        // Description
        graphics.drawCenteredString(this.font,
            Component.translatable(removeMode ? 
                "screen.zmer_test_mod.mining_card.remove_desc" : 
                "screen.zmer_test_mod.mining_card.desc"),
            this.width / 2, this.height / 2 - 25, 0xFFAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}