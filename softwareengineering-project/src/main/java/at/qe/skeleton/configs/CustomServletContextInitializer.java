package at.qe.skeleton.configs;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for servlet context.
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by Innsbruck University.
 */
@Configuration
public class CustomServletContextInitializer implements ServletContextInitializer {

    @Override
    public void onStartup(ServletContext sc) throws ServletException {
        sc.setInitParameter("jakarta.faces.PROJECT_STAGE", "Development");
        sc.setInitParameter("jakarta.faces.STATE_SAVING_METHOD", "server");
        sc.setInitParameter("jakarta.faces.FACELETS_SKIP_COMMENTS", "true");
        sc.setInitParameter("jakarta.faces.DEFAULT_SUFFIX", ".xhtml");

        //theme
        sc.setInitParameter("primefaces.THEME", "rain-cyan-light");
        sc.setInitParameter("javax.faces.FACELETS_LIBRARIES", "/WEB-INF/primefaces-rain.taglib.xml");
    }
}