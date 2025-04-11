package at.qe.skeleton.configs;

import at.qe.skeleton.model.Limits;
import at.qe.skeleton.model.SensorType;

public class DefaultLimits {

    // Add a private constructor to hide the implicit public one (SonarQube rule java:S1118)
    private DefaultLimits() {
    }

    public static final Limits AIR_TEMPERATURE = Limits.builder()
            .lowerLimit(19)
            .upperLimit(24)
            .messageLower("Temperature is too low")
            .messageUpper("Temperature is too high")
            .sensorType(SensorType.AIR_TEMPERATURE)
            .build();

    public static final Limits AIR_HUMIDITY = Limits.builder()
            .lowerLimit(40)
            .upperLimit(60)
            .messageLower("Humidity is too low")
            .messageUpper("Humidity is too high")
            .sensorType(SensorType.AIR_HUMIDITY)
            .build();

    public static final Limits AIR_QUALITY = Limits.builder()
            .lowerLimit(0)
            .upperLimit(100)
            .messageLower("Air quality is too low")
            .messageUpper("Air quality is too high")
            .sensorType(SensorType.AIR_QUALITY)
            .build();

    public static final Limits LIGHT_INTENSITY = Limits.builder()
            .lowerLimit(0)
            .upperLimit(100)
            .messageLower("Light intensity is too low")
            .messageUpper("Light intensity is too high")
            .sensorType(SensorType.LIGHT_INTENSITY)
            .build();


    public static Limits getDefaultLimits(SensorType sensorType){
        switch (sensorType){
            case AIR_TEMPERATURE:
                return AIR_TEMPERATURE;
            case AIR_HUMIDITY:
                return AIR_HUMIDITY;
            case AIR_QUALITY:
                return AIR_QUALITY;
            case LIGHT_INTENSITY:
                return LIGHT_INTENSITY;
            default:
                return null;
        }
    }
}
