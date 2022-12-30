package com.juliuswendland.chessai;

import java.util.Arrays;
import java.util.LinkedList;

public class MoveGenerator {
    Board board;
    LinkedList<Piece> whitePieces = new LinkedList<>();
    LinkedList<Piece> darkPieces = new LinkedList<>();

    public MoveGenerator(Board board) {
        this.board = board;
    }

    public LinkedList<Move> generateLegalMoves() {
        splitPieces();

        LinkedList<Move> legalMoves = new LinkedList<>();
        LinkedList<Piece> friendlyPieces = board.colorAtMove == Piece.WHITE ? whitePieces : darkPieces;
        LinkedList<Piece> enemyPieces = board.colorAtMove == Piece.WHITE ? darkPieces : whitePieces;

        Piece friendlyKing = null;
        for(Piece piece : friendlyPieces) {
            if(piece.getType() == Piece.KING) {
                friendlyKing = piece;
            }
        }

        // Something went wrong, no king exists
        if(friendlyKing == null) {
            throw new Error("Something went wrong. Pleases restart the game and report the error");
        }

        // First generate all possible king moves
        // -> Calculate all squares attacked by the opponent by removing and later adding the friendly king to the board
        Square friendlyKingSquare = (Square) board.getComponent(friendlyKing.positionIndex);
        friendlyKingSquare.removePiece();
        LinkedList<Square> kingDangerSquares = generateAttackedSquares();
        friendlyKingSquare.addPiece(friendlyKing);
        // -> King cannot move to squares attacked by the opponent
        for(Square square : generateKingSquares(friendlyKing, false)) {
            if(kingDangerSquares.contains(square)) continue;
            legalMoves.add(new Move(friendlyKingSquare, square, MoveFlags.NONE));
        }

        // Calculate whether friendly king is in check and store every piece giving check
        LinkedList<Piece> piecesGivingCheck = calculatePiecesGivingCheck(friendlyKing);

        // If more than one piece checks the king the only piece able to move is the king
        if(piecesGivingCheck.size() > 1) return legalMoves;

        LinkedList<Piece> captureMask = new LinkedList<>();
        LinkedList<Square> pushMask = new LinkedList<>();

        // King is in check by one piece
        if(piecesGivingCheck.size() == 1) {
            Piece pieceGivingCheck = piecesGivingCheck.get(0);
            // Piece giving check can be captured
            captureMask.add(pieceGivingCheck);
            // If it is a sliding piece the check can be blocked
            if(pieceGivingCheck.isSlidingPiece()) {
                pushMask.addAll(board.getSquaresBetweenTwoPieces(pieceGivingCheck, friendlyKing));
            }
            // If it is not a sliding piece we can only capture, not block
        }
        // King is not in check, every square is OK to move to
        else {
            captureMask.addAll(enemyPieces);
            for(int i = 0; i < 64; i++) {
                pushMask.add((Square) board.getComponent(i));
            }
        }

        // Calculate moves for pinned pieces
        legalMoves.addAll(generateMovesForPinnedPieces(friendlyKing, friendlyPieces));

        // Remove the king from the rest of move generation
        friendlyPieces.remove(friendlyKing);

        // Calculate the moves for the remaining pieces
        for(Piece piece : friendlyPieces) {
            LinkedList<Square> squares;

            switch (piece.getType()) {
                default -> squares = generateSlidingSquares(piece, false, piece.getType());
                case Piece.KNIGHT -> squares = generateKnightSquares(piece, false);
                case Piece.PAWN -> {
                    squares = generatePawnSquares(piece, false);
                    Move move = generateDoublePawnPush(piece, pushMask);
                    if(move == null) break;
                    legalMoves.add(move);
                }
            }

            for(Square square : squares) {
                Square startSquare = (Square) board.getComponent(piece.positionIndex);
                if(pushMask.contains(square) || captureMask.contains(square.getPiece())) {
                    legalMoves.add(new Move(startSquare, square, MoveFlags.NONE));
                }
            }
        }

        if(piecesGivingCheck.size() == 0) {
            legalMoves.addAll(generateCastleMoves(friendlyKing, kingDangerSquares));
        }

        legalMoves.addAll(board.enPassantMoves);
        board.enPassantMoves.clear();

        return legalMoves;
    }

