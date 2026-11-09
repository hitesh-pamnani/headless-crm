package com.example.headlesscrm.controller;

import com.example.headlesscrm.domain.ActivityStatus;
import com.example.headlesscrm.domain.ActivityType;
import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.ActivityRequest;
import com.example.headlesscrm.dto.ActivityResponse;
import com.example.headlesscrm.exception.GlobalExceptionHandler;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.ActivityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private ActivityController activityController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID activityId;
    private ActivityResponse activityResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        activityId = UUID.randomUUID();

        activityResponse = ActivityResponse.builder()
                .id(activityId)
                .type(ActivityType.MEETING)
                .title("Strategy Meeting")
                .description("Q4 planning")
                .status(ActivityStatus.PENDING)
                .dueDate(LocalDateTime.now().plusDays(3))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("password");
        user.setRoles(Collections.singletonList("ROLE_USER"));
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return userDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(activityController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver, new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createActivity_Returns201Created() throws Exception {
        ActivityRequest request = ActivityRequest.builder()
                .type(ActivityType.MEETING)
                .title("Strategy Meeting")
                .description("Q4 planning")
                .build();

        when(activityService.createActivity(nullable(UUID.class), any(ActivityRequest.class), eq("user@example.com")))
                .thenReturn(activityResponse);

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(activityId.toString()))
                .andExpect(jsonPath("$.title").value("Strategy Meeting"))
                .andExpect(jsonPath("$.type").value("MEETING"));
    }

    @Test
    void getActivities_Returns200Ok() throws Exception {
        when(activityService.getActivities(nullable(UUID.class), nullable(ActivityType.class), nullable(ActivityStatus.class), nullable(UUID.class), nullable(UUID.class), nullable(UUID.class), eq("user@example.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(activityResponse)));

        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(activityId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Strategy Meeting"));
    }

    @Test
    void getActivityById_Returns200Ok() throws Exception {
        when(activityService.getActivityById(eq(activityId), eq("user@example.com")))
                .thenReturn(activityResponse);

        mockMvc.perform(get("/api/activities/{id}", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId.toString()))
                .andExpect(jsonPath("$.title").value("Strategy Meeting"));
    }

    @Test
    void updateActivity_Returns200Ok() throws Exception {
        ActivityRequest request = ActivityRequest.builder()
                .type(ActivityType.MEETING)
                .title("Updated Strategy Meeting")
                .build();

        when(activityService.updateActivity(eq(activityId), any(ActivityRequest.class), eq("user@example.com")))
                .thenReturn(activityResponse);

        mockMvc.perform(put("/api/activities/{id}", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId.toString()));
    }

    @Test
    void completeActivity_Returns200Ok() throws Exception {
        activityResponse.setStatus(ActivityStatus.COMPLETED);
        activityResponse.setCompletedAt(LocalDateTime.now());

        when(activityService.completeActivity(eq(activityId), eq("user@example.com")))
                .thenReturn(activityResponse);

        mockMvc.perform(patch("/api/activities/{id}/complete", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteActivity_Returns204NoContent() throws Exception {
        doNothing().when(activityService).deleteActivity(eq(activityId), eq("user@example.com"));

        mockMvc.perform(delete("/api/activities/{id}", activityId))
                .andExpect(status().isNoContent());

        verify(activityService).deleteActivity(activityId, "user@example.com");
    }
}
