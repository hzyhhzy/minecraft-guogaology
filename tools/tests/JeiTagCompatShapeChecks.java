import dev.guogaology.client.compat.jei.JeiTagCompatShape;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.jar.JarFile;

/** Pure ASM checks: Minecraft and JEI classes are deliberately absent from the runtime classpath. */
public final class JeiTagCompatShapeChecks implements Opcodes {
    private static final String OWNER = JeiTagCompatShape.TARGET.replace('.', '/');
    private static final String REG = "net/minecraft/core/RegistryAccess";
    private static final String FROZEN = "Lnet/minecraft/core/RegistryAccess$Frozen;";
    private static final String LIST = "Ljava/util/List;";
    private static final String LOAD = "(Lnet/minecraft/server/packs/resources/ResourceManager;L" + REG + ";)" + LIST;
    private static int checks;

    public static void main(String[] args) throws Exception {
        accepted(synthetic(), "future JEI version 99.99.99 with compatible flow");
        accepted(edit(node -> method(node).instructions.insert(new InsnNode(NOP))), "harmless instruction change");
        rejected("wrong target class", node -> node.name += "Other");
        rejected("overloaded fallback", node -> node.methods.add(new MethodNode(ACC_STATIC, "loadVanillaRecipeRegistry", "()V", null, null)));
        rejected("changed descriptor", node -> method(node).desc = "(Ljava/lang/Object;)Ljava/lang/Object;");
        rejected("first load uses unfrozen active registry", node -> previousVariable(tagLoads(node).getFirst()).var = 0);
        rejected("first load uses a different registry", node -> {
            MethodInsnNode freeze = calls(node, REG, "freeze").getFirst();
            method(node).instructions.set(freeze.getPrevious(), new FieldInsnNode(GETSTATIC, OWNER, "otherRegistry", "L" + REG + ";"));
        });
        rejected("second load targets active registry", node -> previousVariable(tagLoads(node).getLast()).var = 2);
        rejected("wrong WORLD registry catalog", node -> {
            for (AbstractInsnNode insn : method(node).instructions) {
                if (insn instanceof FieldInsnNode field && field.name.equals("WORLD_REGISTRIES")) field.name = "RELOADABLE_REGISTRIES";
            }
        });
        rejected("different resource managers", node -> {
            MethodInsnNode call = tagLoads(node).getLast();
            ((VarInsnNode) call.getPrevious().getPrevious()).var = 7;
        });
        rejected("extra tag load", node -> {
            InsnList more = new InsnList();
            more.add(new VarInsnNode(ALOAD, 1)); more.add(new VarInsnNode(ALOAD, 2));
            more.add(new MethodInsnNode(INVOKESTATIC, "net/minecraft/tags/TagLoader", "loadTagsForExistingRegistries", LOAD, false));
            more.add(new InsnNode(POP)); method(node).instructions.insertBefore(returnPoint(node), more);
        });
        rejected("base lookup uses world pending list", node -> {
            MethodInsnNode call = calls(node, "net/minecraft/tags/TagLoader", "buildUpdatedLookups").getFirst();
            previousVariable(call).var = 7;
        });
        rejected("apply loop uses another pending list", node -> {
            MethodInsnNode call = calls(node, "java/util/List", "forEach").getFirst();
            ((VarInsnNode) call.getPrevious().getPrevious()).var = 6;
        });
        rejected("apply consumer changed", node -> {
            for (AbstractInsnNode insn : method(node).instructions) {
                if (insn instanceof InvokeDynamicInsnNode dynamic) {
                    dynamic.bsmArgs[1] = new Handle(H_INVOKEINTERFACE, "java/lang/Runnable", "run", "()V", true);
                    break;
                }
            }
        });
        rejected("additional pending size consumer", node -> escape(node, instructions(
            new VarInsnNode(ALOAD, 3), new MethodInsnNode(INVOKEINTERFACE, "java/util/List", "size", "()I", true), new InsnNode(POP))));
        rejected("pending list escapes to static field", node -> escape(node, instructions(
            new VarInsnNode(ALOAD, 3), new FieldInsnNode(PUTSTATIC, OWNER, "saved", LIST))));
        rejected("pending list escapes through instance field", node -> escape(node, instructions(
            new InsnNode(ACONST_NULL), new VarInsnNode(ALOAD, 3), new FieldInsnNode(PUTFIELD, OWNER, "savedInstance", LIST))));
        rejected("pending list escapes through array", node -> escape(node, instructions(
            new InsnNode(ICONST_1), new TypeInsnNode(ANEWARRAY, "java/lang/Object"), new InsnNode(DUP),
            new InsnNode(ICONST_0), new VarInsnNode(ALOAD, 3), new InsnNode(AASTORE), new InsnNode(POP))));
        rejected("pending list captured by lambda", node -> {
            Handle bootstrap = metafactory();
            InvokeDynamicInsnNode capture = new InvokeDynamicInsnNode("get", "(" + LIST + ")Ljava/util/function/Supplier;", bootstrap,
                Type.getType("()Ljava/lang/Object;"), new Handle(H_INVOKESTATIC, OWNER, "capture", "(" + LIST + ")Ljava/lang/Object;", false), Type.getType("()Ljava/lang/Object;"));
            escape(node, instructions(new VarInsnNode(ALOAD, 3), capture, new InsnNode(POP)));
        });
        rejected("pending list requires a concrete subtype", node -> escape(node, instructions(
            new VarInsnNode(ALOAD, 3), new TypeInsnNode(CHECKCAST, "java/util/ArrayList"), new VarInsnNode(ASTORE, 3))));
        accepted(edit(node -> escape(node, instructions(new VarInsnNode(ALOAD, 3),
            new TypeInsnNode(CHECKCAST, "java/util/List"), new VarInsnNode(ASTORE, 3)))), "interface cast remains safe");
        rejected("merged pending list escapes", node -> {
            LabelNode other = new LabelNode(), merged = new LabelNode();
            escape(node, instructions(new InsnNode(ICONST_0), new JumpInsnNode(IFEQ, other), new VarInsnNode(ALOAD, 3),
                new JumpInsnNode(GOTO, merged), other, new InsnNode(ACONST_NULL), merged, new FieldInsnNode(PUTSTATIC, OWNER, "saved", LIST)));
        });
        rejected("pending list controls a branch", node -> {
            LabelNode next = new LabelNode();
            escape(node, instructions(new VarInsnNode(ALOAD, 3), new JumpInsnNode(IFNULL, next), new InsnNode(NOP), next));
        });
        for (String file : args) {
            try (JarFile jar = new JarFile(Path.of(file).toFile())) {
                ClassNode node = new ClassNode();
                new ClassReader(jar.getInputStream(jar.getJarEntry(OWNER + ".class"))).accept(node, 0);
                accepted(node, Path.of(file).getFileName().toString());
            }
        }
        System.out.println("JEI_SHAPE_OK checks=" + checks + " realJars=" + args.length);
    }

