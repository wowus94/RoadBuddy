package ru.vlyashuk.roadbuddy.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.vlyashuk.roadbuddy.data.local.database.AppDatabase
import ru.vlyashuk.roadbuddy.data.local.database.getDatabaseBuilder
import ru.vlyashuk.roadbuddy.data.local.database.getRoomDatabase
import ru.vlyashuk.roadbuddy.data.remote.auth.AuthService
import ru.vlyashuk.roadbuddy.data.remote.auth.AuthServiceImpl
import ru.vlyashuk.roadbuddy.data.remote.firestore.RoadRequestRemoteDataSource
import ru.vlyashuk.roadbuddy.data.repository.RoadRequestRepositoryImpl
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository
import ru.vlyashuk.roadbuddy.domain.usecase.CreateRequestUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.UpdateRequestUseCase
import ru.vlyashuk.roadbuddy.presentation.create.CreateViewModel
import ru.vlyashuk.roadbuddy.presentation.details.DetailsViewModel
import ru.vlyashuk.roadbuddy.presentation.edit.EditViewModel
import ru.vlyashuk.roadbuddy.presentation.home.HomeViewModel
import ru.vlyashuk.roadbuddy.presentation.login.LoginViewModel

val appModule = module {

    // Firestore
    single { RoadRequestRemoteDataSource() }

    // AuthService
    single<AuthService> { AuthServiceImpl() }

    // Room
    single<AppDatabase> { getRoomDatabase(getDatabaseBuilder()) }
    single { get<AppDatabase>().roadRequestDao() }

    // Repository
    single<RoadRequestRepository> { RoadRequestRepositoryImpl(get(), get()) }

    // UseCase
    factoryOf(::GetRequestsUseCase)
    factoryOf(::CreateRequestUseCase)
    factoryOf(::GetRequestByIdUseCase)
    factoryOf(::UpdateRequestUseCase)

    // ViewModel
    viewModelOf(::LoginViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::CreateViewModel)
    viewModel { (requestId: String) ->
        DetailsViewModel(get(), requestId)
    }
    viewModel { (requestId: String) ->
        EditViewModel(get(), get(), requestId)
    }
}