package com.juliuswendland.chessai;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class DragAndDropHandler implements MouseListener, MouseMotionListener {
    Board board;
    private Piece pieceToMove = null;
    private int xAdjustment, yAdjustment;
    private Square startSquare = null;

    public DragAndDropHandler(Board board) {
        this.board = board;
    }


    @Override
    public void mousePressed(MouseEvent e) {
        pieceToMove = null;
        Component componentAtMouse = board.findComponentAt(e.getX(), e.getY());

        // Clicked on an empty square
        if(componentAtMouse instanceof Square) return;

        // Keep relative position of piece to mouse
        Point parentLocation = componentAtMouse.getParent().getLocation();
        pieceToMove = (Piece) componentAtMouse;
        if(pieceToMove.getColor() != board.colorAtMove) {
            pieceToMove = null;
            return;
        }

        xAdjustment = parentLocation.x - e.getX();
        yAdjustment = parentLocation.y - e.getY();

        board.displayMoves(pieceToMove);
        startSquare = (Square) board.getComponent(pieceToMove.positionIndex);

        pieceToMove.setLocation(e.getX() + xAdjustment, e.getY() + yAdjustment);
        board.add(pieceToMove, JLayeredPane.DRAG_LAYER);
        board.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(pieceToMove == null) return;

        // Make sure drag location is within boundaries of the board
        int x = e.getX() + xAdjustment;
        int xMax = board.getWidth() - pieceToMove.getWidth();
        x = Math.min(x, xMax);
        x = Math.max(0, x);

        int y = e.getY() + yAdjustment;
        int yMax = board.getHeight() - pieceToMove.getHeight();
        y = Math.min(y, yMax);
        y = Math.max(0, y);

        pieceToMove.setLocation(x, y);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(pieceToMove == null) return;

        pieceToMove.setVisible(false);
        board.remove(pieceToMove);
        pieceToMove.setVisible(true);

        // Make sure drop location is within boundaries of the board
        int x = e.getX();
        int xMax = board.getWidth() - pieceToMove.getWidth();
        x = Math.min(x, xMax);
        x = Math.max(0, x);

        int y = e.getY();
        int yMax = board.getHeight() - pieceToMove.getHeight();
        y = Math.min(y, yMax);
        y = Math.max(0, y);

        Component componentAtMouse = board.findComponentAt(x, y);

        board.setCursor(null);

        Square targetSquare;
        // Enemy piece on target square
        if(componentAtMouse instanceof Piece) {
            targetSquare = (Square) componentAtMouse.getParent();
        }
        // Target square is empty
        else {
            targetSquare = (Square) componentAtMouse;
        }

        // Check if move was not listed as a possible move
        // If so, return and reset piece to original position
        if(!targetSquare.isTargetSquare) {
            startSquare.addPiece(pieceToMove);
            pieceToMove = null;
            board.resetAllSquares();
            return;
        }

        // If there is a piece on the square, remove it
        if(targetSquare.getPiece() != null) {
            board.pieces.remove(targetSquare.getPiece());
            targetSquare.removePiece();
        }

        // Add new piece to square
        targetSquare.addPiece(pieceToMove);
        pieceToMove.positionIndex = targetSquare.getIndex();
        pieceToMove.hasMovesPreviously = true;
        board.colorAtMove = board.colorAtMove == 1 ? 0 : 1;

        // Reset all squares
        board.resetAllSquares();

        // Check for special moves and pieces
        board.handleMove(pieceToMove, startSquare, targetSquare);

        // Generate possible moves for new position
        board.generatePseudoLegalMoves();
    }

    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {}
}
