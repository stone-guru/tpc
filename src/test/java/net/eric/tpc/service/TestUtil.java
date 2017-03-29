package net.eric.tpc.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class TestUtil {
    private TestUtil() {
    }

    public static void clearTable(String tableName, String driver, String url, String user, String password) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName(driver);

            conn = DriverManager.getConnection(url, user, password);
            stmt = conn.createStatement();
            String sql = "DELETE FROM " + tableName;
            stmt.executeUpdate(sql);
            System.out.println("All record in " + tableName + " deleted");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}
