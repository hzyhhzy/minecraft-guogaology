package dev.googology.ambience;

import dev.googology.GoogologyMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/** Server-authored notes; semitone -1 is a silent rest, row -1 stops the visual performance. */
public record TableNotePayload(BlockPos table,BlockPos note,int semitone,int row,boolean marked) implements CustomPayload {
    public static final Id<TableNotePayload> ID=new Id<>(GoogologyMod.id("table_note"));
    public static final PacketCodec<RegistryByteBuf,TableNotePayload> CODEC=PacketCodec.of(TableNotePayload::write,TableNotePayload::read);
    private void write(RegistryByteBuf buf) { buf.writeBlockPos(table);buf.writeBlockPos(note);buf.writeVarInt(semitone);buf.writeVarInt(row);buf.writeBoolean(marked); }
    private static TableNotePayload read(RegistryByteBuf buf) { return new TableNotePayload(buf.readBlockPos(),buf.readBlockPos(),buf.readVarInt(),buf.readVarInt(),buf.readBoolean()); }
    @Override public Id<TableNotePayload> getId() { return ID; }
}
