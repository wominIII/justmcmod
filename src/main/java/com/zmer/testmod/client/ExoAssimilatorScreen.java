package com.zmer.testmod.client;

import com.zmer.testmod.NetworkHandler;
import com.zmer.testmod.network.ExoAssimilatorActionPacket;
import com.zmer.testmod.network.ExoAssimilatorActionPacket;
import com.zmer.testmod.network.UnlockCurioPacket;
import com.zmer.testmod.ExampleMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ExoAssimilatorScreen extends Screen {
    private final BlockPos pos;
    private boolean autoTrap;
    private final ResourceLocation TEXTURE = new ResourceLocation(ExampleMod.MODID, "textures/gui/exo_assimilator_gui.png");

    public ExoAssimilatorScreen(BlockPos pos, boolean autoTrap) {
        super(Component.translatable("gui.zmer_test_mod.exo_assimilator.title"));
        this.pos = pos;
        this.autoTrap = autoTrap;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - 200) / 2;
        int y = (this.height - 150) / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.zmer_test_mod.assimilator.trap"), btn -> {
            NetworkHandler.CHANNEL.sendToServer(new ExoAssimilatorActionPacket(pos, 0));
        }).bounds(x + 20, y + 30, 160, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.zmer_test_mod.assimilator.start"), btn -> {
            NetworkHandler.CHANNEL.sendToServer(new ExoAssimilatorActionPacket(pos, 1));
        }).bounds(x + 20, y + 60, 160, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.zmer_test_mod.assimilator.release"), btn -> {
            NetworkHandler.CHANNEL.sendToServer(new ExoAssimilatorActionPacket(pos, 2));
        }).bounds(x + 20, y + 90, 160, 20).build());

        Component autoTrapText = Component.literal(autoTrap ? "自动抓捕: 开" : "自动抓捕: 关");
        this.addRenderableWidget(Button.builder(autoTrapText, b -> {
            this.autoTrap = !this.autoTrap;
            b.setMessage(Component.literal(this.autoTrap ? "自动抓捕: 开" : "自动抓捕: 关"));
            NetworkHandler.CHANNEL.sendToServer(new ExoAssimilatorActionPacket(pos, 3));
        }).bounds(x + 20, y + 120, 160, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        int x = (this.width - 200) / 2;
        int y = (this.height - 150) / 2;
        
        // Draw a simple dark tech background
        guiGraphics.fill(x, y, x + 200, y + 150, 0xDD111111);
        guiGraphics.renderOutline(x, y, 200, 150, 0xFF00FFFF);

        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.zmer_test_mod.assimilator.title"), this.width / 2, y + 10, 0x00FFFF);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
