package dev.guogaology.mining;
/** Implemented on vanilla arrows, including tipped and spectral arrows. */
public interface ArrowShotAccess {
    BowShotData guogaology$shot();
    void guogaology$shot(BowShotData shot);
    void guogaology$setPierceLevel(byte level);
}
