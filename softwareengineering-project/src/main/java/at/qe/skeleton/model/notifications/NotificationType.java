package at.qe.skeleton.model.notifications;

import lombok.Getter;

public enum NotificationType {
    INFO(1),
    WARNING(2),
    ERROR(3);

    @Getter
    private final int value;

    NotificationType(int value) {
        this.value = value;
    }

    public static NotificationType fromValue(int value) {
        for (NotificationType type : NotificationType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid value for NotificationType: " + value);
    }
}
