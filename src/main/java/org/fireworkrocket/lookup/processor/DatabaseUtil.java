package org.fireworkrocket.lookup.processor;

import io.github.palexdev.materialfx.enums.DialogType;
import org.fireworkrocket.lookup.exception.DialogUtil;
import org.fireworkrocket.lookup.exception.ExceptionHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseUtil {

   private static final String DB_URL = "jdbc:h2:./api_config_DB";
   private static final String USER = "Api";
   private static final String PASS = "Password";
   private static String tableName = "api_list"; // 默认操作表名

   static {
      try {
         // 加载 H2 驱动程序
         Class.forName("org.h2.Driver");
         System.out.println("H2 Driver loaded successfully.");

         // 创建表
         createTable(tableName);

         // 检查表是否为空，如果为空则添加默认 API
         if (getApiList().length == 0) {
            addItem("https://api.miaomc.cn/image/get?type=json");
         }
      } catch (ClassNotFoundException e) {
         ExceptionHandler.handleException(e);
      }
   }

   public static void setTableName(String newTableName) {
      tableName = newTableName;
   }

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

   public static void deleteTable(String tableName) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String deleteTableSQL = "DROP TABLE IF EXISTS " + tableName;
         stmt.executeUpdate(deleteTableSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

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

   public static void addItem(String Item) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String insertSQL = "INSERT INTO " + tableName + " (api_url) VALUES ('" + Item + "')";
         stmt.executeUpdate(insertSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   public static void updateItem(int id, String newApiUrl) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String updateSQL = "UPDATE " + tableName + " SET api_url = '" + newApiUrl + "' WHERE id = " + id;
         stmt.executeUpdate(updateSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   public static void deleteItem(int id) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String deleteSQL = "DELETE FROM " + tableName + " WHERE id = " + id;
         stmt.executeUpdate(deleteSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

   public static void deleteItem(String apiUrl) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String deleteSQL = "DELETE FROM " + tableName + " WHERE api_url = '" + apiUrl + "'";
         stmt.executeUpdate(deleteSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

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

   public static void changeUsername(String newUsername) {
      try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
           Statement stmt = conn.createStatement()) {
         String updateUsernameSQL = "ALTER USER " + USER + " RENAME TO " + newUsername;
         stmt.executeUpdate(updateUsernameSQL);
      } catch (Exception e) {
         ExceptionHandler.handleException(e);
      }
   }

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