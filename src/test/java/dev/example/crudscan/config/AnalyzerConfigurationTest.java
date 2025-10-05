package dev.example.crudscan.config;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** AnalyzerConfigurationのテストクラス */
@DisplayName("AnalyzerConfiguration機能のテスト")
class AnalyzerConfigurationTest {

  @TempDir Path tempDir;

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  @BeforeEach
  void setUpStreams() {
    System.setOut(new PrintStream(outContent));
  }

  @AfterEach
  void restoreStreams() {
    System.setOut(originalOut);
  }

  @Test
  void testConstructor_WithEmptyArgs_ShouldUseDefaults() {
    // When
    AnalyzerConfiguration config = new AnalyzerConfiguration(new String[] {});

    // Then
    assertThat(config).isNotNull();
    assertThat(config.getSourceDirectory()).isNotNull();
    assertThat(config.getResourcesDirectory()).isNotNull();
    assertThat(config.getOutputDirectory()).isNotNull();
    assertThat(config.isIncludeGenerated()).isTrue();
    assertThat(config.isIncludeDynamicSql()).isTrue();
  }

  @Test
  void testDefaultConstructor_ShouldWork() {
    // When
    AnalyzerConfiguration config = new AnalyzerConfiguration();

    // Then
    assertThat(config).isNotNull();
    assertThat(config.getSourceDirectory()).isNotNull();
    assertThat(config.getResourcesDirectory()).isNotNull();
    assertThat(config.getOutputDirectory()).isNotNull();
  }

  @Test
  void testConstructor_WithThreeArgs_ShouldUseProvidedPaths() throws Exception {
    // Given
    Path srcDir = tempDir.resolve("src");
    Path resourcesDir = tempDir.resolve("resources");
    Path outputDir = tempDir.resolve("output");

    Files.createDirectories(srcDir);
    Files.createDirectories(resourcesDir);
    Files.createDirectories(outputDir);

    String[] args = {srcDir.toString(), resourcesDir.toString(), outputDir.toString()};

    // When
    AnalyzerConfiguration config = new AnalyzerConfiguration(args);

    // Then
    assertThat(config.getSourceDirectory()).isEqualTo(srcDir.toAbsolutePath().normalize());
    assertThat(config.getResourcesDirectory()).isEqualTo(resourcesDir.toAbsolutePath().normalize());
    assertThat(config.getOutputDirectory()).isEqualTo(outputDir.toAbsolutePath().normalize());
  }

  @Test
  void testLogConfiguration_ShouldPrintConfigurationInfo() {
    // Given
    AnalyzerConfiguration config = new AnalyzerConfiguration();

    // When
    config.logConfiguration();

    // Then
    String output = outContent.toString();
    assertThat(output)
        .containsAnyOf("設定情報", "Configuration", "config")
        .containsAnyOf("ソースディレクトリ", "Source", "src")
        .containsAnyOf("リソースディレクトリ", "Resources", "resources")
        .containsAnyOf("出力ディレクトリ", "Output", "output");
  }

  @Test
  void testOutputFlags_ShouldReturnDefaults() {
    // Given
    AnalyzerConfiguration config = new AnalyzerConfiguration();

    // When & Then
    assertThat(config.isMarkdownOutputEnabled()).isTrue();
    assertThat(config.isPlantUmlOutputEnabled()).isTrue();
    assertThat(config.isJsonOutputEnabled()).isTrue();
  }

  @Test
  void testAnalysisFlags_ShouldReturnDefaults() {
    // Given
    AnalyzerConfiguration config = new AnalyzerConfiguration();

    // When & Then
    assertThat(config.isIncludeGenerated()).isTrue();
    assertThat(config.isIncludeDynamicSql()).isTrue();
  }
}
