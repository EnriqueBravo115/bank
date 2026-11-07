package dev.enrique.bank.client;

import static dev.enrique.bank.client.AccountClient.AWAITING_CONFIRMATION;
import static dev.enrique.bank.client.AccountClient.toResult;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.util.FeignErrors;

/**
 * How an error of account-service becomes the status stored for the transaction: only answers that
 * prove the money did not move are final; the rest stay PENDING and are retried with the same key.
 */
class AccountClientFallbackTest {
    private static final MovementResultResponse PENDING =
            new MovementResultResponse(TransactionStatus.PENDING, AWAITING_CONFIRMATION);

    @Test
    void shouldDeclineWithMessageOfAccountServiceOn422() {
        assertThat(toResult(FeignErrors.status(422, "{\"status\":422,\"message\":\"Insufficient funds\"}")))
                .isEqualTo(new MovementResultResponse(TransactionStatus.DECLINED, "Insufficient funds"));
    }

    @Test
    void shouldDeclineWithGenericReasonWhen422BodyIsUnreadable() {
        assertThat(toResult(FeignErrors.status(422, "not json")))
                .isEqualTo(new MovementResultResponse(TransactionStatus.DECLINED, "Transaction declined"));
    }

    @Test
    void shouldFailOnAnswersThatProveNothingMoved() {
        assertThat(toResult(FeignErrors.status(404, "")))
                .isEqualTo(new MovementResultResponse(TransactionStatus.FAILED, "Account not found"));
        assertThat(toResult(FeignErrors.status(400, "")))
                .isEqualTo(new MovementResultResponse(TransactionStatus.FAILED, "Invalid movement request"));
        assertThat(toResult(FeignErrors.status(401, "")))
                .isEqualTo(new MovementResultResponse(TransactionStatus.FAILED, "Unauthorized operation"));
        assertThat(toResult(FeignErrors.status(403, "")))
                .isEqualTo(new MovementResultResponse(TransactionStatus.FAILED, "Unauthorized operation"));
    }

    @Test
    void shouldStayPendingWhenTheOutcomeIsUnknown() {
        assertThat(toResult(FeignErrors.status(500, ""))).isEqualTo(PENDING);
        assertThat(toResult(FeignErrors.status(503, ""))).isEqualTo(PENDING);
        // Same key still being processed by account-service.
        assertThat(toResult(FeignErrors.status(409, ""))).isEqualTo(PENDING);
        // Timeout, connection refused, open circuit.
        assertThat(toResult(new RuntimeException("Connection refused"))).isEqualTo(PENDING);
    }
}
