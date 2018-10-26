/*
  Copyright (C) 2002-2003 Renaud Pawlak <renaud@aopsys.com>

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

import org.objectweb.jac.aspects.gui.FieldUpdate;
import org.objectweb.jac.aspects.gui.Utils;
import org.objectweb.jac.core.rtti.ClassRepository;
import org.objectweb.jac.core.rtti.FieldItem;
import org.objectweb.jac.ide.Class;
import java.awt.Color;

public class ClassNameFigure extends TextFigure implements FieldUpdate {

   Class substance;
   ClassFigure parentFigure;

   static FieldItem nameField  = 
      ClassRepository.get().getClass(Class.class).getField("name");

   public ClassNameFigure(Class substance, ClassFigure parentFigure) {
      this.substance = substance;
      this.parentFigure = parentFigure;
      super.setText(substance.getName());
      Utils.registerField(substance,nameField,this);
   }

   public Color getTextColor() {
      return parentFigure.getColor();
   }

   /**
    * Get the value of substance.
    * @return value of substance.
    */
   public Class getSubstance() {
      return substance;
   }
   
   /**
    * Set the value of substance.
    * @param v  Value to assign to substance.
    */
   public void setSubstance(Class  v) {
      this.substance = v;
   }

   public String getName() {
      return getText();
   }

   public void setText(String s) {
      super.setText(s);
      if (substance != null && !DiagramView.init) {
         substance.setName(s);
      }
   }

   // FieldUpdate interface
   public void fieldUpdated(Object substance, FieldItem field, Object 
                            value, Object param) {
      super.setText((String)value);
   }
}
