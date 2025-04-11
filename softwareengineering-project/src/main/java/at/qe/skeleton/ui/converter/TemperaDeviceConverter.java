package at.qe.skeleton.ui.converter;

import at.qe.skeleton.model.TemperaDevice;
import at.qe.skeleton.services.TemperaDeviceService;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

@Component
@FacesConverter(value = "temperaDeviceConverter")
public class TemperaDeviceConverter implements Converter<TemperaDevice> {

    // string which is shown when no tempera device is selected
    public static final String NO_TEMPERA_DEVICE_STRING = "No Tempera Device";

    private TemperaDeviceService temperaDeviceService;

    public TemperaDeviceService getTemperaDeviceService() {
        if(temperaDeviceService == null){
            WebApplicationContext context = FacesContextUtils.getWebApplicationContext(FacesContext.getCurrentInstance());

            if(context == null){
                throw new IllegalStateException("FacesContext not found");
            }

            temperaDeviceService = context.getBean(TemperaDeviceService.class);
        }
        return temperaDeviceService;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, TemperaDevice temperaDevice) {
        if (temperaDevice == null) {
            return NO_TEMPERA_DEVICE_STRING;
        } else{

            if(temperaDevice.getId() == null){
                throw new ConverterException("TemperaDevice ID is null");
            }

            return temperaDevice.getId().toString();
        }
    }

    @Override
    public TemperaDevice getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        if (submittedValue == null || submittedValue.isEmpty() || submittedValue.isBlank() || submittedValue.equals(NO_TEMPERA_DEVICE_STRING)){
            return null;
        }
        try {
            return getTemperaDeviceService().findTemperaDeviceById(Integer.parseInt(submittedValue));
        } catch (NumberFormatException e) {
            throw new ConverterException(submittedValue + " is not a valid TemperaDevice ID");
        }
    }
}