# Android Stripe SDK

Refer [Stripe](https://stripe.com/docs/payments/accept-a-payment#android)

In Gradle File -> Add this 


```
//Stripe Android SDK
implementation 'com.stripe:stripe-android:13.2.0'
//In case of handling server side code, here I'm using the Java SDK.
implementation "com.stripe:stripe-java:15.0.0"
```
Note: 13.2.0 was the latest version at the time of creating this repo.

In this repo you can find examples for two topic, 

- Making payment without storing the user card details(Ref: MakePaymentActivity.kt). 
- Making payment with storing the user card details(Ref: PreBuiltUIActivity.kt)

In order to store and retrive and process for payment [Stripe](https://stripe.com/docs/mobile/android/basic) is supporting the Pre built UI in SDK
Let's discuss about both one by one

## Making payment without storing the user card details(Ref: MakePaymentActivity.kt)

At very beginning we need to initialize the stripe.
```
  PaymentConfiguration.init(
    applicationContext,
    resources.getString(R.string.stripe_public_key)
  )
```

Coroutine Scope is used to perform the task in background thread. The following code is to be done in Server side. For the clear understanding, I'm handing the server side part in client.
```
GlobalScope.launch(Dispatchers.Main) {
    supportClass.displayLoader(resources.getString(R.string.loading))
    //In order to make the Stripe charge, we need to create confirmPaymentIntent from server side.
    //Because this will maintain the secure connection for the payment as per Stripe document.
    val paymentIntent = async(Dispatchers.IO) { confirmPaymentIntent(params) }
    //paymentIntent object --> Will return the paymentIntent which is used for making the payment with Stripe
    stripe.confirmPayment(this@MakePaymentActivity, paymentIntent.await())
    //Once the payment was success, we can get the result in onActivityResult
}
```

onActivityResult code is following
```
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
```
  
  ## Making payment with storing the user card details(Ref: PreBuiltUIActivity.kt)
  The simple over view of this flow is following
  
  - [Create Customer](https://stripe.com/docs/api/customers/create?lang=java) (Server Side)
  - [EphemeralKey](https://stripe.com/docs/mobile/android/basic#set-up-ephemeral-key) (Server Side)
  - [PaymentMethod](https://stripe.com/docs/mobile/android/basic#paymentmethodsactivity) (Client Side)
  - [PaymentIntent](https://stripe.com/docs/mobile/android/basic#complete-the-payment) (Server Side)
  - [stripe.confirmPayment](https://stripe.com/docs/mobile/android/basic#complete-the-payment) (Client Side)
  
###### [Create Customer](https://stripe.com/docs/api/customers/create?lang=java) (Server Side)
  
```
  Stripe.apiKey = "sk_test_4eC39HqLyjWDarjtT1zdp7dc";

  Map<String, Object> params = new HashMap<>();
  params.put(
    "description",
    "My First Test Customer (created for API docs)"
  );

  Customer customer = Customer.create(params);
```  
  
###### [EphemeralKey](https://stripe.com/docs/mobile/android/basic#set-up-ephemeral-key) (Server Side) Ref: EphemeralKeyProvider.kt

```
  com.stripe.Stripe.apiKey = context.resources.getString(R.string.stripe_secret_key)
  val requestOptions: RequestOptions = RequestOptions.RequestOptionsBuilder()
    .setStripeVersionOverride(apiVersion)
    .build()
  val options: MutableMap<String, Any> =
      HashMap()
  //Here I'm directly hardcoded the Stripe Customer key.
  //You can find the clear flow of creating the Customer in Server side code in https://stripe.com/docs/api/customers/create?lang=java
  options["customer"] = "cus_Gg3ukqg4YdLift"
  val key: EphemeralKey = EphemeralKey.create(options, requestOptions)
  key.rawJson.toString()
```

###### [PaymentMethod](https://stripe.com/docs/mobile/android/basic#paymentmethodsactivity) (Client Side) Ref: PreBuiltUIActivity.kt

```
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
                    }
                }
            }
        }
    }
```

###### [PaymentIntent](https://stripe.com/docs/mobile/android/basic#complete-the-payment) (Server Side) Ref: PreBuiltUIActivity.kt
```
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
  PaymentIntent.create(paymentParams)
```

###### [stripe.confirmPayment](https://stripe.com/docs/mobile/android/basic#complete-the-payment) (Client Side) Ref: PreBuiltUIActivity.kt

```
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
```          
Note: Client Secret is the value which we get from **PaymentIntent** object








  
  
  
