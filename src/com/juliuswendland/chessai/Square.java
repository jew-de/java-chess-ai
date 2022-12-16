package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;

public class Square extends JPanel {
    private final int index;
    private Piece piece = null;
    private final int[] numberOfSquaresToBorder = new int[8];
    Color squareColor;
    public boolean isTargetSquare = false;

    public Square(Color squareColor, int rank, int file) {
        this.index = (rank * 8) + file;
        this.squareColor = squareColor;

        setLayout(new BorderLayout());
        setBackground(squareColor);
        setVisible(true);

        calculateNumberOfSquaresToBorder(rank, file);
    }

    private void calculateNumberOfSquaresToBorder(int rank, int file) {
        /*
         * DIRECTIONS:
         * 0 - left
         * 1 - top left
         * 2 - up
         * 3 - top right
         * 4 - right
         * 5 - bottom right
         * 6 - down
         * 7 - bottom left
         */

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
        setBackground(Color.RED);
        isTargetSquare = true;
    }

    public void reset() {
        setBackground(squareColor);
        isTargetSquare = false;
    }

    public void addPiece(Piece piece) {
        this.piece = piece;
        add(this.piece);
    }

    public void removePiece() {
        if(piece != null) {
            remove(piece);
        }
        this.piece = null;
    }

    // Getters and setters
    public int getIndex() {
        return index;
    }

    public int[] getNumberOfSquaresToBorder() {
        return numberOfSquaresToBorder;
    }

    public Piece getPiece() {
        return piece;
    }
}
