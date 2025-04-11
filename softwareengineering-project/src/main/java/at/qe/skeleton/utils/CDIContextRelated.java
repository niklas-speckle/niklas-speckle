package at.qe.skeleton.utils;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A custom annotation to indicate that a spring-managed bean contains fields
 * which need to be initialized by CDI. See {@link CDIAutowired}
 *
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by the University of Innsbruck.
 *
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface CDIContextRelated {

}
