package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;

public class Square extends JPanel {
    private int index;

    public Square(Color squareColor, int rank, int file) {
        setLayout(new BorderLayout());
        setBackground(squareColor);
        setVisible(true);

        this.index = (rank * 8) + file;
    }

    // Getters and setters
    public int getIndex() {
        return index;
    }
}
