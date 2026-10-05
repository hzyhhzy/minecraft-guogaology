package dev.googology.outer.world.feature;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
/** Backport of the 26.3 template entry data contract. */
public record TemplateEntry(Identifier template,List<Rotation> rotations){
 public static final Codec<TemplateEntry> CODEC=RecordCodecBuilder.create(i->i.group(Identifier.CODEC.fieldOf("id").forGetter(TemplateEntry::template),Rotation.CODEC.listOf().optionalFieldOf("rotations",List.of(Rotation.values())).forGetter(TemplateEntry::rotations)).apply(i,TemplateEntry::new));
}
