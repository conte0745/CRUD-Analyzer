package dev.example.crudscan.mybatis;

import dev.example.crudscan.enums.SqlOperation;
import dev.example.crudscan.model.Models.SqlMapping;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * MyBatis Generator特有の機能に対応したスキャナー
 *
 * <p>MyBatis Generatorが生成する以下の要素を解析します：
 *
 * <ul>
 *   <li>Example クラスを使用するメソッド（selectByExample, updateByExample等）
 *   <li>動的SQL要素（&lt;if&gt;, &lt;choose&gt;, &lt;where&gt;等）
 *   <li>自動生成される標準CRUD操作
 * </ul>
 */
public class MyBatisGeneratorScanner {
  private static final Logger logger = LoggerFactory.getLogger(MyBatisGeneratorScanner.class);
  private final Path resourcesRoot;

  /**
   * リソースディレクトリを指定して初期化
   *
   * @param resourcesRoot resourcesディレクトリ
   */
  public MyBatisGeneratorScanner(Path resourcesRoot) {
    this.resourcesRoot = resourcesRoot;
  }

  /**
   * MyBatis Generator特有のSQLマッピングを抽出
   *
   * @return SQLマッピングリスト
   * @throws IOException ファイル操作失敗時
   */
  public List<SqlMapping> scan() throws IOException {
    var list = new ArrayList<SqlMapping>();
    try (var stream = Files.walk(resourcesRoot)) {
      for (Path p : stream.filter(f -> f.toString().endsWith(".xml")).toList()) {
        try {
          scanXmlFile(p, list);
        } catch (Exception ex) {
          logger.debug("XMLファイルの解析をスキップ: {}", p.getFileName());
        }
      }
    }
    return list;
  }

  /** 個別のXMLファイルを解析してSQLマッピングを抽出 */
  private void scanXmlFile(Path xmlFile, List<SqlMapping> list)
      throws ParserConfigurationException, IOException, SAXException {
    var doc = MyBatisXmlParserFactory.createMyBatisDocumentBuilder().parse(xmlFile.toFile());
    doc.getDocumentElement().normalize();

    String namespace =
        Optional.ofNullable(doc.getDocumentElement().getAttribute("namespace")).orElse("");

    // MyBatis Generatorの特徴的なパターンをチェック
    if (isGeneratedMapper(doc)) {
      logger.debug("MyBatis Generator生成マッパーを検出: {}", namespace);
      extractGeneratedMappings(namespace, list);
    }

    // 動的SQL要素を含むマッピングを抽出
    extractDynamicSqlMappings(doc, namespace, list);
  }

  /** MyBatis Generatorで生成されたマッパーかどうかを判定 */
  private boolean isGeneratedMapper(Document doc) {
    NodeList elements = doc.getElementsByTagName("*");
    for (int i = 0; i < elements.getLength(); i++) {
      Element element = (Element) elements.item(i);
      String id = element.getAttribute("id");

      // Generator特有のメソッド名パターンをチェック
      if (id.matches(".*(ByExample|ByPrimaryKey|Selective).*")
          || id.equals("selectByExample")
          || id.equals("updateByExample")
          || id.equals("deleteByExample")
          || id.equals("countByExample")) {
        return true;
      }
    }
    return false;
  }

  /** Generator生成の標準的なCRUD操作を抽出 */
  private void extractGeneratedMappings(String namespace, List<SqlMapping> list) {
    String tableName = extractTableNameFromNamespace(namespace);

    // 標準的なGenerator生成メソッドを推定
    addGeneratedMapping(list, namespace, "selectByPrimaryKey", SqlOperation.SELECT, tableName);
    addGeneratedMapping(list, namespace, "selectByExample", SqlOperation.SELECT, tableName);
    addGeneratedMapping(list, namespace, "insert", SqlOperation.INSERT, tableName);
    addGeneratedMapping(list, namespace, "insertSelective", SqlOperation.INSERT, tableName);
    addGeneratedMapping(list, namespace, "updateByPrimaryKey", SqlOperation.UPDATE, tableName);
    addGeneratedMapping(
        list, namespace, "updateByPrimaryKeySelective", SqlOperation.UPDATE, tableName);
    addGeneratedMapping(list, namespace, "updateByExample", SqlOperation.UPDATE, tableName);
    addGeneratedMapping(
        list, namespace, "updateByExampleSelective", SqlOperation.UPDATE, tableName);
    addGeneratedMapping(list, namespace, "deleteByPrimaryKey", SqlOperation.DELETE, tableName);
    addGeneratedMapping(list, namespace, "deleteByExample", SqlOperation.DELETE, tableName);
    addGeneratedMapping(list, namespace, "countByExample", SqlOperation.SELECT, tableName);
  }

  /** 動的SQL要素を含むマッピングを抽出 */
  private void extractDynamicSqlMappings(Document doc, String namespace, List<SqlMapping> list) {
    NodeList children = doc.getDocumentElement().getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        Element e = (Element) n;
        String tag = e.getNodeName();

        if (List.of("select", "insert", "update", "delete").contains(tag)) {
          String id = e.getAttribute("id");
          SqlOperation operation = SqlOperation.fromString(tag);

          // 動的SQL要素の存在をチェック
          if (containsDynamicSql(e)) {
            logger.debug("動的SQL検出: {}#{}", namespace, id);
            String tableName = extractTableNameFromSql(e.getTextContent());
            list.add(
                new SqlMapping(
                    namespace,
                    id,
                    operation.toString(),
                    e.getTextContent(),
                    tableName != null ? List.of(tableName) : List.of()));
          }
        }
      }
    }
  }

  /** 動的SQL要素が含まれているかチェック */
  private boolean containsDynamicSql(Element element) {
    NodeList descendants = element.getElementsByTagName("*");
    for (int i = 0; i < descendants.getLength(); i++) {
      String tagName = descendants.item(i).getNodeName();
      if (List.of("if", "choose", "when", "otherwise", "where", "set", "foreach", "trim")
          .contains(tagName)) {
        return true;
      }
    }
    return false;
  }

  /** 名前空間からテーブル名を推定 */
  private String extractTableNameFromNamespace(String namespace) {
    if (namespace.isEmpty()) return "unknown_table";

    String[] parts = namespace.split("\\.");
    String className = parts[parts.length - 1];

    // MapperやRepositoryサフィックスを除去
    className = className.replaceAll("(Mapper|Repository)$", "");

    // キャメルケースをスネークケースに変換
    return className.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
  }

  /** SQLからテーブル名を抽出（簡易版） */
  private String extractTableNameFromSql(String sql) {
    // FROM句からテーブル名を抽出する簡易実装
    String[] words = sql.toUpperCase().split("\\s+");
    for (int i = 0; i < words.length - 1; i++) {
      if ("FROM".equals(words[i]) || "UPDATE".equals(words[i]) || "INTO".equals(words[i])) {
        return words[i + 1].replaceAll("[^A-Z_]", "").toLowerCase();
      }
    }
    return null;
  }

  /** 生成されたマッピング情報を追加 */
  private void addGeneratedMapping(
      List<SqlMapping> list,
      String namespace,
      String methodId,
      SqlOperation operation,
      String tableName) {
    String syntheticSql = String.format("-- Generated %s for %s", operation, tableName);
    list.add(
        new SqlMapping(
            namespace, methodId, operation.toString(), syntheticSql, List.of(tableName)));
  }
}
