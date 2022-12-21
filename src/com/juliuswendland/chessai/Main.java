package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        EventQueue.invokeLater(Main::createAndShowUI);
    }

    public static void createAndShowUI() {
        JFrame mainFrame = new JFrame();
        mainFrame.setTitle("Chess AI made by Julius Wendland");
        mainFrame.setVisible(true);
        mainFrame.requestFocus();
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.add(new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"));
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
    }
}
