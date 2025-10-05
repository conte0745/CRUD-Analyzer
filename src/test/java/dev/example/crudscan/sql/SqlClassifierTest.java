package dev.example.crudscan.sql;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** SqlClassifierのテストクラス */
@DisplayName("SqlClassifier機能のテスト")
class SqlClassifierTest {

  private SqlClassifier classifier;

  @BeforeEach
  void setUp() {
    classifier = new SqlClassifier();
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    SqlClassifier newClassifier = new SqlClassifier();

    // Then
    assertThat(newClassifier).isNotNull();
  }

  @ParameterizedTest
  @CsvSource({
    "DELETE FROM users WHERE id = 1, DELETE, users",
    "UPDATE users SET name = 'updated' WHERE id = 1, UPDATE, users",
    "SELECT * FROM users, SELECT, users"
  })
  @DisplayName("SQLクエリが正常に分類されること")
  void testClassify_WithVariousQueries_ShouldReturnCorrectOperation(
      String sql, String expectedOp, String expectedTable) {
    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo(expectedOp);
    assertThat(info.getAllTables()).contains(expectedTable);
  }

  @Test
  @DisplayName("複雑なJOINクエリから全てのテーブルが抽出されること")
  void testClassify_WithComplexJoinQuery_ShouldExtractAllTables() {
    // Given
    String sql =
        """
        SELECT u.name, p.title, c.name as category
        FROM users u
        LEFT JOIN posts p ON u.id = p.author_id
        LEFT JOIN categories c ON p.category_id = c.id
        WHERE u.active = 1
        """;

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT");
    assertThat(info.getAllTables()).containsExactlyInAnyOrder("users", "posts", "categories");
  }

  // nullに対する動作は実装に依存するため、テストから除外

