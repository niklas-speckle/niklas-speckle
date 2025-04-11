package at.qe.skeleton.model;

/**
 * Enumeration of available work modes
 */
public enum WorkMode {

    AVAILABLE("available", true, "#00ff00"),
    MEETING("meeting", false, "#0000ff"),
    DEEP_WORK("deep work", true, "#ff0000"),
    OUT_OF_OFFICE("out of office", false, "#808080");

    private final String name;
    private final boolean isInRoom;
    private final String colour;

    WorkMode(String name, boolean isInRoom, String colour) {
        this.name = name;
        this.isInRoom = isInRoom;
        this.colour = colour;
    }
    public String getName() {
        return name;
    }

    public boolean isInRoom() {
        return isInRoom;
    }

    public String getColour() {
        return colour;
    }

    public static WorkMode fromString(String name) {
        for (WorkMode workMode : WorkMode.values()) {
            if (workMode.name().equalsIgnoreCase(name)) {
                return workMode;
            }
        }
        return null;
    }
}
