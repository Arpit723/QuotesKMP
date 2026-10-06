package com.demo.quotes.di

import org.koin.core.module.Module

/** Platform-specific Koin definitions, such as the database driver factory. */
expect val platformModule: Module
