package com.example.androidproject1.feature.checkout.di

import com.example.androidproject1.feature.checkout.data.repository.DefaultCheckoutDraftRepository
import com.example.androidproject1.feature.checkout.data.repository.DefaultCheckoutRepository
import com.example.androidproject1.feature.checkout.data.source.DefaultLocalCheckoutDraftDataSource
import com.example.androidproject1.feature.checkout.data.source.DefaultRemoteCheckoutDataSource
import com.example.androidproject1.feature.checkout.data.source.LocalCheckoutDraftDataSource
import com.example.androidproject1.feature.checkout.data.source.RemoteCheckoutDataSource
import com.example.androidproject1.feature.checkout.domain.CheckoutDraftRepository
import com.example.androidproject1.feature.checkout.domain.CheckoutDraftStore
import com.example.androidproject1.feature.checkout.domain.CheckoutRepository
import com.example.androidproject1.feature.checkout.domain.PlaceOrder
import com.example.androidproject1.feature.checkout.presentation.checkoutaddress.CheckoutAddressViewModel
import com.example.androidproject1.feature.checkout.presentation.checkoutdelivery.CheckoutDeliveryViewModel
import com.example.androidproject1.feature.checkout.presentation.checkoutpayment.CheckoutPaymentViewModel
import com.example.androidproject1.feature.checkout.presentation.checkoutreview.CheckoutReviewViewModel
import com.example.androidproject1.service.core.domain.LocalDataEraser
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

object CheckoutModule {

    val module: Module = module {
        viewModelOf(::CheckoutAddressViewModel)
        viewModelOf(::CheckoutDeliveryViewModel)
        viewModelOf(::CheckoutPaymentViewModel)
        viewModelOf(::CheckoutReviewViewModel)

        factoryOf(::PlaceOrder)
        // The wizard's draft, one per app: each step reads and writes it, and leaving or placing
        // the order clears it, so it lives as long as one checkout does — across launches, since
        // it is written to the preferences store. Account data, so it is an eraser too.
        singleOf(::CheckoutDraftStore) bind LocalDataEraser::class
        singleOf(::DefaultCheckoutRepository) bind CheckoutRepository::class
        singleOf(::DefaultRemoteCheckoutDataSource) bind RemoteCheckoutDataSource::class
        singleOf(::DefaultCheckoutDraftRepository) bind CheckoutDraftRepository::class
        singleOf(::DefaultLocalCheckoutDraftDataSource) bind LocalCheckoutDraftDataSource::class
    }
}
