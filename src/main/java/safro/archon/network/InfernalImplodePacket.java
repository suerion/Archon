package safro.archon.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;
import safro.archon.Archon;
import safro.archon.registry.ItemRegistry;
import safro.archon.util.ArchonUtil;

public class InfernalImplodePacket {
    public static final Identifier ID = new Identifier(Archon.MODID, "infernal_implode");

    public static void send() {
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(InfernalImplodePacket.ID, buf);
    }

    public static void receive(ServerPlayerEntity player) {
        if (player.getEquippedStack(EquipmentSlot.CHEST).isOf(ItemRegistry.INFERNAL_COAT)) {
            int mana = ArchonUtil.get(player).getMana();
            float power = MathHelper.clamp((float) mana / 30.0F, 0.1F, 10.0F);
            player.world.createExplosion(player, player.getX(), player.getY(), player.getZ(), power, Explosion.DestructionType.NONE);

            for (int i = 0; i < Direction.Axis.VALUES.length; i++) {
                for (Vec3d vec3d : ArchonUtil.getVectorsForCircle(player.getX(), player.getY(), player.getZ(), 3.5D, 2, Direction.Axis.VALUES[i])) {
                    ParticlePacket.send(player, ParticleTypes.SMALL_FLAME, vec3d.x, vec3d.y, vec3d.z, 0.0D, 0.03D, 0.0D);
                }
            }
            ArchonUtil.get(player).removeMana(mana);
        }
    }
}