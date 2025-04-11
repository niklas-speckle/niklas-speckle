package at.qe.skeleton.utils;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A custom annotation to mark fields within a spring-managed bean which need to
 * be initialized by CDI.
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 *
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface CDIAutowired {

}
