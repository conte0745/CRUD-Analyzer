package dev.example.crudscan.model;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.model.Models.*;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Modelsのテストクラス */
@DisplayName("Models機能のテスト")
class ModelsTest {

  @Test
  @DisplayName("Modelsクラスのインスタンスが正常に作成されること")
  void testModelsConstructor_ShouldCreateInstance() {
    // When
    Models models = new Models();

    // Then
    assertThat(models).isNotNull();
  }

  @Test
  @DisplayName("Endpointレコードが正常に作成されること")
  void testEndpoint_ShouldCreateCorrectly() {
    // Given
    String httpMethod = "GET";
    String url = "/api/users/{id}";
    String controller = "UserController";
    String method = "getUser";
    String packageName = "com.example.controller";

    // When
    Endpoint endpoint = new Endpoint(httpMethod, url, controller, method, packageName);

    // Then
    assertThat(endpoint.httpMethod()).isEqualTo(httpMethod);
    assertThat(endpoint.url()).isEqualTo(url);
    assertThat(endpoint.controller()).isEqualTo(controller);
    assertThat(endpoint.method()).isEqualTo(method);
    assertThat(endpoint.packageName()).isEqualTo(packageName);
  }

  @Test
  @DisplayName("CallEdgeレコードが正常に作成されること")
  void testCallEdge_ShouldCreateCorrectly() {
    // Given
    String fromClass = "UserController";
    String fromMethod = "getUser";
    String toClass = "UserService";
    String toMethod = "findById";

    // When
    CallEdge callEdge = new CallEdge(fromClass, fromMethod, toClass, toMethod);

    // Then
    assertThat(callEdge.fromClass()).isEqualTo(fromClass);
    assertThat(callEdge.fromMethod()).isEqualTo(fromMethod);
    assertThat(callEdge.toClass()).isEqualTo(toClass);
    assertThat(callEdge.toMethod()).isEqualTo(toMethod);
  }

  @Test
  @DisplayName("SqlMappingレコードが正常に作成されること")
  void testSqlMapping_ShouldCreateCorrectly() {
    // Given
    String mapperClass = "com.example.repository.UserRepository";
    String mapperMethod = "findById";
    String op = "SELECT";
    String rawSql = "SELECT * FROM users WHERE id = #{id}";
    List<String> tables = List.of("users");

    // When
    SqlMapping sqlMapping = new SqlMapping(mapperClass, mapperMethod, op, rawSql, tables);

    // Then
    assertThat(sqlMapping.mapperClass()).isEqualTo(mapperClass);
    assertThat(sqlMapping.mapperMethod()).isEqualTo(mapperMethod);
    assertThat(sqlMapping.op()).isEqualTo(op);
    assertThat(sqlMapping.rawSql()).isEqualTo(rawSql);
    assertThat(sqlMapping.tables()).isEqualTo(tables);
  }

  @Test
  @DisplayName("CrudLinkレコードが正常に作成されること")
  void testCrudLink_ShouldCreateCorrectly() {
    // Given
    Endpoint endpoint =
        new Endpoint("GET", "/users", "UserController", "getUsers", "com.example.controller");
    String table = "users";
    String crud = "R";

    // When
    CrudLink crudLink = new CrudLink(endpoint, table, crud);

    // Then
    assertThat(crudLink.ep()).isEqualTo(endpoint);
    assertThat(crudLink.table()).isEqualTo(table);
    assertThat(crudLink.crud()).isEqualTo(crud);
  }

  @Test
  @DisplayName("BatchJobレコードが正常に作成されること")
  void testBatchJob_ShouldCreateCorrectly() {
    // Given
    String className = "com.example.batch.DataProcessJob";
    String jobName = "DataProcessJob";
    String packageName = "com.example.batch";

    // When
    BatchJob batchJob = new BatchJob(className, jobName, packageName);

    // Then
    assertThat(batchJob.className()).isEqualTo(className);
    assertThat(batchJob.jobName()).isEqualTo(jobName);
    assertThat(batchJob.packageName()).isEqualTo(packageName);
  }