    private static void accepted(ClassNode node, String label) {
        String reason = JeiTagCompatShape.incompatibility(node);
        if (reason != null) throw new AssertionError(label + ": " + reason);
        checks++; System.out.println("ACCEPT " + label);
    }

    private static void rejected(String label, Consumer<ClassNode> mutation) {
        ClassNode node = edit(mutation);
        String reason = JeiTagCompatShape.incompatibility(node);
        if (reason == null) throw new AssertionError("Unsafe shape accepted: " + label);
        checks++; System.out.println("REJECT " + label + ": " + reason);
    }

    private static ClassNode edit(Consumer<ClassNode> mutation) {
        ClassNode node = synthetic(); mutation.accept(node); return node;
    }

    private static MethodNode method(ClassNode node) { return node.methods.getFirst(); }
    private static AbstractInsnNode returnPoint(ClassNode node) { return method(node).instructions.getLast().getPrevious(); }
    private static VarInsnNode previousVariable(MethodInsnNode call) { return (VarInsnNode) call.getPrevious(); }
    private static void escape(ClassNode node, InsnList instructions) { method(node).instructions.insertBefore(returnPoint(node), instructions); }
    private static InsnList instructions(AbstractInsnNode... nodes) { InsnList result = new InsnList(); for (AbstractInsnNode node : nodes) result.add(node); return result; }
    private static List<MethodInsnNode> calls(ClassNode node, String owner, String name) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode insn : method(node).instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) result.add(call);
        }
        return result;
    }
    private static List<MethodInsnNode> tagLoads(ClassNode node) { return calls(node, "net/minecraft/tags/TagLoader", "loadTagsForExistingRegistries"); }
    private static Handle metafactory() {
        return new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
    }
    private static void apply(InsnList code, int local) {
        code.add(new VarInsnNode(ALOAD, local));
        code.add(new InvokeDynamicInsnNode("accept", "()Ljava/util/function/Consumer;", metafactory(), Type.getType("(Ljava/lang/Object;)V"),
            new Handle(H_INVOKEINTERFACE, "net/minecraft/core/Registry$PendingTags", "apply", "()V", true), Type.getType("(Lnet/minecraft/core/Registry$PendingTags;)V")));
        code.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/List", "forEach", "(Ljava/util/function/Consumer;)V", true));
    }
    private static ClassNode synthetic() {
        ClassNode node = new ClassNode(); node.version = V25; node.access = ACC_PUBLIC; node.name = OWNER; node.superName = "java/lang/Object";
        node.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "VERSION", "Ljava/lang/String;", null, "99.99.99"));
        MethodNode method = new MethodNode(ACC_PRIVATE | ACC_STATIC, "loadVanillaRecipeRegistry", "(L" + REG + ";)Lnet/minecraft/world/item/crafting/RecipeMap;", null, null);
        node.methods.add(method); InsnList code = method.instructions;
        code.add(instructions(new TypeInsnNode(NEW, "net/minecraft/server/packs/resources/MultiPackResourceManager"), new InsnNode(DUP),
            new InsnNode(ACONST_NULL), new InsnNode(ACONST_NULL), new MethodInsnNode(INVOKESPECIAL, "net/minecraft/server/packs/resources/MultiPackResourceManager", "<init>", "(Lnet/minecraft/server/packs/PackType;Ljava/util/List;)V", false), new VarInsnNode(ASTORE, 1),
            new VarInsnNode(ALOAD, 0), new MethodInsnNode(INVOKEINTERFACE, REG, "freeze", "()" + FROZEN, true), new VarInsnNode(ASTORE, 2),
            new VarInsnNode(ALOAD, 1), new VarInsnNode(ALOAD, 2), new MethodInsnNode(INVOKESTATIC, "net/minecraft/tags/TagLoader", "loadTagsForExistingRegistries", LOAD, false), new VarInsnNode(ASTORE, 3),
            new VarInsnNode(ALOAD, 2), new VarInsnNode(ALOAD, 3), new MethodInsnNode(INVOKESTATIC, "net/minecraft/tags/TagLoader", "buildUpdatedLookups", "(" + FROZEN + LIST + ")" + LIST, false), new VarInsnNode(ASTORE, 4),
            new VarInsnNode(ALOAD, 1), new VarInsnNode(ALOAD, 4), new FieldInsnNode(GETSTATIC, "net/minecraft/resources/RegistryDataLoader", "WORLD_REGISTRIES", LIST), new InsnNode(ACONST_NULL),
            new MethodInsnNode(INVOKESTATIC, "net/minecraft/resources/RegistryDataLoader", "load", "(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", false),
            new MethodInsnNode(INVOKEVIRTUAL, "java/util/concurrent/CompletableFuture", "join", "()Ljava/lang/Object;", false), new TypeInsnNode(CHECKCAST, "net/minecraft/core/RegistryAccess$Frozen"), new VarInsnNode(ASTORE, 5),
            new VarInsnNode(ALOAD, 1), new VarInsnNode(ALOAD, 5), new MethodInsnNode(INVOKESTATIC, "net/minecraft/tags/TagLoader", "loadTagsForExistingRegistries", LOAD, false), new VarInsnNode(ASTORE, 6),
            new VarInsnNode(ALOAD, 5), new VarInsnNode(ALOAD, 6), new MethodInsnNode(INVOKESTATIC, "net/minecraft/tags/TagLoader", "buildUpdatedLookups", "(" + FROZEN + LIST + ")" + LIST, false), new InsnNode(POP),
            new InsnNode(ACONST_NULL), new VarInsnNode(ASTORE, 7)));
        apply(code, 3); apply(code, 6); code.add(new InsnNode(ACONST_NULL)); code.add(new InsnNode(ARETURN));
        method.maxStack = 16; method.maxLocals = 8; return node;
    }
}
