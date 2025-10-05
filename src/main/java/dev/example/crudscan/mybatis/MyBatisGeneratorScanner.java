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
 * <p>このクラスは、MyBatis Generatorによって自動生成されたXMLマッピングファイルを解析し、 標準的なCRUD操作や動的SQL要素を検出・抽出します。
 *
 * <h2>解析対象の要素</h2>
 *
 * <ul>
 *   <li><strong>Example クラス使用メソッド</strong>: selectByExample, updateByExample, deleteByExample等
 *   <li><strong>Primary Key操作</strong>: selectByPrimaryKey, updateByPrimaryKey, deleteByPrimaryKey等
 *   <li><strong>Selective操作</strong>: insertSelective, updateByPrimaryKeySelective等
 *   <li><strong>動的SQL要素</strong>: &lt;if&gt;, &lt;choose&gt;, &lt;when&gt;, &lt;otherwise&gt;,
 *       &lt;where&gt;, &lt;set&gt;, &lt;foreach&gt;, &lt;trim&gt;
 *   <li><strong>標準CRUD操作</strong>: insert, select, update, delete
 * </ul>
 *
 * <h2>検出ロジック</h2>
 *
 * <p>MyBatis Generatorで生成されたマッパーの判定は、以下の特徴的なメソッド名パターンに基づいて行われます：
 *
 * <ul>
 *   <li>メソッド名が "ByExample", "ByPrimaryKey", "Selective" を含む
 *   <li>標準的なGenerator生成メソッド名（selectByExample, countByExample等）
 * </ul>
 *
 * <h2>使用例</h2>
 *
 * <pre>{@code
 * Path resourcesDir = Paths.get("src/main/resources");
 * MyBatisGeneratorScanner scanner = new MyBatisGeneratorScanner(resourcesDir);
 * List<SqlMapping> mappings = scanner.scan();
 *
 * for (SqlMapping mapping : mappings) {
 *     System.out.println("Namespace: " + mapping.namespace());
 *     System.out.println("Method: " + mapping.methodId());
 *     System.out.println("Operation: " + mapping.operation());
 * }
 * }</pre>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 * @see MyBatisXmlParserFactory
 * @see SqlMapping
 * @see SqlOperation
 */
public class MyBatisGeneratorScanner {
  private static final Logger logger = LoggerFactory.getLogger(MyBatisGeneratorScanner.class);
  private final Path resourcesRoot;

  /**
   * MyBatis Generator スキャナーを初期化します
   *
   * <p>指定されたリソースディレクトリ配下のXMLファイルを再帰的にスキャンし、 MyBatis Generatorで生成されたマッピングファイルを検出します。
   *
   * @param resourcesRoot MyBatis XMLマッピングファイルが格納されているリソースディレクトリのパス 通常は "src/main/resources" を指定します
   * @throws IllegalArgumentException resourcesRootがnullの場合
   * @see #scan()
   */
  public MyBatisGeneratorScanner(Path resourcesRoot) {
    if (resourcesRoot == null) {
      throw new IllegalArgumentException("resourcesRoot cannot be null");
    }
    this.resourcesRoot = resourcesRoot;
  }

