package dev.example.crudscan.mybatis;

import dev.example.crudscan.config.AnalyzerConfiguration;
import dev.example.crudscan.model.Models.SqlMapping;
import dev.example.crudscan.sql.SqlClassifier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilderFactory;
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
public class ClasspathMyBatisScanner {
  private static final Logger logger = LoggerFactory.getLogger(ClasspathMyBatisScanner.class);
  private final SqlClassifier classifier = new SqlClassifier();
  private final Path projectRoot;
  private final AnalyzerConfiguration config;

  /**
   * プロジェクトルートを指定して初期化
   *
   * @param projectRoot プロジェクトルートディレクトリ
   */
  public ClasspathMyBatisScanner(Path projectRoot) {
    this.projectRoot = projectRoot;
    this.config = new AnalyzerConfiguration();
  }

  /**
   * プロジェクトルートと設定を指定して初期化
   *
   * @param projectRoot プロジェクトルートディレクトリ
   * @param config アナライザー設定
   */
  public ClasspathMyBatisScanner(Path projectRoot, AnalyzerConfiguration config) {
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
    Path resourcesDir = projectRoot.resolve("src/main/resources");
    if (!Files.exists(resourcesDir)) {
      logger.debug("resourcesディレクトリが存在しません: {}", resourcesDir);
      return;
    }

    try (var stream = Files.walk(resourcesDir)) {
      for (Path xmlFile : stream.filter(f -> f.toString().endsWith(".xml")).toList()) {
        try {
          scanXmlFile(xmlFile, list, "project");
        } catch (Exception ex) {
          logger.debug("プロジェクトXMLファイルの解析をスキップ: {}", xmlFile.getFileName());
        }
      }
    }
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

    try (var stream = Files.walk(directory)) {
      for (Path jarPath :
          stream
              .filter(f -> f.toString().startsWith("common") && f.toString().endsWith(".jar"))
              .toList()) {
        scanJarFile(jarPath, list);
      }
    } catch (IOException ex) {
      logger.error("JARディレクトリのスキャンに失敗: {}", directory);
    }
  }

  /** 個別のJARファイル内のXMLファイルをスキャン */
  private void scanJarFile(Path jarPath, List<SqlMapping> list) {
    try (JarFile jarFile = new JarFile(jarPath.toFile())) {
      Enumeration<JarEntry> entries = jarFile.entries();

      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();

        if (entry.getName().endsWith(".xml") && !entry.isDirectory()) {
          try (InputStream inputStream = jarFile.getInputStream(entry)) {
            scanXmlInputStream(inputStream, list, "jar:" + jarPath.getFileName());
          } catch (Exception ex) {
            logger.debug("JAR内XMLファイルの解析をスキップ: {} in {}", entry.getName(), jarPath.getFileName());
          }
        }
      }
    } catch (IOException ex) {
      logger.debug("JARファイルの読み込みに失敗: {}", jarPath.getFileName());
    }
  }

  /** ファイルシステム上のXMLファイルを解析 */
  private void scanXmlFile(Path xmlFile, List<SqlMapping> list, String source) throws Exception {
    var dbf = DocumentBuilderFactory.newInstance();
    setSecureXmlFeatures(dbf);
    var doc = dbf.newDocumentBuilder().parse(xmlFile.toFile());
    processXmlDocument(doc, list, source + ":" + xmlFile.getFileName());
  }

  /** InputStreamからXMLファイルを解析 */
  private void scanXmlInputStream(InputStream inputStream, List<SqlMapping> list, String source)
      throws Exception {
    var dbf = DocumentBuilderFactory.newInstance();
    setSecureXmlFeatures(dbf);
    var doc = dbf.newDocumentBuilder().parse(inputStream);
    processXmlDocument(doc, list, source);
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

        if (List.of("select", "insert", "update", "delete").contains(tag)) {
          String id = e.getAttribute("id");
          String rawSql = e.getTextContent();

          try {
            var info = classifier.classify(rawSql);
            list.add(new SqlMapping(namespace, id, info.op, rawSql, info.tables));
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

  /** XXE対策: DocumentBuilderFactoryに安全な設定を適用 */
  private static void setSecureXmlFeatures(DocumentBuilderFactory dbf) {
    try {
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
      dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
      dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      dbf.setXIncludeAware(false);
      dbf.setExpandEntityReferences(false);
    } catch (Exception ignore) {
      // セキュリティ設定失敗時は無視（安全性優先）
    }
  }
}
