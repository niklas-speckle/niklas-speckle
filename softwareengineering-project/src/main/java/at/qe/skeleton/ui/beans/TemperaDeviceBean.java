package at.qe.skeleton.ui.beans;


import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.model.Userx;
import lombok.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Scope("view")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TemperaDeviceBean implements Serializable {

    private TemperaDevice temperaDevice;

    private Userx user;

    private AccessPoint accessPoint;

}
