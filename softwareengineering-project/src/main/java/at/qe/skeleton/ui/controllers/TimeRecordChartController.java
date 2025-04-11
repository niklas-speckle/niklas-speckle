package at.qe.skeleton.ui.controllers;

import at.qe.skeleton.model.Project;
import at.qe.skeleton.model.TimeRecord;
import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.model.WorkMode;
import at.qe.skeleton.services.timeTracking.TimeRecordService;
import at.qe.skeleton.services.timeTracking.ProjectService;
import at.qe.skeleton.services.timeTracking.WorkGroupService;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.optionconfig.legend.LegendLabel;
import org.primefaces.model.charts.optionconfig.title.Title;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Component
@Scope("view")
public class TimeRecordChartController implements Serializable {

    public static final String GROUP_LEADER_OVERVIEW = "/workgroups/timeRecordsGroupLeader.xhtml";
    public static final String MANAGER_OVERVIEW = "/projects/timeRecordsManager.xhtml";
    public static final String USER_TIME_RECORDS = "/secured/timeRecords.xhtml";
    public static final String NO_PROJECT = "no project assigned";
    public static final String NO_WORK_GROUP = "no work group assigned";
    public static final String WORK_GROUP = "Work Group";
    public static final String WORK_MODE = "Work Mode";
    @Autowired
    private SessionInfoBean sessionInfoBean;

    @Autowired
    private transient TimeRecordService timeRecordService;

    @Autowired
    private transient ProjectService projectService;

    @Autowired
    private transient WorkGroupService workGroupService;

    private BarChartModel barModel;

    private List<TimeRecord> timeRecords;

    private final String[] colors = {
            "rgb(0, 191, 255)",    // Deep Sky Blue
            "rgb(135, 206, 235)",  // Sky Blue
            "rgb(224, 255, 255)",  // Light Cyan
            "rgb(0, 128, 255)",    // Royal Blue
            "rgb(30, 144, 255)",   // Dodger Blue
            "rgb(173, 216, 230)",  // Light Blue
            "rgb(240, 248, 255)",  // Alice Blue
            "rgb(0, 0, 255)",      // Blue
    };

    private List<String> projectLabels;

    private List<String> workGroupLabels;

    private List<LocalDate> range;

    private String groupBy;


    @PostConstruct
    private void prepareDefaultSettings() {
        range = List.of(LocalDate.now().minusMonths(1), LocalDate.now());
        groupBy = WORK_MODE;
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        if (GROUP_LEADER_OVERVIEW.equals(viewId)) {
            initForGroupLeader();
        } else if (MANAGER_OVERVIEW.equals(viewId)) {
            initForManager();
        } else if (USER_TIME_RECORDS.equals(viewId)){
            initForEmployee();
        }
    }

    public void initForWelcomePage() {
        range = List.of(LocalDate.now(), LocalDate.now());
        timeRecords = timeRecordService.getTodaysTimeRecords(sessionInfoBean.getCurrentUser());
        List<Project> currentUsersProjects = projectService.getProjectsByUser(sessionInfoBean.getCurrentUser());
        generateProjectLabels(currentUsersProjects);
        List<WorkGroup> currentUsersWorkGroups = workGroupService.getWorkGroupsByUser(sessionInfoBean.getCurrentUser());
        generateWorkGroupLabels(currentUsersWorkGroups);
        createChart();
    }

    public void initForEmployee() {
        timeRecords = timeRecordService.getTimeRecordsForUser(sessionInfoBean.getCurrentUser());
        List<Project> currentUsersProjects = projectService.getProjectsByUser(sessionInfoBean.getCurrentUser());
        generateProjectLabels(currentUsersProjects);
        List<WorkGroup> currentUsersWorkGroups = workGroupService.getWorkGroupsByUser(sessionInfoBean.getCurrentUser());
        generateWorkGroupLabels(currentUsersWorkGroups);
        createChart();
    }

    public void initForGroupLeader() {
        timeRecords = timeRecordService.getTimeRecordsForGroupLeader(sessionInfoBean.getCurrentUser());
        List<WorkGroup> groupLeaderWorkGroups = workGroupService.getGroupsByGroupLeader(sessionInfoBean.getCurrentUser());
        generateWorkGroupLabels(groupLeaderWorkGroups);
        workGroupLabels.remove(NO_WORK_GROUP); // group leader only gets shown the time records that are associated with his groups
        List<Project> workGroupProjects = projectService.getProjectByWorkGroups(groupLeaderWorkGroups);
        generateProjectLabels(workGroupProjects);
        createChart();
    }

