package dev.googology.outer;
/** A Java-only entrypoint keeps the Yarn host and Mojang-mapped bridge independent. */
public final class LegacyBootstrap implements Runnable {
    @Override public void run(){GoogologyMod.initialize();}
}
