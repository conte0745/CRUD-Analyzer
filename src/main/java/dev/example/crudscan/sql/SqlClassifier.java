package dev.example.crudscan.sql;

import dev.example.crudscan.enums.SqlOperation;
import java.util.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQL文を解析し、操作種別（SELECT/INSERT/UPDATE/DELETE）やテーブル名を抽出するユーティリティクラス。
 *
 * <p>このクラスは、JSqlParserライブラリを使用してSQL文を構文解析し、 以下の情報を抽出します：
 *
 * <ul>
 *   <li>SQL操作種別（SELECT, INSERT, UPDATE, DELETE, UNKNOWN）
 *   <li>対象となるテーブル名のリスト
 * </ul>
 *
 * <p>JSqlParserでの解析に失敗した場合は、正規表現を使用したフォールバック処理を行います。
 *
 * @author CRUD Analyzer
 * @version 1.0
 * @since 1.0
 */
public class SqlClassifier {
  /** このクラス専用のロガーインスタンス */
  private static final Logger logger = LoggerFactory.getLogger(SqlClassifier.class);

  /** デフォルトコンストラクタ */
  public SqlClassifier() {
    // デフォルトコンストラクタ
  }

  /**
   * SQL文の分類結果を格納するデータクラス。
   *
   * <p>SQL解析の結果として、操作種別とテーブル名リストを保持します。
   */
  public static class Info {

    /** デフォルトコンストラクタ */
    public Info() {
      // デフォルトコンストラクタ
    }

    /**
     * SQL操作種別。
     *
     * <p>以下のいずれかの値を取ります：
     *
     * <ul>
     *   <li>SELECT - 検索操作
     *   <li>INSERT - 挿入操作
     *   <li>UPDATE - 更新操作
     *   <li>DELETE - 削除操作
     *   <li>UNKNOWN - 未知の操作または解析失敗
     * </ul>
     */
    public String op;

    /**
     * 抽出されたテーブル名のリスト。
     *
     * <p>SQL文中で参照されているテーブル名が格納されます。 重複は除去され、発見順に格納されます。
     */
    public List<String> tables = new ArrayList<>();
  }

  /**
   * SQL文を解析し、操作種別とテーブル名リストを返します。
   *
   * <p>このメソッドは以下の手順でSQL文を解析します：
   *
   * <ol>
   *   <li>JSqlParserを使用してSQL文を構文解析
   *   <li>SQL文の種別に応じて適切なハンドラーメソッドを呼び出し
   *   <li>解析に失敗した場合は正規表現によるフォールバック処理を実行
   * </ol>
   *
   * <p>複雑度低減のため、各SQL種別の処理は専用のprivateメソッドに委譲しています。
   *
   * @param raw 解析対象のSQL文（null不可）
   * @return SQL文の分類結果を格納したInfoオブジェクト（null不可）
   * @throws IllegalArgumentException rawがnullの場合
   */
  public Info classify(String raw) {
    logger.info("SQL文の解析を開始: " + raw.substring(0, Math.min(50, raw.length())) + "...");

    Info info = new Info();

    // MyBatisパラメータ構文を標準SQLに変換
    String normalizedSql = normalizeMyBatisParameters(raw);

    try {
      Statement stmt = CCJSqlParserUtil.parse(normalizedSql);
      switch (stmt) {
        case Select select -> handleSelect(select, info);
        case Insert insert -> handleInsert(insert, info);
        case Update update -> handleUpdate(update, info);
        case Delete delete -> handleDelete(delete, info);
        default -> {
          info.op = "UNKNOWN";
          logger.warn("未知のSQL文タイプです: {}", stmt.getClass().getSimpleName());
        }
      }
      logger.info("SQL解析完了 - 操作: {}, テーブル数: {}", info.op, info.tables.size());

    } catch (Exception e) {
      handleParseFallback(raw, info);
    }
    return info;
  }

