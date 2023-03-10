package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;

public class Square extends JPanel {
    private final int index, rank;
    private Piece piece = null;
    private final int[] numberOfSquaresToBorder = new int[8];
    Color squareColor;
    public boolean isTargetSquare = false;

    public Square(Color squareColor, int rank, int file) {
        this.index = (rank * 8) + file;
        this.squareColor = squareColor;
        this.rank = rank;

        setLayout(new BorderLayout());
        setBackground(squareColor);
        setVisible(true);

        calculateNumberOfSquaresToBorder(rank, file);
    }

    private void calculateNumberOfSquaresToBorder(int rank, int file) {
        numberOfSquaresToBorder[0] = file;
        numberOfSquaresToBorder[2] = rank;
        numberOfSquaresToBorder[4] = 7 - file;
        numberOfSquaresToBorder[6] = 7 - rank;

        numberOfSquaresToBorder[1] = Math.min(numberOfSquaresToBorder[0], numberOfSquaresToBorder[2]);
        numberOfSquaresToBorder[3] = Math.min(numberOfSquaresToBorder[2], numberOfSquaresToBorder[4]);
        numberOfSquaresToBorder[5] = Math.min(numberOfSquaresToBorder[4], numberOfSquaresToBorder[6]);
        numberOfSquaresToBorder[7] = Math.min(numberOfSquaresToBorder[6], numberOfSquaresToBorder[0]);
    }

    public void setTargetSquare() {
        if(squareColor == Board.LIGHT_COLOR) {
            setBackground(Board.LIGHT_COLOR_HIGHLIGHT);
        } else {
            setBackground(Board.DARK_COLOR_HIGHLIGHT);
        }
        isTargetSquare = true;
    }

    public void reset() {
        setBackground(squareColor);
        isTargetSquare = false;
    }

    public void addPiece(Piece piece) {
        this.piece = piece;
        add(this.piece);
        repaint();
    }

    public void removePiece() {
        if(this.piece != null) {
            remove(this.piece);
        }
        this.piece = null;
        repaint();
    }

    // Getters and setters
    public int getIndex() {
        return index;
    }

    public int getRank() {
        return rank;
    }

    public int[] getNumberOfSquaresToBorder() {
        return numberOfSquaresToBorder;
    }

    public Piece getPiece() {
        return piece;
    }
}
