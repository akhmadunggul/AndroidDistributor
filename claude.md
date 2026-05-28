# Project Context & Guidelines

## Overview
- **What**: A standalone, local-first Android application designed for small product distributors to handle inventory management, sales invoicing, dynamic pricing, debt settlement logs, and digital receipt generation.
- **Why**: To provide field-based distributors with an offline-capable, serverless tool that tracks multi-layered stock operations, manages reseller credit records, and automates payment distribution.

## Tech Stack
- **Languages**: Kotlin v2.0+
- **Frameworks**: Android Jetpack Compose (Modern Declarative UI), Android Architecture Components (MVVM)
- **Databases/State**: Room SQLite Persistence Library, Kotlin Coroutines, and Asynchronous State Flows
- **Key UI Libraries**: Material Design 3 (M3) Components, Compose Foundation, Native `android.graphics.pdf`

## Development Commands
- **Install**: `./gradlew build --refresh-dependencies`
- **Build**: `./gradlew assembleDebug`
- **Dev**: Run configuration targeting local physical device or Android Virtual Device (AVD)
- **Test**: `./gradlew test` (Local Unit Tests) and `./gradlew connectedAndroidTest` (Instrumented Room/UI Tests)
- **Lint/Format**: `./gradlew ktlintCheck` / `./gradlew ktlintFormat`

## Architecture & Codebase Layout
- `app/src/main/java/com/distributor/app/data/`: Room entities, SQLite DAOs, local database configuration, and data mapping helpers.
- `app/src/main/java/com/distributor/app/repository/`: Data layer abstraction coordinating safe local storage execution and transactional security.
- `app/src/main/java/com/distributor/app/ui/screens/`: Pure, declarative Jetpack Compose UI layout layers (Products, Resellers, Stock, Sales, Payments).
- `app/src/main/java/com/distributor/app/ui/viewmodel/`: State management handlers processing business constraints, UI stream mapping (`StateFlow`), and input verification.
- `app/src/main/java/com/distributor/app/utils/`: Canvas rendering engines for local PDF assembly and OS share intents (WhatsApp FileProvider).

## Coding Conventions
- **Style**: Use clean, functional Compose functions paired with explicit Kotlin state hoistings and structured coroutine scopes.
- **Naming**: Use camelCase for variables/functions, PascalCase for Composable layouts and database Entities, and UPPER_CASE for ledger transaction constants.
- **Indentation**: Explicitly use 4-space indentation (Standard Android/Kotlin style guidelines).
- **State**: Prefer active stream state via Room database Flows over independent mutable client memory structures to ensure immediate app-wide UI synchronization.

## Core Behaviors & Guardrails
- **Action**: Exhaust all database, compilation, layout diagnostic options, and logcat analyses before prompting the user for structural structural changes.
- **Verification**: Never mark a feature, query, or algorithm modification complete without passing structural JVM unit testing (`./gradlew test`).
- **Errors**: Resolve all build warnings and linter violations (`ktlint`) before generating final refactoring patches or commits.
- **Prohibition**: Never push unhandled conversion exceptions (`NumberFormatException` on text input fields) to user-facing workflows.
- **Prohibition**: Do not edit underlying ledger schema designs mid-migration without establishing explicit fallback or destructive database recovery options.