  /**
   * MyBatisのパラメータ構文を標準SQLに正規化します。
   *
   * <p>以下の変換を行います：
   *
   * <ul>
   *   <li>#{parameter} → 'dummy_param'
   *   <li>${parameter} → 'dummy_param'
   *   <li>&lt;if&gt;, &lt;where&gt;等の動的SQL要素を除去
   * </ul>
   *
   * @param sql 元のSQL文
   * @return 正規化されたSQL文
   */
  private String normalizeMyBatisParameters(String sql) {
    if (sql == null || sql.trim().isEmpty()) return sql;

    String normalized =
        sql
            // MyBatisパラメータ構文を置換（より安全な値に）
            .replaceAll("#\\{[^}]*\\}", "1")
            .replaceAll("\\$\\{[^}]*\\}", "1")
            // 動的SQL要素を除去（開始・終了タグを含む）
            .replaceAll("</?\\w+[^>]*>", "")
            // CDATA セクションを除去
            .replaceAll("<!\\[CDATA\\[", "")
            .replaceAll("\\]\\]>", "")
            // 余分な空白を正規化
            .replaceAll("\\s+", " ")
            .trim();

    // 不完全なSQL文を修正
    normalized = fixIncompleteSQL(normalized);

    // 最終的にSQL文として有効かチェック
    if (!isValidSqlStructure(normalized)) {
      // 基本的なSELECT文にフォールバック
      return "SELECT 1 FROM dual";
    }

    return normalized;
  }

  /**
   * SQL文の基本構造が有効かチェックします。
   *
   * @param sql チェック対象のSQL文
   * @return 有効な場合true
   */
  private boolean isValidSqlStructure(String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      return false;
    }

    String upperSql = sql.toUpperCase().trim();

