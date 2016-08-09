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

package fr.petrus.tools.storagecrypt.android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;

import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.activity.MainActivity;

/**
 * The background service which displays a notification and keeps the app alive.
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class StorageCryptService extends Service {
    private static final String TAG = "StorageCryptService";

    /**
     * Starts the {@code StorageCryptService}.
     *
     * @param context the Android context
     */
    public static void startService(Context context) {
        Intent intent = new Intent(context, StorageCryptService.class);
        context.startService(intent);
    }

    /**
     * Stops the {@code StorageCryptService}.
     *
     * @param context the Android context
     */
    public static void stopService(Context context) {
        Intent intent = new Intent(context, StorageCryptService.class);
        context.stopService(intent);
    }

    private boolean running = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            Log.i(TAG, "The foreground service is not running, launch it");

            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(AndroidConstants.SERVICE.NOTIFICATION_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent notificationPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_48dp);
            Notification.Builder notificationBuilder = new Notification.Builder(this);
            notificationBuilder
                    .setContentTitle(getText(R.string.app_name))
                    .setTicker(getText(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_white_icon_48dp)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(notificationPendingIntent)
                    .setOngoing(true)
                    .build();
            Notification notification = notificationBuilder.build();

            startForeground(AndroidConstants.SERVICE.FOREGROUND_SERVICE_NOTIFICATION_ID, notification);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
