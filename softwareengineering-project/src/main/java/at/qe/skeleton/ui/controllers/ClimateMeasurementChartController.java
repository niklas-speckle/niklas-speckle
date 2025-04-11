package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.ClimateMeasurement;
import at.qe.skeleton.model.SensorType;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.climate.ClimateMeasurementService;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.ChartDataSet;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.optionconfig.title.Title;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller class for managing climate measurement charts.
 */

@Getter
@Setter
@Component
@Scope("view")
public class ClimateMeasurementChartController implements Serializable {

    @Autowired
    private transient ClimateMeasurementService climateMeasurementService;

    @Autowired
    private SessionInfoBean sessionInfoBean;


    private LineChartModel lineModel;

    private String dataType;

    private List<LocalDate> range;

    private int granularity;


    @PostConstruct
    private void prepareDefaultSettings() {
        dataType = "Air Temperature";
        range = List.of(LocalDate.now().minusDays(1), LocalDate.now());
        granularity = 15;
        createChart();
    }

    /**
     * Creates the chart based on the selected data type.
     */
    public void createChart() {
        // if not both start and end date of range have been selected, display an error message
        if (range.size() != 2) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please select start and end time."));
            return;
        }
        switch (dataType) {
            case "Air Humidity" -> createHumidityChart();
            case "Air Quality" -> createAirQualityChart();
            case "Light Intensity" -> createLightChart();
            default -> createTemperatureChart();
        }
    }

    public void createTemperatureChart() {
        lineModel = new LineChartModel();
        lineModel.setData(getChartData(SensorType.AIR_TEMPERATURE, range.get(0), range.get(1), granularity));
        lineModel.setOptions(getLineChartOptions(SensorType.AIR_TEMPERATURE));
    }

    public void createHumidityChart() {
        lineModel = new LineChartModel();
        lineModel.setData(getChartData(SensorType.AIR_HUMIDITY, range.get(0), range.get(1), granularity));
        lineModel.setOptions(getLineChartOptions(SensorType.AIR_HUMIDITY));
    }

    public void createAirQualityChart() {
        lineModel = new LineChartModel();
        lineModel.setData(getChartData(SensorType.AIR_QUALITY, range.get(0), range.get(1), granularity));
        lineModel.setOptions(getLineChartOptions(SensorType.AIR_QUALITY));
    }

    public void createLightChart() {
        lineModel = new LineChartModel();
        lineModel.setData(getChartData(SensorType.LIGHT_INTENSITY, range.get(0), range.get(1), granularity));
        lineModel.setOptions(getLineChartOptions(SensorType.LIGHT_INTENSITY));
    }

    /**
     * Creates ChartData for the current user based on a given sensor type, specified time range, and granularity.
     * @param sensorType The type of sensor for which to retrieve the data.
     * @param start The start date of the time range.
     * @param end The end date of the time range.
     * @param granularityInMinutes The granularity of the data in minutes.
     * @return The chart data.
     */
    private ChartData getChartData(SensorType sensorType, LocalDate start, LocalDate end, int granularityInMinutes) {
        ChartData data = new ChartData();
        Userx currentUser = sessionInfoBean.getCurrentUser();
        // Retrieve measurement history based on the selected sensor type, specified time range, and granularity
        List<ClimateMeasurement> measurements;
        switch (sensorType) {
            case AIR_HUMIDITY -> measurements = climateMeasurementService.humidityHistoryForUser(currentUser, start, end, granularityInMinutes);
            case AIR_QUALITY -> measurements = climateMeasurementService.airQualityHistoryForUser(currentUser, start, end, granularityInMinutes);
            case LIGHT_INTENSITY -> measurements = climateMeasurementService.lightHistoryForUser(currentUser, start, end, granularityInMinutes);
            default -> measurements = climateMeasurementService.temperatureHistoryForUser(currentUser, start, end, granularityInMinutes);
        }
        // If no measurements are found, display a warning message
        if (measurements.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "No measurements have been found for the selected period."));
        }

        data.addChartDataSet(createDataSet(measurements, sensorType));
        List<String> labels = getLabels(measurements);
        data.setLabels(labels);
        return data;
    }

    /**
     * Creates a data set containing the measured values based on the provided measurement list and sensor type.
     * @param measurements The list of climate measurements.
     * @param sensorType The type of sensor for which the data set is created.
     * @return The chart data set.
     */
    private ChartDataSet createDataSet(List<ClimateMeasurement> measurements, SensorType sensorType) {
        LineChartDataSet dataSet = new LineChartDataSet();
        List<Object> values = new ArrayList<>();
        for (ClimateMeasurement measurement : measurements) {
            values.add(measurement.getMeasuredValue());
        }
        dataSet.setData(values);
        dataSet.setFill(false);
        dataSet.setTension(0.1);
        switch (sensorType) {
            case AIR_HUMIDITY -> dataSet.setBorderColor("rgb(0, 191, 255)");    // Deep Sky Blue
            case AIR_QUALITY -> dataSet.setBorderColor("rgb(224, 255, 255)");  // Light Cyan
            case LIGHT_INTENSITY -> dataSet.setBorderColor("rgb(0, 128, 255)");    // Royal Blue
            default -> dataSet.setBorderColor("rgb(173, 216, 230)");  // Light Blue
        }
        return dataSet;
    }


    /**
     * Generates labels for the chart based on the timestamps of the provided measurements.
     * @param measurements The list of climate measurements.
     * @return The list of labels formatted as strings.
     */
    private List<String> getLabels(List<ClimateMeasurement> measurements) {
        List<String> labels = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
        for (ClimateMeasurement measurement : measurements) {
            labels.add(formatter.format(measurement.getTimeStamp()));
        }
        return labels;
    }


    /**
     * Generates line chart options based on the provided sensor type.
     * LineChartOptions include the Chart's title and legend.
     * @param sensorType The type of sensor for which the options are generated.
     * @return The line chart options.
     */
    private LineChartOptions getLineChartOptions(SensorType sensorType) {

        LineChartOptions options = new LineChartOptions();

        options.setMaintainAspectRatio(false);

        Legend legend = new Legend();
        legend.setDisplay(false);
        options.setLegend(legend);

        Title title = new Title();
        title.setDisplay(true);
        switch (sensorType) {
            case AIR_HUMIDITY -> title.setText("Humidity in %");
            case AIR_QUALITY -> title.setText("Air Quality in PPM");
            case LIGHT_INTENSITY -> title.setText("Light Intensity in Lux");
            default -> title.setText("Temperature in C°");
        }
        options.setTitle(title);

        return options;
    }
}