    private LinkedList<Piece> calculatePiecesGivingCheck(Piece friendlyKing) {
        LinkedList<Piece> piecesGivingCheck = new LinkedList<>();

        // Calculate moves from king square for every kind of piece
        // If this lands on an enemy piece, this piece is giving check
        for(Square square : generateSlidingSquares(friendlyKing, true, Piece.BISHOP)) {
            if(square.getPiece() == null) continue;
            if(square.getPiece().getColor() == board.colorAtMove) continue;
            if(!(square.getPiece().getType() == Piece.BISHOP || square.getPiece().getType() == Piece.QUEEN)) continue;
            piecesGivingCheck.add(square.getPiece());
        }
        for(Square square : generateSlidingSquares(friendlyKing, true, Piece.ROOK)) {
            if(square.getPiece() == null) continue;
            if(square.getPiece().getColor() == board.colorAtMove) continue;
            if(!(square.getPiece().getType() == Piece.ROOK || square.getPiece().getType() == Piece.QUEEN)) continue;
            piecesGivingCheck.add(square.getPiece());
        }
        for(Square square : generatePawnSquares(friendlyKing, true)) {
            if(square.getPiece() == null) continue;
            if(square.getPiece().getColor() == board.colorAtMove) continue;
            if(!(square.getPiece().getType() == Piece.PAWN)) continue;
            piecesGivingCheck.add(square.getPiece());
        }
        for(Square square : generateKnightSquares(friendlyKing, true)) {
            if(square.getPiece() == null) continue;
            if(square.getPiece().getColor() == board.colorAtMove) continue;
            if(!(square.getPiece().getType() == Piece.KNIGHT)) continue;
            piecesGivingCheck.add(square.getPiece());
        }
        // A king cannot possibly check the enemy king

        return piecesGivingCheck;
    }

