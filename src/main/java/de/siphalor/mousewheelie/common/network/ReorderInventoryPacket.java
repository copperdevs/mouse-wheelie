/*
 * Copyright 2020-2022 Siphalor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

package de.siphalor.mousewheelie.common.network;

import de.siphalor.mousewheelie.MouseWheelie;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReorderInventoryPacket {
    int syncId;
    int[] slotMappings;

    public ReorderInventoryPacket(int syncId, int[] reorderedIndices) {
        this.syncId = syncId;
        slotMappings = reorderedIndices;
    }

    public void write(@NotNull PacketByteBuf buf) {
        buf.writeVarInt(syncId);
        buf.writeIntArray(slotMappings);
    }

    public static @Nullable ReorderInventoryPacket read(PacketByteBuf buf) {
        int syncId = buf.readVarInt();
        int[] reorderedIndices = buf.readIntArray();

        if (reorderedIndices.length % 2 != 0) {
            MouseWheelie.logWarn("Received reorder inventory packet with invalid data!");
            return null;
        }

        return new ReorderInventoryPacket(syncId, reorderedIndices);
    }
}
