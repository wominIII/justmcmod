package com.zmer.testmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;

import java.util.UUID;
import com.zmer.testmod.item.KeycardItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.zmer.testmod.NetworkHandler;
import com.zmer.testmod.network.AuthCollarClientPacket;
import com.zmer.testmod.network.ReleaseCollarPacket;

public class CollarAuthScreen extends Screen {
    private final UUID targetPlayerId;
    private final UUID collarOwnerId;
    private final String collarOwnerName;
    private int imageWidth = 176;
    private int imageHeight = 120;
    private int leftPos;
    private int topPos;
    private boolean isSwiping = false;
    private boolean authSuccess = false;
    private boolean isReleasing = false;
    
    public CollarAuthScreen(UUID targetPlayerId, UUID collarOwnerId, String collarOwnerName) {
        super(Component.translatable("gui.zmer_test_mod.collar_auth"));
        this.targetPlayerId = targetPlayerId;
        this.collarOwnerId = collarOwnerId;
        this.collarOwnerName = collarOwnerName;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        // Auth button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.zmer_test_mod.auth_button"), button -> {
            attemptAuth();
        }).bounds(this.leftPos + 40, this.topPos + 60, 96, 20).build());

        // Release button - only visible if current player is the owner
        UUID currentPlayerId = Minecraft.getInstance().player != null ? 
            Minecraft.getInstance().player.getUUID() : null;
        if (collarOwnerId != null && collarOwnerId.equals(currentPlayerId)) {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.zmer_test_mod.release_button"), button -> {
                releaseCollar();
            }).bounds(this.leftPos + 40, this.topPos + 85, 96, 20).build());
        }
    }
    
    private void attemptAuth() {
        if(Minecraft.getInstance().player == null) return;
        
        // Check if player is holding a keycard
        boolean hasKeycard = false;
        ItemStack mainHandItem = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHandItem = Minecraft.getInstance().player.getItemInHand(InteractionHand.OFF_HAND);
        
        if (mainHandItem.getItem() instanceof KeycardItem || offHandItem.getItem() instanceof KeycardItem) {
            hasKeycard = true;
        }

        if (hasKeycard) {
            this.isSwiping = true;
            this.authSuccess = true;
            // Send auth packet to server
            NetworkHandler.CHANNEL.sendToServer(new AuthCollarClientPacket(targetPlayerId, true));
            // Schedule close
            new Thread(() -> {
                try {
                    Thread.sleep(800);
                } catch(InterruptedException e) {}
                Minecraft.getInstance().execute(() -> this.onClose());
            }).start();
        }
    }

    private void releaseCollar() {
        if(Minecraft.getInstance().player == null) return;
        
        this.isReleasing = true;
        this.authSuccess = true;
        // Send release packet to server
        NetworkHandler.CHANNEL.sendToServer(new ReleaseCollarPacket());
        // Schedule close
        new Thread(() -> {
            try {
                Thread.sleep(800);
            } catch(InterruptedException e) {}
            Minecraft.getInstance().execute(() -> this.onClose());
        }).start();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render semi-transparent background
        this.renderBackground(guiGraphics);
        
        // Draw background panel
        int bgColor = 0xCC000000;
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, bgColor);
        
        // Draw border
        int borderColor = 0xFF555555;
        guiGraphics.renderOutline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, borderColor);
        
        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.topPos + 10, 0xFFFFFF);
        
        // Owner status
        if (collarOwnerId != null) {
            guiGraphics.drawCenteredString(this.font, 
                Component.translatable("gui.zmer_test_mod.collar_owned", collarOwnerName != null ? collarOwnerName : "Unknown"), 
                this.width / 2, this.topPos + 28, 0xFFAA00);
        } else {
            guiGraphics.drawCenteredString(this.font, 
                Component.translatable("gui.zmer_test_mod.collar_unowned"), 
                this.width / 2, this.topPos + 28, 0xAAAAAA);
        }
        
        // Instructions
        if (!isSwiping && !isReleasing) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.zmer_test_mod.auth_instruction"), this.width / 2, this.topPos + 45, 0xAAAAAA);
        } else if (authSuccess) {
            if (isReleasing) {
                guiGraphics.drawCenteredString(this.font, Component.translatable("gui.zmer_test_mod.release_success"), this.width / 2, this.topPos + 45, 0x00FF00);
            } else {
                guiGraphics.drawCenteredString(this.font, Component.translatable("gui.zmer_test_mod.auth_success"), this.width / 2, this.topPos + 45, 0x00FF00);
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}