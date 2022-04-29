/*
 * Copyright (C) 2022 Nicola De Nisco
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nbexportlib;

import java.util.ArrayList;
import org.commonlib5.utils.StringOper;

/**
 * Bean per ogni libreria.
 *
 * @author Nicola De Nisco
 */
public class LibraryBean
{
  public String name, type, displayName;

  public final ArrayList<String> classpath = new java.util.ArrayList<>();
  public final ArrayList<String> src = new java.util.ArrayList<>();
  public final ArrayList<String> javadoc = new java.util.ArrayList<>();
  public final ArrayList<String> mavenPom = new java.util.ArrayList<>();

  boolean isValid()
  {
    if(StringOper.isOkStrAll(name, type))
    {
      return "j2se".equals(type) && !classpath.isEmpty();
    }

    return false;
  }
}
