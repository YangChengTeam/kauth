package com.yangfeisg.kauth.core.data.di

import android.content.Context
import androidx.room.Room
import com.yangfeisg.kauth.core.data.AccountRepository
import com.yangfeisg.kauth.core.data.OfflineAccountRepository
import com.yangfeisg.kauth.core.data.db.AccountDao
import com.yangfeisg.kauth.core.data.db.KauthDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindAccountRepository(repository: OfflineAccountRepository): AccountRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): KauthDatabase =
            Room.databaseBuilder(context, KauthDatabase::class.java, "kauth.db").build()

        @Provides
        fun provideAccountDao(database: KauthDatabase): AccountDao = database.accountDao()
    }
}
