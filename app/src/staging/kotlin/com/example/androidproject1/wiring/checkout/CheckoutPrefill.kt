package com.example.androidproject1.wiring.checkout

import com.example.androidproject1.feature.checkout.domain.CheckoutPrefill

/** See the `dev` source set's copy: a build that talks to a real server starts every checkout empty. */
fun checkoutPrefill(): CheckoutPrefill = CheckoutPrefill.NONE
