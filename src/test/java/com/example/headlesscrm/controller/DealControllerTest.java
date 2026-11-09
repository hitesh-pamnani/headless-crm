package com.example.headlesscrm.controller;

import com.example.headlesscrm.domain.DealStatus;
import com.example.headlesscrm.domain.StageCategory;
import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.DealRequest;
import com.example.headlesscrm.dto.DealResponse;
import com.example.headlesscrm.dto.DealStageUpdateRequest;
import com.example.headlesscrm.exception.GlobalExceptionHandler;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.DealService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
class DealControllerTest {

    @Mock
    private DealService dealService;

    @InjectMocks
    private DealController dealController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID dealId;
    private DealResponse dealResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        dealId = UUID.randomUUID();

        dealResponse = DealResponse.builder()
                .id(dealId)
                .title("Acme Enterprise")
                .value(new BigDecimal("50000.00"))
                .currency("USD")
                .status(DealStatus.OPEN)
                .expectedCloseDate(LocalDate.of(2026, 12, 31))
                .pipelineId(UUID.randomUUID())
                .pipelineName("Sales Pipeline")
                .stageId(UUID.randomUUID())
                .stageName("Qualified")
                .stageCategory(StageCategory.OPEN)
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

        mockMvc = MockMvcBuilders.standaloneSetup(dealController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver, new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createDeal_Returns201Created() throws Exception {
        DealRequest request = DealRequest.builder()
                .title("Acme Enterprise")
                .value(new BigDecimal("50000.00"))
                .currency("USD")
                .build();

        when(dealService.createDeal(nullable(UUID.class), any(DealRequest.class), eq("user@example.com")))
                .thenReturn(dealResponse);

        mockMvc.perform(post("/api/deals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(dealId.toString()))
                .andExpect(jsonPath("$.title").value("Acme Enterprise"))
                .andExpect(jsonPath("$.value").value(50000.00));
    }

    @Test
    void getDeals_Returns200Ok() throws Exception {
        when(dealService.getDeals(nullable(UUID.class), nullable(UUID.class), nullable(UUID.class), nullable(UUID.class), nullable(DealStatus.class), eq("user@example.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(dealResponse)));

        mockMvc.perform(get("/api/deals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(dealId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Acme Enterprise"));
    }

    @Test
    void getDealById_Returns200Ok() throws Exception {
        when(dealService.getDealById(eq(dealId), eq("user@example.com")))
                .thenReturn(dealResponse);

        mockMvc.perform(get("/api/deals/{id}", dealId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dealId.toString()))
                .andExpect(jsonPath("$.title").value("Acme Enterprise"));
    }

    @Test
    void updateDeal_Returns200Ok() throws Exception {
        DealRequest request = DealRequest.builder()
                .title("Updated Enterprise Deal")
                .build();

        when(dealService.updateDeal(eq(dealId), any(DealRequest.class), eq("user@example.com")))
                .thenReturn(dealResponse);

        mockMvc.perform(put("/api/deals/{id}", dealId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dealId.toString()));
    }

    @Test
    void updateDealStage_Returns200Ok() throws Exception {
        DealStageUpdateRequest request = DealStageUpdateRequest.builder()
                .stageId(UUID.randomUUID())
                .build();

        when(dealService.updateDealStage(eq(dealId), any(DealStageUpdateRequest.class), eq("user@example.com")))
                .thenReturn(dealResponse);

        mockMvc.perform(patch("/api/deals/{id}/stage", dealId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dealId.toString()));
    }

    @Test
    void deleteDeal_Returns204NoContent() throws Exception {
        doNothing().when(dealService).deleteDeal(eq(dealId), eq("user@example.com"));

        mockMvc.perform(delete("/api/deals/{id}", dealId))
                .andExpect(status().isNoContent());

        verify(dealService).deleteDeal(dealId, "user@example.com");
    }
}
