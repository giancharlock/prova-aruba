package com.experis.receiver.controller;

import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.ResponseDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import com.experis.dbmanager.dto.ErrorResponseDto;
import com.experis.receiver.service.IReceiverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "REST API per Ricezione Fatture",
        description = "API per ricevere fatture (interne/esterne) e notifiche SdI."
)
@RestController
@RequestMapping(path="/api", produces = {MediaType.APPLICATION_JSON_VALUE})
@Validated
@AllArgsConstructor
@Slf4j
public class ReceiverController {

    private final IReceiverService iReceiverService;

    @Operation(
            summary = "Salva Fattura Interna",
            description = "Riceve una fattura interna, la imposta su INTERNAL_INVOICE_NEW e la invia a Kafka. Risponde immediatamente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "HTTP Status Accepted (Richiesta accettata, elaborazione in corso)",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "417",
                    description = "HTTP Status Expectation Failed (Invio a Kafka fallito)",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    @PostMapping("/salvaFatturaInterna")
    public ResponseEntity<ResponseDto> salvaFatturaInterna(@Valid @RequestBody InvoiceDto invoiceDto) {
        log.debug("Ricevuta richiesta salvaFatturaInterna");
        // Ora restituisce direttamente la risposta (es. 202 o errore)
        return iReceiverService.saveInternalInvoice(invoiceDto);
    }

    @Operation(
            summary = "Salva Fattura Esterna",
            description = "Riceve una fattura esterna, la imposta su EXTERNAL_INVOICE e la invia a Kafka. Risponde immediatamente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "HTTP Status Accepted (Richiesta accettata, elaborazione in corso)",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "417",
                    description = "HTTP Status Expectation Failed (Invio a Kafka fallito)",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    @PostMapping("/salvaFatturaEsterna")
    public ResponseEntity<ResponseDto> salvaFatturaEsterna(@Valid @RequestBody InvoiceDto invoiceDto) {
        log.debug("Ricevuta richiesta salvaFatturaEsterna");
        return iReceiverService.saveExternalInvoice(invoiceDto);
    }

    @Operation(
            summary = "Notifica da SdI",
            description = "Riceve una notifica (esito) da SdI e la inoltra a Kafka sul topic dsiNotification. Risponde immediatamente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "HTTP Status Accepted (Richiesta accettata, elaborazione in corso)",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "417",
                    description = "HTTP Status Expectation Failed (Invio a Kafka fallito)",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    @PostMapping("/notificaSdI")
    public ResponseEntity<ResponseDto> notificaSdI(@Valid @RequestBody SdiNotificationDto notificationDto) {
        log.debug("Ricevuta richiesta notificaSdI");
        return iReceiverService.handleSdiNotification(notificationDto);
    }
}