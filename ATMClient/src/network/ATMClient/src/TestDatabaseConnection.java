package network.ATMClient.src.network.ATMClient.src;

import java.sql.*;

public class TestDatabaseConnection {
    public static void main(String[] args) {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            // 加载MySQL驱动程序
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 连接数据库
            String url = "jdbc:mysql://localhost:3306/atm_database";
            String user = "root"; // 将username改为user
            String password = "0125";
            connection = DriverManager.getConnection(url, user, password);

            // 创建Statement对象
            statement = connection.createStatement();

            // 执行查询
            String sql = "SELECT * FROM users";
            resultSet = statement.executeQuery(sql);

            // 处理查询结果
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                double balance = resultSet.getDouble("balance");
                System.out.println("User ID: " + id + ", Username: " + username + ", Balance: $" + balance);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                // 关闭ResultSet
                if (resultSet != null) resultSet.close();
                // 关闭Statement
                if (statement != null) statement.close();
                // 关闭Connection
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
