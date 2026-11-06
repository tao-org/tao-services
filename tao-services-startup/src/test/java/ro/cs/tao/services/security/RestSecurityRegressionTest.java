package ro.cs.tao.services.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.WebSocketSession;
import ro.cs.tao.docker.ContainerInstance;
import ro.cs.tao.persistence.ContainerInstanceProvider;
import ro.cs.tao.persistence.PersistenceException;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.persistence.UserProvider;
import ro.cs.tao.persistence.managers.UserManager;
import ro.cs.tao.persistence.repository.UserRepository;
import ro.cs.tao.security.SessionContext;
import ro.cs.tao.security.SessionStore;
import ro.cs.tao.security.UserPrincipal;
import ro.cs.tao.services.commons.CapabilityTokens;
import ro.cs.tao.services.entity.controllers.ConfigurationController;
import ro.cs.tao.services.entity.controllers.ContainerController;
import ro.cs.tao.services.entity.impl.RepositoryServiceImpl;
import ro.cs.tao.services.interfaces.ProgressReportService;
import ro.cs.tao.services.interfaces.UserService;
import ro.cs.tao.services.monitoring.controller.SSHController;
import ro.cs.tao.services.monitoring.impl.SSHSession;
import ro.cs.tao.services.progress.controller.ProgressReportController;
import ro.cs.tao.services.user.controller.UserController;
import ro.cs.tao.services.user.impl.UserInfo;
import ro.cs.tao.user.User;
import ro.cs.tao.user.UserStatus;
import ro.cs.tao.workspaces.Repository;

