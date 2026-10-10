package com.example.data

import com.example.model.AddressItem
import com.example.model.CartItem
import com.example.model.CategoryItem
import com.example.model.GroceryCategory
import com.example.model.GrocerySubcategory
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.RecipeItem
import com.example.model.WalletTransaction

object SampleData {

    // Pure Fruits & Vegetables Categories
    val groceryCategories = listOf(
        GroceryCategory(
            id = "cat_veg",
            name = "Fresh Vegetables",
            shortName = "Vegetables",
            emoji = "🥦",
            badge = "Farm Fresh",
            heroTagline = "Harvested at 5 AM • 100% Pesticide Tested Daily",
            bgTintHex = 0xFFE8F5E9,
            primaryColorHex = 0xFF2E7D32,
            subcategories = listOf(
                GrocerySubcategory("sub_veg_all", "All Veggies", "🥬"),
                GrocerySubcategory("sub_veg_leafy", "Leafy Greens & Herbs", "🌿", "Hydroponic"),
                GrocerySubcategory("sub_veg_roots", "Roots & Daily Essentials", "🧅", "Daily Need"),
                GrocerySubcategory("sub_veg_tomatoes", "Tomatoes & Cucumbers", "🍅", "Juicy"),
                GrocerySubcategory("sub_veg_gourds", "Gourds & Beans", "🥒", "Crisp"),
                GrocerySubcategory("sub_veg_organic", "Certified Organic", "🌱", "100% Pure")
            )
        ),
        GroceryCategory(
            id = "cat_fruits",
            name = "Fresh Fruits",
            shortName = "Fruits",
            emoji = "🍎",
            badge = "Direct Orchards",
            heroTagline = "Naturally Ripened • Sweet, Crisp & Nutrient Rich",
            bgTintHex = 0xFFFFF3E0,
            primaryColorHex = 0xFFE65100,
            subcategories = listOf(
                GrocerySubcategory("sub_fruit_all", "All Fruits", "🍎"),
                GrocerySubcategory("sub_fruit_bananas", "Bananas & Melons", "🍌", "Sweet"),
                GrocerySubcategory("sub_fruit_apples", "Apples & Pears", "🍏", "Shimla Crisp"),
                GrocerySubcategory("sub_fruit_citrus", "Citrus & Immunity", "🍊", "Vitamin C"),
                GrocerySubcategory("sub_fruit_tropical", "Tropical & Exotics", "🥭", "Fresh Batch"),
                GrocerySubcategory("sub_fruit_pomegranate", "Pomegranate & Papaya", "🫐", "Antioxidants")
            )
        ),
        GroceryCategory(
            id = "cat_organic",
            name = "Organic Farm Produce",
            shortName = "Organic",
            emoji = "🌱",
            badge = "Pesticide Free",
            heroTagline = "Zero Chemicals • 100% Naturally Grown",
            bgTintHex = 0xFFE0F2F1,
            primaryColorHex = 0xFF00796B,
            subcategories = listOf(
                GrocerySubcategory("sub_org_all", "All Organic", "🌱"),
                GrocerySubcategory("sub_org_veg", "Organic Greens & Veggies", "🥦"),
                GrocerySubcategory("sub_org_fruits", "Organic Fruits", "🍎")
            )
        ),
        GroceryCategory(
            id = "cat_herbs",
            name = "Herbs & Seasonings",
            shortName = "Herbs",
            emoji = "🌿",
            badge = "Aromatic",
            heroTagline = "Fresh Coriander, Mint, Curry Leaves & Ginger",
            bgTintHex = 0xFFF1F8E9,
            primaryColorHex = 0xFF33691E,
            subcategories = listOf(
                GrocerySubcategory("sub_herb_all", "All Herbs", "🌿"),
                GrocerySubcategory("sub_herb_daily", "Daily Herbs (Dhaniya & Pudina)", "🍃"),
                GrocerySubcategory("sub_herb_seasoning", "Ginger, Garlic & Chillies", "🧄")
            )
        ),
        GroceryCategory(
            id = "cat_combos",
            name = "Value Combos & Baskets",
            shortName = "Combos",
            emoji = "🧺",
            badge = "Save 25%",
            heroTagline = "Curated Weekly Salad, Curry & Fruit Boxes",
            bgTintHex = 0xFFFFFDE7,
            primaryColorHex = 0xFFF57F17,
            subcategories = listOf(
                GrocerySubcategory("sub_combo_all", "All Combos", "🧺"),
                GrocerySubcategory("sub_combo_salad", "Salad Baskets", "🥗"),
                GrocerySubcategory("sub_combo_kitchen", "Daily Cooking Baskets", "🥘")
            )
        )
    )

