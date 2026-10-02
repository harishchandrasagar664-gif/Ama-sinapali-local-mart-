package com.example.ui.locale

import com.example.domain.model.AppLanguage
import com.example.domain.model.OrderStatus

data class AppStrings(
    // Common & Header
    val appName: String,
    val odiaGreeting: String,
    val welcomeSub: String,
    val portalBtn: String,
    val switchPortalTitle: String,
    val close: String,
    val cancel: String,
    val save: String,
    val submit: String,
    val back: String,
    val dismiss: String,
    val language: String,
    val selectLanguage: String,
    val englishName: String,
    val odiaName: String,
    val loggedAs: String,

    // Navigation
    val navHome: String,
    val navCategories: String,
    val navCart: String,
    val navOrders: String,
    val navProfile: String,

    // Home Screen
    val deliverTo: String,
    val loginOtp: String,
    val verifiedBadge: String,
    val searchPlaceholder: String,
    val matchingShops: String,
    val viewShopProducts: String,
    val shopByCategory: String,
    val allCategory: String,
    val popularShops: String,
    val trendingItems: String,
    val mins: String,
    val addToBasket: String,
    val outOfStock: String,
    val inStock: String,

    // Category Screen
    val allCategoriesTitle: String,
    val exploreCategories: String,

    // Shop & Product
    val shopDetails: String,
    val productsFromShop: String,
    val noProductsFound: String,
    val productDetails: String,
    val price: String,
    val discount: String,
    val unit: String,
    val description: String,
    val addToCartBtn: String,
    val buyNowBtn: String,

    // Cart Screen
    val yourCartTitle: String,
    val cartEmpty: String,
    val cartEmptyDesc: String,
    val exploreShopsBtn: String,
    val fromShop: String,
    val applyCoupon: String,
    val couponCodePlaceholder: String,
    val applyBtn: String,
    val removeBtn: String,
    val billSummary: String,
    val itemTotal: String,
    val deliveryFeeText: String,
    val couponDiscount: String,
    val toPay: String,
    val proceedToCheckout: String,

    // Checkout Screen
    val checkoutTitle: String,
    val deliveryAddressTitle: String,
    val savedAddressBadge: String,
    val deliveryTypeTitle: String,
    val homeDelivery: String,
    val storePickup: String,
    val paymentMethodTitle: String,
    val codMethod: String,
    val onlineMethod: String,
    val orderSummaryTitle: String,
    val placeOrderCod: String,
    val payAndPlaceOrder: String,
    val orderPlacedSuccess: String,
    val orderSuccessSub: String,

    // Orders Screen
    val myOrdersTitle: String,
    val noOrdersYet: String,
    val orderNumber: String,
    val trackOrderBtn: String,
    val deliveredBy: String,
    val orderStatusPlaced: String,
    val orderStatusAccepted: String,
    val orderStatusPreparing: String,
    val orderStatusReady: String,
    val orderStatusOutForDelivery: String,
    val orderStatusDelivered: String,
    val orderStatusCancelled: String,

    // Customer Profile
    val profileTitle: String,
    val savedDeliveryAddress: String,
    val editDeliveryAddress: String,
    val languageSectionTitle: String,
    val chooseLanguageDesc: String,
    val loginRegisterWithOtp: String,
    val switchPortalMenu: String,
    val helpComplaintMenu: String,
    val settingsAndInfo: String,
    val aboutUs: String,
    val contactUs: String,
    val returnsRefunds: String,
    val privacyPolicy: String,
    val termsConditions: String,
    val updateAddressDialogTitle: String,
    val districtLabel: String,
    val blockLabel: String,
    val gpLabel: String,
    val villageLabel: String,
    val fullAddressLabel: String,
    val logoutBtn: String,
    val logoutConfirmTitle: String,
    val logoutConfirmMessage: String,
    val activeCustomerSessionBadge: String,

    // Auth Screen
    val accountTitle: String,
    val accountSubtitle: String,
    val loginTab: String,
    val registerTab: String,
    val mobileNumberLabel: String,
    val emailLabel: String,
    val useEmailInstead: String,
    val useMobileInstead: String,
    val sendOtpBtn: String,
    val enterOtpLabel: String,
    val verifyProceedBtn: String,
    val resendOtpBtn: String,

    // Help & Complaint
    val helpSupportTitle: String,
    val adminHelplineBanner: String,
    val fileComplaintTitle: String,
    val complaintSubtitle: String,
    val subjectLabel: String,
    val descriptionLabel: String,
    val submitToAdminBtn: String
) {
    val isOdia: Boolean get() = language == "ଭାଷା"

    val loginRegisterTitle: String get() = accountTitle
    val mobileOtpMethod: String get() = if (isOdia) "📱 ମୋବାଇଲ୍ OTP (SMS)" else "📱 Mobile OTP (SMS)"
    val emailOtpMethod: String get() = if (isOdia) "✉️ ଇମେଲ୍ OTP" else "✉️ Email OTP"
    val mobileNumberField: String get() = mobileNumberLabel
    val emailAddressField: String get() = emailLabel
    val enterOtpTitle: String get() = enterOtpLabel
    val verifyOtpBtn: String get() = verifyProceedBtn
    val firstTimeRegNote: String get() = if (isOdia) "ପ୍ରଥମ ପଞ୍ଜୀକରଣ ସମୟରେ, ଭବିଷ୍ୟତର ସମସ୍ତ ଅର୍ଡର ପାଇଁ ଓଡ଼ିଶା ଜିଲ୍ଲା, ବ୍ଲକ, ଜିପି, ଗ୍ରାମ ଏବଂ ସମ୍ପୂର୍ଣ୍ଣ ଡେଲିଭରୀ ଠିକଣା ସଂରକ୍ଷଣ କରନ୍ତୁ।" else "During first registration only, collect Odisha District, Block, GP, Village and full Delivery Address to save for all future orders."
    val customerNameField: String get() = if (isOdia) "ଗ୍ରାହକଙ୍କ ସମ୍ପୂର୍ଣ୍ଣ ନାମ *" else "Customer Full Name *"
    val landmarkLabel: String get() = if (isOdia) "ନିକଟବର୍ତ୍ତୀ ସ୍ଥାନ / ଲ୍ୟାଣ୍ଡମାର୍କ (ଇଚ୍ଛାଧୀନ)" else "Landmark (Optional)"
    val saveAddressRegisterBtn: String get() = if (isOdia) "ଠିକଣା ସଂରକ୍ଷଣ ଏବଂ ପଞ୍ଜୀକରଣ ସମ୍ପୂର୍ଣ୍ଣ କରନ୍ତୁ" else "Save Address & Complete Registration"
    val mobileLabel: String get() = if (isOdia) "ମୋବାଇଲ୍ ନମ୍ବର" else "Mobile Number"
    val editBtn: String get() = if (isOdia) "ସମ୍ପାଦନ" else "Edit"
    val saveAddressBtn: String get() = save
    val cancelBtn: String get() = cancel
    val privacyPolicyTitle: String get() = privacyPolicy
    val contactUsTitle: String get() = contactUs
    val aboutUsTitle: String get() = aboutUs
    val termsTitle: String get() = termsConditions
    val returnsRefundsTitle: String get() = returnsRefunds

    val changeProfilePhoto: String get() = if (isOdia) "ପ୍ରୋଫାଇଲ୍ ଫଟୋ (DP) ବଦଳାନ୍ତୁ" else "Change Profile Photo (DP)"
    val takePhotoCamera: String get() = if (isOdia) "📷 କ୍ୟାମେରା ଦ୍ୱାରା ଫଟୋ ଉଠାନ୍ତୁ" else "📷 Take Photo with Camera"
    val chooseGallery: String get() = if (isOdia) "🖼️ ଗ୍ୟାଲେରୀରୁ ବାଛନ୍ତୁ" else "🖼️ Choose from Gallery"
    val useShopLogoAsDp: String get() = if (isOdia) "🏪 ଦୋକାନ ଫଟୋ / ଲୋଗୋ ବ୍ୟବହାର କରନ୍ତୁ" else "🏪 Use Shop Photo as DP"
    val useMartLogoAsDp: String get() = if (isOdia) "🛍️ ସିନାପାଲି ମାର୍ଟ ଲୋଗୋ ବ୍ୟବହାର କରନ୍ତୁ" else "🛍️ Use Sinapali Mart Logo as DP"
    val alsoUpdateShopPhoto: String get() = if (isOdia) "ଦୋକାନର ଫଟୋ ଭାବେ ମଧ୍ୟ ସେଟ୍ କରନ୍ତୁ" else "Also set as Shop's main photo"
    val removePhoto: String get() = if (isOdia) "🗑️ ଫଟୋ ହଟାନ୍ତୁ" else "🗑️ Remove Photo"
    val photoUpdatedSuccess: String get() = if (isOdia) "ପ୍ରୋଫାଇଲ୍ ଫଟୋ ସଫଳତାର ସହିତ ଅପଡେଟ୍ ହେଲା! 📸" else "Profile photo updated successfully! 📸"
    val cameraPermissionRequired: String get() = if (isOdia) "କ୍ୟାମେରା ଅନୁମତି ଆବଶ୍ୟକ" else "Camera permission is required"
    val updateShopPhoto: String get() = if (isOdia) "ଦୋକାନ ବ୍ୟାନର / ଫଟୋ ବଦଳାନ୍ତୁ" else "Change Shop Banner / Photo"

    fun getStatusText(status: OrderStatus): String = when (status) {
        OrderStatus.PLACED -> orderStatusPlaced
        OrderStatus.ACCEPTED -> orderStatusAccepted
        OrderStatus.PREPARING -> orderStatusPreparing
        OrderStatus.READY_FOR_PICKUP -> orderStatusReady
        OrderStatus.OUT_FOR_DELIVERY -> orderStatusOutForDelivery
        OrderStatus.DELIVERED -> orderStatusDelivered
        OrderStatus.CANCELLED -> orderStatusCancelled
    }

    fun getCategoryDisplayName(categoryName: String): String {
        if (!isOdia) return categoryName
        return when (categoryName.trim()) {
            "All" -> "ସବୁ ବର୍ଗ"
            "Clothing" -> "ପୋଷାକ ଓ ବସ୍ତ୍ର"
            "Saree" -> "ଶାଢ଼ୀ"
            "Chappal" -> "ଚପଲ ଓ ସ୍ଲିପର୍"
            "Shoes" -> "ଜୋତା"
            "Cosmetics" -> "ପ୍ରସାଧନ ସାମଗ୍ରୀ"
            "Fancy" -> "ଫ୍ୟାନ୍ସି ଓ ଉପହାର"
            "Bags" -> "ବ୍ୟାଗ୍ ଓ ଲଗେଜ୍"
            "Jewellery", "Jewelry" -> "ଗହଣା ଓ ଅଳଙ୍କାର"
            "Kids Wear" -> "ଛୁଆଙ୍କ ପୋଷାକ"
            "Men’s Wear", "Men's Wear" -> "ପୁରୁଷ ପୋଷାକ"
            "Women’s Wear", "Women's Wear" -> "ମହିଳା ପୋଷାକ"
            "Household Items" -> "ଘରୋଇ ସାମଗ୍ରୀ"
            "Grocery", "Groceries & Spices" -> "କିରାନା ସାମଗ୍ରୀ"
            "Vegetables", "Fresh Vegetables" -> "ତାଜା ପନିପରିବା"
            "Fruits", "Fresh Fruits" -> "ତାଜା ଫଳ"
            "Electronics", "Electronics & Mobile" -> "ଇଲେକ୍ଟ୍ରୋନିକ୍ସ"
            "Other" -> "ଅନ୍ୟାନ୍ୟ"
            "Fresh Vegetables & Fruits" -> "ପନିପରିବା ଓ ଫଳ"
            "Sinapali Sweets & Dairy" -> "ସିନାପାଲି ମିଠା ଓ କ୍ଷୀର"
            "Bakery & Snacks" -> "ବେକେରୀ ଓ ଜଳଖିଆ"
            "Beverages & Cold Drinks" -> "ଥଣ୍ଡା ପାନୀୟ ଓ ଜୁସ୍"
            "Handloom & Sambalpuri Clothes" -> "ସମ୍ବଲପୁରୀ ବସ୍ତ୍ର ଓ ପୋଷାକ"
            "Personal Care & Hygiene" -> "ବ୍ୟକ୍ତିଗତ ଯତ୍ନ ଓ ପ୍ରସାଧନ"
            "Home & Kitchen Essentials" -> "ଘରୋଇ ଓ ରୋଷେଇ ସାମଗ୍ରୀ"
            "Puja & Festive Items" -> "ପୂଜା ଓ ପର୍ବ ସାମଗ୍ରୀ"
            "Stationery & Books" -> "ଖାତା, ବହି ଓ ଷ୍ଟେସନାରୀ"
            "Medicine & Healthcare" -> "ସ୍ୱାସ୍ଥ୍ୟ ଓ ଔଷଧ"
            "Baby Care & Toys" -> "ଶିଶୁ ଯତ୍ନ ଓ ଖେଳନା"
            "Hardware & Electricals" -> "ହାର୍ଡୱେର ଓ ବିଦ୍ୟୁତ"
            "Paan & Refreshments", "Paan, Mukhwas & Refreshments" -> "ପାନ ଓ ମୁଖବାସ"
            else -> categoryName
        }
    }

    fun getCategoryEmoji(categoryName: String): String {
        return when (categoryName.trim()) {
            "All" -> "🛍️"
            "Clothing" -> "👕"
            "Saree" -> "🥻"
            "Chappal" -> "🩴"
            "Shoes" -> "👟"
            "Cosmetics" -> "💄"
            "Fancy" -> "🎀"
            "Bags" -> "🎒"
            "Jewellery", "Jewelry" -> "💍"
            "Kids Wear" -> "👶"
            "Men’s Wear", "Men's Wear" -> "👔"
            "Women’s Wear", "Women's Wear" -> "👗"
            "Household Items" -> "🏠"
            "Grocery", "Groceries & Spices" -> "🛒"
            "Vegetables", "Fresh Vegetables" -> "🥦"
            "Fruits", "Fresh Fruits" -> "🍎"
            "Electronics", "Electronics & Mobile" -> "⚡"
            "Other" -> "📦"
            "Fresh Vegetables & Fruits" -> "🥬"
            "Sinapali Sweets & Dairy" -> "🍮"
            "Bakery & Snacks" -> "🍪"
            "Beverages & Cold Drinks" -> "🥤"
            "Handloom & Sambalpuri Clothes" -> "🧵"
            "Personal Care & Hygiene" -> "🧴"
            "Home & Kitchen Essentials" -> "🍳"
            "Puja & Festive Items" -> "🪔"
            "Stationery & Books" -> "📚"
            "Medicine & Healthcare" -> "💊"
            "Baby Care & Toys" -> "🍼"
            "Hardware & Electricals" -> "🔧"
            "Paan & Refreshments", "Paan, Mukhwas & Refreshments" -> "🍃"
            else -> "🏷️"
        }
    }

    companion object {
        val English = AppStrings(
            appName = "Ama Sinapali Local Mart",
            odiaGreeting = "ନମସ୍କାର ସିନାପାଲି! 🙏",
            welcomeSub = "Town Fast Delivery • Vocal for Local",
            portalBtn = "Portal",
            switchPortalTitle = "Switch Ama Sinapali Portal",
            close = "Close",
            cancel = "Cancel",
            save = "Save",
            submit = "Submit",
            back = "Back",
            dismiss = "Dismiss",
            language = "Language",
            selectLanguage = "Select Language / ଭାଷା ଚୟନ",
            englishName = "English",
            odiaName = "ଓଡ଼ିଆ (Odia)",
            loggedAs = "Logged as",

            navHome = "Home",
            navCategories = "Categories",
            navCart = "Cart",
            navOrders = "Orders",
            navProfile = "Profile",

            deliverTo = "Deliver to:",
            loginOtp = "Login / OTP",
            verifiedBadge = "Verified ✓",
            searchPlaceholder = "Search Sinapali stores, vegetables, sweets...",
            matchingShops = "Matching Shops 🏪",
            viewShopProducts = "View Shop Products →",
            shopByCategory = "Shop by Category",
            allCategory = "All",
            popularShops = "Popular Sinapali Shops 🏪",
            trendingItems = "Trending Items Today 🔥",
            mins = "mins",
            addToBasket = "Add",
            outOfStock = "Out of Stock",
            inStock = "In Stock",

            allCategoriesTitle = "All Categories",
            exploreCategories = "Explore Categories",

            shopDetails = "Shop Details",
            productsFromShop = "Products from this Shop",
            noProductsFound = "No products found in this shop",
            productDetails = "Product Details",
            price = "Price",
            discount = "Discount",
            unit = "Unit",
            description = "Description",
            addToCartBtn = "Add to Cart 🛒",
            buyNowBtn = "Buy Now",

            yourCartTitle = "Your Cart 🛒",
            cartEmpty = "Your cart is empty",
            cartEmptyDesc = "Add items from Sinapali shops to order!",
            exploreShopsBtn = "Explore Shops",
            fromShop = "From",
            applyCoupon = "Apply Coupon",
            couponCodePlaceholder = "Enter coupon code (e.g. SINAPALI50)",
            applyBtn = "Apply",
            removeBtn = "Remove",
            billSummary = "Bill Summary",
            itemTotal = "Item Total",
            deliveryFeeText = "Delivery Fee (Sinapali Town)",
            couponDiscount = "Coupon Discount",
            toPay = "To Pay",
            proceedToCheckout = "Proceed to Checkout",

            checkoutTitle = "Checkout",
            deliveryAddressTitle = "Delivery Address (Odisha)",
            savedAddressBadge = "✓ Saved Address",
            deliveryTypeTitle = "Delivery Type",
            homeDelivery = "Home Delivery (Doorstep)",
            storePickup = "Store Pickup (Self collect)",
            paymentMethodTitle = "Payment Method",
            codMethod = "Cash on Delivery (Pay at Doorstep)",
            onlineMethod = "Online Payment (UPI, Cards, NetBanking)",
            orderSummaryTitle = "Order Summary",
            placeOrderCod = "Place Order (Cash on Delivery)",
            payAndPlaceOrder = "Pay & Place Order",
            orderPlacedSuccess = "Order Placed Successfully! 🎉",
            orderSuccessSub = "Delivered to your doorstep in Sinapali town & GPs.",

            myOrdersTitle = "My Orders",
            noOrdersYet = "No orders placed yet",
            orderNumber = "Order #",
            trackOrderBtn = "Track Order",
            deliveredBy = "Delivered by",
            orderStatusPlaced = "Order Placed",
            orderStatusAccepted = "Accepted by Shop",
            orderStatusPreparing = "Preparing Items",
            orderStatusReady = "Ready for Pickup",
            orderStatusOutForDelivery = "Out for Delivery",
            orderStatusDelivered = "Delivered",
            orderStatusCancelled = "Cancelled",

            profileTitle = "Customer Profile",
            savedDeliveryAddress = "Saved Delivery Address (Odisha)",
            editDeliveryAddress = "Edit Delivery Address",
            languageSectionTitle = "App Language / ଭାଷା ଚୟନ",
            chooseLanguageDesc = "Switch between English and ଓଡ଼ିଆ anytime",
            loginRegisterWithOtp = "Login / Register with OTP (Mobile or Email)",
            switchPortalMenu = "Switch Portal (Customer / Shop / Admin)",
            helpComplaintMenu = "Help & File Complaint to Admin",
            settingsAndInfo = "Settings & Information",
            aboutUs = "About Us",
            contactUs = "Contact Us",
            returnsRefunds = "Returns & Refunds Policy",
            privacyPolicy = "Privacy Policy",
            termsConditions = "Terms & Conditions",
            updateAddressDialogTitle = "Update Delivery Address",
            districtLabel = "District",
            blockLabel = "Block",
            gpLabel = "Gram Panchayat (GP)",
            villageLabel = "Village / Town",
            fullAddressLabel = "Street / House / Ward Address",
            logoutBtn = "Log Out (Active Session)",
            logoutConfirmTitle = "Log Out from Customer Account?",
            logoutConfirmMessage = "Are you sure you want to log out? Your active session will be cleared until you log in with OTP again.",
            activeCustomerSessionBadge = "Active Customer Session",

            accountTitle = "Customer Account",
            accountSubtitle = "Verified Customer Login & Registration",
            loginTab = "Login (Existing Customer)",
            registerTab = "New Registration (First Order)",
            mobileNumberLabel = "Mobile Number (10 digits)",
            emailLabel = "Email Address",
            useEmailInstead = "Use Email OTP instead",
            useMobileInstead = "Use Mobile OTP instead",
            sendOtpBtn = "Send OTP Code",
            enterOtpLabel = "Enter 6-digit OTP",
            verifyProceedBtn = "Verify OTP & Continue",
            resendOtpBtn = "Resend OTP",

            helpSupportTitle = "Help & Support",
            adminHelplineBanner = "Admin Helpline & Direct Support",
            fileComplaintTitle = "File Complaint / Query to Admin",
            complaintSubtitle = "Admin (Harishchandra Sagar) personally oversees all operations and deliveries in Sinapali.",
            subjectLabel = "Subject (e.g. Delivery Delay, Quality)",
            descriptionLabel = "Description",
            submitToAdminBtn = "Submit to Admin"
        )

        val Odia = AppStrings(
            appName = "ଆମ ସିନାପାଲି ଲୋକାଲ ମାର୍ଟ",
            odiaGreeting = "ନମସ୍କାର ସିନାପାଲି! 🙏",
            welcomeSub = "ସିନାପାଲି ତୁରନ୍ତ ଡେଲିଭରୀ • ଭୋକାଲ୍ ଫର୍ ଲୋକାଲ୍",
            portalBtn = "ପୋର୍ଟାଲ",
            switchPortalTitle = "ଆମ ସିନାପାଲି ପୋର୍ଟାଲ ବଦଳାନ୍ତୁ",
            close = "ବନ୍ଦ କରନ୍ତୁ",
            cancel = "ବାତିଲ",
            save = "ସଂରକ୍ଷଣ",
            submit = "ଦାଖଲ କରନ୍ତୁ",
            back = "ପଛକୁ",
            dismiss = "ଖାରଜ କରନ୍ତୁ",
            language = "ଭାଷା",
            selectLanguage = "ଭାଷା ଚୟନ / Select Language",
            englishName = "English",
            odiaName = "ଓଡ଼ିଆ (Odia)",
            loggedAs = "ଲଗଇନ୍ ହୋଇଛି",

            navHome = "ମୂଳ ପୃଷ୍ଠା",
            navCategories = "ବର୍ଗ",
            navCart = "କାର୍ଟ",
            navOrders = "ଅର୍ଡର",
            navProfile = "ପ୍ରୋଫାଇଲ୍",

            deliverTo = "ଡେଲିଭରୀ ଠିକଣା:",
            loginOtp = "ଲଗଇନ୍ / OTP",
            verifiedBadge = "ଯାଞ୍ଚ ହୋଇଛି ✓",
            searchPlaceholder = "ସିନାପାଲି ଦୋକାନ, ପନିପରିବା, ମିଠା ଖୋଜନ୍ତୁ...",
            matchingShops = "ମେଳ ଖାଉଥିବା ଦୋକାନ 🏪",
            viewShopProducts = "ଦୋକାନର ସାମଗ୍ରୀ ଦେଖନ୍ତୁ →",
            shopByCategory = "ବର୍ଗ ଅନୁସାରେ କିଣନ୍ତୁ",
            allCategory = "ସମସ୍ତ",
            popularShops = "ଲୋକପ୍ରିୟ ସିନାପାଲି ଦୋକାନ 🏪",
            trendingItems = "ଆଜିର ଟ୍ରେଣ୍ଡିଂ ସାମଗ୍ରୀ 🔥",
            mins = "ମିନିଟ୍",
            addToBasket = "ଯୋଡନ୍ତୁ",
            outOfStock = "ଷ୍ଟକ୍ ନାହିଁ",
            inStock = "ଉପଲବ୍ଧ ଅଛି",

            allCategoriesTitle = "ସମସ୍ତ ବର୍ଗ",
            exploreCategories = "ବର୍ଗ ଦେଖନ୍ତୁ",

            shopDetails = "ଦୋକାନ ବିବରଣୀ",
            productsFromShop = "ଏହି ଦୋକାନର ସାମଗ୍ରୀ",
            noProductsFound = "ଏହି ଦୋକାନରେ କୌଣସି ସାମଗ୍ରୀ ମିଳିଲା ନାହିଁ",
            productDetails = "ସାମଗ୍ରୀ ବିବରଣୀ",
            price = "ମୂଲ୍ୟ",
            discount = "ରିହାତି",
            unit = "ଏକକ",
            description = "ବିବରଣୀ",
            addToCartBtn = "କାର୍ଟରେ ଯୋଡନ୍ତୁ 🛒",
            buyNowBtn = "ବର୍ତ୍ତମାନ କିଣନ୍ତୁ",

            yourCartTitle = "ଆପଣଙ୍କ କାର୍ଟ 🛒",
            cartEmpty = "ଆପଣଙ୍କ କାର୍ଟ ଖାଲି ଅଛି",
            cartEmptyDesc = "ଅର୍ଡର କରିବା ପାଇଁ ସିନାପାଲି ଦୋକାନରୁ ସାମଗ୍ରୀ ଯୋଡନ୍ତୁ!",
            exploreShopsBtn = "ଦୋକାନ ଦେଖନ୍ତୁ",
            fromShop = "ଦୋକାନ",
            applyCoupon = "କୁପନ୍ ଲାଗୁ କରନ୍ତୁ",
            couponCodePlaceholder = "କୁପନ୍ କୋଡ୍ ଲେଖନ୍ତୁ (ଯଥା: SINAPALI50)",
            applyBtn = "ଲାଗୁ କରନ୍ତୁ",
            removeBtn = "ହଟାନ୍ତୁ",
            billSummary = "ବିଲ୍ ବିବରଣୀ",
            itemTotal = "ସାମଗ୍ରୀ ମୋଟ",
            deliveryFeeText = "ଡେଲିଭରୀ ଶୁଳ୍କ (ସିନାପାଲି ସହର)",
            couponDiscount = "କୁପନ୍ ରିହାତି",
            toPay = "ଦେୟ ରାଶି",
            proceedToCheckout = "ଚେକଆଉଟ୍ ଆଗକୁ ବଢ଼ନ୍ତୁ",

            checkoutTitle = "ଚେକଆଉଟ୍",
            deliveryAddressTitle = "ଡେଲିଭରୀ ଠିକଣା (ଓଡ଼ିଶା)",
            savedAddressBadge = "✓ ସଂରକ୍ଷିତ ଠିକଣା",
            deliveryTypeTitle = "ଡେଲିଭରୀ ପ୍ରକାର",
            homeDelivery = "ହୋମ୍ ଡେଲିଭରୀ (ଘର ଦୁଆରେ)",
            storePickup = "ଷ୍ଟୋର ପିକଅପ୍ (ନିଜେ ସଂଗ୍ରହ)",
            paymentMethodTitle = "ପେମେଣ୍ଟ ପ୍ରଣାଳୀ",
            codMethod = "କ୍ୟାସ ଅନ ଡେଲିଭରୀ (ହାତରେ ପାଇ ଟଙ୍କା ଦିଅନ୍ତୁ)",
            onlineMethod = "ଅନଲାଇନ୍ ପେମେଣ୍ଟ (UPI, କାର୍ଡ, ନେଟବ୍ୟାଙ୍କିଙ୍ଗ୍)",
            orderSummaryTitle = "ଅର୍ଡର ସାରାଂଶ",
            placeOrderCod = "ଅର୍ଡର କରନ୍ତୁ (କ୍ୟାସ ଅନ ଡେଲିଭରୀ)",
            payAndPlaceOrder = "ପେମେଣ୍ଟ କରି ଅର୍ଡର କରନ୍ତୁ",
            orderPlacedSuccess = "ଅର୍ଡର ସଫଳତାର ସହିତ ରଖାଗଲା! 🎉",
            orderSuccessSub = "ସିନାପାଲି ସହର ଏବଂ ନିକଟସ୍ଥ ପଞ୍ଚାୟତରେ ଆପଣଙ୍କ ଘରେ ପହଞ୍ଚାଯିବ।",

            myOrdersTitle = "ମୋର ଅର୍ଡରଗୁଡ଼ିକ",
            noOrdersYet = "ଏପର୍ଯ୍ୟନ୍ତ କୌଣସି ଅର୍ଡର ହୋଇନାହିଁ",
            orderNumber = "ଅର୍ଡର ନଂ #",
            trackOrderBtn = "ଅର୍ଡର ଟ୍ରାକ୍ କରନ୍ତୁ",
            deliveredBy = "ଡେଲିଭରୀ କରିଛନ୍ତି",
            orderStatusPlaced = "ଅର୍ଡର ହୋଇଛି",
            orderStatusAccepted = "ଦୋକାନୀ ଦ୍ୱାରା ସ୍ୱୀକୃତ",
            orderStatusPreparing = "ସାମଗ୍ରୀ ପ୍ରସ୍ତୁତ ହେଉଛି",
            orderStatusReady = "ପିକଅପ୍ ପାଇଁ ପ୍ରସ୍ତୁତ",
            orderStatusOutForDelivery = "ଡେଲିଭରୀ ପାଇଁ ବାହାରିଛି",
            orderStatusDelivered = "ଡେଲିଭରୀ ସମ୍ପନ୍ନ ହୋଇଛି",
            orderStatusCancelled = "ବାତିଲ କରାଯାଇଛି",

            profileTitle = "ଗ୍ରାହକ ପ୍ରୋଫାଇଲ୍",
            savedDeliveryAddress = "ସଂରକ୍ଷିତ ଡେଲିଭରୀ ଠିକଣା (ଓଡ଼ିଶା)",
            editDeliveryAddress = "ଡେଲିଭରୀ ଠିକଣା ବଦଳାନ୍ତୁ",
            languageSectionTitle = "ଆପ୍ ଭାଷା / App Language",
            chooseLanguageDesc = "ଇଂରାଜୀ ଏବଂ ଓଡ଼ିଆ ଭାଷା ମଧ୍ୟରେ ଯେକୌଣସି ସମୟରେ ବଦଳାନ୍ତୁ",
            loginRegisterWithOtp = "OTP ସହିତ ଲଗଇନ୍ / ପଞ୍ଜୀକରଣ (ମୋବାଇଲ୍ ବା ଇମେଲ୍)",
            switchPortalMenu = "ପୋର୍ଟାଲ ବଦଳାନ୍ତୁ (ଗ୍ରାହକ / ଦୋକାନ / ଆଡମିନ୍)",
            helpComplaintMenu = "ସାହାଯ୍ୟ ଏବଂ ଆଡମିନଙ୍କୁ ଅଭିଯୋଗ ଦାଖଲ",
            settingsAndInfo = "ସେଟିଂସ ଏବଂ ସୂଚନା",
            aboutUs = "ଆମ ବିଷୟରେ",
            contactUs = "ଯୋଗାଯୋଗ କରନ୍ତୁ",
            returnsRefunds = "ଫେରସ୍ତ ଏବଂ ରିଫଣ୍ଡ୍ ନୀତି",
            privacyPolicy = "ଗୋପନୀୟତା ନୀତି",
            termsConditions = "ନିୟମ ଏବଂ ସର୍ତ୍ତାବଳୀ",
            updateAddressDialogTitle = "ଡେଲିଭରୀ ଠିକଣା ଅଦ୍ୟତନ କରନ୍ତୁ",
            districtLabel = "ଜିଲ୍ଲା",
            blockLabel = "ବ୍ଲକ",
            gpLabel = "ଗ୍ରାମ ପଞ୍ଚାୟତ (GP)",
            villageLabel = "ଗ୍ରାମ / ସହର",
            fullAddressLabel = "ସମ୍ପୂର୍ଣ୍ଣ ଠିକଣା / ୱାର୍ଡ / ରାସ୍ତା",
            logoutBtn = "ଲଗ୍ ଆଉଟ୍ (Log Out)",
            logoutConfirmTitle = "ଗ୍ରାହକ ଆକାଉଣ୍ଟରୁ ଲଗ୍ ଆଉଟ୍ କରିବେ କି?",
            logoutConfirmMessage = "ଆପଣ ନିଶ୍ଚିତ ଭାବେ ଲଗ୍ ଆଉଟ୍ କରିବାକୁ ଚାହାଁନ୍ତି କି? ପୁନର୍ବାର OTP ଦେଇ ଲଗଇନ୍ ନକରିବା ପର୍ଯ୍ୟନ୍ତ ଏହା ଲଗ୍ ଆଉଟ୍ ରହିବ।",
            activeCustomerSessionBadge = "ସକ୍ରିୟ ଗ୍ରାହକ ସେସନ୍",

            accountTitle = "ଗ୍ରାହକ ଖାତା",
            accountSubtitle = "ଯାଞ୍ଚ ହୋଇଥିବା ଗ୍ରାହକ ଲଗଇନ୍ ଓ ପଞ୍ଜୀକରଣ",
            loginTab = "ଲଗଇନ୍ (ପୂର୍ବ ଗ୍ରାହକ)",
            registerTab = "ନୂତନ ପଞ୍ଜୀକରଣ (ପ୍ରଥମ ଅର୍ଡର)",
            mobileNumberLabel = "ମୋବାଇଲ୍ ନମ୍ବର (୧୦ ଅଙ୍କ)",
            emailLabel = "ଇମେଲ୍ ଠିକଣା",
            useEmailInstead = "ଇମେଲ୍ OTP ବ୍ୟବହାର କରନ୍ତୁ",
            useMobileInstead = "ମୋବାଇଲ୍ OTP ବ୍ୟବହାର କରନ୍ତୁ",
            sendOtpBtn = "OTP କୋଡ୍ ପଠାନ୍ତୁ",
            enterOtpLabel = "୬ ଅଙ୍କ ବିଶିଷ୍ଟ OTP ଲେଖନ୍ତୁ",
            verifyProceedBtn = "OTP ଯାଞ୍ଚ କରି ଆଗକୁ ବଢ଼ନ୍ତୁ",
            resendOtpBtn = "ପୁଣି OTP ପଠାନ୍ତୁ",

            helpSupportTitle = "ସାହାଯ୍ୟ ଏବଂ ସମର୍ଥନ",
            adminHelplineBanner = "ଆଡମିନ୍ ହେଲ୍ପଲାଇନ୍ ଏବଂ ସିଧାସଳଖ ସହାୟତା",
            fileComplaintTitle = "ଆଡମିନଙ୍କୁ ଅଭିଯୋଗ / ପ୍ରଶ୍ନ ପଠାନ୍ତୁ",
            complaintSubtitle = "ଆଡମିନ୍ (ହରିଶ୍ଚନ୍ଦ୍ର ସାଗର) ସିନାପାଲିରେ ସମସ୍ତ କାର୍ଯ୍ୟ ଏବଂ ଡେଲିଭରୀ ବ୍ୟକ୍ତିଗତ ଭାବେ ତଦାରଖ କରନ୍ତି।",
            subjectLabel = "ବିଷୟ (ଯଥା: ଡେଲିଭରୀ ବିଳମ୍ବ, ସାମଗ୍ରୀ ମାନ)",
            descriptionLabel = "ବିବରଣୀ",
            submitToAdminBtn = "ଆଡମିନଙ୍କୁ ଦାଖଲ କରନ୍ତୁ"
        )

        fun get(language: AppLanguage): AppStrings = when (language) {
            AppLanguage.ODIA -> Odia
            AppLanguage.ENGLISH -> English
        }
    }
}
