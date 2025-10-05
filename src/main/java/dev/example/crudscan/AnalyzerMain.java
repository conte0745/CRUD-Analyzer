package dev.example.crudscan;

import dev.example.crudscan.config.AnalyzerConfiguration;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CRUD解析ツールのメインエントリーポイント
 *
 * <p>このクラスは、Spring BootアプリケーションのCRUD操作を解析し、エンドポイントとデータベーステーブル間の関係を 可視化するためのツールです。
 *
 * <h2>機能概要</h2>
 *
 * <ul>
 *   <li>RESTエンドポイントの自動検出
 *   <li>Controller → Service → Repository の呼び出し関係の解析
 *   <li>MyBatis XMLマッピングの解析（JAR内含む）
 *   <li>CRUDマトリクスの生成（Markdown、PlantUML、JSON形式）
 *   <li>パッケージ別のCRUD分析レポート
 * </ul>
 *
 * <h2>使用方法</h2>
 *
 * <pre>{@code
 * java -jar crud-analyzer.jar <src> <resources> <output>
 * }</pre>
 *
 * <h2>引数</h2>
 *
 * <ul>
 *   <li>{@code src} - Javaソースディレクトリのパス
 *   <li>{@code resources} - リソースディレクトリのパス（MyBatis XMLファイル等）
 *   <li>{@code output} - 解析結果の出力ディレクトリのパス
 * </ul>
 *
 * <h2>出力ファイル</h2>
 *
 * <ul>
 *   <li>{@code crud-matrix.md} - 全体のCRUDマトリクス
 *   <li>{@code crud/[package]-crud-matrix.md} - パッケージ別CRUDマトリクス
 *   <li>{@code crud.puml} - PlantUML図
 *   <li>{@code analysis.json} - 解析結果のJSON形式
 * </ul>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public class AnalyzerMain {
  private static final Logger logger = LoggerFactory.getLogger(AnalyzerMain.class);

  /** デフォルトコンストラクタ */
  public AnalyzerMain() {
    // nothing
  }

  /**
   * メイン関数 - 引数を受け取って解析を実行
   *
   * <p>設定ファイル、システムプロパティ、環境変数、コマンドライン引数から設定を読み込み、 {@link CrudAnalyzer}に解析処理を委譲します。
   *
   * <p><strong>設定の優先順位:</strong>
   *
   * <ol>
   *   <li>コマンドライン引数（従来の3引数形式）
   *   <li>システムプロパティ
   *   <li>環境変数
   *   <li>設定ファイル (analyzer-config.properties)
   *   <li>デフォルト値
   * </ol>
   *
   * @param args コマンドライン引数（オプション）
   *     <ul>
   *       <li>args[0] - Javaソースディレクトリのパス（オプション）
   *       <li>args[1] - リソースディレクトリのパス（オプション）
   *       <li>args[2] - 出力ディレクトリのパス（オプション）
   *     </ul>
   *
   * @throws Exception 解析処理中にエラーが発生した場合
   */
  public static void main(String[] args) throws Exception {
    // 設定を読み込み
    AnalyzerConfiguration config = new AnalyzerConfiguration(args);

    // 従来の3引数形式の場合は使用方法を表示（警告のみ）
    if (args.length > 0 && args.length < 3) {
      logger.warn("従来の使用法: java -jar crud-analyzer.jar <src> <resources> <output>");
      logger.warn("新しい使用法: 設定ファイル analyzer-config.properties を使用");
      logger.warn("引数が不足していますが、設定ファイルまたはデフォルト値を使用して続行します");
    }

    // 設定内容をログ出力
    config.logConfiguration();

    Path srcPath = config.getSourceDirectory();
    Path resPath = config.getResourcesDirectory();
    Path outPath = config.getOutputDirectory();

    logger.info("CRUD解析を開始します");

    var analyzer = new CrudAnalyzer();
    analyzer.analyze(srcPath, resPath, outPath);

    logger.info("CRUD解析が完了しました");
  }
}