  /**
   * MyBatis Generator特有のSQLマッピングを抽出します
   *
   * <p>リソースディレクトリ配下の全XMLファイルを走査し、MyBatis Generatorで生成された マッピングファイルを検出して、SQLマッピング情報を抽出します。
   *
   * <h3>抽出される情報</h3>
   *
   * <ul>
   *   <li>名前空間（namespace）
   *   <li>メソッドID（selectByExample, insertSelective等）
   *   <li>SQL操作タイプ（SELECT, INSERT, UPDATE, DELETE）
   *   <li>対象テーブル名（推定値）
   *   <li>SQL文（実際のSQLまたは合成されたSQL）
   * </ul>
   *
   * <h3>処理フロー</h3>
   *
   * <ol>
   *   <li>XMLファイルの検索と読み込み
   *   <li>MyBatis Generator生成ファイルの判定
   *   <li>標準CRUD操作の抽出
   *   <li>動的SQL要素を含むマッピングの抽出
   * </ol>
   *
   * @return 検出されたSQLマッピングのリスト。見つからない場合は空のリスト
   * @throws IOException ファイルの読み込みやディレクトリの走査に失敗した場合
   * @see SqlMapping
   * @see #scanXmlFile(Path, List)
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

  /**
   * 個別のXMLファイルを解析してSQLマッピングを抽出します
   *
   * <p>指定されたXMLファイルを解析し、MyBatis Generatorで生成されたマッピングかどうかを判定します。
   * 生成されたマッピングの場合は標準的なCRUD操作を抽出し、動的SQL要素も検出します。
   *
   * @param xmlFile 解析対象のXMLファイルパス
   * @param list SQLマッピングを追加するリスト
   * @throws ParserConfigurationException XMLパーサーの設定エラー
   * @throws IOException ファイル読み込みエラー
   * @throws SAXException XML解析エラー
   * @see #isGeneratedMapper(Document)
   * @see #extractGeneratedMappings(String, List)
   * @see #extractDynamicSqlMappings(Document, String, List)
   */
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

  /**
   * MyBatis Generatorで生成されたマッパーかどうかを判定します
   *
   * <p>XMLドキュメント内の要素を走査し、MyBatis Generatorの特徴的なメソッド名パターンを検索します。
   * 以下のパターンのいずれかが見つかった場合、Generatorで生成されたマッパーと判定します：
   *
   * <ul>
   *   <li>"ByExample" を含むメソッド名
   *   <li>"ByPrimaryKey" を含むメソッド名
   *   <li>"Selective" を含むメソッド名
   *   <li>標準的なGeneratorメソッド名（selectByExample, updateByExample, deleteByExample, countByExample）
   * </ul>
   *
   * @param doc 判定対象のXMLドキュメント
   * @return MyBatis Generatorで生成されたマッパーの場合はtrue、そうでなければfalse
   * @see #extractGeneratedMappings(String, List)
   */
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

  /**
   * MyBatis Generator生成の標準的なCRUD操作を抽出します
   *
   * <p>MyBatis Generatorが標準的に生成する以下のメソッドに対応するSQLマッピングを作成します：
   *
   * <h3>SELECT操作</h3>
   *
   * <ul>
   *   <li>selectByPrimaryKey - 主キーによる単一レコード取得
   *   <li>selectByExample - Example条件による複数レコード取得
   *   <li>countByExample - Example条件によるレコード数取得
   * </ul>
   *
   * <h3>INSERT操作</h3>
   *
   * <ul>
   *   <li>insert - 全カラム挿入
   *   <li>insertSelective - NULL以外のカラムのみ挿入
   * </ul>
   *
   * <h3>UPDATE操作</h3>
   *
   * <ul>
   *   <li>updateByPrimaryKey - 主キーによる全カラム更新
   *   <li>updateByPrimaryKeySelective - 主キーによるNULL以外カラム更新
   *   <li>updateByExample - Example条件による更新
   *   <li>updateByExampleSelective - Example条件によるNULL以外カラム更新
   * </ul>
   *
   * <h3>DELETE操作</h3>
   *
   * <ul>
   *   <li>deleteByPrimaryKey - 主キーによる削除
   *   <li>deleteByExample - Example条件による削除
   * </ul>
   *
   * @param namespace マッパーの名前空間
   * @param list SQLマッピングを追加するリスト
   * @see #extractTableNameFromNamespace(String)
   * @see #addGeneratedMapping(List, String, String, SqlOperation, String)
   */
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

