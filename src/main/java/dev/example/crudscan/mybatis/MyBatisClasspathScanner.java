package dev.example.crudscan.mybatis;

import dev.example.crudscan.config.AnalyzerConfiguration;
import dev.example.crudscan.model.Models.SqlMapping;
import dev.example.crudscan.sql.SqlClassifier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

/**
 * クラスパス（JAR含む）からMyBatis XMLファイルを検索・解析するスキャナー
 *
 * <p>以下のリソースからMyBatis XMLマッピングを抽出します：
 *
 * <ul>
 *   <li>プロジェクト内のresourcesディレクトリ
 *   <li>依存JARファイル内のXMLファイル
 *   <li>クラスパス上のリソース
 * </ul>
 */
public class MyBatisClasspathScanner {
  private static final Logger logger = LoggerFactory.getLogger(MyBatisClasspathScanner.class);

  // 定数の抽出
  private static final String XML_EXTENSION = ".xml";
  private static final String JAR_EXTENSION = ".jar";
  private static final List<String> SQL_TAGS = List.of("select", "insert", "update", "delete");
  private static final String RESOURCES_PATH = "src/main/resources";

  // ファイルフィルタ
  private static final Predicate<Path> XML_FILE_FILTER =
      path -> path.toString().endsWith(XML_EXTENSION);
  private static final Predicate<Path> JAR_FILE_FILTER =
      path -> path.toString().endsWith(JAR_EXTENSION);

  private final SqlClassifier classifier = new SqlClassifier();
  private final Path projectRoot;
  private final AnalyzerConfiguration config;

  /**
   * プロジェクトルートを指定して初期化
   *
   * @param projectRoot プロジェクトルートディレクトリ
   */
  public MyBatisClasspathScanner(Path projectRoot) {
    this.projectRoot = projectRoot;
    this.config = new AnalyzerConfiguration();
  }

  /**
   * プロジェクトルートと設定を指定して初期化
   *
   * @param projectRoot プロジェクトルートディレクトリ
   * @param config アナライザー設定
   */
  public MyBatisClasspathScanner(Path projectRoot, AnalyzerConfiguration config) {
    this.projectRoot = projectRoot;
    this.config = config;
  }

  /**
   * クラスパス全体からMyBatis XMLマッピングを抽出
   *
   * @return SQLマッピングリスト
   * @throws IOException ファイル操作失敗時
   */
  public List<SqlMapping> scan() throws IOException {
    var list = new ArrayList<SqlMapping>();

    // 1. プロジェクト内のXMLファイルをスキャン
    scanProjectResources(list);

    // 2. 依存JARファイル内のXMLファイルをスキャン
    scanDependencyJars(list);

    logger.info("クラスパススキャン完了 - 検出されたSQLマッピング数: {}", list.size());
    return list;
  }

  /** プロジェクト内のresourcesディレクトリをスキャン */
  private void scanProjectResources(List<SqlMapping> list) throws IOException {
    Path resourcesDir = projectRoot.resolve(RESOURCES_PATH);
    if (!Files.exists(resourcesDir)) {
      logger.debug("resourcesディレクトリが存在しません: {}", resourcesDir);
      return;
    }

    scanXmlFilesInDirectory(resourcesDir, list, "project");
  }

  /** 依存JARファイル内のXMLファイルをスキャン */
  private void scanDependencyJars(List<SqlMapping> list) {
    // AnalyzerConfigurationから固定パスを取得
    List<Path> jarPaths = config.getJarPaths();

    // jar.pathsが空の場合は外部JARを読み込まない
    if (jarPaths.isEmpty()) {
      logger.info("jar.pathsが空のため、外部JARファイルのスキャンをスキップします");
      return;
    }

    for (Path jarPath : jarPaths) {
      if (Files.exists(jarPath)) {
        logger.debug("設定された固定パスをスキャン: {}", jarPath);
        scanJarsInDirectory(jarPath, list);
      } else {
        logger.warn("設定されたJARパスが存在しません: {}", jarPath);
      }
    }
  }

  /** 指定ディレクトリ内のJARファイルをスキャン */
  private void scanJarsInDirectory(Path directory, List<SqlMapping> list) {
    if (!Files.exists(directory)) {
      return;
    }

    executeWithErrorHandling(
        () -> {
          try (var stream = Files.walk(directory)) {
            stream.filter(JAR_FILE_FILTER).forEach(jarPath -> scanJarFile(jarPath, list));
          }
        },
        "JARディレクトリのスキャンに失敗: " + directory);
  }

