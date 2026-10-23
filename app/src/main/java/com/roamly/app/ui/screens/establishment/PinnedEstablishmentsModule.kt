package com.roamly.app.ui.screens.establishment


import com.roamly.app.entity.ViewModel.UserEstablishmentsViewModel
import com.roamly.app.manager.PinnedEstablishmentsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object PinnedEstablishmentsModule {

    @Provides
    @ViewModelScoped
    fun provideUserEstablishmentsViewModel(
        repository: PinnedEstablishmentsRepository
    ): UserEstablishmentsViewModel {
        return UserEstablishmentsViewModel(repository)
    }
}