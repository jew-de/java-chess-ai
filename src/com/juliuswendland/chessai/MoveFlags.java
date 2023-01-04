package com.juliuswendland.chessai;

public class MoveFlags {
    public static final int NONE = 0;
    public static final int DOUBLE_PAWN_PUSH = 1;
    public static final int EN_PASSANT = 2;
    public static final int CASTLE_KING_SIDE = 3;
    public static final int CASTLE_QUEEN_SIDE = 4;
    public static final int PROMOTE_PLAYER = 5;

    // Flags specifically for AI moves
    public static final int PROMOTE_QUEEN = 6;
    public static final int PROMOTE_ROOK = 7;
    public static final int PROMOTE_BISHOP = 8;
    public static final int PROMOTE_KNIGHT = 9;
}