    // 基本的なSQL文の構造をチェック
    return upperSql.matches("^(SELECT|INSERT|UPDATE|DELETE)\\s+.*")
        && !upperSql.matches(".*\\s+(WHERE|FROM|SET)\\s*$")
        && !upperSql.contains("WHERE WHERE")
        && !upperSql.contains("FROM FROM");
  }

  /**
   * 動的SQL要素の除去により不完全になったSQL文を修正します。
   *
   * @param sql 正規化途中のSQL文
   * @return 修正されたSQL文
   */
  private String fixIncompleteSQL(String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      return sql;
    }

    // 不正な AND/OR で始まる文を修正
    sql =
        sql.replaceAll("\\bAND\\s+(?=\\s*$)", "")
            .replaceAll("\\bOR\\s+(?=\\s*$)", "")
            .replaceAll("^\\s*AND\\s+", "")
            .replaceAll("^\\s*OR\\s+", "");

    // WHERE句が空になった場合の修正
    sql =
        sql.replaceAll("\\bWHERE\\s+(?=\\s*$)", "")
            .replaceAll("\\bWHERE\\s+(?=ORDER\\s+BY|GROUP\\s+BY|HAVING|LIMIT|$)", "");

    // FROM句の後に直接WHERE/AND/ORが来る場合の修正
    sql = sql.replaceAll("\\bFROM\\s+([\\w`\"']+)\\s+(?:WHERE|AND|OR)\\b", "FROM $1 WHERE 1=1");

    // SELECT文が不完全な場合（FROM句のみなど）の修正
    if (sql.matches("^\\s*FROM\\s+\\w+.*")) {
      sql = "SELECT * " + sql;
    }

    // WHERE句の直後にWHEREが来る場合の修正
    sql = sql.replaceAll("\\bWHERE\\s+WHERE\\b", "WHERE");

    // 連続する AND/OR を修正
    sql = sql.replaceAll("\\b(?:AND|OR)\\s+(?:AND|OR)\\b", "AND");

    // 空のSELECT文の修正
    if (sql.matches("^\\s*SELECT\\s*$")) {
      sql = "SELECT 1";
    }

    // 余分な空白を再度正規化
    sql = sql.replaceAll("\\s+", " ").trim();

    return sql;
  }

  /**
   * SELECT文からテーブル名を抽出します。
   *
   * <p>JSqlParser 5.3の新APIであるTablesNamesFinderを優先的に使用し、 失敗した場合は手動解析にフォールバックします。
   *
   * @param select 解析対象のSelectオブジェクト
   * @param info 結果を格納するInfoオブジェクト
   */
  private void handleSelect(Select select, Info info) {
    info.op = SqlOperation.SELECT.toString();
    logger.info("SELECT文の解析を開始");
    try {
      // 新API: TablesNamesFinder.getTables()を使用
      net.sf.jsqlparser.util.TablesNamesFinder<Void> tablesNamesFinder =
          new net.sf.jsqlparser.util.TablesNamesFinder<>();
      Set<String> tableNames =
          tablesNamesFinder.getTables((net.sf.jsqlparser.statement.Statement) select);
      info.tables.addAll(tableNames);
      logger.info("TablesNamesFinderでテーブル抽出完了: " + String.join(", ", tableNames));
    } catch (Exception e) {
      logger.warn("TablesNamesFinderが失敗。手動解析にフォールバック: " + e.getMessage());
      // フォールバック: 手動でSelectBodyを解析
      extractTablesFromSelectBody(select, info);
    }
  }

  /**
   * Selectオブジェクトから手動でテーブル名を抽出します。
   *
   * <p>JSqlParser 5.3では SelectBody が削除されたため、 Selectオブジェクトを直接型チェックして処理します。 対応する型：
   *
   * <ul>
   *   <li>PlainSelect - 単純なSELECT文
   *   <li>SetOperationList - UNION等の複合クエリ
   *   <li>ParenthesedSelect - 括弧で囲まれたSELECT文
   * </ul>
   *
   * @param select 解析対象のSelectオブジェクト
   * @param info 結果を格納するInfoオブジェクト
   */
  private void extractTablesFromSelectBody(Select select, Info info) {
    logger.info("手動でSelectからテーブル名を抽出開始: " + select.getClass().getSimpleName());
    try {
      // JSqlParser 5.3: SelectBodyが削除されたため、Selectを直接型チェック
      if (select instanceof net.sf.jsqlparser.statement.select.PlainSelect ps) {
        logger.info("PlainSelectとして処理");
        extractTablesFromPlainSelect(ps, info);
      } else if (select instanceof net.sf.jsqlparser.statement.select.SetOperationList setOp) {
        logger.info("SetOperationListとして処理 - サブクエリ数: " + setOp.getSelects().size());
        for (var subSelect : setOp.getSelects()) {
          extractTablesFromSelectBody(subSelect, info);
        }
      } else if (select
          instanceof net.sf.jsqlparser.statement.select.ParenthesedSelect parenthesedSelect) {
        logger.info("ParenthesedSelectとして処理");
        // ParenthesedSelectの場合、内部のSelectを再帰的に処理
        extractTablesFromSelectBody(parenthesedSelect.getSelect(), info);
      }
    } catch (Exception e) {
      logger.warn("手動解析が失敗。正規表現フォールバックを実行: " + e.getMessage());
      // 最終フォールバック: 正規表現
      extractTablesWithRegex(select.toString(), info);
    }
  }

  /**
   * PlainSelectオブジェクトからテーブル名を抽出します。
   *
   * <p>FROM句のテーブルとJOIN句のテーブルを抽出します。
   *
   * @param ps 解析対象のPlainSelectオブジェクト
   * @param info 結果を格納するInfoオブジェクト
   */
  private void extractTablesFromPlainSelect(
      net.sf.jsqlparser.statement.select.PlainSelect ps, Info info) {
    if (ps.getFromItem() instanceof net.sf.jsqlparser.schema.Table table) {
      info.tables.add(table.getName());
    }
    if (ps.getJoins() != null) {
      for (var join : ps.getJoins()) {
        if (join.getRightItem() instanceof net.sf.jsqlparser.schema.Table joinTable) {
          info.tables.add(joinTable.getName());
        }
      }
    }
  }

  /**
   * 正規表現を使用してテーブル名を抽出します（最終フォールバック処理）。
   *
   * <p>JSqlParserでの解析が完全に失敗した場合の最後の手段として、 正規表現パターンマッチングでテーブル名を抽出します。
   *
   * <p>対応パターン：
   *
   * <ul>
   *   <li>FROM句のテーブル名
   *   <li>JOIN句のテーブル名
   * </ul>
   *
   * @param sql 解析対象のSQL文字列
   * @param info 結果を格納するInfoオブジェクト
   */
  private void extractTablesWithRegex(String sql, Info info) {
    logger.info("正規表現によるテーブル名抽出を開始");
    int initialTableCount = info.tables.size();
    String[] patterns = {"FROM\\s+([`\"']?\\w+[`\"']?)", "JOIN\\s+([`\"']?\\w+[`\"']?)"};
    for (String pat : patterns) {
      java.util.regex.Matcher m =
          java.util.regex.Pattern.compile(
                  pat, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.MULTILINE)
              .matcher(sql);
      while (m.find()) {
        String table = m.group(1).replaceAll("[`\"']", "");
        if (!info.tables.contains(table)) {
          info.tables.add(table);
          logger.info("正規表現でテーブル発見: " + table);
        }
      }
    }
    int extractedCount = info.tables.size() - initialTableCount;
    logger.info("正規表現抽出完了 - 新規テーブル数: " + extractedCount);
  }

  /**
   * INSERT文からテーブル名を抽出します。
   *
   * <p>INSERT文の対象テーブルを特定し、操作種別を"INSERT"に設定します。
   *
   * @param ins 解析対象のInsertオブジェクト
   * @param info 結果を格納するInfoオブジェクト
   */
  private void handleInsert(Insert ins, Info info) {
    info.op = SqlOperation.INSERT.toString();
    String tableName = ins.getTable().getName();
    info.tables.add(tableName);
    logger.info("INSERT文解析完了 - 対象テーブル: " + tableName);
  }

  /**
   * UPDATE文からテーブル名を抽出します。
   *
   * <p>UPDATE文の対象テーブルを特定し、操作種別を"UPDATE"に設定します。 テーブル情報が取得できない場合は警告ログを出力します。
   *
   * @param up 解析対象のUpdateオブジェクト
   * @param info 結果を格納するInfoオブジェクト
   */
  private void handleUpdate(Update up, Info info) {
    info.op = SqlOperation.UPDATE.toString();
    if (up.getTable() != null) {
      String tableName = up.getTable().getName();
      info.tables.add(tableName);
      logger.info("UPDATE文解析完了 - 対象テーブル: " + tableName);
    } else {
      logger.warn("UPDATE文でテーブル名が取得できませんでした");
    }
  }

  /**
   * DELETE文からテーブル名を抽出します。
   *
   * <p>DELETE文の対象テーブルを特定し、操作種別を"DELETE"に設定します。
   *
   * @param del 解析対象のDeleteオブジェクト
   * @param info 結果を格納するInfoオブジェクト
   */
  private void handleDelete(Delete del, Info info) {
    info.op = SqlOperation.DELETE.toString();
    String tableName = del.getTable().getName();
    info.tables.add(tableName);
    logger.info("DELETE文解析完了 - 対象テーブル: " + tableName);
  }

  /**
   * JSqlParserでの解析が失敗した場合のフォールバック処理を実行します。
   *
   * <p>SQL文の文字列を直接解析して以下を実行します：
   *
   * <ol>
   *   <li>SQL文中のキーワوردから操作種別を推定
   *   <li>正規表現パターンマッチングでテーブル名を抽出
   * </ol>
   *
   * <p>対応する正規表現パターン：
   *
   * <ul>
   *   <li>FROM句 - SELECT, DELETE文用
   *   <li>INSERT INTO句 - INSERT文用
   *   <li>UPDATE句 - UPDATE文用
   * </ul>
   *
   * @param raw 解析対象のSQL文字列
   * @param info 結果を格納するInfoオブジェクト
   */
  private void handleParseFallback(String raw, Info info) {
    // 静かにフォールバック処理を実行（ログ出力を抑制）
    String upper = raw.toUpperCase(Locale.ROOT);
    if (upper.contains("INSERT ")) info.op = SqlOperation.INSERT.toString();
    else if (upper.contains("UPDATE ")) info.op = SqlOperation.UPDATE.toString();
    else if (upper.contains("DELETE ")) info.op = SqlOperation.DELETE.toString();
    else info.op = SqlOperation.SELECT.toString();

    // テーブル名抽出 (簡易版: 各SQL種別ごとに正規表現で拾う)
    // 複数行やインデント、バッククォート対応
    String[] patterns = {
      "FROM\\s+([`\"']?\\w+[`\"']?)", // SELECT, DELETE
      "INSERT\\s+INTO\\s+([`\"']?\\w+[`\"']?)", // INSERT
      "UPDATE\\s+([`\"']?\\w+[`\"']?)" // UPDATE
    };
    for (String pat : patterns) {
      java.util.regex.Matcher m =
          java.util.regex.Pattern.compile(
                  pat, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.MULTILINE)
              .matcher(raw);
      while (m.find()) {
        String table = m.group(1);
        // バッククォート等を除去
        table = table.replaceAll("[`\"']", "");
        if (!info.tables.contains(table)) {
          info.tables.add(table);
          // ログ出力を抑制
        }
      }
    }
    // ログ出力を抑制
  }
}
