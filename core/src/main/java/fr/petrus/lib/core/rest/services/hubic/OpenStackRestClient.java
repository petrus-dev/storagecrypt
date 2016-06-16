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

package fr.petrus.lib.core.rest.services.hubic;

import java.util.concurrent.TimeUnit;

import fr.petrus.lib.core.Constants;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * This class is used to create and return the API interface to call the OpenStack API.
 *
 * <p>The OpenStack API is used by HubiC to provide access to its files.
 *
 * @author Pierre Sagne
 * @since 03.03.2015
 */
public class OpenStackRestClient {
    private OpenStackApiService apiService;

    /**
     * Creates a new {@code OpenStackRestClient} instance for the given {@code endpoint}.
     *
     * @param endpoint the OpenStack endpoint provided by HubiC for each account (also contains an
     *                 OpenStack user name)
     */
    public OpenStackRestClient(String endpoint) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(Constants.RETROFIT.LOG_LEVEL);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(Constants.HUBIC.CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
                .readTimeout(Constants.HUBIC.READ_TIMEOUT_S, TimeUnit.SECONDS)
                .writeTimeout(Constants.HUBIC.WRITE_TIMEOUT_S, TimeUnit.SECONDS)
                .build();

        String openStackEndPoint = endpoint;
        if (!openStackEndPoint.endsWith("/")) {
            openStackEndPoint += "/";
        }

        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl(openStackEndPoint)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = restAdapter.create(OpenStackApiService.class);
    }

    public OpenStackApiService getApiService() {
        return apiService;
    }
}
