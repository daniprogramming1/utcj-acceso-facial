package edu.utcj.acceso.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.utcj.acceso.data.repository.CsvBackedStudentStatusRepository
import edu.utcj.acceso.data.repository.StudentStatusRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindStudentStatusRepository(
        impl: CsvBackedStudentStatusRepository
    ): StudentStatusRepository
}
