package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;

public class Square extends JPanel {
    private final int index;
    private Piece piece = null;

    public Square(Color squareColor, int rank, int file) {
        this.index = (rank * 8) + file;

        setLayout(new BorderLayout());
        setBackground(squareColor);
        setVisible(true);
    }

    public void addPiece(Piece piece) {
        this.piece = piece;
        add(this.piece);
        revalidate();
    }

    public void removePiece() {
        remove(this.piece);
        this.piece = null;
        revalidate();
    }

    // Getters and setters
    public int getIndex() {
        return index;
    }
}
