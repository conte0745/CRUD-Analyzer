package dev.example.crudscan.mybatis;

import dev.example.crudscan.model.Models.SqlMapping;
import dev.example.crudscan.sql.SqlClassifier;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * MyBatis XMLマッピングファイルを解析してSQLマッピング情報を抽出するスキャナー
 *
 * <p>
 * このクラスは、プロジェクト内のMyBatis XMLファイルを解析し、 SQL文とテーブル情報を抽出してCRUD解析に必要な情報を収集します。
 *
 * <h2>解析対象</h2>
 *
 * <ul>
 * <li>MyBatis XMLマッピングファイル（*.xml）
 * <li>select、insert、update、delete要素
 * <li>SQL文内のテーブル名
 * </ul>
 *
 * <h2>抽出される情報</h2>
 *
 * <ul>
 * <li>マッパークラス名（namespace属性）
 * <li>メソッド名（id属性）
 * <li>操作種別（SELECT、INSERT、UPDATE、DELETE）
 * <li>生SQL文
 * <li>対象テーブル名のリスト
 * </ul>
 *
 * <h2>セキュリティ対策</h2>
 *
 * <ul>
 * <li>XXE（XML External Entity）攻撃対策
 * <li>外部エンティティの無効化
 * <li>安全なXML解析設定
 * </ul>
 *
 * <h2>使用例</h2>
 *
 * <pre>{@code
 * MyBatisXmlScanner scanner = new MyBatisXmlScanner(Paths.get("src/main/resources"));
 * List<SqlMapping> mappings = scanner.scan();
 *
 * for (SqlMapping mapping : mappings) {
 *   System.out.println(mapping.mapperClass() + "#" + mapping.mapperMethod() +
 *       " -> " + mapping.op() + " " + mapping.tables());
 * }
 * }</pre>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public class MyBatisXmlScanner {
  private static final Logger logger = LoggerFactory.getLogger(MyBatisXmlScanner.class);

  /** 解析対象のリソースディレクトリ */
  private final Path resourceDir;

  /** SQL分類器 */
  private final SqlClassifier classifier = new SqlClassifier();

  /**
   * MyBatisXmlScannerを構築
   *
   * @param resourceDir 解析対象のリソースディレクトリ（MyBatis XMLファイルが格納されているディレクトリ）
   */
  public MyBatisXmlScanner(Path resourceDir) {
    this.resourceDir = resourceDir;
  }

  /**
   * MyBatis XMLファイルのスキャンを実行
   *
   * <p>
   * 指定されたリソースディレクトリ内のすべてのXMLファイルを解析し、 MyBatis XMLマッピングファイルからSQLマッピング情報を抽出します。
   *
   * <p>
   * 解析では以下の処理を行います：
   *
   * <ol>
   * <li>XMLファイルの検索と読み込み
   * <li>MyBatis XMLファイルかどうかの判定
   * <li>namespace属性からマッパークラス名を取得
   * <li>select、insert、update、delete要素の解析
   * <li>SQL文からテーブル名を抽出
   * <li>SqlMappingオブジェクトの生成
   * </ol>
   *
   * @return 抽出されたSQLマッピング情報のリスト
   * @throws IOException ファイル読み込みまたはXML解析でエラーが発生した場合
   */
  public List<SqlMapping> scan() throws IOException {
    var list = new ArrayList<SqlMapping>();
    logger.info("MyBatisXmlScanner: スキャン開始 - {}", resourceDir);

    if (!Files.exists(resourceDir)) {
      logger.warn("MyBatisXmlScanner: リソースディレクトリが存在しません: {}", resourceDir);
      return list;
    }

    List<Path> xmlFiles = discoverXmlFiles();
    processXmlFiles(xmlFiles, list);

    logger.info("MyBatisXmlScanner: スキャン完了 - 検出されたSQLマッピング数: {}", list.size());
    return list;
  }

  /**
   * XMLファイルを検索して取得
   *
   * @return 検出されたXMLファイルのリスト
   * @throws IOException ファイルウォークエラーが発生した場合
   */
  private List<Path> discoverXmlFiles() throws IOException {
    try (var stream = Files.walk(resourceDir)) {
      List<Path> xmlFiles = stream.filter(f -> f.toString().endsWith(".xml")).toList();
      logger.info("MyBatisXmlScanner: 検出されたXMLファイル数 - {}", xmlFiles.size());
      return xmlFiles;
    } catch (IOException ex) {
      logger.error("MyBatisXmlScanner エラー: {}", ex.getMessage(), ex);
      throw new IOException("MyBatisXMLスキャンに失敗しました: " + ex.getMessage(), ex);
    }
  }

  /**
   * XMLファイルのリストを処理
   *
   * @param xmlFiles 処理対象のXMLファイルリスト
   * @param list     SQLマッピングを追加するリスト
   */
  private void processXmlFiles(List<Path> xmlFiles, List<SqlMapping> list) {
    for (Path xmlFile : xmlFiles) {
      processXmlFile(xmlFile, list);
    }
  }

  /**
   * 単一のXMLファイルを処理
   *
   * @param xmlFile 処理対象のXMLファイル
   * @param list    SQLマッピングを追加するリスト
   */
  private void processXmlFile(Path xmlFile, List<SqlMapping> list) {
    logger.debug("MyBatisXmlScanner: 処理中 - {}", xmlFile);
    try {
      Document doc = parseXmlDocument(xmlFile);
      String namespace = extractNamespace(doc, xmlFile);

      if (namespace == null) {
        return; // namespace が空の場合はスキップ
      }

      int sqlElementCount = processSqlElements(doc, namespace, list);
      logger.debug("MyBatisXmlScanner: ファイル処理完了 - {} (SQL要素数: {})", xmlFile, sqlElementCount);

    } catch (SAXException ex) {
      logger.error("MyBatisXmlScanner: XML構文エラー - {}: {}", xmlFile, ex.getMessage(), ex);
    } catch (IOException ex) {
      logger.error("MyBatisXmlScanner: ファイル読み込みエラー - {}: {}", xmlFile, ex.getMessage(), ex);
    } catch (ParserConfigurationException ex) {
      logger.error("MyBatisXmlScanner: XMLパーサー設定エラー - {}: {}", xmlFile, ex.getMessage(), ex);
    }
  }

  /**
   * XMLドキュメントを解析
   *
   * @param xmlFile 解析対象のXMLファイル
   * @return 解析されたDocumentオブジェクト
   * @throws SAXException                 XML解析エラーが発生した場合
   * @throws IOException                  ファイル読み込みエラーが発生した場合
   * @throws ParserConfigurationException パーサー設定エラーが発生した場合
   */
  private Document parseXmlDocument(Path xmlFile)
      throws SAXException, IOException, ParserConfigurationException {
    var doc = MyBatisXmlParserFactory.createMyBatisDocumentBuilder().parse(xmlFile.toFile());
    doc.getDocumentElement().normalize();
    return doc;
  }

  /**
   * XMLドキュメントからnamespace属性を抽出
   *
   * @param doc     解析済みのDocumentオブジェクト
   * @param xmlFile ログ出力用のファイルパス
   * @return namespace文字列、空の場合はnull
   */
  private String extractNamespace(Document doc, Path xmlFile) {
    String namespace = Optional.ofNullable(doc.getDocumentElement().getAttribute("namespace")).orElse("");
    if (namespace.isEmpty()) {
      logger.debug("MyBatisXmlScanner: namespaceが空のためスキップ - {}", xmlFile);
      return null;
    }

    logger.debug("MyBatisXmlScanner: namespace検出 - {} in {}", namespace, xmlFile);
    return namespace;
  }

  /**
   * SQL要素（select、insert、update、delete）を処理
   *
   * @param doc       解析済みのDocumentオブジェクト
   * @param namespace マッパーのnamespace
   * @param list      SQLマッピングを追加するリスト
   * @return 処理されたSQL要素数
   */
  private int processSqlElements(Document doc, String namespace, List<SqlMapping> list) {
    NodeList children = doc.getDocumentElement().getChildNodes();
    int sqlElementCount = 0;

    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (isSqlElement(node)) {
        processSqlElement((Element) node, namespace, list);
        sqlElementCount++;
      }
    }

    return sqlElementCount;
  }

  /**
   * ノードがSQL要素かどうかを判定
   *
   * @param node 判定対象のノード
   * @return SQL要素の場合true
   */
  private boolean isSqlElement(Node node) {
    return node.getNodeType() == Node.ELEMENT_NODE
        && List.of("select", "insert", "update", "delete").contains(node.getNodeName());
  }

  /**
   * 単一のSQL要素を処理してSqlMappingを作成
   *
   * @param element   処理対象のSQL要素
   * @param namespace マッパーのnamespace
   * @param list      SQLマッピングを追加するリスト
   */
  private void processSqlElement(Element element, String namespace, List<SqlMapping> list) {
    String id = element.getAttribute("id");
    String rawSql = element.getTextContent();

    try {
      var info = classifier.classify(rawSql);
      list.add(new SqlMapping(namespace, id, info.op, rawSql, info.tables));
      logger.debug(
          "MyBatisXmlScanner: SQLマッピング追加 - {}#{} -> {} {}", namespace, id, info.op, info.tables);
    } catch (RuntimeException sqlEx) {
      logger.warn("MyBatisXmlScanner: SQL解析エラー - {}#{}: {}", namespace, id, sqlEx.getMessage());
    }
  }
}
