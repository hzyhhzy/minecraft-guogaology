package dev.googology.outer.client;
public final class LegacyClientBootstrap implements Runnable {
    @Override public void run(){new GoogologyClient().onInitializeClient();}
}
