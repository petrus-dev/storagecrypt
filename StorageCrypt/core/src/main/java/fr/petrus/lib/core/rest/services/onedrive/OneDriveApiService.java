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

package fr.petrus.lib.core.rest.services.onedrive;

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import fr.petrus.lib.core.rest.models.onedrive.OneDriveDelta;
import fr.petrus.lib.core.rest.models.onedrive.OneDriveItem;
import fr.petrus.lib.core.rest.models.onedrive.OneDriveItems;
import fr.petrus.lib.core.rest.models.onedrive.NewFolderArg;
import fr.petrus.lib.core.rest.models.onedrive.OneDriveRoot;
import retrofit2.http.Streaming;

/**
 * This interface is used to call the OneDrive API.
 *
 * <p>Each method is mapped to an API URL.
 *
 * @author Pierre Sagne
 * @since 03.03.2015
 */
public interface OneDriveApiService {
    @GET("v1.0/drive")
    Call<OneDriveRoot> getDriveRoot(@Header("Authorization") String authHeader);

    @GET("v1.0/drive/root:{path}")
    Call<OneDriveItem> getDocumentByPath(@Header("Authorization") String authHeader,
                                         @Path(value="path", encoded=true) String path);

    @GET("v1.0/drive/items/{id}")
    Call<OneDriveItem> getDocumentById(@Header("Authorization") String authHeader,
                                       @Path(value="id", encoded=true) String id);

    @GET("v1.0/drive/root:{path}:/children")
    Call<OneDriveItems> getChildrenByPath(@Header("Authorization") String authHeader,
                                          @Path(value="path", encoded=true) String path);

    @GET("v1.0/drive/items/{id}/children")
    Call<OneDriveItems> getChildrenById(@Header("Authorization") String authHeader,
                                        @Path(value="id", encoded=true) String id);

    @GET("v1.0/drive/root:{path}:/view.delta")
    Call<OneDriveDelta> getDeltaByPath(@Header("Authorization") String authHeader,
                                       @Path(value="path", encoded=true) String path,
                                       @QueryMap Map<String, String> params);

    @GET("v1.0/drive/items/{id}/view.delta")
    Call<OneDriveDelta> getDeltaById(@Header("Authorization") String authHeader,
                                     @Path(value="id", encoded=true) String id,
                                     @QueryMap Map<String, String> params);

    @POST("v1.0/drive/root/children")
    Call<OneDriveItem> createFolderInRoot(@Header("Authorization") String authHeader,
                                          @Body NewFolderArg body);

    @POST("v1.0/drive/root:{path}:/children")
    Call<OneDriveItem> createFolderByPath(@Header("Authorization") String authHeader,
                                          @Path(value="path", encoded=true) String path,
                                          @Body NewFolderArg body);

    @POST("v1.0/drive/items/{id}/children")
    Call<OneDriveItem> createFolderById(@Header("Authorization") String authHeader,
                                        @Path(value="id", encoded=true) String id,
                                        @Body NewFolderArg body);

    @PUT("v1.0/drive/root:{path}/{name}:/content?@name.conflictBehavior=fail")
    Call<OneDriveItem> createFileByPath(@Header("Authorization") String authHeader,
                                        @Path(value="path", encoded=true) String path,
                                        @Path(value="name", encoded=true) String name,
                                        @Body RequestBody body);

    @PUT("v1.0/drive/items/{id}/children/{name}/content?@name.conflictBehavior=fail")
    Call<OneDriveItem> createFileById(@Header("Authorization") String authHeader,
                                      @Path(value="id", encoded=true) String id,
                                      @Path(value="name", encoded=true) String name,
                                      @Body RequestBody body);

    @PUT("v1.0/drive/root:{path}/{name}:/content?@name.conflictBehavior=fail")
    Call<OneDriveItem> uploadNewFileByPath(@Header("Authorization") String authHeader,
                                           @Path(value="path", encoded=true) String path,
                                           @Path(value="name", encoded=true) String name,
                                           @Body RequestBody body);

    @PUT("v1.0/drive/items/{id}/children/{name}/content?@name.conflictBehavior=fail")
    Call<OneDriveItem> uploadNewFileById(@Header("Authorization") String authHeader,
                                         @Path(value="id", encoded=true) String id,
                                         @Path(value="name", encoded=true) String name,
                                         @Body RequestBody body);

    @DELETE("v1.0/drive/root:{path}")
    Call<ResponseBody> deleteDocumentByPath(@Header("Authorization") String authHeader,
                                            @Path(value="path", encoded=true) String path);

    @DELETE("v1.0/drive/items/{id}")
    Call<ResponseBody> deleteDocumentById(@Header("Authorization") String authHeader,
                                          @Path(value="id", encoded=true) String id);

    @PUT("v1.0/drive/root:{path}/{name}:/content")
    Call<OneDriveItem> uploadFileByPath(@Header("Authorization") String authHeader,
                                        @Path(value="path", encoded=true) String path,
                                        @Path(value="name", encoded=true) String name,
                                        @Body RequestBody body);

    @PUT("v1.0/drive/items/{id}/children/{name}/content")
    Call<OneDriveItem> uploadFileById(@Header("Authorization") String authHeader,
                                      @Path(value="id", encoded=true) String id,
                                      @Path(value="name", encoded=true) String name,
                                      @Body RequestBody body);

    @Streaming
    @GET("v1.0/drive/root:{path}/content")
    Call<ResponseBody> downloadDocumentByPath(@Header("Authorization") String authHeader,
                                              @Path(value="path", encoded=true) String path);

    @Streaming
    @GET("v1.0/drive/items/{id}/content")
    Call<ResponseBody> downloadDocumentById(@Header("Authorization") String authHeader,
                                            @Path(value="id", encoded=true) String id);
}
