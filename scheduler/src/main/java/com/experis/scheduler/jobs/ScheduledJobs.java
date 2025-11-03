package com.experis.scheduler.jobs;

import com.experis.scheduler.config.SchedulerProperties;
import com.experis.scheduler.dto.DltMessageDto;
import com.experis.scheduler.dto.InvoiceReportDto;
import com.experis.scheduler.service.DbManagerClient;
import com.experis.scheduler.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@Slf4j
public class ScheduledJobs {

    private final SchedulerProperties properties;
    private final ReportService reportService;
    private final DbManagerClient dbManagerClient;
    private final KafkaConsumer<String, byte[]> dltConsumer;
    private final List<String> dltTopics;
    private final Duration dltPollTimeout;

    public ScheduledJobs(SchedulerProperties properties, ReportService reportService,
                         DbManagerClient dbManagerClient, KafkaConsumer<String, byte[]> dltConsumer) {
        this.properties = properties;
        this.reportService = reportService;
        this.dbManagerClient = dbManagerClient;
        this.dltConsumer = dltConsumer;

        // Configurazione DLT dal file properties
        this.dltTopics = properties.getDltJob().getTopics();
        this.dltPollTimeout = properties.getDltJob().getPollTimeout();

        // Sottoscrizione ai DLT topic all'avvio
        this.dltConsumer.subscribe(this.dltTopics);
        log.info("Sottoscrizione ai DLT topics: {}", this.dltTopics);
    }

    /**
     * JOB 1: Lettura DLT e Report CSV
     * Gira secondo la configurazione cron.
     */
    @Scheduled(cron = "${app.scheduler.dlt-job.cron}")
    public void readDltAndReport() {
        log.info("Esecuzione Job 1: Lettura DLT...");

        List<DltMessageDto> dltMessages = new ArrayList<>();
        try {
            // Eseguiamo il poll() manuale dei topic DLT
            ConsumerRecords<String, byte[]> records = dltConsumer.poll(dltPollTimeout);

            if (records.isEmpty()) {
                log.info("Job 1: Nessun messaggio trovato nei DLT.");
                return;
            }

            log.warn("Job 1: Trovati {} messaggi nei DLT. Preparazione report.", records.count());

            for (ConsumerRecord<String, byte[]> record : records) {
                dltMessages.add(mapRecordToDltDto(record));
            }

            // Scrivi il report
            reportService.writeReport(properties.getDltReportPath(), dltMessages, DltMessageDto.class);

            // Conferma il commit dell'offset
            dltConsumer.commitSync();

        } catch (Exception e) {
            log.error("Errore durante il Job 1 (Lettura DLT)", e);
            // Non facciamo il commit se c'è un errore, così riproviamo al prossimo giro
        }
    }

    /**
     * JOB 2: Report Fatture da DBManager
     * Gira secondo la configurazione cron.
     */
    @Scheduled(cron = "${app.scheduler.invoice-report-job.cron}")
    public void generateInvoiceStatusReport() {
        log.info("Esecuzione Job 2: Report Stato Fatture...");

        try {
            // Chiama dbmanager
            List<InvoiceReportDto> invoices = dbManagerClient.fetchAllInvoices().block(Duration.ofSeconds(30));

            if (invoices == null || invoices.isEmpty()) {
                log.info("Job 2: Nessuna fattura ricevuta da dbmanager.");
                return;
            }

            // Scrivi il report
            reportService.writeReport(properties.getInvoiceReportPath(), invoices, InvoiceReportDto.class);

        } catch (Exception e) {
            log.error("Errore durante il Job 2 (Report Fatture)", e);
        }
    }

    // --- Metodi Helper ---

    private DltMessageDto mapRecordToDltDto(ConsumerRecord<String, byte[]> record) {
        String headers = StreamSupport.stream(record.headers().spliterator(), false)
                .map(header -> header.key() + "=" + new String(header.value(), StandardCharsets.UTF_8))
                .collect(Collectors.joining(", "));

        String payloadPreview = "N/A";
        if (record.value() != null) {
            // Mostra solo i primi 200 caratteri del payload
            payloadPreview = new String(record.value(), StandardCharsets.UTF_8);
            if (payloadPreview.length() > 200) {
                payloadPreview = payloadPreview.substring(0, 200) + "... (truncated)";
            }
        }

        return new DltMessageDto(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), java.time.ZoneId.systemDefault()),
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                headers,
                payloadPreview
        );
    }
}