package at.qe.skeleton.rest.mapper;

import at.qe.skeleton.model.LogTemperaDevice;
import at.qe.skeleton.rest.dto.LogTemperaDeviceDTO;
import org.springframework.stereotype.Service;


@Service
public class LogTemperaDeviceDTOMapper {
    /**
     * maps a given LogTemperaDevice to a new LogTemperaDeviceDTO.
     * Timestamp, LogStatus, TemperaDeviceId and NewStatus are adopted.
     * @param entity that should be mapped
     * @return the mapped LogTemperaDeviceDTO
     */

    public LogTemperaDeviceDTO mapTo(LogTemperaDevice entity) {
        if (entity == null) {
            return null;
        }
        return new LogTemperaDeviceDTO(
                entity.getTimestamp(),
                entity.getLogStatus().toString(),
                entity.getTemperaDeviceId(),
                entity.getNewStatus() == null ? null : entity.getNewStatus().toString());
    }
}
