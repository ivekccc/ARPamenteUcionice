package com.example.pametneucionice.network

import com.example.pametneucionice.model.RoomResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface RoomApiService {

    @GET("api/rooms")
    suspend fun getRooms(): List<RoomResponse>

    @GET("api/rooms/{roomId}")
    suspend fun getRoom(@Path("roomId") roomId: String): RoomResponse
}
