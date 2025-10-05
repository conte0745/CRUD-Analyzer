package dev.example.crudscan.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CRUD Analyzer の設定を管理するクラス
 *
 * <p>設定ファイルとコマンドライン引数から設定値を読み込み、優先順位に従って適用します。
 *
 * <p><strong>設定の優先順位:</strong>
 *
 * <ol>
 *   <li>コマンドライン引数
 *   <li>設定ファイル (analyzer-config.properties)
 *   <li>デフォルト値
 * </ol>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public class AnalyzerConfiguration {
  private static final Logger logger = LoggerFactory.getLogger(AnalyzerConfiguration.class);

  private static final String CONFIG_FILE_NAME = "analyzer-config.properties";
  private static final String DEFAULT_SRC_DIR = "src/main/java";
  private static final String DEFAULT_RESOURCES_DIR = "src/main/resources";
  private static final String DEFAULT_OUTPUT_DIR = "output";

  private final Properties properties;
  private final String[] commandLineArgs;

  /** デフォルトコンストラクタ */
  public AnalyzerConfiguration() {
    this(new String[0]);
  }

  /**
   * コマンドライン引数を指定して初期化
   *
   * @param args コマンドライン引数
   */
  public AnalyzerConfiguration(String[] args) {
    this.commandLineArgs = args.clone();
    this.properties = loadConfiguration();
  }

  /**
   * 設定を読み込む
   *
   * @return 読み込まれた設定
   */
  private Properties loadConfiguration() {
    Properties props = new Properties();

    // 設定ファイルからのみ読み込み
    loadFromConfigFile(props);

    return props;
  }

  /** 設定ファイルから設定を読み込む */
  private void loadFromConfigFile(Properties props) {
    Path configFile = Paths.get(CONFIG_FILE_NAME);
    if (Files.exists(configFile)) {
      try (InputStream is = Files.newInputStream(configFile)) {
        props.load(is);
        logger.info("設定ファイルを読み込みました: {}", configFile.toAbsolutePath());
      } catch (IOException ex) {
        logger.warn("設定ファイルの読み込みに失敗しました: {}", configFile, ex);
      }
    } else {
      logger.error("設定ファイルが見つかりません: {}", configFile.toAbsolutePath());
      System.exit(1);
    }
  }

  /**
   * ソースディレクトリのパスを取得
   *
   * @return ソースディレクトリのパス
   */
  public Path getSourceDirectory() {
    // コマンドライン引数が最優先
    if (commandLineArgs.length > 0) {
      return Paths.get(commandLineArgs[0]).toAbsolutePath().normalize();
    }

    String srcDir = properties.getProperty("src.directory", DEFAULT_SRC_DIR);
    return Paths.get(srcDir).toAbsolutePath().normalize();
  }

  /**
   * リソースディレクトリのパスを取得
   *
   * @return リソースディレクトリのパス
   */
  public Path getResourcesDirectory() {
    // コマンドライン引数が最優先
    if (commandLineArgs.length > 1) {
      return Paths.get(commandLineArgs[1]).toAbsolutePath().normalize();
    }

    String resourcesDir = properties.getProperty("resources.directory", DEFAULT_RESOURCES_DIR);
    return Paths.get(resourcesDir).toAbsolutePath().normalize();
  }

  /**
   * 出力ディレクトリのパスを取得
   *
   * @return 出力ディレクトリのパス
   */
  public Path getOutputDirectory() {
    // コマンドライン引数が最優先
    if (commandLineArgs.length > 2) {
      return Paths.get(commandLineArgs[2]).toAbsolutePath().normalize();
    }

    String outputDir = properties.getProperty("output.directory", DEFAULT_OUTPUT_DIR);
    return Paths.get(outputDir).toAbsolutePath().normalize();
  }

  /**
   * 外部JARパスのリストを取得
   *
   * @return JARパスのリスト
   */
  public List<Path> getJarPaths() {
    String jarPaths = properties.getProperty("jar.paths", "");
    if (jarPaths.trim().isEmpty()) {
      return List.of();
    }

    return Arrays.stream(jarPaths.split(System.getProperty("path.separator")))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Paths::get)
        .toList();
  }

  /**
   * ログレベルを取得
   *
   * @return ログレベル
   */
  public String getLogLevel() {
    return properties.getProperty("log.level", "INFO");
  }

  /**
   * 含めるパッケージのリストを取得
   *
   * @return 含めるパッケージのリスト
   */
  public List<String> getIncludePackages() {
    String packages = properties.getProperty("include.packages", "");
    if (packages.trim().isEmpty()) {
      return List.of();
    }

    return Arrays.stream(packages.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  /**
   * 除外するパッケージのリストを取得
   *
   * @return 除外するパッケージのリスト
   */
  public List<String> getExcludePackages() {
    String packages = properties.getProperty("exclude.packages", "");
    if (packages.trim().isEmpty()) {
      return List.of();
    }

    return Arrays.stream(packages.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  /**
   * Markdown出力が有効かどうか
   *
   * @return Markdown出力が有効な場合true
   */
  public boolean isMarkdownOutputEnabled() {
    return Boolean.parseBoolean(properties.getProperty("output.markdown", "true"));
  }

  /**
   * PlantUML出力が有効かどうか
   *
   * @return PlantUML出力が有効な場合true
   */
  public boolean isPlantUmlOutputEnabled() {
    return Boolean.parseBoolean(properties.getProperty("output.plantuml", "true"));
  }

  /**
   * JSON出力が有効かどうか
   *
   * @return JSON出力が有効な場合true
   */
  public boolean isJsonOutputEnabled() {
    return Boolean.parseBoolean(properties.getProperty("output.json", "true"));
  }

  /**
   * 生成されたコードを含めるかどうか
   *
   * @return 生成されたコードを含める場合true
   */
  public boolean isIncludeGenerated() {
    return Boolean.parseBoolean(properties.getProperty("analysis.include.generated", "true"));
  }

  /**
   * 動的SQLを含めるかどうか
   *
   * @return 動的SQLを含める場合true
   */
  public boolean isIncludeDynamicSql() {
    return Boolean.parseBoolean(properties.getProperty("analysis.include.dynamic.sql", "true"));
  }

  /**
   * 解析の最大深度を取得
   *
   * @return 解析の最大深度
   */
  public int getMaxAnalysisDepth() {
    return Integer.parseInt(properties.getProperty("analysis.max.depth", "10"));
  }

  /** 設定の概要をログ出力 */
  public void logConfiguration() {
    logger.info("=== CRUD Analyzer 設定 ===");
    logger.info("ソースディレクトリ: {}", getSourceDirectory());
    logger.info("リソースディレクトリ: {}", getResourcesDirectory());
    logger.info("出力ディレクトリ: {}", getOutputDirectory());
    logger.info("外部JARパス数: {}", getJarPaths().size());
    logger.info("ログレベル: {}", getLogLevel());
    logger.info("Markdown出力: {}", isMarkdownOutputEnabled());
    logger.info("PlantUML出力: {}", isPlantUmlOutputEnabled());
    logger.info("JSON出力: {}", isJsonOutputEnabled());
    logger.info("========================");
  }
}
