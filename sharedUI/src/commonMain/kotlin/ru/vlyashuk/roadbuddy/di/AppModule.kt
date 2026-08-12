package ru.vlyashuk.roadbuddy.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.vlyashuk.roadbuddy.data.local.database.AppDatabase
import ru.vlyashuk.roadbuddy.data.local.database.getDatabaseBuilder
import ru.vlyashuk.roadbuddy.data.local.database.getRoomDatabase
import ru.vlyashuk.roadbuddy.data.repository.RoadRequestRepositoryImpl
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository
import ru.vlyashuk.roadbuddy.domain.usecase.CreateRequestUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestByIdUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsUseCase
import ru.vlyashuk.roadbuddy.presentation.create.CreateViewModel
import ru.vlyashuk.roadbuddy.presentation.details.DetailsViewModel
import ru.vlyashuk.roadbuddy.presentation.home.HomeViewModel

val appModule = module {

    // Room
    single<AppDatabase> { getRoomDatabase(getDatabaseBuilder()) }
    single { get<AppDatabase>().roadRequestDao() }

    // Repository
    single<RoadRequestRepository> { RoadRequestRepositoryImpl(get()) }

    // UseCase
    factoryOf(::GetRequestsUseCase)
    factoryOf(::CreateRequestUseCase)
    factoryOf(::GetRequestByIdUseCase)

    // ViewModel
    viewModelOf(::HomeViewModel)
    viewModelOf(::CreateViewModel)
    viewModel { (requestId: String) ->
        DetailsViewModel(get(), requestId)
    }
}