package at.qe.skeleton.ui.converter;

import at.qe.skeleton.model.Project;
import at.qe.skeleton.services.timeTracking.ProjectService;
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
@FacesConverter(value = "projectConverter")
public class ProjectConverter implements Converter<Project> {


    // string which is shown when no project is selected
    public static final String NO_PROJECT_STRING = "No Project";


    private ProjectService projectService;


    public ProjectService getProjectService() {
        if (projectService == null) {
            WebApplicationContext context = FacesContextUtils.getWebApplicationContext(FacesContext.getCurrentInstance());

            if (context == null) {
                throw new IllegalStateException("FacesContext not found");
            }

            projectService = context.getBean(ProjectService.class);
        }
        return projectService;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Project project) {
        if (project == null) {
            return NO_PROJECT_STRING;
        } else {
            return project.getName();
        }
    }

    @Override
    public Project getAsObject(FacesContext context, UIComponent component, String submittedValue) {

        if (submittedValue == null || submittedValue.isEmpty() || submittedValue.isBlank() || submittedValue.equals(NO_PROJECT_STRING)) {
            return null;
        }

        Optional<Project> project = getProjectService().getProjectByName(submittedValue);
        if (project.isPresent()) {
            return project.get();
        } else {
            throw new ConverterException(submittedValue + " is not a valid Project name");
        }
    }

}
