package br.com.asa.actsquad.util;

public final class CpfUtil {

  private CpfUtil() {}

  public static String digitsOnly(String raw) {
    if (raw == null) {
      return "";
    }
    return raw.replaceAll("\\D", "");
  }

  /** Valida CPF (11 dígitos + dígitos verificadores). Rejeita sequências iguais. */
  public static boolean isValid(String raw) {
    String cpf = digitsOnly(raw);
    if (cpf.length() != 11) {
      return false;
    }
    if (cpf.chars().distinct().count() == 1) {
      return false;
    }
    try {
      int d1 = checkDigit(cpf, 9, 10);
      int d2 = checkDigit(cpf, 10, 11);
      return d1 == Character.getNumericValue(cpf.charAt(9))
          && d2 == Character.getNumericValue(cpf.charAt(10));
    } catch (Exception e) {
      return false;
    }
  }

  private static int checkDigit(String cpf, int length, int weightStart) {
    int sum = 0;
    for (int i = 0; i < length; i++) {
      sum += Character.getNumericValue(cpf.charAt(i)) * (weightStart - i);
    }
    int mod = sum % 11;
    return mod < 2 ? 0 : 11 - mod;
  }
}
