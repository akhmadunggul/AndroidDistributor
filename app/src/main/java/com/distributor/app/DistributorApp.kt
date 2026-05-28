package com.distributor.app

import android.app.Application

/**
 * Application subclass declared in AndroidManifest.xml.
 * Provides the Application context that AppDatabase.getInstance() requires
 * for its WAL-mode singleton. No explicit initialization is needed here —
 * the database is opened lazily on first DAO access.
 */
class DistributorApp : Application()
