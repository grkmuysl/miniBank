package com.gorkemuysal.minibank.account;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.SecureRandom;

// A helper component for the generate Random IBANs
@Component
public class IbanGenerator {

    private static final String COUNTRY_CODE = "TR";
    private static final String BANK_CODE = "00016"; // random code
    private static final String RESERVED_DIGIT = "0";
    private static final int ACCOUNT_NO_LENGTH = 16;

    private static final String TR00_NUMERIC = "292700";

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates Random IBAN and return as a String
     * @return IBAN as a String
     * **/
    public String generate() {
        String accountNo = generateAccountNumber();
        String bban = BANK_CODE + RESERVED_DIGIT + accountNo;

        String checkDigits = calculateCheckDigits(bban);

        return COUNTRY_CODE + checkDigits + bban;
    }

    /**
     * Generates Random 16 digits Account number
     *
     * @return 16 digit Account number as a string
     * */
    private String generateAccountNumber() {
        StringBuilder sb = new StringBuilder(ACCOUNT_NO_LENGTH);
        for (int i = 0; i < ACCOUNT_NO_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Calculates checks digits according to BBAN number
     * Gets BBAN and adds '292700' to end of its
     * then calculate mod 97 of this new number and substruct with 98
     * Finally generates checkDigit (2 digit)
     *
     * @return check digits as a String type. If checkDigit is 1 digit adds '0'
     * */
    private String calculateCheckDigits(String bban) {
        String numericString = bban + TR00_NUMERIC;
        BigInteger number = new BigInteger(numericString);
        BigInteger remainder = number.mod(BigInteger.valueOf(97));

        int checkDigit = 98 - remainder.intValue();

        return checkDigit < 10 ? "0" + checkDigit : String.valueOf(checkDigit);
    }

  /**
   * Checks IBAN is a valid or not.
   *
   * @param iban as a String
   * @return valid state asa boolean type
   * */
    private boolean isValid(String iban) {
        if (iban == null || iban.length() != 26) {
            return false;
        }
        if (!iban.substring(0, 2).equals("TR")) {
            return false;
        }

        String checkDigits = iban.substring(2, 4);
        String bban = iban.substring(4, 26);

        String numericString = bban + "2927" + checkDigits;

        BigInteger number = new BigInteger(numericString);
        BigInteger remainder = number.mod(BigInteger.valueOf(97));

        return remainder.equals(BigInteger.ONE);
    }
}
