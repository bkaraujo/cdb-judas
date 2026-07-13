package br.commons.framework.message;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.*;

@NullMarked
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageListener {
}
