package com.dailydrug.photopicker.di

import com.dailydrug.photopicker.domain.repository.PhotoPickerRepository
import com.dailydrug.photopicker.data.repository.PhotoPickerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoPickerModule {

    @Binds
    @Singleton
    abstract fun bindPhotoPickerRepository(
        impl: PhotoPickerRepositoryImpl
    ): PhotoPickerRepository
}
