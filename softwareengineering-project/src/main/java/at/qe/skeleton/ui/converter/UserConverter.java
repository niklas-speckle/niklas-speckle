package at.qe.skeleton.ui.converter;

import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.UserService;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;

@Component
@FacesConverter(value = "userConverter")
public class UserConverter implements Converter<Userx> {

    // string which is shown when no user is selected
    public static final String NO_USER_STRING = "No User";

    private UserService userService;

    public UserService getUserService() {
        if(userService == null){
            WebApplicationContext context = FacesContextUtils.getWebApplicationContext(FacesContext.getCurrentInstance());

            if(context == null){
                throw new IllegalStateException("FacesContext not found");
            }

            userService = context.getBean(UserService.class);
        }
        return userService;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Userx user) {
        if (user == null) {
            return NO_USER_STRING;
        } else{
            return user.getUsername();
        }
    }

    @Override
    public Userx getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        if (submittedValue == null || submittedValue.isEmpty() || submittedValue.isBlank() || submittedValue.equals(NO_USER_STRING)) {
            return null;
        }

        try {
            return getUserService().loadUser(submittedValue);
        } catch (NumberFormatException e) {
            throw new ConverterException(submittedValue + " is not a valid User ID");
        }
    }
}