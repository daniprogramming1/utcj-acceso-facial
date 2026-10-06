package edu.utcj.acceso.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.utcj.acceso.data.security.KeyValueStore
import edu.utcj.acceso.data.security.SecurePrefs
import javax.inject.Singleton

/**
 * Enlaces de seguridad. [SecurePrefs] (EncryptedSharedPreferences) respalda a
 * [KeyValueStore], que usa GuardAuthManager. EmbeddingCrypto y PasswordHasher
 * no requieren providers adicionales.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {
    @Binds
    @Singleton
    abstract fun bindKeyValueStore(impl: SecurePrefs): KeyValueStore
}
