# Changelog

## [1.9] – 2026-06-02 (versionCode 10)

### Changed
- Version bump — no new features. Promotes 1.8 changes to the next release increment.

---

## [1.8] – 2026-06-02 (versionCode 9)

### Added
- **Main Menu (Menu Utama)** — a new home screen displayed after the splash screen, showing all app modules as a 2-column grid of tappable cards: Products, Stock, Sales, Payment, Resellers, Returns, Ledger, and Settings. Each card has a coloured icon and a short description.
- **Home tab** added to the bottom navigation bar (replaces the Returns tab).

### Changed
- **Returns (Retur)** removed from the bottom navigation bar — now accessible exclusively from the Main Menu card. The screen still operates as a full-screen flow with a back button.
- **Products and Buku Kas** removed from the Settings menu — both are now reached directly from the Main Menu. Settings retains only the About link alongside business configuration fields.
- Splash screen now lands on the Main Menu instead of going directly to Sales.

---

## [1.7] – 2026-06-02 (versionCode 8)

### Fixed
- **Release build lint errors** — removed invalid `domain="cache"` exclusions from `backup_rules.xml` and `data_extraction_rules.xml`. The `cache` domain is not recognised by the Auto Backup schema; the cache directory is excluded by Android automatically and requires no explicit rule. Release assembly now passes lint clean.

---

## [1.6] – 2026-06-02 (versionCode 7)

### Added
- **Google Drive Auto Backup** — app data is automatically backed up to the user's Google account on Android's backup schedule (typically daily when charging on Wi-Fi). Restored automatically on reinstall or device migration with no user action required.
  - Backed up: Room database (all inventory, sales, payments, resellers, returns), business settings (name, phone, address), and business logo.
  - Excluded: cache directory (generated PDF receipts are transient and regenerable).
  - Configured via `backup_rules.xml` (API 26–30) and `data_extraction_rules.xml` (API 31+).

---

## [1.5] – 2026-06-01 (versionCode 6)

### Added
- **Splash screen** — eliminates the blank white flash on cold start using the AndroidX SplashScreen API. A branded splash (Design 2: blue background, DISTRO-KU logo, "Partner Distribusi Anda") displays for 2 seconds before the main screen appears.
- **Product edit** — each product card now has an edit (pencil) button. Tapping it opens the product form pre-filled with existing values for price adjustment, SKU, unit, threshold, or any other field. Saves via `UPDATE` rather than `INSERT`, preserving the product's history.

### Changed
- Design mockup files moved from `res/drawable/` to `design-assets/` at the project root to keep them out of the resource pipeline.

---

## [1.4] – 2026-06-01 (versionCode 5)

### Added
- **About screen** — accessible from Settings → About (Tentang Aplikasi). Contains a structured feature guide covering all modules: Products, Stock Management, Sales & Invoicing, Payment Settlement, Resellers, Product Returns, and Ledger & Analytics. Each section has an icon, title, and bullet-point description.
- App name, version number, and release date shown at the top of the About screen.
- Fully translated in English and Bahasa Indonesia, switches with the in-app language toggle.

---

## [1.3] – 2026-06-01 (versionCode 4)

### Added
- **Stock restock threshold** — each product can now have an optional minimum stock threshold set from the product form.
  - When current stock drops to or below the threshold, a **Low Stock Alert banner** appears at the top of the Products screen listing every at-risk product with its current stock and minimum level.
  - Individual product cards show a warning icon and a tinted background when below threshold.
  - The threshold is stored in the database via a proper schema migration (v2 → v3); existing installs upgrade in-place with no data loss.
- Strings translated in English and Bahasa Indonesia.

---

## [1.2] – 2026-06-01 (versionCode 3)

### Added
- **Ledger: Top Buyers analytics** — A new "Top Buyers" section appears in the Ledger screen whenever the selected period contains purchase data:
  - **Overall Top Buyer** card — highlights the single reseller with the highest total spend across all products in the period.
  - **Top Buyer Per Product** card — lists the leading buyer for each individual product, sorted by product name.
- Both analytics are period-aware (Today / This Week / This Month / All Time) and update reactively with the period filter.
- Strings translated in English and Bahasa Indonesia.

---

## [1.1] – 2025-xx-xx (versionCode 2)

### Added
- **Product return feature** — resellers can submit returns with per-item quantities, unit prices, and reasons (Unsold / Defect / Other). Return amounts are deducted from revenue and gross profit in the Ledger.
- **Language switching** — in-app toggle between English and Bahasa Indonesia across all screens.
- **Ledger screen** — period-filtered summary (Revenue, Collected, Outstanding Balance, Gross Profit) with a chronological entry list combining sales invoices, payment logs, and returns.
- **Navigation restructure** — bottom navigation bar covering Products, Stock, Sales, Payment, Resellers, Ledger, and Returns.

---

## [1.0] – 2025-xx-xx (versionCode 1)

### Added
- Initial release.
- **Product management** — add, edit, and delete products with SKU, unit, base cost, and selling price.
- **Reseller management** — register resellers with name, phone (contact-picker integration), and address.
- **Sales / invoicing** — cart-based sales order flow generating numbered invoices with UNPAID / PARTIAL / PAID status.
- **Stock ledger** — Stock In / Out / Adjustment In / Adjustment Out / Return In entries with running balance per product.
- **Stock Opname** — side-by-side system vs. physical count with one-tap adjustment save.
- **Payment settlement** — FIFO allocation engine distributes payments across outstanding invoices oldest-first, with allocation preview and PDF receipt generation shareable via WhatsApp or system share.
- **Room SQLite persistence** — offline-first, no server required.
