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

package com.rgi.geopackage.verification;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Luke Lambert
 *
 */
@SuppressWarnings("serial")
public class ConformanceException extends Exception
{
    /**
     * @param conformanceIssues a collection of all the failed requirements of the GeoPackage that did not pass
     */
    public ConformanceException(final Collection<VerificationIssue> conformanceIssues)
    {
        this.conformanceIssues = conformanceIssues;
    }

    @Override
    public String getMessage()
    {
        return this.toString();
    }

    @Override
    public String toString()
    {
        return String.format("GeoPackage failed to meet the following requirements:\n%s",
                             this.conformanceIssues.stream()
                                                   .sorted((requirement1, requirement2) -> Integer.compare(requirement1.getRequirement().number(), requirement2.getRequirement().number()))
                                                   .map(failedRequirement -> String.format("* (%s) Requirement %d: \"%s\"\n%s",
                                                                                     failedRequirement.getRequirement().severity(),
                                                                                     failedRequirement.getRequirement().number(),
                                                                                     failedRequirement.getRequirement().text(),
                                                                                     failedRequirement.getReason()))
                                                   .collect(Collectors.joining("\n\n")));
    }

    private final Collection<VerificationIssue> conformanceIssues;
}