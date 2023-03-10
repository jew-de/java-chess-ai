package com.juliuswendland.chessai;

import javax.swing.*;

public class Piece extends JLabel {
    public int positionIndex;
    private int type;
    private final int color;
    public boolean doubleMovePossible;
    public boolean hasMovesPreviously = false;
    public static final int KING = 0, QUEEN = 1, BISHOP = 2, KNIGHT = 3, ROOK = 4, PAWN = 5;
    public static final int WHITE = 1, DARK = 0;

    public Piece(int positionIndex, int type, int color) {
        this.positionIndex = positionIndex;
        this.type = type;
        this.color = color;

        setVisible(true);
        setIcon(Resources.ICONS[color][type]);
        setHorizontalAlignment(0);
        setVerticalAlignment(0);
        repaint();
        revalidate();

        doubleMovePossible = type == 5;
    }

    public void transformInto(int type) {
        this.type = type;
        setIcon(Resources.ICONS[color][type]);
    }

    public boolean isSlidingPiece() {
        return type == Piece.QUEEN || type == Piece.BISHOP || type == Piece.ROOK;
    }

    // Getters and setters
    public int getType() {
        return type;
    }

    public int getColor() {
        return color;
    }
}
