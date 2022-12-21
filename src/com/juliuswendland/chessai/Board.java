package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
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
    public int colorAtMove = 1;

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
            if(piece.getColor() != colorAtMove) continue;

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

        // Generate possible castle moves
        for(Piece piece : pieces) {
            if(piece.getType() != Piece.KING) continue;
            generateCastleMoves(piece);
        }
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
        int moveFlag = MoveFlags.NONE;

        // If is black piece, move down, else move up
        int directionIndex = piece.getColor() == 0 ? 6 : 2;
        int currentIndex = piece.positionIndex;

        if(numberSquaresToBorder[directionIndex] <= 0) return;

        currentIndex += OFFSETS[directionIndex];
        Square targetSquare = (Square) getComponent(currentIndex);

        // Check if pawn can be transformed
        if(targetSquare.getRank() == 0 || targetSquare.getRank() == 7) {
            moveFlag = MoveFlags.TRANSFORM;
        }

        // Pawn must not be blocked by another piece
        if(targetSquare.getPiece() == null) {
            pseudoLegalMoves.add(new Move(startSquare, targetSquare, moveFlag));

            // Check if double pawn push is possible, if so, generate double move
            if(piece.doubleMovePossible) {
                int doubleMoveSquareIndex = currentIndex + OFFSETS[directionIndex];
                Square doubleMoveSquare = (Square) getComponent(doubleMoveSquareIndex);
                // Pawn must not be blocked by another piece
                if(doubleMoveSquare.getPiece() == null) {
                    pseudoLegalMoves.add(new Move(startSquare, doubleMoveSquare, MoveFlags.DOUBLE_PAWN_PUSH));
                }
            }
        }

        // Check if there are any pieces to attack
        // Pawn attacks diagonally in the direction it moves normally
        numberSquaresToBorder = targetSquare.getNumberOfSquaresToBorder();

        if (numberSquaresToBorder[0] > 0) {
            int indexOfLeftSquare = currentIndex + OFFSETS[0];
            targetSquare = (Square) getComponent(indexOfLeftSquare);
            if(targetSquare.getPiece() != null && targetSquare.getPiece().getColor() != piece.getColor()) {
                pseudoLegalMoves.add(new Move(startSquare, targetSquare, moveFlag));
            }
        }

        if (numberSquaresToBorder[4] > 0) {
            int indexOfRightSquare = currentIndex + OFFSETS[4];
            targetSquare = (Square) getComponent(indexOfRightSquare);
            if(targetSquare.getPiece() != null && targetSquare.getPiece().getColor() != piece.getColor()) {
                pseudoLegalMoves.add(new Move(startSquare, targetSquare, moveFlag));
            }
        }
    }

    public void generateEnPassantMoves(Square targetSquareOfPrevMove) {
        // Generate possible En Passant moves after a double pawn push
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

    private void generateCastleMoves(Piece piece) {
        // King must not have moved
        if(piece.hasMovesPreviously) return;
        // King must not be in check
        for(Move move : pseudoLegalMoves) {
            if(move.targetSquare().getIndex() == piece.positionIndex) return;
        }

        Square startSquare = (Square) getComponent(piece.positionIndex);
        int[] numberSquaresToBorder = startSquare.getNumberOfSquaresToBorder();

        // Check if castling is possible for each side
        int currentIndex;
        Square targetSquare  = null;
        boolean pathIsClear;

        for(int directionIndex = 0; directionIndex <= 4; directionIndex += 4) {
            currentIndex = startSquare.getIndex();

            // Check if path to rook is clear
            pathIsClear = false;
            for(int i = 0; i < numberSquaresToBorder[directionIndex] - 1; i++) {
                currentIndex += OFFSETS[directionIndex];

                targetSquare = (Square) getComponent(currentIndex);
                // Path must not be blocked by any pieces
                if(targetSquare.getPiece() != null) break;
                // Squares in the path must not be attacked by any enemy piece
                if(checkIfSquareIsAttacked(targetSquare, piece.getColor())) break;

                pathIsClear = true;
            }

            // Path must be clear
            if(!pathIsClear) continue;

            int targetSquareIndex = currentIndex + OFFSETS[directionIndex];
            Square rookSquare = (Square) getComponent(targetSquareIndex);
            // Target square must have piece on it
            if(rookSquare.getPiece() == null) continue;
            // Piece on target square must be a rook
            if(rookSquare.getPiece().getType() != Piece.ROOK) continue;
            // Rook must not have moved in this game
            if(rookSquare.getPiece().hasMovesPreviously) continue;

            pseudoLegalMoves.add(new Move(startSquare, targetSquare, MoveFlags.CASTLE));
        }
    }

    private boolean checkIfSquareIsAttacked(Square square, int friendlyColor) {
        // Check if given square is under attack by enemy piece
        for(Move move : pseudoLegalMoves) {
            if(move.targetSquare() == square) {
                if(move.startSquare().getPiece().getColor() != friendlyColor) {
                    return true;
                }
            }
        }
        return false;
    }

    public Move getMove(Square startSquare, Square targetSquare) {
        for(Move move : pseudoLegalMoves) {
            if(move.startSquare() == startSquare && move.targetSquare() == targetSquare) {
                return move;
            }
        }
        return null;
    }

    public void handleMove(Piece pieceMoved, Square startSquare, Square targetSquare) {
        // Handle special pieces
        if(pieceMoved.getType() == Piece.PAWN) {
            pieceMoved.doubleMovePossible = false;
        }

        // Handle special moves
        Move moveDone = getMove(startSquare, targetSquare);
        if(moveDone != null) {
            if(moveDone.moveFlag() == MoveFlags.DOUBLE_PAWN_PUSH) {
                // Generate en passant moves
                generateEnPassantMoves(targetSquare);
            }
            else if(moveDone.moveFlag() == MoveFlags.EN_PASSANT) {
                // Handle the en passant move
                // Find the square of the captured piece
                int directionOfCapturedPiece = pieceMoved.getColor() == 0 ? 2 : 6;
                int indexOfCaptureSquare = pieceMoved.positionIndex + Board.OFFSETS[directionOfCapturedPiece];
                Square captureSquare = (Square) getComponent(indexOfCaptureSquare);
                pieces.remove(captureSquare.getPiece());
                System.out.println(captureSquare.getPiece());
                captureSquare.removePiece();
            }
            else if(moveDone.moveFlag() == MoveFlags.CASTLE) {
                // Handle castle
                // Get left and right square of king
                int indexOfLeftSquare = targetSquare.getIndex() + Board.OFFSETS[0];
                int indexOfRightSquare = targetSquare.getIndex() + Board.OFFSETS[4];
                Square leftSquare = (Square) getComponent(indexOfLeftSquare);
                Square rightSquare = (Square) getComponent(indexOfRightSquare);

                // Switch the piece of left and right square
                Piece rook;
                if(leftSquare.getPiece() != null) {
                    rook = leftSquare.getPiece();
                    leftSquare.removePiece();
                    rightSquare.addPiece(rook);
                    rook.positionIndex = rightSquare.getIndex();
                } else {
                    rook = rightSquare.getPiece();
                    rightSquare.removePiece();
                    leftSquare.addPiece(rook);
                    rook.positionIndex = leftSquare.getIndex();
                }
                rook.hasMovesPreviously = true;
            }
            else if(moveDone.moveFlag() == MoveFlags.TRANSFORM) {
                // Handle transform
                Object typeOfNewPiece = createTransformDialog(pieceMoved.getColor());
                if(typeOfNewPiece == null) {
                    typeOfNewPiece = createTransformDialog(pieceMoved.getColor());
                }
                pieceMoved.transformInto((int) typeOfNewPiece);
            }
        }
    }

    private Object createTransformDialog(int color) {
        Container parent = getParent();
        JOptionPane transformPane = new JOptionPane();
        transformPane.setMessage("Select which piece you want to turn your pawn into.");
        transformPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);

        JButton queenButton = createTransformDialogButton(transformPane, Piece.QUEEN, color);
        JButton knightButton = createTransformDialogButton(transformPane, Piece.KNIGHT, color);
        JButton rookButton = createTransformDialogButton(transformPane, Piece.ROOK, color);
        JButton bishopButton = createTransformDialogButton(transformPane, Piece.BISHOP, color);
        Object[] options = new Object[] {queenButton, knightButton, rookButton, bishopButton};

        transformPane.setOptions(options);
        JDialog transformDialog = transformPane.createDialog(parent, "Pawn Transformation");
        transformDialog.setVisible(true);

        return transformPane.getValue();
    }

    private JButton createTransformDialogButton(JOptionPane parent, int type, int color) {
        JButton button = new JButton(Resources.ICONS[color][type]);
        ActionListener buttonListener = e -> parent.setValue(type);
        button.addActionListener(buttonListener);
        return button;
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
