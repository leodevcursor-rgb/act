package br.com.asa.actsquad.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Converters that store enum names as PostgreSQL enum labels (varchar-compatible bind). */
public final class PgEnumConverter {

  private PgEnumConverter() {}

  @Converter(autoApply = false)
  public static class UserRoleConverter implements AttributeConverter<UserRole, String> {
    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
      return attribute == null ? null : attribute.name();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
      return dbData == null ? null : UserRole.valueOf(dbData);
    }
  }

  @Converter(autoApply = false)
  public static class SubscriptionStatusConverter
      implements AttributeConverter<SubscriptionStatus, String> {
    @Override
    public String convertToDatabaseColumn(SubscriptionStatus attribute) {
      return attribute == null ? null : attribute.name();
    }

    @Override
    public SubscriptionStatus convertToEntityAttribute(String dbData) {
      return dbData == null ? null : SubscriptionStatus.valueOf(dbData);
    }
  }
}
