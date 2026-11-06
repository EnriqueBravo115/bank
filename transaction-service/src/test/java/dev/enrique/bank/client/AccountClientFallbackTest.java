package dev.enrique.bank.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import feign.FeignException;
import feign.Request;
import feign.Response;

/** How an answer of account-service becomes the status stored for the transaction. */
class AccountClientFallbackTest {
    private final AccountClient client = mock(AccountClient.class, CALLS_REAL_METHODS);

    @Test
    void shouldDeclineWithMessageOfAccountServiceOn422() {
        MovementResultResponse result = client.movementFallback(
                feignError(422, "{\"status\":422,\"message\":\"Insufficient funds\"}"));

        assertThat(result).isEqualTo(new MovementResultResponse(TransactionStatus.DECLINED, "Insufficient funds"));
    }

    @Test
    void shouldDeclineWithGenericReasonWhen422BodyIsUnreadable() {
        MovementResultResponse result = client.movementFallback(feignError(422, "not json"));

        assertThat(result).isEqualTo(new MovementResultResponse(TransactionStatus.DECLINED, "Transaction declined"));
    }

    @Test
    void shouldFailWhenAccountDoesNotExist() {
        MovementResultResponse result = client.movementFallback(feignError(404, ""));

        assertThat(result).isEqualTo(new MovementResultResponse(TransactionStatus.FAILED, "Account not found"));
    }

    @Test
    void shouldFailWhenUnauthorized() {
        MovementResultResponse result = client.movementFallback(feignError(401, ""));

        assertThat(result).isEqualTo(new MovementResultResponse(TransactionStatus.FAILED, "Unauthorized operation"));
    }

    @Test
    void shouldFailAsUnavailableOnServerErrorOrConnectionProblem() {
        assertThat(client.movementFallback(feignError(503, "")))
                .isEqualTo(new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable"));
        assertThat(client.movementFallback(new RuntimeException("Connection refused")))
                .isEqualTo(new MovementResultResponse(TransactionStatus.FAILED, "Service unavailable"));
    }

    private static FeignException feignError(int status, String body) {
        Request request = Request.create(Request.HttpMethod.POST, "/api/v1/account/movement/transfer",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(Map.of())
                .body(body, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("AccountClient#processTransfer", response);
    }
}
