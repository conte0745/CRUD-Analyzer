package dev.example.crudscan.performance;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.CrudAnalyzer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * パフォーマンステストクラス
 *
 * <p>大量のファイルやデータに対する処理性能をテストします。
 */
@DisplayName("パフォーマンステスト")
class PerformanceTest {

  private CrudAnalyzer analyzer;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    analyzer = new CrudAnalyzer();
  }

  @Test
  void testAnalyze_WithLargeNumberOfFiles_ShouldCompleteInReasonableTime() throws Exception {
    // Given
    Path srcDir = tempDir.resolve("src");
    Path resourcesDir = tempDir.resolve("resources");
    Path outputDir = tempDir.resolve("output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // 複数のテストファイルを作成
    createMultipleTestFiles(srcDir, 10);
    createMultipleXmlFiles(resourcesDir, 5);

    // When
    Instant start = Instant.now();
    analyzer.analyze(srcDir, resourcesDir, outputDir);
    Instant end = Instant.now();

    // Then
    Duration duration = Duration.between(start, end);

    // 10秒以内で完了することを期待
    assertThat(duration).isLessThan(Duration.ofSeconds(10));

    // 出力ファイルが作成されることを確認
    assertThat(outputDir.resolve("crud-matrix.md")).exists();
  }

  @Test
  void testAnalyze_WithEmptyProject_ShouldCompleteQuickly() throws Exception {
    // Given
    Path srcDir = tempDir.resolve("empty_src");
    Path resourcesDir = tempDir.resolve("empty_resources");
    Path outputDir = tempDir.resolve("output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    // When
    Instant start = Instant.now();
    analyzer.analyze(srcDir, resourcesDir, outputDir);
    Instant end = Instant.now();

    // Then
    Duration duration = Duration.between(start, end);

    // 空のプロジェクトは1秒以内で完了することを期待
    assertThat(duration).isLessThan(Duration.ofSeconds(1));
  }

  /** 複数のテストJavaファイルを作成 */
  private void createMultipleTestFiles(Path srcDir, int count) throws Exception {
    for (int i = 0; i < count; i++) {
      Path packageDir = srcDir.resolve("com/example/test" + i);
      Files.createDirectories(packageDir);

      String controllerContent =
          String.format(
              """
          package com.example.test%d;

          import org.springframework.web.bind.annotation.*;

          @RestController
          @RequestMapping("/api/test%d")
          public class TestController%d {

              @GetMapping("/hello")
              public String hello() {
                  return "Hello from controller %d!";
              }

              @PostMapping("/create")
              public String create(@RequestBody String data) {
                  return "Created in controller %d: " + data;
              }
          }
          """,
              i, i, i, i, i);

      Files.writeString(packageDir.resolve("TestController" + i + ".java"), controllerContent);
    }
  }

  /** 複数のテストXMLファイルを作成 */
  private void createMultipleXmlFiles(Path resourcesDir, int count) throws Exception {
    Path mapperDir = resourcesDir.resolve("mapper");
    Files.createDirectories(mapperDir);

    for (int i = 0; i < count; i++) {
      String xmlContent =
          String.format(
              """
          <?xml version="1.0" encoding="UTF-8"?>
          <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
                  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
          <mapper namespace="com.example.test%d.TestMapper%d">
              <select id="findById" resultType="String">
                  SELECT name FROM test_table_%d WHERE id = #{id}
              </select>

              <insert id="insertData">
                  INSERT INTO test_table_%d (name, value) VALUES (#{name}, #{value})
              </insert>
          </mapper>
          """,
              i, i, i, i);

      Files.writeString(mapperDir.resolve("TestMapper" + i + ".xml"), xmlContent);
    }
  }
}
