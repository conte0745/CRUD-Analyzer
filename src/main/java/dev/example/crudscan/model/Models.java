package dev.example.crudscan.model;

import java.util.*;

/**
 * CRUD解析で使用される各種データモデルの定義
 *
 * <p>このクラスは、CRUD解析プロセス全体で使用される主要なデータ構造を Recordとして定義しています。すべてのモデルはイミュータブルで、 データの整合性と安全性を保証します。
 *
 * <h2>含まれるモデル</h2>
 *
 * <ul>
 *   <li>{@link Endpoint} - RESTエンドポイント情報
 *   <li>{@link CallEdge} - メソッド呼び出し関係
 *   <li>{@link SqlMapping} - MyBatis SQLマッピング情報
 *   <li>{@link CrudLink} - エンドポイントとテーブル間のCRUD関係
 * </ul>
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public class Models {

  /** デフォルトコンストラクタ */
  public Models() {
    // デフォルトコンストラクタ
  }

  /**
   * RESTエンドポイント情報
   *
   * <p>Spring MVCコントローラーから抽出されたRESTエンドポイントの情報を格納します。
   * 各エンドポイントは、HTTPメソッド、URL、実装クラス・メソッド、パッケージ情報を含みます。
   *
   * <p>使用例:
   *
   * <pre>{@code
   * Endpoint endpoint = new Endpoint(
   *     "GET",
   *     "/api/users/{id}",
   *     "UserController",
   *     "getUser",
   *     "com.example.controller");
   * }</pre>
   *
   * @param httpMethod HTTPメソッド（GET、POST、PUT、DELETE等）
   * @param url URLパス（パスパラメータを含む）
   * @param controller コントローラクラス名（単純名）
   * @param method メソッド名
   * @param packageName パッケージ名（完全修飾）
   */
  public record Endpoint(
      String httpMethod, String url, String controller, String method, String packageName) {}

  /**
   * メソッド呼び出し関係の情報
   *
   * <p>Javaソースコード解析で検出されたメソッド間の呼び出し関係を表現します。 Controller → Service → Repository の依存関係を追跡するために使用されます。
   *
   * <p>使用例:
   *
   * <pre>{@code
   * CallEdge edge = new CallEdge(
   *     "UserController",
   *     "getUser",
   *     "UserService",
   *     "findById");
   * }</pre>
   *
   * @param fromClass 呼び出し元クラス名
   * @param fromMethod 呼び出し元メソッド名
   * @param toClass 呼び出し先クラス名
   * @param toMethod 呼び出し先メソッド名
   */
  public record CallEdge(String fromClass, String fromMethod, String toClass, String toMethod) {}

  /**
   * MyBatis SQLマッピング情報
   *
   * <p>MyBatis XMLファイルから抽出されたSQL文とその関連情報を格納します。 CRUD操作の種別、対象テーブル、実際のSQL文を含み、エンドポイントとの マッピングに使用されます。
   *
   * <p>使用例:
   *
   * <pre>{@code
   * SqlMapping mapping = new SqlMapping(
   *     "com.example.repository.UserRepository",
   *     "findById",
   *     "SELECT",
   *     "SELECT * FROM users WHERE id = #{id}",
   *     List.of("users"));
   * }</pre>
   *
   * @param mapperClass マッパークラス名（完全修飾名）
   * @param mapperMethod メソッド名（XMLのid属性値）
   * @param op 操作種別（SELECT、INSERT、UPDATE、DELETE）
   * @param rawSql 生SQL文（パラメータプレースホルダーを含む）
   * @param tables 対象テーブル名のリスト（複数テーブルのJOIN等に対応）
   */
  public record SqlMapping(
      String mapperClass, String mapperMethod, String op, String rawSql, List<String> tables) {}

  /**
   * エンドポイントとテーブル間のCRUD関係
   *
   * <p>RESTエンドポイントが特定のデータベーステーブルに対して実行する CRUD操作を表現します。CRUDマトリクスの基本単位となります。
   *
   * <p>CRUD操作の種別:
   *
   * <ul>
   *   <li>C - Create（INSERT操作）
   *   <li>R - Read（SELECT操作）
   *   <li>U - Update（UPDATE操作）
   *   <li>D - Delete（DELETE操作）
   * </ul>
   *
   * 使用例:
   *
   * <pre>{@code
   * CrudLink link = new CrudLink(ep, "users", "C");
   * }</pre>
   *
   * @param ep 対象のRESTエンドポイント
   * @param table データベーステーブル名
   * @param crud CRUD操作の種別（C、R、U、D）
   */
  public record CrudLink(Endpoint ep, String table, String crud) {}

  /**
   * バッチJob（@Configuration付きのJobクラス）
   *
   * @param className クラス名
   * @param jobName ジョブ名
   * @param packageName パッケージ名
   */
  public record BatchJob(String className, String jobName, String packageName) {}

  /**
   * HTTPメソッド名を大文字化（null安全）
   *
   * <p>HTTPメソッド名を統一的に大文字で正規化します。 null値に対しても安全に動作し、空文字列を返します。
   *
   * @param s HTTPメソッド名（null可）
   * @return 大文字化されたHTTPメソッド名（nullの場合は空文字列）
   */
  public static String normalizeHttp(String s) {
    return s == null ? "" : s.toUpperCase(Locale.ROOT);
  }
}