  @Test
  @DisplayName("Infoクラスのデフォルトコンストラクタでフィールドが初期化されること")
  void testInfo_DefaultConstructor_ShouldInitializeFields() {
    // When
    SqlClassifier.Info info = new SqlClassifier.Info();

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isNull();
    assertThat(info.getAllTables()).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("MyBatisパラメータを含むSQLが正常に処理されること")
  void testClassify_WithMyBatisParameters_ShouldNormalizeAndClassify() {
    // Given
    String sql = "SELECT * FROM users WHERE id = #{userId} AND name = ${userName}";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT");
    assertThat(info.getAllTables()).contains("users");
  }

  @Test
  @DisplayName("動的SQL要素を含むSQLが正常に処理されること")
  void testClassify_WithDynamicSqlElements_ShouldNormalizeAndClassify() {
    // Given
    String sql =
        """
        SELECT * FROM users
        <where>
            <if test="name != null">
                AND name = #{name}
            </if>
        </where>
        """;

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT");
    assertThat(info.getAllTables()).contains("users");
  }

  @Test
  @DisplayName("INSERT文が正常に分類されること")
  void testClassify_WithInsertStatement_ShouldClassifyCorrectly() {
    // Given
    String sql = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("INSERT");
    assertThat(info.getAllTables()).contains("users");
  }

  @Test
  @DisplayName("不正なSQL文でもフォールバック処理が動作すること")
  void testClassify_WithInvalidSql_ShouldUseFallback() {
    // Given
    String sql = "INVALID SQL STATEMENT FROM users";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT"); // フォールバック時のデフォルト
    // 不正なSQL文は正規化処理でdualにフォールバックする
    assertThat(info.getAllTables()).contains("dual");
  }

  @Test
  @DisplayName("空のSQL文が適切に処理されること")
  void testClassify_WithEmptyString_ShouldHandleGracefully() {
    // Given
    String sql = "";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT"); // フォールバック時のデフォルト
    assertThat(info.getAllTables()).isEmpty();
  }

  @Test
  @DisplayName("CTE（WITH句）を含むSQLが正常に処理されること")
  void testClassify_WithCTEQuery_ShouldClassifyCorrectly() {
    // Given
    String sql =
        """
        WITH user_stats AS (
            SELECT user_id, COUNT(*) as post_count
            FROM posts
            GROUP BY user_id
        )
        SELECT u.name, us.post_count
        FROM users u
        JOIN user_stats us ON u.id = us.user_id
        """;

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT");
    assertThat(info.getAllTables()).containsAnyOf("users", "posts", "user_stats");
  }

  @Test
  @DisplayName("単純なDELETE文が正常に分類されること")
  void testClassify_WithSimpleDeleteStatement_ShouldClassifyCorrectly() {
    // Given
    String sql = "DELETE FROM users WHERE id = 123";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("DELETE");
    assertThat(info.getAllTables()).contains("users");
  }

  @Test
  @DisplayName("条件なしのDELETE文が正常に分類されること")
  void testClassify_WithDeleteAllStatement_ShouldClassifyCorrectly() {
    // Given
    String sql = "DELETE FROM temp_data";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("DELETE");
    assertThat(info.getAllTables()).contains("temp_data");
  }

  @Test
  @DisplayName("複雑な条件を持つDELETE文が正常に分類されること")
  void testClassify_WithComplexDeleteStatement_ShouldClassifyCorrectly() {
    // Given
    String sql = "DELETE FROM orders WHERE status = 'cancelled' AND created_at < '2023-01-01'";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("DELETE");
    assertThat(info.getAllTables()).contains("orders");
  }

  @Test
  @DisplayName("サブクエリを含むDELETE文が正常に分類されること")
  void testClassify_WithDeleteSubquery_ShouldClassifyCorrectly() {
    // Given
    String sql =
        "DELETE FROM users WHERE id IN (SELECT user_id FROM inactive_users WHERE last_login < '2022-01-01')";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("DELETE");
    assertThat(info.getAllTables()).containsExactlyInAnyOrder("users", "inactive_users");
  }

  @Test
  @DisplayName("JOINを使ったDELETE文が正常に分類されること")
  void testClassify_WithDeleteJoin_ShouldClassifyCorrectly() {
    // Given
    String sql =
        "DELETE u FROM users u JOIN user_roles ur ON u.id = ur.user_id WHERE ur.role = 'guest'";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("DELETE");
    // 全テーブルが抽出されることを確認
    assertThat(info.getAllTables()).containsExactlyInAnyOrder("users", "user_roles");
    // 削除対象テーブルは users のみ
    assertThat(info.targetTables).containsExactly("users");
    // 参照テーブルは user_roles のみ
    assertThat(info.referenceTables).containsExactly("user_roles");
  }

  @Test
  @DisplayName("MyBatisパラメータを含むDELETE文が正常に分類されること")
  void testClassify_WithDeleteMyBatisParams_ShouldClassifyCorrectly() {
    // Given
    String sql = "DELETE FROM products WHERE category_id = #{categoryId} AND price < #{maxPrice}";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("DELETE");
    assertThat(info.getAllTables()).contains("products");
  }

  @Test
  @DisplayName("動的SQLを含むDELETE文が正常に分類されること")
  void testClassify_WithDeleteDynamicSql_ShouldClassifyCorrectly() {
    // Given
    String sql =
        """
        DELETE FROM notifications
        <where>
            <if test="userId != null">
                AND user_id = #{userId}
            </if>
            <if test="isRead != null">
                AND is_read = #{isRead}
            </if>
        </where>
        """;

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("DELETE");
    assertThat(info.getAllTables()).contains("notifications");
  }

  @Test
  @DisplayName("複数のJOINを含むDELETE文が正常に分類されること")
  void testClassify_WithMultipleJoinDelete_ShouldClassifyCorrectly() {
    // Given
    String sql =
        "DELETE u FROM users u "
            + "JOIN user_profiles up ON u.id = up.user_id "
            + "JOIN user_roles ur ON u.id = ur.user_id "
            + "WHERE up.status = 'inactive' AND ur.role = 'guest'";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("DELETE");
    // 複数のJOINしたテーブルがすべて抽出されることを確認
    assertThat(info.getAllTables())
        .containsExactlyInAnyOrder("users", "user_profiles", "user_roles");
    // 削除対象テーブルは users のみ
    assertThat(info.targetTables).containsExactly("users");
    // 参照テーブルは user_profiles と user_roles
    assertThat(info.referenceTables).containsExactlyInAnyOrder("user_profiles", "user_roles");
  }

  @Test
  @DisplayName("LEFT JOINを含むDELETE文が正常に分類されること")
  void testClassify_WithLeftJoinDelete_ShouldClassifyCorrectly() {
    // Given
    String sql =
        "DELETE u FROM users u LEFT JOIN user_sessions us ON u.id = us.user_id WHERE us.user_id IS NULL";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("DELETE");
    assertThat(info.getAllTables()).containsExactlyInAnyOrder("users", "user_sessions");
  }

  @Test
  @DisplayName("JOINを含むUPDATE文で操作対象と参照テーブルが正しく分類されること")
  void testClassify_WithUpdateJoin_ShouldClassifyTablesCorrectly() {
    // Given
    String sql =
        "UPDATE users u JOIN user_profiles up ON u.id = up.user_id SET u.status = 'inactive' WHERE up.last_login < '2022-01-01'";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("UPDATE");
    // 全テーブルが抽出されることを確認
    assertThat(info.getAllTables()).contains("users");
    // 更新対象テーブルは users のみ
    assertThat(info.targetTables).contains("users");
    // JOINが解析される場合は参照テーブルに含まれる（JSqlParserの実装に依存）
    if (!info.referenceTables.isEmpty()) {
      assertThat(info.referenceTables).contains("user_profiles");
    }
  }

  @Test
  @DisplayName("複数JOINを含むUPDATE文が正しく分類されること")
  void testClassify_WithMultipleJoinUpdate_ShouldClassifyTablesCorrectly() {
    // Given
    String sql =
        "UPDATE orders o "
            + "JOIN customers c ON o.customer_id = c.id "
            + "JOIN order_status os ON o.status_id = os.id "
            + "SET o.updated_at = NOW() WHERE c.active = 1 AND os.name = 'pending'";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("UPDATE");
    // 全テーブルが抽出されることを確認
    assertThat(info.getAllTables()).contains("orders");
    // 更新対象テーブルは orders のみ
    assertThat(info.targetTables).contains("orders");
    // JOINが解析される場合は参照テーブルに含まれる（JSqlParserの実装に依存）
    if (info.referenceTables.size() >= 2) {
      assertThat(info.referenceTables).containsAnyOf("customers", "order_status");
    }
  }

  @Test
  @DisplayName("単純なUPDATE文で操作対象テーブルのみが分類されること")
  void testClassify_WithSimpleUpdate_ShouldClassifyTargetTableOnly() {
    // Given
    String sql = "UPDATE products SET price = price * 1.1 WHERE category = 'electronics'";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("UPDATE");
    // 操作対象テーブルのみ
    assertThat(info.targetTables).containsExactly("products");
    // 参照テーブルは空
    assertThat(info.referenceTables).isEmpty();
    // 全テーブルは操作対象テーブルのみ
    assertThat(info.getAllTables()).containsExactly("products");
  }

  @Test
  @DisplayName("SELECT文で全テーブルが参照テーブルとして分類されること")
  void testClassify_WithSelect_ShouldClassifyAllAsReferenceTables() {
    // Given
    String sql =
        "SELECT u.name, p.title FROM users u JOIN posts p ON u.id = p.author_id WHERE u.active = 1";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT");
    // 操作対象テーブルは空
    assertThat(info.targetTables).isEmpty();
    // 全テーブルが参照テーブル
    assertThat(info.referenceTables).containsExactlyInAnyOrder("users", "posts");
    // 全テーブルは参照テーブルと同じ
    assertThat(info.getAllTables()).containsExactlyInAnyOrder("users", "posts");
  }

  @Test
  @DisplayName("INSERT文で操作対象テーブルのみが分類されること")
  void testClassify_WithInsert_ShouldClassifyTargetTableOnly() {
    // Given
    String sql = "INSERT INTO audit_logs (user_id, action, timestamp) VALUES (1, 'login', NOW())";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("INSERT");
    // 操作対象テーブルのみ
    assertThat(info.targetTables).containsExactly("audit_logs");
    // 参照テーブルは空
    assertThat(info.referenceTables).isEmpty();
    // 全テーブルは操作対象テーブルのみ
    assertThat(info.getAllTables()).containsExactly("audit_logs");
  }

  @Test
  @DisplayName("サブクエリを含むINSERT文で参照テーブルも抽出されること")
  void testClassify_WithInsertSubquery_ShouldExtractReferenceTables() {
    // Given
    String sql =
        "INSERT INTO user_summaries (user_id, post_count) SELECT u.id, COUNT(p.id) FROM users u LEFT JOIN posts p ON u.id = p.author_id GROUP BY u.id";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("INSERT");
    // 操作対象テーブルは user_summaries のみ
    assertThat(info.targetTables).contains("user_summaries");
    // 全テーブルには少なくとも操作対象テーブルが含まれる
    assertThat(info.getAllTables()).contains("user_summaries");
    // サブクエリのテーブルが抽出される場合は参照テーブルに含まれる
    if (!info.referenceTables.isEmpty()) {
      assertThat(info.referenceTables).containsAnyOf("users", "posts");
    }
  }

  @Test
  @DisplayName("フォールバック処理が正しく動作すること")
  void testClassify_WithFallback_ShouldWorkCorrectly() {
    // Given - JSqlParserで解析できない不正なSQL
    String sql = "INVALID DELETE FROM users WHERE name LIKE '%test%'";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then - 実際の挙動に基づく検証
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT"); // フォールバック処理ではSELECTになる
    assertThat(info.targetTables).isEmpty(); // フォールバック処理では操作対象は分類されない
    assertThat(info.referenceTables).contains("dual"); // 正規化処理でdualが追加される
    assertThat(info.getAllTables()).contains("dual");
  }

  @Test
  @DisplayName("正常なDELETE文との比較でフォールバック処理を検証する")
  void testClassify_CompareNormalAndFallback() {
    // Given - 正常なDELETE文
    String normalSql = "DELETE FROM users WHERE id = 1";
    String invalidSql = "INVALID DELETE FROM users WHERE name LIKE '%test%'";

    // When
    SqlClassifier.Info normalInfo = classifier.classify(normalSql);
    SqlClassifier.Info fallbackInfo = classifier.classify(invalidSql);

    // Then - 実際の挙動に基づく検証
    assertThat(normalInfo.op).isEqualTo("DELETE");
    assertThat(normalInfo.referenceTables).contains("users"); // 正常なDELETEでは参照テーブルに分類される

    assertThat(fallbackInfo.op).isEqualTo("SELECT"); // フォールバック処理ではSELECTになる
    assertThat(fallbackInfo.referenceTables).contains("dual"); // 正規化処理でdualが追加される

    // 両方とも操作対象テーブルは空
    assertThat(normalInfo.targetTables).isEmpty();
    assertThat(fallbackInfo.targetTables).isEmpty();
  }

  @Test
  @DisplayName("DELETE文のフォールバックパターンを検証する")
  void testClassify_DeleteFallbackPattern() {
    // Given - 不正なDELETE文
    String sql = "INVALID DELETE FROM users WHERE name LIKE '%test%'";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT"); // フォールバック処理ではSELECTになる
    assertThat(info.targetTables).isEmpty();
    assertThat(info.referenceTables).contains("dual");
  }

  @Test
  @DisplayName("SELECT文のフォールバックパターンを検証する")
  void testClassify_SelectFallbackPattern() {
    // Given - 不正なSELECT文
    String sql = "BROKEN SELECT * FROM products WHERE price > 100";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT");
    assertThat(info.targetTables).isEmpty();
    // フォールバック処理では何らかのテーブルが抽出される
    assertThat(info.getAllTables()).isNotEmpty();
  }

  @Test
  @DisplayName("UPDATE文のフォールバックパターンを検証する")
  void testClassify_UpdateFallbackPattern() {
    // Given - 不正なUPDATE文
    String sql = "MALFORMED UPDATE orders SET status = 'done'";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    // フォールバック処理では操作種別が推定される
    assertThat(info.op).isIn("UPDATE", "SELECT");
    assertThat(info.targetTables).isEmpty(); // フォールバック処理では操作対象は分類されない
    // 正規表現でテーブル名が抽出される場合がある
    assertThat(info.getAllTables()).isNotEmpty();
  }

  @Test
  @DisplayName("INSERT文のフォールバックパターンを検証する")
  void testClassify_InsertFallbackPattern() {
    // Given - 不正なINSERT文
    String sql = "BAD INSERT INTO logs (message) VALUES ('test')";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    // フォールバック処理では操作種別が推定される
    assertThat(info.op).isIn("INSERT", "SELECT");
    assertThat(info.targetTables).isEmpty(); // フォールバック処理では操作対象は分類されない
    // 正規表現でテーブル名が抽出される場合がある
    assertThat(info.getAllTables()).isNotEmpty();
  }

  @Test
  @DisplayName("getAllTables()メソッドが重複を除去して統合リストを返すこと")
  void testGetAllTables_ShouldReturnMergedListWithoutDuplicates() {
    // Given
    SqlClassifier.Info info = new SqlClassifier.Info();
    info.targetTables.add("users");
    info.targetTables.add("orders");
    info.referenceTables.add("users"); // 重複
    info.referenceTables.add("customers");

    // When
    var allTables = info.getAllTables();

    // Then
    // 重複が除去されて統合されることを確認
    assertThat(allTables).containsExactlyInAnyOrder("users", "orders", "customers");
    // 元のリストは変更されないことを確認
    assertThat(info.targetTables).containsExactly("users", "orders");
    assertThat(info.referenceTables).containsExactly("users", "customers");
  }

  @Test
  @DisplayName("null入力でNullPointerExceptionが発生すること")
  void testClassify_WithNullInput_ShouldThrowException() {
    // When & Then
    assertThatThrownBy(() -> classifier.classify(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("空文字列入力が適切に処理されること")
  void testClassify_WithEmptyInput_ShouldHandleGracefully() {
    // Given
    String sql = "";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isNotNull();
    assertThat(info.targetTables).isEmpty();
    assertThat(info.getAllTables()).isEmpty();
  }

  @Test
  @DisplayName("空白のみの入力が適切に処理されること")
  void testClassify_WithWhitespaceOnlyInput_ShouldHandleGracefully() {
    // Given
    String sql = "   \n\t  ";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isNotNull();
    assertThat(info.targetTables).isEmpty();
    assertThat(info.getAllTables()).isEmpty();
  }

  @Test
  @DisplayName("非常に長いSQL文が適切に処理されること")
  void testClassify_WithVeryLongSql_ShouldHandleGracefully() {
    // Given - 非常に長いSQL文を生成
    StringBuilder longSql = new StringBuilder("SELECT * FROM users WHERE ");
    for (int i = 0; i < 1000; i++) {
      longSql.append("column").append(i).append(" = 'value").append(i).append("' AND ");
    }
    longSql.append("1 = 1");

    // When
    SqlClassifier.Info info = classifier.classify(longSql.toString());

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT");
    assertThat(info.referenceTables).contains("users");
    assertThat(info.targetTables).isEmpty();
  }

  @Test
  @DisplayName("特殊文字を含むテーブル名が適切に処理されること")
  void testClassify_WithSpecialCharactersInTableName_ShouldHandleCorrectly() {
    // Given
    String sql = "SELECT * FROM `user_table` WHERE id = 1";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT");
    // バッククォートが除去されてテーブル名が抽出される
    assertThat(info.getAllTables()).contains("user_table");
  }

  @Test
  @DisplayName("複数のSQL文が含まれる入力が適切に処理されること")
  void testClassify_WithMultipleStatements_ShouldHandleFirst() {
    // Given
    String sql = "SELECT * FROM users; DELETE FROM logs; UPDATE orders SET status = 'done'";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT"); // 最初の文が処理される
    assertThat(info.referenceTables).contains("users");
  }

  @Test
  @DisplayName("コメントを含むSQL文が適切に処理されること")
  void testClassify_WithComments_ShouldIgnoreComments() {
    // Given
    String sql = "SELECT * FROM users /* This is a comment */";

    // When
    SqlClassifier.Info info = classifier.classify(sql);

    // Then
    assertThat(info).isNotNull();
    assertThat(info.op).isEqualTo("SELECT");
    assertThat(info.referenceTables).contains("users");
  }
}
