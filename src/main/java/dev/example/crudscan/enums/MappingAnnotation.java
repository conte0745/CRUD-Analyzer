package dev.example.crudscan.enums;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring MVCマッピングアノテーションの種別を表すEnum
 *
 * <p>Spring Frameworkで使用されるHTTPリクエストマッピングアノテーションを型安全に表現するためのEnum定義です。
 * 各アノテーションに対応するHTTPメソッドの情報も保持し、RESTエンドポイントの解析に使用されます。
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public enum MappingAnnotation {
  /** `@RequestMapping` アノテーション - 汎用マッピング（HTTPメソッド指定なし） */
  REQUEST_MAPPING("RequestMapping", null),

  /** `@GetMapping` アノテーション - GETリクエスト用 */
  GET_MAPPING("GetMapping", "GET"),

  /** `@PostMapping` アノテーション - POSTリクエスト用 */
  POST_MAPPING("PostMapping", "POST"),

  /** `@PutMapping` アノテーション - PUTリクエスト用 */
  PUT_MAPPING("PutMapping", "PUT"),

  /** `@DeleteMapping` アノテーション - DELETEリクエスト用 */
  DELETE_MAPPING("DeleteMapping", "DELETE"),

  /** `@PatchMapping` アノテーション - PATCHリクエスト用 */
  PATCH_MAPPING("PatchMapping", "PATCH");

  /** アノテーション名 */
  private final String annotationName;

  /** 対応するHTTPメソッド（RequestMappingの場合はnull） */
  private final String httpMethod;

  /**
   * マッピングアノテーションを構築
   *
   * @param annotationName アノテーション名
   * @param httpMethod 対応するHTTPメソッド（RequestMappingの場合はnull）
   */
  MappingAnnotation(String annotationName, String httpMethod) {
    this.annotationName = annotationName;
    this.httpMethod = httpMethod;
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
   * 対応するHTTPメソッドを取得
   *
   * @return HTTPメソッド（RequestMappingの場合はempty）
   */
  public Optional<String> getHttpMethod() {
    return Optional.ofNullable(httpMethod);
  }

  /**
   * 全てのマッピングアノテーション名のセットを取得
   *
   * @return アノテーション名のセット
   */
  public static Set<String> getAnnotationNames() {
    return Arrays.stream(values())
        .map(MappingAnnotation::getAnnotationName)
        .collect(Collectors.toSet());
  }

  /**
   * クラスレベルで使用可能なアノテーション名のセットを取得
   *
   * @return クラスレベルアノテーション名のセット
   */
  public static Set<String> getClassLevelAnnotationNames() {
    return Set.of(REQUEST_MAPPING.getAnnotationName());
  }

  /**
   * メソッドレベルで使用可能なアノテーション名のセットを取得
   *
   * @return メソッドレベルアノテーション名のセット
   */
  public static Set<String> getMethodLevelAnnotationNames() {
    return Arrays.stream(values())
        .map(MappingAnnotation::getAnnotationName)
        .collect(Collectors.toSet());
  }

  /**
   * 指定されたアノテーション名に対応するHTTPメソッドを取得
   *
   * @param annotationName アノテーション名
   * @return 対応するHTTPメソッド（見つからない場合やRequestMappingの場合はempty）
   */
  public static Optional<String> getHttpMethodFor(String annotationName) {
    return Arrays.stream(values())
        .filter(annotation -> annotation.getAnnotationName().equals(annotationName))
        .findFirst()
        .flatMap(MappingAnnotation::getHttpMethod);
  }
}
