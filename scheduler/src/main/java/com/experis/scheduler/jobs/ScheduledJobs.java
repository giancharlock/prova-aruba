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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@Slf4j
public class ScheduledJobs {

    private final SchedulerProperties properties;
    private final ReportService reportService;
    private final DbManagerClient dbManagerClient;
    private final KafkaConsumer<String, byte[]> dltConsumer;
    private final Duration dltPollTimeout;

    public ScheduledJobs(SchedulerProperties properties, ReportService reportService,
                         DbManagerClient dbManagerClient, KafkaConsumer<String, byte[]> dltConsumer) {
        this.properties = properties;
        this.reportService = reportService;
        this.dbManagerClient = dbManagerClient;
        this.dltConsumer = dltConsumer;

        // Configurazione DLT dal file properties
        List<String> dltTopics = properties.getDltJob().getTopics();
        this.dltPollTimeout = properties.getDltJob().getPollTimeout();

        // Sottoscrizione ai DLT topic all'avvio
        this.dltConsumer.subscribe(dltTopics);
        log.info("Sottoscrizione ai DLT topics: {}", dltTopics);
    }

    /**
     * JOB 1: Lettura DLT e Report CSV
     * Gira secondo la configurazione cron.
     */
    @Scheduled(cron = "${app.scheduler.dlt-job.cron}")
    public void readDltAndReport() {
        log.info("Esecuzione Job 1: Lettura DLT...");

        try {
            ConsumerRecords<String, byte[]> records = dltConsumer.poll(dltPollTimeout);

            if (records.isEmpty()) {
                log.info("Job 1: Nessun messaggio trovato nei DLT.");
                return;
            }

            log.warn("Job 1: Trovati {} messaggi nei DLT. Preparazione report.", records.count());

            List<DltMessageDto> dltMessages = new ArrayList<>();
            for (ConsumerRecord<String, byte[]> record : records) {
                dltMessages.add(mapRecordToDltDto(record));
            }

            // Raggruppa i messaggi per tipo
            Map<String, List<DltMessageDto>> messagesByType = dltMessages.stream()
                    .collect(Collectors.groupingBy(DltMessageDto::getType));

            // Scrive un report per ogni tipo
            for (Map.Entry<String, List<DltMessageDto>> entry : messagesByType.entrySet()) {
                String type = entry.getKey();
                List<DltMessageDto> messages = entry.getValue();
                String reportPath = generateReportPathForType(type);
                reportService.writeReport(reportPath, messages);
                log.info("Report DLT per il tipo '{}' scritto in: {}", type, reportPath);
            }

            dltConsumer.commitSync();

        } catch (Exception e) {
            log.error("Errore durante il Job 1 (Lettura DLT)", e);
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
            List<InvoiceReportDto> invoices = dbManagerClient.fetchAllInvoices().block(Duration.ofSeconds(30));

            if (invoices == null || invoices.isEmpty()) {
                log.info("Job 2: Nessuna fattura ricevuta da dbmanager.");
                return;
            }

            reportService.writeReport(properties.getInvoiceReportPath(), invoices);

        } catch (Exception e) {
            log.error("Errore durante il Job 2 (Report Fatture)", e);
        }
    }

    // --- Metodi Helper ---

    private DltMessageDto mapRecordToDltDto(ConsumerRecord<String, byte[]> record) {
        String headers = StreamSupport.stream(record.headers().spliterator(), false)
                .map(header -> header.key() + "=" + new String(header.value(), StandardCharsets.UTF_8))
                .collect(Collectors.joining(", "));

        String payload = "N/A";
        if (record.value() != null) {
            payload = new String(record.value(), StandardCharsets.UTF_8);
        }

        String type = record.topic().replace(".DLT", "");

        return new DltMessageDto(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), java.time.ZoneId.systemDefault()),
                record.topic(),
                type,
                record.partition(),
                record.offset(),
                record.key(),
                headers,
                payload
        );
    }

    private String generateReportPathForType(String type) {
        String originalPath = properties.getDltReportPath();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        // Inserisce il tipo e il timestamp prima dell'estensione .csv
        return originalPath.replace(".csv", "_" + type + "_" + timestamp + ".csv");
    }
}