  /** 個別のJARファイル内のXMLファイルをスキャン */
  private void scanJarFile(Path jarPath, List<SqlMapping> list) {
    executeWithErrorHandling(
        () -> {
          try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Collections.list(jarFile.entries()).stream()
                .filter(entry -> entry.getName().endsWith(XML_EXTENSION) && !entry.isDirectory())
                .forEach(entry -> parseJarEntryWithErrorHandling(jarFile, entry, list, jarPath));
          }
        },
        "JARファイルの読み込みに失敗: " + jarPath.getFileName());
  }

  /** JAR内XMLエントリ解析（エラーハンドリング付き） */
  private void parseJarEntryWithErrorHandling(
      JarFile jarFile, JarEntry entry, List<SqlMapping> list, Path jarPath) {
    executeWithErrorHandling(
        () -> {
          try (InputStream inputStream = jarFile.getInputStream(entry)) {
            parseXmlInputStream(inputStream, list, "jar:" + jarPath.getFileName());
          }
        },
        "JAR内XMLファイルの解析をスキップ: " + entry.getName() + " in " + jarPath.getFileName());
  }

  /** 共通エラーハンドリング */
  private void executeWithErrorHandling(ThrowingRunnable action, String errorMessage) {
    try {
      action.run();
    } catch (Exception ex) {
      logger.debug(errorMessage);
    }
  }

  /** 例外を投げる可能性のあるRunnable */
  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  /** 指定ディレクトリ内のXMLファイルをスキャン */
  private void scanXmlFilesInDirectory(Path directory, List<SqlMapping> list, String source)
      throws IOException {
    try (var stream = Files.walk(directory)) {
      stream
          .filter(XML_FILE_FILTER)
          .forEach(xmlFile -> parseXmlFileWithErrorHandling(xmlFile, list, source));
    }
  }

  /** XMLファイル解析（エラーハンドリング付き） */
  private void parseXmlFileWithErrorHandling(Path xmlFile, List<SqlMapping> list, String source) {
    executeWithErrorHandling(
        () -> parseXmlFile(xmlFile, list, source + ":" + xmlFile.getFileName()),
        "XMLファイルの解析をスキップ: " + xmlFile.getFileName());
  }

  /** XMLファイル解析の統一メソッド */
  private void parseXmlFile(Path xmlFile, List<SqlMapping> list, String source) throws Exception {
    Document doc = createSecureDocumentBuilder().parse(xmlFile.toFile());
    processXmlDocument(doc, list, source);
  }

  /** InputStreamからXML解析の統一メソッド */
  private void parseXmlInputStream(InputStream inputStream, List<SqlMapping> list, String source)
      throws Exception {
    Document doc = createSecureDocumentBuilder().parse(inputStream);
    processXmlDocument(doc, list, source);
  }

  /** セキュアなDocumentBuilderを作成 */
  private javax.xml.parsers.DocumentBuilder createSecureDocumentBuilder() throws Exception {
    return MyBatisXmlParserFactory.createMyBatisDocumentBuilder();
  }

  /** XMLドキュメントを処理してSQLマッピングを抽出 */
  private void processXmlDocument(Document doc, List<SqlMapping> list, String source) {
    doc.getDocumentElement().normalize();
    String namespace =
        Optional.ofNullable(doc.getDocumentElement().getAttribute("namespace")).orElse("");

    // MyBatis XMLかどうかをチェック
    if (!isMyBatisXml(doc)) {
      return;
    }

    logger.debug("MyBatis XMLを検出: {} (namespace: {})", source, namespace);

    NodeList children = doc.getDocumentElement().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        Element e = (Element) n;
        String tag = e.getNodeName();

        if (SQL_TAGS.contains(tag)) {
          String id = e.getAttribute("id");
          String rawSql = e.getTextContent();

          try {
            var info = classifier.classify(rawSql);
            list.add(new SqlMapping(namespace, id, info.op, rawSql, info.getAllTables()));
            logger.debug("SQLマッピング追加: {}#{} ({})", namespace, id, source);
          } catch (Exception ex) {
            logger.debug("SQL解析失敗: {}#{} in {}", namespace, id, source);
          }
        }
      }
    }
  }

  /** MyBatis XMLファイルかどうかを判定 */
  private boolean isMyBatisXml(Document doc) {
    Element root = doc.getDocumentElement();
    String rootTagName = root.getTagName();

    // MyBatis XMLの特徴的な要素をチェック
    return "mapper".equals(rootTagName)
        && (root.hasAttribute("namespace")
            || root.getElementsByTagName("select").getLength() > 0
            || root.getElementsByTagName("insert").getLength() > 0
            || root.getElementsByTagName("update").getLength() > 0
            || root.getElementsByTagName("delete").getLength() > 0);
  }
}
