package at.qe.skeleton.ui.controllers;


import at.qe.skeleton.model.TimeRecord;
import at.qe.skeleton.model.WorkMode;
import at.qe.skeleton.services.timeTracking.TimeRecordService;
import at.qe.skeleton.ui.beans.SessionInfoBean;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.LazyScheduleModel;
import org.primefaces.model.ScheduleModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@Scope("view")
public class TimeRecordScheduleController implements Serializable {

    @Autowired
    private transient TimeRecordService timeRecordService;

    @Autowired
    private SessionInfoBean sessionInfoBean;




    private transient ScheduleModel model;

    private LocalDate initialDate = LocalDate.now();

    private List<TimeRecord> allTimeRecords;

    private Map<DefaultScheduleEvent<TimeRecord>, TimeRecord> eventTimeRecordMapping; // Mapping between ScheduleEvent and TimeRecord


    @PostConstruct
    public void init() {
        allTimeRecords = timeRecordService.getTimeRecordsForUser(sessionInfoBean.getCurrentUser());
        initializeScheduleModel();
    }

    public void initializeScheduleModel() {
        eventTimeRecordMapping = new HashMap<>();
        model = new LazyScheduleModel() {
            @Override
            public void loadEvents(LocalDateTime beginningOfWeek, LocalDateTime endOfWeek) {
                for (TimeRecord timeRecord : allTimeRecords) {
                        if (timeRecord.getStartTime().isAfter(beginningOfWeek) && (timeRecord.getEndTime()==null || timeRecord.getEndTime().isBefore(endOfWeek))) {
                            DefaultScheduleEvent<TimeRecord> event = createScheduleEventForTimeRecord(timeRecord);
                            model.addEvent(event);
                    }
                }
            }
        };
    }

    private DefaultScheduleEvent<TimeRecord> createScheduleEventForTimeRecord(TimeRecord timeRecord) {
        DefaultScheduleEvent<TimeRecord> event = new DefaultScheduleEvent<>();
        event.setTitle(timeRecord.getWorkMode().toString());
        if ((timeRecord.getWorkMode() != WorkMode.OUT_OF_OFFICE) && (timeRecord.getProject() != null)) {
            event.setTitle(timeRecord.getWorkMode().toString() + " (" + timeRecord.getProject().toString() + ")");
        } else {
            event.setTitle(timeRecord.getWorkMode().toString());
        }
        event.setStartDate(timeRecord.getStartTime());
        if (timeRecord.getEndTime() != null) {
            event.setEndDate(timeRecord.getEndTime());
        } else {
            event.setEndDate(LocalDateTime.now());
        }
        event.setData(timeRecord);

        event.setStyleClass(getStyleClassForWorkMode(timeRecord.getWorkMode()));
        return event;
    }


    // overwriting the colour doesn't work (yet)
    private String getStyleClassForWorkMode(WorkMode workMode) {
        switch (workMode) {
            case AVAILABLE:
                return ".my-event-AVAILABLE";
            case MEETING:
                return ".my-event-MEETING";
            case DEEP_WORK:
                return ".my-event-DEEP_WORK";
            case OUT_OF_OFFICE:
                return ".my-event-OUT_OF_OFFICE";
            default:
                return ""; // Default style if work mode is unknown
        }
    }





}
