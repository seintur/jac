/*
  Copyright (C) 2003 Laurent Martelli <laurent@aopsys.com>

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
*/

package org.objectweb.jac.ide.diagrams;

import CH.ifa.draw.figures.AttributeFigure;
import CH.ifa.draw.figures.FontSizeHandle;
import CH.ifa.draw.framework.Figure;
import CH.ifa.draw.framework.FigureChangeEvent;
import CH.ifa.draw.framework.FigureChangeListener;
import CH.ifa.draw.standard.NullHandle;
import CH.ifa.draw.standard.OffsetLocator;
import CH.ifa.draw.standard.RelativeLocator;
import CH.ifa.draw.standard.TextHolder;
import CH.ifa.draw.util.ColorMap;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Vector;

/**
 * A TextFigure whose text color can be redefined with the method <code>getTextColor()</code>
 */
public  class TextFigure
    extends AttributeFigure
    implements FigureChangeListener, TextHolder {

    protected int fOriginX;
    protected int fOriginY;

    // cache of the TextFigure's size
    transient private boolean fSizeIsDirty = true;
    transient private int     fWidth;
    transient private int     fHeight;

    private String  fText;
    private Font    fFont;
    private boolean fIsReadOnly;

    private Figure  fObservedFigure = null;
    private OffsetLocator fLocator = null;

    private static String fgCurrentFontName  = "Helvetica";
    private static int    fgCurrentFontSize  = 12;
    private static int    fgCurrentFontStyle = Font.PLAIN;

    /*
     * Serialization support.
     */
    private static final long serialVersionUID = 4599820785949456124L;

    public TextFigure() {
        fOriginX = 0;
        fOriginY = 0;
        fFont = createCurrentFont();
        setAttribute("FillColor", ColorMap.color("None"));
        fText = "";
        fSizeIsDirty = true;
    }

    public void moveBy(int x, int y) {
        willChange();
        basicMoveBy(x, y);
        if (fLocator != null) {
            fLocator.moveBy(x, y);
        }
        changed();
    }

    protected void basicMoveBy(int x, int y) {
        fOriginX += x;
        fOriginY += y;
    }

    public void basicDisplayBox(Point newOrigin, Point newCorner) {
        fOriginX = newOrigin.x;
        fOriginY = newOrigin.y;
    }

    public Rectangle displayBox() {
        Dimension extent = textExtent();
        return new Rectangle(fOriginX, fOriginY, extent.width, extent.height);
    }

    public Rectangle textDisplayBox() {
        return displayBox();
    }

    /**
     * Tests whether this figure is read only.
     */
    public boolean readOnly() {
        return fIsReadOnly;
    }

    /**
     * Sets the read only status of the text figure.
     */
    public void setReadOnly(boolean isReadOnly) {
        fIsReadOnly = isReadOnly;
    }

    /**
     * Gets the font.
     */
    public Font getFont() {
        return fFont;
    }

    /**
     * Sets the font.
     */
    public void setFont(Font newFont) {
        willChange();
        fFont = newFont;
        markDirty();
        changed();
    }

    /**
     * Updates the location whenever the figure changes itself.
     */
    public void changed() {
        super.changed();
        updateLocation();
    }

    /**
     * A text figure understands the "FontSize", "FontStyle", and "FontName"
     * attributes.
     */
    public Object getAttribute(String name) {
        Font font = getFont();
        if (name.equals("FontSize")) {
            return new Integer(font.getSize());
        }
        if (name.equals("FontStyle")) {
            return new Integer(font.getStyle());
        }
        if (name.equals("FontName")) {
            return font.getName();
        }
        return super.getAttribute(name);
    }

    /**
     * A text figure understands the "FontSize", "FontStyle", and "FontName"
     * attributes.
     */
    public void setAttribute(String name, Object value) {
        Font font = getFont();
        if (name.equals("FontSize")) {
            Integer s = (Integer)value;
            setFont(new Font(font.getName(), font.getStyle(), s.intValue()) );
        }
        else if (name.equals("FontStyle")) {
            Integer s = (Integer)value;
            int style = font.getStyle();
            if (s.intValue() == Font.PLAIN) {
                style = Font.PLAIN;
            }
            else {
                style = style ^ s.intValue();
            }
            setFont(new Font(font.getName(), style, font.getSize()) );
        }
        else if (name.equals("FontName")) {
            String n = (String)value;
            setFont(new Font(n, font.getStyle(), font.getSize()) );
        }
        else {
            super.setAttribute(name, value);
        }
    }

    /**
     * Gets the text shown by the text figure.
     */
    public String getText() {
        return fText;
    }

    /**
     * Sets the text shown by the text figure.
     */
    public void setText(String newText) {
        if (!newText.equals(fText)) {
            willChange();
            fText = newText;
            markDirty();
            changed();
        }
    }

    /**
     * Tests whether the figure accepts typing.
     */
    public boolean acceptsTyping() {
        return !fIsReadOnly;
    }

    public void drawBackground(Graphics g) {
        Rectangle r = displayBox();
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    public void drawFrame(Graphics g) {
        g.setFont(fFont);
        g.setColor(getTextColor());
        FontMetrics metrics = g.getFontMetrics(fFont);
        g.drawString(fText, fOriginX, fOriginY + metrics.getAscent());
    }

    protected Color getTextColor() {
        return (Color)getAttribute("TextColor");
    }

    private Dimension textExtent() {
        if (!fSizeIsDirty) {
            return new Dimension(fWidth, fHeight);
        }
        FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(fFont);
        fWidth = metrics.stringWidth(fText);
        fHeight = metrics.getHeight();
        fSizeIsDirty = false;
        return new Dimension(metrics.stringWidth(fText), metrics.getHeight());
    }

    private void markDirty() {
        fSizeIsDirty = true;
    }

    /**
     * Gets the number of columns to be overlaid when the figure is edited.
     */
    public int overlayColumns() {
        int length = getText().length();
        int columns = 20;
        if (length != 0) {
            columns = getText().length()+ 3;
        }
        return columns;
    }

    public Vector handles() {
        Vector handles = new Vector();
        handles.addElement(new NullHandle(this, RelativeLocator.northWest()));
        handles.addElement(new NullHandle(this, RelativeLocator.northEast()));
        handles.addElement(new NullHandle(this, RelativeLocator.southEast()));
        handles.addElement(new FontSizeHandle(this, RelativeLocator.southWest()));
        return handles;
    }

    public void connect(Figure figure) {
        if (fObservedFigure != null) {
            fObservedFigure.removeFigureChangeListener(this);
        }

        fObservedFigure = figure;
        fLocator = new OffsetLocator(figure.connectedTextLocator(this));
        fObservedFigure.addFigureChangeListener(this);
        updateLocation();
    }

    public void figureChanged(FigureChangeEvent e) {
        updateLocation();
    }

    public void figureRemoved(FigureChangeEvent e) {
        if (listener() != null) {
            listener().figureRequestRemove(new FigureChangeEvent(this));
        }
    }

    public void figureRequestRemove(FigureChangeEvent e) {}
    public void figureInvalidated(FigureChangeEvent e) {}
    public void figureRequestUpdate(FigureChangeEvent e) {}

    /**
     * Updates the location relative to the connected figure.
     * The TextFigure is centered around the located point.
     */
    protected void updateLocation() {
        if (fLocator != null) {
            Point p = fLocator.locate(fObservedFigure,this);

            p.x -= size().width/2 + fOriginX;
            p.y -= size().height/2 + fOriginY;
            if (p.x != 0 || p.y != 0) {
                willChange();
                basicMoveBy(p.x, p.y);
                changed();
            }
        }
    }

    public void release() {
        super.release();
        disconnect(fObservedFigure);
        fObservedFigure = null;
    }

    /**
     * Disconnects a text holder from a connect figure.
     */
    public void disconnect(Figure disconnectFigure) {
        if (disconnectFigure != null) {
            disconnectFigure.removeFigureChangeListener(this);
        }
        fLocator = null;
    }


    /**
     * Creates the current font to be used for new text figures.
     */
    static public Font createCurrentFont() {
        return new Font(fgCurrentFontName, fgCurrentFontStyle, fgCurrentFontSize);
    }

    /**
     * Sets the current font name
     */
    static public void setCurrentFontName(String name) {
        fgCurrentFontName = name;
    }

    /**
     * Sets the current font size.
     */
    static public void setCurrentFontSize(int size) {
        fgCurrentFontSize = size;
    }

    /**
     * Sets the current font style.
     */
    static public void setCurrentFontStyle(int style) {
        fgCurrentFontStyle = style;
    }
}