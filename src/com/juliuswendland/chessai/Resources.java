package com.juliuswendland.chessai;

import javax.swing.*;
import java.util.Objects;

public class Resources {
    public static final ImageIcon[][] ICONS = new ImageIcon[2][6];

    public static void loadIcons() {
        ICONS[0][0] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/dark/dark_king.png")));
        ICONS[0][1] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/dark/dark_queen.png")));
        ICONS[0][2] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/dark/dark_bishop.png")));
        ICONS[0][3] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/dark/dark_knight.png")));
        ICONS[0][4] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/dark/dark_rook.png")));
        ICONS[0][5] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/dark/dark_pawn.png")));

        ICONS[1][0] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/light/light_king.png")));
        ICONS[1][1] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/light/light_queen.png")));
        ICONS[1][2] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/light/light_bishop.png")));
        ICONS[1][3] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/light/light_knight.png")));
        ICONS[1][4] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/light/light_rook.png")));
        ICONS[1][5] = new ImageIcon(Objects.requireNonNull(Resources.class.getResource("icons/light/light_pawn.png")));
    }
}
