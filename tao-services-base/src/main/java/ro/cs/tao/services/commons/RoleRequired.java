package ro.cs.tao.services.commons;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to specify required roles for accessing a method.
 * This annotation can be applied to methods to enforce role-based access control.
 * The annotated method will require the specified roles to be present
 * in the security context to allow execution.
 *
 * The `roles` element defines the list of roles that are required.
 * These roles should be comma-separated strings.
 *
 * The annotation is retained at runtime and can be used by frameworks or tools
 * to enforce security policies during method invocation.
 *
 * This annotation is intended to be used in conjunction with security
 * configurations and checks to ensure that appropriate access control mechanisms
 * are applied.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RoleRequired {
    String roles();
}
