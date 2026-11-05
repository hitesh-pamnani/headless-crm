package com.example.headlesscrm.controller;

import com.example.headlesscrm.dto.CustomerRequest;
import com.example.headlesscrm.dto.CustomerResponse;
import com.example.headlesscrm.exception.GlobalExceptionHandler;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.CustomerService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID customerId;
    private CustomerResponse customerResponse;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        customerId = UUID.randomUUID();

        customerResponse = CustomerResponse.builder()
                .id(customerId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .address("123 Main St")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customerRequest = new CustomerRequest();
        customerRequest.setFirstName("John");
        customerRequest.setLastName("Doe");
        customerRequest.setEmail("john.doe@example.com");
        customerRequest.setPhone("+1234567890");
        customerRequest.setAddress("123 Main St");

        com.example.headlesscrm.domain.User mockUser = new com.example.headlesscrm.domain.User();
        mockUser.setEmail("user@example.com");
        mockUser.setPassword("password");
        mockUser.setRoles(Collections.singletonList("ROLE_USER"));

        UserDetailsImpl userDetails = new UserDetailsImpl(mockUser);

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

        mockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver, new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createCustomer_Returns201Created() throws Exception {
        when(customerService.createCustomer(nullable(UUID.class), any(CustomerRequest.class), eq("user@example.com")))
                .thenReturn(customerResponse);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void createCustomer_WithWorkspaceHeader_PassesWorkspaceId() throws Exception {
        UUID customWorkspaceId = UUID.randomUUID();
        when(customerService.createCustomer(eq(customWorkspaceId), any(CustomerRequest.class), eq("user@example.com")))
                .thenReturn(customerResponse);

        mockMvc.perform(post("/api/customers")
                        .header("X-Workspace-Id", customWorkspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(customerId.toString()));
    }

    @Test
    void getAllCustomers_Returns200Ok() throws Exception {
        when(customerService.getAllCustomers(nullable(UUID.class), eq("user@example.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(customerResponse)));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(customerId.toString()));
    }

    @Test
    void getCustomerById_Returns200Ok() throws Exception {
        when(customerService.getCustomerById(eq(customerId), eq("user@example.com")))
                .thenReturn(customerResponse);

        mockMvc.perform(get("/api/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void getCustomerById_NotFound_Returns404ProblemDetail() throws Exception {
        when(customerService.getCustomerById(eq(customerId), eq("user@example.com")))
                .thenThrow(new ResourceNotFoundException("Customer not found with id: " + customerId));

        mockMvc.perform(get("/api/customers/{id}", customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Customer not found with id: " + customerId));
    }

    @Test
    void updateCustomer_Returns200Ok() throws Exception {
        when(customerService.updateCustomer(eq(customerId), any(CustomerRequest.class), eq("user@example.com")))
                .thenReturn(customerResponse);

        mockMvc.perform(put("/api/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()));
    }

    @Test
    void deleteCustomer_Returns204NoContent() throws Exception {
        doNothing().when(customerService).deleteCustomer(eq(customerId), eq("user@example.com"));

        mockMvc.perform(delete("/api/customers/{id}", customerId))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(eq(customerId), eq("user@example.com"));
    }
}