    // Legacy categories list
    val categories = listOf(
        CategoryItem("cat_veg", "Fresh Vegetables", "Farm Produce", listOf("All", "Leafy Greens", "Roots & Onions", "Tomatoes", "Organic"), "🥦", 0xFFE8F5E9),
        CategoryItem("cat_fruits", "Fresh Fruits", "Farm Produce", listOf("All", "Bananas", "Apples & Pears", "Citrus", "Tropical"), "🍎", 0xFFFFF3E0),
        CategoryItem("cat_organic", "Certified Organic", "Farm Produce", listOf("All", "Organic Greens", "Organic Fruits"), "🌱", 0xFFE0F2F1),
        CategoryItem("cat_herbs", "Herbs & Seasonings", "Farm Produce", listOf("All", "Coriander & Mint", "Ginger & Garlic"), "🌿", 0xFFF1F8E9),
        CategoryItem("cat_combos", "Produce Combos", "Farm Produce", listOf("All", "Salad Basket", "Weekly Veggie Box"), "🧺", 0xFFFFFDE7)
    )

    // Complete Fruits & Vegetables Catalog
    val products = listOf(
        // === VEGETABLES ===
        Product(
            id = "p_coriander",
            name = "Fresh Coriander Bunch (Kothmir)",
            category = "cat_veg",
            subcategory = "Leafy Greens & Herbs",
            price = 12.0,
            originalPrice = 20.0,
            unit = "100 g",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 890,
            discountPercent = 40,
            emoji = "🌿",
            description = "Aromatic, crispy fresh green coriander harvested at dawn from local hydroponic farms.",
            badge = "40% OFF",
            isOrganic = true
        ),
        Product(
            id = "p_spinach",
            name = "Baby Spinach / Palak Leaves",
            category = "cat_veg",
            subcategory = "Leafy Greens & Herbs",
            price = 22.0,
            originalPrice = 30.0,
            unit = "250 g",
            deliveryMinutes = 10,
            rating = 4.9f,
            ratingCount = 640,
            discountPercent = 26,
            emoji = "🥬",
            description = "Tender, triple-washed green spinach leaves, 100% soil-free and iron-packed.",
            badge = "Hydroponic",
            isOrganic = true
        ),
        Product(
            id = "p_mint",
            name = "Fresh Mint Leaves (Pudina)",
            category = "cat_veg",
            subcategory = "Leafy Greens & Herbs",
            price = 15.0,
            originalPrice = 22.0,
            unit = "100 g",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 420,
            discountPercent = 31,
            emoji = "🍃",
            description = "Fragrant, deeply refreshing green mint leaves for detox juices, chutneys, and teas.",
            badge = "Farm Pick",
            isOrganic = true
        ),
        Product(
            id = "p_fenugreek",
            name = "Fresh Methi (Fenugreek Leaves)",
            category = "cat_veg",
            subcategory = "Leafy Greens & Herbs",
            price = 18.0,
            originalPrice = 25.0,
            unit = "200 g",
            deliveryMinutes = 10,
            rating = 4.7f,
            ratingCount = 310,
            discountPercent = 28,
            emoji = "🌱",
            description = "Tender methi greens with gentle pleasant bitterness, ideal for parathas and curries.",
            isOrganic = true
        ),
        Product(
            id = "p_onion",
            name = "Fresh Red Onions (Eerulli)",
            category = "cat_veg",
            subcategory = "Roots & Daily Essentials",
            price = 36.0,
            originalPrice = 45.0,
            unit = "1 kg",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 1420,
            discountPercent = 20,
            emoji = "🧅",
            description = "Crispy, hand-sorted pungent red onions, freshly cured for great shelf life.",
            badge = "Bestseller"
        ),
        Product(
            id = "p_potato",
            name = "Pahari Jyoti Potatoes (Aloo)",
            category = "cat_veg",
            subcategory = "Roots & Daily Essentials",
            price = 34.0,
            originalPrice = 40.0,
            unit = "1 kg",
            deliveryMinutes = 10,
            rating = 4.7f,
            ratingCount = 1100,
            discountPercent = 15,
            emoji = "🥔",
            description = "Clean mountain potatoes with thin skin, excellent for boiling, roasting, and curries.",
            badge = "Kitchen Must"
        ),
        Product(
            id = "p_carrot",
            name = "Ooty Orange Sweet Carrots",
            category = "cat_veg",
            subcategory = "Roots & Daily Essentials",
            price = 28.0,
            originalPrice = 35.0,
            unit = "500 g",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 780,
            discountPercent = 20,
            emoji = "🥕",
            description = "Sweet, crunchy washed carrots grown in the cool heights of Ooty.",
            badge = "Farm Fresh",
            isOrganic = true
        ),
        Product(
            id = "p_ginger",
            name = "Fresh Mountain Ginger (Adrak)",
            category = "cat_veg",
            subcategory = "Roots & Daily Essentials",
            price = 25.0,
            originalPrice = 32.0,
            unit = "150 g",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 490,
            discountPercent = 21,
            emoji = "🫚",
            description = "Fibrous, fiery fresh ginger root with strong aroma."
        ),
        Product(
            id = "p_garlic",
            name = "Desi Garlic Cloves (Lehsun)",
            category = "cat_veg",
            subcategory = "Roots & Daily Essentials",
            price = 35.0,
            originalPrice = 45.0,
            unit = "200 g",
            deliveryMinutes = 10,
            rating = 4.7f,
            ratingCount = 520,
            discountPercent = 22,
            emoji = "🧄",
            description = "Tight-bulbed, aromatic garlic cloves rich in allicin."
        ),
        Product(
            id = "p_tomato",
            name = "Hybrid Fresh Red Tomatoes",
            category = "cat_veg",
            subcategory = "Tomatoes & Cucumbers",
            price = 32.0,
            originalPrice = 40.0,
            unit = "1 kg",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 1350,
            discountPercent = 20,
            emoji = "🍅",
            description = "Plump, ruby-red farm-fresh tomatoes with sweet pulp and firm skin.",
            badge = "Farm Fresh",
            isOrganic = true
        ),
        Product(
            id = "p_cherry_tomatoes",
            name = "Hydroponic Cherry Tomatoes",
            category = "cat_veg",
            subcategory = "Tomatoes & Cucumbers",
            price = 45.0,
            originalPrice = 60.0,
            unit = "250 g",
            deliveryMinutes = 10,
            rating = 4.9f,
            ratingCount = 380,
            discountPercent = 25,
            emoji = "🍅",
            description = "Bite-sized sweet red cherry tomatoes bursting with flavor.",
            badge = "Organic",
            isOrganic = true
        ),
        Product(
            id = "p_cucumber",
            name = "English Seedless Green Cucumber",
            category = "cat_veg",
            subcategory = "Tomatoes & Cucumbers",
            price = 24.0,
            originalPrice = 30.0,
            unit = "500 g",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 610,
            discountPercent = 20,
            emoji = "🥒",
            description = "Crisp, thin-skinned seedless cucumber for daily salads and hydration."
        ),
        Product(
            id = "p_capsicum",
            name = "Crisp Green Bell Capsicum",
            category = "cat_veg",
            subcategory = "Tomatoes & Cucumbers",
            price = 26.0,
            originalPrice = 35.0,
            unit = "250 g",
            deliveryMinutes = 10,
            rating = 4.7f,
            ratingCount = 470,
            discountPercent = 25,
            emoji = "🫑",
            description = "Crunchy, thick green bell peppers for stir fries and salads."
        ),
        Product(
            id = "p_ladies_finger",
            name = "Tender Green Bhindi (Okra)",
            category = "cat_veg",
            subcategory = "Gourds & Beans",
            price = 28.0,
            originalPrice = 38.0,
            unit = "500 g",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 590,
            discountPercent = 26,
            emoji = "🌿",
            description = "Tender, snap-fresh green bhindi selected without tough fibers."
        ),
        Product(
            id = "p_bottle_gourd",
            name = "Fresh Lauki (Bottle Gourd)",
            category = "cat_veg",
            subcategory = "Gourds & Beans",
            price = 22.0,
            originalPrice = 30.0,
            unit = "1 pc (700-900 g)",
            deliveryMinutes = 10,
            rating = 4.7f,
            ratingCount = 340,
            discountPercent = 26,
            emoji = "🥒",
            description = "Pale green, light tender lauki for healthy juices and curries."
        ),
        Product(
            id = "p_organic_broccoli",
            name = "Certified Organic Broccoli Head",
            category = "cat_veg",
            subcategory = "Certified Organic",
            price = 55.0,
            originalPrice = 75.0,
            unit = "1 pc (350 g)",
            deliveryMinutes = 10,
            rating = 4.9f,
            ratingCount = 420,
            discountPercent = 26,
            emoji = "🥦",
            description = "100% chemical-free dense green broccoli crown, rich in sulforaphane.",
            badge = "Organic",
            isOrganic = true
        ),

        // === FRUITS ===
        Product(
            id = "p_banana",
            name = "Robusta Golden Bananas",
            category = "cat_fruits",
            subcategory = "Bananas & Melons",
            price = 35.0,
            originalPrice = 42.0,
            unit = "500 g (3-4 pcs)",
            deliveryMinutes = 10,
            rating = 4.9f,
            ratingCount = 1250,
            discountPercent = 16,
            emoji = "🍌",
            description = "Naturally ripened sweet bananas packed with energy and potassium.",
            badge = "Bestseller",
            isOrganic = true
        ),
        Product(
            id = "p_yellaki_banana",
            name = "Royal Yellaki Miniature Bananas",
            category = "cat_fruits",
            subcategory = "Bananas & Melons",
            price = 48.0,
            originalPrice = 58.0,
            unit = "500 g (6-8 pcs)",
            deliveryMinutes = 10,
            rating = 4.9f,
            ratingCount = 890,
            discountPercent = 17,
            emoji = "🍌",
            description = "Tiny, intensely sweet aromatic bananas beloved by kids and families.",
            badge = "Sweetest",
            isOrganic = true
        ),
        Product(
            id = "p_watermelon",
            name = "Kiran Sweet Red Watermelon",
            category = "cat_fruits",
            subcategory = "Bananas & Melons",
            price = 59.0,
            originalPrice = 75.0,
            unit = "1 pc (1.8-2.2 kg)",
            deliveryMinutes = 12,
            rating = 4.8f,
            ratingCount = 760,
            discountPercent = 21,
            emoji = "🍉",
            description = "Sugar-sweet ruby red watermelon chilled and ready to enjoy.",
            badge = "Hydration"
        ),
        Product(
            id = "p_muskmelon",
            name = "Honey Dew Sweet Muskmelon",
            category = "cat_fruits",
            subcategory = "Bananas & Melons",
            price = 45.0,
            originalPrice = 55.0,
            unit = "1 pc (800-1000 g)",
            deliveryMinutes = 10,
            rating = 4.7f,
            ratingCount = 380,
            discountPercent = 18,
            emoji = "🍈",
            description = "Aromatic orange-fleshed muskmelon loaded with vitamins."
        ),
        Product(
            id = "p_apple",
            name = "Shimla Royal Crisp Apples",
            category = "cat_fruits",
            subcategory = "Apples & Pears",
            price = 120.0,
            originalPrice = 145.0,
            unit = "4 pcs (500 g)",
            deliveryMinutes = 12,
            rating = 4.8f,
            ratingCount = 920,
            discountPercent = 17,
            emoji = "🍎",
            description = "Crisp, sweet, hand-graded mountain apples with natural waxless blush.",
            badge = "Fresh Pick"
        ),
        Product(
            id = "p_green_apple",
            name = "Granny Smith Tart Green Apples",
            category = "cat_fruits",
            subcategory = "Apples & Pears",
            price = 135.0,
            originalPrice = 160.0,
            unit = "4 pcs (500 g)",
            deliveryMinutes = 12,
            rating = 4.7f,
            ratingCount = 340,
            discountPercent = 15,
            emoji = "🍏",
            description = "Crisp, tangy green apples perfect for juicing and refreshing salads."
        ),
        Product(
            id = "p_pears",
            name = "Kashmir Nakh Sweet Pears",
            category = "cat_fruits",
            subcategory = "Apples & Pears",
            price = 78.0,
            originalPrice = 95.0,
            unit = "500 g (3 pcs)",
            deliveryMinutes = 10,
            rating = 4.7f,
            ratingCount = 280,
            discountPercent = 18,
            emoji = "🍐",
            description = "Juicy, crunchy white-fleshed pears with delicate floral sweetness."
        ),
        Product(
            id = "p_orange",
            name = "Nagpur Sweet Santra Oranges",
            category = "cat_fruits",
            subcategory = "Citrus & Immunity",
            price = 65.0,
            originalPrice = 80.0,
            unit = "1 kg (5-6 pcs)",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 610,
            discountPercent = 19,
            emoji = "🍊",
            description = "Sweet, pulpy Indian table oranges loaded with natural Vitamin C.",
            badge = "Immunity"
        ),
        Product(
            id = "p_mosambi",
            name = "Sweet Lime (Mosambi)",
            category = "cat_fruits",
            subcategory = "Citrus & Immunity",
            price = 68.0,
            originalPrice = 85.0,
            unit = "1 kg",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 540,
            discountPercent = 20,
            emoji = "🍋",
            description = "Mild, non-acidic sweet lime for refreshing fresh juices."
        ),
        Product(
            id = "p_papaya",
            name = "Taiwan Red Lady Sweet Papaya",
            category = "cat_fruits",
            subcategory = "Tropical & Exotics",
            price = 48.0,
            originalPrice = 60.0,
            unit = "1 pc (800-1000 g)",
            deliveryMinutes = 10,
            rating = 4.8f,
            ratingCount = 490,
            discountPercent = 20,
            emoji = "🥭",
            description = "Golden orange flesh, melt-in-mouth sweet and great for digestion.",
            badge = "Digestive",
            isOrganic = true
        ),
        Product(
            id = "p_pomegranate",
            name = "Arakta Ruby Red Pomegranate (Anar)",
            category = "cat_fruits",
            subcategory = "Pomegranate & Papaya",
            price = 98.0,
            originalPrice = 120.0,
            unit = "2 pcs (400 g)",
            deliveryMinutes = 10,
            rating = 4.9f,
            ratingCount = 630,
            discountPercent = 18,
            emoji = "🫐",
            description = "Deep crimson sweet arils, packed with powerful antioxidants.",
            badge = "Antioxidant"
        ),
        Product(
            id = "p_guava",
            name = "Allahabad White Sweet Guava",
            category = "cat_fruits",
            subcategory = "Tropical & Exotics",
            price = 45.0,
            originalPrice = 55.0,
            unit = "500 g",
            deliveryMinutes = 10,
            rating = 4.7f,
            ratingCount = 290,
            discountPercent = 18,
            emoji = "🍏",
            description = "Crunchy, aromatic white-fleshed guava with high dietary fiber."
        ),

        // === COMBOS ===
        Product(
            id = "p_salad_box",
            name = "Farm Salad Crunch Basket",
            category = "cat_combos",
            subcategory = "Salad Baskets",
            price = 85.0,
            originalPrice = 115.0,
            unit = "1 Box (Tomatoes, Cucumber, Carrots, Mint)",
            deliveryMinutes = 12,
            rating = 4.9f,
            ratingCount = 410,
            discountPercent = 26,
            emoji = "🥗",
            description = "Complete ready-to-chop fresh salad essentials box.",
            badge = "Save 26%",
            isOrganic = true
        ),
        Product(
            id = "p_fruit_basket",
            name = "Daily Vitamin Fruit Basket",
            category = "cat_combos",
            subcategory = "Salad Baskets",
            price = 195.0,
            originalPrice = 250.0,
            unit = "1 Basket (Apples, Bananas, Oranges, Pears)",
            deliveryMinutes = 15,
            rating = 4.9f,
            ratingCount = 520,
            discountPercent = 22,
            emoji = "🧺",
            description = "Assorted daily seasonal fruits for healthy family nutrition.",
            badge = "Save 22%",
            isOrganic = true
        )
    )

