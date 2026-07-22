package com.asianmobile.privatebrower.ui.premium

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.asianmobile.privatebrower.ads.utils.Utils
import com.asianmobile.privatebrower.constant.BASE_MONTHLY_COST_ID
import com.asianmobile.privatebrower.constant.BASE_WEEKLY_COST_ID
import com.asianmobile.privatebrower.constant.BASE_YEARLY_COST_ID
import com.asianmobile.privatebrower.constant.PRODUCT_ID
import com.google.common.collect.ImmutableList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * Copyright © 2026 Asian Mobile Co.,Ltd
 * Created by am_viennv on 4/6/2026
 */

@HiltViewModel
class PremiumViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel(), PurchasesUpdatedListener {
    internal var tagPlanId: String = BASE_MONTHLY_COST_ID

    private val productDetailsList: MutableList<ProductDetails> = mutableListOf()

    private lateinit var billingClient: BillingClient

    private val _purchaseSuccess = MutableStateFlow(false)
    val purchaseSuccess: StateFlow<Boolean> = _purchaseSuccess.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    init {
        initBillClient()
        establishConnection()
    }

    private val _priceWeeklyCost = MutableStateFlow("")
    internal val priceWeeklyCost: StateFlow<String> = _priceWeeklyCost.asStateFlow()

    private val _priceMonthlyCost = MutableStateFlow("")
    internal val priceMonthlyCost: StateFlow<String> = _priceMonthlyCost.asStateFlow()

    private val _priceWeekInMonthCost = MutableStateFlow<String?>(null)
    internal val priceWeekInMonthCost: StateFlow<String?> = _priceWeekInMonthCost.asStateFlow()

    private val _priceYearlyCost = MutableStateFlow("")
    internal val priceYearlyCost: StateFlow<String> = _priceYearlyCost.asStateFlow()

    private val _priceWeekInYearCost = MutableStateFlow<String?>(null)
    internal val priceWeekInYearCost: StateFlow<String?> = _priceWeekInYearCost.asStateFlow()

    private val _isCheckInternet = MutableStateFlow(true)
    internal val isCheckInternet: StateFlow<Boolean> = _isCheckInternet.asStateFlow()

