package dev.example.crudscan;

import dev.example.crudscan.ast.BatchJobScanner;
import dev.example.crudscan.ast.CallGraphScanner;
import dev.example.crudscan.ast.ControllerScanner;
import dev.example.crudscan.ast.ControllerScanner.ScanResult;
import dev.example.crudscan.config.AnalyzerConfiguration;
import dev.example.crudscan.model.Models.*;
import dev.example.crudscan.mybatis.MyBatisAnnotationScanner;
import dev.example.crudscan.mybatis.MyBatisClasspathScanner;
import dev.example.crudscan.mybatis.MyBatisGeneratorScanner;
import dev.example.crudscan.mybatis.MyBatisXmlScanner;
import dev.example.crudscan.output.*;
import dev.example.crudscan.sql.SqlMappingMatcher;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CRUD解析の実行クラス
 *
 * <p>
 * このクラスは、Spring BootアプリケーションのCRUD操作を包括的に解析し、 エンドポイントとデータベーステーブル間の関係を明らかにします。
 *
 * <h2>解析フロー</h2>
 *
 * <ol>
 * <li>コントローラーとコールグラフのスキャン
 * <li>MyBatis XMLマッピングの収集（プロジェクト内、Generator、JAR内）
 * <li>デバッグ情報の出力
 * <li>CRUD解析の実行（エンドポイント→Mapper の到達可能性分析）
 * <li>結果の出力（Markdown、PlantUML、JSON形式）
 * </ol>
 *
 * <h2>対応範囲</h2>
 *
 * <ul>
 * <li>Spring MVC アノテーション（@RequestMapping、@GetMapping等）
 * <li>MyBatis XMLマッピング（プロジェクト内、依存JAR内）
 * <li>MyBatis Generator で生成されたマッピング
 * <li>Controller → Service → Repository/Mapper/Dao の呼び出し関係
 * </ul>
 *
 * <h2>出力形式</h2>
 *
 * <ul>
 * <li>Markdownテーブル形式のCRUDマトリクス
 * <li>パッケージ別のCRUDマトリクス
 * <li>PlantUML形式の関係図
 * <li>JSON形式の詳細データ
 * </ul>
 *
 * <h2>使用例</h2>
 *
 * <pre>{@code
 * // 設定ファイルから自動読み込み
 * CrudAnalyzer analyzer = new CrudAnalyzer();
 * analyzer.analyze(); // 引数不要
 * }</pre>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public class CrudAnalyzer {
  private static final Logger logger = LoggerFactory.getLogger(CrudAnalyzer.class);

  // リテラル定数
  private static final String MAPPER_SUFFIX = "Mapper";
  private static final String SERVICE_SUFFIX = "Service";
  private static final String REPOSITORY_SUFFIX = "Repository";
  private static final String DAO_SUFFIX = "Dao";
  private static final String VIEW_SUFFIX = "View";

  // 依存関係
  private final SqlMappingMatcher sqlMappingMatcher;

  /** デフォルトコンストラクタ */
  public CrudAnalyzer() {
    this.sqlMappingMatcher = new SqlMappingMatcher();
  }

  /**
   * CRUD解析を実行
   *
   * <p>
   * 指定されたディレクトリを解析し、CRUDマトリクスを生成します。 解析結果は複数の形式（Markdown、PlantUML、JSON）で出力されます。
   *
   * <p>
   * 解析処理の詳細:
   *
   * <ol>
   * <li><strong>コントローラースキャン</strong>: Spring MVCアノテーションを解析してRESTエンドポイントを抽出
   * <li><strong>コールグラフ構築</strong>: Service、Repository、Mapper、Daoクラス間の呼び出し関係を解析
   * <li><strong>MyBatis解析</strong>: XMLマッピングファイルからSQL文とテーブル情報を抽出
   * <li><strong>到達可能性分析</strong>: BFS探索でエンドポイントから到達可能なMapperメソッドを特定
   * <li><strong>CRUDマッピング</strong>: SQLマッピングと呼び出し関係をマッチングしてCRUDリンクを生成
   * <li><strong>結果出力</strong>: 複数形式でのCRUDマトリクス生成
   * </ol>
   *
   * 出力ファイル:
   *
   * <ul>
   * <li>{@code crud-matrix.md} - 全体のCRUDマトリクス
   * <li>{@code crud/[package]-crud-matrix.md} - パッケージ別CRUDマトリクス
   * <li>{@code crud.puml} - PlantUML関係図
   * <li>{@code analysis.json} - 解析結果の詳細データ
   * </ul>
   *
   * @param src 解析対象のJavaソースディレクトリ
   * @param res 解析対象のリソースディレクトリ（MyBatis XMLファイル等）
   * @param out 解析結果の出力ディレクトリ
   * @throws IOException ファイル読み込みまたは書き込みでエラーが発生した場合
   */
  public void analyze(Path src, Path res, Path out) throws IOException {
    // 1. コントローラー、バッチJob、コールグラフをスキャン
    var ctrl = new ControllerScanner(src).scan();
    var batchJobs = new BatchJobScanner(src).scan();
    var graphEdges = new CallGraphScanner(src).scan();
    var allCalls = new ArrayList<>(ctrl.calls());
    allCalls.addAll(graphEdges);

    // 2. MyBatis XMLマッピングをスキャン
    var sqls = new ArrayList<SqlMapping>();
    try {
      var xmlSqls = new MyBatisXmlScanner(res).scan();
      sqls.addAll(xmlSqls);
      logger.debug("MyBatis XML SQLマッピング数: {}", xmlSqls.size());
    } catch (IOException ex) {
      logger.error("MyBatis XMLスキャンでエラーが発生しました: {}", ex.getMessage(), ex);
    }

    // MyBatis Generator対応のスキャンも追加
    var generatorSqls = new MyBatisGeneratorScanner(res).scan();
    sqls.addAll(generatorSqls);
    logger.debug("MyBatis Generator SQLマッピング数: {}", generatorSqls.size());

    // MyBatisアノテーションベースマッパーのスキャンも追加
    var annotationSqls = new MyBatisAnnotationScanner(src).scan();
    sqls.addAll(annotationSqls);
    logger.debug("MyBatisアノテーション SQLマッピング数: {}", annotationSqls.size());

    // クラスパス（JAR含む）からのMyBatis XMLスキャンも追加
    var analyzerConfig = new AnalyzerConfiguration();
    var classpathSqls = new MyBatisClasspathScanner(out, analyzerConfig).scan();
    sqls.addAll(classpathSqls);
    logger.debug("クラスパス SQLマッピング数: {}", classpathSqls.size());

    // 3. デバッグ情報出力
    logDebugInfo(ctrl, batchJobs, sqls, graphEdges);

    // 4. CRUD解析実行
    var links = performCrudAnalysis(ctrl, allCalls, sqls);

    // 5. 結果出力
    outputResults(out, ctrl, batchJobs, allCalls, sqls, links);
  }

  /**
   * デバッグ情報をログに出力
   *
   * <p>
   * 解析過程で収集された情報を詳細にログ出力します。 トラブルシューティングや解析結果の検証に使用されます。
   *
   * @param ctrl コントローラースキャンの結果
   * @param batchJobs バッチJobのリスト
   * @param sqls SQLマッピングのリスト
   * @param graphEdges CallGraphScannerで検出された呼び出し関係
   */
  private void logDebugInfo(ScanResult ctrl, List<BatchJob> batchJobs, List<SqlMapping> sqls,
      List<CallEdge> graphEdges) {
    logger.debug("=== 呼び出し関係デバッグ ===");
    logger.debug("Controller呼び出し数: {}", ctrl.calls().size());
    for (var call : ctrl.calls()) {
      logger.debug("呼び出し: {}#{} -> {}#{}", call.fromClass(), call.fromMethod(), call.toClass(),
          call.toMethod());
    }

    logger.debug("=== CallGraph呼び出し関係デバッグ ===");
    logger.debug("CallGraph呼び出し数: {}", graphEdges.size());
    for (var call : graphEdges) {
      String fromClass = call.fromClass();
      String toClass = call.toClass();
      // AbstractView関連の呼び出しを特に注目
      if (fromClass.contains(VIEW_SUFFIX) || toClass.contains(VIEW_SUFFIX)
          || toClass.contains(MAPPER_SUFFIX) || toClass.contains(SERVICE_SUFFIX)) {
        logger.debug("CallGraph呼び出し: {}#{} -> {}#{} (View/Mapper/Service関連)", call.fromClass(),
            call.fromMethod(), call.toClass(), call.toMethod());
      }
    }

    logger.debug("=== バッチJobデバッグ ===");
    logger.debug("バッチJob数: {}", batchJobs.size());
    for (var job : batchJobs) {
      logger.debug("バッチJob: {} ({})", job.className(), job.jobName());
    }

    logger.debug("=== エンドポイント解析デバッグ ===");
    logger.debug("エンドポイント数: {}", ctrl.endpoints().size());
    logger.debug("SQL数: {}", sqls.size());

    logger.debug("=== SQLマッピング情報 ===");
    for (var sql : sqls) {
      logger.debug("SQL: {}#{} -> {} {}", sql.mapperClass(), sql.mapperMethod(), sql.op(),
          sql.tables());
    }
  }

  /**
   * CRUD解析を実行し、CRUDリンクを生成
   *
   * <p>
   * エンドポイントから到達可能なMapperメソッドを特定し、 対応するSQLマッピングとマッチングしてCRUDリンクを生成します。
   *
   * <p>
   * 解析手順:
   *
   * <ol>
   * <li>呼び出し関係から隣接リストを作成
   * <li>各エンドポイントについてBFS探索で到達可能なMapperメソッドを特定
   * <li>SQLマッピングとマッチングしてCRUDリンクを生成
   * </ol>
   *
   * @param ctrl コントローラースキャンの結果
   * @param allCalls 全ての呼び出し関係（Controller+CallGraph）
   * @param sqls SQLマッピングのリスト
   * @return 生成されたCRUDリンクのリスト
   */
  private List<CrudLink> performCrudAnalysis(ScanResult ctrl, List<CallEdge> allCalls,
      List<SqlMapping> sqls) {
    var links = new ArrayList<CrudLink>();
    var adj = createAdjacencyMap(allCalls);

    for (Endpoint ep : ctrl.endpoints()) {
      processEndpointForCrudAnalysis(adj, sqls, links, ep);
    }

    logAnalysisResults(links);
    return links;
  }

  /**
   * 解析結果を各種形式で出力
   *
   * <p>
   * CRUDマトリクスを以下の形式で出力します：
   *
   * <ul>
   * <li>Markdown形式のCRUDマトリクス（全体およびパッケージ別）
   * <li>PlantUML形式の図
   * <li>JSON形式の詳細データ
   * </ul>
   *
   * @param out 出力ディレクトリのパス
   * @param ctrl コントローラースキャンの結果
   * @param batchJobs バッチJobのリスト
   * @param allCalls 全ての呼び出し関係
   * @param sqls SQLマッピングのリスト
   * @param links 生成されたCRUDリンクのリスト
   */
  private void outputResults(Path out, ScanResult ctrl, List<BatchJob> batchJobs,
      List<CallEdge> allCalls, List<SqlMapping> sqls, List<CrudLink> links) {
    var markdownWriter = new MarkdownWriter();
    markdownWriter.writeMatrixByPackage(out, links, ctrl.endpoints());

    // 全体のCRUDマトリクス（パッケージ別への参照リンク集）
    markdownWriter.writeIndexMatrix(out.resolve("crud-matrix.md"), links, ctrl.endpoints(),
        batchJobs);

    new PlantumlWriter().write(out.resolve("crud/crud.puml"), links, batchJobs, allCalls);
    new JsonWriter().write(out.resolve("analysis.json"), ctrl.endpoints(), allCalls, sqls, links,
        batchJobs);

    logger.info("Done. Output -> {}", out);
    logger.info("Generated package-specific CRUD matrices in output/ directory");
  }

  /**
   * 呼び出し関係から隣接リストを作成
   *
   * <p>
   * コントローラースキャンで収集された呼び出し関係を、 グラフ探索用の隣接リスト形式に変換します。
   *
   * @param allCalls 全ての呼び出し関係のリスト
   * @return クラス#メソッド をキーとした隣接リスト
   */
  private Map<String, Set<String>> createAdjacencyMap(List<CallEdge> allCalls) {
    Map<String, Set<String>> adj = new HashMap<>();

    logger.debug("=== 隣接リスト構築開始 - 総呼び出し数: {} ===", allCalls.size());

    for (var e : allCalls) {
      String from = e.fromClass() + "#" + e.fromMethod();
      String to = e.toClass() + "#" + e.toMethod();
      adj.computeIfAbsent(from, k -> new HashSet<>()).add(to);

      // 重要な呼び出し関係をログ出力（Mapper、Service、Repository関連）
      if (containsImportantClass(from) || containsImportantClass(to)) {
        logger.debug("重要な呼び出し関係: {} -> {}", from, to);
      }
    }

    logger.debug("=== 隣接リスト構築完了 - ノード数: {} ===", adj.size());

    // 重要なノードの隣接情報を詳細出力
    adj.entrySet().stream().filter(entry -> containsImportantClass(entry.getKey()))
        .forEach(entry -> {
          logger.debug("重要ノード {} の隣接: {}", entry.getKey(), entry.getValue());
        });

    return adj;
  }

  /**
   * エンドポイントから到達可能なすべてのメソッドを検索（デバッグ用）
   *
   * @param adj 呼び出し関係の隣接リスト
   * @param ep 検索対象のエンドポイント
   * @return 到達可能なすべてのメソッドのセット
   */
  private Set<String> findAllReachableMethods(Map<String, Set<String>> adj, Endpoint ep) {
    var q = new ArrayDeque<String>();
    var visited = new HashSet<String>();
    String start = ep.controller() + "#" + ep.method();
    q.add(start);
    visited.add(start);

    while (!q.isEmpty()) {
      var cur = q.poll();
      for (var nxt : adj.getOrDefault(cur, Set.of())) {
        if (visited.add(nxt)) {
          q.add(nxt);
        }
      }
    }
    return visited;
  }

  /**
   * エンドポイントから到達可能なMapperメソッドを検索
   *
   * <p>
   * 幅優先探索（BFS）を使用して、指定されたエンドポイントから 呼び出し可能なすべてのMapperメソッドを特定します。 AbstractViewなどの中間層も考慮して探索を行います。
   *
   * @param adj 呼び出し関係の隣接リスト
   * @param ep 検索対象のエンドポイント
   * @return 到達可能なMapperメソッドのセット（"クラス名#メソッド名" 形式）
   */
  private Set<String> findReachableMapperMethods(Map<String, Set<String>> adj, Endpoint ep) {
    var q = new ArrayDeque<String>();
    var visited = new HashSet<String>();
    String start = ep.controller() + "#" + ep.method();
    q.add(start);
    visited.add(start);

    var reached = new HashSet<String>();
    logSearchStart(ep, start);

    while (!q.isEmpty()) {
      var cur = q.poll();
      Set<String> neighbors = adj.getOrDefault(cur, Set.of());
      logNeighborInfo(cur, neighbors);

      for (var nxt : neighbors) {
        if (visited.add(nxt)) {
          q.add(nxt);
          processReachableNode(cur, nxt, reached);
        }
      }
    }

    logger.debug("=== エンドポイント {} の到達可能Mapper: {} ===", start, reached);
    return reached;
  }

  /**
   * 探索開始時のログ出力
   */
  private void logSearchStart(Endpoint ep, String start) {
    logger.debug("=== エンドポイント {} の到達可能性探索開始 ===", start);
    logger.debug("エンドポイント詳細: {}", ep);
  }

  /**
   * 隣接ノード情報のログ出力
   */
  private void logNeighborInfo(String current, Set<String> neighbors) {
    if (!neighbors.isEmpty()) {
      logger.debug("  {} から呼び出し可能: {} (隣接数: {})", current, neighbors, neighbors.size());
    }
  }

  /**
   * 到達可能ノードの処理
   */
  private void processReachableNode(String current, String next, Set<String> reached) {
    String className = extractClassName(next);
    logNodeTransition(current, next, className);

    if (isMapperClass(className)) {
      reached.add(next);
      logMapperReached(next);
    } else {
      logIntermediateLayer(next);
    }
  }

  /**
   * ノード遷移のログ出力
   */
  private void logNodeTransition(String current, String next, String className) {
    logger.debug("    探索: {} -> {} (クラス: {})", current, next, className);
  }

  /**
   * Mapper到達時のログ出力
   */
  private void logMapperReached(String mapperMethod) {
    logger.debug("    → Mapper到達: {}", mapperMethod);
  }

  /**
   * 中間層のログ出力
   */
  private void logIntermediateLayer(String method) {
    logger.debug("    → 中間層: {}", method);
  }

  /**
   * 重要なクラス（Mapper、Service、Repository、Dao）を含むかどうかを判定
   */
  private boolean containsImportantClass(String className) {
    return className.contains(MAPPER_SUFFIX) || className.contains(SERVICE_SUFFIX)
        || className.contains(REPOSITORY_SUFFIX) || className.contains(DAO_SUFFIX);
  }

  /**
   * メソッド名からクラス名を抽出
   */
  private String extractClassName(String methodName) {
    return methodName.substring(0, Math.max(0, methodName.indexOf('#')));
  }

  /**
   * Mapperクラスかどうかを判定
   */
  private boolean isMapperClass(String className) {
    return className.endsWith(MAPPER_SUFFIX) || className.endsWith(REPOSITORY_SUFFIX)
        || className.endsWith(DAO_SUFFIX);
  }


  /**
   * 単一エンドポイントのCRUD解析を実行
   *
   * <p>
   * 指定されたエンドポイントから到達可能なMapperメソッドを特定し、 対応するSQLマッピングとマッチングしてCRUDリンクを生成します。
   *
   * @param adj 呼び出し関係の隣接リスト
   * @param sqls SQLマッピングのリスト
   * @param links 生成されたCRUDリンクを追加するリスト
   * @param ep 処理対象のエンドポイント
   */
  private void processEndpointForCrudAnalysis(Map<String, Set<String>> adj, List<SqlMapping> sqls,
      List<CrudLink> links, Endpoint ep) {
    var reachableMappers = findReachableMapperMethods(adj, ep);
    logEndpointDebugInfo(adj, ep, reachableMappers);

    if (!reachableMappers.isEmpty()) {
      sqlMappingMatcher.searchMapper(sqls, links, ep, reachableMappers);
    }
  }

  /**
   * エンドポイントのデバッグ情報をログ出力
   *
   * <p>
   * エンドポイントから到達可能なメソッドとMapperの詳細情報を デバッグログとして出力します。
   *
   * @param adj 呼び出し関係の隣接リスト
   * @param ep 対象のエンドポイント
   * @param reachableMappers 到達可能なMapperメソッドのセット
   */
  private void logEndpointDebugInfo(Map<String, Set<String>> adj, Endpoint ep,
      Set<String> reachableMappers) {
    logger.debug("エンドポイント: {} {} -> 到達Mapper数: {}", ep.url(), ep.httpMethod(),
        reachableMappers.size());

    // デバッグ: 呼び出しパスの詳細を出力
    logger.debug("  開始点: {}#{}", ep.controller(), ep.method());
    var allReachable = findAllReachableMethods(adj, ep);
    logger.debug("  全到達可能メソッド数: {}", allReachable.size());

    for (String method : allReachable) {
      String className = method.substring(0, Math.max(0, method.indexOf('#')));
      if (className.endsWith(VIEW_SUFFIX) || className.endsWith(SERVICE_SUFFIX)
          || className.endsWith(MAPPER_SUFFIX) || className.endsWith(REPOSITORY_SUFFIX)) {
        logger.debug("    到達: {}", method);
      }
    }

    for (String mapperMethod : reachableMappers) {
      logger.debug("  最終到達Mapper: {}", mapperMethod);
    }
  }

  /**
   * 解析結果をログ出力
   *
   * <p>
   * 生成されたCRUDリンクの統計情報と詳細をログ出力します。
   *
   * @param links 生成されたCRUDリンクのリスト
   */
  private void logAnalysisResults(List<CrudLink> links) {
    logger.info("=== 解析結果 ===");
    logger.info("生成されたCRUDリンク数: {}", links.size());

    if (!links.isEmpty()) {
      var tables = links.stream().map(CrudLink::table).distinct().count();
      var endpoints = links.stream().map(CrudLink::ep).distinct().count();
      logger.info("対象テーブル数: {}, 対象エンドポイント数: {}", tables, endpoints);

      logger.debug("=== CRUDリンク詳細（最初の10件） ===");
      for (int i = 0; i < Math.min(10, links.size()); i++) {
        var link = links.get(i);
        logger.debug("  {} {} -> {} ({})", link.ep().url(), link.ep().httpMethod(), link.table(),
            link.crud());
      }
      if (links.size() > 10) {
        logger.debug("  ... 他 {} 件", links.size() - 10);
      }
    } else {
      logger.warn("CRUDリンクが生成されませんでした。呼び出し関係の検出に問題がある可能性があります。");
    }
  }

}