    public void initForManager() {
        timeRecords = timeRecordService.getTimeRecordsForManager(sessionInfoBean.getCurrentUser());
        List<Project> managerProjects = projectService.getProjectByManager(sessionInfoBean.getCurrentUser());
        generateProjectLabels(managerProjects);
        projectLabels.remove(NO_PROJECT); // manager only gets shown the time records that are associated with his projects
        List<WorkGroup> projectWorkGroups = workGroupService.getWorkGroupsByProjects(managerProjects);
        generateWorkGroupLabels(projectWorkGroups);
        createChart();
    }

    private void generateWorkGroupLabels(List<WorkGroup> currentUsersWorkGroups) {
        workGroupLabels = Stream.concat(
                        currentUsersWorkGroups.stream().map(WorkGroup::toString), // Work group names
                        Stream.of(NO_WORK_GROUP))
                .collect(Collectors.toList());
    }

    private void generateProjectLabels(List<Project> currentUsersProjects) {
        projectLabels = Stream.concat(
                        currentUsersProjects.stream().map(Project::toString), // Project names
                        Stream.of(NO_PROJECT))
                .collect(Collectors.toList());
    }


    public void createChart() {
        if (range.size() != 2) {
            FacesContext.getCurrentInstance().addMessage("chartPanel", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please select start and end time."));
            return;
        }
        switch (groupBy) {
            case WORK_GROUP -> createWorkGroupChart();
            case WORK_MODE -> createWorkModeChart();
            default -> createProjectsChart();
        }
    }

    private BarChartOptions generateOptions(String groupBy) {
        BarChartOptions options = new BarChartOptions();
        options.setMaintainAspectRatio(true);
        Title title = new Title();
        title.setDisplay(true);
        title.setText("Work Hours by " + groupBy);
        options.setTitle(title);

        // Legend is not displayed at the moment. Necessary if we display different datasets (e.g. 1 dataset per work group) which could be grouped by other attributes (e.g. work mode).
        Legend legend = new Legend();
        legend.setDisplay(false);
        legend.setPosition("top");
        LegendLabel legendLabels = new LegendLabel();
        legendLabels.setFontStyle("italic");
        legend.setLabels(legendLabels);
        options.setLegend(legend);

        return options;
    }

    private void createWorkModeChart() {
        barModel = new BarChartModel();
        barModel.setOptions(generateOptions(WORK_MODE));
        ChartData data = initializeForWorkMode();
        data.addChartDataSet(getDataSetForWorkMode());
        barModel.setData(data);
    }

    private ChartData initializeForWorkMode() {
        ChartData chartData = new ChartData();
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        // don't show out of office time records in the view for accumulative work hours for managers and groupleaders
        if (GROUP_LEADER_OVERVIEW.equals(viewId) || MANAGER_OVERVIEW.equals(viewId)) {
            chartData.setLabels(List.of("Available", "Meeting", "Deep Work"));
        } else {
            chartData.setLabels(List.of("Available", "Meeting", "Deep Work", "Out Of Office"));
        }
        return chartData;
    }

    private BarChartDataSet getDataSetForWorkMode() {
        BarChartDataSet dataSet = new BarChartDataSet();

        Map<WorkMode, Double> totalDurations = timeRecords
                .stream().filter(tr -> isInRange(tr, range))
                .collect(Collectors.groupingBy(
                        TimeRecord::getWorkMode,
                        Collectors.summingDouble(tr -> tr.getDuration() / 60.0)
                ));
        if (totalDurations.isEmpty()) {
            addNoTimeRecordsFoundMessage();
        }
        dataSet.setData(new ArrayList<>(List.of(totalDurations.getOrDefault(WorkMode.AVAILABLE, .0),
                totalDurations.getOrDefault(WorkMode.MEETING, .0),
                totalDurations.getOrDefault(WorkMode.DEEP_WORK, .0)
        )));
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        // don't show out of office time records in the view for accumulative work hours for managers and groupleaders
        if (!(GROUP_LEADER_OVERVIEW.equals(viewId) || MANAGER_OVERVIEW.equals(viewId))) {
            dataSet.getData().add(totalDurations.getOrDefault(WorkMode.OUT_OF_OFFICE, .0));
        }
        dataSet.setBackgroundColor(List.of("rgb(102, 255, 102)", "rgb(255, 205, 86)", "rgb(153, 102, 255)", "rgb(255, 99, 132)")); // green, yellow, purple, red
        return dataSet;
    }


    private void createProjectsChart() {

        barModel = new BarChartModel();
        barModel.setOptions(generateOptions("Project"));
        ChartData data = initializeForProjects();
        data.addChartDataSet(getDataSetForProjects());
        barModel.setData(data);
    }

    private ChartData initializeForProjects() {
        ChartData chartData = new ChartData();
        chartData.setLabels(projectLabels);
        return chartData;
    }

    private BarChartDataSet getDataSetForProjects() {
        BarChartDataSet dataSet = new BarChartDataSet();
        Map<String, Double> totalDurations = timeRecords.stream()
                .filter(tr -> isInRange(tr, range))
                .collect(Collectors.groupingBy(
                        tr -> tr.getWorkMode().equals(WorkMode.OUT_OF_OFFICE) ? NO_PROJECT :
                                Optional.ofNullable(tr.getProject()).map(Project::toString).orElse(NO_PROJECT),
                        Collectors.summingDouble(tr -> tr.getDuration() / 60.0)
                ));
        if (totalDurations.isEmpty()) {
            addNoTimeRecordsFoundMessage();
        }
        List<Number> durationData = new ArrayList<>();
        for (String projectLable : projectLabels) {
            durationData.add(totalDurations.get(projectLable));
        }
        dataSet.setData(durationData);
        dataSet.setBackgroundColor(generateBackgroundColors(projectLabels.toArray().length));
        return dataSet;
    }

    private static void addNoTimeRecordsFoundMessage() {
        FacesContext.getCurrentInstance().addMessage("chartPanel", new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "No timeRecords have been found for the selected period."));
    }