    private LinkedList<Move> generateMovesForPinnedPieces(Piece friendlyKing, LinkedList<Piece> friendlyPieces) {
        LinkedList<Move> moves = new LinkedList<>();

        Square friendlyKingSquare = (Square) board.getComponent(friendlyKing.positionIndex);
        int[] numberSquaresToBorder = friendlyKingSquare.getNumberOfSquaresToBorder();
        int index;
        Piece pinnedPiece; // A piece that may be pinned by another piece

        // Generate moves for vertically / horizontally pinned pieces
        for(int directionIndex = Directions.LEFT; directionIndex <= Directions.BOTTOM; directionIndex += 2) {
            // Go in every direction from friendly king until border is reached
            pinnedPiece = null;
            index = friendlyKing.positionIndex;

            for(int i = 0; i < numberSquaresToBorder[directionIndex]; i++) {
                index += Board.OFFSETS[directionIndex];
                Square square = (Square) board.getComponent(index);
                Piece piece = square.getPiece();

                if(piece == null) continue;
                if(piece.getColor() == friendlyKing.getColor()) {
                    // Two friendly pieces in this direction, piece is not pinned
                    if(pinnedPiece != null) break;
                    // This piece may be pinned
                    pinnedPiece = piece;
                    continue;
                }

                if(pinnedPiece == null) break;
                if(!piece.isSlidingPiece()) break;

                // Piece can only be pinned by queens or Rooks
                if(piece.getType() != Piece.QUEEN && piece.getType() != Piece.ROOK) break;
                Square pinnedPieceSquare = (Square) board.getComponent(pinnedPiece.positionIndex);
                Square squareOfPinningPiece = (Square) board.getComponent(piece.positionIndex);

                if(pinnedPiece.getType() == Piece.QUEEN || pinnedPiece.getType() == Piece.ROOK) {
                    // Piece can only move along the diagonal line
                    for(Square moveSquare : board.getSquaresBetweenTwoPieces(piece, friendlyKing)) {
                        if(pinnedPieceSquare == moveSquare) continue;
                        moves.add(new Move(pinnedPieceSquare, moveSquare, MoveFlags.NONE));
                    }
                    // Piece can also capture the pinning piece
                    moves.add(new Move(pinnedPieceSquare, squareOfPinningPiece, MoveFlags.NONE));
                }

                else if(pinnedPiece.getType() == Piece.PAWN) {
                    // Pawn can only capture the pinning piece if next to it
                    int allowedDirection = pinnedPiece.getColor() == Piece.WHITE ? Directions.TOP : Directions.BOTTOM;

                    if(directionIndex == allowedDirection) {
                        int indexOfSquareAttackedByPawn = pinnedPiece.positionIndex + Board.OFFSETS[directionIndex];
                        Square squareAttackedByPawn = (Square) board.getComponent(indexOfSquareAttackedByPawn);

                        if(squareAttackedByPawn == pinnedPieceSquare) {
                            moves.add(new Move(pinnedPieceSquare, squareOfPinningPiece, MoveFlags.NONE));
                        }
                    }
                }

                // Remove the pinned piece from the rest of move generation
                // Filter is necessary to remove the specific piece, not just one random piece
                Piece finalPinnedPiece = pinnedPiece;
                friendlyPieces.removeIf(e -> e.positionIndex == finalPinnedPiece.positionIndex);
            }
        }

        // Generate moves for diagonally pinned pieces
        for(int directionIndex = Directions.TOP_LEFT; directionIndex <= Directions.BOTTOM_LEFT; directionIndex += 2) {
            // Go in every direction from friendly king until border is reached
            pinnedPiece = null;
            index = friendlyKing.positionIndex;

            for(int i = 0; i < numberSquaresToBorder[directionIndex]; i++) {
                index += Board.OFFSETS[directionIndex];
                Square square = (Square) board.getComponent(index);
                Piece piece = square.getPiece();

                if(piece == null) continue;
                if(piece.getColor() == friendlyKing.getColor()) {
                    // Two friendly pieces in this direction, piece is not pinned
                    if(pinnedPiece != null) break;
                    // This piece may be pinned
                    pinnedPiece = piece;
                    continue;
                }

                if(pinnedPiece == null) break;
                if(!piece.isSlidingPiece()) break;

                // Piece can only be pinned by queens or bishops
                if(piece.getType() != Piece.QUEEN && piece.getType() != Piece.BISHOP) break;
                Square pinnedPieceSquare = (Square) board.getComponent(pinnedPiece.positionIndex);
                Square squareOfPinningPiece = (Square) board.getComponent(piece.positionIndex);

                if(pinnedPiece.getType() == Piece.QUEEN || pinnedPiece.getType() == Piece.BISHOP) {
                    // Piece can only move along the diagonal line
                    for(Square moveSquare : board.getSquaresBetweenTwoPieces(piece, friendlyKing)) {
                        if(pinnedPieceSquare == moveSquare) continue;
                        moves.add(new Move(pinnedPieceSquare, moveSquare, MoveFlags.NONE));
                    }
                    // Piece can also capture the pinning piece
                    moves.add(new Move(pinnedPieceSquare, squareOfPinningPiece, MoveFlags.NONE));
                }

                else if(pinnedPiece.getType() == Piece.PAWN) {
                    // Pawn can only capture the pinning piece if next to it
                    int[] allowedDirections = pinnedPiece.getColor() == Piece.WHITE ? new int[]{1, 3} : new int[]{5, 7};

                    int finalDirectionIndex = directionIndex;

                    if(Arrays.stream(allowedDirections).anyMatch(e -> e == finalDirectionIndex)) {
                        int indexOfSquareAttackedByPawn = pinnedPiece.positionIndex + Board.OFFSETS[directionIndex];
                        Square squareAttackedByPawn = (Square) board.getComponent(indexOfSquareAttackedByPawn);

                        if(squareAttackedByPawn == pinnedPieceSquare) {
                            moves.add(new Move(pinnedPieceSquare, squareOfPinningPiece, MoveFlags.NONE));
                        }
                    }
                }

                // Remove the pinned piece from the rest of move generation
                // Filter is necessary to remove the specific piece, not just one random piece
                Piece finalPinnedPiece = pinnedPiece;
                friendlyPieces.removeIf(e -> e.positionIndex == finalPinnedPiece.positionIndex);
            }
        }

        return moves;
    }

