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

        for(Piece piece : pieces) {
            if(piece.getType() == 0) {
                generateKingMoves(piece);
            }
            else if(piece.getType() == 1) {
                generateStraightSlidingMoves(piece);
                generateDiagonalSlidingMoves(piece);
            }
            else if(piece.getType() == 2) {
                generateDiagonalSlidingMoves(piece);
            }
            else if(piece.getType() == 3) {
                generateKnightMoves(piece);
            }
            else if(piece.getType() == 4) {
                generateStraightSlidingMoves(piece);
            }
        }
    }

    private void generateStraightSlidingMoves(Piece piece) {
        Square startSquare = (Square) getComponent(piece.positionIndex);
        int[] numberOfSquaresToBorder = startSquare.getNumberOfSquaresToBorder();

        for(int directionIndex = 0; directionIndex <= 6; directionIndex += 2) {
            int currentIndex = startSquare.getIndex();

            for(int i = 0; i < numberOfSquaresToBorder[directionIndex]; i++) {
                currentIndex += OFFSETS[directionIndex];
                Square targetSquare = (Square) getComponent(currentIndex);

                if(targetSquare.getPiece() == null) {
                    pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                }
                // Square is blocked by friendly piece
                else if(targetSquare.getPiece().getColor() == piece.getColor()) {
                    break;
                }
                // Square is blocked by enemy piece, attack enemy piece
                else {
                    pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                    break;
                }
            }
        }
    }

    private void generateDiagonalSlidingMoves(Piece piece) {
        Square startSquare = (Square) getComponent(piece.positionIndex);
        int[] numberOfSquaresToBorder = startSquare.getNumberOfSquaresToBorder();

        for(int directionIndex = 1; directionIndex <= 7; directionIndex += 2) {
            int currentIndex = startSquare.getIndex();

            for(int i = 0; i < numberOfSquaresToBorder[directionIndex]; i++) {
                currentIndex += OFFSETS[directionIndex];
                Square targetSquare = (Square) getComponent(currentIndex);

                if(targetSquare.getPiece() == null) {
                    pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                }
                // Square is blocked by friendly piece
                else if(targetSquare.getPiece().getColor() == piece.getColor()) {
                    break;
                }
                // Square is blocked by enemy piece, attack enemy piece
                else {
                    pseudoLegalMoves.add(new Move(startSquare, targetSquare));
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
                pseudoLegalMoves.add(new Move(startSquare, targetSquare));
            }
            else if(targetSquare.getPiece().getColor() == piece.getColor()) {
                continue;
            }
            else {
                pseudoLegalMoves.add(new Move(startSquare, targetSquare));
            }
        }
    }

    private void generateKnightMoves(Piece piece) {
        Square startSquare = (Square) getComponent(piece.positionIndex);
        int[] numberOfSquaresToBorder;

        // Make the knight move in its typical "L" shape
        for(int directionIndex = 0; directionIndex <= 6; directionIndex += 2) {
            numberOfSquaresToBorder = startSquare.getNumberOfSquaresToBorder();
            int currentIndex = piece.positionIndex;

            // Make the two steps in one direction
            Square tempSquare = null;
            if(numberOfSquaresToBorder[directionIndex] >= 2) {
                currentIndex += (OFFSETS[directionIndex] * 2);
                tempSquare = (Square) getComponent(currentIndex);
            }

            if(tempSquare == null) continue;

            numberOfSquaresToBorder = tempSquare.getNumberOfSquaresToBorder();

            // Last move was horizontal, so next move must be vertical
            if(directionIndex == 0 || directionIndex == 4) {
                if(numberOfSquaresToBorder[2] > 0) {
                    Square targetSquare = (Square) getComponent(currentIndex + OFFSETS[2]);

                    if(targetSquare.getPiece() == null) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                    } else if(targetSquare.getPiece().getColor() != piece.getColor()) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                    }
                }
                if(numberOfSquaresToBorder[6] > 0) {
                    Square targetSquare = (Square) getComponent(currentIndex + OFFSETS[6]);

                    if(targetSquare.getPiece() == null) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                    } else if(targetSquare.getPiece().getColor() != piece.getColor()) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                    }
                }
            }
            // Last move was vertical, so next move must be horizontal
            else {
                if(numberOfSquaresToBorder[0] > 0) {
                    Square targetSquare = (Square) getComponent(currentIndex + OFFSETS[0]);

                    if(targetSquare.getPiece() == null) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                    } else if(targetSquare.getPiece().getColor() != piece.getColor()) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                    }
                }
                if(numberOfSquaresToBorder[4] > 0) {
                    Square targetSquare = (Square) getComponent(currentIndex + OFFSETS[4]);

                    if(targetSquare.getPiece() == null) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                    } else if(targetSquare.getPiece().getColor() != piece.getColor()) {
                        pseudoLegalMoves.add(new Move(startSquare, targetSquare));
                    }
                }
            }
        }
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
}
