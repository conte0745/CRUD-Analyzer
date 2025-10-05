package dev.example.crudscan.mybatis;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyBatisXML解析専用の安全なXMLパーサーを提供するファクトリクラス
 *
 * <p>このクラスは、MyBatis XMLマッピングファイルの解析に特化した 安全な設定が適用されたDocumentBuilderを提供します。
 *
 * <h2>MyBatis対応の安全対策</h2>
 *
 * <ul>
 *   <li>DOCTYPE宣言を許可（MyBatis XMLファイルに必要）
 *   <li>外部一般エンティティの無効化
 *   <li>外部パラメータエンティティの無効化
 *   <li>外部DTD読み込みの無効化（ローカルDTDのみ許可）
 *   <li>XInclude処理の無効化
 *   <li>エンティティ参照展開の制限
 * </ul>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public class MyBatisXmlParserFactory {
  private static final Logger logger = LoggerFactory.getLogger(MyBatisXmlParserFactory.class);

  /** プライベートコンストラクタ - ユーティリティクラスのため */
  private MyBatisXmlParserFactory() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * MyBatis XML解析用の安全なDocumentBuilderを作成
   *
   * @return MyBatis XML解析に適した安全設定のDocumentBuilder
   * @throws ParserConfigurationException パーサー設定エラー
   */
  public static DocumentBuilder createMyBatisDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); // ignore
    setMyBatisSafeXmlFeatures(dbf);
    return dbf.newDocumentBuilder();
  }

  /**
   * DocumentBuilderFactoryにMyBatis XML解析用の安全設定を適用
   *
   * @param dbf 設定対象のDocumentBuilderFactory
   */
  private static void setMyBatisSafeXmlFeatures(DocumentBuilderFactory dbf) {
    try {
      // MyBatis対応: DOCTYPE宣言を許可（MyBatis XMLファイルに必要）
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);

      // XXE対策: 外部一般エンティティを無効化
      dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);

      // XXE対策: 外部パラメータエンティティを無効化
      dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

      // XXE対策: 外部DTD読み込みを無効化（ローカルDTDは許可）
      dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

      // XXE対策: XInclude処理を無効化
      dbf.setXIncludeAware(false);

      // XXE対策: エンティティ参照展開を制限
      dbf.setExpandEntityReferences(false);

      // 名前空間を有効化（MyBatis XMLで使用される場合がある）
      dbf.setNamespaceAware(true);

      // 検証を無効化（DTDによる検証は不要）
      dbf.setValidating(false);

      logger.debug("MyBatis XML解析用の安全設定を適用しました");

    } catch (ParserConfigurationException ex) {
      logger.warn(
          "MyBatis XML解析: DocumentBuilderFactoryに安全な設定を適用できませんでした: {}", ex.getMessage(), ex);
      // 設定に失敗した場合でも処理を続行（デフォルト設定で動作させる）
    } catch (IllegalArgumentException ex) {
      logger.warn("MyBatis XML解析: 不正な設定パラメータが指定されました: {}", ex.getMessage(), ex);
      // 設定に失敗した場合でも処理を続行（デフォルト設定で動作させる）
    }
  }
}
