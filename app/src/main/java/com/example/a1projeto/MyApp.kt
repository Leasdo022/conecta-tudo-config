package com.example.a1projeto


import android.app.Application
import com.example.a1projeto.di.AppGraph

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}

