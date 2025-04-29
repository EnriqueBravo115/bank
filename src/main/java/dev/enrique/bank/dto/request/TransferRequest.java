package dev.enrique.bank.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class TransferRequest {
    private Long id;
    private String description;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private String transactionType;
}
