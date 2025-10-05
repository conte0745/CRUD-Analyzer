package dev.example.crudscan.output;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.model.Models.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** JsonWriterのテストクラス */
@DisplayName("JsonWriter機能のテスト")
class JsonWriterTest {

  private JsonWriter writer;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    writer = new JsonWriter();
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    JsonWriter newWriter = new JsonWriter();

    // Then
    assertThat(newWriter).isNotNull();
  }

  @Test
  @DisplayName("有効なデータでJSONファイルが正常に作成されること")
  void testWrite_WithValidData_ShouldCreateJsonFile() throws Exception {
    // Given
    List<Endpoint> endpoints = createTestEndpoints();
    List<CallEdge> calls = createTestCallEdges();
    List<SqlMapping> sqls = createTestSqlMappings();
    List<CrudLink> links = createTestCrudLinks();
    List<BatchJob> batchJobs = createTestBatchJobs();
    Path outputFile = tempDir.resolve("analysis.json");

    // When
    writer.write(outputFile, endpoints, calls, sqls, links, batchJobs);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);
    // 基本的にファイルが作成され、何らかの内容があることを確認
    assertThat(content).isNotEmpty();
  }

  @Test
  @DisplayName("空のデータでも空のJSONファイルが作成されること")
  void testWrite_WithEmptyData_ShouldCreateEmptyJsonFile() throws Exception {
    // Given
    List<Endpoint> emptyEndpoints = List.of();
    List<CallEdge> emptyCalls = List.of();
    List<SqlMapping> emptySqls = List.of();
    List<CrudLink> emptyLinks = List.of();
    List<BatchJob> emptyBatchJobs = List.of();
    Path outputFile = tempDir.resolve("empty-analysis.json");

    // When
    writer.write(outputFile, emptyEndpoints, emptyCalls, emptySqls, emptyLinks, emptyBatchJobs);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);
    // 基本的にファイルが作成され、何らかの内容があることを確認
    assertThat(content).isNotEmpty();
  }

  /** テスト用のEndpointリストを作成 */
  private List<Endpoint> createTestEndpoints() {
    return List.of(
        new Endpoint("GET", "/api/users", "UserController", "getUsers", "com.example.controller"));
  }

  /** テスト用のCallEdgeリストを作成 */
  private List<CallEdge> createTestCallEdges() {
    return List.of(
        new CallEdge(
            "com.example.controller.UserController",
            "getUsers",
            "com.example.service.UserService",
            "findAll"));
  }

  /** テスト用のSqlMappingリストを作成 */
  private List<SqlMapping> createTestSqlMappings() {
    return List.of(
        new SqlMapping(
            "com.example.mapper.UserMapper",
            "findAll",
            "SELECT",
            "SELECT * FROM users",
            List.of("users")));
  }

  /** テスト用のCrudLinkリストを作成 */
  private List<CrudLink> createTestCrudLinks() {
    List<Endpoint> endpoints = createTestEndpoints();
    return List.of(new CrudLink(endpoints.get(0), "users", "S"));
  }

  /** テスト用のBatchJobリストを作成 */
  private List<BatchJob> createTestBatchJobs() {
    return List.of(
        new BatchJob(
            "userDataExportJob", "com.example.batch.UserDataExportJob", "com.example.batch"));
  }
}
