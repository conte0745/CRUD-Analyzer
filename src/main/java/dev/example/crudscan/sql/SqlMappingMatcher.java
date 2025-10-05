package dev.example.crudscan.sql;

import dev.example.crudscan.model.Models.*;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQLマッピングとエンドポイントのマッチング処理を担当するクラス
 *
 * <p>到達可能なMapperメソッドとSQLマッピングを照合し、
 * 一致するものについてCRUDリンクを生成します。
 */
public class SqlMappingMatcher {
  private static final Logger logger = LoggerFactory.getLogger(SqlMappingMatcher.class);

  // リテラル定数
  private static final String MAPPER_SUFFIX = "Mapper";
  private static final String REPOSITORY_SUFFIX = "Repository";
  private static final String DAO_SUFFIX = "Dao";

  /**
   * SQLマッピングとエンドポイントをマッチングしてCRUDリンクを生成
   *
   * @param sqls SQLマッピングのリスト
   * @param links 生成されたCRUDリンクを追加するリスト
   * @param ep 対象のエンドポイント
   * @param reachableMappers 到達可能なMapperメソッドのセット
   */
  public void searchMapper(
      List<SqlMapping> sqls, List<CrudLink> links, Endpoint ep, Set<String> reachableMappers) {

    logSearchStart(ep, reachableMappers);

    for (var sql : sqls) {
      String fullMapperMethod = sql.mapperClass() + "#" + sql.mapperMethod();
      logger.debug("SQLマッピング検査: {}", fullMapperMethod);

      boolean matched = findMatch(sql, reachableMappers);

      if (matched) {
        createCrudLinks(sql, links, ep);
      }
    }
  }

  /**
   * SQLマッピングに対するマッチングを検索
   */
  private boolean findMatch(SqlMapping sql, Set<String> reachableMappers) {
    String fullMapperMethod = sql.mapperClass() + "#" + sql.mapperMethod();

    // 1. 完全修飾名での一致をチェック
    boolean matched = reachableMappers.contains(fullMapperMethod);

    // 2. 単純名での一致をチェック
    if (!matched) {
      matched = checkSimpleNameMatch(sql, reachableMappers);
    }

    // 3. 部分一致をチェック
    if (!matched) {
      matched = checkPartialMatch(sql, reachableMappers);
    }

    // 4. 制限付きマッチング
    if (!matched) {
      matched = checkRestrictedMatch(sql, reachableMappers);
    }

    return matched;
  }

  /**
   * 単純名でのマッチングをチェック
   */
  private boolean checkSimpleNameMatch(SqlMapping sql, Set<String> reachableMappers) {
    String simpleClassName = extractSimpleName(sql.mapperClass());
    String simpleMapperMethod = simpleClassName + "#" + sql.mapperMethod();
    boolean matched = reachableMappers.contains(simpleMapperMethod);

    logger.debug("単純名チェック: {} -> 一致: {}", simpleMapperMethod, matched);
    return matched;
  }

  /**
   * 部分一致をチェック
   */
  private boolean checkPartialMatch(SqlMapping sql, Set<String> reachableMappers) {
    String targetMethod = sql.mapperMethod();
    String fullMapperMethodPattern = sql.mapperClass() + "#" + targetMethod;

    // より厳密な一致: 完全なクラス名#メソッド名の組み合わせで確認
    boolean matched = reachableMappers.stream()
        .anyMatch(mapper -> mapper.equals(fullMapperMethodPattern));

    // 完全一致しない場合のみ部分一致を試行（クラス名のみチェック）
    if (!matched) {
      String simpleClassName = extractSimpleName(sql.mapperClass());
      matched = reachableMappers.stream()
          .anyMatch(mapper ->
              mapper.contains(simpleClassName) && mapper.endsWith("#" + targetMethod));
    }

    if (matched) {
      logger.debug("部分一致成功: {} with method {}", sql.mapperClass(), targetMethod);
    }

    return matched;
  }

  /**
   * 制限付きマッチングをチェック
   */
  private boolean checkRestrictedMatch(SqlMapping sql, Set<String> reachableMappers) {
    if (!isMapperClass(sql.mapperClass())) {
      return false;
    }

    String simpleMapperClass = extractSimpleName(sql.mapperClass());
    boolean matched = reachableMappers.stream().anyMatch(mapper ->
        mapper.contains(simpleMapperClass));

    if (matched) {
      logger.debug("制限付きマッチング: {}", sql.mapperClass());
    }

    return matched;
  }

  /**
   * CRUDリンクを生成
   */
  private void createCrudLinks(SqlMapping sql, List<CrudLink> links, Endpoint ep) {
    String fullMapperMethod = sql.mapperClass() + "#" + sql.mapperMethod();
    logger.debug("SQLマッピング一致: {} -> {}", fullMapperMethod, sql.tables());
    logger.debug("CRUDリンク生成: {} -> {}", ep, sql.tables());

    for (String table : sql.tables()) {
      links.add(new CrudLink(ep, table, sql.op().substring(0, 1)));
    }
  }

  /**
   * 検索開始時のログ出力
   */
  private void logSearchStart(Endpoint ep, Set<String> reachableMappers) {
    logger.debug("=== エンドポイント {} のMapper検索開始 ===", ep);
    logger.debug("到達可能Mapperメソッド数: {}", reachableMappers.size());
    if (logger.isDebugEnabled() && !reachableMappers.isEmpty()) {
      logger.debug("到達可能Mapperメソッド: {}", reachableMappers);
    }
  }

  /**
   * 完全修飾名から単純クラス名を抽出
   */
  private String extractSimpleName(String fqn) {
    if (fqn == null) return "";
    int lastDot = fqn.lastIndexOf('.');
    return lastDot >= 0 ? fqn.substring(lastDot + 1) : fqn;
  }

  /**
   * Mapperクラスかどうかを判定
   */
  private boolean isMapperClass(String className) {
    return className.endsWith(MAPPER_SUFFIX)
        || className.endsWith(REPOSITORY_SUFFIX)
        || className.endsWith(DAO_SUFFIX);
  }
}
