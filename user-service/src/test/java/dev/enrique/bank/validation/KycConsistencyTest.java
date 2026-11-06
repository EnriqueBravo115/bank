package dev.enrique.bank.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import dev.enrique.bank.commons.exception.KycDataMismatchException;
import dev.enrique.bank.commons.validation.KycConsistency;

class KycConsistencyTest {
    private static final LocalDate BIRTHDAY = LocalDate.of(1992, 6, 15);

    @Test
    void shouldAcceptMatchingCurpRfcAndBirthday() {
        assertThatCode(() -> KycConsistency.check("MALA920615MSLRPN09", "MALA920615AB3", BIRTHDAY))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectCurpWithAnotherBirthDate() {
        assertThatThrownBy(() -> KycConsistency.check("MALA920616MSLRPN09", "MALA920615AB3", BIRTHDAY))
                .isInstanceOf(KycDataMismatchException.class)
                .hasMessage("CURP birth date does not match the birthday of the profile");
    }

    @Test
    void shouldRejectRfcWithAnotherBirthDate() {
        assertThatThrownBy(() -> KycConsistency.check("MALA920615MSLRPN09", "MALA930615AB3", BIRTHDAY))
                .isInstanceOf(KycDataMismatchException.class)
                .hasMessage("RFC birth date does not match the birthday of the profile");
    }

    @Test
    void shouldCheckCenturyWithCurpHomonymCharacter() {
        LocalDate bornIn2001 = LocalDate.of(2001, 3, 4);

        assertThatCode(() -> KycConsistency.check("MALA010304MSLRPNA9", "MALA010304AB3", bornIn2001))
                .doesNotThrowAnyException();
        // A digit there means born up to 1999, i.e. 1901 instead of 2001.
        assertThatThrownBy(() -> KycConsistency.check("MALA010304MSLRPN09", "MALA010304AB3", bornIn2001))
                .isInstanceOf(KycDataMismatchException.class)
                .hasMessage("CURP does not match the century of the birthday of the profile");
        assertThatThrownBy(() -> KycConsistency.check("MALA920615MSLRPNA9", "MALA920615AB3", BIRTHDAY))
                .isInstanceOf(KycDataMismatchException.class);
    }

    @Test
    void shouldRejectCurpAndRfcOfDifferentPeople() {
        assertThatThrownBy(() -> KycConsistency.check("MALA920615MSLRPN09", "OARO920615AB3", BIRTHDAY))
                .isInstanceOf(KycDataMismatchException.class)
                .hasMessage("CURP and RFC do not belong to the same person");
    }

    @Test
    void shouldAcceptInconvenientWordReplacedDifferently() {
        // CURP replaces the 2nd letter with X, RFC the 4th one.
        assertThatCode(() -> KycConsistency.check("BXEI920615HSLRPN09", "BUEX920615AB3", BIRTHDAY))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> KycConsistency.check("BXEI920615HSLRPN09", "BUEI920615AB3", BIRTHDAY))
                .isInstanceOf(KycDataMismatchException.class);
    }

    @Test
    void shouldAcceptEnieWrittenAsXInCurp() {
        assertThatCode(() -> KycConsistency.check("XUAL920615HSLRPN09", "ÑUAL920615AB3", BIRTHDAY))
                .doesNotThrowAnyException();
    }
}