    private LinkedList<Move> generateEnPassantMoves(Square squareOfCapturedPiece) {
        // This is called when a double pawn push move has been made
        LinkedList<Move> moves = new LinkedList<>();
        LinkedList<Move> legalMoves = new LinkedList<>();

        Piece pieceCaptured = squareOfCapturedPiece.getPiece();
        // Pawn must be captured by an enemy pawn, thus the capture needs to be in the opposite direction
        int directionToCaptureIn = pieceCaptured.getColor() == Piece.WHITE ? Directions.BOTTOM : Directions.TOP;

        for(int directionIndex = Directions.LEFT; directionIndex <= Directions.RIGHT; directionIndex += 4) {
            int indexOfSquareToLookAt = squareOfCapturedPiece.getIndex() + Board.OFFSETS[directionIndex];
            Square squareToLookAt = (Square) board.getComponent(indexOfSquareToLookAt);

            if(squareToLookAt.getPiece() == null) continue;
            if(squareToLookAt.getPiece().getType() != Piece.PAWN) continue;
            if(squareToLookAt.getPiece().getColor() == pieceCaptured.getColor()) continue;

            int indexOfSquareToCaptureAt = squareOfCapturedPiece.getIndex() + Board.OFFSETS[directionToCaptureIn];
            Square squareToCaptureAt = (Square) board.getComponent(indexOfSquareToCaptureAt);
            moves.add(new Move(squareToLookAt, squareToCaptureAt, MoveFlags.EN_PASSANT));
        }

        // Check if en passant moves are valid
        for(Move move : moves) {
            // remove both pawns from the board
            squareOfCapturedPiece.removePiece();
            Piece movingPiece = move.startSquare().getPiece();
            move.startSquare().removePiece();

            int[] numberSquaresToBorder = move.startSquare().getNumberOfSquaresToBorder();
            Piece king = null;
            Piece attacker = null;

            for(int directionIndex = Directions.LEFT; directionIndex <= Directions.RIGHT; directionIndex += 4) {
                int index = move.startSquare().getIndex();
                for(int i = 0; i < numberSquaresToBorder[directionIndex]; i++) {
                    index += Board.OFFSETS[directionIndex];
                    Square square = (Square) board.getComponent(index);
                    Piece piece = square.getPiece();

                    if(square.getPiece() == null) continue;

                    if(piece.getType() == Piece.KING && piece.getColor() == movingPiece.getColor()) {
                        king = piece;
                    }

                    if(piece.getColor() == movingPiece.getColor()) break;
                    if(piece.getType() == Piece.QUEEN || piece.getType() == Piece.ROOK) break;
                    attacker = piece;
                }
            }

            // add pawns back to the board
            move.startSquare().addPiece(movingPiece);
            squareOfCapturedPiece.addPiece(pieceCaptured);

            if(king != null || attacker != null) continue;
            legalMoves.add(move);
        }

        return legalMoves;
    }

