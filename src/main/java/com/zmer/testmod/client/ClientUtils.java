package com.zmer.testmod.client;

import net.minecraft.client.Minecraft;
import java.util.UUID;

public class ClientUtils {
    public static void openCollarAuthGui(UUID targetPlayerId, UUID collarOwnerId, String collarOwnerName) {
        Minecraft.getInstance().setScreen(new CollarAuthScreen(targetPlayerId, collarOwnerId, collarOwnerName));
    }

    public static void openExoScreen(net.minecraft.core.BlockPos pos, boolean autoTrap) {
        Minecraft.getInstance().setScreen(new ExoAssimilatorScreen(pos, autoTrap));
    }
}
