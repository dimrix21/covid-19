package com.android_academy.covid_19.repository

import android.location.Location
import android.util.Log
import com.android_academy.covid_19.db.dao.RoomUserLocationEntity
import com.android_academy.covid_19.db.dao.UserLocationsDao
import com.android_academy.covid_19.db.dao.toRoomLocationEntity
import com.android_academy.covid_19.providers.ILocationManager
import com.android_academy.covid_19.providers.UserLocationModel
import com.android_academy.covid_19.providers.fromRoomEntity
import com.android_academy.covid_19.util.logTag
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface UsersLocationRepo {
    suspend fun getLocation()
    fun getUserLocationsFlow(): Flow<List<UserLocationModel>>
    suspend fun getUserLocationsAsync(): List<UserLocationModel>
    suspend fun saveLocation(location: RoomUserLocationEntity)
    suspend fun cleanOldTimeLineProviderLocation()
    suspend fun getLastTimeLineLocation(): RoomUserLocationEntity?
}

class UsersLocationRepoImpl(
    private val usersLocDao: UserLocationsDao,
    private val locationManager: ILocationManager,
    private val scope: CoroutineScope
) : UsersLocationRepo {
    override suspend fun getLocation() {
        val location = locationManager.getUpdatedLocation()
        Log.d(logTag, "got location $location")
        onLocationReceived(location)
    }

    override fun getUserLocationsFlow(): Flow<List<UserLocationModel>> {
        return usersLocDao.getUserLocations()
            .map { userLocations -> userLocations.map { fromRoomEntity(it) } }
    }

    override suspend fun getUserLocationsAsync(): List<UserLocationModel> =
        usersLocDao.getUserLocationsAsync().map { fromRoomEntity(it) }

    private fun onLocationReceived(location: Location?) =
        scope.launch(CoroutineExceptionHandler { _, throwable ->
            Log.e(logTag, "Exception something something ${throwable.message}")
        }) {
            location?.let { newLocation ->
                saveLocation(newLocation.toRoomLocationEntity())
            }
        }

    override suspend fun saveLocation(location: RoomUserLocationEntity) {
        Log.d(logTag, "Saving location $location")
        usersLocDao.saveLocation(location)
    }

    override suspend fun cleanOldTimeLineProviderLocation() = withContext(Dispatchers.IO) {
        usersLocDao.deleteTimelineLocations()
    }

    override suspend fun getLastTimeLineLocation(): RoomUserLocationEntity? =
        withContext(Dispatchers.IO) {
            usersLocDao.getTimelineLocations()?.firstOrNull()
        }

    companion object {
        const val DISTANCE_THRESHOLD = 30
        const val TIME_THRESHOLD = 30L
    }
}
