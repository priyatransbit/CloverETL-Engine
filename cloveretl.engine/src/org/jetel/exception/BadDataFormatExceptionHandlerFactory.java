/*
 *  jETeL/Clover - Java based ETL application framework.
 *  Created on Apr 17, 2003
 *  Copyright (C) 2003, 2002  David Pavlis, Wes Maciorowski
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jetel.exception;

/**
 * @author maciorowski
 *
 */
public class BadDataFormatExceptionHandlerFactory {

public static BadDataFormatExceptionHandler getHandler(String aDataPolicy) {
	if(aDataPolicy.equalsIgnoreCase("strict")) {
		return new BadDataFormatExceptionHandler();
	} else if(aDataPolicy.equalsIgnoreCase("controlled")) {
		return new ControlledBadDataFormatExceptionHandler();
	} else if(aDataPolicy.equalsIgnoreCase("lenient")) {
		return new LenientBadDataFormatExceptionHandler();
	}

	return null;
}
}
