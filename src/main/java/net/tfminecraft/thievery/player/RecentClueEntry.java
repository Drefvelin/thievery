package net.tfminecraft.thievery.player;

public class RecentClueEntry {

    private String text;
    private long usedAtMs;
    private String targetKey;

    public RecentClueEntry() {}

    public RecentClueEntry(String text, long usedAtMs, String targetKey) {
        this.text = text;
        this.usedAtMs = usedAtMs;
        this.targetKey = targetKey;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getUsedAtMs() {
        return usedAtMs;
    }

    public void setUsedAtMs(long usedAtMs) {
        this.usedAtMs = usedAtMs;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public void setTargetKey(String targetKey) {
        this.targetKey = targetKey;
    }
}
