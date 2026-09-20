package net.tfminecraft.thievery.door;

public enum LockState {
    PRIVATE,
    GUILD,
    PUBLIC;

    public static final LockState DEFAULT = PUBLIC;

    public LockState next() {
        LockState[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
