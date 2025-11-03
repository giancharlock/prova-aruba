package com.experis.scheduler.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.scheduler")
@Validated
@Data
public class SchedulerProperties {

    @NotEmpty
    private String dbmanagerUrl;

    @NotEmpty
    private String dltReportPath;

    @NotEmpty
    private String invoiceReportPath;

    @NotEmpty
    private DltJob dltJob;

    @NotEmpty
    private InvoiceReportJob invoiceReportJob;

    @Data
    public static class DltJob {
        @NotEmpty
        private String cron;
        @NotEmpty
        private List<String> topics;
        private Duration pollTimeout = Duration.ofSeconds(10);
    }

    @Data
    public static class InvoiceReportJob {
        @NotEmpty
        private String cron;
    }
}