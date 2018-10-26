/*
  Copyright (C) 2002 Laurent Martelli <laurent@aopsys.com>

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
  USA */

package  org.objectweb.jac.core.translators;

import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;


/**
 * Various often used functions.
 */
public class Utils {
   /**
    * Returns true is t is a primitive type, excluding VOID.
    */
   public static boolean isPrimitive(Type t) {
      return (t instanceof BasicType) && t!=Type.VOID;
   }

}
