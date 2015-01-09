/*  Copyright (C) 2014 Reinventing Geospatial, Inc
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>,
 *  or write to the Free Software Foundation, Inc., 59 Temple Place -
 *  Suite 330, Boston, MA 02111-1307, USA.
 */

package com.rgi.erdc;

import javax.swing.SwingUtilities;

import com.rgi.erdc.ApplicationContext.Window;

public class GeoSuite {
	private ApplicationContext context;
	private GeoSuite() {
		SwingUtilities.invokeLater(() -> {
			this.context = new ApplicationContext();

			this.context.addWindow(Window.MAIN, new MainWindow(this.context));
			this.context.addWindow(Window.SETTINGS, new SettingsWindow(this.context));
			this.context.addWindow(Window.FILECHOOSER, new FileChooserWindow(this.context));
			this.context.addWindow(Window.PROGRESS, new ProgressWindow(this.context));
			this.context.addWindow(Window.ERROR, new ErrorWindow(this.context));
			this.context.addWindow(Window.DONE, new DoneWindow(this.context));

			this.context.go();
		});
	}

	private static void runHeadless(String[] args) {
		// TODO
	}

	public static void main(String[] args) {
		if (args != null && args.length > 0) {
			GeoSuite.runHeadless(args);
		} else {
			new GeoSuite();
		}
	}

}