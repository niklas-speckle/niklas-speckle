package at.qe.skeleton.rest.mapper;

import at.qe.skeleton.model.TimeRecord;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.model.WorkMode;
import at.qe.skeleton.rest.dto.TimeRecordDTO;
import at.qe.skeleton.services.TemperaDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TimeRecordMapper {
    @Autowired
    private TemperaDeviceService temperaDeviceService;

    /**
     * maps a given TimeRecordDTO to a TimeRecord.
     * The transmitted timestamp is used as the start time of the TimeRecord; the WorkMode and User is set according to
     * the transmitted values. The Project is set to the default project of the User. The id is not adopt.
     * @param dto TimeRecordDTO the TimeRecordDTO to be mapped
     * @return the mapped TimeRecord
     */

    public TimeRecord mapFrom(TimeRecordDTO dto) {
        if (dto == null) {
            return null;
        }

        TimeRecord timeRecord = new TimeRecord();

        timeRecord.setStartTime(dto.timestamp());
        timeRecord.setWorkMode(WorkMode.fromString(dto.workMode()));

        Userx user = temperaDeviceService.findUserOfTemperaDevice(dto.temperaDeviceId());
        timeRecord.setUser(user);
        timeRecord.setProject(user.getDefaultProject());
        return timeRecord;
    }

}
