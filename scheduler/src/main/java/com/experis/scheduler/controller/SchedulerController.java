package com.experis.scheduler.controller;

import com.experis.scheduler.constants.SchedulerConstants;
import com.experis.scheduler.dto.SchedulerContactInfoDto;
import com.experis.scheduler.dto.SchedulerDto;
import com.experis.scheduler.dto.ErrorResponseDto;
import com.experis.scheduler.dto.ResponseDto;
import com.experis.scheduler.service.ISchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author Experis
 */

@Tag(
        name = "CRUD REST APIs for Scheduler in InvoiceApp",
        description = "CRUD REST APIs in InvoiceApp to CREATE, UPDATE, FETCH AND DELETE scheduler details"
)
@RestController
@RequestMapping(path = "/api", produces = {MediaType.APPLICATION_JSON_VALUE})
@Validated
public class SchedulerController {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerController.class);

    private ISchedulerService iSchedulerService;

    public SchedulerController(ISchedulerService iSchedulerService) {
        this.iSchedulerService = iSchedulerService;
    }

    @Value("${build.version}")
    private String buildVersion;

    @Autowired
    private Environment environment;

    @Autowired
    private SchedulerContactInfoDto schedulerContactInfoDto;

    @Operation(
            summary = "Create Scheduler REST API",
            description = "REST API to create new Scheduler inside InvoiceApp"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "HTTP Status CREATED"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    }
    )
    @PostMapping("/create")
    public ResponseEntity<ResponseDto> createScheduler(@Valid @RequestParam
                                                      @Pattern(regexp="(^$|[0-9]{10})",sender = "Mobile number must be 10 digits")
                                                      String mobileNumber) {
        iSchedulerService.createScheduler(mobileNumber);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new ResponseDto(SchedulerConstants.STATUS_201, SchedulerConstants.MESSAGE_201));
    }

    @Operation(
            summary = "Fetch Scheduler Details REST API",
            description = "REST API to fetch scheduler details based on a mobile number"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @GetMapping("/fetch")
    public ResponseEntity<SchedulerDto> fetchSchedulerDetails(@RequestHeader("experis-correlation-id") String correlationId,
                                                                @RequestParam
                                                               @Pattern(regexp="(^$|[0-9]{10})",sender = "Mobile number must be 10 digits")
                                                               String mobileNumber) {
        logger.debug("fetchSchedulerDetails method start");
        SchedulerDto schedulerDto = iSchedulerService.fetchScheduler(mobileNumber);
        logger.debug("fetchSchedulerDetails method end");
        return ResponseEntity.status(HttpStatus.OK).body(schedulerDto);
    }

    @Operation(
            summary = "Update Scheduler Details REST API",
            description = "REST API to update scheduler details based on a scheduler number"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "417",
                    description = "Expectation Failed"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
        })
    @PutMapping("/update")
    public ResponseEntity<ResponseDto> updateSchedulerDetails(@Valid @RequestBody SchedulerDto schedulerDto) {
        boolean isUpdated = iSchedulerService.updateScheduler(schedulerDto);
        if(isUpdated) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto(SchedulerConstants.STATUS_200, SchedulerConstants.MESSAGE_200));
        }else{
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto(SchedulerConstants.STATUS_417, SchedulerConstants.MESSAGE_417_UPDATE));
        }
    }

    @Operation(
            summary = "Delete Scheduler Details REST API",
            description = "REST API to delete Scheduler details based on a mobile number"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "417",
                    description = "Expectation Failed"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @DeleteMapping("/delete")
    public ResponseEntity<ResponseDto> deleteSchedulerDetails(@RequestParam
                                                                @Pattern(regexp="(^$|[0-9]{10})",sender = "Mobile number must be 10 digits")
                                                                String mobileNumber) {
        boolean isDeleted = iSchedulerService.deleteScheduler(mobileNumber);
        if(isDeleted) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto(SchedulerConstants.STATUS_200, SchedulerConstants.MESSAGE_200));
        }else{
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto(SchedulerConstants.STATUS_417, SchedulerConstants.MESSAGE_417_DELETE));
        }
    }

    @Operation(
            summary = "Get Build information",
            description = "Get Build information that is deployed into scheduler microservice"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    }
    )
    @GetMapping("/build-info")
    public ResponseEntity<String> getBuildInfo() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(buildVersion);
    }

    @Operation(
            summary = "Get Java version",
            description = "Get Java versions details that is installed into scheduler microservice"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    }
    )
    @GetMapping("/java-version")
    public ResponseEntity<String> getJavaVersion() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(environment.getProperty("JAVA_HOME"));
    }

    @Operation(
            summary = "Get Contact Info",
            description = "Contact Info details that can be reached out in case of any issues"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    }
    )
    @GetMapping("/contact-info")
    public ResponseEntity<SchedulerContactInfoDto> getContactInfo() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(schedulerContactInfoDto);
    }

}
