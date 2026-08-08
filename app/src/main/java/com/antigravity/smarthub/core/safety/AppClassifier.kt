package com.antigravity.smarthub.core.safety

enum class AppClassification {
    NEVER_TOUCH,
    PROTECTED,
    NORMAL,
    BACKGROUND_RESTRICTABLE,
    AGGRESSIVELY_RESTRICTABLE
}

class AppClassifier {

    private val neverTouchPackages = setOf(
        "com.antigravity.smarthub",
        "com.sec.android.app.launcher",
        "com.android.systemui",
        "com.android.phone",
        "com.samsung.android.incallui",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.sec.android.app.clockpackage",
        "com.google.android.deskclock",
        "com.android.mms",
        "com.samsung.android.messaging"
    )

    private val protectedPackages = setOf(
        // Messaging & Communication
        "com.whatsapp",
        "org.telegram.messenger",
        "org.thoughtcrime.securesms",
        "com.microsoft.teams",
        "com.microsoft.office.outlook",
        "com.google.android.gm",

        // Banking & Payments
        "net.one97.paytm",
        "com.phonepe.app",
        "com.google.android.apps.nfc.payment",
        "com.axis.mobile",
        "in.hsbc.hsbcindia",
        "com.sbi.upi",
        "com.icicibank.mobile",
        "com.hdfcbank.payzapp",

        // Auth & Identity
        "in.gov.uidai.facerd",
        "com.digilocker.android",
        "com.authy.authy",
        "com.google.android.apps.authenticator2",
        "com.onepassword.android",
        "com.x8bit.bitwarden",

        // Navigation & Delivery
        "com.google.android.apps.maps",
        "com.waze",
        "com.application.zomato",
        "com.swiggy.consumer",
        "com.ubercab"
    )

    fun classifyApp(packageName: String): AppClassification {
        val lower = packageName.lowercase()
        return when {
            neverTouchPackages.contains(lower) -> AppClassification.NEVER_TOUCH
            protectedPackages.contains(lower) -> AppClassification.PROTECTED
            lower.contains("launcher") || lower.contains("systemui") -> AppClassification.NEVER_TOUCH
            lower.contains("bank") || lower.contains("pay") || lower.contains("auth") -> AppClassification.PROTECTED
            else -> AppClassification.NORMAL
        }
    }

    fun isProtected(packageName: String): Boolean {
        val classification = classifyApp(packageName)
        return classification == AppClassification.NEVER_TOUCH || classification == AppClassification.PROTECTED
    }
}
