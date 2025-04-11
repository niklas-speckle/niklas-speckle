package at.qe.skeleton.ui.converter;

import at.qe.skeleton.model.WorkGroup;
import at.qe.skeleton.services.timeTracking.WorkGroupService;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

import java.util.Optional;

@Component
@FacesConverter(value = "workGroupConverter")
public class WorkGroupConverter implements Converter<WorkGroup> {


    // string which is shown when no access point is selected
    public static final String NO_WORK_GROUP_STRING = "No Work Group";


    private WorkGroupService workGroupService;


    public WorkGroupService getWorkGroupService() {
        if (workGroupService == null) {
            WebApplicationContext context = FacesContextUtils.getWebApplicationContext(FacesContext.getCurrentInstance());

            if (context == null) {
                throw new IllegalStateException("FacesContext not found");
            }

            workGroupService = context.getBean(WorkGroupService.class);
        }
        return workGroupService;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, WorkGroup workGroup) {
        if (workGroup == null) {
            return NO_WORK_GROUP_STRING;
        } else {
            return workGroup.getName();
        }
    }

    @Override
    public WorkGroup getAsObject(FacesContext context, UIComponent component, String submittedValue) {

        if (submittedValue == null || submittedValue.isEmpty() || submittedValue.isBlank() || submittedValue.equals(NO_WORK_GROUP_STRING)) {
            return null;
        }

        Optional<WorkGroup> workGroup = getWorkGroupService().getGroupByName(submittedValue);
        if (workGroup.isPresent()) {
            return workGroup.get();
        } else {
            throw new ConverterException(submittedValue + " is not a valid WorkGroup name");
        }
    }

}
