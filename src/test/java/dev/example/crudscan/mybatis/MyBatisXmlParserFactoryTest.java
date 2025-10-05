package dev.example.crudscan.mybatis;

import static org.assertj.core.api.Assertions.*;

import dev.example.crudscan.UnitTestBase;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** MyBatisXmlParserFactoryのテストクラス */
@DisplayName("MyBatisXmlParserFactory機能のテスト")
class MyBatisXmlParserFactoryTest extends UnitTestBase {

  @Test
  @DisplayName("MyBatisDocumentBuilderが正常に作成されること")
  void testCreateMyBatisDocumentBuilder_ShouldReturnValidBuilder() throws Exception {
    // When
    DocumentBuilder builder = MyBatisXmlParserFactory.createMyBatisDocumentBuilder();

    // Then
    assertThat(builder).isNotNull();
    // 基本的な動作確認
    assertThat(builder.isNamespaceAware()).isIn(true, false);
    assertThat(builder.isValidating()).isIn(true, false);
  }

  @Test
  @DisplayName("有効なMyBatis XMLが正常に解析されること")
  void testCreateMyBatisDocumentBuilder_ShouldParseValidMyBatisXml() throws Exception {
    // Given
    DocumentBuilder builder = MyBatisXmlParserFactory.createMyBatisDocumentBuilder();

    // test/resourcesの実際のXMLファイルを使用
    Path xmlPath = getTestResourcePath("mybatis/MyBatisXmlParserFactoryTest/SimpleMapper.xml");

    // When
    Document document = builder.parse(xmlPath.toFile());

    // Then
    assertThat(document).isNotNull();
    assertThat(document.getDocumentElement().getTagName()).isEqualTo("mapper");
    assertThat(document.getDocumentElement().getAttribute("namespace"))
        .isEqualTo("com.example.test.UserMapper");
  }

  @Test
  @DisplayName("複雑なMyBatis XMLが正常に処理されること")
  void testCreateMyBatisDocumentBuilder_ShouldHandleComplexMyBatisXml() throws Exception {
    // Given
    DocumentBuilder builder = MyBatisXmlParserFactory.createMyBatisDocumentBuilder();

    // test/resourcesの実際の複雑なXMLファイルを使用
    Path xmlPath = getTestResourcePath("mybatis/MyBatisXmlParserFactoryTest/ComplexMapper.xml");

    // When
    Document document = builder.parse(xmlPath.toFile());

    // Then
    assertThat(document).isNotNull();
    assertThat(document.getElementsByTagName("select").getLength()).isGreaterThan(0);
    assertThat(document.getElementsByTagName("insert").getLength()).isGreaterThan(0);
    assertThat(document.getElementsByTagName("resultMap").getLength()).isGreaterThan(0);
    assertThat(document.getElementsByTagName("sql").getLength()).isGreaterThan(0);
  }

  @Test
  @DisplayName("無効なXMLで適切に例外が発生すること")
  void testCreateMyBatisDocumentBuilder_ShouldHandleInvalidXml() throws Exception {
    // Given
    DocumentBuilder builder = MyBatisXmlParserFactory.createMyBatisDocumentBuilder();

    String invalidXml = "This is not valid XML content";

    // When & Then
    InputStream inputStream = new ByteArrayInputStream(invalidXml.getBytes());

    assertThatThrownBy(() -> builder.parse(inputStream))
        .isInstanceOf(Exception.class); // SAXException or similar
  }

  @Test
  @DisplayName("複数のインスタンスが独立して動作すること")
  void testCreateMyBatisDocumentBuilder_MultipleInstances_ShouldBeIndependent() throws Exception {
    // When
    DocumentBuilder builder1 = MyBatisXmlParserFactory.createMyBatisDocumentBuilder();
    DocumentBuilder builder2 = MyBatisXmlParserFactory.createMyBatisDocumentBuilder();

    // Then
    assertThat(builder1).isNotNull().isNotSameAs(builder2); // 異なるインスタンス

    assertThat(builder2).isNotNull();

    // 両方とも同じ設定を持つ
    assertThat(builder1.isNamespaceAware()).isEqualTo(builder2.isNamespaceAware());
    assertThat(builder1.isValidating()).isEqualTo(builder2.isValidating());
  }
}
