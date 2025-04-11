package at.qe.skeleton.model;

import lombok.Getter;

/**
 * Enumeration of available sensor types.
 */
@Getter
public enum SensorType {
    AIR_TEMPERATURE ("Air Temperature"),
    AIR_HUMIDITY ("Air Humidity"),
    AIR_QUALITY ("Air Quality"),
    LIGHT_INTENSITY ("Light Intensity");

    private final String name;

    SensorType(String name) {
        this.name = name;
    }

    @Override
    public String toString(){
        return name;
    }
}
