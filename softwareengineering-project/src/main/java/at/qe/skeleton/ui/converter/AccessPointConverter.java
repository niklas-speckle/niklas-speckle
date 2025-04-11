package at.qe.skeleton.ui.converter;

import at.qe.skeleton.model.AccessPoint;
import at.qe.skeleton.services.AccessPointService;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

@Component
@FacesConverter(value = "accessPointConverter")
public class AccessPointConverter implements Converter<AccessPoint> {

    // string which is shown when no access point is selected
    public static final String NO_ACCESS_POINT_STRING = "No Access Point";

    private AccessPointService accessPointService;

    public AccessPointService getAccessPointService() {
        if(accessPointService == null){
            WebApplicationContext context = FacesContextUtils.getWebApplicationContext(FacesContext.getCurrentInstance());

            if(context == null){
                throw new IllegalStateException("FacesContext not found");
            }

            accessPointService = context.getBean(AccessPointService.class);
        }
        return accessPointService;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, AccessPoint accessPoint) {
        if (accessPoint == null) {
            return NO_ACCESS_POINT_STRING;
        } else{

            if(accessPoint.getId() == null){
                throw new ConverterException("AccessPoint ID is null");
            }

            return accessPoint.getId().toString();
        }
    }

    @Override
    public AccessPoint getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        if (submittedValue.isBlank() || submittedValue.isEmpty() || submittedValue.equals(NO_ACCESS_POINT_STRING)) {
            return null;
        }

        try {
            AccessPoint accessPointFromDB = getAccessPointService().getAccessPointById(Long.parseLong(submittedValue));
            if (accessPointFromDB == null) {
                throw new ConverterException("AccessPoint with ID " + submittedValue + " not found");
            }
            return accessPointFromDB;
        } catch (NumberFormatException e) {
            throw new ConverterException(submittedValue + " is not a valid AccessPoint ID");
        }
    }
}