package ru.vlyashuk.roadbuddy.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.vlyashuk.roadbuddy.data.local.database.AppDatabase
import ru.vlyashuk.roadbuddy.data.local.database.getDatabaseBuilder
import ru.vlyashuk.roadbuddy.data.local.database.getRoomDatabase
import ru.vlyashuk.roadbuddy.data.remote.auth.RemoteAuthDataSource
import ru.vlyashuk.roadbuddy.data.remote.firestore.RoadRequestRemoteDataSource
import ru.vlyashuk.roadbuddy.data.repository.AuthRepositoryImpl
import ru.vlyashuk.roadbuddy.data.repository.RoadRequestRepositoryImpl
import ru.vlyashuk.roadbuddy.domain.repository.AuthRepository
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository
import ru.vlyashuk.roadbuddy.domain.usecase.CreateRequestUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.UpdateRequestUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.GetCurrentUserUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.SignInUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.SignOutUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.auth.SignUpUseCase
import ru.vlyashuk.roadbuddy.presentation.create.CreateViewModel
import ru.vlyashuk.roadbuddy.presentation.details.DetailsViewModel
import ru.vlyashuk.roadbuddy.presentation.edit.EditViewModel
import ru.vlyashuk.roadbuddy.presentation.home.HomeViewModel
import ru.vlyashuk.roadbuddy.presentation.login.LoginViewModel
import ru.vlyashuk.roadbuddy.presentation.register.RegisterViewModel

val appModule = module {

    // Firestore
    single { RoadRequestRemoteDataSource() }

    // Room
    single<AppDatabase> { getRoomDatabase(getDatabaseBuilder()) }
    single { get<AppDatabase>().roadRequestDao() }

    // Repository
    single<RoadRequestRepository> {
        RoadRequestRepositoryImpl(get(), get(), get())
    }
    single { RemoteAuthDataSource() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }

    // UseCase
    factoryOf(::GetRequestsUseCase)
    factoryOf(::CreateRequestUseCase)
    factoryOf(::GetRequestByIdUseCase)
    factoryOf(::UpdateRequestUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::GetCurrentUserUseCase)

    // ViewModel
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::CreateViewModel)
    viewModel { (requestId: String) ->
        DetailsViewModel(get(), get(), requestId)
    }
    viewModel { (requestId: String) ->
        EditViewModel(get(), get(), requestId)
    }
}