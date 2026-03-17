package com.zmer.testmod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Decorative Goggles — 纯装饰眼镜，无任何效果
 * 位置比线框眼镜高2像素（用于解决模组冲突）
 */
public class DecorativeGoggles extends Item implements ICurioItem {
    public DecorativeGoggles(Properties props) {
        super(props);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "head".equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}