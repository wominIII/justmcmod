package com.zmer.testmod.network;

import com.zmer.testmod.NetworkHandler;
import com.zmer.testmod.control.BrainwashManager;
import com.zmer.testmod.control.RestraintManager;
import com.zmer.testmod.control.TargetManager;
import com.zmer.testmod.control.TaskData;
import com.zmer.testmod.item.AnkleShacklesItem;
import com.zmer.testmod.item.ElectronicShacklesItem;
import com.zmer.testmod.item.TechCollar;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 闂備胶鎳撻悘婵堢矓閻㈠灚鏆滈柛顐ｆ礃椤ュ牓鏌曡箛濠傚⒉缂佽埖鐓￠弻鐔哄枈濡桨澹曢梻浣告惈閻楀棝藝鏉堚晝绀婂璺虹灱绾惧吋绻濇繝鍌氭殭闁伙箑鐖煎濠氬幢濡や緡妫涢梺鐓庣仛閸ㄥ湱鍒掓繝姘櫖闁告洦鍓欓埀顒傚仱閺?
 * 闂備礁鎲＄粙鏍涢崟顖氱畺闁哄洨濮峰Λ顖炴煥閺囨浜鹃梺浼欑秮缁犳牠寮鍛瀻?S = 闂佽楠哥粻宥夊垂濞差亜鏄ユ繛鎴炴皑閸?闂?闂備礁鎼悧鍡欑矓鐎涙ɑ鍙忛柣鏃囨閸楁碍銇勯弽銉モ偓妤冪矆婵?C = 闂備礁鎼悧鍡欑矓鐎涙ɑ鍙忛柣鏃囨閸?闂?闂佽楠哥粻宥夊垂濞差亜鏄ユ繛鎴炴皑閸楁碍銇勯弽銊囨岸宕?
 */
public class ControlPanelPackets {

    /* 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?
     *  1. C2S 闂?闂佽崵濮村ú顓㈠绩闁秵鍎戝ù鍏兼綑缁犮儵鏌熼幆褏锛嶇痪鎯ь煼濮婂宕煎☉妯间痪濠电姰鍨洪敃銏ゅ极瀹ュ閱囬柨娑氬濞茬喎鐣烽敐澶婄闁绘劕顕悿鈧梻浣告啞濡垹妲愰弴銏╂晜妞ゆ帒瀚粻銉︺亜閹板墎鍒板ù鐓庨叄閺岋綁鍩℃繝鍌涚亶闂佺粯鐗紞浣哥暦濮橆儵鏃堝礋閳规儳浜鹃柛鎰靛枟閺?
     * 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?*/

    public static class C2SOpenPanel {
        public C2SOpenPanel() {}
        public C2SOpenPanel(FriendlyByteBuf buf) {}
        public void encode(FriendlyByteBuf buf) {}

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;

                List<UUID> targets = TargetManager.findAvailableTargets(sender.getUUID());

                // 闂佽绻愮换鎰涘Δ鍛闁跨喓濮寸€氬顭跨捄鐑樻拱闁伙綀浜槐鎾存媴閻熸壆浠撮梺浼欑悼閸嬫挾绮氶柆宥呯労闁告剬鍛槬濠电偞鍨堕幐鎾磻閹炬惌娈介柣鎰煐绾墽鎲搁弶鍨偓鍨暦?
                UUID bound = TargetManager.getBoundTarget(sender.getUUID());

                NetworkHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> sender),
                        new S2COpenPanel(targets, bound)
                );
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?
     *  2. S2C 闂?闂備礁鎲￠悷锕傚垂閸ф鐒垫い鎴ｅ劵閸忓本绻涢煫顓炲祮鐎殿喕绮欏畷鎯邦槻闁诲繑鐟╅幃?+ 闂備胶鎳撻悘姘跺箰閸濄儲顫?GUI
     * 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?*/

    public static class S2COpenPanel {
        private final List<UUID> targets;
        private final UUID boundTarget; // 闂備礁鎲￠悷顖炲垂閻㈢绀傛俊顖濆吹閳?null闂備焦瀵х粙鎴︽偋韫囨稑鏋侀柕鍫濐槸缁€鍌炴煏婵炵偓娅嗙紒浣叉櫊閹泛鈽夐弽褍顬嬮梺?闂?闂?

        public S2COpenPanel(List<UUID> targets, UUID boundTarget) {
            this.targets = targets;
            this.boundTarget = boundTarget;
        }

        public S2COpenPanel(FriendlyByteBuf buf) {
            int count = buf.readVarInt();
            this.targets = new ArrayList<>(count);
            for (int i = 0; i < count; i++) targets.add(buf.readUUID());
            boolean hasBound = buf.readBoolean();
            this.boundTarget = hasBound ? buf.readUUID() : null;
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(targets.size());
            for (UUID uuid : targets) buf.writeUUID(uuid);
            buf.writeBoolean(boundTarget != null);
            if (boundTarget != null) buf.writeUUID(boundTarget);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // 闂備線娼荤拹鐔煎礉瀹€鍕垫晣鐟滅増甯掔粻锝咁渻鐎ｎ亜鐦ㄥù鐓庡€块弻鐔煎垂椤愶絽纰嶇紓?GUI
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player == null) return;
                var screen = new com.zmer.testmod.client.ControlPanelScreen();
                screen.initFromServer(targets, boundTarget);
                mc.setScreen(screen);
            });
            ctx.get().setPacketHandled(true);
        }

        public List<UUID> getTargets() { return targets; }
        public UUID getBoundTarget() { return boundTarget; }
    }

    /* 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?
     *  3. C2S 闂?缂傚倸鍊烽悞锕傚垂閻㈠憡鍋?/ 闂佽崵鍠愰悷杈╁緤鐠恒劌鍨濋柨鏇炲€归崕搴亜閺冨倹娅曢柛?
     * 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?*/

    public static class C2SBindTarget {
        private final UUID targetUUID;

        public C2SBindTarget(UUID targetUUID) { this.targetUUID = targetUUID; }
        public C2SBindTarget(FriendlyByteBuf buf) { this.targetUUID = buf.readUUID(); }
        public void encode(FriendlyByteBuf buf) { buf.writeUUID(targetUUID); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;

                if (TargetManager.canControl(sender.getUUID(), targetUUID)) {
                    TargetManager.bind(sender.getUUID(), targetUUID);
                    sender.displayClientMessage(Component.literal("\u00A7a\u76EE\u6807\u7ED1\u5B9A\u6210\u529F"), true);
                    // 闂備礁鎲￠悷锕傚垂閸ф鐒垫い鎴ｅ劵閸忓苯鈹戦鑺ュ€愭鐐╁亾婵炶揪缍€椤鐥鐐寸厸?
                    sendStatusToMaster(sender, targetUUID);
                } else {
                    sender.displayClientMessage(Component.literal("\u00A7c\u65E0\u6CD5\u7ED1\u5B9A\u76EE\u6807\uFF1A\u6CA1\u6709\u63A7\u5236\u6743\u9650"), true);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class C2SUnbindTarget {
        public C2SUnbindTarget() {}
        public C2SUnbindTarget(FriendlyByteBuf buf) {}
        public void encode(FriendlyByteBuf buf) {}

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                TargetManager.unbind(sender.getUUID());
                sender.displayClientMessage(Component.literal("\u00A7e\u5DF2\u89E3\u9664\u7ED1\u5B9A"), true);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?
     *  4. C2S 闂?濠电偞鍨堕幐鎼侇敄閸涜埇浜归柛灞剧〒椤╃兘鎮归崶銊ョ祷妞?
     * 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?*/

    public static class C2SSendTask {
        private final UUID targetUUID;
        private final int taskType;      // TaskData.TaskType ordinal
        private final int posX, posY, posZ;
        private final boolean hasPos;
        private final String itemId;
        private final int count;
        private final String customText;

        public C2SSendTask(UUID target, TaskData.TaskType type,
                           BlockPos pos, String itemId, int count, String customText) {
            this.targetUUID = target;
            this.taskType = type.ordinal();
            this.hasPos = pos != null;
            this.posX = hasPos ? pos.getX() : 0;
            this.posY = hasPos ? pos.getY() : 0;
            this.posZ = hasPos ? pos.getZ() : 0;
            this.itemId = itemId != null ? itemId : "";
            this.count = count;
            this.customText = customText != null ? customText : "";
        }

        public C2SSendTask(FriendlyByteBuf buf) {
            targetUUID = buf.readUUID();
            taskType = buf.readVarInt();
            hasPos = buf.readBoolean();
            posX = buf.readInt();
            posY = buf.readInt();
            posZ = buf.readInt();
            itemId = buf.readUtf(256);
            count = buf.readVarInt();
            customText = buf.readUtf(512);
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(targetUUID);
            buf.writeVarInt(taskType);
            buf.writeBoolean(hasPos);
            buf.writeInt(posX);
            buf.writeInt(posY);
            buf.writeInt(posZ);
            buf.writeUtf(itemId, 256);
            buf.writeVarInt(count);
            buf.writeUtf(customText, 512);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                if (!TargetManager.canControl(sender.getUUID(), targetUUID)) return;

                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;
                ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
                if (target == null) return;

                TaskData.TaskType[] allTypes = TaskData.TaskType.values();
                if (taskType < 0 || taskType >= allTypes.length) return;
                TaskData.TaskType type = allTypes[taskType];
                TaskData task = switch (type) {
                    case FOLLOW  -> TaskData.follow(sender.getUUID());
                    case FREE    -> TaskData.free(sender.getUUID());
                    case STAY    -> TaskData.stay(target.blockPosition(), sender.getUUID());
                    case GOTO    -> TaskData.goTo(new BlockPos(posX, posY, posZ), sender.getUUID());
                    case COLLECT -> TaskData.collect(itemId, count, sender.getUUID(), countItems(target, itemId));
                    case CUSTOM  -> TaskData.custom(customText, sender.getUUID());
                };

                TargetManager.setTask(targetUUID, task);

                target.displayClientMessage(
                        Component.literal("\u00A7e\u65B0\u6307\u4EE4\u5DF2\u4E0B\u53D1: \u00A7f" + task.getDisplayText()), false);
                sender.displayClientMessage(
                        Component.literal("\u00A7a\u5DF2\u5411 " + target.getGameProfile().getName() + " \u53D1\u9001\u4EFB\u52A1"), true);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?
     *  5. C2S 闂?闂備礁缍婇弨杈懌濠电偟鈷堥崑濠囩嵁閹烘洦妲婚梺?
     * 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?*/

    public static class C2SRestraintControl {
        /** 闂備胶鎳撻悘婵堢矓瀹曞洨绀婇柡鍐ㄥ€荤亸鐢告偣閸ヮ亜鐨洪柍? 0=闂傚倷绀侀妵妯肩矆娓氣偓閹偓銈ｉ崘銊ь吋闁诲函缍嗛崰妤呭箚? 1=闂佽崵鍠愰悷杈╃礊娓氣偓瀵劑鏁愭径濠勵吋闁诲函缍嗛崰妤呭箚? 2=闂傚倷绀侀妵妯肩矆娓氣偓閹偓銈ｉ崘鈺佹畻濠殿喗顭堥崺鏍р枔? 3=闂佽崵鍠愰悷杈╃礊娓氣偓瀵劑鏁愭径瀣畻濠殿喗顭堥崺鏍р枔? 4=闂備胶顭堢换鍫ュ礉閹达箑缁? 5=闂備胶顭堢换鍫ュ礉韫囨凹鏆?*/
        private final UUID targetUUID;
        private final int action;

        public static final int LOCK_HANDCUFFS = 0;
        public static final int UNLOCK_HANDCUFFS = 1;
        public static final int LOCK_ANKLETS = 2;
        public static final int UNLOCK_ANKLETS = 3;
        public static final int LOCK_ALL = 4;
        public static final int UNLOCK_ALL = 5;

        public C2SRestraintControl(UUID target, int action) {
            this.targetUUID = target;
            this.action = action;
        }

        public C2SRestraintControl(FriendlyByteBuf buf) {
            targetUUID = buf.readUUID();
            action = buf.readVarInt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(targetUUID);
            buf.writeVarInt(action);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                if (!TargetManager.canControl(sender.getUUID(), targetUUID)) return;

                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;
                ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
                if (target == null) return;

                switch (action) {
                    case LOCK_HANDCUFFS   -> {
                        RestraintManager.lockHandcuffs(target, sender.getUUID());
                        target.displayClientMessage(Component.literal("\u00A7c\u4F60\u7684\u624B\u94D0\u5DF2\u88AB\u9501\u5B9A"), false);
                    }
                    case UNLOCK_HANDCUFFS -> {
                        RestraintManager.unlockHandcuffs(target);
                        target.displayClientMessage(Component.literal("\u00A7a\u624B\u94D0\u5DF2\u89E3\u9501"), false);
                    }
                    case LOCK_ANKLETS     -> {
                        RestraintManager.lockAnklets(target, sender.getUUID());
                        target.displayClientMessage(Component.literal("\u00A7c\u4F60\u7684\u811A\u9563\u5DF2\u88AB\u9501\u5B9A"), false);
                    }
                    case UNLOCK_ANKLETS   -> {
                        RestraintManager.unlockAnklets(target);
                        target.displayClientMessage(Component.literal("\u00A7a\u811A\u9563\u5DF2\u89E3\u9501"), false);
                    }
                    case LOCK_ALL         -> {
                        RestraintManager.lockAll(target, sender.getUUID());
                        target.displayClientMessage(Component.literal("\u00A7c\u624B\u94D0\u4E0E\u811A\u9563\u5DF2\u5168\u90E8\u9501\u5B9A"), false);
                    }
                    case UNLOCK_ALL       -> {
                        RestraintManager.unlockAll(target);
                        target.displayClientMessage(Component.literal("\u00A7a\u624B\u94D0\u4E0E\u811A\u9563\u5DF2\u5168\u90E8\u89E3\u9501"), false);
                    }
                }

                sender.displayClientMessage(Component.literal("\u00A7a\u9547\u5177\u63A7\u5236\u5DF2\u66F4\u65B0"), true);
                // 闂備礁鎲￠悷锕傚垂閸ф鐒垫い鎴ｅ劵閸忓苯鈹戦鑺ュ€愭鐐╁亾婵炶揪缍€椤鐥鐐寸厸闁稿本绋戦ˉ瀣磼椤旀儳顣肩紒瀣槺閹峰鎼归銈嗙盀
                sendStatusToMaster(sender, targetUUID);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?
     *  6. C2S 闂?濠碉紕鍋戦崐妤呭箠閹剧鑰块柍褜鍓熼弻鐔烘尒婢跺﹤鏆欓柣?
     * 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?*/

    public static class C2SCollarControl {
        public static final int RELEASE = 0;
        public static final int RESTRICT_MOVEMENT = 1;
        public static final int UNRESTRICT_MOVEMENT = 2;
        public static final int RESTRICT_INTERACTION = 3;
        public static final int UNRESTRICT_INTERACTION = 4;

        private final UUID targetUUID;
        private final int action;

        public C2SCollarControl(UUID target, int action) {
            this.targetUUID = target;
            this.action = action;
        }

        public C2SCollarControl(FriendlyByteBuf buf) {
            targetUUID = buf.readUUID();
            action = buf.readVarInt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(targetUUID);
            buf.writeVarInt(action);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                if (!TargetManager.canControl(sender.getUUID(), targetUUID)) return;

                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;
                ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
                if (target == null) return;

                // 闂備胶鎳撻悘姘跺磿閹惰棄鏄ョ€光偓閸曨剙鍓銈嗘⒒閺咁偊宕规總鍛婎棅妞ゆ帒顦禍妤冪磼濡も偓閸婂潡骞嗛崘顔肩妞ゆ牗菤閸嬫挸顓兼径濠冨祶?
                var handlerOpt = CuriosApi.getCuriosHelper().getCuriosHandler(target);
                if (!handlerOpt.isPresent()) return;
                var handler = handlerOpt.resolve().get();
                var necklaceOpt = handler.getStacksHandler("necklace");
                if (necklaceOpt.isEmpty()) return;

                var stacks = necklaceOpt.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (!(stack.getItem() instanceof TechCollar)) continue;

                    switch (action) {
                        case RELEASE -> {
                            TechCollar.clearOwner(stack);
                            TechCollar.unlockRemoval();
                            TechCollar.setMovementRestricted(stack, false);
                            TechCollar.setInteractionRestricted(stack, false);
                            TargetManager.setExoDarknessEnabled(targetUUID, false);
                            TargetManager.setGogglesVisionEnabled(targetUUID, false);
                            sendVisionStateToTarget(target);
                            target.displayClientMessage(Component.literal("\u00A7a\u9879\u5708\u5DF2\u8FDC\u7A0B\u91CA\u653E"), false);
                        }
                        case RESTRICT_MOVEMENT -> {
                            TechCollar.setMovementRestricted(stack, true);
                            target.displayClientMessage(Component.literal("\u00A7c\u79FB\u52A8\u5DF2\u88AB\u9650\u5236"), false);
                        }
                        case UNRESTRICT_MOVEMENT -> {
                            TechCollar.setMovementRestricted(stack, false);
                            target.displayClientMessage(Component.literal("\u00A7a\u79FB\u52A8\u9650\u5236\u5DF2\u89E3\u9664"), false);
                        }
                        case RESTRICT_INTERACTION -> {
                            TechCollar.setInteractionRestricted(stack, true);
                            target.displayClientMessage(Component.literal("\u00A7c\u4EA4\u4E92\u5DF2\u88AB\u9650\u5236"), false);
                        }
                        case UNRESTRICT_INTERACTION -> {
                            TechCollar.setInteractionRestricted(stack, false);
                            target.displayClientMessage(Component.literal("\u00A7a\u4EA4\u4E92\u9650\u5236\u5DF2\u89E3\u9664"), false);
                        }
                    }
                    break; // 闂備礁鎲￠悷顖涚濠婂煻鍥煛閸涱喖浠╅梺绯曞墲閻燁垶宕曢幋婵冩闁圭虎鍨版禍鐐箾閹寸偞鎯勫ù婊庝邯婵℃潙顓兼径濠冨祶?
                }

                sender.displayClientMessage(Component.literal("\u00A7a\u9879\u5708\u63A7\u5236\u5DF2\u66F4\u65B0"), true);
                sendStatusToMaster(sender, targetUUID);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?
     *  7. C2S 闂?闂佽崵濮村ú顓㈠绩闁秵鍎戝ù鐓庣摠閸庡孩銇勯弮鍌涙珪闁搞劌銈搁弻锝呂熼崹顔惧帿闂?
     * 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?*/

    public static class C2SBrainwashControl {
        private final UUID targetUUID;
        private final boolean enabled;

        public C2SBrainwashControl(UUID targetUUID, boolean enabled) {
            this.targetUUID = targetUUID;
            this.enabled = enabled;
        }

        public C2SBrainwashControl(FriendlyByteBuf buf) {
            this.targetUUID = buf.readUUID();
            this.enabled = buf.readBoolean();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(targetUUID);
            buf.writeBoolean(enabled);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                if (!TargetManager.canControl(sender.getUUID(), targetUUID)) return;

                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;
                ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
                if (target == null) return;

                BrainwashManager.setActive(targetUUID, enabled);
                NetworkHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> target),
                        new S2CBrainwashState(enabled)
                );

                target.displayClientMessage(Component.literal(enabled ? "\u00A7d\u6D17\u8111\u6EE4\u955C\u5DF2\u542F\u7528" : "\u00A7a\u6D17\u8111\u6EE4\u955C\u5DF2\u5173\u95ED"), true);
                sender.displayClientMessage(Component.literal(enabled ? "\u00A7d\u5DF2\u5BF9\u76EE\u6807\u542F\u7528\u6D17\u8111\u6EE4\u955C" : "\u00A7a\u5DF2\u5BF9\u76EE\u6807\u5173\u95ED\u6D17\u8111\u6EE4\u955C"), true);
                sendStatusToMaster(sender, targetUUID);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class S2CBrainwashState {
        private final boolean enabled;

        public S2CBrainwashState(boolean enabled) {
            this.enabled = enabled;
        }

        public S2CBrainwashState(FriendlyByteBuf buf) {
            this.enabled = buf.readBoolean();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(enabled);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> com.zmer.testmod.client.BrainwashHudOverlay.setEnabled(enabled));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class C2SEffectControl {
        public static final int ENABLE_EXO_DARKNESS = 0;
        public static final int DISABLE_EXO_DARKNESS = 1;
        public static final int ENABLE_GOGGLES_VISION = 2;
        public static final int DISABLE_GOGGLES_VISION = 3;

        private final UUID targetUUID;
        private final int action;

        public C2SEffectControl(UUID targetUUID, int action) {
            this.targetUUID = targetUUID;
            this.action = action;
        }

        public C2SEffectControl(FriendlyByteBuf buf) {
            this.targetUUID = buf.readUUID();
            this.action = buf.readVarInt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(targetUUID);
            buf.writeVarInt(action);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                if (!TargetManager.canControl(sender.getUUID(), targetUUID)) return;

                var server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return;
                ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
                if (target == null) return;

                switch (action) {
                    case ENABLE_EXO_DARKNESS -> {
                        TargetManager.setExoDarknessEnabled(targetUUID, true);
                        target.displayClientMessage(Component.literal("§5外骨骼黑暗效果已启用"), true);
                    }
                    case DISABLE_EXO_DARKNESS -> {
                        TargetManager.setExoDarknessEnabled(targetUUID, false);
                        target.displayClientMessage(Component.literal("§a外骨骼黑暗效果已关闭"), true);
                    }
                    case ENABLE_GOGGLES_VISION -> {
                        TargetManager.setGogglesVisionEnabled(targetUUID, true);
                        target.displayClientMessage(Component.literal("§5眼镜黑白视觉已启用"), true);
                    }
                    case DISABLE_GOGGLES_VISION -> {
                        TargetManager.setGogglesVisionEnabled(targetUUID, false);
                        target.displayClientMessage(Component.literal("§a眼镜黑白视觉已关闭"), true);
                    }
                    default -> {
                        return;
                    }
                }

                sendVisionStateToTarget(target);
                sender.displayClientMessage(Component.literal("§a视觉效果开关已更新"), true);
                sendStatusToMaster(sender, targetUUID);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class S2CVisionControl {
        private final boolean gogglesVisionEnabled;

        public S2CVisionControl(boolean gogglesVisionEnabled) {
            this.gogglesVisionEnabled = gogglesVisionEnabled;
        }

        public S2CVisionControl(FriendlyByteBuf buf) {
            this.gogglesVisionEnabled = buf.readBoolean();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(gogglesVisionEnabled);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    com.zmer.testmod.client.SoundDarknessRenderer.setPanelVisionEnabled(gogglesVisionEnabled));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class C2SRequestStatus {
        private final UUID targetUUID;

        public C2SRequestStatus(UUID target) { this.targetUUID = target; }
        public C2SRequestStatus(FriendlyByteBuf buf) { targetUUID = buf.readUUID(); }
        public void encode(FriendlyByteBuf buf) { buf.writeUUID(targetUUID); }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if (sender == null) return;
                if (!TargetManager.canControl(sender.getUUID(), targetUUID)) return;
                sendStatusToMaster(sender, targetUUID);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /* 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?
     *  8. S2C 闂?闂備胶鍎甸弲鈺呭窗濡ゅ懏鍋夐柨婵嗩槹閸嬫劙鏌ら崫銉毌闁稿鎸婚幏鍛喆閸曨剦鍟€闂?
     * 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?*/

    public static class S2CTargetStatus {
        private final UUID targetUUID;
        private final String targetName;
        private final int posX, posY, posZ;
        private final String dimension;
        private final float health;
        private final int hunger;
        private final boolean hasCollar;
        private final boolean collarMovementRestricted;
        private final boolean collarInteractionRestricted;
        private final boolean hasHandcuffs;
        private final boolean handcuffsLocked;
        private final boolean hasAnklets;
        private final boolean ankletsLocked;
        private final boolean brainwashed;
        private final boolean exoDarknessEnabled;
        private final boolean gogglesVisionEnabled;
        private final String currentTask;

        public S2CTargetStatus(UUID targetUUID, String targetName,
                               int posX, int posY, int posZ, String dimension,
                               float health, int hunger,
                               boolean hasCollar, boolean collarMovement, boolean collarInteraction,
                                boolean hasHandcuffs, boolean handcuffsLocked,
                                boolean hasAnklets, boolean ankletsLocked,
                                boolean brainwashed,
                                boolean exoDarknessEnabled, boolean gogglesVisionEnabled,
                                String currentTask) {
            this.targetUUID = targetUUID;
            this.targetName = targetName;
            this.posX = posX; this.posY = posY; this.posZ = posZ;
            this.dimension = dimension;
            this.health = health;
            this.hunger = hunger;
            this.hasCollar = hasCollar;
            this.collarMovementRestricted = collarMovement;
            this.collarInteractionRestricted = collarInteraction;
            this.hasHandcuffs = hasHandcuffs;
            this.handcuffsLocked = handcuffsLocked;
            this.hasAnklets = hasAnklets;
            this.ankletsLocked = ankletsLocked;
            this.brainwashed = brainwashed;
            this.exoDarknessEnabled = exoDarknessEnabled;
            this.gogglesVisionEnabled = gogglesVisionEnabled;
            this.currentTask = currentTask;
        }

        public S2CTargetStatus(FriendlyByteBuf buf) {
            targetUUID = buf.readUUID();
            targetName = buf.readUtf(64);
            posX = buf.readInt(); posY = buf.readInt(); posZ = buf.readInt();
            dimension = buf.readUtf(128);
            health = buf.readFloat();
            hunger = buf.readVarInt();
            hasCollar = buf.readBoolean();
            collarMovementRestricted = buf.readBoolean();
            collarInteractionRestricted = buf.readBoolean();
            hasHandcuffs = buf.readBoolean();
            handcuffsLocked = buf.readBoolean();
            hasAnklets = buf.readBoolean();
            ankletsLocked = buf.readBoolean();
            brainwashed = buf.readBoolean();
            exoDarknessEnabled = buf.readBoolean();
            gogglesVisionEnabled = buf.readBoolean();
            currentTask = buf.readUtf(512);
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(targetUUID);
            buf.writeUtf(targetName, 64);
            buf.writeInt(posX); buf.writeInt(posY); buf.writeInt(posZ);
            buf.writeUtf(dimension, 128);
            buf.writeFloat(health);
            buf.writeVarInt(hunger);
            buf.writeBoolean(hasCollar);
            buf.writeBoolean(collarMovementRestricted);
            buf.writeBoolean(collarInteractionRestricted);
            buf.writeBoolean(hasHandcuffs);
            buf.writeBoolean(handcuffsLocked);
            buf.writeBoolean(hasAnklets);
            buf.writeBoolean(ankletsLocked);
            buf.writeBoolean(brainwashed);
            buf.writeBoolean(exoDarknessEnabled);
            buf.writeBoolean(gogglesVisionEnabled);
            buf.writeUtf(currentTask, 512);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.screen instanceof com.zmer.testmod.client.ControlPanelScreen screen) {
                    screen.receiveStatus(this);
                }
            });
            ctx.get().setPacketHandled(true);
        }

        // Getters
        public UUID getTargetUUID()        { return targetUUID; }
        public String getTargetName()      { return targetName; }
        public int getPosX()               { return posX; }
        public int getPosY()               { return posY; }
        public int getPosZ()               { return posZ; }
        public String getDimension()       { return dimension; }
        public float getHealth()           { return health; }
        public int getHunger()             { return hunger; }
        public boolean hasCollar()         { return hasCollar; }
        public boolean isCollarMovementRestricted()    { return collarMovementRestricted; }
        public boolean isCollarInteractionRestricted() { return collarInteractionRestricted; }
        public boolean hasHandcuffs()      { return hasHandcuffs; }
        public boolean isHandcuffsLocked() { return handcuffsLocked; }
        public boolean hasAnklets()        { return hasAnklets; }
        public boolean isAnkletsLocked()   { return ankletsLocked; }
        public boolean isBrainwashed()     { return brainwashed; }
        public boolean isExoDarknessEnabled() { return exoDarknessEnabled; }
        public boolean isGogglesVisionEnabled() { return gogglesVisionEnabled; }
        public String getCurrentTask()     { return currentTask; }
    }

    /* 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?
     *  闁诲氦顫夐幃鍫曞磿闁秴鐭楅柛褎顨呭Λ姗€鎮峰▎蹇擃伀闁靛棗锕弻銊モ槈濞嗘埈鏆￠梺杞扮缂嶅﹤顫忛崸妤€骞㈡繛鍡樺姉閻涖儵姊洪崫鍕㈡繛灞傚妽閹便劑骞栨担鍝ョ暠婵炶揪绲鹃悺鏇犫偓姘懇閺屾稑鈻庨幇顒備紝闂侀€炲苯鍘搁柤鍐插閸掓帡鎮ч崼鈶╂灃闁荤姳娴囧▍鏇㈡儑?
     * 闂備礁纾崕銈夊礉韫囨稑鐤鹃柡灞诲劚閻撴盯鏌熼懜顒€濡芥繛鍛矒閺屽秹鎮滃Ο鍝勵潊闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻濇娊姊哄畷鍥у妺闁告柨绻樺鏌ュ蓟閵夈儳鍘搁梺纭呭焽閸斿秴鈻嶅鍫熺厓闁绘垶锚婵偓闂佸搫鎳忛悡锟犲春閿熺姴绠涢柕濠忛檮閻?*/

    public static void sendStatusToMaster(ServerPlayer master, UUID targetUUID) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer target = server.getPlayerList().getPlayer(targetUUID);
        if (target == null) return;

        BlockPos pos = target.blockPosition();
        String dim = target.level().dimension().location().toString();
        float health = target.getHealth();
        int hunger = target.getFoodData().getFoodLevel();
        String name = target.getGameProfile().getName();

        // 濠碉紕鍋戦崐妤呭箠閹剧鑰块柍褜鍓熼弻锝呂熼崹顔惧帿闂?
        boolean hasCollar = false, collarMove = false, collarInteract = false;
        boolean hasHandcuffs = false, hasAnklets = false;

        var handlerOpt = CuriosApi.getCuriosHelper().getCuriosHandler(target);
        if (handlerOpt.isPresent()) {
            var handler = handlerOpt.resolve().get();

            // 濠碉紕鍋戦崐妤呭箠閹剧鑰块柍?
            var neckOpt = handler.getStacksHandler("necklace");
            if (neckOpt.isPresent()) {
                var stacks = neckOpt.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() instanceof TechCollar) {
                        hasCollar = true;
                        collarMove = TechCollar.isMovementRestricted(stack);
                        collarInteract = TechCollar.isInteractionRestricted(stack);
                        break;
                    }
                }
            }

            // 闂備礁銈稿褍顭囬敓鐘茬？?
            var handsOpt = handler.getStacksHandler("hands");
            if (handsOpt.isPresent()) {
                var stacks = handsOpt.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() instanceof ElectronicShacklesItem) {
                        hasHandcuffs = true;
                        break;
                    }
                }
            }

            var legsOpt = handler.getStacksHandler("legs");
            if (legsOpt.isPresent()) {
                var stacks = legsOpt.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack.getItem() instanceof AnkleShacklesItem) hasAnklets = true;
                }
            }
        }

        boolean handcuffsLocked = RestraintManager.isHandcuffsLocked(targetUUID);
        boolean ankletsLocked = RestraintManager.isAnkletsLocked(targetUUID);
        boolean brainwashed = BrainwashManager.isActive(targetUUID);
        boolean exoDarknessEnabled = TargetManager.isExoDarknessEnabled(targetUUID);
        boolean gogglesVisionEnabled = TargetManager.isGogglesVisionEnabled(targetUUID);

        TaskData task = TargetManager.getTask(targetUUID);
        String taskText = task != null ? task.getDisplayText() : "";

        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> master),
                new S2CTargetStatus(targetUUID, name,
                        pos.getX(), pos.getY(), pos.getZ(), dim,
                        health, hunger,
                        hasCollar, collarMove, collarInteract,
                        hasHandcuffs, handcuffsLocked,
                        hasAnklets, ankletsLocked,
                        brainwashed,
                        exoDarknessEnabled, gogglesVisionEnabled,
                        taskText)
        );
    }

    private static void sendVisionStateToTarget(ServerPlayer target) {
        boolean gogglesVisionEnabled = TargetManager.isGogglesVisionEnabled(target.getUUID());
        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> target),
                new S2CVisionControl(gogglesVisionEnabled)
        );
    }

    private static int countItems(ServerPlayer player, String itemId) {
        if (itemId == null || itemId.isBlank()) return 0;
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return 0;
        var item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) return 0;

        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }
}

