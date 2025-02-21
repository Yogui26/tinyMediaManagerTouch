package org.tinymediamanager.scraper.mdblist.services;

import org.tinymediamanager.scraper.mdblist.entities.MdbMediaEntity;
import org.tinymediamanager.scraper.mdblist.entities.MdbSearchResults;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MdbListService {

  @GET("{mediaProvider}/{mediaType}/{mediaId}")
  Call<MdbMediaEntity> getMdbMediaEntity(@Path("mediaProvider") String mediaProvider, @Path("mediaType") String mediaType,
      @Path("mediaId") String mediaId);

  @GET("{mediaProvider}/{mediaType}/{mediaId}")
  Call<MdbMediaEntity> getMdbMediaEntity(@Path("mediaProvider") String mediaProvider, @Path("mediaType") String mediaType,
      @Path("mediaId") String mediaId, @Query("append_to_response") String appendToResponse);

  // more filtering params possible https://mdblist.docs.apiary.io/#reference/0/search-for-media/get-search-results
  @GET("search/{mediaType}")
  Call<MdbSearchResults> search(@Path("mediaType") String mediaType, @Query("query") String query);

}