    private LinkedList<Move> generateCastleMoves(Piece king, LinkedList<Square> attackedSquares) {
        LinkedList<Move> moves = new LinkedList<>();

        if(king.hasMovesPreviously) return moves;

        Square kingSquare = (Square) board.getComponent(king.positionIndex);
        int[] numberSquaresToBorder = kingSquare.getNumberOfSquaresToBorder();

        for(int directionIndex = Directions.LEFT; directionIndex <= Directions.RIGHT; directionIndex += 4) {
            int index = kingSquare.getIndex();
            for(int i = 0; i < numberSquaresToBorder[directionIndex]; i++) {
                int castleDirectionIndex = directionIndex == Directions.LEFT ? 0 : 1;
                if(!board.possibleCastles[king.getColor()][castleDirectionIndex]) break;

                index += Board.OFFSETS[directionIndex];
                Square square = (Square) board.getComponent(index);

                // King cannot move through or end up in check
                if(attackedSquares.contains(square) && i <= 1) break;

                if(square.getPiece() == null) continue;

                // There cannot be any pieces between king and rook
                if(square.getPiece().getType() != Piece.ROOK) break;
                if(square.getPiece().getColor() != king.getColor()) break;
                // Rook must not have moved previously
                if(square.getPiece().hasMovesPreviously) break;

                int moveFlag = directionIndex == Directions.LEFT ? MoveFlags.CASTLE_QUEEN_SIDE : MoveFlags.CASTLE_KING_SIDE;
                int squareToMoveToIndex = kingSquare.getIndex() + (Board.OFFSETS[directionIndex] * 2);
                Square squareToMoveTo =  (Square) board.getComponent(squareToMoveToIndex);
                moves.add(new Move(kingSquare, squareToMoveTo, moveFlag));
                break;
            }
        }

        return moves;
    }

    private Move generateDoublePawnPush(Piece piece, LinkedList<Square> pushMask) {
        // Separate double pawn push method is necessary to add the move flag
        if(!piece.doubleMovePossible) return null;

        int directionIndex = piece.getColor() == Piece.WHITE ? Directions.TOP : Directions.BOTTOM;
        int index = piece.positionIndex + (Board.OFFSETS[directionIndex] * 2);
        Square startSquare = (Square) board.getComponent(piece.positionIndex);
        Square square = (Square) board.getComponent(index);

        if(square.getPiece() != null) return null;

        if(pushMask.contains(square)) {
            return new Move(startSquare, square, MoveFlags.DOUBLE_PAWN_PUSH);
        }
        return null;
    }

    private LinkedList<Square> generateAttackedSquares() {
        LinkedList<Piece> pieces = board.colorAtMove == Piece.WHITE ? darkPieces : whitePieces;
        LinkedList<Square> attackedSquares = new LinkedList<>();

        for(Piece piece : pieces) {
            switch (piece.getType()) {
                default -> attackedSquares.addAll(generateSlidingSquares(piece, true, piece.getType()));
                case Piece.KING -> attackedSquares.addAll(generateKingSquares(piece, true));
                case Piece.KNIGHT -> attackedSquares.addAll(generateKnightSquares(piece, true));
                case Piece.PAWN -> attackedSquares.addAll(generatePawnSquares(piece, true));
            }
        }
        return attackedSquares;
    }

    private LinkedList<Square> generateSlidingSquares(Piece piece, boolean kingDangerSquares, int typeOfPiece) {
        LinkedList<Square> squares = new LinkedList<>();

        Square squareOfPiece = (Square) board.getComponent(piece.positionIndex);
        int[] numberSquaresToBorder = squareOfPiece.getNumberOfSquaresToBorder();

        int startDirectionIndex;
        int directionIndexIncrement;
        switch (typeOfPiece) {
            // Queen can move all directions
            default -> {
                startDirectionIndex = Directions.LEFT;
                directionIndexIncrement = 1;
            }
            // Bishop can only move diagonally
            case Piece.BISHOP -> {
                startDirectionIndex = Directions.TOP_LEFT;
                directionIndexIncrement = 2;
            }
            // Rook can only move straight
            case Piece.ROOK -> {
                startDirectionIndex = Directions.LEFT;
                directionIndexIncrement = 2;
            }
        }

        for(int directionIndex = startDirectionIndex; directionIndex < 8; directionIndex += directionIndexIncrement) {
            int index = squareOfPiece.getIndex();
            for(int i = 0; i < numberSquaresToBorder[directionIndex]; i++) {
                index += Board.OFFSETS[directionIndex];
                Square square = (Square) board.getComponent(index);

                // Square is empty, add to attacked squares
                if(square.getPiece() == null) {
                    squares.add(square);
                }
                // Square is blocked by friendly piece
                else if(square.getPiece().getColor() == piece.getColor()) {
                    // If calculating king danger squares, king cannot capture that piece
                    // because he would end up in check
                    if(kingDangerSquares) {
                        squares.add(square);
                    }
                    break;
                }
                // Square is blocked by enemy piece
                else {
                    squares.add(square);
                    break;
                }
            }
        }
        return squares;
    }

