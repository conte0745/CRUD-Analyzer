package dev.example.crudscan.enums;

/**
 * SQL操作の種別を表すEnum
 *
 * <p>CRUD操作の種類を型安全に表現するためのEnum定義です。 文字列の直接指定を避け、コードの保守性と可読性を向上させます。
 *
 * @author CRUD Analyzer Team
 * @version 1.0
 * @since 1.0
 */
public enum SqlOperation {
  /** SELECT操作 - データの読み取り */
  SELECT("SELECT"),

  /** INSERT操作 - データの挿入 */
  INSERT("INSERT"),

  /** UPDATE操作 - データの更新 */
  UPDATE("UPDATE"),

  /** DELETE操作 - データの削除 */
  DELETE("DELETE");

  /** SQL操作名 */
  private final String operationName;

  /**
   * SQL操作を構築
   *
   * @param operationName SQL操作名
   */
  SqlOperation(String operationName) {
    this.operationName = operationName;
  }

  /**
   * SQL操作名を取得
   *
   * @return SQL操作名
   */
  public String getOperationName() {
    return operationName;
  }

  /**
   * 文字列からSqlOperationを取得
   *
   * @param operationName 操作名文字列
   * @return 対応するSqlOperation、見つからない場合はnull
   */
  public static SqlOperation fromString(String operationName) {
    if (operationName == null) {
      return null;
    }

    String upperCase = operationName.toUpperCase();
    for (SqlOperation op : values()) {
      if (op.operationName.equals(upperCase)) {
        return op;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return operationName;
  }
}
