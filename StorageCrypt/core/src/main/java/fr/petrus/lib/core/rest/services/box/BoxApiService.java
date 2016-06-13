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

package fr.petrus.lib.core.rest.services.box;

import java.util.Map;

import fr.petrus.lib.core.rest.models.box.BoxItem;
import fr.petrus.lib.core.rest.models.box.BoxItems;
import fr.petrus.lib.core.rest.models.box.NewItemArg;
import fr.petrus.lib.core.rest.models.box.BoxUser;
import fr.petrus.lib.core.rest.models.OauthTokenResponse;
import fr.petrus.lib.core.rest.models.box.UpdateDescriptionArg;

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

/**
 * This interface is used to call the Box.com API.
 *
 * <p>Each method is mapped to an API URL.
 *
 * @author Pierre Sagne
 * @since 03.03.2015
 */
public interface BoxApiService {
    @FormUrlEncoded
    @POST("oauth2/token")
    Call<OauthTokenResponse> getOauthToken(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("oauth2/revoke")
    Call<ResponseBody> revokeOauthToken(@FieldMap Map<String, String> params);

    @GET("2.0/users/me")
    Call<BoxUser> getAccountInfo(@Header("Authorization") String authHeader);

    @GET("2.0/folders/{id}")
    Call<BoxItem> getFolder(@Header("Authorization") String authHeader, @Path("id") String id);

    @GET("2.0/files/{id}?fields=version_number")
    Call<BoxItem> getFile(@Header("Authorization") String authHeader, @Path("id") String id);

    @PUT("2.0/files/{id}")
    Call<BoxItem> updateFileDescription(@Header("Authorization") String authHeader, @Path("id") String id,
                                        @Body UpdateDescriptionArg body);

    @GET("2.0/folders/{id}/items?fields=version_number")
    Call<BoxItems> getFolderItems(@Header("Authorization") String authHeader, @Path("id") String id,
                                  @QueryMap Map<String, String> params);

    @POST("2.0/folders")
    Call<BoxItem> createFolder(@Header("Authorization") String authHeader, @Body NewItemArg body);

    @PUT("2.0/folders/{id}")
    Call<BoxItem> updateFolderDescription(@Header("Authorization") String authHeader, @Path("id") String id,
                                          @Body UpdateDescriptionArg body);

    @DELETE("2.0/folders/{id}?recursive=true")
    Call<ResponseBody> deleteFolder(@Header("Authorization") String authHeader, @Path("id") String id);

    @DELETE("2.0/files/{id}")
    Call<ResponseBody> deleteFile(@Header("Authorization") String authHeader, @Path("id") String id);

    @GET("2.0/search")
    Call<BoxItems> searchItems(@Header("Authorization") String authHeader, @QueryMap Map<String, String> params);

    @Streaming
    @GET("2.0/files/{id}/content")
    Call<ResponseBody> downloadFile(@Header("Authorization") String authHeader, @Path("id") String id);
}
