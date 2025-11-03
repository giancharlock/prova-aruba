package com.experis.receiver.controller;

import com.experis.dbmanager.dto.CustomerDto;
import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.ResponseDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.receiver.constants.ReceiverConstants;
import com.experis.receiver.service.IReceiverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReceiverController.class)
@DisplayName("Unit Test del ReceiverController")
class ReceiverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IReceiverService iReceiverService;

    @Autowired
    private ObjectMapper objectMapper;

    private InvoiceDto validInvoiceDto;
    private SdiNotificationDto validSdiNotificationDto;
    private CustomerDto customerDto;

    @BeforeEach
    void setUp() {
        // Setup DTO validi usati nella maggior parte dei test
        customerDto = new CustomerDto();
        customerDto.setCustomerId(1);
        customerDto.setUsername("test-customer");

        validInvoiceDto = new InvoiceDto();
        validInvoiceDto.setInvoiceNumber(123);
        validInvoiceDto.setCustomer(customerDto);
        validInvoiceDto.setInvoice("<xml>...</xml>");
        validInvoiceDto.setCallback("http://callback.url");

        validSdiNotificationDto = new SdiNotificationDto();
        validSdiNotificationDto.setCustomerId(1);
        validSdiNotificationDto.setInvoiceNumber(123);
        validSdiNotificationDto.setStatus(InvoiceStatus.INTERNAL_INVOICE_DELIVERED);
    }

    // --- Test per /api/salvaFatturaInterna ---

    @Test
    @DisplayName("salvaFatturaInterna - 200 OK (Successo)")
    void salvaFatturaInterna_shouldReturnSuccess_whenServiceSucceeds() throws Exception {
        // Setup Mock: Il servizio completa con successo (OK)
        ResponseDto successResponse = new ResponseDto(ReceiverConstants.STATUS_200, ReceiverConstants.MESSAGE_200);
        ResponseEntity<ResponseDto> responseEntity = new ResponseEntity<>(successResponse, HttpStatus.OK);

        when(iReceiverService.saveInternalInvoice(any(InvoiceDto.class)))
                .thenReturn(CompletableFuture.completedFuture(responseEntity));

        // Azione (Chiamata HTTP) e Attesa Asincrona
        MvcResult mvcResult = mockMvc.perform(post("/api/salvaFatturaInterna")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInvoiceDto)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted()) // Verifica che la richiesta sia asincrona
                .andReturn();

        // Assert (Dispatch Asincrono e Verifica Risultato Finale)
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk()) // Status finale dal CompletableFuture
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(ReceiverConstants.STATUS_200))
                .andExpect(jsonPath("$.statusMsg").value(ReceiverConstants.MESSAGE_200));
    }

    @Test
    @DisplayName("salvaFatturaInterna - 504 Gateway Timeout (Timeout del Servizio)")
    void salvaFatturaInterna_shouldReturnTimeout_whenServiceTimesOut() throws Exception {
        // Setup Mock: Il servizio completa con un errore di Timeout
        ResponseDto errorResponse = new ResponseDto("504", "Fattura timeout");
        ResponseEntity<ResponseDto> responseEntity = new ResponseEntity<>(errorResponse, HttpStatus.GATEWAY_TIMEOUT);

        when(iReceiverService.saveInternalInvoice(any(InvoiceDto.class)))
                .thenReturn(CompletableFuture.completedFuture(responseEntity));

        // Azione e Attesa Asincrona
        MvcResult mvcResult = mockMvc.perform(post("/api/salvaFatturaInterna")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInvoiceDto)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        // Assert (Dispatch Asincrono)
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isGatewayTimeout()) // Status finale 504
                .andExpect(jsonPath("$.statusCode").value("504"))
                .andExpect(jsonPath("$.statusMsg").value("Fattura timeout"));
    }

    @Test
    @DisplayName("salvaFatturaInterna - 500 Internal Server Error (Eccezione Asincrona)")
    void salvaFatturaInterna_shouldReturn500_whenServiceFutureFails() throws Exception {
        // Setup Mock: Il CompletableFuture fallisce con un'eccezione
        when(iReceiverService.saveInternalInvoice(any(InvoiceDto.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Errore DB asincrono")));

        // Azione e Attesa Asincrona
        MvcResult mvcResult = mockMvc.perform(post("/api/salvaFatturaInterna")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInvoiceDto)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        // Assert (Dispatch Asincrono)
        // Spring MVC gestisce il failedFuture e lo converte in 500
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("salvaFatturaInterna - 400 Bad Request (JSON Malformato)")
    void salvaFatturaInterna_shouldReturn400_whenJsonIsMalformed() throws Exception {
        String malformedJson = "{\"invoiceNumber\": 123, \"customer\": \"manca-chiusura\""; // JSON invalido

        // Azione e Assert (Questa è sincrona, non serve asyncDispatch)
        mockMvc.perform(post("/api/salvaFatturaInterna")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest()); // Errore 400
    }

    /*
    @Test
    @DisplayName("salvaFatturaInterna - 400 Bad Request (Validazione Fallita)")
    void salvaFatturaInterna_shouldReturn400_whenValidationFails() throws Exception {
        // Per far funzionare questo test, aggiungi @NotNull a 'customer' in InvoiceDto

        InvoiceDto invalidDto = new InvoiceDto();
        invalidDto.setInvoiceNumber(123);
        invalidDto.setCustomer(null); // Campo invalido

        // Azione e Assert
        mockMvc.perform(post("/api/salvaFatturaInterna")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest()); // Errore 400
    }
    */

    // --- Test per /api/salvaFatturaEsterna ---

    @Test
    @DisplayName("salvaFatturaEsterna - 200 OK (Successo)")
    void salvaFatturaEsterna_shouldReturnSuccess_whenServiceSucceeds() throws Exception {
        // Setup Mock
        ResponseDto successResponse = new ResponseDto(ReceiverConstants.STATUS_200, ReceiverConstants.MESSAGE_200);
        ResponseEntity<ResponseDto> responseEntity = new ResponseEntity<>(successResponse, HttpStatus.OK);

        when(iReceiverService.saveExternalInvoice(any(InvoiceDto.class)))
                .thenReturn(CompletableFuture.completedFuture(responseEntity));

        // Azione e Attesa Asincrona
        MvcResult mvcResult = mockMvc.perform(post("/api/salvaFatturaEsterna")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInvoiceDto)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        // Assert
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ReceiverConstants.STATUS_200))
                .andExpect(jsonPath("$.statusMsg").value(ReceiverConstants.MESSAGE_200));
    }

    // --- Test per /api/notificaSdI ---

    @Test
    @DisplayName("notificaSdI - 200 OK (Successo)")
    void notificaSdI_shouldReturnSuccess_whenServiceSucceeds() throws Exception {
        // Setup Mock
        ResponseDto successResponse = new ResponseDto(ReceiverConstants.STATUS_200, ReceiverConstants.MESSAGE_200);
        ResponseEntity<ResponseDto> responseEntity = new ResponseEntity<>(successResponse, HttpStatus.OK);

        when(iReceiverService.handleSdiNotification(any(SdiNotificationDto.class)))
                .thenReturn(CompletableFuture.completedFuture(responseEntity));

        // Azione e Attesa Asincrona
        MvcResult mvcResult = mockMvc.perform(post("/api/notificaSdI")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSdiNotificationDto)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        // Assert
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(ReceiverConstants.STATUS_200))
                .andExpect(jsonPath("$.statusMsg").value(ReceiverConstants.MESSAGE_200));
    }

    @Test
    @DisplayName("notificaSdI - 500 Internal Server Error (Eccezione Asincrona)")
    void notificaSdI_shouldReturn500_whenServiceFutureFails() throws Exception {
        // Setup Mock: Il CompletableFuture fallisce
        when(iReceiverService.handleSdiNotification(any(SdiNotificationDto.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Errore Kafka asincrono")));

        // Azione e Attesa Asincrona
        MvcResult mvcResult = mockMvc.perform(post("/api/notificaSdI")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSdiNotificationDto)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        // Assert (Dispatch Asincrono)
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isInternalServerError());
    }

    /*
    @Test
    @DisplayName("notificaSdI - 400 Bad Request (Validazione Fallita)")
    void notificaSdI_shouldReturn400_whenValidationFails() throws Exception {
        // Per far funzionare questo test, aggiungi @NotNull a 'invoiceNumber' e 'status' in SdiNotificationDto

        SdiNotificationDto invalidDto = new SdiNotificationDto();
        invalidDto.setCustomerId(1);
        invalidDto.setInvoiceNumber(null); // Invalido
        invalidDto.setStatus(null); // Invalido

        // Azione e Assert
        mockMvc.perform(post("/api/notificaSdI")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest()); // Errore 400
    }
    */
}