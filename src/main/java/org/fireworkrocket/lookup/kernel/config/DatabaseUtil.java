package org.fireworkrocket.lookup.kernel.config;

import io.github.palexdev.materialfx.enums.DialogType;
import org.fireworkrocket.lookup.ui.exception.DialogUtil;
import org.fireworkrocket.lookup.kernel.exception.ExceptionHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库工具类，用于管理 API 配置数据库。
 */
public class DatabaseUtil {

   private static final String DB_URL = "jdbc:h2:"+ DefaultConfig.configHome+"/api_config_DB";
   private static final String USER = "Api"; // 默认用户
   private static final String PASS = "Password"; // 默认密码
   private static String tableName = "api_list"; // 默认操作表名

   static {
      try {
         // 加载 H2 驱动程序
         Class.forName("org.h2.Driver");
         System.out.println("H2 Driver loaded successfully.");

         // 创建表
         createTable(tableName);
         createFlexibleApiTable(); // 创建灵活 API 表

         // 检查表是否为空，如果为空则添加默认 API
         if (getApiList().length == 0) {
            addItem("https://api.miaomc.cn/image/get?type=json");
         }
      } catch (ClassNotFoundException e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 创建表
    * @param tableName 表名
    */
   public static void createTable(String tableName) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                 "id INT AUTO_INCREMENT PRIMARY KEY, " +
                 "api_url VARCHAR(255) NOT NULL)";
         stmt.executeUpdate(createTableSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 创建灵活 API 表
    */
   public static void createFlexibleApiTable() {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String createTableSQL = "CREATE TABLE IF NOT EXISTS flexible_api_list (" +
                 "id INT AUTO_INCREMENT PRIMARY KEY, " +
                 "api_url VARCHAR(255) NOT NULL)";
         stmt.executeUpdate(createTableSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 获取 API 列表
    * @return API 列表
    */
   public static String[] getApiList() {
      List<String> apiList = new ArrayList<>();
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT api_url FROM " + tableName)) {

         while (rs.next()) {
            apiList.add(rs.getString("api_url"));
         }
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
      return apiList.toArray(new String[0]);
   }

   /**
    * 获取灵活 API 列表
    * @return 灵活 API 列表
    */
   public static String[] getFlexibleApiList() {
      List<String> apiList = new ArrayList<>();
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT api_url FROM flexible_api_list")) {

         while (rs.next()) {
            apiList.add(rs.getString("api_url"));
         }
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
      return apiList.toArray(new String[0]);
   }

   /**
    * 添加新的 API
    * @param Item API URL
    */
   public static void addItem(String Item) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String insertSQL = "INSERT INTO " + tableName + " (api_url) VALUES ('" + Item + "')";
         stmt.executeUpdate(insertSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 添加灵活 API 项
    * @param apiUrl API URL
    */
   public static void addFlexibleApiItem(String apiUrl) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String insertSQL = "INSERT INTO flexible_api_list (api_url) VALUES ('" + apiUrl + "')";
         stmt.executeUpdate(insertSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 更新 API
    * @param id API ID
    * @param newApiUrl 新的 API URL
    */
   public static void updateItem(int id, String newApiUrl) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String updateSQL = "UPDATE " + tableName + " SET api_url = '" + newApiUrl + "' WHERE id = " + id;
         stmt.executeUpdate(updateSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 更新灵活 API 项
    * @param apiUrl API URL
    * @param supportsFlexible 是否支持灵活 API
    */
   public static void updateFlexibleApiItem(String apiUrl, boolean supportsFlexible) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String updateSQL = supportsFlexible ?
                 "INSERT INTO flexible_api_list (api_url) VALUES ('" + apiUrl + "')" :
                 "DELETE FROM flexible_api_list WHERE api_url = '" + apiUrl + "'";
         stmt.executeUpdate(updateSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 替换 API
    * @param oldItem 旧的 API URL
    * @param newItem 新的 API URL
    */
   public static void replaceItem(String oldItem, String newItem) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String updateSQL = "UPDATE " + tableName + " SET api_url = '" + newItem + "' WHERE api_url = '" + oldItem + "'";
         stmt.executeUpdate(updateSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 删除 API
    * @param id API ID
    */
   public static void deleteItem(int id) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String deleteSQL = "DELETE FROM " + tableName + " WHERE id = " + id;
         stmt.executeUpdate(deleteSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 删除 API
    * @param apiUrl API URL
    */
   public static void deleteItem(String apiUrl) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String deleteSQL = "DELETE FROM " + tableName + " WHERE api_url = '" + apiUrl + "'";
         stmt.executeUpdate(deleteSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 搜索 API
    * @param keyword 关键词
    * @return 匹配的 API 列表
    */
   public static List<String> searchItem(String keyword) {
      List<String> result = new ArrayList<>();
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT api_url FROM " + tableName + " WHERE api_url LIKE '%" + keyword + "%'")) {

         while (rs.next()) {
            result.add(rs.getString("api_url"));
         }
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
      return result;
   }

   /**
    * 设置操作表名
    * @param newTableName 新的表名
    */
   public static void setTableName(String newTableName) {
      tableName = newTableName;
   }

   /**
    * 删除表
    * @param tableName 表名
    */
   public static void deleteTable(String tableName) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String deleteTableSQL = "DROP TABLE IF EXISTS " + tableName;
         stmt.executeUpdate(deleteTableSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 更改用户名
    * @param newUsername 新用户名
    */
   public static void changeUsername(String newUsername) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String updateUsernameSQL = "ALTER USER " + USER + " RENAME TO " + newUsername;
         stmt.executeUpdate(updateUsernameSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 更新密码
    * @param newPassword 新密码
    */
   public static void updatePassword(String newPassword) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String updatePasswordSQL = "ALTER USER " + USER + " SET PASSWORD '" + newPassword + "'";
         stmt.executeUpdate(updatePasswordSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   static boolean AlwaysAllow = false;

   /**
    * 创建临时用户
    * @param username 用户名
    * @param password 密码
    * @param readOnly 是否只读
    * @deprecated 所有临时用户都将在重启后删除！！！
    */
   public static void createTemporaryUser(String username, String password, boolean readOnly) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String createUserSQL = "CREATE USER " + username + " PASSWORD '" + password + "'";
         stmt.executeUpdate(createUserSQL);

         if (readOnly) {
            String grantReadOnlySQL = "GRANT SELECT ON SCHEMA PUBLIC TO " + username;
            stmt.executeUpdate(grantReadOnlySQL);
         } else {
            Map<String, Runnable> buttons = new HashMap<>();
            buttons.put("拒绝", null);
            buttons.put("允许", () -> {
               try (Connection innerConn = DriverManager.getConnection(DB_URL, USER, PASS);
                    Statement innerStmt = innerConn.createStatement()) {
                  String grantReadWriteSQL = "GRANT ALL ON SCHEMA PUBLIC TO " + username;
                  innerStmt.executeUpdate(grantReadWriteSQL);
               } catch (Exception e) {
                  ExceptionHandler.handleException(e);
               }
            });
            buttons.put("总是允许（不推荐）", () -> {
               if (!AlwaysAllow && (AlwaysAllow = true)) {
                  try (Connection innerConn = DriverManager.getConnection(DB_URL, USER, PASS);
                       Statement innerStmt = innerConn.createStatement()) {
                     String grantReadWriteSQL = "GRANT ALL ON SCHEMA PUBLIC TO " + username;
                     innerStmt.executeUpdate(grantReadWriteSQL);
                  } catch (Exception e) {
                     ExceptionHandler.handleException(e);
                  }
               }
            });
            DialogUtil.showDialog(DialogType.INFO, "DB授权请求", "是否授权用户 " + username + " 读写权限？", buttons);
         }
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   /**
    * 删除所有临时用户
    */
   public static void deleteAllTemporaryUsers() {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String deleteUsersSQL = "DROP USER IF EXISTS temp_user";
         stmt.executeUpdate(deleteUsersSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }
}