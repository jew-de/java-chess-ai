package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Board extends JLayeredPane {
    public static final Color DARK_COLOR = new Color(150, 80, 14);
    public static final Color LIGHT_COLOR = new Color(242, 165, 92);
    public static final Color DARK_COLOR_HIGHLIGHT = new Color(133, 28, 120);
    public static final Color LIGHT_COLOR_HIGHLIGHT = new Color(191, 82, 178);
    public LinkedList<Piece> pieces = new LinkedList<>();
    public LinkedList<Move> pseudoLegalMoves = new LinkedList<>();
    public LinkedList<Move> enPassantMoves = new LinkedList<>();
    public static final int[] OFFSETS = {-1, -9, -8, -7, 1, 9, 8, 7};

    public Board(String fen) {
        Dimension boardSize = new Dimension(1000, 1000);
        setPreferredSize(boardSize);
        setBounds(0, 0, boardSize.width, boardSize.height);
        setLayout(new GridLayout(8, 8));
        DragAndDropHandler dragAndDropHandler = new DragAndDropHandler(this);
        addMouseListener(dragAndDropHandler);
        addMouseMotionListener(dragAndDropHandler);

        // Build the chess board by squares
        for(int rank = 0; rank < 8; rank++) {
            for(int file = 0; file < 8; file++) {
                boolean isLightSquare = (rank + file) % 2 == 0;
                Color squareColor = isLightSquare ? LIGHT_COLOR : DARK_COLOR;
                Square square = new Square(squareColor, rank, file);
                add(square, JLayeredPane.DEFAULT_LAYER);
             }
        }

        // Add pieces based on given FEN-String
        interpretFenString(fen);

        renderPiecesInitially();

        // Generate every possible move
        generatePseudoLegalMoves();
    }

    public void generatePseudoLegalMoves() {
        pseudoLegalMoves.clear();

        // Generate moves for individual pieces
        for(Piece piece : pieces) {
            if(piece.getType() == Piece.KING) {
                generateKingMoves(piece);
            }
            else if(piece.getType() == Piece.QUEEN) {
                generateSlidingMoves(piece);
            }
            else if(piece.getType() == Piece.BISHOP) {
                generateSlidingMoves(piece);
            }
            else if(piece.getType() == Piece.KNIGHT) {
                generateKnightMoves(piece);
            }
            else if(piece.getType() == Piece.ROOK) {
                generateSlidingMoves(piece);
            } else {
                generatePawnMoves(piece);
            }
        }

        // Generate possible en passant moves
        pseudoLegalMoves.addAll(enPassantMoves);
        // En Passant moves must be done immediately
        enPassantMoves.clear();
    }

    private void generateSlidingMoves(Piece piece) {
        Square startSquare = (Square) getComponent(piece.positionIndex);
        int[] numberSquaresToBorder = startSquare.getNumberOfSquaresToBorder();

        int startDirectionIndex;
        int directionIndexIncrement;
        // Piece is a bishop, move diagonally
        if(piece.getType() == Piece.BISHOP) {
            startDirectionIndex = 1;
            directionIndexIncrement = 2;
        }
        // Piece is rook, move in straight line
        else if(piece.getType() == Piece.ROOK) {
            startDirectionIndex = 0;
            directionIndexIncrement = 2;
        }
        // Piece is a queen, move all directions
        else {
            startDirectionIndex = 0;
            directionIndexIncrement = 1;
        }

        for(int directionIndex = startDirectionIndex; directionIndex <= 7; directionIndex += directionIndexIncrement) {
            int currentIndex = startSquare.getIndex();

            for(int i = 0; i < numberSquaresToBorder[directionIndex]; i++) {
                currentIndex += OFFSETS[directionIndex];
                Square targetSquare = (Square) getComponent(currentIndex);

                if(targetSquare.getPiece() == null) {
                    pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
                }
                // Square is blocked by friendly piece, cant move there
                else if(targetSquare.getPiece().getColor() == piece.getColor()) {
                    break;
                }
                // Square is blocked by enemy piece, attack that piece
                else {
                    pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
                    break;
                }
            }
        }
    }

    private void generateKingMoves(Piece piece) {
        Square startSquare = (Square) getComponent(piece.positionIndex);
        int[] numberOfSquaresToBorder = startSquare.getNumberOfSquaresToBorder();

        for(int directionIndex = 0; directionIndex <= 7; directionIndex++) {
            if(numberOfSquaresToBorder[directionIndex] == 0) continue;

            Square targetSquare = (Square) getComponent(piece.positionIndex + OFFSETS[directionIndex]);
            if(targetSquare.getPiece() == null) {
                pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
            }
            else if(targetSquare.getPiece().getColor() != piece.getColor()) {
                pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
            }
        }
    }

    private void generateKnightMoves(Piece piece) {
        Square startSquare = (Square) getComponent(piece.positionIndex);
        int[] numberOfSquaresToBorder;

        // Make the knight move in its typical "L" shape
        for (int directionIndex = 0; directionIndex <= 6; directionIndex += 2) {
            numberOfSquaresToBorder = startSquare.getNumberOfSquaresToBorder();
            int currentIndex = piece.positionIndex;

            // Make the two steps in one direction
            Square tempSquare = null;
            if (numberOfSquaresToBorder[directionIndex] >= 2) {
                currentIndex += (OFFSETS[directionIndex] * 2);
                tempSquare = (Square) getComponent(currentIndex);
            }

            if (tempSquare == null) continue;

            numberOfSquaresToBorder = tempSquare.getNumberOfSquaresToBorder();

            // Last move was horizontal, so next move must be vertical
            if (directionIndex == 0 || directionIndex == 4) {
                if (numberOfSquaresToBorder[2] > 0) {
                    Square targetSquare = (Square) getComponent(currentIndex + OFFSETS[2]);

                    if (targetSquare.getPiece() == null) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
                    } else if (targetSquare.getPiece().getColor() != piece.getColor()) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
                    }
                }
                if (numberOfSquaresToBorder[6] > 0) {
                    Square targetSquare = (Square) getComponent(currentIndex + OFFSETS[6]);

                    if (targetSquare.getPiece() == null) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
                    } else if (targetSquare.getPiece().getColor() != piece.getColor()) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
                    }
                }
            }
            // Last move was vertical, so next move must be horizontal
            else {
                if (numberOfSquaresToBorder[0] > 0) {
                    Square targetSquare = (Square) getComponent(currentIndex + OFFSETS[0]);

                    if (targetSquare.getPiece() == null) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
                    } else if (targetSquare.getPiece().getColor() != piece.getColor()) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
                    }
                }
                if (numberOfSquaresToBorder[4] > 0) {
                    Square targetSquare = (Square) getComponent(currentIndex + OFFSETS[4]);

                    if (targetSquare.getPiece() == null) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
                    } else if (targetSquare.getPiece().getColor() != piece.getColor()) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
                    }
                }
            }
        }
    }

    private void generatePawnMoves(Piece piece) {
        Square startSquare = (Square) getComponent(piece.positionIndex);
        int[] numberSquaresToBorder = startSquare.getNumberOfSquaresToBorder();

        // If is black piece, move down, else move up
        int directionIndex = piece.getColor() == 0 ? 6 : 2;
        int currentIndex = piece.positionIndex;

        if(numberSquaresToBorder[directionIndex] <= 0) return;

        currentIndex += OFFSETS[directionIndex];
        Square targetSquare = (Square) getComponent(currentIndex);

        if(targetSquare.getPiece() == null) {
            pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));

            // Check if double pawn push is possible, if so, generate double move
            if(piece.doubleMovePossible) {
                int doubleMoveSquareIndex = currentIndex + OFFSETS[directionIndex];
                Square doubleMoveSquare = (Square) getComponent(doubleMoveSquareIndex);

                if(doubleMoveSquare.getPiece() == null) {
                    pseudoLegalMoves.add(new Move(startSquare, doubleMoveSquare, MoveFlags.DOUBLE_PAWN_PUSH));
                }
            }
        }

        // Check if there are any pieces to attack
        numberSquaresToBorder = targetSquare.getNumberOfSquaresToBorder();

        if (numberSquaresToBorder[0] > 0) {
            int indexOfLeftSquare = currentIndex + OFFSETS[0];
            targetSquare = (Square) getComponent(indexOfLeftSquare);
            if(targetSquare.getPiece() != null && targetSquare.getPiece().getColor() != piece.getColor()) {
                pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
            }
        }

        if (numberSquaresToBorder[4] > 0) {
            int indexOfRightSquare = currentIndex + OFFSETS[4];
            targetSquare = (Square) getComponent(indexOfRightSquare);
            if(targetSquare.getPiece() != null && targetSquare.getPiece().getColor() != piece.getColor()) {
                pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.NONE));
            }
        }
    }

    public void generateEnPassantMoves(Square targetSquareOfPrevMove) {
        // Check if a piece is on the square to the left or right of the previous target square
        int currentIndex = targetSquareOfPrevMove.getIndex();
        int indexOfLeftSquare = currentIndex + OFFSETS[0];
        int indexOfRightSquare = currentIndex + OFFSETS[4];
        Square leftSquare = (Square) getComponent(indexOfLeftSquare);
        Square rightSquare = (Square) getComponent(indexOfRightSquare);

        // Calculate the target square of the en passant move
        int directionOfTargetSquare = targetSquareOfPrevMove.getPiece().getColor() == 0 ? 2 : 6;
        int indexOfTargetSquare = currentIndex + OFFSETS[directionOfTargetSquare];
        Square targetSquare = (Square) getComponent(indexOfTargetSquare);

        if(leftSquare.getPiece() != null && leftSquare.getPiece().getColor() != targetSquareOfPrevMove.getPiece().getColor()) {
            if(leftSquare.getPiece().getType() == Piece.PAWN) {
                enPassantMoves.add(new Move(leftSquare, targetSquare, MoveFlags.EN_PASSANT));
            }
        }
        if(rightSquare.getPiece() != null && rightSquare.getPiece().getColor() != targetSquareOfPrevMove.getPiece().getColor()) {
            if(rightSquare.getPiece().getType() == Piece.PAWN) {
                enPassantMoves.add(new Move(rightSquare, targetSquare, MoveFlags.EN_PASSANT));
            }
        }
    }

    public Move getMove(Square startSquare, Square targetSquare) {
        for(Move move : pseudoLegalMoves) {
            if(move.startSquare() == startSquare && move.targetSquare() == targetSquare) {
                return move;
            }
        }
        return null;
    }

    public void displayMoves(Piece pieceToMove) {
        // Display possible moves
        Square square = (Square) pieceToMove.getParent();
        square.removePiece();
        for(Move move : pseudoLegalMoves) {
            if(square.getIndex() == move.startSquare().getIndex()) {
                move.targetSquare().setTargetSquare();
            }
        }
    }

    private void interpretFenString(String fen) {
        Map<Character, Integer> fenToPiece = new HashMap<>();
        fenToPiece.put('k', 0);
        fenToPiece.put('q', 1);
        fenToPiece.put('b', 2);
        fenToPiece.put('n', 3);
        fenToPiece.put('r', 4);
        fenToPiece.put('p', 5);

        char[] characters = fen.toCharArray();
        int currentIndex = 0;

        for(char character : characters) {
            char lowerCaseCharacter = Character.toLowerCase(character);

            if(fenToPiece.containsKey(lowerCaseCharacter)) {
                // Character resembles a piece
                int type = fenToPiece.get(lowerCaseCharacter);
                int color = Character.isUpperCase(character) ? 1 : 0;
                pieces.add(new Piece(currentIndex, type, color));
                currentIndex++;
                continue;
            }

            int numberOfSquares = Character.getNumericValue(character);
            // Character is a slash
            if(numberOfSquares == -1) continue;
            currentIndex += numberOfSquares;

        }
    }

    private void renderPiecesInitially() {
        for(Piece piece : pieces) {
            Square square = (Square) getComponent(piece.positionIndex);
            square.addPiece(piece);
        }
    }

    public void resetAllSquares() {
        for(int i = 0; i < 64; i++) {
            Square squareToReset = (Square) getComponent(i);
            squareToReset.reset();
        }
    }
}