  @Test
  @DisplayName("normalizeHttpメソッドが正常に大文字化すること")
  void testNormalizeHttp_WithValidString_ShouldReturnUpperCase() {
    // When & Then
    assertThat(Models.normalizeHttp("get")).isEqualTo("GET");
    assertThat(Models.normalizeHttp("post")).isEqualTo("POST");
    assertThat(Models.normalizeHttp("PUT")).isEqualTo("PUT");
    assertThat(Models.normalizeHttp("Delete")).isEqualTo("DELETE");
    assertThat(Models.normalizeHttp("patch")).isEqualTo("PATCH");
  }

  @Test
  @DisplayName("normalizeHttpメソッドがnullに対して空文字列を返すこと")
  void testNormalizeHttp_WithNull_ShouldReturnEmptyString() {
    // When
    String result = Models.normalizeHttp(null);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("normalizeHttpメソッドが空文字列に対して空文字列を返すこと")
  void testNormalizeHttp_WithEmptyString_ShouldReturnEmptyString() {
    // When
    String result = Models.normalizeHttp("");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("normalizeHttpメソッドが空白文字列に対して空白文字列を大文字化して返すこと")
  void testNormalizeHttp_WithWhitespace_ShouldReturnUpperCaseWhitespace() {
    // When
    String result = Models.normalizeHttp("  ");

    // Then
    assertThat(result).isEqualTo("  ");
  }

  @Test
  @DisplayName("レコードのequalsメソッドが正常に動作すること")
  void testRecordEquals_ShouldWorkCorrectly() {
    // Given
    Endpoint endpoint1 = new Endpoint("GET", "/users", "UserController", "getUsers", "com.example");
    Endpoint endpoint2 = new Endpoint("GET", "/users", "UserController", "getUsers", "com.example");
    Endpoint endpoint3 =
        new Endpoint("POST", "/users", "UserController", "createUser", "com.example");

    // When & Then
    assertThat(endpoint1).isEqualTo(endpoint2);
    assertThat(endpoint1).isNotEqualTo(endpoint3);
  }

  @Test
  @DisplayName("レコードのhashCodeメソッドが正常に動作すること")
  void testRecordHashCode_ShouldWorkCorrectly() {
    // Given
    Endpoint endpoint1 = new Endpoint("GET", "/users", "UserController", "getUsers", "com.example");
    Endpoint endpoint2 = new Endpoint("GET", "/users", "UserController", "getUsers", "com.example");

    // When & Then
    assertThat(endpoint1.hashCode()).isEqualTo(endpoint2.hashCode());
  }

  @Test
  @DisplayName("レコードのtoStringメソッドが正常に動作すること")
  void testRecordToString_ShouldWorkCorrectly() {
    // Given
    Endpoint endpoint = new Endpoint("GET", "/users", "UserController", "getUsers", "com.example");

    // When
    String toString = endpoint.toString();

    // Then
    assertThat(toString).contains("Endpoint");
    assertThat(toString).contains("GET");
    assertThat(toString).contains("/users");
    assertThat(toString).contains("UserController");
    assertThat(toString).contains("getUsers");
    assertThat(toString).contains("com.example");
  }

  @Test
  @DisplayName("SqlMappingで複数テーブルが正常に処理されること")
  void testSqlMapping_WithMultipleTables_ShouldHandleCorrectly() {
    // Given
    List<String> multipleTables = List.of("users", "user_roles", "roles");
    SqlMapping mapping =
        new SqlMapping(
            "com.example.UserMapper",
            "findUsersWithRoles",
            "SELECT",
            "SELECT u.*, r.name FROM users u JOIN user_roles ur ON u.id = ur.user_id JOIN roles r ON ur.role_id = r.id",
            multipleTables);

    // When & Then
    assertThat(mapping.tables()).hasSize(3);
    assertThat(mapping.tables()).containsExactly("users", "user_roles", "roles");
  }

  @Test
  @DisplayName("空のテーブルリストでSqlMappingが作成できること")
  void testSqlMapping_WithEmptyTables_ShouldCreateCorrectly() {
    // Given
    List<String> emptyTables = List.of();
    SqlMapping mapping =
        new SqlMapping("com.example.Mapper", "customQuery", "SELECT", "SELECT 1", emptyTables);

    // When & Then
    assertThat(mapping.tables()).isEmpty();
  }
}
