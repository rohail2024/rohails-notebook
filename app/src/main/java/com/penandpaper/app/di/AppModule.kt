package com.penandpaper.app.di

import android.content.Context
import androidx.room.Room
import com.penandpaper.data.db.AppDb
import com.penandpaper.data.repo.NotesRepositoryImpl
import com.penandpaper.domain.repo.NotesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDb =
        Room.databaseBuilder(ctx, AppDb::class.java, "penandpaper.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNotesDao(db: AppDb) = db.notesDao()

    @Provides
    fun provideBlocksDao(db: AppDb) = db.blocksDao()

    @Provides
    fun provideFoldersDao(db: AppDb) = db.foldersDao()

    @Provides
    @Singleton
    fun provideNotesRepository(db: AppDb): NotesRepository = NotesRepositoryImpl(db)
}
