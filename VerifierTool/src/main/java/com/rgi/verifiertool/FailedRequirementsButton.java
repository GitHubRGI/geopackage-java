/* The MIT License (MIT)
 *
 * Copyright (c) 2015 Reinventing Geospatial, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.rgi.verifiertool;

import java.util.Collection;

import javafx.scene.control.Button;

import com.rgi.geopackage.verification.VerificationIssue;

/**
 * @author Jenifer Cochran
 *
 */
public class FailedRequirementsButton extends Button
{
    private Collection<VerificationIssue> failedRequirements;
    /**
     * @param text message on the button
     * @param failedRequirements the requirements that the GeoPackage Failed
     */
    public FailedRequirementsButton(final Collection<VerificationIssue> failedRequirements)
    {
        super("show more");
        this.failedRequirements = failedRequirements;
    }

    /**
     * @return the failed requirements associated with this button
     */
    public Collection<VerificationIssue> getFailedRequirements()
    {
        return this.failedRequirements;
    }

    public void setRequirements(final Collection<VerificationIssue> failedRequirements)
    {
        this.failedRequirements = failedRequirements;
    }

}
