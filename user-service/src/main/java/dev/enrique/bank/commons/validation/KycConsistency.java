package dev.enrique.bank.commons.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import dev.enrique.bank.commons.exception.KycDataMismatchException;

/**
 * Cross-checks a CURP and an RFC (persona física) against each other and against the birthday of the
 * profile. Their format is already validated by {@code UserKycDataRequest}.
 *
 * Both start with the same 4 letters taken from the name and the birth date as YYMMDD. Exceptions:
 * the CURP writes Ñ as X, and when the 4 letters form an inconvenient word the CURP replaces the
 * 2nd letter with X while the RFC replaces the 4th one.
 */
public final class KycConsistency {
    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");
    private static final int DATE_START = 4;
    private static final int DATE_END = 10;
    // Homonym character: a digit for people born up to 1999, a letter from 2000 on.
    private static final int CURP_CENTURY_POSITION = 16;

    private KycConsistency() {
    }

    public static void check(String curp, String rfc, LocalDate birthday) {
        String birthDate = birthday.format(YYMMDD);
        if (!curp.substring(DATE_START, DATE_END).equals(birthDate)) {
            throw new KycDataMismatchException("CURP birth date does not match the birthday of the profile");
        }
        if (!rfc.substring(DATE_START, DATE_END).equals(birthDate)) {
            throw new KycDataMismatchException("RFC birth date does not match the birthday of the profile");
        }
        if (Character.isLetter(curp.charAt(CURP_CENTURY_POSITION)) != birthday.getYear() >= 2000) {
            throw new KycDataMismatchException("CURP does not match the century of the birthday of the profile");
        }
        if (!sameNameLetters(curp, rfc)) {
            throw new KycDataMismatchException("CURP and RFC do not belong to the same person");
        }
    }

    private static boolean sameNameLetters(String curp, String rfc) {
        if (sameLetter(curp, rfc, 0) && sameLetter(curp, rfc, 1) && sameLetter(curp, rfc, 2) && sameLetter(curp, rfc, 3)) {
            return true;
        }
        // Inconvenient word, replaced differently by each one.
        return sameLetter(curp, rfc, 0) && curp.charAt(1) == 'X' && sameLetter(curp, rfc, 2) && rfc.charAt(3) == 'X';
    }

    private static boolean sameLetter(String curp, String rfc, int position) {
        char c = curp.charAt(position);
        char r = rfc.charAt(position);
        return c == r || (c == 'X' && r == 'Ñ');
    }
}
