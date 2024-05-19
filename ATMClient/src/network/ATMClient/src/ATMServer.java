package network.ATMClient.src.network.ATMClient.src;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class ATMServer {

    private static final String LOG_FILE = "server.log";
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/atm_database";
    static final String USER = "root";
    static final String PASS = "0125";
    static String username;
    static int flag=0;


    public static void main(String[] args) {
        // 初始化用户账户

        Connection conn = null;
        Statement stmt = null;
        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);

            // 打开链接
            System.out.println("连接数据库...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            // loadUserAccountsFromDatabase();
            // 执行查询
            //System.out.println("实例化Statement对象...");
            //log("Creating statement...");
            stmt = conn.createStatement();
            String sql;
            sql = "SELECT username FROM users";
            ResultSet rs = stmt.executeQuery(sql);

            // 展开结果集数据库
            // 完成后关闭
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
            log("SQLException occurred: " + se.getMessage());
        } catch (Exception e) {
            // 处理 Class.forName 错误
            e.printStackTrace();
            log("Exception occurred: " + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se2) {
                se2.printStackTrace();
                log("SQLException occurred while closing statement: " + se2.getMessage());
            } // 什么都不做
            try {

                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
                log("SQLException occurred while closing connection: " + se.getMessage());
            }
        }
        // 启动服务器，接受客户端请求
        try (ServerSocket serverSocket = new ServerSocket(2525)) {
            log("连接成功");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("Client connected: " + clientSocket.getInetAddress());
                new Thread(() -> processClientRequests(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            log("IOException occurred: " + e.getMessage());
        }
    }
    
    private static void processClientRequests(Socket clientSocket) {
        try (
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String request;
            while ((request = input.readLine()) != null) {
                log("Request received from client: " + request);
                if (request.startsWith("HELO")) {
                    handleHELO(request, output);
                } else if (request.startsWith("PASS")) {
                    handlePASS(request, output);
                } else if (request.startsWith("WDRA") && flag==1) {
                    handleWithdraw(request, output);
                } else if (request.equals("BALA")&& flag==1) {
                    BALA(username, output);
                } else if (request.equals("BYE")) {
                    output.println("BYE");
                    break;
                } else {
                    output.println("401 ERROR!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            log("IOException occurred: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                log("IOException occurred while closing client socket: " + e.getMessage());
            }
        }
    }

    private static void handleHELO(String request, PrintWriter output) {
        username = request.substring(5);
        if (verifyLogin(username)) {
            output.println("500 AUTH REQUIRE");
            log("HELO request processed successfully.");
        } else {
            output.println("401 ERROR!");
            log("Error processing HELO request.");
        }
    }

    private static void handlePASS(String request, PrintWriter output) {
        String password = request.substring(5);
        if (verifyPassword(username, password)) {
            output.println("525 OK!");
            flag=1;
            log("PASS request processed successfully.");
        } else {
            output.println("401 ERROR!");
            log("Error processing PASS request.");
        }
    }

    private static void handleWithdraw(String request, PrintWriter output) {
        String parts = request.substring(5);
        double amount = Double.parseDouble(parts);
        if (withdraw(username, amount)) {
            try (Connection con = getConnection();
                 PreparedStatement statement = con
                         .prepareStatement("INSERT INTO record(username,amountChange,time) VALUES(?,?,?)")) {
                statement.setString(1, username);
                statement.setString(2, "-" + parts);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime dateTime = LocalDateTime.now();
                String formattedDateTime = dateTime.format(formatter);
                statement.setString(3, formattedDateTime);
                statement.executeUpdate();
                output.println("525 OK!");
                log("WDRA request processed successfully.");
            } catch (SQLException ex) {
                ex.printStackTrace();
                log("SQLException occurred while processing WDRA request: " + ex.getMessage());
            }
        } else {
            output.println("401 ERROR!");
            log("Error processing WDRA request. Insufficient funds.");
        }
    }

    private static void BALA(String username, PrintWriter output) {
        double balance = 0.0; // 初始化余额为0.0
        try (Connection con = getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT balance FROM users WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    balance = rs.getDouble("balance"); // 将查询到的余额赋值给balance变量
                    output.println("AMNT:" + balance);
                    log("BALA request processed successfully.");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("SQLException occurred while processing BALA request: " + ex.getMessage());
        }
    }

    private static boolean verifyLogin(String username) {
        try (Connection con = getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT * FROM users WHERE username = ? ")) {
            statement.setString(1, username);
            System.out.println(username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next(); // 返回true表示用户名和密码匹配
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("SQLException occurred while processing HELO request: " + ex.getMessage());
            return false;
        }
    }

    private static boolean verifyPassword(String username, String password) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection
                     .prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
            statement.setString(1, username);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    log("Password verified for username: " + username);
                    return true; // 返回true表示用户名和密码匹配
                } else {
                    log("Password verification failed for username: " + username);
                    return false;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            log("SQLException occurred while processing PASS request: " + ex.getMessage());
            return false;
        }
    }

    private static boolean withdraw(String username, double amount) {
        try (Connection con = getConnection();
             PreparedStatement statement = con.prepareStatement("SELECT balance FROM users WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    double balance = rs.getDouble("balance");
                    if (balance >= amount) {
                        // 更新余额
                        String updateQuery = "UPDATE users SET balance=? WHERE username=?";
                        try (PreparedStatement updateStmt = con.prepareStatement(updateQuery)) {
                            updateStmt.setDouble(1, balance - amount);
                            updateStmt.setString(2, username);
                            updateStmt.executeUpdate();
                            log("BALA request processed successfully for username: " + username + ", amount: " + amount);
                            return true; // 取款成功
                        }
                    } else {
                        log("Insufficient funds for username: " + username + ", amount requested: " + amount);
                        return false; // 余额不足
                    }
                } else {
                    log("Account not found for username: " + username);
                    return false; // 账户不存在
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            log("SQLException occurred while processing WDRA request: " + ex.getMessage());
            return false;
        }
    }

    private static void log(String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logMessage = timestamp + " : " + message;
        System.out.println(logMessage); // 在控制台打印日志信息

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)))) {
            writer.println(logMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }
}