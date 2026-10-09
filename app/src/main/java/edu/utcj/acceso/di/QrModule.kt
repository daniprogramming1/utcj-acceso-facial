package edu.utcj.acceso.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.utcj.acceso.data.qr.AndroidKeystoreQrKeyStore
import edu.utcj.acceso.domain.qr.QrKeyStore
import javax.inject.Singleton

/** Módulo separado para que las pruebas lo reemplacen con llaves en software. */
@Module
@InstallIn(SingletonComponent::class)
abstract class QrModule {
    @Binds
    @Singleton
    abstract fun bindQrKeyStore(impl: AndroidKeystoreQrKeyStore): QrKeyStore
}
