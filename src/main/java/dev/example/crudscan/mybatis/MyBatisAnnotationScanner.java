package dev.example.crudscan.mybatis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import dev.example.crudscan.enums.SqlOperation;
import dev.example.crudscan.model.Models.SqlMapping;
import dev.example.crudscan.sql.SqlClassifier;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyBatisアノテーションベースマッパーを解析してSQLマッピング情報を抽出するスキャナー
 *
 * <p>このクラスは、プロジェクト内のMyBatisアノテーションベースマッパーインターフェースを解析し、 @Select、@Insert、@Update、@DeleteアノテーションからSQL文とテーブル情報を抽出します。
 *
 * <h2>解析対象</h2>
 *
 * <ul>
 *   <li>@Mapperアノテーションが付与されたインターフェース
 *   <li>@Select、@Insert、@Update、@Deleteアノテーション
 *   <li>アノテーション内のSQL文
 * </ul>
 *
 * <h2>抽出される情報</h2>
 *
 * <ul>
 *   <li>マッパークラス名（インターフェース名）
 *   <li>メソッド名
 *   <li>操作種別（SELECT、INSERT、UPDATE、DELETE）
 *   <li>生SQL文
 *   <li>対象テーブル名のリスト
 * </ul>
 *
 * <h2>使用例</h2>
 *
 * <pre>{@code
 * MyBatisAnnotationScanner scanner = new MyBatisAnnotationScanner(Paths.get("src/main/java"));
 * List<SqlMapping> mappings = scanner.scan();
 *
 * for (SqlMapping mapping : mappings) {
 *   System.out.println(mapping.mapperClass() + "#" + mapping.mapperMethod() + " -> " + mapping.op()
 *       + " " + mapping.tables());
 * }
 * }</pre>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public class MyBatisAnnotationScanner {
  private static final Logger logger = LoggerFactory.getLogger(MyBatisAnnotationScanner.class);

  /** 解析対象のソースディレクトリ */
  private final Path sourceDir;

  /** SQL分類器 */
  private final SqlClassifier classifier = new SqlClassifier();

  /** MyBatisアノテーションの名前セット */
  private static final Set<String> MYBATIS_SQL_ANNOTATIONS =
      Set.of("Select", "Insert", "Update", "Delete");

  /** Mapperアノテーションの名前セット */
  private static final Set<String> MAPPER_ANNOTATIONS = Set.of("Mapper", "Repository");

  /**
   * MyBatisAnnotationScannerを構築
   *
   * @param sourceDir 解析対象のJavaソースディレクトリ
   */
  public MyBatisAnnotationScanner(Path sourceDir) {
    this.sourceDir = sourceDir;
  }

  /**
   * MyBatisアノテーションベースマッパーのスキャンを実行
   *
   * <p>指定されたソースディレクトリ内のすべてのJavaファイルを解析し、 MyBatisアノテーションベースマッパーからSQLマッピング情報を抽出します。
   *
   * <p>解析では以下の処理を行います：
   *
   * <ol>
   *   <li>Javaファイルの検索と読み込み
   *   <li>@Mapperまたは@Repositoryアノテーションが付与されたインターフェースの特定
   *   <li>@Select、@Insert、@Update、@Deleteアノテーションの解析
   *   <li>SQL文からテーブル名を抽出
   *   <li>SqlMappingオブジェクトの生成
   * </ol>
   *
   * @return 抽出されたSQLマッピング情報のリスト
   * @throws IOException ファイル読み込みまたは解析でエラーが発生した場合
   */
  public List<SqlMapping> scan() throws IOException {
    var mappings = new ArrayList<SqlMapping>();
    configureJavaParser();

    try (var stream = Files.walk(sourceDir)) {
      for (Path path : stream.filter(f -> f.toString().endsWith(".java")).toList()) {
        processJavaFile(path, mappings);
      }
    } catch (IOException ex) {
      logger.error("MyBatisAnnotationScanner エラー: {}", ex.getMessage(), ex);
    }

    logger.info("MyBatisアノテーションスキャン完了 - 検出されたSQLマッピング数: {}", mappings.size());
    return mappings;
  }

  /** JavaParserの設定を行う */
  private void configureJavaParser() {
    var config = new com.github.javaparser.ParserConfiguration();
    config.setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21);
    StaticJavaParser.setConfiguration(config);
  }

  /**
   * 単一のJavaファイルを処理する
   *
   * @param path ファイルパス
   * @param mappings SQLマッピングリスト
   */
  private void processJavaFile(Path path, List<SqlMapping> mappings) {
    CompilationUnit cu;
    try {
      cu = StaticJavaParser.parse(path);
    } catch (IOException ex) {
      logger.warn("Javaファイル読み込みエラー: {} - {}", path.getFileName(), ex.getMessage(), ex);
      return;
    } catch (RuntimeException ex) {
      logger.warn("Javaファイル解析エラー: {} - {}", path.getFileName(), ex.getMessage(), ex);
      return;
    }

    for (var cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
      if (isMapperInterface(cls)) {
        processMapperInterface(cls, mappings);
      }
    }
  }

  /**
   * クラスがMapperインターフェースかどうかを判定する
   *
   * @param cls クラス宣言
   * @return Mapperインターフェースの場合true
   */
  private boolean isMapperInterface(ClassOrInterfaceDeclaration cls) {
    if (!cls.isInterface()) {
      return false;
    }

    return cls.getAnnotations().stream()
        .map(AnnotationExpr::getNameAsString)
        .anyMatch(MAPPER_ANNOTATIONS::contains);
  }

  /**
   * Mapperインターフェースを処理する
   *
   * @param cls クラス宣言
   * @param mappings SQLマッピングリスト
   */
  private void processMapperInterface(ClassOrInterfaceDeclaration cls, List<SqlMapping> mappings) {
    String className = cls.getNameAsString();
    logger.debug("Mapperインターフェース処理開始: {}", className);

    for (MethodDeclaration method : cls.getMethods()) {
      processSqlAnnotations(className, method, mappings);
    }
  }

  /**
   * メソッドのSQLアノテーションを処理する
   *
   * @param className クラス名
   * @param method メソッド宣言
   * @param mappings SQLマッピングリスト
   */
  private void processSqlAnnotations(
      String className, MethodDeclaration method, List<SqlMapping> mappings) {
    String methodName = method.getNameAsString();

    for (AnnotationExpr annotation : method.getAnnotations()) {
      String annotationName = annotation.getNameAsString();

      if (MYBATIS_SQL_ANNOTATIONS.contains(annotationName)) {
        processSqlAnnotation(className, methodName, annotation, annotationName, mappings);
      }
    }
  }

  /**
   * 個別のSQLアノテーションを処理する
   *
   * @param className クラス名
   * @param methodName メソッド名
   * @param annotation アノテーション
   * @param annotationName アノテーション名
   * @param mappings SQLマッピングリスト
   */
  private void processSqlAnnotation(
      String className,
      String methodName,
      AnnotationExpr annotation,
      String annotationName,
      List<SqlMapping> mappings) {
    Optional<String> sqlOpt = extractSqlFromAnnotation(annotation);

    if (sqlOpt.isPresent()) {
      String sql = sqlOpt.get();
      String operation = mapAnnotationToOperation(annotationName);

      try {
        var info = classifier.classify(sql);
        mappings.add(new SqlMapping(className, methodName, operation, sql, info.getAllTables()));
        logger.debug(
            "SQLマッピング追加: {}#{} -> {} {}", className, methodName, operation, info.getAllTables());
      } catch (RuntimeException ex) {
        logger.warn(
            "SQL解析エラー: {}#{} - SQL: '{}' - {}",
            className,
            methodName,
            sql.length() > 100 ? sql.substring(0, 100) + "..." : sql,
            ex.getMessage(),
            ex);
        // エラーが発生した場合でも、基本的な情報は保存
        mappings.add(
            new SqlMapping(className, methodName, operation, sql, Collections.emptyList()));
      }
    }
  }

  /**
   * アノテーションからSQL文を抽出する
   *
   * @param annotation アノテーション
   * @return SQL文（存在しない場合はempty）
   */
  private Optional<String> extractSqlFromAnnotation(AnnotationExpr annotation) {
    if (annotation instanceof SingleMemberAnnotationExpr singleMember
        && singleMember.getMemberValue() instanceof StringLiteralExpr stringLiteral) {
      return Optional.of(stringLiteral.getValue());
    }

    // NormalAnnotationExprの場合のvalue属性処理は将来の拡張として保留
    return Optional.empty();
  }

  /**
   * アノテーション名をSQL操作種別にマッピングする
   *
   * @param annotationName アノテーション名
   * @return SQL操作種別
   */
  private String mapAnnotationToOperation(String annotationName) {
    return switch (annotationName) {
      case "Select" -> SqlOperation.SELECT.toString();
      case "Insert" -> SqlOperation.INSERT.toString();
      case "Update" -> SqlOperation.UPDATE.toString();
      case "Delete" -> SqlOperation.DELETE.toString();
      default -> "UNKNOWN";
    };
  }
}
