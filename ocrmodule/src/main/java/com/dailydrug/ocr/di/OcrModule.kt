package com.dailydrug.ocr.di

import com.dailydrug.ocr.data.datasource.OcrDataSource
import com.dailydrug.ocr.data.datasource.OcrDataSourceImpl
import com.dailydrug.ocr.data.parser.DrugInfoParser
import com.dailydrug.ocr.data.repository.OcrRepositoryImpl
import com.dailydrug.ocr.domain.repository.OcrRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OcrModule {

    @Binds
    @Singleton
    abstract fun bindOcrDataSource(
        impl: OcrDataSourceImpl
    ): OcrDataSource

    @Binds
    @Singleton
    abstract fun bindOcrRepository(
        impl: OcrRepositoryImpl
    ): OcrRepository

    @Binds
    @Singleton
    abstract fun bindDrugInfoParser(
        impl: DrugInfoParser
    ): DrugInfoParser
}
