package dev.enrique.bank.service.util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public class TransactionHelper {
    // Agrupa elementos en un mapa anidado de dos niveles(f1 -> f2), y devuelve
    // una List<R>
    public static <T, A, B, R> Collector<T, ?, Map<A, Map<B, List<R>>>> twoLevelGroupingBy(
            Function<? super T, ? extends A> f1,
            Function<? super T, ? extends B> f2,
            Function<? super T, ? extends R> mapper) {

        Collector<T, ?, List<R>> downstream = mapping(mapper, toList());
        Collector<T, ?, Map<B, List<R>>> secondGrouping = groupingBy(f2, downstream);

        return groupingBy(f1, secondGrouping);
    }
}
