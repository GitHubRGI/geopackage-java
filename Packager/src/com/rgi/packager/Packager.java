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

package com.rgi.packager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.rgi.common.task.MonitorableTask;
import com.rgi.common.task.TaskMonitor;
import com.rgi.common.tile.store.TileHandle;
import com.rgi.common.tile.store.TileStoreException;
import com.rgi.common.tile.store.TileStoreReader;
import com.rgi.common.tile.store.TileStoreWriter;

/**
 * Package tiles from a tile store into a GeoPackage or append to an existing GeoPackage.
 *
 * @author Steven D. Lander
 * @author Luke D. Lambert
 *
 */
public class Packager implements MonitorableTask, TaskMonitor
{
    private final ExecutorService executor  = Executors.newSingleThreadExecutor();

    private final int jobTotal  = 0;
    private int jobCount  = 0;
    private int completed = 0;

    private final TileStoreReader tileStoreReader;
    private final TileStoreWriter tileStoreWriter;

    /**
     * Constructor
     *
     * @param tileStoreReader
     *             Input tile store
     * @param tileStoreWriter
     *             Destination tile store
     */
    public Packager(final TileStoreReader tileStoreReader, final TileStoreWriter tileStoreWriter)
    {
        this.tileStoreReader = tileStoreReader;
        this.tileStoreWriter = tileStoreWriter;
    }

    private final Set<TaskMonitor> monitors = new HashSet<>();

    /**
     * Starts the packaging job
     */
    public void execute()
    {
        //final Thread jobWaiter = new Thread(new JobWaiter(this.executor.submit(this.createPackageJob())));
        //jobWaiter.setDaemon(true);
        //jobWaiter.start();

        // TODO *temporary* make this run on the main thread
        this.createPackageJob().run();
    }

    private Runnable createPackageJob()
    {
        return () -> { int tileCount = 0;

                       try
                       {
                           for(final TileHandle tileHandle : (Iterable<TileHandle>)this.tileStoreReader.stream()::iterator)
                           {
                               try
                               {
                                   this.tileStoreWriter.addTile(tileHandle.getCrsCoordinate(this.tileStoreWriter.getTileOrigin()),
                                                                tileHandle.getZoomLevel(),
                                                                tileHandle.getImage());
                                   ++tileCount;
                               }
                               catch(final TileStoreException | IllegalArgumentException ex)
                               {
                                   // TODO: report this somewhere else?
                                   System.err.printf("Tile z: %d, x: %d, y: %d failed to get copied into the package: %s\n",
                                                     tileHandle.getZoomLevel(),
                                                     tileHandle.getColumn(),
                                                     tileHandle.getRow(),
                                                     ex.getMessage());
                               }
                           }
                       }
                       catch(final TileStoreException ex)
                       {
                           // TODO Auto-generated catch block
                           ex.printStackTrace();
                       }

                       try
                       {
                           System.out.printf("Packaged %d of %d tiles.\n", tileCount, this.tileStoreReader.countTiles());
                       }
                       catch(final TileStoreException ex)
                       {
                           System.out.printf("Copied %d tiles.\n", tileCount);
                       }
                       finally
                       {
                           this.fireFinished();
                       }
                     };
    }

    @Override
    public void addMonitor(final TaskMonitor monitor)
    {
        this.monitors.add(monitor);
    }

    @Override
    public void requestCancel()
    {
        this.executor.shutdownNow();
        try
        {
            this.executor.awaitTermination(60, TimeUnit.SECONDS);
        }
        catch(final InterruptedException ie)
        {
            this.fireCancelled();
        }
    }

    private void fireCancelled()
    {
        for(final TaskMonitor monitor : this.monitors)
        {
            monitor.cancelled();
        }
    }

    private void fireProgressUpdate()
    {
        for(final TaskMonitor monitor : this.monitors)
        {
            monitor.setProgress(this.completed);
        }
    }

    @SuppressWarnings("unused")
    private void fireError(final Exception e)
    {
        for(final TaskMonitor monitor : this.monitors)
        {
            monitor.setError(e);
        }
    }

    private void fireFinished()
    {
        for(final TaskMonitor monitor : this.monitors)
        {
            monitor.finished();
        }
    }

//    private class JobWaiter implements Runnable
//    {
//        private final Future<?> job;
//
//        public JobWaiter(final Future<?> job)
//        {
//            ++Packager.this.jobTotal;
//            this.job = job;
//        }
//
//        @Override
//        public void run()
//        {
//            try
//            {
//                this.job.get();
//            }
//            catch(final InterruptedException ie)
//            {
//                // unlikely, but we still need to handle it
//                System.err.println("Packaging job was interrupted.");
//                ie.printStackTrace();
//                Packager.this.fireError(ie);
//            }
//            catch(final ExecutionException ee)
//            {
//                System.err.println("Packaging job failed with exception: " + ee.getMessage());
//                ee.printStackTrace();
//                Packager.this.fireError(ee);
//            }
//            catch(final CancellationException ce)
//            {
//                System.err.println("Packaging job was cancelled.");
//                ce.printStackTrace();
//                Packager.this.fireError(ce);
//            }
//        }
//    }

    @Override
    public void setMaximum(final int max)
    {
        // updates the progress bar to exit indeterminate mode
        for(final TaskMonitor monitor : this.monitors)
        {
            monitor.setMaximum(100);
        }
    }

    @Override
    public void setProgress(final int value)
    {
        System.out.println("progress updated: " + value);
        // when called by a tilejob, reports a number from 0-100.
        final double perJob = 100.0 / this.jobTotal;
        this.completed = (int)((this.jobCount * perJob) + ((value / 100.0) * perJob));
        this.fireProgressUpdate();
    }

    @Override
    public void cancelled()
    {
        // not used
    }

    @Override
    public void finished()
    {
        ++this.jobCount;
        if(this.jobCount == this.jobTotal)
        {
            this.fireFinished();
        }
        else
        {
            this.setProgress(0);
        }
    }

    @Override
    public void setError(final Exception e)
    {
        // this shouldn't be used
    }
}
