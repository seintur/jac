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

import CH.ifa.draw.framework.ConnectionFigure;
import CH.ifa.draw.framework.DrawingEditor;


public class ConnectionTool extends CH.ifa.draw.standard.ConnectionTool {

   public ConnectionTool(DrawingEditor drawingEditor, ConnectionFigure prototype) {
      super(drawingEditor,prototype);
   }

   public boolean isActive() {
      // The AbstractTool implementation seems to always return false
      // because isUsable()==false
      return editor().tool() == this;
   }
}

