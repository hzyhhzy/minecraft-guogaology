package dev.guogaology.client.compat.jei;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayList;
import java.util.List;

/** Checks bytecode provenance without resolving or loading any Minecraft/JEI class. */
public final class JeiTagCompatShape {
    public static final String TARGET = "mezz.jei.common.recipes.VanillaClientRecipeLoader";
    private static final String REGISTRY = "net/minecraft/core/RegistryAccess";
    private static final String FROZEN = "Lnet/minecraft/core/RegistryAccess$Frozen;";
    private static final String RESOURCES = "Lnet/minecraft/server/packs/resources/ResourceManager;";
    private static final String LIST = "Ljava/util/List;";
    private static final String LOAD_TAGS = "(" + RESOURCES + "L" + REGISTRY + ";)" + LIST;
    private static final String LOOKUPS = "(" + FROZEN + LIST + ")" + LIST;
    private static final String LOAD_REGISTRY = "(" + RESOURCES + LIST + LIST
        + "Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;";

    private JeiTagCompatShape() { }

    /** Null means the first tag load is safe to bypass; otherwise returns the reason. */
    public static String incompatibility(ClassNode owner) {
        if (!owner.name.equals(TARGET.replace('.', '/'))) return "unexpected target class";
        List<MethodNode> targets = owner.methods.stream()
            .filter(m -> m.name.equals("loadVanillaRecipeRegistry")).toList();
        if (targets.size() != 1) return "expected one fallback method";
        MethodNode method = targets.getFirst();
        if ((method.access & Opcodes.ACC_STATIC) == 0 || !method.desc.equals(
            "(L" + REGISTRY + ";)Lnet/minecraft/world/item/crafting/RecipeMap;")) {
            return "unexpected fallback descriptor";
        }
        try {
            Flow flow = new Flow();
            Frame<SourceValue>[] frames = new Analyzer<>(flow).analyze(owner.name, method);
            List<MethodInsnNode> loads = calls(method, "net/minecraft/tags/TagLoader",
                "loadTagsForExistingRegistries", LOAD_TAGS);
            List<MethodInsnNode> lookups = calls(method, "net/minecraft/tags/TagLoader", "buildUpdatedLookups", LOOKUPS);
            List<MethodInsnNode> applies = calls(method, "java/util/List", "forEach", "(Ljava/util/function/Consumer;)V");
            if (loads.size() != 2 || lookups.size() != 2 || applies.size() != 2) {
                return "expected two tag loads, two lookup builders and two apply loops";
            }
            MethodInsnNode base = loads.getFirst(), world = loads.getLast();
            if (base.getOpcode() != Opcodes.INVOKESTATIC || world.getOpcode() != Opcodes.INVOKESTATIC
                || lookups.stream().anyMatch(call -> call.getOpcode() != Opcodes.INVOKESTATIC)) {
                return "tag loads must be static";
            }
            AbstractInsnNode resources = input(method, frames, base, 0);
            AbstractInsnNode frozen = input(method, frames, base, 1);
            if (!(resources instanceof TypeInsnNode allocation) || allocation.getOpcode() != Opcodes.NEW
                || !allocation.desc.equals("net/minecraft/server/packs/resources/MultiPackResourceManager")) {
                return "base resources are not the temporary pack manager";
            }
            if (!isCall(frozen, REGISTRY, "freeze", "()" + FROZEN)
                || input(method, frames, (MethodInsnNode) frozen, 0) != flow.parameter) {
                return "first tag load does not use the active registry parameter";
            }
            if (input(method, frames, world, 0) != resources) return "tag loads use different resources";
            if (input(method, frames, lookups.getFirst(), 0) != frozen
                || input(method, frames, lookups.getFirst(), 1) != base) return "base lookup provenance changed";
            AbstractInsnNode joined = input(method, frames, world, 1);
            if (!isCall(joined, "java/util/concurrent/CompletableFuture", "join", "()Ljava/lang/Object;")) {
                return "second tag load does not use a freshly loaded registry";
            }
            AbstractInsnNode loaded = input(method, frames, (MethodInsnNode) joined, 0);
            if (!isCall(loaded, "net/minecraft/resources/RegistryDataLoader", "load", LOAD_REGISTRY)
                || loaded.getOpcode() != Opcodes.INVOKESTATIC) return "world registry loader changed";
            MethodInsnNode loading = (MethodInsnNode) loaded;
            AbstractInsnNode registryKeys = input(method, frames, loading, 2);
            if (input(method, frames, loading, 0) != resources
                || input(method, frames, loading, 1) != lookups.getFirst()
                || !(registryKeys instanceof FieldInsnNode keys) || keys.getOpcode() != Opcodes.GETSTATIC
                || !keys.owner.equals("net/minecraft/resources/RegistryDataLoader")
                || !keys.name.equals("WORLD_REGISTRIES") || !keys.desc.equals(LIST)) {
                return "second registry is not the temporary WORLD registry";
            }
            if (input(method, frames, lookups.getLast(), 0) != joined
                || input(method, frames, lookups.getLast(), 1) != world) return "world lookup provenance changed";
            for (int i = 0; i < 2; i++) {
                if (input(method, frames, applies.get(i), 0) != loads.get(i)
                    || !isApplyConsumer(input(method, frames, applies.get(i), 1))) {
                    return "pending-tag application provenance changed";
                }
            }
            // No hidden use of a pending list may rely on its being nonempty.
            for (AbstractInsnNode instruction : method.instructions) {
                int operands;
                if (instruction instanceof MethodInsnNode call) {
                    if (lookups.contains(call) || applies.contains(call)) continue;
                    operands = Type.getArgumentTypes(call.desc).length
                        + (call.getOpcode() == Opcodes.INVOKESTATIC ? 0 : 1);
                } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                    operands = Type.getArgumentTypes(dynamic.desc).length;
                } else {
                    operands = switch (instruction.getOpcode()) {
                        case Opcodes.CHECKCAST -> ((TypeInsnNode) instruction).desc.equals("java/util/List")
                            || ((TypeInsnNode) instruction).desc.equals("java/lang/Object") ? 0 : 1;
                        case Opcodes.PUTSTATIC, Opcodes.GETFIELD, Opcodes.ARETURN, Opcodes.ATHROW,
                            Opcodes.IFNULL, Opcodes.IFNONNULL, Opcodes.INSTANCEOF,
                            Opcodes.MONITORENTER, Opcodes.MONITOREXIT -> 1;
                        case Opcodes.PUTFIELD, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE -> 2;
                        case Opcodes.AASTORE -> 3;
                        default -> 0;
                    };
                }
                Frame<SourceValue> frame = frames[method.instructions.indexOf(instruction)];
                if (frame == null) continue;
                for (int index = frame.getStackSize() - operands; index < frame.getStackSize(); index++) {
                    SourceValue value = frame.getStack(index);
                    if (value.insns.contains(base) || value.insns.contains(world)) return "pending tags escape or have an additional consumer";
                }
            }
            return null;
        } catch (Exception failure) {
            return "unrecognized bytecode: " + failure.getClass().getSimpleName();
        }
    }

    private static List<MethodInsnNode> calls(MethodNode method, String owner, String name, String desc) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (isCall(instruction, owner, name, desc)) result.add((MethodInsnNode) instruction);
        }
        return result;
    }

    private static boolean isCall(AbstractInsnNode instruction, String owner, String name, String desc) {
        return instruction instanceof MethodInsnNode call && call.owner.equals(owner)
            && call.name.equals(name) && call.desc.equals(desc);
    }

    /** Includes the receiver as argument zero for instance calls. Merged origins are rejected. */
    private static AbstractInsnNode input(MethodNode method, Frame<SourceValue>[] frames, MethodInsnNode call, int argument) {
        Frame<SourceValue> frame = frames[method.instructions.indexOf(call)];
        if (frame == null) return null;
        int operands = Type.getArgumentTypes(call.desc).length + (call.getOpcode() == Opcodes.INVOKESTATIC ? 0 : 1);
        SourceValue value = frame.getStack(frame.getStackSize() - operands + argument);
        return value.insns.size() == 1 ? value.insns.iterator().next() : null;
    }

    private static boolean isApplyConsumer(AbstractInsnNode source) {
        if (!(source instanceof InvokeDynamicInsnNode dynamic) || !dynamic.desc.equals("()Ljava/util/function/Consumer;")
            || dynamic.bsm.getTag() != Opcodes.H_INVOKESTATIC
            || !dynamic.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")
            || !dynamic.bsm.getName().equals("metafactory") || dynamic.bsmArgs.length != 3) return false;
        return dynamic.bsmArgs[1] instanceof Handle handle && handle.getTag() == Opcodes.H_INVOKEINTERFACE
            && handle.getOwner().equals("net/minecraft/core/Registry$PendingTags")
            && handle.getName().equals("apply") && handle.getDesc().equals("()V");
    }

    /** Loads/stores, DUP/SWAP and casts preserve origins, including optimized JEI builds. */
    private static final class Flow extends SourceInterpreter {
        private final LabelNode parameter = new LabelNode();
        private Flow() { super(Opcodes.ASM9); }
        @Override public SourceValue newParameterValue(boolean instance, int local, Type type) {
            return new SourceValue(type.getSize(), parameter);
        }
        @Override public SourceValue copyOperation(AbstractInsnNode instruction, SourceValue value) { return value; }
        @Override public SourceValue unaryOperation(AbstractInsnNode instruction, SourceValue value) {
            return instruction.getOpcode() == Opcodes.CHECKCAST ? value : super.unaryOperation(instruction, value);
        }
    }
}
