package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;

public class Square extends JPanel {
    public Square(Color squareColor, int rank, int file) {
        setLayout(new BorderLayout());
        setBackground(squareColor);
        setVisible(true);
    }
}
