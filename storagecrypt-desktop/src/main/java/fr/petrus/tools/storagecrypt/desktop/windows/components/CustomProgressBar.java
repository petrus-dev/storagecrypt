/*
 *  Copyright Pierre Sagne (12 december 2014)
 *
 * petrus.dev.fr@gmail.com
 *
 * This software is a computer program whose purpose is to encrypt and
 * synchronize files on the cloud.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *
 */

package fr.petrus.tools.storagecrypt.desktop.windows.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import java.util.Locale;

import fr.petrus.lib.core.Progress;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * A custom SWT ProgressBar, with a percentage of progression label and a raw progression label.
 *
 * <p>The progress can be indeterminate, which means the maximum value is not known. In this case,
 * the percentage of progression label is not shown
 *
 * @author Pierre Sagne
 * @since 29.04.2016
 */
public class CustomProgressBar extends Composite {
    private ProgressBar progressBar;
    private Label percentLabel;
    private Label progressLabel;

    private boolean indeterminate;

    /**
     * Creates a new {@code CustomProgressBar} instance.
     * @param parent the parent of this {@code CustomProgressBar}
     * @param progress the default progress values (current and max)
     */
    public CustomProgressBar(Composite parent, Progress progress) {
        super(parent, SWT.NULL);

        applyGridLayout(this).numColumns(2);

        indeterminate = progress.isIndeterminate();

        int progressBarFlags = SWT.HORIZONTAL | SWT.SMOOTH;
        if (indeterminate) {
            progressBarFlags |= SWT.INDETERMINATE;
        }
        progressBar = new ProgressBar(this, progressBarFlags);
        applyGridData(progressBar).horizontalSpan(2).withHorizontalFill();
        if (!indeterminate) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(progress.getMax());
            progressBar.setSelection(progress.getProgress());
        }

        percentLabel = new Label(this, SWT.NULL);
        applyGridData(percentLabel).horizontalAlignment(SWT.BEGINNING).grabExcessHorizontalSpace(true);

        progressLabel = new Label(this, SWT.NULL);
        applyGridData(progressLabel).horizontalAlignment(SWT.END).grabExcessHorizontalSpace(true);

        layout();
    }

    /**
     * Updates this {@code CustomProgressBar} with the given {@code progress} values (current and max).
     *
     * @param progress the progress values (current and max)
     */
    public void update(Progress progress) {
        if (indeterminate) {
            if (!progressLabel.isDisposed()) {
                progressLabel.setText(String.valueOf(progress.getProgress()));
                progressLabel.pack();
            }
        } else {
            if (!progressBar.isDisposed()) {
                progressBar.setMaximum(progress.getMax());
                progressBar.setSelection(progress.getProgress());
            }

            if (!percentLabel.isDisposed()) {
                if (progress.getMax() > 0) {
                    long percent = progress.getProgress() * 100L / progress.getMax();
                    percentLabel.setText(String.format(Locale.getDefault(), "%d%%", percent));
                    percentLabel.pack();
                } else {
                    percentLabel.setText("");
                }
            }

            if (!progressLabel.isDisposed()) {
                progressLabel.setText(String.format(Locale.getDefault(), "%d/%d",
                        progress.getProgress(), progress.getMax()));
                progressLabel.pack();
            }
        }
        layout();
    }
}
