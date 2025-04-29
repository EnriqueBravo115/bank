# bank
A continuación se presenta una explicación de métodos de servicios que tienen un grado de complejidad alto:

## getTransferHistory()
```
@Override
public Map<String, Map<String, List<TransferRequest>>> getTransferHistory(Long accountId) {
    return transactionRepository.findAllByAccountId(accountId).stream()
            .collect(twoLevelGroupingBy(
                    t -> t.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    t -> t.getAmount().toString(),
                    t -> basicMapper.convertToResponse(t, TransferRequest.class)));
}
```
Obtiene el historial de transferencias de una cuenta específica y las organiza en una estructura jerárquica agrupada por
fecha y monto.

`Map<String, Map<String, List<TransferRequest>>>`:
- Nivel 1: Agrupado por fecha en formato ISO 8601
- Nivel 2: Agrupado por monto(Representado como String)
- Valor: Lista de objetos `TransferRequest` que representan las transferencias
