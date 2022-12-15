package com.juliuswendland.chessai;

import javax.swing.*;
import java.util.Objects;

public class Piece extends JLabel {
    public int positionIndex;
    public static ImageIcon[][] icons = new ImageIcon[2][6];

    public Piece(int positionIndex, int type, int color) {
        this.positionIndex = positionIndex;

        setVisible(true);
        loadIcons();
        setIcon(icons[color][type]);
        setHorizontalAlignment(0);
        setVerticalAlignment(0);
    }

    private void loadIcons() {
        icons[0][0] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/dark/dark_king.png")));
        icons[0][1] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/dark/dark_queen.png")));
        icons[0][2] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/dark/dark_bishop.png")));
        icons[0][3] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/dark/dark_knight.png")));
        icons[0][4] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/dark/dark_rook.png")));
        icons[0][5] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/dark/dark_pawn.png")));

        icons[1][0] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/light/light_king.png")));
        icons[1][1] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/light/light_queen.png")));
        icons[1][2] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/light/light_bishop.png")));
        icons[1][3] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/light/light_knight.png")));
        icons[1][4] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/light/light_rook.png")));
        icons[1][5] = new ImageIcon(Objects.requireNonNull(getClass().getResource("icons/light/light_pawn.png")));
    }
}
