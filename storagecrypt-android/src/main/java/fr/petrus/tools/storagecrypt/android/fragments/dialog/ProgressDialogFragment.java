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

package fr.petrus.tools.storagecrypt.android.fragments.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Locale;

import fr.petrus.lib.core.Progress;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.events.DismissProgressDialogEvent;
import fr.petrus.tools.storagecrypt.android.events.TaskProgressEvent;
import fr.petrus.tools.storagecrypt.android.parcelables.ProgressParcelable;

/**
 * This dialog displays the progress of a running task, optionally letting the user pause and canceling
 * it.
 *
 * @author Pierre Sagne
 * @since 11.04.2015
 */
public class ProgressDialogFragment extends CustomDialogFragment<ProgressDialogFragment.Parameters> {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "ProgressDialogFragment";

    private static final String SAVE_STATE_PROGRESSES = "save_state_progresses";

    /**
     * The class which holds the parameters to create this dialog.
     */
    public static class Parameters extends CustomDialogFragment.Parameters {
        private int dialogId = -1;
        private String title = null;
        private boolean dialogCancelable = false;
        private boolean cancelButton = false;
        private boolean pauseButton = false;
        private Progress[] progresses = null;

        /**
         * Creates a new empty {@code Parameters} instance.
         */
        public Parameters() {}

        /**
         * Sets the ID of the dialog to create.
         *
         * @param dialogId the ID of the dialog to create
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setDialogId(int dialogId) {
            this.dialogId = dialogId;
            return this;
        }

        /**
         * Sets the title of the dialog to create.
         *
         * @param title the title of the dialog to create
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets whether the dialog is cancelable by clicking outside.
         *
         * @param dialogCancelable if true, the dialog will be cancelable by clicking outside
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setDialogCancelable(boolean dialogCancelable) {
            this.dialogCancelable = dialogCancelable;
            return this;
        }

        /**
         * Sets whether the dialog has a button to cancel the associated task.
         *
         * @param cancelButton if true, the dialog will have a cancel button
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setCancelButton(boolean cancelButton) {
            this.cancelButton = cancelButton;
            return this;
        }

        /**
         * Sets whether the dialog has a button to pause the associated task.
         *
         * @param pauseButton if true, the dialog will have a pause button
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setPauseButton(boolean pauseButton) {
            this.pauseButton = pauseButton;
            return this;
        }

        /**
         * Sets the progress data for the dialog.
         *
         * @param progresses the data used to create the progress bars
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setProgresses(Progress... progresses) {
            this.progresses = progresses;
            return this;
        }

        /**
         * Returns the ID of the dialog to create.
         *
         * @return the ID of the dialog to create
         */
        public int getDialogId() {
            return dialogId;
        }

        /**
         * Sets whether the dialog is cancelable by clicking outside.
         *
         * @return true if the dialog will be cancelable by clicking outside
         */
        public boolean isDialogCancelable() {
            return dialogCancelable;
        }

        /**
         * Returns the title of the dialog to create.
         *
         * @return the title of the dialog to create
         */
        public String getTitle() {
            return title;
        }

        /**
         * Returns the number of progress channels to display in the dialog.
         *
         * @return the number of progress channels to display in the dialog
         */
        public int getNumProgresses() {
            if (null==progresses) {
                return 0;
            }
            return progresses.length;
        }

        /**
         * Returns the progress for the {@code i}th channel.
         *
         * @return the progress for the {@code i}th channel
         */
        public Progress getProgress(int i) {
            if (null==progresses || i<0 || i>=progresses.length) {
                return null;
            }
            return progresses[i];
        }

        /**
         * Returns whether the dialog should have a cancel button.
         *
         * @return true if the dialog should have a cancel button
         */
        public boolean hasCancelButton() {
            return cancelButton;
        }

        /**
         * Returns whether the dialog should have a pause button.
         *
         * @return true if the dialog should have a pause button
         */
        public boolean hasPauseButton() {
            return pauseButton;
        }
    }

    /**
     * The interface used by this dialog to communicate with the Activity.
     */
    public interface DialogListener {

        /**
         * Pauses the task associated with the given {@code dialogId}.
         *
         * @param dialogId the ID of the dialog associated to the task to pause
         */
        void onPauseTask(int dialogId);

        /**
         * Resumes the task associated with the given {@code dialogId}.
         *
         * @param dialogId the ID of the dialog associated to the task to resume
         */
        void onResumeTask(int dialogId);

        /**
         * Returns whether the task associated with the given {@code dialogId} is paused.
         *
         * @param dialogId the ID of the dialog associated to the task to check
         * @return true if the task is paused
         */
        boolean isTaskPaused(int dialogId);

