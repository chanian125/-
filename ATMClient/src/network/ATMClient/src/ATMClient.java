package network.ATMClient.src.network.ATMClient.src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ATMClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame frame;
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JTextArea outputArea;

    public ATMClient() {
        try {
            socket = new Socket("10.234.107.37", 2525);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            initializeGUI();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeGUI() {
        frame = new JFrame("ATM Client");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel loginPanel = new JPanel(new GridLayout(5, 2));
        userIdField = new JTextField(10);
        passwordField = new JPasswordField(10);
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new LoginButtonListener());
        loginPanel.add(new JLabel("User ID:"));
        loginPanel.add(userIdField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);
        loginPanel.add(loginButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage("BYE");
                System.exit(0);
            }
        });
        loginPanel.add(exitButton);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        frame.add(loginPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void sendMessage(String message) {
        out.println(message);
        outputArea.append("Sent: " + message + "\n");
    }

    private class LoginButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String userId = userIdField.getText();
            String password = new String(passwordField.getPassword());

            sendMessage("HELO " + userId );
            sendMessage("PASS " + password);

            try {
                String response = in.readLine();
                outputArea.append("Received: " + response + "\n");

                if (response.equals("525 OK!")) {
                    frame.dispose();
                    showMainMenu();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void showMainMenu() {
        JFrame mainMenuFrame = new JFrame("Main Menu");
        mainMenuFrame.setSize(300, 200);
        mainMenuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainMenuPanel = new JPanel(new GridLayout(3, 1));
        JButton balanceButton = new JButton("Check Balance");
        balanceButton.addActionListener(new BalanceButtonListener());
        JButton depositButton = new JButton("Deposit");
        depositButton.addActionListener(new DepositButtonListener());
        JButton withdrawButton = new JButton("Withdraw");
        withdrawButton.addActionListener(new WithdrawButtonListener());
        mainMenuPanel.add(balanceButton);
        mainMenuPanel.add(depositButton);
        mainMenuPanel.add(withdrawButton);

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane resultScrollPane = new JScrollPane(resultArea);

        mainMenuFrame.add(mainMenuPanel, BorderLayout.NORTH);
        mainMenuFrame.add(resultScrollPane, BorderLayout.CENTER);
        mainMenuFrame.setVisible(true);

        outputArea = resultArea;
    }

    private class BalanceButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String userId = userIdField.getText();

            sendMessage("BALA");

            try {
                String response = in.readLine();
                outputArea.append("Received: " + response + "\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private class DepositButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String userId = userIdField.getText();
            String amount = JOptionPane.showInputDialog(frame, "Enter deposit amount:");

            sendMessage("DEPOSIT " + userId + " " + amount);

            try {
                String response = in.readLine();
                outputArea.append("Received: " + response + "\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private class WithdrawButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String userId = userIdField.getText();
            String amount = JOptionPane.showInputDialog(frame, "Enter withdrawal amount:");

            sendMessage("WDRA "+ amount);

            try {
                String response = in.readLine();
                outputArea.append("Received: " + response + "\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ATMClient());
    }
}
