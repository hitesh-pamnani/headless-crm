package com.example.headlesscrm.controller;

import com.example.headlesscrm.domain.User;
import com.example.headlesscrm.dto.ContactRequest;
import com.example.headlesscrm.dto.ContactResponse;
import com.example.headlesscrm.exception.GlobalExceptionHandler;
import com.example.headlesscrm.exception.ResourceNotFoundException;
import com.example.headlesscrm.security.UserDetailsImpl;
import com.example.headlesscrm.service.ContactService;
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
class ContactControllerTest {

    @Mock
    private ContactService contactService;

    @InjectMocks
    private ContactController contactController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID customerId;
    private UUID contactId;
    private ContactResponse contactResponse;
    private ContactRequest contactRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        customerId = UUID.randomUUID();
        contactId = UUID.randomUUID();

        contactResponse = ContactResponse.builder()
                .id(contactId)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .phone("+9876543210")
                .customerId(customerId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        contactRequest = new ContactRequest();
        contactRequest.setFirstName("Jane");
        contactRequest.setLastName("Doe");
        contactRequest.setEmail("jane.doe@example.com");
        contactRequest.setPhone("+9876543210");

        User mockUser = new User();
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

        mockMvc = MockMvcBuilders.standaloneSetup(contactController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver, new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createContact_Returns201Created() throws Exception {
        when(contactService.createContact(any(ContactRequest.class), eq(customerId), eq("user@example.com")))
                .thenReturn(contactResponse);

        mockMvc.perform(post("/api/customers/{customerId}/contacts", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contactRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(contactId.toString()))
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"));
    }

    @Test
    void getContactsByCustomer_Returns200Ok() throws Exception {
        when(contactService.getContactsByCustomer(eq(customerId), eq("user@example.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(contactResponse)));

        mockMvc.perform(get("/api/customers/{customerId}/contacts", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(contactId.toString()))
                .andExpect(jsonPath("$.content[0].email").value("jane.doe@example.com"));
    }

    @Test
    void getContactById_Returns200Ok() throws Exception {
        when(contactService.getContactById(eq(customerId), eq(contactId), eq("user@example.com")))
                .thenReturn(contactResponse);

        mockMvc.perform(get("/api/customers/{customerId}/contacts/{contactId}", customerId, contactId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contactId.toString()))
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"));
    }

    @Test
    void getContactById_NotFound_Returns404ProblemDetail() throws Exception {
        when(contactService.getContactById(eq(customerId), eq(contactId), eq("user@example.com")))
                .thenThrow(new ResourceNotFoundException("Contact not found with id: " + contactId));

        mockMvc.perform(get("/api/customers/{customerId}/contacts/{contactId}", customerId, contactId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Contact not found with id: " + contactId));
    }

    @Test
    void updateContact_Returns200Ok() throws Exception {
        when(contactService.updateContact(eq(customerId), eq(contactId), any(ContactRequest.class), eq("user@example.com")))
                .thenReturn(contactResponse);

        mockMvc.perform(put("/api/customers/{customerId}/contacts/{contactId}", customerId, contactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contactRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contactId.toString()));
    }

    @Test
    void deleteContact_Returns204NoContent() throws Exception {
        doNothing().when(contactService).deleteContact(eq(customerId), eq(contactId), eq("user@example.com"));

        mockMvc.perform(delete("/api/customers/{customerId}/contacts/{contactId}", customerId, contactId))
                .andExpect(status().isNoContent());

        verify(contactService).deleteContact(eq(customerId), eq(contactId), eq("user@example.com"));
    }
}
