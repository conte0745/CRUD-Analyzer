package dev.example.crudscan.enums;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ControllerAnnotationのテストクラス */
@DisplayName("ControllerAnnotation機能のテスト")
class ControllerAnnotationTest {

  @Test
  void testValues_ShouldReturnAllAnnotations() {
    // When
    ControllerAnnotation[] values = ControllerAnnotation.values();

    // Then
    assertThat(values)
        .isNotEmpty()
        .contains(ControllerAnnotation.CONTROLLER, ControllerAnnotation.REST_CONTROLLER);
  }

  @Test
  void testValueOf_WithValidName_ShouldReturnCorrectEnum() {
    // When & Then
    assertThat(ControllerAnnotation.valueOf("CONTROLLER"))
        .isEqualTo(ControllerAnnotation.CONTROLLER);
    assertThat(ControllerAnnotation.valueOf("REST_CONTROLLER"))
        .isEqualTo(ControllerAnnotation.REST_CONTROLLER);
  }

  @Test
  void testValueOf_WithInvalidName_ShouldThrowException() {
    // When & Then
    assertThatThrownBy(() -> ControllerAnnotation.valueOf("INVALID"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testEnumProperties_ShouldHaveExpectedValues() {
    // When & Then
    assertThat(ControllerAnnotation.CONTROLLER.name()).isEqualTo("CONTROLLER");
    assertThat(ControllerAnnotation.REST_CONTROLLER.name()).isEqualTo("REST_CONTROLLER");
  }
}
