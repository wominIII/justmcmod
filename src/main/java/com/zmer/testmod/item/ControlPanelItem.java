package com.zmer.testmod.item;

import com.zmer.testmod.NetworkHandler;
import com.zmer.testmod.network.ControlPanelPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 操控面板物品 —— 右键使用时发送请求到服务端，
 * 服务端扫描可用目标后把列表发给客户端打开 GUI。
 */
public class ControlPanelItem extends Item {

    public ControlPanelItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // 直接在服务端处理：扫描目标列表 → 发 S2C 包打开 GUI
            var targets = com.zmer.testmod.control.TargetManager.findAvailableTargets(serverPlayer.getUUID());
            var bound = com.zmer.testmod.control.TargetManager.getBoundTarget(serverPlayer.getUUID());

            NetworkHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new ControlPanelPackets.S2COpenPanel(targets, bound)
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("item.zmer_test_mod.control_panel.tooltip")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
