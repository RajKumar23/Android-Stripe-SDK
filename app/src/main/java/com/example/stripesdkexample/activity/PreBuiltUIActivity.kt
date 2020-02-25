package com.example.stripesdkexample.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.stripesdkexample.R
import com.example.stripesdkexample.utils.EphemeralKeyProvider
import com.example.stripesdkexample.utils.SupportClass
import com.google.gson.GsonBuilder
import com.stripe.android.*
import com.stripe.android.model.*
import com.stripe.android.view.PaymentMethodsActivityStarter
import com.stripe.android.view.ShippingInfoWidget
import com.stripe.model.PaymentIntent
import kotlinx.android.synthetic.main.activity_pre_built_u_i.*
import kotlinx.android.synthetic.main.content_pre_built_u_i.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

class PreBuiltUIActivity : AppCompatActivity() {

    private lateinit var paymentSession: PaymentSession
    private lateinit var stripe: Stripe
    private lateinit var supportClass: SupportClass

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        paymentSession.savePaymentSessionInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_built_u_i)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val window: Window = this.window

        // clear FLAG_TRANSLUCENT_STATUS flag:
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        // finally change the color
        // finally change the color
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)

        PaymentConfiguration.init(
            applicationContext,
            resources.getString(R.string.stripe_public_key)
        )
        supportClass = SupportClass(this)

        if (supportClass.checkInternetStatus()) {
            //For starting the process with Pre Built UI activity, first we need EphemeralKey
            //Which is need to process in Server side.
            //For clear understanding, I'm handling it in Client side
            //Now refer the EphemeralKeyProvider.class
            CustomerSession.initCustomerSession(this, EphemeralKeyProvider(this))

            paymentSession = PaymentSession(
                this,
                createPaymentSessionConfig()
            )

            // Create your listener and your configuration
            // ...
            // It's fine if the savedInstanceState is null.
            paymentSession.init(
                createPaymentSessionListener(),
                savedInstanceState
            )
            PaymentMethodsActivityStarter(this).startForResult()
        } else {
            supportClass.showNoInternetConnectAlert()
        }
    }

    private fun createPaymentSessionConfig(): PaymentSessionConfig {
        return PaymentSessionConfig.Builder()

            // hide the phone field on the shipping information form
            .setHiddenShippingInfoFields(
                ShippingInfoWidget.CustomizableShippingField.PHONE_FIELD
            )

            // make the address line 2 field optional
            .setOptionalShippingInfoFields(
                ShippingInfoWidget.CustomizableShippingField.ADDRESS_LINE_TWO_FIELD
            )

            // specify an address to pre-populate the shipping information form
            .setPrepopulatedShippingInfo(
                ShippingInformation(
                    Address.Builder()
                        .setLine1("123 Market St")
                        .setCity("San Francisco")
                        .setState("CA")
                        .setPostalCode("94107")
                        .setCountry("US")
                        .build(),
                    "Rajkumar Rajan",
                    "9876543210"
                )
            )

            // collect shipping information
            .setShippingInfoRequired(true)

            // collect shipping method
            .setShippingMethodsRequired(true)

            // specify the payment method types that the customer can use;
            // defaults to PaymentMethod.Type.Card
            .setPaymentMethodTypes(
                listOf(PaymentMethod.Type.Card)
            )

            // only allow US and Canada shipping addresses
            .setAllowedShippingCountryCodes(
                setOf("US", "CA")
            )

            // specify a layout to display under the payment collection form
            .setAddPaymentMethodFooter(R.layout.add_payment_method_footer)

            // specify the shipping information validation delegate
            .setShippingInformationValidator(AppShippingInfoValidator())

            // specify the shipping methods factory delegate
            .setShippingMethodsFactory(AppShippingMethodsFactory())

            // if `true`, will show "Google Pay" as an option on the
            // Payment Methods selection screen
            .setShouldShowGooglePay(true)

            .build()
    }

    private class AppShippingInfoValidator : PaymentSessionConfig.ShippingInformationValidator {
        override fun getErrorMessage(
            shippingInformation: ShippingInformation
        ): String {
            return "A US address is required"
        }

        override fun isValid(
            shippingInformation: ShippingInformation
        ): Boolean {
            return Locale.US.country == shippingInformation.address?.country
        }
    }

    private class AppShippingMethodsFactory : PaymentSessionConfig.ShippingMethodsFactory {
        override fun create(shippingInformation: ShippingInformation): List<ShippingMethod> {
            return listOf(
                ShippingMethod(
                    label = "UPS Ground",
                    identifier = "ups-ground",
                    detail = "Arrives in 3-5 days",
                    amount = 0,
                    currency = Currency.getInstance(Locale.UK)
                ),
                ShippingMethod(
                    label = "FedEx",
                    identifier = "fedex",
                    detail = "Arrives tomorrow",
                    amount = 599,
                    currency = Currency.getInstance(Locale.UK)
                )
            )
        }
    }

    private fun createPaymentSessionListener(): PaymentSession.PaymentSessionListener {
        return object : PaymentSession.PaymentSessionListener {
            override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
                Log.e("onCommunicatingState", isCommunicating.toString())
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                Log.e("onCommunicatingState", errorMessage)
            }

            override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
                val paymentMethod: PaymentMethod? = data.paymentMethod
                if (paymentMethod != null) {
                    Log.e("onCommunicatingState", paymentMethod.id!!)
                }

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            when (requestCode) {
                PaymentMethodsActivityStarter.REQUEST_CODE -> {

                    val result = PaymentMethodsActivityStarter.Result.fromIntent(data)
                    val paymentMethod = result?.paymentMethod
                    if (paymentMethod != null) {
                        //Once User select the card in Payment Method screen, we'll receive the paymentMethod here
                        Log.e("PaymentMethod", paymentMethod.toString())

                        GlobalScope.launch(Dispatchers.Main) {
                            supportClass.displayLoader("Preparing you for payment.")
                            //Payment Intent is need to handle in Server side
                            val paymentIntentValue =
                                async(Dispatchers.IO) { generatePaymentIntent() }

                            com.stripe.Stripe.apiKey =
                                resources.getString(R.string.stripe_secret_key)
                            val context: Context = applicationContext
                            stripe = Stripe(
                                context,
                                PaymentConfiguration.getInstance(context).publishableKey
                            )
                            //This is place where we request the Stripe to make the charge using the PaymentMethod and Client Secret.
                            //The request code is 50000
                            val confirmParams =
                                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                                    paymentMethod.id!!, paymentIntentValue.await().clientSecret,
                                    "https://www.google.com/"
                                )

                            try {
                                stripe.confirmPayment(this@PreBuiltUIActivity, confirmParams)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                50000 -> {
                    //This is the block where can the check charge was success or not.
                    supportClass.dismissLoader()
                    supportClass.displayLoader("Payment completed! Confirming your order!")
                    stripe.onPaymentResult(
                        requestCode,
                        data,
                        object : ApiResultCallback<PaymentIntentResult> {
                            override fun onSuccess(result: PaymentIntentResult) {
                                supportClass.dismissLoader()
                                val paymentIntent = result.intent
                                val status = paymentIntent.status
                                if (status == StripeIntent.Status.Succeeded) {
                                    val gSon = GsonBuilder().setPrettyPrinting().create()
                                    Log.e("Stripe ", "Success")
                                    val paymentIntentString = gSon.toJson(paymentIntent)
                                    val paymentIntentJSON = JSONObject(paymentIntentString)
                                    Log.e("Res ", paymentIntentString)
                                    textViewResponse.text = paymentIntentString
                                    //If it was success, we can get the response from the Stripe.
                                } else {
                                    Log.e(
                                        "Stripe",
                                        "Payment failed " + paymentIntent.lastPaymentError?.message
                                    )
                                }
                            }

                            override fun onError(e: Exception) {
                                supportClass.dismissLoader()
                                Log.e(
                                    "Stripe",
                                    "Payment failed $e"
                                )
                            }
                        })
                }
                else -> {
                    paymentSession.handlePaymentData(requestCode, resultCode, data)
                }
            }
        }
    }

    private suspend fun generatePaymentIntent(): PaymentIntent {
        return GlobalScope.async(Dispatchers.IO) {
            com.stripe.Stripe.apiKey = resources.getString(R.string.stripe_secret_key)
            val paymentMethodTypes: MutableList<Any> = ArrayList()
            paymentMethodTypes.add("card")
            val paymentParams: MutableMap<String, Any> = HashMap()
            //2000 means 20.00
            //As per Stripe documentation we couldn't pass float value as param.
            paymentParams["amount"] = 10000
            paymentParams["currency"] = "usd"
            //Here I'm directly hardcoded the Stripe Customer key.
            //You can find the clear flow of creating the Customer in Server side code in https://stripe.com/docs/api/customers/create?lang=java
            paymentParams["customer"] = "cus_Gg3ukqg4YdLift"
            paymentParams["payment_method_types"] = paymentMethodTypes

            return@async PaymentIntent.create(paymentParams)
        }.await()
    }
}
