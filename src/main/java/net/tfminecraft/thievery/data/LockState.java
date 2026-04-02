package net.tfminecraft.thievery.data;

public enum LockState {
    PRIVATE,
    GUILD,
    PUBLIC;

    public LockState next() {
        LockState[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
