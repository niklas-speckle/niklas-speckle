package at.qe.skeleton.auditing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLifecycleListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger generalLogger = LoggerFactory.getLogger("generalLogger");
    private static final Logger errorLogger = LoggerFactory.getLogger("errorLogger");

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            generalLogger.info("Application Lifecycle Event: Application context refreshed");
        } else if (event instanceof ContextStartedEvent) {
            generalLogger.info("Application Lifecycle Event: Application context started");
        } else if (event instanceof ContextStoppedEvent) {
            generalLogger.info("Application Lifecycle Event: Application context stopped");
        } else if (event instanceof ContextClosedEvent) {
            generalLogger.info("Application Lifecycle Event: Application context closed");
        } else if (event instanceof ApplicationFailedEvent applicationFailedEvent) {
            generalLogger.info("Application Lifecycle Event: Application context failed", applicationFailedEvent.getException());
            errorLogger.error("Application Lifecycle Event: Application context failed", applicationFailedEvent.getException());
        }
    }
}
