package com.experis.receiver.service.impl;

import com.experis.dbmanager.constants.Constants;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.*;

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
    public ResponseEntity<ResponseDto> saveInternalInvoice(InvoiceDto invoice) {
        return processInvoice(invoice, InvoiceStatus.INTERNAL_INVOICE_NEW);
    }

    @Override
    public ResponseEntity<ResponseDto> saveExternalInvoice(InvoiceDto invoice) {
        return processInvoice(invoice, InvoiceStatus.EXTERNAL_INVOICE);
    }

    @Override
    public ResponseEntity<ResponseDto> handleSdiNotification(SdiNotificationDto notification) {
        String cacheKey = buildCacheKey(notification.getCustomerId(), notification.getCorrelationId());
        log.info("Ricevuta notifica SdI per {}. In attesa di salvataggio DB.", cacheKey);

        // Creiamo il future per la gestione asincrona (timeout/callback)
        CompletableFuture<ResponseEntity<ResponseDto>> future = new CompletableFuture<>();
        asyncResponseCache.put(cacheKey, future);

        try {
            boolean sent = streamBridge.send("publishSdiNotification-out-0", notification);
            if (!sent) {
                log.error("Errore nell'invio Kafka della notifica SdI per {}", cacheKey);
                // Rimuoviamo il future dalla cache se l'invio fallisce subito
                asyncResponseCache.remove(cacheKey);
                // Restituiamo errore 417 immediato
                return createErrorResponse(ReceiverConstants.MESSAGE_417_UPDATE, HttpStatus.EXPECTATION_FAILED);
            } else {
                log.info("Notifica SdI per {} inviata a dsiNotification.", cacheKey);
                // Avviamo il timeout per il future in background
                setupTimeout(cacheKey, future, "business-dlt-out-0", notification);
                // Restituiamo 202 Accepted immediato
                return createSuccessResponse(ReceiverConstants.MESSAGE_202, HttpStatus.ACCEPTED);
            }
        } catch (Exception e) {
            log.error("Eccezione durante invio Kafka della notifica SdI per {}", cacheKey, e);
            // Rimuoviamo il future dalla cache se l'invio fallisce subito
            asyncResponseCache.remove(cacheKey);
            // Restituiamo 500 immediato
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void handleSavedInvoice(InvoiceDto savedInvoice) {
        String cacheKey = buildCacheKey(savedInvoice.getCustomer().getCustomerId(), savedInvoice.getCorrelationId());

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
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-ARUBA-API-KEY","aruba4hookdeck-secret");
                headers.set(Constants.CORRELATION_ID_HEADER, savedInvoice.getCorrelationId());
                HttpEntity<Object> requestEntity = new HttpEntity<>(savedInvoice, headers);
                restTemplate.postForEntity(savedInvoice.getCallback(), requestEntity, String.class);
                future.complete(createSuccessResponse(ReceiverConstants.MESSAGE_200, HttpStatus.OK));
            } catch (Exception e) {
                log.error("Errore durante l'esecuzione della callback per {} a {}: {}", cacheKey, savedInvoice.getCallback(), e.getMessage());
                streamBridge.send("business-dlt-out-0", savedInvoice);
                future.complete(createErrorResponse("Errore Callback", HttpStatus.SERVICE_UNAVAILABLE));
            }
        } else {
            future.complete(createSuccessResponse(ReceiverConstants.MESSAGE_200, HttpStatus.OK));
        }
    }

    @Override
    public void handleUpdatedInvoice(SdiNotificationDto sdiNotificationDto) {

        sdiNotificationDto.setUpdatedAt(LocalDateTime.now());
        String cacheKey = buildCacheKey(sdiNotificationDto.getCustomerId(), sdiNotificationDto.getCorrelationId());

        CompletableFuture<ResponseEntity<ResponseDto>> future = asyncResponseCache.remove(cacheKey);

        if (future == null) {
            log.warn("Ricevuta di update della fattura per notifica SdI {}, ma non c'è nessuna richiesta asincrona in attesa.", cacheKey);
            return;
        }

        if (future.isDone()) {
            log.warn("Ricevuta di update della fattura per notifica SdI {}, ma la richiesta era già stata completata (probabilmente timeout).", cacheKey);
            return;
        }

        log.info("Ricevuta di update della fattura per notifica SdI {}. Stato: {}", cacheKey, sdiNotificationDto.getStatus());

        future.complete(createSuccessResponse(ReceiverConstants.MESSAGE_200, HttpStatus.OK));
    }

    private ResponseEntity<ResponseDto> processInvoice(InvoiceDto invoice, InvoiceStatus status) {
        invoice.setInvoiceStatus(status);
        invoice.setStatusLastUpdatedAt(LocalDateTime.now());
        if(InvoiceStatus.EXTERNAL_INVOICE.equals(invoice.getInvoiceStatus())
                || InvoiceStatus.INTERNAL_INVOICE_NEW.equals(invoice.getInvoiceStatus())){
            invoice.setCreatedAt(LocalDateTime.now());
        }else{
            invoice.setUpdatedAt(LocalDateTime.now());
        }

        String cacheKey = buildCacheKey(invoice.getCustomer().getCustomerId(), invoice.getCorrelationId());
        log.info("Processando fattura {}. Stato: {}. In attesa di salvataggio DB.", cacheKey, status);

        // Creiamo il future per la gestione asincrona (timeout/callback)
        CompletableFuture<ResponseEntity<ResponseDto>> future = new CompletableFuture<>();
        asyncResponseCache.put(cacheKey, future);

        try {
            boolean sent = streamBridge.send("publishInvoice-out-0", invoice);
            if (!sent) {
                log.error("Errore nell'invio Kafka della fattura {}", cacheKey);
                // Rimuoviamo il future dalla cache se l'invio fallisce subito
                asyncResponseCache.remove(cacheKey);
                // Restituiamo errore 417 immediato
                return createErrorResponse(ReceiverConstants.MESSAGE_417_UPDATE, HttpStatus.EXPECTATION_FAILED);
            } else {
                log.info("Fattura {} inviata a incomingInvoice.", cacheKey);
                // Avviamo il timeout per il future in background
                setupTimeout(cacheKey, future, "business-dlt-out-0", invoice);
                // Restituiamo 202 Accepted immediato
                return createSuccessResponse(ReceiverConstants.MESSAGE_202, HttpStatus.ACCEPTED);
            }
        } catch (Exception e) {
            log.error("Eccezione durante invio Kafka della fattura {}", cacheKey, e);
            // Rimuoviamo il future dalla cache se l'invio fallisce subito
            asyncResponseCache.remove(cacheKey);
            // Restituiamo 500 immediato
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static String buildCacheKey(Integer customerId, String invoiceNumber) {
        return (customerId != null ? customerId : "NA") + "-" + (invoiceNumber != null ? invoiceNumber : "NA");
    }

    private ResponseEntity<ResponseDto> createErrorResponse(String sender, HttpStatus status) {
        String statusCode = (status != null) ? String.valueOf(status.value()) : "500";
        return ResponseEntity
                .status(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseDto(statusCode, sender));
    }

    private ResponseEntity<ResponseDto> createSuccessResponse(String sender, HttpStatus status) {
        String statusCode = (status != null) ? String.valueOf(status.value()) : "200";
        return ResponseEntity
                .status(status != null ? status : HttpStatus.OK)
                .body(new ResponseDto(statusCode, sender));
    }

    private void setupTimeout(String cacheKey, CompletableFuture<ResponseEntity<ResponseDto>> future, String dltout, Object obj) {
        future.orTimeout(callbackTimeoutMs, TimeUnit.MILLISECONDS).whenComplete((response, throwable) -> {
            if (throwable instanceof TimeoutException) {
                log.warn("Timeout per {} {}. Spostamento in DLT.", dltout, cacheKey);
                streamBridge.send(dltout, obj);
                asyncResponseCache.remove(cacheKey); // Assicurati di rimuovere dalla cache anche in caso di timeout
                future.complete(createErrorResponse("Richiesta asincrona non completata entro il timeout", HttpStatus.GATEWAY_TIMEOUT));
            }
            // Se non è un TimeoutException, il future è stato completato normalmente (da savedInvoice) o con un altro errore
            // In ogni caso, la rimozione dalla cache avviene in savedInvoice o qui
            else if (response != null) {
                // Completato normalmente, la cache è già stata rimossa da savedInvoice
            } else if (throwable != null) {
                // Completato con un errore diverso dal timeout
                log.error("Future completato con errore imprevisto per {}: {}", cacheKey, throwable.getMessage());
                asyncResponseCache.remove(cacheKey); // Rimuovi in caso di errore
            }
        });
    }
}