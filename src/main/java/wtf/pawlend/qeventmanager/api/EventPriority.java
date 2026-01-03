package wtf.pawlend.qeventmanager.api;

public enum EventPriority {
    LOWEST(1),
    LOW(2),
    NORMAL(5),
    HIGH(8),
    HIGHEST(10);

    private final int weight;

    EventPriority(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}