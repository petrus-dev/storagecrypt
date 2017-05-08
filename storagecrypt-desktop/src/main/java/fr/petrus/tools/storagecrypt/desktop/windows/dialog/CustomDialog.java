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

package fr.petrus.tools.storagecrypt.desktop.windows.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import fr.petrus.tools.storagecrypt.desktop.TextBundle;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The abstract class used as a base for the dialogs.
 *
 * @param <D> the type of the {@code CustomDialog} subclass
 *
 * @author Pierre Sagne
 * @since 10.08.2015
 */
public abstract class CustomDialog<D extends CustomDialog<D>> extends Dialog {

    /**
     * The application window.
     */
    protected AppWindow appWindow = null;

    /**
     * A TextBundle instance.
     */
    protected TextBundle textBundle = null;

    private String title = null;
    private String positiveButtonText = null;
    private String negativeButtonText = null;

    /**
     * The button for the negative answer.
     */
    protected Button negativeButton = null;
    /**
     * The button for the positive answer.
     */
    protected Button positiveButton = null;

    private boolean positiveResult = false;


    /**
     * Creates a new {@code CustomDialog} instance.
     *
     * @param appWindow the application window
     */
    public CustomDialog(AppWindow appWindow) {
        super(appWindow);
        setShellStyle(SWT.TITLE);
        this.appWindow = appWindow;
        this.textBundle = appWindow.getTextBundle();
    }

    /**
     * Makes this {@code CustomDialog} resizable or not.
     *
     * @param resizable if true, this dialog will be resizable
     * @return this {@code CustomDialog} for further configuration
     */
    @SuppressWarnings("unchecked")
    public D setResizable(boolean resizable) {
        if (resizable) {
            setShellStyle(getShellStyle() | SWT.RESIZE);
        } else {
            setShellStyle(getShellStyle() & ~SWT.RESIZE);
        }
        return (D)this;
    }

    /**
     * Makes this {@code CustomDialog} closable or not (with a close button in the title bar).
     *
     * @param closable if true, this {@code CustomDialog} will have a close button in the title bar
     * @return this {@code CustomDialog} for further configuration
     */
    @SuppressWarnings("unchecked")
    public D setClosable(boolean closable) {
        if (closable) {
            setShellStyle(getShellStyle() | SWT.CLOSE);
        } else {
            setShellStyle(getShellStyle() & ~SWT.CLOSE);
        }
        return (D)this;
    }

    /**
     * Makes this {@code CustomDialog} modal or not.
     *
     * @param modal if true, this {@code CustomDialog} will be a modal dialog
     * @return this {@code CustomDialog} for further configuration
     */
    @SuppressWarnings("unchecked")
    public D setModal(boolean modal) {
        if (modal) {
            setShellStyle(getShellStyle() | SWT.APPLICATION_MODAL);
        } else {
            setShellStyle(getShellStyle() & ~SWT.APPLICATION_MODAL);
        }
        return (D)this;
    }

    /**
     * Makes this {@code CustomDialog} modal and always on top or not.
     *
     * @param onTopModal if true, this {@code CustomDialog} will be a modal dialog, displayed on top
     *                   of other windows
     * @return this {@code CustomDialog} for further configuration
     */
    @SuppressWarnings("unchecked")
    public D setOnTopModal(boolean onTopModal) {
        if (onTopModal) {
            setShellStyle(getShellStyle() | SWT.APPLICATION_MODAL | SWT.ON_TOP);
        } else {
            setShellStyle(getShellStyle() & ~(SWT.APPLICATION_MODAL | SWT.ON_TOP));
        }
        return (D)this;
    }

    /**
     * Sets the text to display in the title bar of this {@code CustomDialog}.
     *
     * @param title the text to display in the title bar of this {@code CustomDialog}
     * @return this {@code CustomDialog} for further configuration
     */
    @SuppressWarnings("unchecked")
    public D setTitle(String title) {
        this.title = title;
        return (D) this;
    }

