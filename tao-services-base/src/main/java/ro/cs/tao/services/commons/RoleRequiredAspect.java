package ro.cs.tao.services.commons;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.security.SystemPrincipal;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.user.Group;
import ro.cs.tao.user.User;
import ro.cs.tao.utils.StringUtilities;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.stream.Collectors;

@Aspect
@Component
public class RoleRequiredAspect {

    @Autowired
    private UserProvider userProvider;

    @Around("@annotation(ro.cs.tao.services.commons.RoleRequired)")
    public Object validate(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        final RoleRequired annotation = method.getAnnotation(RoleRequired.class);
        if (annotation != null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!(principal instanceof Principal)) {
                final User user = userProvider.get((String) principal);
                principal = new UserPrincipal(user.getId(),
                                              user.getGroups().stream().map(Group::getName).collect(Collectors.toSet()));
            }
            if (principal instanceof SystemPrincipal) {
                return pjp.proceed();
            } else if (principal instanceof UserPrincipal) {
                final String roles = annotation.roles();
                if (!StringUtilities.isNullOrEmpty(roles)) {
                    final String[] userRoles = roles.split(",");
                    for (String role : userRoles) {
                        if (((UserPrincipal) principal).hasRole(role)) {
                            return pjp.proceed();
                        }
                    }
                } else {
                    throw new AuthorizationServiceException("Admin role required");
                }
            }
            throw new AuthorizationServiceException("Admin role required");
        } else {
            return pjp.proceed();
        }
    }
}
