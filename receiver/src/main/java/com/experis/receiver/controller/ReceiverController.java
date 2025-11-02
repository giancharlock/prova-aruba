package com.experis.receiver.controller;

import com.experis.receiver.constants.ReceiverConstants;
import com.experis.receiver.dto.ErrorResponseDto;
import com.experis.receiver.dto.InvoiceDto;
import com.experis.receiver.dto.ReceiverContactInfoDto;
import com.experis.receiver.dto.ResponseDto;
import com.experis.receiver.service.IReceiverService;
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
        name = "CRUD REST APIs for Receiver in InvoiceApp",
        description = "CRUD REST APIs in InvoiceApp to CREATE, UPDATE, FETCH AND DELETE invoice details"
)
@RestController
@RequestMapping(path="/api", produces = {MediaType.APPLICATION_JSON_VALUE})
@Validated
public class ReceiverController {

    private static final Logger logger = LoggerFactory.getLogger(ReceiverController.class);

    private final IReceiverService iReceiverService;

    public ReceiverController(IReceiverService iReceiverService) {
        this.iReceiverService = iReceiverService;
    }

    @Value("${build.version}")
    private String buildVersion;

    @Autowired
    private Environment environment;

    @Operation(
            summary = "saveInternalInvoice Receiver Details REST API",
            description = "REST API Needed to save internal invoices"
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
    @GetMapping("/saveInternalInvoice")
    public ResponseEntity<ResponseDto> saveInternalInvoice(@RequestParam InvoiceDto invoice) {
        ResponseDto responseDto = iReceiverService.fetchReceiver(invoice);
        return ResponseEntity.status(HttpStatus.OK).body(customerDto);
    }

    @Operation(
            summary = "saveExternalInvoice Receiver Details REST API",
            description = "REST API save invoices coming from third parties"
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
    }
    )
    @PutMapping("/saveExternalInvoice")
    public ResponseEntity<ResponseDto> saveExternalInvoice(@Valid @RequestBody CustomerDto customerDto) {
        boolean isUpdated = iReceiverService.updateReceiver(customerDto);
        if(isUpdated) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto(ReceiverConstants.STATUS_200, ReceiverConstants.MESSAGE_200));
        }else{
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto(ReceiverConstants.STATUS_417, ReceiverConstants.MESSAGE_417_UPDATE));
        }
    }

    @Operation(
            summary = "sdiNotification Details REST API",
            description = "REST API expected reply from SdI and related to a sent invoice"
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
    }
    )
    @DeleteMapping("/sdiNotification")
    public ResponseEntity<ResponseDto> sdiNotification(@RequestParam
                                                                @Pattern(regexp="(^$|[0-9]{10})",message = "Mobile number must be 10 digits")
                                                                String mobileNumber) {
        boolean isDeleted = iReceiverService.deleteReceiver(mobileNumber);
        if(isDeleted) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto(ReceiverConstants.STATUS_200, ReceiverConstants.MESSAGE_200));
        }else{
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto(ReceiverConstants.STATUS_417, ReceiverConstants.MESSAGE_417_DELETE));
        }
    }

}
