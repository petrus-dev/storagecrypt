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

package fr.petrus.tools.storagecrypt.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.tools.storagecrypt.R;

/**
 * The {@code ArrayAdapter} for encrypted documents.
 *
 * @author Pierre Sagne
 * @since 16.01.2015
 */
public class EncryptedDocumentArrayAdapter extends ArrayAdapter<EncryptedDocument> {

    /**
     * Returns the document at the given {@code position} of the given {@code listView}.
     *
     * @param listView the list where the documents are displayed
     * @param position the position of the document in the list
     * @return the document at the given {@code position} of the given {@code listView}
     */
    public static EncryptedDocument getListItemAt(ListView listView, int position) {
        ListAdapter adapter = listView.getAdapter();
        if (null!=adapter && adapter instanceof EncryptedDocumentArrayAdapter) {
            return ((EncryptedDocumentArrayAdapter) adapter).getItem(position);
        }
        return null;
    }

    /**
     * Returns a list containing all the documents displayed in the given {@code listView}.
     *
     * @param listView the list where the documents are displayed
     * @return the list containing all the documents displayed in the given {@code listView}
     */
    public static List<EncryptedDocument> getListItems(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (null!=adapter && adapter instanceof EncryptedDocumentArrayAdapter) {
            return ((EncryptedDocumentArrayAdapter) adapter).documents;
        }
        return null;
    }

    private Context context;
    private int layoutResourceId;
    private List<EncryptedDocument> documents;

    /**
     * Creates a new {@code EncryptedDocumentArrayAdapter} instance.
     *
     * @param context    the Android context
     * @param documents  the list of documents managed by this adapter
     */
    public EncryptedDocumentArrayAdapter(Context context, List<EncryptedDocument> documents) {
        this(context, R.layout.row_documentmapping, documents);
    }

    /**
     * Creates a new {@code EncryptedDocumentArrayAdapter} instance.
     *
     * @param context          the Android context
     * @param layoutResourceId the resource id of the layout used to display a row of the list
     * @param documents        the list of documents managed by this adapter
     */
    public EncryptedDocumentArrayAdapter(Context context, int layoutResourceId,
                                         List<EncryptedDocument> documents) {
        super(context, layoutResourceId, documents);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.documents = documents;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if(view==null) {
            // inflate the layout
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(layoutResourceId, parent, false);
        }

        EncryptedDocument encryptedDocument = documents.get(position);

        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        ImageView changesSyncIcon = (ImageView) view.findViewById(R.id.changes_sync_icon);
        ImageView downloadIcon = (ImageView) view.findViewById(R.id.download_icon);
        ImageView uploadIcon = (ImageView) view.findViewById(R.id.upload_icon);
        ImageView deleteIcon = (ImageView) view.findViewById(R.id.delete_icon);
        TextView textViewName = (TextView) view.findViewById(R.id.name_text);
        TextView textViewLeft = (TextView) view.findViewById(R.id.left_text);
        TextView textViewRight = (TextView) view.findViewById(R.id.right_text);

        if (encryptedDocument.isRoot()) {
            textViewName.setText(encryptedDocument.storageTypeText());
            icon.setImageResource(R.drawable.ic_launcher);
            textViewLeft.setVisibility(View.VISIBLE);
            if (encryptedDocument.isUnsynchronizedRoot()) {
                textViewLeft.setText("");
            } else {
                textViewLeft.setText(encryptedDocument.getBackStorageAccount().getAccountName());
            }
            textViewRight.setVisibility(View.VISIBLE);
            textViewRight.setText(encryptedDocument.getBackStorageQuotaText());
            if (encryptedDocument.isUnsynchronizedRoot()) {
                changesSyncIcon.setVisibility(View.GONE);
            } else {
                if (null== encryptedDocument.getBackStorageAccount().getChangesSyncState()) {
                    changesSyncIcon.setVisibility(View.GONE);
                } else {
                    switch (encryptedDocument.getBackStorageAccount().getChangesSyncState()) {
                        case Done:
                            changesSyncIcon.setVisibility(View.GONE);
                            break;
                        case Planned:
                            changesSyncIcon.setVisibility(View.VISIBLE);
                            changesSyncIcon.setImageResource(R.drawable.ic_sync_violet);
                            break;
                        case Running:
                            changesSyncIcon.setVisibility(View.VISIBLE);
                            changesSyncIcon.setImageResource(R.drawable.ic_sync_green);
                            break;
                    }
                }
            }
            downloadIcon.setVisibility(View.GONE);
            uploadIcon.setVisibility(View.GONE);
            deleteIcon.setVisibility(View.GONE);
        } else {
            textViewName.setText(encryptedDocument.getDisplayName());
            if (encryptedDocument.isFolder()) {
                icon.setImageResource(R.drawable.ic_folder);
                textViewLeft.setVisibility(View.GONE);
                textViewRight.setVisibility(View.GONE);
                textViewRight.setVisibility(View.GONE);
            } else {
                icon.setImageResource(R.drawable.ic_file);
                textViewLeft.setVisibility(View.VISIBLE);
                textViewLeft.setText(encryptedDocument.getMimeType());

                textViewRight.setVisibility(View.VISIBLE);
                textViewRight.setText(getContext().getString(R.string.size_text, encryptedDocument.getSizeText()));
            }

            if(encryptedDocument.isUnsynchronized()) {
                changesSyncIcon.setVisibility(View.GONE);
                downloadIcon.setVisibility(View.GONE);
                uploadIcon.setVisibility(View.GONE);
                deleteIcon.setVisibility(View.GONE);
            } else {
                switch (encryptedDocument.getSyncState(SyncAction.Upload)) {
                    case Done:
                        uploadIcon.setVisibility(View.GONE);
                        break;
                    case Planned:
                        uploadIcon.setVisibility(View.VISIBLE);
                        uploadIcon.setImageResource(R.drawable.ic_upload_violet);
                        break;
                    case Running:
                        uploadIcon.setVisibility(View.VISIBLE);
                        uploadIcon.setImageResource(R.drawable.ic_upload_green);
                        break;
                    case Failed:
                        uploadIcon.setVisibility(View.VISIBLE);
                        uploadIcon.setImageResource(R.drawable.ic_upload_red);
                        break;
                }
                switch (encryptedDocument.getSyncState(SyncAction.Download)) {
                    case Done:
                        downloadIcon.setVisibility(View.GONE);
                        break;
                    case Planned:
                        downloadIcon.setVisibility(View.VISIBLE);
                        downloadIcon.setImageResource(R.drawable.ic_download_violet);
                        break;
                    case Running:
                        downloadIcon.setVisibility(View.VISIBLE);
                        downloadIcon.setImageResource(R.drawable.ic_download_green);
                        break;
                    case Failed:
                        downloadIcon.setVisibility(View.VISIBLE);
                        downloadIcon.setImageResource(R.drawable.ic_download_red);
                        break;
                }
                switch (encryptedDocument.getSyncState(SyncAction.Deletion)) {
                    case Done:
                        deleteIcon.setVisibility(View.GONE);
                        break;
                    case Planned:
                        deleteIcon.setVisibility(View.VISIBLE);
                        deleteIcon.setImageResource(R.drawable.ic_delete_violet);
                        break;
                    case Running:
                        deleteIcon.setVisibility(View.VISIBLE);
                        deleteIcon.setImageResource(R.drawable.ic_delete_green);
                        break;
                    case Failed:
                        deleteIcon.setVisibility(View.VISIBLE);
                        deleteIcon.setImageResource(R.drawable.ic_delete_red);
                        break;
                }
            }
        }
        return view;
    }
}