  /**
   * 動的SQL要素を含むマッピングを抽出します
   *
   * <p>XMLドキュメント内のSQL要素（select, insert, update, delete）を走査し、 動的SQL要素が含まれているマッピングを検出して抽出します。
   *
   * <h3>検出対象の動的SQL要素</h3>
   *
   * <ul>
   *   <li>&lt;if&gt; - 条件分岐
   *   <li>&lt;choose&gt;, &lt;when&gt;, &lt;otherwise&gt; - 複数条件分岐
   *   <li>&lt;where&gt; - WHERE句の動的生成
   *   <li>&lt;set&gt; - SET句の動的生成
   *   <li>&lt;foreach&gt; - 繰り返し処理
   *   <li>&lt;trim&gt; - 文字列のトリム処理
   * </ul>
   *
   * @param doc 解析対象のXMLドキュメント
   * @param namespace マッパーの名前空間
   * @param list SQLマッピングを追加するリスト
   * @see #containsDynamicSql(Element)
   * @see #extractTableNameFromSql(String)
   */
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

  /**
   * 指定されたXML要素に動的SQL要素が含まれているかチェックします
   *
   * <p>要素の子孫要素を再帰的に走査し、MyBatisの動的SQL要素が存在するかを判定します。
   *
   * @param element チェック対象のXML要素
   * @return 動的SQL要素が含まれている場合はtrue、そうでなければfalse
   * @see #extractDynamicSqlMappings(Document, String, List)
   */
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

  /**
   * マッパーの名前空間からテーブル名を推定します
   *
   * <p>名前空間の最後の部分（クラス名）を抽出し、以下の変換を行ってテーブル名を推定します：
   *
   * <ol>
   *   <li>"Mapper" または "Repository" サフィックスを除去
   *   <li>キャメルケースをスネークケースに変換
   *   <li>小文字に変換
   * </ol>
   *
   * <h3>変換例</h3>
   *
   * <ul>
   *   <li>com.example.UserMapper → user
   *   <li>com.example.OrderDetailMapper → order_detail
   *   <li>com.example.ProductRepository → product
   * </ul>
   *
   * @param namespace マッパーの名前空間（例: com.example.UserMapper）
   * @return 推定されたテーブル名。名前空間が空の場合は "unknown_table"
   * @see #extractGeneratedMappings(String, List)
   */
  private String extractTableNameFromNamespace(String namespace) {
    if (namespace.isEmpty()) return "unknown_table";

    String[] parts = namespace.split("\\.");
    String className = parts[parts.length - 1];

    // MapperやRepositoryサフィックスを除去
    className = className.replaceAll("(Mapper|Repository)$", "");

    // キャメルケースをスネークケースに変換
    return className.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
  }

  /**
   * SQL文からテーブル名を抽出します（簡易実装）
   *
   * <p>SQL文を解析し、FROM句、UPDATE句、INSERT INTO句からテーブル名を抽出します。 この実装は簡易版であり、複雑なSQL文や結合クエリには対応していません。
   *
   * <h3>対応するSQL構文</h3>
   *
   * <ul>
   *   <li>SELECT ... FROM table_name
   *   <li>UPDATE table_name SET ...
   *   <li>INSERT INTO table_name ...
   * </ul>
   *
   * @param sql 解析対象のSQL文
   * @return 抽出されたテーブル名（小文字）。抽出できない場合はnull
   * @see #extractDynamicSqlMappings(Document, String, List)
   */
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

  /**
   * MyBatis Generatorで生成されたマッピング情報をリストに追加します
   *
   * <p>指定されたパラメータを使用してSqlMappingオブジェクトを作成し、リストに追加します。 実際のSQL文の代わりに、操作とテーブル名を示す合成されたコメント文を使用します。
   *
   * @param list SQLマッピングを追加するリスト
   * @param namespace マッパーの名前空間
   * @param methodId メソッドID（例: selectByPrimaryKey, insertSelective）
   * @param operation SQL操作タイプ（SELECT, INSERT, UPDATE, DELETE）
   * @param tableName 対象テーブル名
   * @see SqlMapping
   * @see SqlOperation
   * @see #extractGeneratedMappings(String, List)
   */
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
