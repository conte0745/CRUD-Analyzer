package dev.example.crudscan.enums;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SqlOperationのテストクラス */
@DisplayName("SqlOperation機能のテスト")
class SqlOperationTest {

  @Test
  void testValues_ShouldReturnAllOperations() {
    // When
    SqlOperation[] values = SqlOperation.values();

    // Then
    assertThat(values)
        .isNotEmpty()
        .contains(
            SqlOperation.SELECT,
            SqlOperation.INSERT,
            SqlOperation.UPDATE,
            SqlOperation.DELETE,
            SqlOperation.UNKOWN);
  }

  @Test
  void testValueOf_WithValidName_ShouldReturnCorrectEnum() {
    // When & Then
    assertThat(SqlOperation.valueOf("SELECT")).isEqualTo(SqlOperation.SELECT);
    assertThat(SqlOperation.valueOf("INSERT")).isEqualTo(SqlOperation.INSERT);
    assertThat(SqlOperation.valueOf("UPDATE")).isEqualTo(SqlOperation.UPDATE);
    assertThat(SqlOperation.valueOf("DELETE")).isEqualTo(SqlOperation.DELETE);
    assertThat(SqlOperation.valueOf("UNKOWN")).isEqualTo(SqlOperation.UNKOWN);
  }

  @Test
  void testValueOf_WithInvalidName_ShouldThrowException() {
    // When & Then
    assertThatThrownBy(() -> SqlOperation.valueOf("INVALID"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testEnumProperties_ShouldHaveExpectedValues() {
    // When & Then
    assertThat(SqlOperation.SELECT.name()).isEqualTo("SELECT");
    assertThat(SqlOperation.INSERT.name()).isEqualTo("INSERT");
    assertThat(SqlOperation.UPDATE.name()).isEqualTo("UPDATE");
    assertThat(SqlOperation.DELETE.name()).isEqualTo("DELETE");
    assertThat(SqlOperation.UNKOWN.name()).isEqualTo("UNKOWN");
  }

  @Test
  void testOrdinal_ShouldReturnExpectedOrder() {
    // When & Then
    assertThat(SqlOperation.SELECT.ordinal()).isZero();
    assertThat(SqlOperation.INSERT.ordinal()).isEqualTo(1);
    assertThat(SqlOperation.UPDATE.ordinal()).isEqualTo(2);
    assertThat(SqlOperation.DELETE.ordinal()).isEqualTo(3);
    assertThat(SqlOperation.UNKOWN.ordinal()).isEqualTo(4);
  }
}
