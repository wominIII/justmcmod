package com.zmer.testmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import com.zmer.testmod.network.SyncValuePacket;
import com.zmer.testmod.network.OpenCollarAuthGuiPacket;
import com.zmer.testmod.network.AuthCollarClientPacket;
import com.zmer.testmod.network.ReleaseCollarPacket;
import com.zmer.testmod.network.OpenMiningCardGuiPacket;
import com.zmer.testmod.network.InsertMiningCardPacket;
import com.zmer.testmod.network.RemoveMiningCardPacket;
import com.zmer.testmod.network.ControlPanelPackets;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ExampleMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static void register() {
        CHANNEL.registerMessage(id(), SyncValuePacket.class,
            SyncValuePacket::encode,
            SyncValuePacket::new,
            SyncValuePacket::handle
        );
        CHANNEL.registerMessage(id(), OpenCollarAuthGuiPacket.class,
            OpenCollarAuthGuiPacket::toBytes,
            OpenCollarAuthGuiPacket::new,
            OpenCollarAuthGuiPacket::handle
        );
        CHANNEL.registerMessage(id(), AuthCollarClientPacket.class,
            AuthCollarClientPacket::toBytes,
            AuthCollarClientPacket::new,
            AuthCollarClientPacket::handle
        );
        CHANNEL.registerMessage(id(), ReleaseCollarPacket.class,
            ReleaseCollarPacket::encode,
            ReleaseCollarPacket::decode,
            ReleaseCollarPacket::handle
        );
        CHANNEL.registerMessage(id(), OpenMiningCardGuiPacket.class,
            OpenMiningCardGuiPacket::toBytes,
            OpenMiningCardGuiPacket::new,
            OpenMiningCardGuiPacket::handle
        );
        CHANNEL.registerMessage(id(), InsertMiningCardPacket.class,
            InsertMiningCardPacket::toBytes,
            InsertMiningCardPacket::new,
            InsertMiningCardPacket::handle
        );
        CHANNEL.registerMessage(id(), RemoveMiningCardPacket.class,
            RemoveMiningCardPacket::encode,
            RemoveMiningCardPacket::decode,
            RemoveMiningCardPacket::handle
        );
        CHANNEL.registerMessage(id(), com.zmer.testmod.network.UnlockCurioPacket.class,
            com.zmer.testmod.network.UnlockCurioPacket::encode,
            com.zmer.testmod.network.UnlockCurioPacket::decode,
            com.zmer.testmod.network.UnlockCurioPacket::handle
        );
        CHANNEL.registerMessage(id(), com.zmer.testmod.network.ExoAssimilatorActionPacket.class,
            com.zmer.testmod.network.ExoAssimilatorActionPacket::toBytes,
            com.zmer.testmod.network.ExoAssimilatorActionPacket::new,
            com.zmer.testmod.network.ExoAssimilatorActionPacket::handle
        );
        CHANNEL.registerMessage(id(), com.zmer.testmod.network.OpenExoAssimilatorGuiPacket.class,
            com.zmer.testmod.network.OpenExoAssimilatorGuiPacket::toBytes,
            com.zmer.testmod.network.OpenExoAssimilatorGuiPacket::new,
            com.zmer.testmod.network.OpenExoAssimilatorGuiPacket::handle
        );
        
        // Control Panel Packets
        CHANNEL.registerMessage(id(), ControlPanelPackets.C2SOpenPanel.class,
            ControlPanelPackets.C2SOpenPanel::encode,
            ControlPanelPackets.C2SOpenPanel::new,
            ControlPanelPackets.C2SOpenPanel::handle
        );
        CHANNEL.registerMessage(id(), ControlPanelPackets.S2COpenPanel.class,
            ControlPanelPackets.S2COpenPanel::encode,
            ControlPanelPackets.S2COpenPanel::new,
            ControlPanelPackets.S2COpenPanel::handle
        );
        CHANNEL.registerMessage(id(), ControlPanelPackets.C2SBindTarget.class,
            ControlPanelPackets.C2SBindTarget::encode,
            ControlPanelPackets.C2SBindTarget::new,
            ControlPanelPackets.C2SBindTarget::handle
        );
        CHANNEL.registerMessage(id(), ControlPanelPackets.C2SUnbindTarget.class,
            ControlPanelPackets.C2SUnbindTarget::encode,
            ControlPanelPackets.C2SUnbindTarget::new,
            ControlPanelPackets.C2SUnbindTarget::handle
        );
        CHANNEL.registerMessage(id(), ControlPanelPackets.C2SSendTask.class,
            ControlPanelPackets.C2SSendTask::encode,
            ControlPanelPackets.C2SSendTask::new,
            ControlPanelPackets.C2SSendTask::handle
        );
        CHANNEL.registerMessage(id(), ControlPanelPackets.C2SRestraintControl.class,
            ControlPanelPackets.C2SRestraintControl::encode,
            ControlPanelPackets.C2SRestraintControl::new,
            ControlPanelPackets.C2SRestraintControl::handle
        );
        CHANNEL.registerMessage(id(), ControlPanelPackets.C2SCollarControl.class,
            ControlPanelPackets.C2SCollarControl::encode,
            ControlPanelPackets.C2SCollarControl::new,
            ControlPanelPackets.C2SCollarControl::handle
        );
        CHANNEL.registerMessage(id(), ControlPanelPackets.C2SRequestStatus.class,
            ControlPanelPackets.C2SRequestStatus::encode,
            ControlPanelPackets.C2SRequestStatus::new,
            ControlPanelPackets.C2SRequestStatus::handle
        );
        CHANNEL.registerMessage(id(), ControlPanelPackets.S2CTargetStatus.class,
            ControlPanelPackets.S2CTargetStatus::encode,
            ControlPanelPackets.S2CTargetStatus::new,
            ControlPanelPackets.S2CTargetStatus::handle
        );
    }
}
