package com.example.headlesscrm.controller;

import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.TimelineItemResponse;
import com.example.headlesscrm.exception.GlobalExceptionHandler;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.TimelineService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TimelineControllerTest {

    @Mock
    private TimelineService timelineService;

    @InjectMocks
    private TimelineController timelineController;

    private MockMvc mockMvc;
    private UUID customerId;
    private UUID dealId;
    private TimelineItemResponse timelineItem;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        dealId = UUID.randomUUID();

        timelineItem = TimelineItemResponse.builder()
                .id("event-1")
                .timestamp(LocalDateTime.now())
                .type("MEETING")
                .category("ACTIVITY")
                .title("Customer Sync")
                .description("Discussed roadmap")
                .actorName("Jane Doe")
                .actorEmail("jane@example.com")
                .entityType("CUSTOMER")
                .entityId(customerId)
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

        mockMvc = MockMvcBuilders.standaloneSetup(timelineController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver, new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getWorkspaceTimeline_Returns200Ok() throws Exception {
        when(timelineService.getWorkspaceTimeline(nullable(UUID.class), eq("user@example.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(timelineItem)));

        mockMvc.perform(get("/api/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("event-1"))
                .andExpect(jsonPath("$.content[0].type").value("MEETING"))
                .andExpect(jsonPath("$.content[0].title").value("Customer Sync"));
    }

    @Test
    void getCustomerTimeline_Returns200Ok() throws Exception {
        when(timelineService.getCustomerTimeline(eq(customerId), eq("user@example.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(timelineItem)));

        mockMvc.perform(get("/api/timeline/customers/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("event-1"))
                .andExpect(jsonPath("$.content[0].title").value("Customer Sync"));
    }

    @Test
    void getDealTimeline_Returns200Ok() throws Exception {
        when(timelineService.getDealTimeline(eq(dealId), eq("user@example.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(timelineItem)));

        mockMvc.perform(get("/api/timeline/deals/{dealId}", dealId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("event-1"))
                .andExpect(jsonPath("$.content[0].type").value("MEETING"));
    }
}
