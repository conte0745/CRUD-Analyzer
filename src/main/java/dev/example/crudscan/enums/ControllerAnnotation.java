package dev.example.crudscan.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring MVCコントローラーアノテーションの種別を表すEnum
 *
 * <p>Spring Frameworkで使用されるコントローラーアノテーションを型安全に表現するためのEnum定義です。 文字列の直接指定を避け、コードの保守性と可読性を向上させます。
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public enum ControllerAnnotation {
  /** {@code @Controller} アノテーション - 従来のMVCコントローラー */
  CONTROLLER("Controller"),

  /** {@code @RestController} アノテーション - RESTful Webサービス用コントローラー */
  REST_CONTROLLER("RestController");

  /** アノテーション名 */
  private final String annotationName;

  /**
   * コントローラーアノテーションを構築
   *
   * @param annotationName アノテーション名
   */
  ControllerAnnotation(String annotationName) {
    this.annotationName = annotationName;
  }

  /**
   * アノテーション名を取得
   *
   * @return アノテーション名
   */
  public String getAnnotationName() {
    return annotationName;
  }

  /**
   * 全てのコントローラーアノテーション名のセットを取得
   *
   * @return アノテーション名のセット
   */
  public static Set<String> getAnnotationNames() {
    return Arrays.stream(values())
        .map(ControllerAnnotation::getAnnotationName)
        .collect(Collectors.toSet());
  }
}
