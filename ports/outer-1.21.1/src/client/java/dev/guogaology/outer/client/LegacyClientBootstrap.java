package dev.guogaology.outer.client;
public final class LegacyClientBootstrap implements Runnable {
    @Override public void run(){new GuogaologyClient().onInitializeClient();}
}
