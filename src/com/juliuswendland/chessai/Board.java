package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;

public class Board extends JLayeredPane {
    public static final Color DARK_COLOR = new Color(150, 80, 14);
    public static final Color LIGHT_COLOR = new Color(242, 165, 92);
    public Board() {
        Dimension boardSize = new Dimension(1000, 1000);
        setPreferredSize(boardSize);
        setBounds(0, 0, boardSize.width, boardSize.height);
        setLayout(new GridLayout(8, 8));

        // Build the chess board by squares
        for(int rank = 0; rank < 8; rank++) {
            for(int file = 0; file < 8; file++) {
                boolean isLightSquare = (rank + file) % 2 == 0;
                Color squareColor = isLightSquare ? LIGHT_COLOR : DARK_COLOR;
                Square square = new Square(squareColor, rank, file);
                add(square, JLayeredPane.DEFAULT_LAYER);
             }
        }
    }
}