    val addresses = listOf(
        AddressItem(
            id = "addr_home",
            title = "HOME",
            addressLine = "Floor 1, Flat 876, 12th Main Road",
            locality = "HAL 2nd Stage, Indiranagar, Bengaluru - 560038",
            isDefault = true
        ),
        AddressItem(
            id = "addr_work",
            title = "WORK",
            addressLine = "Tech Park Tower 4, 3rd Floor",
            locality = "Outer Ring Road, Bellandur, Bengaluru - 560103",
            isDefault = false
        )
    )

    val walletTransactions = listOf(
        WalletTransaction("tx_1", "Added to THE GREEN SHOP Money", 500.0, true, "Yesterday", "UPI • HDFC Bank"),
        WalletTransaction("tx_2", "Paid for Fresh Veggie Order #GS-98214", 95.0, false, "2 days ago", "Farm Produce Delivered"),
        WalletTransaction("tx_3", "Cashback: Organic Sunday Promo", 50.0, true, "4 days ago", "Promotional cashback")
    )

    val recipes = listOf(
        RecipeItem(
            id = "rec_garden_salad",
            name = "Farm Harvest Garden Salad",
            prepTime = "8 mins",
            calories = "95 kcal",
            difficulty = "Super Quick",
            rating = 4.9f,
            emoji = "🥗",
            ingredientProductIds = listOf("p_tomato", "p_cucumber", "p_carrot", "p_coriander", "p_mint"),
            instructions = listOf(
                "Dice crisp tomatoes, cucumbers, and sweet carrots.",
                "Toss with hand-torn mint and finely chopped coriander.",
                "Drizzle with lemon juice, sea salt, and crushed black pepper.",
                "Serve chilled for a burst of pure farm vitality!"
            )
        ),
        RecipeItem(
            id = "rec_green_smoothie",
            name = "Vitality Green Detox Juice",
            prepTime = "5 mins",
            calories = "110 kcal",
            difficulty = "Instant",
            rating = 4.8f,
            emoji = "🥤",
            ingredientProductIds = listOf("p_spinach", "p_cucumber", "p_banana", "p_ginger"),
            instructions = listOf(
                "Blend washed baby spinach with sliced cucumber and ginger.",
                "Add half a banana for natural creaminess and sweetness.",
                "Blend with a cup of cold water until vibrant green and smooth.",
                "Drink immediately for maximum enzymatic freshness!"
            )
        ),
        RecipeItem(
            id = "rec_fruit_platter",
            name = "Immunity Citrus Fruit Bowl",
            prepTime = "10 mins",
            calories = "160 kcal",
            difficulty = "Easy",
            rating = 4.9f,
            emoji = "🍉",
            ingredientProductIds = listOf("p_watermelon", "p_orange", "p_pomegranate", "p_banana"),
            instructions = listOf(
                "Chop ruby red watermelon into bite-sized cubes.",
                "Segment juicy Nagpur oranges and peel pomegranate arils.",
                "Slice golden bananas and mix in a large serving bowl.",
                "Top with a pinch of chaat masala and mint leaves."
            )
        )
    )

    val sampleOrders = listOf(
        Order(
            id = "GS-98214",
            items = listOf(
                CartItem(products.first { it.id == "p_tomato" }, 1),
                CartItem(products.first { it.id == "p_coriander" }, 1),
                CartItem(products.first { it.id == "p_banana" }, 1)
            ),
            totalAmount = 79.0,
            placedTime = "Today, 10:04 AM",
            status = OrderStatus.DELIVERED,
            deliveryAddress = "Floor 1, Flat 876, Indiranagar",
            etaMinutes = 0,
            deliveryOtp = "4829",
            riderName = "Ramesh Kumar",
            riderPhone = "+91 98765 01234",
            riderVehicle = "Green Electric Scooter • KA-05-EV-1904"
        )
    )
}
