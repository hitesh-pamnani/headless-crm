package com.example.headlesscrm.controller;

import com.example.headlesscrm.domain.StageCategory;
import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.PipelineRequest;
import com.example.headlesscrm.dto.PipelineResponse;
import com.example.headlesscrm.dto.PipelineStageRequest;
import com.example.headlesscrm.dto.PipelineStageResponse;
import com.example.headlesscrm.exception.GlobalExceptionHandler;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.PipelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
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
class PipelineControllerTest {

    @Mock
    private PipelineService pipelineService;

    @InjectMocks
    private PipelineController pipelineController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID pipelineId;
    private PipelineResponse pipelineResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        pipelineId = UUID.randomUUID();

        pipelineResponse = PipelineResponse.builder()
                .id(pipelineId)
                .name("Sales Pipeline")
                .description("Primary outbound pipeline")
                .isDefault(true)
                .stages(Collections.singletonList(
                        PipelineStageResponse.builder()
                                .id(UUID.randomUUID())
                                .name("Lead")
                                .displayOrder(0)
                                .probability(10)
                                .category(StageCategory.OPEN)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ))
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

        mockMvc = MockMvcBuilders.standaloneSetup(pipelineController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    void createPipeline_Returns201Created() throws Exception {
        PipelineRequest request = PipelineRequest.builder()
                .name("Sales Pipeline")
                .description("Primary outbound pipeline")
                .stages(Collections.singletonList(
                        PipelineStageRequest.builder()
                                .name("Lead")
                                .displayOrder(0)
                                .probability(10)
                                .category(StageCategory.OPEN)
                                .build()
                ))
                .build();

        when(pipelineService.createPipeline(nullable(UUID.class), any(PipelineRequest.class), eq("user@example.com")))
                .thenReturn(pipelineResponse);

        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(pipelineId.toString()))
                .andExpect(jsonPath("$.name").value("Sales Pipeline"))
                .andExpect(jsonPath("$.stages[0].name").value("Lead"));
    }

    @Test
    void getPipelines_Returns200Ok() throws Exception {
        when(pipelineService.getPipelines(nullable(UUID.class), eq("user@example.com")))
                .thenReturn(Collections.singletonList(pipelineResponse));

        mockMvc.perform(get("/api/pipelines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pipelineId.toString()))
                .andExpect(jsonPath("$[0].name").value("Sales Pipeline"));
    }

    @Test
    void getPipelineById_Returns200Ok() throws Exception {
        when(pipelineService.getPipelineById(eq(pipelineId), eq("user@example.com")))
                .thenReturn(pipelineResponse);

        mockMvc.perform(get("/api/pipelines/{id}", pipelineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pipelineId.toString()))
                .andExpect(jsonPath("$.name").value("Sales Pipeline"));
    }

    @Test
    void updatePipeline_Returns200Ok() throws Exception {
        PipelineRequest request = PipelineRequest.builder()
                .name("Updated Pipeline")
                .build();

        when(pipelineService.updatePipeline(eq(pipelineId), any(PipelineRequest.class), eq("user@example.com")))
                .thenReturn(pipelineResponse);

        mockMvc.perform(put("/api/pipelines/{id}", pipelineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pipelineId.toString()));
    }

    @Test
    void deletePipeline_Returns204NoContent() throws Exception {
        doNothing().when(pipelineService).deletePipeline(eq(pipelineId), eq("user@example.com"));

        mockMvc.perform(delete("/api/pipelines/{id}", pipelineId))
                .andExpect(status().isNoContent());

        verify(pipelineService).deletePipeline(pipelineId, "user@example.com");
    }

    @Test
    void setDefaultPipeline_Returns200Ok() throws Exception {
        when(pipelineService.setDefaultPipeline(eq(pipelineId), eq("user@example.com")))
                .thenReturn(pipelineResponse);

        mockMvc.perform(post("/api/pipelines/{id}/set-default", pipelineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pipelineId.toString()))
                .andExpect(jsonPath("$.default").value(true));
    }
}
