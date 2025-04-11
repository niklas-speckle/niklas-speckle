package at.qe.skeleton.model;

import lombok.Getter;

/**
 * Enumeration of available device types. Used for id formatting and notifications.
 */
public enum DeviceType {
    TEMPERA_DEVICE("TD"),
    ACCESS_POINT("AP"),
    SERVER("Server");

    @Getter
    private final String abbreviatedString;

    DeviceType(String abbreviatedString) {
        this.abbreviatedString = abbreviatedString;
    }

    public static DeviceType fromValue(String abbreviatedString) {
        for (DeviceType type : DeviceType.values()) {
            if (type.getAbbreviatedString().equals(abbreviatedString)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid value for DeviceType: " + abbreviatedString);
    }
}
