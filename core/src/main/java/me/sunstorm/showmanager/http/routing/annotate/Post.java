package me.sunstorm.showmanager.http.routing.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Post {
    /**
     * @return the post request path to be handled
     */
    String value();
}
