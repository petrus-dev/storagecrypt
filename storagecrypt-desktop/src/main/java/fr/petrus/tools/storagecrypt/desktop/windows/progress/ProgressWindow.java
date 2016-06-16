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

package fr.petrus.tools.storagecrypt.desktop.windows.progress;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.petrus.lib.core.Progress;
import fr.petrus.lib.core.platform.TaskCreationException;
import fr.petrus.lib.core.tasks.Task;
import fr.petrus.lib.core.platform.AppContext;
import fr.petrus.tools.storagecrypt.desktop.TextBundle;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;
import fr.petrus.tools.storagecrypt.desktop.windows.components.CustomProgressBar;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The abstract class used as a base for the progress windows.
 *
 * @param <E> the type of the {@link ProgressWindow.ProgressEvent} subclass handled by this progress
 *           window
 * @param <T> the type of the {@link Task} subclass handled by this progress window
 * @author Pierre Sagne
 * @since 28.07.2015
 */
public abstract class ProgressWindow<E extends ProgressWindow.ProgressEvent, T extends Task>
        extends Window {
    private static Logger LOG = LoggerFactory.getLogger(ProgressWindow.class);

    /**
     * The type Progress event.
     */
    public static class ProgressEvent {
        /**
         * The Progresses.
         */
        public Progress[] progresses = null;

        /**
         * Instantiates a new Progress event.
         *
         * @param progresses the progresses
         */
        public ProgressEvent(Progress... progresses) {
            this.progresses = progresses;
        }

        /**
         * Set.
         *
         * @param progressEvent the progress event
         */
        public void set(ProgressEvent progressEvent) {
            if (null!=progressEvent) {
                if (null != progresses && null != progressEvent.progresses) {
                    for (int i = 0; i < progresses.length && i < progressEvent.progresses.length; i++) {
                        progresses[i].set(progressEvent.progresses[i]);
                    }
                }
            }
        }
    }

    /**
     * The application window.
     */
    protected AppWindow appWindow;

    /**
     * The {@code AppContext} which provides the dependencies of this class.
     */
    protected AppContext appContext;

    /**
     * The {@code TextBundle} used to retrieve the i18n texts.
     */
    protected TextBundle textBundle;

    private String title;
    private boolean withCancelButton;
    private boolean withPauseButton;
    private Progress[] progresses;
    private CustomProgressBar[] customProgressBars;

    /**
     * The {@code ProgressEvent} handled by this progress window.
     */
    protected E progressEvent;

    private volatile boolean updatingProgress;

    /**
     * The class of the {@code Task} handled by this window.
     */
    protected Class<T> taskClass;

    /**
     * The bounds of this window, to open it at the same place the next time.
     */
    protected Rectangle bounds;

    /**
     * Creates a new {@code ProgressWindow} instance with a cancel button and a pause button.
     *
     * @param appWindow     the application window
     * @param taskClass     the class of the {@code Task} handled by this progress window
     * @param title         the text to display in the title bar of this progress window
     * @param progressEvent an instance of the progress event, to initialize this progress window
     * @param progresses    the progresses, used to initialize this progress window
     */
    protected ProgressWindow(AppWindow appWindow, Class<T> taskClass,
                             String title, E progressEvent, Progress... progresses) {
        this(appWindow, taskClass, title, progressEvent, true, true, progresses);
    }

    /**
     * Creates a new {@code ProgressWindow} instance.
     *
     * @param appWindow        the application window
     * @param taskClass        the class of the {@code Task} handled by this progress window
     * @param title            the text to display in the title bar of this progress window
     * @param progressEvent    an instance of the progress event, to initialize this progress window
     * @param withCancelButton if true, display a cancel button
     * @param withPauseButton  if true, display a pause button
     * @param progresses       the progresses, used to initialize this progress window
     */
    protected ProgressWindow(AppWindow appWindow, final Class<T> taskClass,
                             String title, E progressEvent,
                             boolean withCancelButton, boolean withPauseButton,
                             Progress... progresses) {
        super(appWindow);
        setShellStyle(SWT.TITLE | SWT.RESIZE);
        this.appWindow = appWindow;
        this.appContext = appWindow.getAppContext();
        this.textBundle = appWindow.getTextBundle();
        this.taskClass = taskClass;
        this.title = title;
        this.progressEvent = progressEvent;
        this.withCancelButton = withCancelButton;
        this.withPauseButton = withPauseButton;
        this.progresses = progresses;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(title);
    }

    @Override
    protected Point getInitialSize() {
        if (null!=bounds) {
            return new Point(bounds.width, bounds.height);
        } else {
            return super.getInitialSize();
        }
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        if (null!=bounds) {
            return new Point(bounds.x, bounds.y);
        } else {
            return super.getInitialLocation(initialSize);
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        parent.setLayout(new FillLayout());
        Composite contentsComposite = new Composite(parent, SWT.NONE);
        applyGridLayout(contentsComposite).numColumns(2).columnsEqualWidth(true);

        Composite progressContents = new Composite(contentsComposite, SWT.NULL);
        applyGridData(progressContents).withHorizontalFill().horizontalSpan(2);

        if (null==progresses) {
            customProgressBars = null;
        } else {
            customProgressBars = new CustomProgressBar[progresses.length];
            for (int i=0; i<progresses.length; i++) {
                customProgressBars[i] = new CustomProgressBar(contentsComposite, progresses[i]);
                applyGridData(customProgressBars[i]).withHorizontalFill().horizontalSpan(2);
            }
        }

        if (withCancelButton || withPauseButton) {
            if (withCancelButton) {
                Button cancelButton = new Button(contentsComposite, SWT.PUSH);
                cancelButton.setText(textBundle.getString("progress_dialog_cancel_button_text"));
                applyGridData(cancelButton).withFill().verticalAlignment(SWT.BOTTOM);
                cancelButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent selectionEvent) {
                        try {
                            appContext.getTask(taskClass).cancel();
                        } catch (TaskCreationException e) {
                            LOG.error("Failed to get task {}", e.getTaskClass().getCanonicalName(), e);
                        }
                    }
                });
            } else {
                Label placeholderLabel = new Label(contentsComposite, SWT.NULL);
                applyGridData(placeholderLabel).withFill().verticalAlignment(SWT.BOTTOM);
            }

            if (withPauseButton) {
                final Button pauseButton = new Button(contentsComposite, SWT.PUSH);
                applyGridData(pauseButton).withFill().verticalAlignment(SWT.BOTTOM);
                pauseButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent selectionEvent) {
                        try {
                            T task = appContext.getTask(taskClass);
                            if (task.isPaused()) {
                                task.resume();
                            } else {
                                task.pause();
                            }
                            updatePauseButtonText(pauseButton);
                        } catch (TaskCreationException e) {
                            LOG.error("Failed to get task {}", e.getTaskClass().getCanonicalName(), e);
                        }
                    }
                });
                updatePauseButtonText(pauseButton);
            }
        }

        createProgressContents(progressContents);

        updatingProgress = false;

        return contentsComposite;
    }

    private void updatePauseButtonText(Button pauseButton) {
        try {
            if (appContext.getTask(taskClass).isPaused()) {
                pauseButton.setText(textBundle.getString("progress_dialog_resume_button_text"));
            } else {
                pauseButton.setText(textBundle.getString("progress_dialog_pause_button_text"));
            }
        } catch (TaskCreationException e) {
            LOG.error("Failed to get task {}", e.getTaskClass().getCanonicalName(), e);
        }
    }

    /**
     * Updates this window with the given {@code progressEvent}.
     *
     * @param progressEvent the progress event to update this window
     */
    protected abstract void updateProgress(E progressEvent);

    /**
     * Updates this window with the given {@code progressEvent} for the given {@code i} channel.
     *
     * @param i        the channel to update
     * @param progress the progress to update the channel with
     */
    protected void updateProgress(final int i, final Progress progress) {
        if (null != customProgressBars && i >= 0 && i < customProgressBars.length) {
            customProgressBars[i].update(progress);
        }
    }

    /**
     * Creates this progress dialog graphic contents.
     *
     * <p>This method must be implemented by the subclasses of {@code ProgressWindow}
     *
     * @param parent the {@code Composite} where the contents will be created
     */
    protected abstract void createProgressContents(Composite parent);

    /**
     * Updates this window with the given {@code progressEvent}.
     *
     * @param progressEvent the progress event to update this window
     */
    public void update(final E progressEvent) {
        if (!updatingProgress) {
            updatingProgress = true;
            this.progressEvent.set(progressEvent);
            if (null!=getShell() && !getShell().isDisposed()) {
                getShell().getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        updateProgress(progressEvent);
                        for (int i = 0; i < progressEvent.progresses.length; i++) {
                            updateProgress(i, progressEvent.progresses[i]);
                        }
                        updatingProgress = false;
                    }
                });
            }
        }
    }

    /**
     * Returns whether this window is closed.
     *
     * @return true if this window has been closed
     */
    public boolean isClosed() {
        return null == getShell() || getShell().isDisposed();
    }

    @Override
    public boolean close() {
        bounds = getShell().getBounds();
        return super.close();
    }

    /**
     * Closes this window.
     *
     * @param async if true, the close method will be called asynchronously in the main thread
     */
    public void close(boolean async) {
        if (async) {
            appWindow.asyncExec(new Runnable() {
                @Override
                public void run() {
                    close();
                }
            });
        } else {
            close();
        }
    }
}
