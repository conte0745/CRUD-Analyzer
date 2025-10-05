package dev.example.crudscan.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import dev.example.crudscan.model.Models.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * メソッド呼び出し関係を解析してコールグラフを構築するスキャナー
 *
 * <p>このクラスは、Javaソースコードを解析してクラス間・メソッド間の呼び出し関係を抽出し、 CRUD解析に必要なコールグラフを構築します。
 *
 * <h2>解析対象</h2>
 *
 * <ul>
 *   <li>Service、Repository、Mapper、Daoクラス
 *   <li>メソッド内でのメソッド呼び出し（MethodCallExpr）
 *   <li>フィールド経由の呼び出し（依存性注入されたオブジェクト）
 * </ul>
 *
 * <h2>解析機能</h2>
 *
 * <ul>
 *   <li>フィールド名から型名への解決（@Autowired等の依存性注入対応）
 *   <li>メソッド呼び出しの追跡（Service → Repository の関係等）
 *   <li>呼び出し関係の正規化（クラス名#メソッド名 形式）
 * </ul>
 *
 * <h2>使用例</h2>
 *
 * <pre>{@code
 * CallGraphScanner scanner = new CallGraphScanner(Paths.get("src/main/java"));
 * List<CallEdge> callEdges = scanner.scan();
 *
 * for (CallEdge edge : callEdges) {
 *   System.out.println(edge.fromClass() + "#" + edge.fromMethod() +
 *       " -> " + edge.toClass() + "#" + edge.toMethod());
 * }
 * }</pre>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public class CallGraphScanner {
  private static final Logger logger = LoggerFactory.getLogger(CallGraphScanner.class);

  /** 解析対象のクラスサフィックス */
  private static final Set<String> TARGET_CLASS_SUFFIXES =
      Set.of("Service", "Repository", "Mapper", "Dao");

  /** 解析対象のソースディレクトリ */
  private final Path sourceDir;

  /**
   * CallGraphScannerを構築
   *
   * @param sourceDir 解析対象のJavaソースディレクトリ
   */
  public CallGraphScanner(Path sourceDir) {
    this.sourceDir = sourceDir;
  }

  /**
   * コールグラフスキャンを実行
   *
   * <p>指定されたソースディレクトリ内のすべてのJavaファイルを解析し、 メソッド呼び出し関係を抽出してCallEdgeのリストとして返します。
   *
   * <p>解析では以下の処理を行います：
   *
   * <ol>
   *   <li>Javaファイルの解析とASTの構築
   *   <li>クラス内フィールドの型情報収集
   *   <li>メソッド内の呼び出し式の抽出
   *   <li>フィールド名から型名への解決
   *   <li>呼び出し関係の正規化とCallEdge生成
   * </ol>
   *
   * @return 検出されたメソッド呼び出し関係のリスト
   */
  public List<CallEdge> scan() {
    var callEdges = new ArrayList<CallEdge>();
    logger.debug("=== CallGraphScanner デバッグ ===");

    configureJavaParser();

    handleWithErrorLogging(
        () -> {
          try (var stream = Files.walk(sourceDir)) {
            stream
                .filter(f -> f.toString().endsWith(".java"))
                .forEach(javaFile -> processJavaFile(javaFile, callEdges));
          }
        },
        "CallGraphScanner エラー");

    logger.debug("CallGraphScanner 完了 - 検出された呼び出し数: {}", callEdges.size());
    return callEdges;
  }

  /** JavaParserの設定を行う */
  private void configureJavaParser() {
    var config = new com.github.javaparser.ParserConfiguration();
    config.setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21);
    StaticJavaParser.setConfiguration(config);
  }

  /**
   * 単一のJavaファイルを処理してコールエッジを抽出する
   *
   * @param javaFile 処理対象のJavaファイル
   * @param callEdges 抽出されたコールエッジを追加するリスト
   */
  private void processJavaFile(Path javaFile, List<CallEdge> callEdges) {
    try {
      var cu = StaticJavaParser.parse(javaFile);
      var classes = cu.findAll(ClassOrInterfaceDeclaration.class);

      classes.stream().filter(this::isTargetClass).forEach(cls -> processClass(cls, callEdges));
    } catch (IOException e) {
      logError("ファイル処理エラー: " + javaFile, e);
    } catch (RuntimeException e) {
      logError("ファイル解析エラー: " + javaFile, e);
    }
  }

  /**
   * 解析対象のクラスかどうかを判定する
   *
   * @param cls 判定対象のクラス
   * @return 解析対象の場合true
   */
  private boolean isTargetClass(ClassOrInterfaceDeclaration cls) {
    return isTargetClassName(cls.getNameAsString());
  }

  /**
   * クラス内のメソッド呼び出しを処理する
   *
   * @param cls 処理対象のクラス
   * @param callEdges 抽出されたコールエッジを追加するリスト
   */
  private void processClass(ClassOrInterfaceDeclaration cls, List<CallEdge> callEdges) {
    String className = cls.getNameAsString();
    Map<String, String> fieldTypes = buildFieldTypeMap(cls);

    cls.getMethods()
        .forEach(method -> processMethodCalls(className, method, fieldTypes, callEdges));
  }

  /**
   * クラス内のフィールド名から型名へのマッピングを構築する
   *
   * @param cls 処理対象のクラス
   * @return フィールド名から型名へのマップ
   */
  private Map<String, String> buildFieldTypeMap(ClassOrInterfaceDeclaration cls) {
    Map<String, String> fieldTypes = new HashMap<>();

    cls.getFields().stream()
        .flatMap(fd -> fd.getVariables().stream())
        .forEach(vd -> fieldTypes.put(vd.getNameAsString(), vd.getType().asString()));

    return fieldTypes;
  }

  /**
   * メソッド内のメソッド呼び出しを処理する
   *
   * @param className 呼び出し元クラス名
   * @param method 処理対象のメソッド
   * @param fieldTypes フィールド名から型名へのマップ
   * @param callEdges 収集されたコールエッジのリスト
   */
  private void processMethodCalls(
      String className,
      MethodDeclaration method,
      Map<String, String> fieldTypes,
      List<CallEdge> callEdges) {
    String methodName = method.getNameAsString();

    method
        .findAll(MethodCallExpr.class)
        .forEach(call -> processMethodCall(className, methodName, call, fieldTypes, callEdges));
  }

  /**
   * 単一のメソッド呼び出しを処理する
   *
   * @param className 呼び出し元クラス名
   * @param methodName 呼び出し元メソッド名
   * @param call メソッド呼び出し式
   * @param fieldTypes フィールド名から型名へのマップ
   * @param callEdges コールエッジのリスト
   */
  private void processMethodCall(
      String className,
      String methodName,
      MethodCallExpr call,
      Map<String, String> fieldTypes,
      List<CallEdge> callEdges) {
    call.getScope()
        .ifPresent(
            scope -> {
              String targetClass = resolveTargetClass(scope.toString(), fieldTypes);

              if (isTargetMethodCall(targetClass)) {
                String targetMethod = call.getName().getIdentifier();

                logMethodCallDetection(className, methodName, targetClass, targetMethod);
                callEdges.add(new CallEdge(className, methodName, targetClass, targetMethod));
              }
            });
  }

  /**
   * メソッド呼び出し検出のログを出力する
   *
   * @param className 呼び出し元クラス名
   * @param methodName 呼び出し元メソッド名
   * @param targetClass ターゲットクラス名
   * @param targetMethod ターゲットメソッド名
   */
  private void logMethodCallDetection(
      String className, String methodName, String targetClass, String targetMethod) {
    logger.debug(
        "呼び出し検出: {}#{} -> {}#{} (型: {})",
        className,
        methodName,
        targetClass,
        targetMethod,
        targetClass);
  }

  /**
   * スコープ文字列から実際のターゲットクラス名を解決する
   *
   * @param scopeStr スコープ文字列
   * @param fieldTypes フィールド名から型名へのマップ
   * @return 解決されたクラス名
   */
  private String resolveTargetClass(String scopeStr, Map<String, String> fieldTypes) {
    return fieldTypes.getOrDefault(scopeStr, scopeStr);
  }

  /**
   * エラーハンドリングを伴う処理実行
   *
   * @param operation 実行する処理
   * @param errorMessage エラー時のメッセージ
   */
  private void handleWithErrorLogging(IOOperation operation, String errorMessage) {
    try {
      operation.execute();
    } catch (IOException e) {
      logError(errorMessage, e);
    }
  }

  /**
   * エラーログを出力する
   *
   * @param message エラーメッセージ
   * @param e 例外
   */
  private void logError(String message, Exception e) {
    logger.error("{}: {}", message, e.getMessage(), e);
  }

  /** IOException を投げる可能性のある操作を表すインターフェース */
  @FunctionalInterface
  private interface IOOperation {
    void execute() throws IOException;
  }

  /**
   * 対象となるメソッド呼び出しかどうかを判定する
   *
   * @param targetClass ターゲットクラス名
   * @return 対象の場合true
   */
  private boolean isTargetMethodCall(String targetClass) {
    return isTargetClassName(targetClass);
  }

  /**
   * クラス名が解析対象かどうかを判定する
   *
   * @param className 判定対象のクラス名
   * @return 解析対象の場合true
   */
  private boolean isTargetClassName(String className) {
    return TARGET_CLASS_SUFFIXES.stream().anyMatch(className::endsWith);
  }
}
