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

import java.util.List;

import fr.petrus.lib.core.rest.models.hubic.OpenStackContainer;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import fr.petrus.lib.core.rest.models.hubic.OpenStackObject;
import retrofit2.http.Streaming;

/**
 * This interface is used to call the OpenStack API used by HubiC for storage access.
 *
 * <p>Each method is mapped to an API URL.
 *
 * @author Pierre Sagne
 * @since 03.03.2015
 */
public interface OpenStackApiService {
    @HEAD("v1/{account}")
    Call<Void> getAccountInfos(@Header("X-Auth-Token") String authHeader,
                               @Path("account") String account);

    @GET("v1/{account}?format=json")
    Call<List<OpenStackContainer>> getAllContainers(@Header("X-Auth-Token") String authHeader,
                                                    @Path("account") String account);

    @HEAD("v1/{account}/{container}")
    Call<Void> getContainerInfos(@Header("X-Auth-Token") String authHeader,
                                 @Path("account") String account, @Path("container") String container);

    @GET("v1/{account}/{container}?format=json")
    Call<List<OpenStackObject>> getAllObjects(@Header("X-Auth-Token") String authHeader,
                                              @Path("account") String account, @Path("container") String container);

    @GET("v1/{account}/{container}?format=json&delimiter=/")
    Call<List<OpenStackObject>> getContainerChildren(@Header("X-Auth-Token") String authHeader,
                                                     @Path("account") String account, @Path("container") String container);

    @GET("v1/{account}/{container}?format=json&delimiter=/")
    Call<List<OpenStackObject>> getFolderChildren(@Header("X-Auth-Token") String authHeader, @Path("account") String account,
                                                  @Path("container") String container, @Query("prefix") String folderName);

    @GET("v1/{account}/{container}?format=json")
    Call<List<OpenStackObject>> getFolderRecursiveChildren(@Header("X-Auth-Token") String authHeader,
                                                           @Path("account") String account,
                                                           @Path("container") String container,
                                                           @Query("prefix") String folderName);

    @HEAD("v1/{account}/{container}/{path}")
    Call<Void> getDocument(@Header("X-Auth-Token") String authHeader, @Path("account") String account,
                           @Path("container") String container, @Path(value="path", encoded=true) String path);

    @DELETE("v1/{account}/{container}/{path}")
    Call<ResponseBody> deleteDocument(@Header("X-Auth-Token") String authHeader, @Path("account") String account,
                                      @Path("container") String container, @Path(value="path", encoded=true) String path);

    @PUT("v1/{account}/{container}/{path}")
    Call<ResponseBody> uploadDocument(@Header("X-Auth-Token") String authHeader, @Path("account") String account,
                                      @Path("container") String container,
                                      @Path(value="path", encoded=true) String path, @Body RequestBody file);


    @Streaming
    @GET("v1/{account}/{container}/{path}")
    Call<ResponseBody> downloadDocument(@Header("X-Auth-Token") String authHeader, @Path("account") String account,
                                        @Path("container") String container, @Path(value="path", encoded=true) String path);
}
