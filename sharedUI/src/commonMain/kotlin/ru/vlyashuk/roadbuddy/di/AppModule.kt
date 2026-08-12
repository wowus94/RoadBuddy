package ru.vlyashuk.roadbuddy.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import ru.vlyashuk.roadbuddy.data.repository.RoadRequestRepositoryImpl
import ru.vlyashuk.roadbuddy.domain.repository.RoadRequestRepository
import ru.vlyashuk.roadbuddy.domain.usecase.CreateRequestUseCase
import ru.vlyashuk.roadbuddy.domain.usecase.GetRequestsUseCase
import ru.vlyashuk.roadbuddy.presentation.home.HomeViewModel

val appModule = module {

    // Repository
    single<RoadRequestRepository> { RoadRequestRepositoryImpl() }

    // UseCase
    factoryOf(::GetRequestsUseCase)
    factoryOf(::CreateRequestUseCase)

    // ViewModel
    viewModelOf(::HomeViewModel)
}