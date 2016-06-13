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

package fr.petrus.lib.core.rest.services.gdrive;

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import retrofit2.http.Streaming;

import fr.petrus.lib.core.rest.models.OauthTokenResponse;
import fr.petrus.lib.core.rest.models.gdrive.GoogleDriveAbout;
import fr.petrus.lib.core.rest.models.gdrive.GoogleDriveChanges;
import fr.petrus.lib.core.rest.models.gdrive.GoogleDriveItem;
import fr.petrus.lib.core.rest.models.gdrive.GoogleDriveItems;
import fr.petrus.lib.core.rest.models.gdrive.NewItemArg;

/**
 * This interface is used to call the Google Drive API.
 *
 * <p>Each method is mapped to an API URL.
 *
 * @author Pierre Sagne
 * @since 03.03.2015
 */
public interface GoogleDriveApiService {
    @FormUrlEncoded
    @POST("oauth2/v3/token")
    Call<OauthTokenResponse> getOauthToken(@FieldMap Map<String, String> params);

    @GET("drive/v2/about")
    Call<GoogleDriveAbout> getAccountInfo(@Header("Authorization") String authHeader);

    @GET("drive/v2/files/{id}")
    Call<GoogleDriveItem> getItem(@Header("Authorization") String authHeader, @Path("id") String id);

    @GET("drive/v2/files")
    Call<GoogleDriveItems> getItems(@Header("Authorization") String authHeader, @QueryMap Map<String, String> params);

    @GET("drive/v2/changes")
    Call<GoogleDriveChanges> getChanges(@Header("Authorization") String authHeader, @QueryMap Map<String, String> params);

    @GET("drive/v2/files/{id}/children")
    Call<GoogleDriveItems> getChildren(@Header("Authorization") String authHeader, @Path("id") String parentId,
                                       @QueryMap Map<String, String> params);

    @POST("drive/v2/files")
    Call<GoogleDriveItem> createFolder(@Header("Authorization") String authHeader, @Body NewItemArg body);

    @POST("drive/v2/files")
    Call<GoogleDriveItem> createFile(@Header("Authorization") String authHeader, @Body NewItemArg body);

    @POST("upload/drive/v2/files?uploadType=multipart")
    Call<GoogleDriveItem> uploadNewFile(@Header("Authorization") String authHeader, @Body RequestBody body);

    @DELETE("drive/v2/files/{id}")
    Call<ResponseBody> deleteItem(@Header("Authorization") String authHeader, @Path("id") String id);

    @PUT("upload/drive/v2/files/{id}?uploadType=media")
    Call<GoogleDriveItem> uploadFile(@Header("Authorization") String authHeader, @Path("id") String id, @Body RequestBody body);

    @Streaming
    @GET("drive/v2/files/{id}?alt=media")
    Call<ResponseBody> downloadItem(@Header("Authorization") String authHeader, @Path("id") String id);
}
