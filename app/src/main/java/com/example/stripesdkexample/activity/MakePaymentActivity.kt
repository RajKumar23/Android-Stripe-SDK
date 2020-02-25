package com.example.stripesdkexample.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.stripesdkexample.R
import com.example.stripesdkexample.utils.SupportClass
import com.google.gson.GsonBuilder
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.model.PaymentIntent
import kotlinx.android.synthetic.main.activity_make_payment.*
import kotlinx.android.synthetic.main.content_make_payment.*
import kotlinx.coroutines.*
import java.lang.ref.WeakReference


class MakePaymentActivity : AppCompatActivity() {

    private lateinit var stripe: Stripe
    private lateinit var supportClass: SupportClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_payment)

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

        //At very beginning we need to initialize the stripe.
        PaymentConfiguration.init(
            applicationContext,
            resources.getString(R.string.stripe_public_key)
        )

        //
        supportClass = SupportClass(this)


        payButton.setOnClickListener {
            // cardInputWidget will have current user typed card details.
            //Before processing with card, we need to check where it is empty or not
            if (cardInputWidget.paymentMethodCreateParams != null) {
                if (supportClass.checkInternetStatus()) {
                    val params: PaymentMethodCreateParams =
                        cardInputWidget.paymentMethodCreateParams!!

                    val context: Context = applicationContext
                    stripe = Stripe(
                        context,
                        PaymentConfiguration.getInstance(context).publishableKey
                    )

                    //Coroutine Scope is used to perform the task in background thread.
                    //The following code is to be done in Server side.
                    //For the clear understanding, I'm handing the server side part in client.
                    GlobalScope.launch(Dispatchers.Main) {
                        supportClass.displayLoader(resources.getString(R.string.loading))
                        //In order to make the Stripe charge, we need to create confirmPaymentIntent from server side.
                        //Because this will maintain the secure connection for the payment as per Stripe document.
                        val paymentIntent = async(Dispatchers.IO) { confirmPaymentIntent(params) }
                        //paymentIntent object --> Will return the paymentIntent which is used for making the payment with Stripe
                        stripe.confirmPayment(this@MakePaymentActivity, paymentIntent.await())
                        //Once the payment was success, we can get the result in onActivityResult
                    }
                } else {
                    supportClass.showNoInternetConnectAlert()
                }

            } else {
                supportClass.shortToast("Please enter valid card details")
            }
        }
    }

    private suspend fun confirmPaymentIntent(params: PaymentMethodCreateParams): ConfirmPaymentIntentParams{
        return withContext(Dispatchers.IO) {
            com.stripe.Stripe.apiKey = resources.getString(R.string.stripe_secret_key)

            val paymentMethodTypes: MutableList<Any> = ArrayList()
            paymentMethodTypes.add("card")
            val paymentParams: MutableMap<String, Any> = HashMap()
            //2000 means 20.00
            //As per Stripe documentation we couldn't pass float value as param.
            paymentParams["amount"] = 2000
            paymentParams["currency"] = "usd"
            paymentParams["payment_method_types"] = paymentMethodTypes


            val paymentIntent: PaymentIntent = PaymentIntent.create(paymentParams)
            return@withContext ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                params,
                paymentIntent.clientSecret
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /*val weakActivity = WeakReference<Activity>(this)*/

        supportClass.dismissLoader()
        // Handle the result of stripe.confirmPayment
        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {
                val paymentIntent = result.intent
                val status = paymentIntent.status
                if (status == StripeIntent.Status.Succeeded) {
                    val gSon = GsonBuilder().setPrettyPrinting().create()
                    supportClass.shortToast("Stripe payment success")
                    Log.e("Stripe ", "Success")
                    Log.e("Res ", gSon.toJson(paymentIntent))
                    textViewResponse.text = gSon.toJson(paymentIntent)
                } else {
                    supportClass.shortToast("Stripe payment fail")
                    Log.e(
                        "Stripe",
                        "Payment failed " + paymentIntent.lastPaymentError?.message
                    )
                }
            }

            override fun onError(e: Exception) {
                supportClass.shortToast("Stripe payment connection error")
                Log.e(
                    "Stripe",
                    "Payment failed $e"
                )
            }
        })
    }
}