import java.time.Duration;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RestSecurityRegressionTest {
    @BeforeEach public void identity() {
        UserPrincipal principal = new UserPrincipal("alice", Set.of("USER"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        SessionContext context = mock(SessionContext.class);
        when(context.getPrincipal()).thenReturn(principal);
        SessionStore.setSessionContextProvider(() -> context);
    }

    @AfterEach public void clearIdentity() {
        SecurityContextHolder.clearContext();
        SessionStore.setSessionContextProvider(null);
    }

    @Test public void profileNeverSerializesCredentialsIncludingExtendedView() {
        User user = user("alice");
        user.setPassword("password-secret"); user.setPasswordResetKey("reset-secret"); user.setApiKey("api-secret");
        var mapper = new tools.jackson.databind.ObjectMapper();
        for (Object value : List.of(user, new UserInfo(user, 0, 0))) {
            String json = mapper.writeValueAsString(value);
            assertFalse(json.contains("password-secret"));
            assertFalse(json.contains("reset-secret"));
            assertFalse(json.contains("api-secret"));
        }
    }

    @Test public void deniesOtherUsersProfilesBeforeLookup() {
        UserController controller = new UserController();
        UserService service = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "userService", service);
        assertEquals(HttpStatus.FORBIDDEN, controller.getUserInfo("bob", false, null).getStatusCode());
        verifyNoInteractions(service);
    }

    @Test public void selfServiceCannotChangeWorkspaceOrResetKey() throws Exception {
        User original = user("alice"); original.setPasswordResetKey("original-reset");
        User submitted = user("alice"); submitted.setWorkspaceId("victim"); submitted.setPasswordResetKey("forged");
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByUsername("alice")).thenReturn(original);
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        UserManager manager = new UserManager(); ReflectionTestUtils.setField(manager, "repository", repository);
        User result = manager.update(submitted, false);
        assertNull(result.getWorkspaceId()); assertEquals("original-reset", result.getPasswordResetKey());
    }

    @Test public void mismatchedProfilePathDoesNotUpdate() {
        UserController controller = new UserController();
        UserService service = mock(UserService.class); ReflectionTestUtils.setField(controller, "userService", service);
        assertEquals(HttpStatus.UNAUTHORIZED, controller.updateUserInfo("bob", user("alice")).getStatusCode());
        verifyNoInteractions(service);
    }

    @Test public void forgedRepositoryOwnerDoesNotAuthorizeUpdateOrDelete() throws Exception {
        Repository existing = new Repository(); existing.setId("target"); existing.setUserId("bob");
        Repository submitted = new Repository(); submitted.setId("target"); submitted.setUserId("alice");
        RepositoryProvider provider = mock(RepositoryProvider.class); when(provider.get("target")).thenReturn(existing);
        RepositoryServiceImpl service = new RepositoryServiceImpl(); ReflectionTestUtils.setField(service, "workspaceProvider", provider);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.update(submitted));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.delete("target"));
        verify(provider, never()).update(any()); verify(provider, never()).delete(anyString());
    }

    @Test public void forbiddenContainerStopNeverReachesDocker() {
        ContainerInstance instance = mock(ContainerInstance.class);
        when(instance.getUserId()).thenReturn("bob");
        when(instance.getContainerId()).thenThrow(new AssertionError("Docker path must not be reached"));
        ContainerInstanceProvider provider = mock(ContainerInstanceProvider.class); when(provider.get("target")).thenReturn(instance);
        ContainerController controller = new ContainerController(); ReflectionTestUtils.setField(controller, "containerInstanceProvider", provider);
        assertEquals(HttpStatus.FORBIDDEN, controller.stopContainerInstance("target").getStatusCode());
        verify(instance, never()).getContainerId();
    }

    @Test public void legacyArbitraryProxyNeverConnects() throws Exception {
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        new ContainerController().tunnel("169.254.169.254", 80, "latest/meta-data", null, response);
        verify(response).sendError(410, "Use the authenticated instance tunnel");
        verifyNoMoreInteractions(response);
    }

    @Test public void foreignConfigurationCannotBeReadOrWritten() {
        ConfigurationController controller = new ConfigurationController();
        assertEquals(HttpStatus.FORBIDDEN, controller.getConfigurationItems(null, null, false, "bob").getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.saveUserConfigurationItems("bob", List.of()).getStatusCode());
    }

    @Test public void foreignProgressNeverFallsBackToGlobalResults() {
        ProgressReportController controller = new ProgressReportController();
        ProgressReportService service = mock(ProgressReportService.class); ReflectionTestUtils.setField(controller, "progressReportService", service);
        assertEquals(HttpStatus.FORBIDDEN, controller.getTasksInProgress(null, "bob", null).getStatusCode());
        verifyNoInteractions(service);
    }

    @Test public void activationGetIsReadOnlyAndWrongSubjectCannotActivate() throws Exception {
        UserController controller = new UserController();
        UserProvider provider = mock(UserProvider.class); UserService service = mock(UserService.class);
        ReflectionTestUtils.setField(controller, "userProvider", provider); ReflectionTestUtils.setField(controller, "userService", service);
        User pending = user("alice"); pending.setStatus(UserStatus.PENDING); when(provider.get("alice")).thenReturn(pending);
        String token = CapabilityTokens.issue("activation", "alice", Duration.ofHours(1));
        assertEquals(HttpStatus.OK, controller.confirmActivation("alice", token).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller.activate("bob", token).getStatusCode());
        verifyNoInteractions(service); verify(provider, never()).update(any(), anyBoolean());
    }

    @Test public void activationStateIsConsumedAtomically() throws Exception {
        User pending = user("alice"); pending.setStatus(UserStatus.PENDING);
        UserRepository repository = mock(UserRepository.class); when(repository.findById("alice")).thenReturn(Optional.of(pending));
        when(repository.activatePending("alice", UserStatus.PENDING, UserStatus.ACTIVE)).thenReturn(1, 0);
        UserManager manager = new UserManager(); ReflectionTestUtils.setField(manager, "repository", repository);
        manager.activate("alice");
        assertThrows(PersistenceException.class, () -> manager.activate("alice"));
        verify(repository, never()).save(any());
    }

    @Test public void sshSessionUUIDDoesNotAuthorizeAnotherUserOrAnonymousCaller() throws Exception {
        SSHController controller = new SSHController();
        WebSocketSession socket = mock(WebSocketSession.class); when(socket.getPrincipal()).thenReturn(new UserPrincipal("bob"));
        SSHSession session = mock(SSHSession.class); when(session.getWss()).thenReturn(socket);
        @SuppressWarnings("unchecked") var cache = (com.github.benmanes.caffeine.cache.Cache<UUID, SSHSession>) ReflectionTestUtils.getField(controller, "sessions");
        UUID id = UUID.randomUUID(); cache.put(id, session);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> controller.close(id.toString())).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class, () -> controller.resized(id.toString(), "{}")).getStatusCode());
        SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class, () -> controller.close(id.toString())).getStatusCode());
        verify(session, never()).destroy(); verify(session, never()).resized(anyInt(), anyInt());
    }

    @Test public void callbackRejectsForeignStaleAndReplayedEvents() throws Exception {
        var controller = new ro.cs.tao.services.orchestration.controllers.OrchestrationController();
        var provider = mock(ro.cs.tao.execution.persistence.ExecutionTaskProvider.class);
        ReflectionTestUtils.setField(controller, "executionTaskProvider", provider);
        var task = new ro.cs.tao.execution.model.ExecutionTaskSummary();
        task.setUserId("bob"); task.setTaskStatus(ro.cs.tao.execution.model.ExecutionStatus.QUEUED_ACTIVE);
        when(provider.getTaskStatus(1)).thenReturn(task);
        long now = System.currentTimeMillis() / 1000;
        String event = "unique_callback_event_1";
        assertEquals(HttpStatus.FORBIDDEN, controller.taskStatusChanged(1, "RUNNING", event, now, null).getStatusCode());
        task.setUserId("alice");
        assertEquals(HttpStatus.BAD_REQUEST, controller.taskStatusChanged(1, "RUNNING", event, now - 301, null).getStatusCode());
        try (var messages = mockStatic(ro.cs.tao.messaging.Messaging.class)) {
            assertEquals(HttpStatus.OK, controller.taskStatusChanged(1, "RUNNING", event, now, null).getStatusCode());
            assertEquals(HttpStatus.CONFLICT, controller.taskStatusChanged(1, "RUNNING", event, now, null).getStatusCode());
            messages.verify(() -> ro.cs.tao.messaging.Messaging.send(ro.cs.tao.security.SystemPrincipal.instance(),
                    ro.cs.tao.messaging.Topic.EXECUTION.value(), 1L, "RUNNING"), times(1));
        }
        task.setTaskStatus(ro.cs.tao.execution.model.ExecutionStatus.DONE);
        assertEquals(HttpStatus.CONFLICT, controller.taskStatusChanged(1, "RUNNING", "unique_callback_event_2", now, null).getStatusCode());
        var method = controller.getClass().getMethod("taskStatusChanged", long.class, String.class, String.class, long.class, String.class);
        assertEquals("ADMIN,EXECUTION_CALLBACK", method.getAnnotation(ro.cs.tao.services.commons.RoleRequired.class).roles());
    }

    @Test public void inspectorRejectsCrossOriginURLsBeforeSendingCredentials() throws Exception {
        var client = new ro.cs.tao.component.PublicServiceHttpClient(null, new java.net.URL("https://example.com/wms"));
        for (String url : List.of("https://other.example/wms", "http://example.com/wms", "https://example.com:8443/wms")) {
            assertThrows(java.io.IOException.class, () -> client.get(new java.net.URL(url), Map.of()));
        }
    }

    @Test public void publicConfigurationUsesAnExplicitAllowlist() {
        assertTrue(ro.cs.tao.services.commons.ConfigurationAccess.isPublic("tao.ui.base"));
        for (String key : List.of("security.capability.secret", "spring.datasource.password", "tao.dev.mode")) {
            assertFalse(ro.cs.tao.services.commons.ConfigurationAccess.isPublic(key));
        }
    }

    @Test public void directoryDownloadRetainsOrdinaryFiles() throws Exception {
        java.nio.file.Path root = java.nio.file.Files.createTempDirectory("security-zip-");
        try {
            java.nio.file.Files.writeString(root.resolve("inside.txt"), "inside");
            var bytes = new java.io.ByteArrayOutputStream();
            try (var zip = new java.util.zip.ZipOutputStream(bytes)) {
                new ro.cs.tao.services.entity.impl.FileStorageService().streamToZip(root.toString(), zip);
            }
            try (var zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
                assertEquals("inside.txt", zip.getNextEntry().getName());
                assertEquals("inside", new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                assertNull(zip.getNextEntry());
            }
        } finally {
            java.nio.file.Files.deleteIfExists(root.resolve("inside.txt"));
            java.nio.file.Files.deleteIfExists(root);
        }
    }

    private User user(String id) {
        User user = new User(); user.setId(id); user.setUsername(id); user.setEmail(id + "@example.invalid");
        user.setFirstName("First"); user.setLastName("Last"); user.setOrganization("Org"); user.setGroups(List.of());
        return user;
    }
    private static final class ReflectionTestUtils {
        static java.lang.reflect.Field field(Object target, String name) {
            for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
                try { var field = type.getDeclaredField(name); field.setAccessible(true); return field; }
                catch (NoSuchFieldException ignored) { }
            }
            throw new AssertionError("Missing field " + name);
        }
        static void setField(Object target, String name, Object value) {
            try { field(target, name).set(target, value); }
            catch (IllegalAccessException e) { throw new AssertionError(e); }
        }
        static Object getField(Object target, String name) {
            try { return field(target, name).get(target); }
            catch (IllegalAccessException e) { throw new AssertionError(e); }
        }
    }
}
