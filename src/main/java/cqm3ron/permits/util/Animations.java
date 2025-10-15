package cqm3ron.permits.util;

import cqm3ron.permits.component.ModDataComponentTypes;
import cqm3ron.permits.item.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class Animations {
    public static void playTotemAnimation(ServerPlayerEntity player){
        player.networkHandler.sendPacket(new EntityStatusS2CPacket(player, (byte) 35));
        // custom item for totem?!?!?!?
    }
}
