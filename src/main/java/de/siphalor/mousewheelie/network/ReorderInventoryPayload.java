package de.siphalor.mousewheelie.network;

import de.siphalor.mousewheelie.MouseWheelie;
import lombok.CustomLog;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@CustomLog
public record ReorderInventoryPayload(int syncId, int[] slotMappings) implements CustomPayload {

    public static final PacketCodec<PacketByteBuf, ReorderInventoryPayload> CODEC = PacketCodec.of(ReorderInventoryPayload::write, ReorderInventoryPayload::read);
    public static final Id<ReorderInventoryPayload> ID = new Id<>(MouseWheelie.id("reorder_inventory_c2s"));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(@NotNull PacketByteBuf buf) {
        buf.writeVarInt(syncId);
        buf.writeIntArray(slotMappings);
    }

    public static @Nullable ReorderInventoryPayload read(PacketByteBuf buf) {
        int syncId = buf.readVarInt();
        int[] reorderedIndices = buf.readIntArray();

        if (reorderedIndices.length % 2 != 0) {
            log.warn("Received reorder inventory packet with invalid data!");
            return null;
        }

        return new ReorderInventoryPayload(syncId, reorderedIndices);
    }
}
