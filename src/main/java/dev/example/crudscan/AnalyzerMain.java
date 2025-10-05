package dev.example.crudscan;

import dev.example.crudscan.config.AnalyzerConfiguration;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CRUD解析ツールのメインエントリーポイント
 *
 * <p>
 * このクラスは、Spring BootアプリケーションのCRUD操作を解析し、エンドポイントとデータベーステーブル間の関係を 可視化するためのツールです。
 *
 * <h2>機能概要</h2>
 *
 * <ul>
 * <li>RESTエンドポイントの自動検出
 * <li>Controller → Service → Repository の呼び出し関係の解析
 * <li>MyBatis XMLマッピングの解析（JAR内含む）
 * <li>CRUDマトリクスの生成（Markdown、PlantUML、JSON形式）
 * <li>パッケージ別のCRUD分析レポート
 * </ul>
 *
 * <h2>使用方法</h2>
 *
 * <p>
 * 設定ファイル {@code analyzer-config.properties} を作成してから実行します：
 *
 * <pre>{@code
 * // 1. 設定ファイルの作成
 * cp analyzer-config.properties.example analyzer-config.properties
 *
 * // 2. 設定ファイルの編集（パスを実際のプロジェクトに合わせて変更）
 * src.directory=/path/to/your/project/src/main/java
 * resources.directory=/path/to/your/project/src/main/resources
 * output.directory=/path/to/your/project/docs
 *
 * // 3. 実行（引数不要）
 * java -jar crud-analyzer-all.jar
 * }</pre>
 *
 * <h2>出力ファイル</h2>
 *
 * <ul>
 * <li>{@code crud-matrix.md} - 全体のCRUDマトリクス
 * <li>{@code crud/[package]-crud-matrix.md} - パッケージ別CRUDマトリクス
 * <li>{@code crud/crud.puml} - PlantUML図
 * <li>{@code crud/analysis.json} - 解析結果のJSON形式
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
   * <p>
   * 設定ファイルから設定を読み込み、 {@link CrudAnalyzer}に解析処理を委譲します。
   *
   * @throws Exception 解析処理中にエラーが発生した場合
   */
  public static void main(String[] args) throws Exception {
    // 設定を読み込み
    AnalyzerConfiguration config = new AnalyzerConfiguration(args);

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
