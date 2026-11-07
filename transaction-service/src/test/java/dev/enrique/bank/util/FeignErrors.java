package dev.enrique.bank.util;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import feign.FeignException;
import feign.Request;
import feign.Response;

/** The exceptions Feign throws for an error answer of account-service. */
public final class FeignErrors {
    private FeignErrors() {
    }

    public static FeignException status(int status, String body) {
        Request request = Request.create(Request.HttpMethod.POST, "/api/v1/account/movement/transfer",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(Map.of())
                .body(body, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("AccountClient#call", response);
    }
}