    private LinkedList<Square> generatePawnSquares(Piece piece, boolean kingDangerSquares) {
        LinkedList<Square> squares = new LinkedList<>();

        int directionIndex = piece.getColor() == Piece.WHITE ? Directions.TOP : Directions.BOTTOM;
        // Since pawns reaching the end of the board get promoted
        // there is no need to worry about there being enough squares between it and the end
        int index = piece.positionIndex + Board.OFFSETS[directionIndex];
        Square square = (Square) board.getComponent(index);
        // Only add square if standard moves are calculated as this square cannot be attacked by the pawn
        if(!kingDangerSquares && square.getPiece() == null) {
            squares.add(square);
        }
        // Pawn only attacks diagonally in the forward direction
        for(directionIndex = Directions.LEFT; directionIndex <= Directions.RIGHT; directionIndex += 4) {
            int[] numberSquaresToBorder = square.getNumberOfSquaresToBorder();
            if(numberSquaresToBorder[directionIndex] <= 0) continue;

            int attackedIndex = index + Board.OFFSETS[directionIndex];
            Square attackedSquare = (Square) board.getComponent(attackedIndex);
            if(attackedSquare.getPiece() == null) {
                // Only add when king danger squares are calculated since pawn can only move diagonally
                // when capturing a piece
                if(kingDangerSquares)  {
                    squares.add(attackedSquare);
                }
            } else if(attackedSquare.getPiece().getColor() == piece.getColor()) {
                // King cannot capture this piece as he would end up in check
                if(kingDangerSquares) {
                    squares.add(attackedSquare);
                }
            } else {
                squares.add(attackedSquare);
            }
        }
        return squares;
    }

    private LinkedList<Square> generateKnightSquares(Piece piece, boolean kingDangerSquares) {
        LinkedList<Square> squares = new LinkedList<>();

        Square squareOfPiece = (Square) board.getComponent(piece.positionIndex);
        int[] numberSquaresToBorder;

        for(int directionIndex = Directions.LEFT; directionIndex <= Directions.BOTTOM; directionIndex += 2) {
            numberSquaresToBorder = squareOfPiece.getNumberOfSquaresToBorder();
            if(numberSquaresToBorder[directionIndex] < 2) continue;

            // Go two steps in this direction
            int firstStepIndex = piece.positionIndex + (Board.OFFSETS[directionIndex] * 2);
            Square firstStepSquare = (Square) board.getComponent(firstStepIndex);
            numberSquaresToBorder = firstStepSquare.getNumberOfSquaresToBorder();

            int startDirectionIndex;
            int endDirectionIndex;
            if(directionIndex == Directions.LEFT || directionIndex == Directions.RIGHT) {
                // Last step was horizontal so this step must be vertical
                startDirectionIndex = Directions.TOP;
                endDirectionIndex = Directions.BOTTOM;
            } else {
                // Last step was vertical so this step must be horizontal
                startDirectionIndex = Directions.LEFT;
                endDirectionIndex = Directions.RIGHT;
            }

            for(int secondDirectionIndex = startDirectionIndex; secondDirectionIndex <= endDirectionIndex; secondDirectionIndex += 4) {
                if(numberSquaresToBorder[secondDirectionIndex] <= 0) continue;
                int secondStepIndex = firstStepIndex + Board.OFFSETS[secondDirectionIndex];
                Square secondStepSquare = (Square) board.getComponent(secondStepIndex);

                if(secondStepSquare.getPiece() == null) {
                    squares.add(secondStepSquare);
                } else if(secondStepSquare.getPiece().getColor() == piece.getColor()) {
                    // King cannot capture this piece as he would end up in check
                    if(kingDangerSquares) {
                        squares.add(secondStepSquare);
                    }
                } else {
                    squares.add(secondStepSquare);
                }
            }
        }
        return squares;
    }

