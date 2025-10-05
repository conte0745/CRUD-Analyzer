package dev.example.crudscan.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import dev.example.crudscan.model.Models.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * バッチJob（@Configuration付きクラス）をスキャンするクラス
 *
 * <p>指定されたディレクトリを再帰的にスキャンし、@Configuration("XXXXXXJob")アノテーションが 付与されたクラスを検出してバッチJobとして認識します。
 *
 * <h2>検出対象</h2>
 *
 * <ul>
 *   <li>@Configuration("XXXXXXJob")アノテーション付きクラス
 *   <li>Jobで終わる設定値を持つ@Configurationアノテーション
 * </ul>
 *
 * <h2>使用例</h2>
 *
 * <pre>{@code
 * BatchJobScanner scanner = new BatchJobScanner(Paths.get("src/main/java"));
 * List<BatchJob> jobs = scanner.scan();
 * }</pre>
 */
public class BatchJobScanner {
  /** クォートを除去する正規表現 */
  private static final String QUOTE = "(^\"|\"$)";

  private static final Logger logger = LoggerFactory.getLogger(BatchJobScanner.class);
  private final Path sourceDir;
  private final JavaParser parser;

  /**
   * コンストラクタ
   *
   * @param sourceDir スキャン対象のソースディレクトリ
   */
  public BatchJobScanner(Path sourceDir) {
    this.sourceDir = sourceDir;
    this.parser = new JavaParser();
  }

  /**
   * バッチJobをスキャンする
   *
   * @return 検出されたバッチJobのリスト
   */
  public List<BatchJob> scan() {
    List<BatchJob> jobs = new ArrayList<>();

    if (!Files.exists(sourceDir)) {
      logger.warn("ソースディレクトリが存在しません: {}", sourceDir);
      return jobs;
    }

    try (Stream<Path> paths = Files.walk(sourceDir)) {
      paths
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(javaFile -> scanJavaFile(javaFile, jobs));
    } catch (IOException ex) {
      logger.error("ディレクトリスキャン中にエラーが発生しました: {}", sourceDir, ex);
    }

    logger.info("バッチJob検出完了: {}個のJobを発見", jobs.size());
    return jobs;
  }

  /**
   * 単一のJavaファイルをスキャンする
   *
   * @param javaFile スキャン対象のJavaファイル
   * @param jobs 検出されたバッチJobを追加するリスト
   */
  private void scanJavaFile(Path javaFile, List<BatchJob> jobs) {
    try {
      CompilationUnit cu = parseJavaFile(javaFile);
      if (cu == null) return;

      String packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");

      // クラス宣言を検索してバッチJobを処理
      cu.findAll(ClassOrInterfaceDeclaration.class)
          .forEach(classDecl -> processClassDeclaration(classDecl, packageName, jobs));

    } catch (IOException ex) {
      logger.debug("ファイル読み込みエラー: {}", javaFile, ex);
    }
  }

  /**
   * Javaファイルをパースして CompilationUnit を取得する
   *
   * @param javaFile パース対象のJavaファイル
   * @return パース結果のCompilationUnit（失敗時はnull）
   * @throws IOException ファイル読み込みエラー
   */
  private CompilationUnit parseJavaFile(Path javaFile) throws IOException {
    ParseResult<CompilationUnit> result = parser.parse(javaFile);

    if (!result.isSuccessful()) {
      logger.debug("パースに失敗したファイルをスキップ: {}", javaFile);
      return null;
    }

    return result.getResult().orElse(null);
  }

  /**
   * クラス宣言を処理してバッチJobを検出する
   *
   * @param classDecl クラス宣言
   * @param packageName パッケージ名
   * @param jobs 検出されたバッチJobを追加するリスト
   */
  private void processClassDeclaration(
      ClassOrInterfaceDeclaration classDecl, String packageName, List<BatchJob> jobs) {
    if (classDecl.isInterface()) return;

    String className = classDecl.getNameAsString();
    String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;

    // @Configurationアノテーションをチェック
    for (AnnotationExpr annotation : classDecl.getAnnotations()) {
      processBatchJobAnnotation(annotation, fullClassName, packageName, jobs);
    }
  }

  /**
   * バッチJob用のアノテーションを処理する
   *
   * @param annotation 処理対象のアノテーション
   * @param fullClassName 完全修飾クラス名
   * @param packageName パッケージ名
   * @param jobs 検出されたバッチJobを追加するリスト
   */
  private void processBatchJobAnnotation(
      AnnotationExpr annotation, String fullClassName, String packageName, List<BatchJob> jobs) {
    if (!isConfigurationJobAnnotation(annotation)) return;

    String jobName = extractJobName(annotation);
    if (jobName != null) {
      BatchJob job = new BatchJob(fullClassName, jobName, packageName);
      jobs.add(job);
      logger.debug("バッチJob検出: {} ({})", fullClassName, jobName);
    }
  }

  /**
   * `@Configuration`アノテーションがバッチJob用かどうかを判定する
   *
   * @param annotation 検査対象のアノテーション
   * @return バッチJob用の@Configurationアノテーションの場合true
   */
  private boolean isConfigurationJobAnnotation(AnnotationExpr annotation) {
    String annotationName = annotation.getNameAsString();

    // @Configurationアノテーションかチェック
    if (!"Configuration".equals(annotationName)
        && !"org.springframework.context.annotation.Configuration".equals(annotationName)) {
      return false;
    }

    // 値を持つ@Configurationアノテーションかチェック
    if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
      String value = singleMember.getMemberValue().toString();
      // クォートを除去
      value = value.replaceAll(QUOTE, "");
      // "Job"で終わるかチェック
      return value.endsWith("Job");
    }

    return false;
  }

  /**
   * `@Configuration`アノテーションからJob名を抽出する
   *
   * @param annotation @Configurationアノテーション
   * @return Job名（抽出できない場合はnull）
   */
  private String extractJobName(AnnotationExpr annotation) {
    if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
      String value = singleMember.getMemberValue().toString();
      // クォートを除去
      return value.replaceAll(QUOTE, "");
    }
    return null;
  }
}
