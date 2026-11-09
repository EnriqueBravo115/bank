package dev.enrique.bank.commons.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GeneratorsTest {
    // CLABEs published by banks and Banxico as examples
    @ParameterizedTest
    @ValueSource(strings = { "032180000118359719", "002010077777777771", "014027000005555558" })
    void clabeCheckDigitMatchesTheBanxicoAlgorithm(String clabe) {
        assertThat(ClabeGenerator.isValid(clabe)).isTrue();
    }

    @Test
    void invalidClabesAreRejected() {
        assertThat(ClabeGenerator.isValid("032180000118359710")).isFalse();
        assertThat(ClabeGenerator.isValid("03218000011835971")).isFalse();
        assertThat(ClabeGenerator.isValid(null)).isFalse();
    }

    @Test
    void generatedClabesAreValidAndDoNotRepeat() {
        ClabeGenerator generator = new ClabeGenerator();
        Set<String> clabes = new HashSet<>();
        IntStream.range(0, 1000).forEach(i -> clabes.add(generator.generate()));

        assertThat(clabes).hasSize(1000).allSatisfy(clabe -> {
            assertThat(clabe).startsWith(ClabeGenerator.BANK_CODE + ClabeGenerator.CITY_CODE);
            assertThat(ClabeGenerator.isValid(clabe)).isTrue();
        });
    }

    @Test
    void accountNumbersHaveTheColumnSizeAndDoNotRepeatWithinTheSameMillisecond() {
        AccountNumberGenerator generator = new AccountNumberGenerator();
        Set<String> numbers = new HashSet<>();
        IntStream.range(0, 1000).forEach(i -> numbers.add(generator.generate()));

        assertThat(numbers).hasSize(1000).allSatisfy(number -> assertThat(number).matches("ACC-[1-9]\\d{15}"));
    }
}
