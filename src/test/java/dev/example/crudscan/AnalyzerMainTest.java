package dev.example.crudscan;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AnalyzerMainのテストクラス */
@DisplayName("AnalyzerMainクラスのテスト")
class AnalyzerMainTest {

  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private ByteArrayOutputStream outContent;
  private ByteArrayOutputStream errContent;

  @BeforeEach
  void setUp() {
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  @DisplayName("コンストラクタでインスタンスが正常に作成されること")
  void testConstructor_ShouldCreateInstance() {
    // When
    AnalyzerMain main = new AnalyzerMain();

    // Then
    assertThat(main).isNotNull();
  }

  @Test
  @DisplayName("引数なしでmainを実行した場合に設定ファイルから設定が読み込まれること")
  void testMain_WithNoArgs_ShouldLoadFromConfig() {
    // When & Then
    // 設定ファイルが存在する場合は正常に実行される
    assertThatCode(() -> AnalyzerMain.main(new String[0])).doesNotThrowAnyException();
  }
}
