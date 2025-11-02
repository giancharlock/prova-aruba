package com.experis.receiver;

import com.experis.receiver.dto.ReceiverContactInfoDto;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
@EnableConfigurationProperties(value = {ReceiverContactInfoDto.class})
@OpenAPIDefinition(
		info = @Info(
				title = "Receiver microservice REST API Documentation",
				description = "InvoiceApp Receiver microservice REST API Documentation",
				version = "v1",
				contact = @Contact(
						name = "Madan Reddy",
						email = "tutor@experis.com",
						url = "https://www.experis.com"
				),
				license = @License(
						name = "Apache 2.0",
						url = "https://www.experis.com"
				)
		),
		externalDocs = @ExternalDocumentation(
				description =  "InvoiceApp Receiver microservice REST API Documentation",
				url = "https://www.experis.com/swagger-ui.html"
		)
)
public class ReceiverApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReceiverApplication.class, args);
	}

}
