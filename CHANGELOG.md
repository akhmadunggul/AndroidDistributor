# Changelog

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