        /**
         * Cancels the task associated with the given {@code dialogId}.
         *
         * @param dialogId the ID of the dialog associated to the task to cancel
         */
        void onCancelTask(int dialogId);
    }

    private DialogListener dialogListener;

    private boolean paused = false;
    private Progress[] progresses = null;

    private LinearLayout[] progressBarLayouts = new LinearLayout[2];
    private TextView[] messages = new TextView[2];
    private ProgressBar[] progressBars = new ProgressBar[2];
    private TextView[] progressBarPercentLabels = new TextView[2];
    private TextView[] progressBarProgressLabels = new TextView[2];
    private LinearLayout buttonsLayout;
    private Button cancelButton;
    private Button pauseButton;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        EventBus.getDefault().register(this);
        if (activity instanceof DialogListener) {
            dialogListener = (DialogListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement "+DialogListener.class.getName());
        }
    }

    @Override
    public void onDetach() {
        EventBus.getDefault().unregister(this);
        dialogListener = null;
        super.onDetach();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            Parcelable[] savedProgressesParcelableArray =
                    savedInstanceState.getParcelableArray(SAVE_STATE_PROGRESSES);
            if (null!=savedProgressesParcelableArray) {
                ProgressParcelable[] savedProgresses = (ProgressParcelable[]) savedProgressesParcelableArray;
                for (int i = 0; i < progresses.length; i++) {
                    progresses[i].set(savedProgresses[i].getProgress());
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ProgressParcelable[] savedProgresses = new ProgressParcelable[progresses.length];
        for (int i=0; i<progresses.length; i++) {
            savedProgresses[i] = new ProgressParcelable(progresses[i]);
        }
        outState.putParcelableArray(SAVE_STATE_PROGRESSES, savedProgresses);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_progress, null);

        progressBarLayouts[0] = (LinearLayout) view.findViewById(R.id.progress_bar_layout_1);
        progressBarLayouts[1] = (LinearLayout) view.findViewById(R.id.progress_bar_layout_2);
        messages[0] = (TextView) view.findViewById(R.id.message_1);
        messages[1] = (TextView) view.findViewById(R.id.message_2);
        progressBars[0] = (ProgressBar) view.findViewById(R.id.progress_bar_1);
        progressBars[1] = (ProgressBar) view.findViewById(R.id.progress_bar_2);
        progressBarPercentLabels[0] = (TextView) view.findViewById(R.id.progress_bar_percent_1);
        progressBarPercentLabels[1] = (TextView) view.findViewById(R.id.progress_bar_percent_2);
        progressBarProgressLabels[0] = (TextView) view.findViewById(R.id.progress_bar_progress_1);
        progressBarProgressLabels[1] = (TextView) view.findViewById(R.id.progress_bar_progress_2);

        buttonsLayout = (LinearLayout) view.findViewById(R.id.buttons_layout);
        cancelButton = (Button) view.findViewById(R.id.cancel_button);
        pauseButton = (Button) view.findViewById(R.id.pause_button);

        AlertDialog.Builder dialogBuilder = new  AlertDialog.Builder(getActivity());
        if (null!=parameters) {
            if (null != parameters.getTitle()) {
                dialogBuilder.setTitle(parameters.getTitle());
            }

            progressBarLayouts[0].setVisibility(View.GONE);
            progressBarLayouts[1].setVisibility(View.GONE);
            if (parameters.getNumProgresses()>0) {
                progresses = new Progress[parameters.getNumProgresses()];
                for (int i = 0; i < progresses.length && i < progressBars.length; i++) {
                    progresses[i] = new Progress(parameters.getProgress(i));
                    progressBarLayouts[i].setVisibility(View.VISIBLE);
                    updateProgressBar(i);
                }
            }

            if (!parameters.hasCancelButton() && !parameters.hasPauseButton()) {
                buttonsLayout.setVisibility(View.GONE);
            } else {
                if (parameters.hasCancelButton()) {
                    cancelButton.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialogListener.onCancelTask(parameters.getDialogId());
                                }
                            }
                    );
                    cancelButton.setVisibility(View.VISIBLE);
                }

                if (parameters.hasPauseButton()) {
                    paused = dialogListener.isTaskPaused(parameters.getDialogId());
                    String buttonText;
                    if (paused) {
                        buttonText = getActivity().getString(R.string.progress_dialog_fragment_resume_button_text);
                    } else {
                        buttonText = getActivity().getString(R.string.progress_dialog_fragment_pause_button_text);
                    }
                    pauseButton.setText(buttonText);
                    pauseButton.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    paused = dialogListener.isTaskPaused(parameters.getDialogId());
                                    if (paused) {
                                        dialogListener.onResumeTask(parameters.getDialogId());
                                        pauseButton.setText(getActivity().getString(R.string.progress_dialog_fragment_pause_button_text));
                                    } else {
                                        dialogListener.onPauseTask(parameters.getDialogId());
                                        pauseButton.setText(getActivity().getString(R.string.progress_dialog_fragment_resume_button_text));
                                    }
                                    paused = !paused;
                                }
                            }
                    );
                }
            }
            dialogBuilder.setView(view);
        }

        Dialog dialog = dialogBuilder.create();
        if (null!=parameters) {
            setCancelable(parameters.isDialogCancelable());
        }
        return dialog;
    }

    /**
     * The {@link EventBus} callback which receives the {@code TaskProgressEvent}s posted by tasks.
     *
     * <p>This method checks if the given {@code TaskProgressEvent} has the same dialog ID as this
     * dialog, and updates its progress if they match.
     *
     * @param event the {@code TaskProgressEvent} posted by a task
     */
    @Subscribe(sticky=true)
    public void onEvent(final TaskProgressEvent event) {
        if (null!=parameters) {
            if (event.getDialogId() == parameters.getDialogId()) {
                EventBus.getDefault().removeStickyEvent(event);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (ProgressDialogFragment.this) {
                            if (null != progresses) {
                                for (int i = 0; i < progresses.length && i < progressBars.length; i++) {
                                    Progress progress = event.getProgress(i);
                                    if (null != progress) {
                                        progresses[i].set(progress);
                                        updateProgressBar(i);
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * The {@link EventBus} callback which receives the {@code DismissProgressDialogEvent}s posted by tasks.
     *
     * <p>This method checks if the given {@code DismissProgressDialogEvent} has the same dialog ID
     * as this dialog, and closes the dialog if they match.
     *
     * @param event the {@code DismissProgressDialogEvent} posted by a task
     */
    @Subscribe(sticky=true)
    public void onEvent(final DismissProgressDialogEvent event) {
        if (null!=parameters) {
            if (event.getDialogId() == parameters.getDialogId()) {
                EventBus.getDefault().removeStickyEvent(event);
                dismiss();
            }
        }
    }

    private void updateProgressBar(int i) {
        if (null != progressBars[i] && null!=progressBarPercentLabels[i] && null!=progressBarProgressLabels[i]) {
            Progress progress = progresses[i];
            if (null!=progress.getMessage()) {
                messages[i].setText(progress.getMessage());
                messages[i].setVisibility(View.VISIBLE);
            } else {
                messages[i].setVisibility(View.GONE);
            }
            progressBars[i].setIndeterminate(progress.isIndeterminate());
            progressBars[i].setMax(progress.getMax());
            progressBars[i].setProgress(progress.getProgress());
            if (progress.isIndeterminate()) {
                progressBarPercentLabels[i].setVisibility(View.GONE);
                if (progress.hasCounter()) {
                    progressBarProgressLabels[i].setVisibility(View.VISIBLE);
                    progressBarProgressLabels[i].setText(String.valueOf(progress.getProgress()));
                } else {
                    progressBarProgressLabels[i].setVisibility(View.GONE);
                }
            } else {
                if (progress.hasCounter()) {
                    progressBarPercentLabels[i].setVisibility(View.VISIBLE);
                    long percent;
                    if (progress.getMax() > 0) {
                        percent = progress.getProgress() * 100L / progress.getMax();
                    } else {
                        percent = 0;
                    }
                    progressBarPercentLabels[i].setText(String.format(Locale.getDefault(),
                            "%d%%", percent));
                    progressBarPercentLabels[i].setVisibility(View.VISIBLE);

                    progressBarProgressLabels[i].setVisibility(View.VISIBLE);
                    progressBarProgressLabels[i].setText(String.format(Locale.getDefault(),
                            "%d/%d", progress.getProgress(), progress.getMax()));
                } else {
                    progressBarPercentLabels[i].setVisibility(View.GONE);
                    progressBarProgressLabels[i].setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * Creates a {@code ProgressDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code ProgressDialogFragment} to
     * @param parameters      the parameters to create the {@code ProgressDialogFragment}
     * @return the newly created {@code ProgressDialogFragment}
     */
    public static ProgressDialogFragment showFragment(FragmentManager fragmentManager, Parameters parameters) {
        ProgressDialogFragment fragment = new ProgressDialogFragment();
        fragment.setParameters(parameters);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}