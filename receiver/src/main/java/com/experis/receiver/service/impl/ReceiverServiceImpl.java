package com.experis.receiver.service.impl;

import com.experis.dbmanager.dto.InvoiceDto;
import com.experis.dbmanager.dto.ResponseDto;
import com.experis.dbmanager.dto.SdiNotificationDto;
import com.experis.dbmanager.enumerations.InvoiceStatus;
import com.experis.receiver.constants.ReceiverConstants;
import com.experis.receiver.service.IReceiverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiverServiceImpl implements IReceiverService {

    private final StreamBridge streamBridge;
    private final RestTemplate restTemplate;

    @Value("${app.callback.timeout-ms:30000}")
    private long callbackTimeoutMs;

    private static final ConcurrentMap<String, CompletableFuture<ResponseEntity<ResponseDto>>> asyncResponseCache = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<ResponseEntity<ResponseDto>> saveInternalInvoice(InvoiceDto invoice) {
        return processInvoice(invoice, InvoiceStatus.INTERNAL_INVOICE_NEW, "publishInvoice-out-0");
    }

    @Override
    public CompletableFuture<ResponseEntity<ResponseDto>> saveExternalInvoice(InvoiceDto invoice) {
        return processInvoice(invoice, InvoiceStatus.EXTERNAL_INVOICE, "publishInvoice-out-0");
    }

    @Override
    public CompletableFuture<ResponseEntity<ResponseDto>> handleSdiNotification(SdiNotificationDto notification) {
        String cacheKey = buildCacheKey(notification.getCustomerId(), notification.getInvoiceNumber());
        log.info("Ricevuta notifica SdI per {}. In attesa di salvataggio DB.", cacheKey);

        CompletableFuture<ResponseEntity<ResponseDto>> future = new CompletableFuture<>();
        asyncResponseCache.put(cacheKey, future);

        try {
            boolean sent = streamBridge.sender("publishSdiNotification-out-0", notification);
            if (!sent) {
                log.error("Errore nell'invio Kafka della notifica SdI per {}", cacheKey);
                future.complete(createErrorResponse(ReceiverConstants.MESSAGE_417_UPDATE, HttpStatus.EXPECTATION_FAILED));
            } else {
                log.info("Notifica SdI per {} inviata a DSI_NOTIFICATION.", cacheKey);
                setupTimeout(cacheKey, future, "Notifica SdI");
            }
        } catch (Exception e) {
            log.error("Eccezione durante invio Kafka della notifica SdI per {}", cacheKey, e);
            future.complete(createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }

        return future;
    }

    private CompletableFuture<ResponseEntity<ResponseDto>> processInvoice(InvoiceDto invoice, InvoiceStatus status, String topic) {
        invoice.setInvoiceStatus(status);

        String cacheKey = buildCacheKey(invoice.getCustomer().getCustomerId(), invoice.getInvoiceNumber());
        log.info("Processando fattura {}. Stato: {}. In attesa di salvataggio DB.", cacheKey, status);

        CompletableFuture<ResponseEntity<ResponseDto>> future = new CompletableFuture<>();
        asyncResponseCache.put(cacheKey, future);

        try {
            boolean sent = streamBridge.sender(topic, invoice);
            if (!sent) {
                log.error("Errore nell'invio Kafka della fattura {}", cacheKey);
                future.complete(createErrorResponse(ReceiverConstants.MESSAGE_417_UPDATE, HttpStatus.EXPECTATION_FAILED));
            } else {
                log.info("Fattura {} inviata a INCOMING_INVOICE.", cacheKey);
                setupTimeout(cacheKey, future, "Fattura");
            }
        } catch (Exception e) {
            log.error("Eccezione durante invio Kafka della fattura {}", cacheKey, e);
            future.complete(createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }

        return future;
    }

    @Bean
    public Consumer<InvoiceDto> consumeSavedInvoice() {
        return savedInvoice -> {
            String cacheKey = buildCacheKey(savedInvoice.getCustomer().getCustomerId(), savedInvoice.getInvoiceNumber());

            CompletableFuture<ResponseEntity<ResponseDto>> future = asyncResponseCache.remove(cacheKey);

            if (future == null) {
                log.warn("Ricevuta fattura salvata per {}, ma non c'è nessuna richiesta asincrona in attesa.", cacheKey);
                return;
            }

            if (future.isDone()) {
                log.warn("Ricevuta fattura salvata per {}, ma la richiesta era già stata completata (probabilmente timeout).", cacheKey);
                return;
            }

            log.info("Ricevuta conferma salvataggio per {}. Stato: {}", cacheKey, savedInvoice.getInvoiceStatus());

            if (savedInvoice.getCallback() != null && !savedInvoice.getCallback().isBlank()) {
                try {
                    log.info("Esecuzione callback per {} a {}", cacheKey, savedInvoice.getCallback());
                    restTemplate.postForEntity(savedInvoice.getCallback(), savedInvoice, String.class);
                    future.complete(createSuccessResponse(ReceiverConstants.MESSAGE_200, HttpStatus.OK));
                } catch (Exception e) {
                    log.error("Errore durante l'esecuzione della callback per {} a {}: {}", cacheKey, savedInvoice.getCallback(), e.getMessage());
                    streamBridge.sender("business-dlt-out-0", savedInvoice);
                    future.complete(createErrorResponse("Errore Callback", HttpStatus.SERVICE_UNAVAILABLE));
                }
            } else {
                future.complete(createSuccessResponse(ReceiverConstants.MESSAGE_200, HttpStatus.OK));
            }
        };
    }

    private String buildCacheKey(Integer customerId, Integer invoiceNumber) {
        return (customerId != null ? customerId : "NA") + "-" + (invoiceNumber != null ? invoiceNumber : "NA");
    }

    private ResponseEntity<ResponseDto> createErrorResponse(String sender, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(new ResponseDto(String.valueOf(status.value()), sender));
    }

    private ResponseEntity<ResponseDto> createSuccessResponse(String sender, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(new ResponseDto(String.valueOf(status.value()), sender));
    }

    private void setupTimeout(String cacheKey, CompletableFuture<ResponseEntity<ResponseDto>> future, String type) {
        future.orTimeout(callbackTimeoutMs, TimeUnit.MILLISECONDS).whenComplete((response, throwable) -> {
            if (throwable instanceof TimeoutException) {
                log.warn("Timeout per {} {}. Spostamento in DLT (simulato).", type, cacheKey);
                asyncResponseCache.remove(cacheKey);
                future.complete(createErrorResponse(type + " timeout", HttpStatus.GATEWAY_TIMEOUT));
            }
        });
    }
}