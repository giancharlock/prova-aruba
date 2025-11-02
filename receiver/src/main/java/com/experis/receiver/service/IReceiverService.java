package com.experis.receiver.service;

import com.experis.receiver.dto.InvoiceDto;
import com.experis.receiver.dto.ResponseDto;

public interface IReceiverService {

    /**
     *
     * @param invoice - InvoiceDto Object
     * @return result of operation
     */
    ResponseDto saveInternalInvoice(InvoiceDto invoice);

    /**
     *
     * @param invoice - InvoiceDto Object
     * @return result of operation
     */
    ResponseDto saveExternalInvoice(InvoiceDto invoice);

    /**
     *
     * @param mobileNumber - Input Mobile Number
     * @return boolean indicating if the delete of Receiver details is successful or not
     */
    ResponseDto deleteReceiver(String mobileNumber);


}
