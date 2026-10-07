package dev.guogaology.ambience;

import dev.guogaology.GuogaologyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-authored notes; semitone -1 is a silent rest, row -1 stops the visual performance. */
public record TableNotePayload(BlockPos table,BlockPos note,int semitone,int row,boolean marked) implements CustomPacketPayload {
    public static final Type<TableNotePayload> ID=new Type<>(GuogaologyMod.id("table_note"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TableNotePayload> CODEC=StreamCodec.ofMember(TableNotePayload::write,TableNotePayload::read);
    private void write(RegistryFriendlyByteBuf buf) { buf.writeBlockPos(table);buf.writeBlockPos(note);buf.writeVarInt(semitone);buf.writeVarInt(row);buf.writeBoolean(marked); }
    private static TableNotePayload read(RegistryFriendlyByteBuf buf) { return new TableNotePayload(buf.readBlockPos(),buf.readBlockPos(),buf.readVarInt(),buf.readVarInt(),buf.readBoolean()); }
    @Override public Type<TableNotePayload> type() { return ID; }
}
