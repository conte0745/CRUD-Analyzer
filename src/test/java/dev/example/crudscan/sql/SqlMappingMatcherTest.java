package dev.example.crudscan.sql;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.TestBase;
import dev.example.crudscan.model.Models.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SqlMappingMatcherのテストクラス */
@DisplayName("SqlMappingMatcher機能のテスト")
class SqlMappingMatcherTest extends TestBase {

  private SqlMappingMatcher target;
  private List<CrudLink> links;
  private Endpoint testEndpoint;

  @BeforeEach
  void setUp() {
    target = new SqlMappingMatcher();
    links = new ArrayList<>();
    testEndpoint = new Endpoint("GET", "/api/users", "UserController", "getUsers", "com.example.controller");
  }

  @Test
  @DisplayName("完全修飾名での一致でCRUDリンクが正しく生成されること")
  void testSearchMapper_WithExactMatch_ShouldCreateCrudLinks() {
    // Given - 完全修飾名で一致するテストデータ
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.mapper.UserMapper", "findAll", "SELECT", "SELECT * FROM users", List.of("users"))
    );
    Set<String> reachableMappers = Set.of("com.example.mapper.UserMapper#findAll");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されることを確認
    assertThat(links)
        .hasSize(1)
        .first()
        .satisfies(link -> {
          assertThat(link.ep()).isEqualTo(testEndpoint);
          assertThat(link.table()).isEqualTo("users");
          assertThat(link.crud()).isEqualTo("S");
        });
  }

  @Test
  @DisplayName("単純名での一致でCRUDリンクが正しく生成されること")
  void testSearchMapper_WithSimpleNameMatch_ShouldCreateCrudLinks() {
    // Given - 単純名で一致するテストデータ
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.mapper.UserMapper", "findById", "SELECT", "SELECT * FROM users WHERE id = ?", List.of("users"))
    );
    Set<String> reachableMappers = Set.of("UserMapper#findById");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されることを確認
    assertThat(links)
        .hasSize(1)
        .first()
        .satisfies(link -> {
          assertThat(link.ep()).isEqualTo(testEndpoint);
          assertThat(link.table()).isEqualTo("users");
          assertThat(link.crud()).isEqualTo("S");
        });
  }

  @Test
  @DisplayName("部分一致でCRUDリンクが正しく生成されること")
  void testSearchMapper_WithPartialMatch_ShouldCreateCrudLinks() {
    // Given - 部分一致するテストデータ
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.mapper.UserMapper", "insert", "INSERT", "INSERT INTO users (name) VALUES (?)", List.of("users"))
    );
    Set<String> reachableMappers = Set.of("com.example.different.UserMapper#insert");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されることを確認
    assertThat(links)
        .hasSize(1)
        .first()
        .satisfies(link -> {
          assertThat(link.ep()).isEqualTo(testEndpoint);
          assertThat(link.table()).isEqualTo("users");
          assertThat(link.crud()).isEqualTo("I");
        });
  }

  @Test
  @DisplayName("制限付きマッチングでCRUDリンクが正しく生成されること")
  void testSearchMapper_WithRestrictedMatch_ShouldCreateCrudLinks() {
    // Given - Mapperクラスで制限付きマッチングするテストデータ
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.mapper.UserMapper", "update", "UPDATE", "UPDATE users SET name = ? WHERE id = ?", List.of("users"))
    );
    Set<String> reachableMappers = Set.of("some.other.UserMapper#differentMethod");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されることを確認
    assertThat(links)
        .hasSize(1)
        .first()
        .satisfies(link -> {
          assertThat(link.ep()).isEqualTo(testEndpoint);
          assertThat(link.table()).isEqualTo("users");
          assertThat(link.crud()).isEqualTo("U");
        });
  }

  @Test
  @DisplayName("Repositoryクラスで制限付きマッチングが動作すること")
  void testSearchMapper_WithRepositoryClass_ShouldCreateCrudLinks() {
    // Given - Repositoryクラスのテストデータ
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.repository.UserRepository", "delete", "DELETE", "DELETE FROM users WHERE id = ?", List.of("users"))
    );
    Set<String> reachableMappers = Set.of("some.other.UserRepository#differentMethod");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されることを確認
    assertThat(links)
        .hasSize(1)
        .first()
        .satisfies(link -> {
          assertThat(link.ep()).isEqualTo(testEndpoint);
          assertThat(link.table()).isEqualTo("users");
          assertThat(link.crud()).isEqualTo("D");
        });
  }

  @Test
  @DisplayName("Daoクラスで制限付きマッチングが動作すること")
  void testSearchMapper_WithDaoClass_ShouldCreateCrudLinks() {
    // Given - Daoクラスのテストデータ
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.dao.UserDao", "findAll", "SELECT", "SELECT * FROM users", List.of("users"))
    );
    Set<String> reachableMappers = Set.of("some.other.UserDao#differentMethod");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されることを確認
    assertThat(links)
        .hasSize(1)
        .first()
        .satisfies(link -> {
          assertThat(link.ep()).isEqualTo(testEndpoint);
          assertThat(link.table()).isEqualTo("users");
          assertThat(link.crud()).isEqualTo("S");
        });
  }

  @Test
  @DisplayName("複数テーブルのSQLマッピングで複数のCRUDリンクが生成されること")
  void testSearchMapper_WithMultipleTables_ShouldCreateMultipleCrudLinks() {
    // Given - 複数テーブルを含むSQLマッピング
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.mapper.UserMapper", "joinQuery", "SELECT", "SELECT * FROM users u JOIN profiles p ON u.id = p.user_id JOIN addresses a ON u.id = a.user_id", List.of("users", "profiles", "addresses"))
    );
    Set<String> reachableMappers = Set.of("com.example.mapper.UserMapper#joinQuery");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - 複数のCRUDリンクが生成されることを確認
    assertThat(links)
        .hasSize(3)
        .extracting(CrudLink::table)
        .containsExactlyInAnyOrder("users", "profiles", "addresses");
    
    assertThat(links)
        .allSatisfy(link -> {
          assertThat(link.ep()).isEqualTo(testEndpoint);
          assertThat(link.crud()).isEqualTo("S");
        });
  }

  @Test
  @DisplayName("一致しないSQLマッピングではCRUDリンクが生成されないこと")
  void testSearchMapper_WithNoMatch_ShouldNotCreateCrudLinks() {
    // Given - 一致しないテストデータ
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.mapper.UserMapper", "findAll", "SELECT", "SELECT * FROM users", List.of("users"))
    );
    Set<String> reachableMappers = Set.of("com.example.mapper.ProductMapper#findAll");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されないことを確認
    assertThat(links).isEmpty();
  }

  @Test
  @DisplayName("非Mapperクラスでは制限付きマッチングが動作しないこと")
  void testSearchMapper_WithNonMapperClass_ShouldNotCreateCrudLinks() {
    // Given - 非Mapperクラスのテストデータ
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.service.UserService", "findAll", "SELECT", "SELECT * FROM users", List.of("users"))
    );
    Set<String> reachableMappers = Set.of("some.other.UserService#differentMethod");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されないことを確認
    assertThat(links).isEmpty();
  }

  @Test
  @DisplayName("空のSQLマッピングリストでCRUDリンクが生成されないこと")
  void testSearchMapper_WithEmptySqlList_ShouldNotCreateCrudLinks() {
    // Given - 空のSQLマッピングリスト
    List<SqlMapping> sqls = List.of();
    Set<String> reachableMappers = Set.of("com.example.mapper.UserMapper#findAll");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されないことを確認
    assertThat(links).isEmpty();
  }

  @Test
  @DisplayName("空の到達可能MapperセットでCRUDリンクが生成されないこと")
  void testSearchMapper_WithEmptyReachableMappers_ShouldNotCreateCrudLinks() {
    // Given - 空の到達可能Mapperセット
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.mapper.UserMapper", "findAll", "SELECT", "SELECT * FROM users", List.of("users"))
    );
    Set<String> reachableMappers = Set.of();

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されないことを確認
    assertThat(links).isEmpty();
  }

  @Test
  @DisplayName("複数のSQLマッピングで適切にマッチングが動作すること")
  void testSearchMapper_WithMultipleSqlMappings_ShouldCreateCorrectCrudLinks() {
    // Given - 複数のSQLマッピング（一部は一致、一部は不一致）
    List<SqlMapping> sqls = List.of(
        new SqlMapping("com.example.mapper.UserMapper", "findAll", "SELECT", "SELECT * FROM users", List.of("users")),
        new SqlMapping("com.example.mapper.ProductMapper", "insert", "INSERT", "INSERT INTO products (name) VALUES (?)", List.of("products")),
        new SqlMapping("com.example.mapper.OrderMapper", "update", "UPDATE", "UPDATE orders SET status = ? WHERE id = ?", List.of("orders"))
    );
    Set<String> reachableMappers = Set.of(
        "com.example.mapper.UserMapper#findAll",
        "com.example.mapper.OrderMapper#update"
    );

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - 一致するもののみCRUDリンクが生成されることを確認
    assertThat(links)
        .hasSize(2)
        .extracting(CrudLink::table)
        .containsExactlyInAnyOrder("users", "orders");
    
    assertThat(links)
        .extracting(CrudLink::crud)
        .containsExactlyInAnyOrder("S", "U");
  }

  @Test
  @DisplayName("nullのmapperClassで例外が発生しないこと")
  void testSearchMapper_WithNullMapperClass_ShouldNotThrowException() {
    // Given - nullのmapperClassを含むSQLマッピング
    List<SqlMapping> sqls = List.of(
        new SqlMapping(null, "findAll", "SELECT", "SELECT * FROM users", List.of("users"))
    );
    Set<String> reachableMappers = Set.of("UserMapper#findAll");

    // When & Then - 例外が発生しないことを確認
    assertThatCode(() -> target.searchMapper(sqls, links, testEndpoint, reachableMappers))
        .doesNotThrowAnyException();
    
    // 部分一致ロジックによりCRUDリンクが生成されることを確認
    assertThat(links)
        .hasSize(1)
        .first()
        .satisfies(link -> {
          assertThat(link.ep()).isEqualTo(testEndpoint);
          assertThat(link.table()).isEqualTo("users");
          assertThat(link.crud()).isEqualTo("S");
        });
  }

  @Test
  @DisplayName("空文字列のmapperClassで適切に処理されること")
  void testSearchMapper_WithEmptyMapperClass_ShouldHandleCorrectly() {
    // Given - 空文字列のmapperClassを含むSQLマッピング
    List<SqlMapping> sqls = List.of(
        new SqlMapping("", "findAll", "SELECT", "SELECT * FROM users", List.of("users"))
    );
    Set<String> reachableMappers = Set.of("#findAll");

    // When - マッチング処理を実行
    target.searchMapper(sqls, links, testEndpoint, reachableMappers);

    // Then - CRUDリンクが生成されることを確認
    assertThat(links)
        .hasSize(1)
        .first()
        .satisfies(link -> {
          assertThat(link.ep()).isEqualTo(testEndpoint);
          assertThat(link.table()).isEqualTo("users");
          assertThat(link.crud()).isEqualTo("S");
        });
  }
}
