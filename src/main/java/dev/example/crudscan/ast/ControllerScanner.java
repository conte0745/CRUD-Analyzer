package dev.example.crudscan.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import dev.example.crudscan.enums.ControllerAnnotation;
import dev.example.crudscan.enums.MappingAnnotation;
import dev.example.crudscan.model.Models.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring MVCコントローラーの解析とエンドポイント抽出を行うスキャナー
 *
 * <p>このクラスは、Javaソースコードを解析してSpring MVCコントローラーを特定し、 RESTエンドポイントとメソッド呼び出し関係を抽出します。
 *
 * <h2>解析対象</h2>
 *
 * <ul>
 *   <li>@Controller、@RestController アノテーションが付与されたクラス
 *   <li>@RequestMapping、@GetMapping、@PostMapping 等のマッピングアノテーション
 *   <li>コントローラーメソッド内からのService、Mapperメソッド呼び出し
 * </ul>
 *
 * <h2>抽出される情報</h2>
 *
 * <ul>
 *   <li>RESTエンドポイント（URL、HTTPメソッド、コントローラー、メソッド名、パッケージ）
 *   <li>メソッド呼び出し関係（呼び出し元→呼び出し先）
 * </ul>
 *
 * <h2>使用例</h2>
 *
 * <pre>{@code
 * ControllerScanner scanner = new ControllerScanner(Paths.get("src/main/java"));
 * ScanResult result = scanner.scan();
 *
 * // エンドポイント一覧
 * for (Endpoint ep : result.endpoints()) {
 *   System.out.println(ep.httpMethod() + " " + ep.url());
 * }
 *
 * // 呼び出し関係
 * for (CallEdge call : result.calls()) {
 *   System.out.println(call.fromClass() + " -> " + call.toClass());
 * }
 * }</pre>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public class ControllerScanner {
  private static final Logger logger = LoggerFactory.getLogger(ControllerScanner.class);

  /** 解析対象のソースディレクトリ */
  private final Path sourceDir;

  /** 対象となるサービスクラスのサフィックス */
  private static final Set<String> TARGET_SERVICE_SUFFIXES = Set.of("Service", "Mapper", "Dao");

  /**
   * ControllerScannerを構築
   *
   * @param sourceDir 解析対象のJavaソースディレクトリ
   */
  public ControllerScanner(Path sourceDir) {
    this.sourceDir = sourceDir;
  }

  /**
   * 指定されたアノテーションのいずれかが存在するかチェック
   *
   * @param required 必要なアノテーション名リスト
   * @param annotations 対象アノテーション
   * @return 存在すればtrue
   */
  public static boolean hasAnyAnnotation(
      Set<String> required, NodeList<AnnotationExpr> annotations) {
    return annotations.stream().map(AnnotationExpr::getNameAsString).anyMatch(required::contains);
  }

  /**
   * Mappingアノテーションからパス値を抽出
   *
   * @param ann アノテーション
   * @return パス値（なければempty）
   */
  public static Optional<String> readMappingValue(AnnotationExpr ann) {
    if (ann instanceof SingleMemberAnnotationExpr s
        && s.getMemberValue() instanceof StringLiteralExpr str) {
      return Optional.of(str.getValue());
    }
    if (ann instanceof NormalAnnotationExpr n) {
      for (MemberValuePair p : n.getPairs()) {
        if ((p.getNameAsString().equals("value") || p.getNameAsString().equals("path"))
            && p.getValue() instanceof StringLiteralExpr str) {
          return Optional.of(str.getValue());
        }
      }
    }
    return Optional.empty();
  }

  /**
   * アノテーション名からHTTPメソッドを推定
   *
   * @param annotationName アノテーション名
   * @return HTTPメソッド（推定できない場合はempty）
   */
  public static Optional<String> httpMethodForAnno(String annotationName) {
    return MappingAnnotation.getHttpMethodFor(annotationName);
  }

  /**
   * コントローラースキャンを実行
   *
   * <p>指定されたソースディレクトリ内のすべてのJavaファイルを解析し、 コントローラークラスとそのエンドポイント、メソッド呼び出し関係を抽出します。
   *
   * @return スキャン結果（エンドポイントリストと呼び出し関係リスト）
   */
  public ScanResult scan() {
    var endpoints = new ArrayList<Endpoint>();
    var calls = new ArrayList<CallEdge>();

    configureJavaParser();
    processJavaFiles(endpoints, calls);

    return new ScanResult(endpoints, calls);
  }

  /** JavaParserの設定を行う */
  private void configureJavaParser() {
    var config = new com.github.javaparser.ParserConfiguration();
    config.setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21);
    StaticJavaParser.setConfiguration(config);
  }

  /**
   * Javaファイルを処理してエンドポイントと呼び出し関係を抽出する
   *
   * @param endpoints エンドポイントリスト
   * @param calls 呼び出し関係リスト
   */
  private void processJavaFiles(List<Endpoint> endpoints, List<CallEdge> calls) {
    try (var stream = Files.walk(sourceDir)) {
      for (Path path : stream.filter(f -> f.toString().endsWith(".java")).toList()) {
        processJavaFile(path, endpoints, calls);
      }
    } catch (IOException ex) {
      logger.error("ControllerScanner エラー: {}", ex.getMessage());
    }
  }

  /**
   * 単一のJavaファイルを処理する
   *
   * @param path ファイルパス
   * @param endpoints エンドポイントリスト
   * @param calls 呼び出し関係リスト
   */
  private void processJavaFile(Path path, List<Endpoint> endpoints, List<CallEdge> calls) {
    CompilationUnit cu;
    try {
      cu = StaticJavaParser.parse(path);
    } catch (IOException ex) {
      logger.debug("ファイル読み込みエラー: {} - {}", path.getFileName(), ex.getMessage());
      return;
    } catch (RuntimeException ex) {
      logger.debug("ファイル解析エラー: {} - {}", path.getFileName(), ex.getMessage());
      return;
    }

    for (var cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
      if (shouldProcessClass(cls)) {
        processControllerClass(cu, cls, endpoints, calls);
      }
    }
  }

  /**
   * クラスを処理対象とするかどうかを判定する
   *
   * @param cls クラス宣言
   * @return 処理対象の場合true
   */
  private boolean shouldProcessClass(ClassOrInterfaceDeclaration cls) {
    return hasAnyAnnotation(ControllerAnnotation.getAnnotationNames(), cls.getAnnotations())
        || hasAnyAnnotation(MappingAnnotation.getClassLevelAnnotationNames(), cls.getAnnotations());
  }

  /**
   * コントローラークラスを処理する
   *
   * @param cu コンパイル単位
   * @param cls クラス宣言
   * @param endpoints エンドポイントリスト
   * @param calls 呼び出し関係リスト
   */
  private void processControllerClass(
      CompilationUnit cu,
      ClassOrInterfaceDeclaration cls,
      List<Endpoint> endpoints,
      List<CallEdge> calls) {
    Map<String, String> fieldTypes = buildFieldTypeMap(cls);
    String classBasePath = resolveClassBasePath(cls);
    String packageName = resolvePackageName(cu);

    processMethods(cls, fieldTypes, classBasePath, packageName, endpoints, calls);
  }

  /**
   * フィールド名から型名へのマッピングを構築する
   *
   * @param cls クラス宣言
   * @return フィールド名→型名のマップ
   */
  private Map<String, String> buildFieldTypeMap(ClassOrInterfaceDeclaration cls) {
    Map<String, String> fieldTypes = new HashMap<>();
    for (FieldDeclaration fd : cls.getFields()) {
      for (VariableDeclarator vd : fd.getVariables()) {
        fieldTypes.put(vd.getNameAsString(), vd.getType().asString());
      }
    }
    return fieldTypes;
  }

  /**
   * クラスレベルのベースパスを解決する
   *
   * @param cls クラス宣言
   * @return ベースパス
   */
  private String resolveClassBasePath(ClassOrInterfaceDeclaration cls) {
    return cls.getAnnotations().stream()
        .filter(a -> a.getNameAsString().equals("RequestMapping"))
        .findFirst()
        .flatMap(ControllerScanner::readMappingValue)
        .orElse("");
  }

  /**
   * パッケージ名を解決する
   *
   * @param cu コンパイル単位
   * @return パッケージ名
   */
  private String resolvePackageName(CompilationUnit cu) {
    return cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("default");
  }

  /**
   * メソッドを処理してエンドポイントと呼び出し関係を抽出する
   *
   * @param cls クラス宣言
   * @param fieldTypes フィールド型マップ
   * @param classBasePath クラスベースパス
   * @param packageName パッケージ名
   * @param endpoints エンドポイントリスト
   * @param calls 呼び出し関係リスト
   */
  private void processMethods(
      ClassOrInterfaceDeclaration cls,
      Map<String, String> fieldTypes,
      String classBasePath,
      String packageName,
      List<Endpoint> endpoints,
      List<CallEdge> calls) {
    for (MethodDeclaration method : cls.getMethods()) {
      var mappingAnnotation = findMappingAnnotation(method);
      if (mappingAnnotation.isPresent()) {
        var context = new MethodProcessingContext(cls, classBasePath, packageName, fieldTypes);
        processMethodWithMapping(method, mappingAnnotation.get(), context, endpoints, calls);
      }
    }
  }

  /**
   * メソッドのマッピングアノテーションを検索する
   *
   * @param method メソッド宣言
   * @return マッピングアノテーション
   */
  private Optional<AnnotationExpr> findMappingAnnotation(MethodDeclaration method) {
    return method.getAnnotations().stream()
        .filter(
            a -> MappingAnnotation.getMethodLevelAnnotationNames().contains(a.getNameAsString()))
        .findFirst();
  }

  /**
   * マッピングアノテーションを持つメソッドを処理する
   *
   * @param method メソッド宣言
   * @param mappingAnnotation マッピングアノテーション
   * @param context メソッド処理コンテキスト
   * @param endpoints エンドポイントリスト
   * @param calls 呼び出し関係リスト
   */
  private void processMethodWithMapping(
      MethodDeclaration method,
      AnnotationExpr mappingAnnotation,
      MethodProcessingContext context,
      List<Endpoint> endpoints,
      List<CallEdge> calls) {
    Endpoint endpoint =
        createEndpoint(
            context.cls, method, mappingAnnotation, context.classBasePath, context.packageName);
    endpoints.add(endpoint);

    processMethodCalls(context.cls, method, context.fieldTypes, calls);
  }

  /**
   * エンドポイントを作成する
   *
   * @param cls クラス宣言
   * @param method メソッド宣言
   * @param method メソッド宣言
   * @param mappingAnnotation マッピングアノテーション
   * @param classBasePath クラスベースパス
   * @param packageName パッケージ名
   * @return エンドポイント
   */
  private Endpoint createEndpoint(
      ClassOrInterfaceDeclaration cls,
      MethodDeclaration method,
      AnnotationExpr mappingAnnotation,
      String classBasePath,
      String packageName) {
    String methodPath = readMappingValue(mappingAnnotation).orElse("");
    String url = ("/" + classBasePath + "/" + methodPath).replaceAll("/+", "/");
    String httpMethod = httpMethodForAnno(mappingAnnotation.getNameAsString()).orElse("GET");

    return new Endpoint(
        httpMethod, url, cls.getNameAsString(), method.getNameAsString(), packageName);
  }

  /**
   * メソッド内の呼び出しを処理する
   *
   * @param cls クラス宣言
   * @param method メソッド宣言
   * @param fieldTypes フィールド型マップ
   * @param calls 呼び出し関係リスト
   */
  private void processMethodCalls(
      ClassOrInterfaceDeclaration cls,
      MethodDeclaration method,
      Map<String, String> fieldTypes,
      List<CallEdge> calls) {
    for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
      processMethodCall(cls, method, call, fieldTypes, calls);
    }
  }

  /**
   * 個別のメソッド呼び出しを処理する
   *
   * @param cls クラス宣言
   * @param method メソッド宣言
   * @param call メソッド呼び出し式
   * @param fieldTypes フィールド型マップ
   * @param calls 呼び出し関係リスト
   */
  private void processMethodCall(
      ClassOrInterfaceDeclaration cls,
      MethodDeclaration method,
      MethodCallExpr call,
      Map<String, String> fieldTypes,
      List<CallEdge> calls) {
    var scopeOpt = call.getScope();
    if (scopeOpt.isEmpty()) return;

    String targetClass = resolveTargetClass(scopeOpt.get().toString(), fieldTypes);
    if (shouldRecordCall(targetClass)) {
      calls.add(
          new CallEdge(
              cls.getNameAsString(),
              method.getNameAsString(),
              targetClass,
              call.getName().getIdentifier()));
    }
  }

  /**
   * ターゲットクラスを解決する
   *
   * @param scopeString スコープ文字列
   * @param fieldTypes フィールド型マップ
   * @return 解決されたクラス名
   */
  private String resolveTargetClass(String scopeString, Map<String, String> fieldTypes) {
    return fieldTypes.getOrDefault(scopeString, scopeString);
  }

  /**
   * 呼び出しを記録すべきかどうかを判定する
   *
   * @param targetClass ターゲットクラス名
   * @return 記録すべき場合true
   */
  private boolean shouldRecordCall(String targetClass) {
    return isTargetServiceClass(targetClass);
  }

  /** メソッド処理のコンテキスト情報を保持するレコード */
  private record MethodProcessingContext(
      ClassOrInterfaceDeclaration cls,
      String classBasePath,
      String packageName,
      Map<String, String> fieldTypes) {}

  /**
   * スキャン結果を格納するレコード
   *
   * @param endpoints 検出されたRESTエンドポイントのリスト
   * @param calls 検出されたメソッド呼び出し関係のリスト
   */
  /**
   * サービスクラスかどうかを判定する
   *
   * @param className クラス名
   * @return サービスクラスの場合true
   */
  private boolean isTargetServiceClass(String className) {
    return TARGET_SERVICE_SUFFIXES.stream().anyMatch(className::endsWith);
  }

  /**
   * コントローラースキャンの結果を格納するレコード
   *
   * @param endpoints 検出されたエンドポイントのリスト
   * @param calls 検出されたメソッド呼び出し関係のリスト
   */
  public record ScanResult(List<Endpoint> endpoints, List<CallEdge> calls) {}
}
