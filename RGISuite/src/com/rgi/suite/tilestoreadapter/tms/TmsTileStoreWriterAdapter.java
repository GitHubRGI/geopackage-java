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
package com.rgi.suite.tilestoreadapter.tms;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import javax.activation.MimeType;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.rgi.common.tile.store.TileStoreException;
import com.rgi.common.tile.store.TileStoreReader;
import com.rgi.common.tile.store.TileStoreWriter;
import com.rgi.common.tile.store.tms.TmsWriter;
import com.rgi.common.util.FileUtility;
import com.rgi.suite.Settings;
import com.rgi.suite.tilestoreadapter.ImageFormatTileStoreAdapter;

public class TmsTileStoreWriterAdapter extends ImageFormatTileStoreAdapter
{
    private static final String TmsOutputLocationSettingName = "ui.tms.outputLocation";
    private static final String DefaultTmsOutputLocation     = System.getProperty("user.home");

    private final JTextField directory         = new JTextField();
    private final JButton    directorySelector = new JButton("\u2026");

    private final Collection<Collection<JComponent>> writerParameterControls = Arrays.asList(Arrays.asList(new JLabel("Image format:"),        this.imageFormat),
                                                                                             Arrays.asList(new JLabel("Compression type:"),    this.imageCompressionType),
                                                                                             Arrays.asList(new JLabel("Compression quality:"), this.compressionQuality),
                                                                                             Arrays.asList(new JLabel("Directory:"),           this.directory, this.directorySelector));

    public TmsTileStoreWriterAdapter(final Settings settings)
    {
        super(settings);

        // TODO save values of controls to settings

        this.directorySelector.addActionListener(e -> { final String startDirectory = this.settings.get(TmsOutputLocationSettingName, DefaultTmsOutputLocation);

                                                     final JFileChooser fileChooser = new JFileChooser(new File(startDirectory));

                                                     fileChooser.setMultiSelectionEnabled(false);
                                                     fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                                                     final int option = fileChooser.showOpenDialog(null);

                                                     if(option == JFileChooser.APPROVE_OPTION)
                                                     {
                                                         final File file = fileChooser.getSelectedFile();

                                                         this.settings.set(TmsOutputLocationSettingName, file.getParent());
                                                         this.settings.save();

                                                         this.directory.setText(String.format("%s%c%s%c",
                                                                                              fileChooser.getSelectedFile().getPath(),
                                                                                              File.separatorChar,
                                                                                              new File(this.directory.getText()).getName(),
                                                                                              File.separatorChar));
                                                     }
                                                   });
    }

    @Override
    public String toString()
    {
        return "TMS";
    }

    @Override
    protected MimeType getInitialImageFormat()
    {
        return TmsWriter.SupportedImageFormats
                        .stream()
                        .filter(mimeType -> mimeType.toString()
                                                    .toLowerCase()
                                                    .equals("image/png"))
                        .findFirst()
                        .get();
    }

    @Override
    protected Collection<MimeType> getSupportedImageFormats()
    {
        return TmsWriter.SupportedImageFormats;
    }

    @Override
    public void hint(final File inputFile) throws TileStoreException
    {
        if(!inputFile.getName().isEmpty())
        {
            final String directoryName = String.format("%s%c%s",
                                                       this.settings.get(TmsOutputLocationSettingName, DefaultTmsOutputLocation),
                                                       File.separatorChar,
                                                       FileUtility.nameWithoutExtension(inputFile));

            this.directory.setText(FileUtility.appendForUnique(directoryName) + File.separatorChar);
        }
    }

    @Override
    public Collection<Collection<JComponent>> getWriterParameterControls()
    {
        return this.writerParameterControls;
    }

    @Override
    public TileStoreWriter getTileStoreWriter(final TileStoreReader tileStoreReader) throws TileStoreException
    {
        final MimeType mimeType = (MimeType)this.imageFormat.getSelectedItem();

        return new TmsWriter(tileStoreReader.getCoordinateReferenceSystem(),
                             new File(this.directory.getText()).toPath(),
                             mimeType,
                             this.getImageWriteParameter());
    }
}