    private fun initBillClient() {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                verifySubPurchase(purchase)
            }
        }
    }

    private fun verifySubPurchase(purchase: Purchase) {
        val acknowledgePurchaseParams =
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _isLoading.value = true
                _purchaseSuccess.value = true
            }
        }
    }

    private var isConnecting = false
    private val billingHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    private fun establishConnection() {
        if (billingClient.isReady || isConnecting) {
            return
        }
        isConnecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    showProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                billingHandler.postDelayed({
                    establishConnection()
                }, 3000)
            }
        })
    }

    fun checkExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchaseList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        verifySubPurchase(purchase)
                    } else if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        _isLoading.value = true
                        _purchaseSuccess.value = true
                    }
                }
            }
        }
    }

    private fun showProducts() {
        val productList = ImmutableList.of(
            QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS).build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient.queryProductDetailsAsync(params) { _, prodDetailsList ->
            setProduct(prodDetailsList.productDetailsList)
        }
    }

    internal fun onClickBuyPremium(activity: Activity) {
        if (Utils.isNetworkAvailable(context)) {
            _isCheckInternet.value = true

            if (productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val subscription = productDetails.subscriptionOfferDetails
                if (subscription != null) {
                    var positionProduct = 0
                    for (i in 0 until subscription.size) {
                        if (subscription[i].basePlanId == tagPlanId) {
                            positionProduct = i
                        }
                    }

                    val productDetailsParamsList = ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(subscription[positionProduct].offerToken).build()
                    )
                    val billingFlowParams =
                        BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build()
                    billingClient.launchBillingFlow(activity, billingFlowParams)
                }
            }
        } else {
            _isCheckInternet.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingHandler.removeCallbacksAndMessages(null)
        if (::billingClient.isInitialized && billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    internal fun setProduct(prodDetailsList: MutableList<ProductDetails>) {
        if (prodDetailsList.isEmpty()) return
        productDetailsList.clear()
        productDetailsList.addAll(prodDetailsList)
        prodDetailsList[0].subscriptionOfferDetails?.apply {
            for (i in 0 until size) {
                for (j in 0 until get(i).pricingPhases.pricingPhaseList.size) {
                    val price = get(i).pricingPhases.pricingPhaseList[j].formattedPrice
                    when (get(i).basePlanId) {
                        BASE_WEEKLY_COST_ID -> _priceWeeklyCost.value = price
                        BASE_MONTHLY_COST_ID -> {
                            _priceMonthlyCost.value = price
                            _priceWeekInMonthCost.value = calculatorPrice(
                                get(i).pricingPhases.pricingPhaseList[j],
                                price,
                                true
                            )

                        }

                        BASE_YEARLY_COST_ID -> {
                            _priceYearlyCost.value = price
                            _priceWeekInYearCost.value = calculatorPrice(
                                get(i).pricingPhases.pricingPhaseList[j],
                                price,
                                false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun calculatorPrice(
        pricingPhase: ProductDetails.PricingPhase?,
        formattedPrice: String,
        isMonth: Boolean,
        isDuplicate: Boolean = false
    ): String {
        try {
            if (pricingPhase == null) return ""
            val micros = pricingPhase.priceAmountMicros
            val basePrice = BigDecimal.valueOf(micros)
                .divide(BigDecimal("1000000"), 6, RoundingMode.HALF_UP)
            val weekly = if (isDuplicate) {
                basePrice.multiply(BigDecimal(2))
            } else {
                val weeks = if (isMonth) BigDecimal(4) else BigDecimal(52)
                basePrice.divide(weeks, 6, RoundingMode.HALF_UP)
            }
            val currencyCode = try {
                pricingPhase.priceCurrencyCode
            } catch (_: Exception) {
                null
            }
            return formatLikeOriginal(formattedPrice, weekly, currencyCode)

        } catch (_: Exception) {
            return formattedPrice
        }
    }

    fun formatLikeOriginal(
        formattedPrice: String,
        amount: BigDecimal,
        currencyCode: String?
    ): String {
        if (formattedPrice.isBlank()) return ""

        // tìm vị trí chữ số đầu / cuối (hỗ trợ digits-localized)
        val firstDigitIndex = formattedPrice.indexOfFirst { it.isDigit() }
        val lastDigitIndex = formattedPrice.indexOfLast { it.isDigit() }

        // fallback nếu không tìm thấy digit
        if (firstDigitIndex == -1 || lastDigitIndex == -1) {
            val currency = try {
                if (currencyCode != null) Currency.getInstance(currencyCode) else null
            } catch (_: Exception) {
                null
            }
            val nf = NumberFormat.getCurrencyInstance().apply {
                currency?.let { this.currency = it }
                maximumFractionDigits = currency?.defaultFractionDigits ?: 2
                minimumFractionDigits = currency?.defaultFractionDigits ?: 2
                isGroupingUsed = true
            }
            return nf.format(
                amount.setScale(
                    currency?.defaultFractionDigits ?: 2,
                    RoundingMode.HALF_UP
                )
            )
        }

        // giữ nguyên prefix / suffix (kể cả khoảng trắng / NBSP)
        val prefixRaw = formattedPrice.substring(0, firstDigitIndex)
        val suffixRaw = formattedPrice.substring(lastDigitIndex + 1)

        // phần số (chứa dấu phân nhóm và dấu thập phân)
        val numericPart = formattedPrice.substring(firstDigitIndex, lastDigitIndex + 1)

        // lấy fraction digits từ currency nếu có
        val currencyFractionDigits = try {
            if (currencyCode != null) Currency.getInstance(currencyCode).defaultFractionDigits
            else Currency.getInstance(Locale.getDefault()).defaultFractionDigits
        } catch (_: Exception) {
            2
        }.coerceAtLeast(0)

        // normalize một số loại khoảng trắng phổ biến
        val normalizedNumeric = numericPart
            .replace('\u00A0', ' ')   // NBSP
            .replace('\u202F', ' ')   // narrow no-break
            .replace('\u2009', ' ')   // thin space

        // vị trí '.' và ','
        val lastDot = normalizedNumeric.lastIndexOf('.')
        val lastComma = normalizedNumeric.lastIndexOf(',')

        // quyết định dấu thập phân dựa vào số chữ số sau dấu => so sánh với currencyFractionDigits
        var decimalSep: Char?
        if (currencyFractionDigits > 0) {
            decimalSep =
                if (lastDot >= 0 && (normalizedNumeric.length - lastDot - 1) == currencyFractionDigits) {
                    '.'
                } else if (lastComma >= 0 && (normalizedNumeric.length - lastComma - 1) == currencyFractionDigits) {
                    ','
                } else {
                    // nếu không chắc, chọn ký tự xuất hiện ở vị trí cuối hơn
                    when {
                        lastDot >= 0 && lastComma >= 0 -> if (lastDot > lastComma) '.' else ','
                        lastDot >= 0 -> '.'
                        lastComma >= 0 -> ','
                        else -> null
                    }
                }
        } else {
            // currency không có phân số (ví dụ VND): không coi '.' hoặc ',' là dấu thập phân
            decimalSep = null
        }

        // tìm phân cách nhóm (grouping). danh sách candidate bao gồm: . , space, NBSP, apostrophe
        val groupingCandidates = listOf('.', ',', '\u00A0', '\u202F', '\u2009', ' ', '\'', '’')

        // đếm tần suất xuất hiện mỗi candidate trong normalizedNumeric (trừ dấu thập phân đã xác định)
        val freqs = groupingCandidates.map { ch ->
            ch to normalizedNumeric.count { it == ch && (decimalSep == null || it != decimalSep) }
        }

        // chọn candidate có tần suất cao nhất (và > 0)
        val groupingSep = freqs.filter { it.second > 0 }.maxByOrNull { it.second }?.first
        // nếu không tìm được, thử lấy ký tự khác còn lại giữa '.' và ',' (nếu một trong hai tồn tại)
            ?: run {
                val other = when (decimalSep) {
                    '.' -> if (normalizedNumeric.contains(',')) ',' else null
                    ',' -> if (normalizedNumeric.contains('.')) '.' else null
                    else -> null
                }
                other
            }

        // nếu decimalSep vẫn null -> mặc định decimal='.' nhưng fractionDigits sẽ là 0 => không hiển thị phần thập phân
        val finalDecimal = decimalSep ?: '.'
        val finalGrouping = groupingSep ?: (if (finalDecimal == '.') ',' else '.')

        // tạo DecimalFormatSymbols theo ký tự phát hiện được
        val symbols = DecimalFormatSymbols().apply {
            decimalSeparator = finalDecimal
            groupingSeparator = finalGrouping
        }

        val df = DecimalFormat().apply {
            decimalFormatSymbols = symbols
            isGroupingUsed = true
            maximumFractionDigits = currencyFractionDigits
            minimumFractionDigits = currencyFractionDigits
        }

        val rounded = amount.setScale(currencyFractionDigits, RoundingMode.HALF_UP)
        val formattedNumber = df.format(rounded)

        // ghép lại đúng như original (giữ nguyên prefixRaw / suffixRaw)
        return prefixRaw + formattedNumber + suffixRaw
    }
}