    private LinkedList<Square> generateKingSquares(Piece piece, boolean kingDangerSquares) {
        LinkedList<Square> squares = new LinkedList<>();

        Square squareOfPiece = (Square) board.getComponent(piece.positionIndex);
        int[] numberSquaresToBorder = squareOfPiece.getNumberOfSquaresToBorder();

        for(int directionIndex = Directions.LEFT; directionIndex <= Directions.BOTTOM_LEFT; directionIndex++) {
            if(numberSquaresToBorder[directionIndex] <= 0) continue;
            int index = piece.positionIndex + Board.OFFSETS[directionIndex];
            Square square = (Square) board.getComponent(index);

            if(square.getPiece() == null) {
                squares.add(square);
            } else if(square.getPiece().getColor() == piece.getColor()) {
                // King cannot move there as he would end up in check
                if(kingDangerSquares) {
                    squares.add(square);
                }
            } else {
                squares.add(square);
            }
        }
        return squares;
    }

    void splitPieces() {
        whitePieces.clear();
        darkPieces.clear();
        for(Piece piece : board.pieces) {
            if(piece.getColor() == Piece.WHITE) {
                whitePieces.add(piece);
            } else {
                darkPieces.add(piece);
            }
        }
    }

    public void handleMove(Piece pieceMoved, Square startSquare, Square targetSquare) {
        // Handle special pieces
        pieceMoved.doubleMovePossible = false;

        // Handle special moves
        Move moveDone = board.getMove(startSquare, targetSquare);
        if(moveDone == null) return;

        switch (moveDone.moveFlag()) {
            case MoveFlags.DOUBLE_PAWN_PUSH:
                // Generate en passant moves
                board.enPassantMoves = generateEnPassantMoves(targetSquare);
                break;
            case MoveFlags.EN_PASSANT:
                // Handle the en passant move
                // Find the square of the captured piece
                int directionOfCapturedPiece = pieceMoved.getColor() == 0 ? 2 : 6;
                int indexOfCaptureSquare = pieceMoved.positionIndex + Board.OFFSETS[directionOfCapturedPiece];
                Square captureSquare = (Square) board.getComponent(indexOfCaptureSquare);
                board.pieces.remove(captureSquare.getPiece());
                captureSquare.removePiece();
                break;
            case MoveFlags.CASTLE_KING_SIDE:
                int rookSquareIndex = moveDone.targetSquare().getIndex() + Board.OFFSETS[Directions.RIGHT];
                int newRookSquareIndex = moveDone.targetSquare().getIndex() + Board.OFFSETS[Directions.LEFT];
                Square rookSquare = (Square) board.getComponent(rookSquareIndex);
                Square newRookSquare = (Square) board.getComponent(newRookSquareIndex);
                Piece rook = rookSquare.getPiece();
                rookSquare.removePiece();
                newRookSquare.addPiece(rook);
                rook.positionIndex = newRookSquare.getIndex();
                break;
            case MoveFlags.CASTLE_QUEEN_SIDE:
                rookSquareIndex = moveDone.targetSquare().getIndex() + (Board.OFFSETS[Directions.LEFT] * 2);
                newRookSquareIndex = moveDone.targetSquare().getIndex() + Board.OFFSETS[Directions.RIGHT];
                rookSquare = (Square) board.getComponent(rookSquareIndex);
                newRookSquare = (Square) board.getComponent(newRookSquareIndex);
                rook = rookSquare.getPiece();
                rookSquare.removePiece();
                newRookSquare.addPiece(rook);
                rook.positionIndex = newRookSquare.getIndex();
                break;
            case MoveFlags.TRANSFORM:
                // Handle transform
                Object typeOfNewPiece = board.createTransformDialog(pieceMoved.getColor());
                if (typeOfNewPiece == null) {
                    typeOfNewPiece = board.createTransformDialog(pieceMoved.getColor());
                }
                pieceMoved.transformInto((int) typeOfNewPiece);
                break;
        }
    }
}
