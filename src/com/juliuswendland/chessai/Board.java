package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class Board extends JLayeredPane {
    public static final Color DARK_COLOR = new Color(150, 80, 14);
    public static final Color LIGHT_COLOR = new Color(242, 165, 92);
    public static final Color DARK_COLOR_HIGHLIGHT = new Color(133, 28, 120);
    public static final Color LIGHT_COLOR_HIGHLIGHT = new Color(191, 82, 178);
    public LinkedList<Piece> pieces = new LinkedList<>();
    public LinkedList<Move> legalMoves;
    public LinkedList<Move> enPassantMoves = new LinkedList<>();
    public static final int[] OFFSETS = {-1, -9, -8, -7, 1, 9, 8, 7};
    public int colorAtMove = 1;
    public MoveGenerator moveGenerator;
    public boolean[][] possibleCastles = {{true, true}, {true, true}};
    public Map<Move, Piece> moveHistory = new HashMap<>();

    public Board(String fen) {
        Dimension boardSize = new Dimension(1000, 1000);
        setPreferredSize(boardSize);
        setBounds(0, 0, boardSize.width, boardSize.height);
        setLayout(new GridLayout(8, 8));
        DragAndDropHandler dragAndDropHandler = new DragAndDropHandler(this);
        addMouseListener(dragAndDropHandler);
        addMouseMotionListener(dragAndDropHandler);
        moveGenerator = new MoveGenerator(this);

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
        legalMoves = moveGenerator.generateLegalMoves();
    }

    public void choseComputerMove() {
        // TODO remove this
        if(legalMoves.size() == 0) return;

        // Choose a random move
        int minMoveIndex = 0;
        int maxMoveIndex = legalMoves.size();
        Random random = new Random();
        int moveIndex = random.nextInt(maxMoveIndex) + minMoveIndex;

        Move move = legalMoves.get(moveIndex);
        makeMove(move);
    }

    public void makeMove(Move move) {
        Square startSquare = move.startSquare();
        Square targetSquare = move.targetSquare();
        Piece piece = startSquare.getPiece();

        // Captured piece isn't always on target square because of en passant moves
        Piece capturedPiece;
        Square capturedSquare;

        if(move.moveFlag() == MoveFlags.EN_PASSANT) {
            int capturedPieceDirection = piece.getColor() == Piece.WHITE ? Directions.BOTTOM : Directions.TOP;
            int capturedSquareIndex = targetSquare.getIndex() + OFFSETS[capturedPieceDirection];
            capturedSquare = (Square) getComponent(capturedSquareIndex);
        }
        else {
            capturedSquare = targetSquare;
        }
        capturedPiece = capturedSquare.getPiece();

        // Remove piece from start square and add to target square
        startSquare.removePiece();
        targetSquare.removePiece();
        targetSquare.addPiece(piece);
        piece.positionIndex = targetSquare.getIndex();

        // Handle special moves
        moveGenerator.handleMove(piece, move);

        // Add to moveHistory to make it possible to unmake the move later
        moveHistory.put(move, capturedPiece);
    }

    public LinkedList<Square> getSquaresBetweenTwoPieces(Piece pieceOne, Piece pieceTwo) {
        LinkedList<Square> squares = new LinkedList<>();

        int index = pieceOne.positionIndex;
        Square startSquare = (Square) getComponent(index);
        int[] numberSquaresToBorder = startSquare.getNumberOfSquaresToBorder();

        // Start going to the edge of the board until you reach the desired second square
        // If edge is reached then there are no squares in between
        for(int directionIndex = Directions.LEFT; directionIndex <= Directions.BOTTOM_LEFT; directionIndex++) {
            squares.clear();
            index = pieceOne.positionIndex;
            for(int i = 0; i < numberSquaresToBorder[directionIndex]; i++) {
                index += OFFSETS[directionIndex];
                Square square = (Square) getComponent(index);
                if(square.getPiece() == pieceTwo) {
                    return squares;
                }
                squares.add(square);
            }
        }
        squares.clear();
        return squares;
    }

    public Move getMove(Square startSquare, Square targetSquare) {
        for(Move move : legalMoves) {
            if(move.startSquare() == startSquare && move.targetSquare() == targetSquare) {
                return move;
            }
        }
        return null;
    }

    public Object createTransformDialog(int color) {
        Container parent = getParent();
        JOptionPane transformPane = new JOptionPane();
        transformPane.setMessage("Select which piece you want to promote your pawn to.");
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

    public JButton createTransformDialogButton(JOptionPane parent, int type, int color) {
        JButton button = new JButton(Resources.ICONS[color][type]);
        ActionListener buttonListener = e -> parent.setValue(type);
        button.addActionListener(buttonListener);
        return button;
    }

    public void displayMoves(Piece pieceToMove) {
        // Display possible moves
        Square square = (Square) pieceToMove.getParent();
        square.removePiece();
        for(Move move : legalMoves) {
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
