package com.experis.dbmanager;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
@EnableScheduling
@OpenAPIDefinition(
		info = @Info(
				title = "DbManager microservice REST API Documentation",
				description =
                        """
                        InvoiceApp DbManager microservice create/update/search/delete invoices and customers. 
                        """,
				version = "v1",
				contact = @Contact(
						name = "Giancarlo Cadei",
						email = "giancarlo.cadei@it.experis.com",
						url = "https://www.experis.com"
				),
				license = @License(
						name = "Apache 2.0",
						url = "https://www.experis.com"
				)
		),
		externalDocs = @ExternalDocumentation(
				description = "InvoiceApp DbManager microservice REST API Documentation",
				url = "https://www.experis.com/swagger-ui.html"
		)
)
public class DbManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DbManagerApplication.class, args);
	}
}