    private void createWorkGroupChart() {
        barModel = new BarChartModel();
        barModel.setOptions(generateOptions(WORK_GROUP));
        ChartData data = initializeForWorkGroup();
        data.addChartDataSet(getDataSetForWorkGroup());
        barModel.setData(data);
    }

    private ChartData initializeForWorkGroup() {
        ChartData chartData = new ChartData();
        chartData.setLabels(workGroupLabels);
        return chartData;
    }

    private BarChartDataSet getDataSetForWorkGroup() {
        BarChartDataSet dataSet = new BarChartDataSet();
        Map<String, Double> totalDurations = timeRecords
                .stream().filter(tr -> isInRange(tr, range))
                .collect(Collectors.groupingBy(
                        tr -> tr.getWorkMode().equals(WorkMode.OUT_OF_OFFICE) ? NO_WORK_GROUP :
                                Optional.ofNullable(tr.getWorkGroup()).map(WorkGroup::toString).orElse(NO_WORK_GROUP),
                        Collectors.summingDouble(tr -> tr.getDuration() / 60.0)
                ));
        if (totalDurations.isEmpty()) {
            addNoTimeRecordsFoundMessage();
        }
        List<Number> durationData = new ArrayList<>();
        for (String workGroup : workGroupLabels) {
            durationData.add(totalDurations.get(workGroup));
        }
        dataSet.setData(durationData);
        dataSet.setBackgroundColor(generateBackgroundColors(projectLabels.toArray().length));
        return dataSet;
    }

    private List<String> generateBackgroundColors(int amount) {
        List<String> backgroundColors = new ArrayList<>();
        int repetitions = amount / colors.length;
        int remainder = amount % colors.length;
        // Repeat colors if needed amount of background colors is higher than the number of available unique colors
        for (int i = 0; i < repetitions; i++) {
            backgroundColors.addAll(Arrays.asList(colors));
        }
        // Add remaining colors
        backgroundColors.addAll(Arrays.asList(colors).subList(0, remainder));
        return backgroundColors;
    }


    private boolean isInRange(TimeRecord timeRecord, List<LocalDate> range) {
        // the current time record doesn't have an endTime yet. To check if it is in range, use now() as endTime.
        if (timeRecord.getEndTime() != null) {
            return (!timeRecord.getStartTime().toLocalDate().isBefore(range.get(0)))
                    && (!timeRecord.getEndTime().toLocalDate().isAfter(range.get(range.size() - 1)));
        } else {
            return (!timeRecord.getStartTime().toLocalDate().isBefore(range.get(0)))
                    && (!LocalDate.now().isAfter(range.get(range.size() - 1)));
        }
    }


}
