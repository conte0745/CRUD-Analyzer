package dev.example.crudscan.enums;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** MappingAnnotationのテストクラス */
@DisplayName("MappingAnnotation機能のテスト")
class MappingAnnotationTest {

  @Test
  @DisplayName("各アノテーションの名前が正しく取得できること")
  void testGetAnnotationName_ShouldReturnCorrectNames() {
    // Then
    assertThat(MappingAnnotation.REQUEST_MAPPING.getAnnotationName()).isEqualTo("RequestMapping");
    assertThat(MappingAnnotation.GET_MAPPING.getAnnotationName()).isEqualTo("GetMapping");
    assertThat(MappingAnnotation.POST_MAPPING.getAnnotationName()).isEqualTo("PostMapping");
    assertThat(MappingAnnotation.PUT_MAPPING.getAnnotationName()).isEqualTo("PutMapping");
    assertThat(MappingAnnotation.DELETE_MAPPING.getAnnotationName()).isEqualTo("DeleteMapping");
    assertThat(MappingAnnotation.PATCH_MAPPING.getAnnotationName()).isEqualTo("PatchMapping");
  }

  @Test
  @DisplayName("各アノテーションのHTTPメソッドが正しく取得できること")
  void testGetHttpMethod_ShouldReturnCorrectHttpMethods() {
    // Then
    assertThat(MappingAnnotation.REQUEST_MAPPING.getHttpMethod()).isEmpty();
    assertThat(MappingAnnotation.GET_MAPPING.getHttpMethod()).contains("GET");
    assertThat(MappingAnnotation.POST_MAPPING.getHttpMethod()).contains("POST");
    assertThat(MappingAnnotation.PUT_MAPPING.getHttpMethod()).contains("PUT");
    assertThat(MappingAnnotation.DELETE_MAPPING.getHttpMethod()).contains("DELETE");
    assertThat(MappingAnnotation.PATCH_MAPPING.getHttpMethod()).contains("PATCH");
  }

  @Test
  @DisplayName("全てのアノテーション名のセットが正しく取得できること")
  void testGetAnnotationNames_ShouldReturnAllAnnotationNames() {
    // When
    Set<String> annotationNames = MappingAnnotation.getAnnotationNames();

    // Then
    assertThat(annotationNames).hasSize(6);
    assertThat(annotationNames)
        .containsExactlyInAnyOrder(
            "RequestMapping",
            "GetMapping",
            "PostMapping",
            "PutMapping",
            "DeleteMapping",
            "PatchMapping");
  }

  @Test
  @DisplayName("クラスレベルアノテーション名のセットが正しく取得できること")
  void testGetClassLevelAnnotationNames_ShouldReturnClassLevelAnnotations() {
    // When
    Set<String> classLevelNames = MappingAnnotation.getClassLevelAnnotationNames();

    // Then
    assertThat(classLevelNames).hasSize(1);
    assertThat(classLevelNames).containsExactly("RequestMapping");
  }

  @Test
  @DisplayName("メソッドレベルアノテーション名のセットが正しく取得できること")
  void testGetMethodLevelAnnotationNames_ShouldReturnMethodLevelAnnotations() {
    // When
    Set<String> methodLevelNames = MappingAnnotation.getMethodLevelAnnotationNames();

    // Then
    assertThat(methodLevelNames).hasSize(6);
    assertThat(methodLevelNames)
        .containsExactlyInAnyOrder(
            "RequestMapping",
            "GetMapping",
            "PostMapping",
            "PutMapping",
            "DeleteMapping",
            "PatchMapping");
  }

  @Test
  @DisplayName("指定されたアノテーション名に対応するHTTPメソッドが正しく取得できること")
  void testGetHttpMethodFor_WithValidAnnotationName_ShouldReturnHttpMethod() {
    // When & Then
    assertThat(MappingAnnotation.getHttpMethodFor("RequestMapping")).isEmpty();
    assertThat(MappingAnnotation.getHttpMethodFor("GetMapping")).contains("GET");
    assertThat(MappingAnnotation.getHttpMethodFor("PostMapping")).contains("POST");
    assertThat(MappingAnnotation.getHttpMethodFor("PutMapping")).contains("PUT");
    assertThat(MappingAnnotation.getHttpMethodFor("DeleteMapping")).contains("DELETE");
    assertThat(MappingAnnotation.getHttpMethodFor("PatchMapping")).contains("PATCH");
  }

  @Test
  @DisplayName("存在しないアノテーション名に対してはemptyが返されること")
  void testGetHttpMethodFor_WithInvalidAnnotationName_ShouldReturnEmpty() {
    // When
    Optional<String> result = MappingAnnotation.getHttpMethodFor("NonExistentMapping");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("nullアノテーション名に対してはemptyが返されること")
  void testGetHttpMethodFor_WithNullAnnotationName_ShouldReturnEmpty() {
    // When
    Optional<String> result = MappingAnnotation.getHttpMethodFor(null);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("空文字列アノテーション名に対してはemptyが返されること")
  void testGetHttpMethodFor_WithEmptyAnnotationName_ShouldReturnEmpty() {
    // When
    Optional<String> result = MappingAnnotation.getHttpMethodFor("");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("大文字小文字が異なるアノテーション名に対してはemptyが返されること")
  void testGetHttpMethodFor_WithDifferentCase_ShouldReturnEmpty() {
    // When
    Optional<String> result = MappingAnnotation.getHttpMethodFor("getmapping");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("全てのEnum値が定義されていること")
  void testValues_ShouldContainAllExpectedValues() {
    // When
    MappingAnnotation[] values = MappingAnnotation.values();

    // Then
    assertThat(values).hasSize(6);
    assertThat(values)
        .containsExactlyInAnyOrder(
            MappingAnnotation.REQUEST_MAPPING,
            MappingAnnotation.GET_MAPPING,
            MappingAnnotation.POST_MAPPING,
            MappingAnnotation.PUT_MAPPING,
            MappingAnnotation.DELETE_MAPPING,
            MappingAnnotation.PATCH_MAPPING);
  }

  @Test
  @DisplayName("valueOf()メソッドが正常に動作すること")
  void testValueOf_ShouldReturnCorrectEnumValue() {
    // When & Then
    assertThat(MappingAnnotation.valueOf("REQUEST_MAPPING"))
        .isEqualTo(MappingAnnotation.REQUEST_MAPPING);
    assertThat(MappingAnnotation.valueOf("GET_MAPPING")).isEqualTo(MappingAnnotation.GET_MAPPING);
    assertThat(MappingAnnotation.valueOf("POST_MAPPING")).isEqualTo(MappingAnnotation.POST_MAPPING);
    assertThat(MappingAnnotation.valueOf("PUT_MAPPING")).isEqualTo(MappingAnnotation.PUT_MAPPING);
    assertThat(MappingAnnotation.valueOf("DELETE_MAPPING"))
        .isEqualTo(MappingAnnotation.DELETE_MAPPING);
    assertThat(MappingAnnotation.valueOf("PATCH_MAPPING"))
        .isEqualTo(MappingAnnotation.PATCH_MAPPING);
  }

  @Test
  @DisplayName("存在しない値でvalueOf()を呼び出すと例外が発生すること")
  void testValueOf_WithInvalidValue_ShouldThrowException() {
    // When & Then
    assertThatThrownBy(() -> MappingAnnotation.valueOf("INVALID_MAPPING"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
