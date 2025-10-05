package dev.example.crudscan.output;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.UnitTestBase;
import dev.example.crudscan.model.Models.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** PlantumlWriterのテストクラス */
@DisplayName("PlantumlWriter機能のテスト")
class PlantumlWriterTest extends UnitTestBase {

  private PlantumlWriter writer;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    writer = new PlantumlWriter();
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    PlantumlWriter newWriter = new PlantumlWriter();

    // Then
    assertThat(newWriter).isNotNull();
  }

  @Test
  @DisplayName("空のCRUDリンクでPlantUMLファイルが正常に出力されること")
  void testWrite_WithEmptyLinks_ShouldCreateValidPlantUMLFile() throws Exception {
    // Given
    Path outputFile = tempDir.resolve("empty.puml");
    List<CrudLink> emptyLinks = List.of();

    // When
    writer.write(outputFile, emptyLinks);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);
    assertThat(content).startsWith("@startuml").endsWith("@enduml\n");
  }

  @Test
  @DisplayName("CRUDリンクを含むPlantUMLファイルが正常に出力されること")
  void testWrite_WithCrudLinks_ShouldCreateValidPlantUMLFile() throws Exception {
    // Given
    Path outputFile = tempDir.resolve("crud.puml");
    List<CrudLink> links = createSampleCrudLinks();

    // When
    writer.write(outputFile, links);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    assertThat(content)
        .startsWith("@startuml")
        .endsWith("@enduml\n")
        .contains("actor User")
        .contains("UserController")
        .contains("GET /users")
        .contains("users")
        .contains("SELECT");
  }

  @Test
  @DisplayName("バッチJobを含むPlantUMLファイルが正常に出力されること")
  void testWrite_WithBatchJobs_ShouldCreateValidPlantUMLFile() throws Exception {
    // Given
    Path outputFile = tempDir.resolve("batch.puml");
    List<CrudLink> links = List.of();
    List<BatchJob> batchJobs = createSampleBatchJobs();
    List<CallEdge> edges = createSampleCallEdges();

    // When
    writer.write(outputFile, links, batchJobs, edges);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    assertThat(content)
        .startsWith("@startuml")
        .endsWith("@enduml\n")
        .contains("participant Scheduler")
        .contains("DataProcessJob")
        .contains("trigger");
  }

  @Test
  @DisplayName("CRUDリンクとバッチJobの両方を含むPlantUMLファイルが正常に出力されること")
  void testWrite_WithBothCrudLinksAndBatchJobs_ShouldCreateValidPlantUMLFile() throws Exception {
    // Given
    Path outputFile = tempDir.resolve("complete.puml");
    List<CrudLink> links = createSampleCrudLinks();
    List<BatchJob> batchJobs = createSampleBatchJobs();
    List<CallEdge> edges = createSampleCallEdges();

    // When
    writer.write(outputFile, links, batchJobs, edges);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    assertThat(content)
        .startsWith("@startuml")
        .endsWith("@enduml\n")
        .contains("actor User")
        .contains("participant Scheduler")
        .contains("UserController")
        .contains("DataProcessJob");
  }

  @Test
  @DisplayName("出力ディレクトリが存在しない場合でも正常に作成されること")
  void testWrite_WithNonExistentDirectory_ShouldCreateDirectory() throws Exception {
    // Given
    Path outputDir = tempDir.resolve("non-existent/sub-dir");
    Path outputFile = outputDir.resolve("test.puml");
    List<CrudLink> links = createSampleCrudLinks();

    // When
    writer.write(outputFile, links);

    // Then
    assertThat(outputDir).exists();
    assertThat(outputFile).exists();
  }

  @Test
  @DisplayName("大量のCRUDリンクがある場合でも制限されて出力されること")
  void testWrite_WithManyLinks_ShouldLimitOutput() throws Exception {
    // Given
    Path outputFile = tempDir.resolve("many-links.puml");
    List<CrudLink> manyLinks = createManyLinks();

    // When
    writer.write(outputFile, manyLinks);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // 最大8個のリンクまでしか出力されないことを確認
    long linkCount = content.lines().filter(line -> line.contains("User ->")).count();
    assertThat(linkCount).isLessThanOrEqualTo(8);
  }

  @Test
  @DisplayName("大量のバッチJobがある場合でも制限されて出力されること")
  void testWrite_WithManyBatchJobs_ShouldLimitOutput() throws Exception {
    // Given
    Path outputFile = tempDir.resolve("many-jobs.puml");
    List<CrudLink> links = List.of();
    List<BatchJob> manyJobs = createManyBatchJobs();
    List<CallEdge> edges = List.of();

    // When
    writer.write(outputFile, links, manyJobs, edges);

    // Then
    assertThat(outputFile).exists();
    String content = Files.readString(outputFile);

    // 最大5個のJobまでしか出力されないことを確認
    long jobCount = content.lines().filter(line -> line.contains("Scheduler ->")).count();
    assertThat(jobCount).isLessThanOrEqualTo(5);
  }

  /** サンプルCRUDリンクを作成 */
  private List<CrudLink> createSampleCrudLinks() {
    Endpoint endpoint =
        new Endpoint("GET", "/users", "UserController", "findUsers", "com.example.controller");
    return List.of(new CrudLink(endpoint, "users", "SELECT"));
  }

  /** サンプルバッチJobを作成 */
  private List<BatchJob> createSampleBatchJobs() {
    return List.of(
        new BatchJob("com.example.batch.DataProcessJob", "DataProcessJob", "com.example.batch"));
  }

  /** サンプル呼び出し関係を作成 */
  private List<CallEdge> createSampleCallEdges() {
    return List.of(
        new CallEdge(
            "com.example.batch.DataProcessJob", "execute",
            "com.example.service.UserService", "processUsers"));
  }

  /** 大量のCRUDリンクを作成 */
  private List<CrudLink> createManyLinks() {
    return List.of(
        new CrudLink(
            new Endpoint("GET", "/path1", "Controller1", "method1", "com.example"),
            "table1",
            "SELECT"),
        new CrudLink(
            new Endpoint("POST", "/path2", "Controller2", "method2", "com.example"),
            "table2",
            "INSERT"),
        new CrudLink(
            new Endpoint("PUT", "/path3", "Controller3", "method3", "com.example"),
            "table3",
            "UPDATE"),
        new CrudLink(
            new Endpoint("DELETE", "/path4", "Controller4", "method4", "com.example"),
            "table4",
            "DELETE"),
        new CrudLink(
            new Endpoint("GET", "/path5", "Controller5", "method5", "com.example"),
            "table5",
            "SELECT"),
        new CrudLink(
            new Endpoint("POST", "/path6", "Controller6", "method6", "com.example"),
            "table6",
            "INSERT"),
        new CrudLink(
            new Endpoint("PUT", "/path7", "Controller7", "method7", "com.example"),
            "table7",
            "UPDATE"),
        new CrudLink(
            new Endpoint("DELETE", "/path8", "Controller8", "method8", "com.example"),
            "table8",
            "DELETE"),
        new CrudLink(
            new Endpoint("GET", "/path9", "Controller9", "method9", "com.example"),
            "table9",
            "SELECT"),
        new CrudLink(
            new Endpoint("POST", "/path10", "Controller10", "method10", "com.example"),
            "table10",
            "INSERT"));
  }

  /** 大量のバッチJobを作成 */
  private List<BatchJob> createManyBatchJobs() {
    return List.of(
        new BatchJob("Job1", "Job1", "package1"),
        new BatchJob("Job2", "Job2", "package2"),
        new BatchJob("Job3", "Job3", "package3"),
        new BatchJob("Job4", "Job4", "package4"),
        new BatchJob("Job5", "Job5", "package5"),
        new BatchJob("Job6", "Job6", "package6"),
        new BatchJob("Job7", "Job7", "package7"));
  }
}
