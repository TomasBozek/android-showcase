package com.example.androidproject1.feature.catalog.presentation.productdetail

import com.example.androidproject1.feature.cart.domain.AddProductToCart
import com.example.androidproject1.feature.catalog.api.ProductDetailDestination
import com.example.androidproject1.feature.catalog.domain.CatalogRepository
import com.example.androidproject1.feature.catalog.domain.FavouritesRepository
import com.example.androidproject1.feature.catalog.domain.Product
import com.example.androidproject1.feature.catalog.presentation.R
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.state.ContentState
import com.example.androidproject1.service.core.ui.text.toPluralUiText
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.update

class ProductDetailViewModel(
    logger: Logger,
    // The route key, handed in by the destination. Available in `init`, and it comes back with
    // the back stack entry after process death.
    private val args: ProductDetailDestination,
    private val catalogRepository: CatalogRepository,
    private val favouritesRepository: FavouritesRepository,
    private val addProductToCart: AddProductToCart,
) : BaseViewModel<ProductDetailState, ProductDetailEvent, ProductDetailNavigation>(
    // Nothing to show until the product named by the route has loaded.
    initialState = null,
    logger = logger.withTag("ProductDetailViewModel"),
) {

    companion object {

        /** Its own id, so the "go back" action is told apart from a retry of a failed load. */
        const val CONTENT_NOT_FOUND = "product_not_found"

        /** The added-to-cart snackbar, so its action is told apart from any other's. */
        const val SNACKBAR_VIEW_CART = "view_cart"

        /** The heart's snackbar; its action goes Home. */
        const val SNACKBAR_VIEW_FAVOURITES = "view_favourites"
    }

    // The screen is two independent reads — the product, once, and the favourite flag, for as long
    // as the screen lives — and either can arrive first. Each used to write the whole state: the
    // load overwrote a flag that had already arrived, and the flag was dropped outright while
    // `data` was still null. Both now write their own half here and the state is composed from the
    // pair, so whichever loses the race is still in the answer.
    private var product: Product? = null
    private var isFavourite = false

    init {
        load()
        observeFavourite()
    }

    private fun publish() {
        val product = product ?: return
        uiState.update {
            it.copy(data = ProductDetailState(product = product, isFavourite = isFavourite))
        }
    }

    override fun onUiEvent(event: ProductDetailEvent) = when (event) {
        ProductDetailEvent.FavouriteToggled -> toggleFavourite()

        ProductDetailEvent.ShareClicked -> share()

        is ProductDetailEvent.LinkClicked -> sendCommand(UiCommand.OpenBrowser(event.url))

        // The sheet opens on one, every time: the last quantity added is not a guess about the next.
        ProductDetailEvent.AddToCartClicked -> updateData { copy(quantitySheetOpen = true, quantity = 1) }

        is ProductDetailEvent.QuantityChanged -> updateData { copy(quantity = event.quantity) }

        ProductDetailEvent.ConfirmAddClicked -> addToCart()

        ProductDetailEvent.QuantitySheetDismissed -> updateData { copy(quantitySheetOpen = false) }

        ProductDetailEvent.NavigateUpClicked -> navigate(ProductDetailNavigation.NavigateUp)
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.SnackbarAction && event.id == SNACKBAR_VIEW_CART) {
            navigate(ProductDetailNavigation.OpenCart)
            return
        }
        if (event is SystemEvent.SnackbarAction && event.id == SNACKBAR_VIEW_FAVOURITES) {
            navigate(ProductDetailNavigation.OpenHome)
            return
        }
        if (event is SystemEvent.ContentAction && event.id == CONTENT_NOT_FOUND) {
            // There is no product to come back to, so the only useful action is leaving.
            sendCommand(UiCommand.NavigateBack)
            return
        }
        super.onSystemEvent(event)
    }

    /**
     * The heart follows the database, not the tap.
     *
     * Writing the state optimistically and letting the flow confirm it would mean two sources of
     * truth for one boolean; observing means a favourite removed from Home is already un-hearted
     * when the user navigates back here. No overlay, because a heart is not worth one.
     */
    private fun observeFavourite() = observe(
        flow = { favouritesRepository.observeIsFavourite(args.productId) },
        errorDisplay = ErrorDisplay.Silent,
        onData = { favourite ->
            isFavourite = favourite
            publish()
        },
    )

    /**
     * The write runs here, in the ViewModel's own scope.
     *
     * It used to be launched from `AppNavHost`'s `rememberCoroutineScope`, which a rotation just
     * after the tap cancels — the item never reached the cart and nothing said so. A ViewModel
     * survives a configuration change, so this one does not. The product is already loaded, so
     * there is no second read of it either.
     */
    private fun addToCart() {
        val product = product ?: return
        val quantity = uiState.value.data?.quantity ?: 1
        updateData { copy(quantitySheetOpen = false) }
        execute(
            action = {
                addProductToCart(
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    quantity = quantity,
                )
            },
            onData = {
                // The snackbar goes somewhere: its action opens the cart the line went to.
                showSnackbar(
                    message = R.plurals.product_detail_added_to_cart.toPluralUiText(quantity, quantity),
                    actionLabel = R.string.product_detail_view_cart.toUiText(),
                    id = SNACKBAR_VIEW_CART,
                )
            },
        )
    }

    // The door, not the link: the scheme is the application id, which only the shell reads.
    private fun share() {
        val product = product ?: return
        sendCommand(UiCommand.Share(text = product.name, subject = product.name, appPath = "product/${product.id}"))
    }

    // Alert on failure: the user asked for this, so silence would look like the tap did nothing —
    // and a success says so too, with a way to Home, where the favourites are.
    private fun toggleFavourite() {
        val favourite = !isFavourite
        execute(
            action = { favouritesRepository.setFavourite(args.productId, favourite) },
            onData = {
                showSnackbar(
                    message = if (favourite) {
                        R.string.product_detail_favourite_added.toUiText()
                    } else {
                        R.string.product_detail_favourite_removed.toUiText()
                    },
                    actionLabel = R.string.product_detail_favourite_view.toUiText(),
                    id = SNACKBAR_VIEW_FAVOURITES,
                )
            },
        )
    }

    // Inline rather than Alert: this is the call that loads the screen, so a dialog would leave
    // nothing behind it. BaseViewModel remembers the call and the retry button re-runs it.
    private fun load() = execute(
        loading = overlay(),
        errorDisplay = ErrorDisplay.Inline,
        action = { catalogRepository.getProduct(args.productId) },
        onData = { product ->
            if (product == null) {
                // A success that found nothing, not a failure — the same distinction ProductsScreen
                // draws for an empty category.
                showContent(
                    ContentState.Empty(
                        id = CONTENT_NOT_FOUND,
                        message = R.string.product_detail_not_found.toUiText(),
                        actionLabel = R.string.product_detail_go_back.toUiText(),
                    ),
                )
                return@execute
            }
            this.product = product
            publish()
        },
    )
}