    /**
     * Setups a button for the positive answer, displaying the given {@code positiveButtonText}.
     *
     * <p>If this method is not called before {@link CustomDialog#open}, there will be no button for
     * the positive answer.
     *
     * @param positiveButtonText the text to display in the positive answer button
     * @return this {@code CustomDialog} for further configuration
     */
    @SuppressWarnings("unchecked")
    public D setPositiveButtonText(String positiveButtonText) {
        this.positiveButtonText = positiveButtonText;
        return (D) this;
    }

    /**
     * Setups a button for the negative answer, displaying the given {@code negativeButtonText}.
     *
     * <p>If this method is not called before {@link CustomDialog#open}, there will be no button for
     * the negative answer.
     *
     * @param negativeButtonText the text to display in the negative answer button
     * @return this {@code CustomDialog} for further configuration
     */
    @SuppressWarnings("unchecked")
    public D setNegativeButtonText(String negativeButtonText) {
        this.negativeButtonText = negativeButtonText;
        return (D) this;
    }

    /**
     * Returns whether the positive answer button was clicked.
     *
     * @return true if the positive answer button was clicked
     */
    public boolean isResultPositive() {
        return positiveResult;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(title);
    }

    @Override
    protected Control createContents(Composite parent) {
        parent.setLayout(new FillLayout());
        Composite contentsComposite = new Composite(parent, SWT.NONE);
        applyGridLayout(contentsComposite).numColumns(2).columnsEqualWidth(true);

        Composite customContents = new Composite(contentsComposite, SWT.NULL);
        applyGridData(customContents).horizontalSpan(2).withFill();

        if (null!=positiveButtonText || null!=negativeButtonText) {
            if (null != negativeButtonText) {
                negativeButton = new Button(contentsComposite, SWT.PUSH);
                negativeButton.setText(negativeButtonText);
                applyGridData(negativeButton).withHorizontalFill();
                negativeButton.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event event) {
                        returnResult(false);
                    }
                });
            } else {
                Label emptyLabel = new Label(contentsComposite, SWT.NULL);
                applyGridData(emptyLabel).withHorizontalFill();
            }

            if (null != positiveButtonText) {
                positiveButton = new Button(contentsComposite, SWT.PUSH);
                positiveButton.setText(positiveButtonText);
                applyGridData(positiveButton).withHorizontalFill();
                positiveButton.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event event) {
                        returnResult(true);
                    }
                });
                checkInputValidity();
            }
        }

        createDialogContents(customContents);

        getShell().layout();

        return contentsComposite;
    }

    /**
     * Checks the dialog state when the RETURN key is pressed when one of the given {@code widgets}
     * is active, and act as if the positive answer button was clicked if the state is valid.
     *
     * @param widgets the widgets which can trigger the positive answer with the RETURN key
     */
    protected void validateOnReturnPressed(Control... widgets) {
        TraverseListener traverseListener = new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent event) {
                if (event.detail == SWT.TRAVERSE_RETURN) {
                    if (isInputValid()) {
                        returnResult(true);
                    }
                }
            }
        };
        if (null!=widgets) {
            for (Control widget: widgets) {
                widget.addTraverseListener(traverseListener);
            }
        }
    }

    /**
     * Checks the validity of the user input.
     */
    protected void checkInputValidity() {
        if (null!=positiveButton) {
            positiveButton.setEnabled(isInputValid());
        }
    }

    /**
     * Sets the result of this {@code CustomDialog} as positive or negative and closes it.
     *
     * @param result if true, the result of this {@code CustomDialog} will be considered as positive
     */
    protected void returnResult(boolean result) {
        this.positiveResult = result;
        close();
    }

    /**
     * Creates this dialog graphic contents.
     *
     * <p>This method must be implemented by the subclasses of {@code CustomDialog}
     *
     * @param parent the {@code Composite} where the contents will be created
     */
    protected abstract void createDialogContents(Composite parent);

    /**
     * Checks the input of this {@code CustomDialog} to determine if it is valid.
     *
     * <p>This method must be implemented by the subclasses of {@code CustomDialog}
     *
     * @return true if the input of this {@code CustomDialog} is valid
     */
    protected boolean isInputValid() {
        return true;
    }
}
